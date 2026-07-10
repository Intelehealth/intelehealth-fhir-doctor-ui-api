package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.PatientIdentifierSnapshot;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.SourcePatientIdentifierUpdateResponse;
import org.openmrs.module.ihmodule.api.patientexchange.event.FhirSyncSuppressionContext;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.springframework.stereotype.Service;

/**
 * Applies MPI and source-patient identifier changes on the facility OpenMRS patient record.
 * <p>
 * Per-patient locking and duplicate cleanup prevent concurrent central-sync threads from creating
 * multiple active MPI or Source Patient Id rows for the same patient.
 */
@Service
public class LocalPatientMpiUpdateService extends IHConstant {
	
	/** Response body when {@code POST .../mpi/local} skips because MPI is already present. */
	public static final String MESSAGE_MPI_ALREADY_SET_LOCAL_ENDPOINT = "MPI is already assigned on this local patient. The local MPI identifier was not updated.";
	
	/** Response body when {@code POST .../sync/force} skips because MPI is already present. */
	public static final String MESSAGE_MPI_ALREADY_SET_FORCE_SYNC = "MPI is already assigned on this local patient. Force sync was not performed.";
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	/** Secondary OpenMRS type used elsewhere for the same central {@code Patient.id} value. */
	private static final String FHIR_PATIENT_ID_TYPE_NAME = "FHIR Patient ID";
	
	private static final String OPENMRS_ID_TYPE_TEXT = "OpenMRS ID";
	
	private static final String DUPLICATE_IDENTIFIER_VOID_REASON = "Duplicate identifier removed during MPI/source-id upsert";
	
	private static final String OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	private static final ConcurrentMap<String, Object> PATIENT_IDENTIFIER_UPSERT_LOCKS = new ConcurrentHashMap<String, Object>();
	
	/**
	 * Same rule as patient export scheduling: {@code true} when any identifier's type text equals
	 * {@code intelehealth.fhir.resource.identifier.name} ({@link #globalIdentifierName}), matching
	 * {@code DataSendToFHIR#hasMPI} semantics (null-safe; no extra heuristics) so operator
	 * endpoints do not diverge from the scheduler.
	 */
	/**
	 * {@code true} when the facility patient already has active MPI and Source Patient Id rows with
	 * non-blank values. Used to skip {@code patient_sync_log} on routine updates after initial
	 * central sync.
	 */
	public boolean localPatientHasMpiAndSourcePatientId(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return false;
		}
		try {
			org.openmrs.Patient patient = Context.getPatientService().getPatientByUuid(patientUuid.trim());
			return localPatientHasMpiAndSourcePatientId(patient);
		}
		catch (Exception ex) {
			return false;
		}
	}
	
	public boolean localPatientHasMpiAndSourcePatientId(org.openmrs.Patient patient) {
		return hasNonBlankMpiIdentifier(patient) && hasNonBlankSourcePatientId(patient);
	}
	
	public boolean patientHasMpiPerSchedulerExportRule(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return false;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (!identifier.hasType() || !identifier.getType().hasText()) {
				continue;
			}
			String text = identifier.getType().getText();
			if (text != null && text.equals(globalIdentifierName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @deprecated Only used by deprecated {@code POST .../patient-exchange/mpi-local}.
	 */
	@Deprecated
	public void applyMpiIdentifierToLocalPatient(String patientUuid, String mpiIdentifierValue) {
		final String uuid = requirePatientUuid(patientUuid);
		runWithPatientIdentifierLock(uuid, new Runnable() {
			
			@Override
			public void run() {
				org.openmrs.Patient patient = loadLocalPatientOrThrow(uuid);
				List<PatientIdentifier> active = collectActiveMpiIdentifiers(patient);
				PatientIdentifier canonical = chooseCanonicalIdentifier(active);
				voidExtraMpiIdentifiers(patient, canonical);
				if (canonical != null) {
					throw new LocalMpiAlreadySetException();
				}
				upsertMpiIdentifierOnNativePatient(patient, mpiIdentifierValue, false);
			}
		});
	}
	
	/**
	 * After a successful central patient write: apply MPI and optional Source Patient Id in one
	 * locked transaction and a single {@code savePatient}, so concurrent sync threads cannot insert
	 * duplicate source rows between separate MPI and source upserts.
	 */
	public void upsertCentralSyncIdentifiersToLocalPatient(String patientUuid, String mpiIdentifierValue,
	        String centralSourceLogicalId) {
		if (mpiIdentifierValue == null || mpiIdentifierValue.trim().isEmpty()) {
			throw new IllegalArgumentException("mpiIdentifierValue is required");
		}
		final String uuid = requirePatientUuid(patientUuid);
		final String mpiTrim = mpiIdentifierValue.trim();
		final String sourceTrim = centralSourceLogicalId != null ? centralSourceLogicalId.trim() : null;
		runWithPatientIdentifierLock(uuid, new Runnable() {
			
			@Override
			public void run() {
				org.openmrs.Patient patient = loadLocalPatientOrThrow(uuid);
				applyMpiIdentifierChangesWithoutSave(patient, mpiTrim, true);
				if (StringUtils.isNotBlank(sourceTrim)) {
					applyCentralSourcePatientIdChangesWithoutSave(patient, sourceTrim, null);
				}
				savePatientSuppressed(patient);
			}
		});
	}
	
	/**
	 * Scheduler/transfer path: add MPI when absent or update existing MPI to the supplied value.
	 */
	public void upsertMpiIdentifierToLocalPatient(String patientUuid, String mpiIdentifierValue) {
		final String uuid = requirePatientUuid(patientUuid);
		runWithPatientIdentifierLock(uuid, new Runnable() {
			
			@Override
			public void run() {
				org.openmrs.Patient patient = loadLocalPatientOrThrow(uuid);
				upsertMpiIdentifierOnNativePatient(patient, mpiIdentifierValue, true);
			}
		});
	}
	
	/**
	 * Stores the central FHIR server logical id (Patient.id) on the facility patient as the
	 * configured source patient identifier type ({@link #sourcePatientIdTypeUuid} /
	 * {@link #sourcePatientIdTypeName}).
	 * 
	 * @return {@code true} when an existing source identifier was updated; {@code false} when a new
	 *         one was added
	 */
	public boolean upsertCentralSourcePatientIdToLocalPatient(String patientUuid, String centralLogicalId) {
		if (centralLogicalId == null || centralLogicalId.trim().isEmpty()) {
			return false;
		}
		final String uuid = requirePatientUuid(patientUuid);
		final String idVal = centralLogicalId.trim();
		final boolean[] updatedExisting = new boolean[1];
		runWithPatientIdentifierLock(uuid, new Runnable() {
			
			@Override
			public void run() {
				org.openmrs.Patient patient = loadLocalPatientOrThrow(uuid);
				updatedExisting[0] = applyCentralSourcePatientIdChangesWithoutSave(patient, idVal, null);
				savePatientSuppressed(patient);
			}
		});
		return updatedExisting[0];
	}
	
	/**
	 * REST entry: upsert source patient identifier (central FHIR Patient logical id) on the
	 * facility patient — update when a matching row exists, otherwise create.
	 * 
	 * @param locationUuid OpenMRS location for the identifier row (optional). When omitted, uses
	 *            the patient's preferred OpenMRS ID location, then the module default location.
	 *            Required only when creating a new row and neither can be resolved.
	 */
	public SourcePatientIdentifierUpdateResponse upsertSourcePatientIdentifier(String patientUuid, String identifierValue,
	        String locationUuid) {
		if (identifierValue == null || identifierValue.trim().isEmpty()) {
			throw new IllegalArgumentException("identifierValue is required");
		}
		final String uuid = requirePatientUuid(patientUuid);
		final String idVal = identifierValue.trim();
		final String locUuid = StringUtils.trimToNull(locationUuid);
		final SourcePatientIdentifierUpdateResponse[] responseHolder = new SourcePatientIdentifierUpdateResponse[1];
		runWithPatientIdentifierLock(uuid, new Runnable() {
			
			@Override
			public void run() {
				org.openmrs.Patient patient = loadLocalPatientOrThrow(uuid);
				List<PatientIdentifier> active = collectActiveCentralSourceLinkIdentifiers(patient, idVal);
				PatientIdentifier canonical = chooseCanonicalSourceLinkIdentifier(active);
				if (canonical == null && locUuid == null && requiresExplicitLocationUuidForNewSourceIdentifier(patient)) {
					throw new IllegalArgumentException(
					        "locationUuid is required when adding a new source patient identifier");
				}
				boolean updatedExisting = applyCentralSourcePatientIdChangesWithoutSave(patient, idVal, locUuid);
				savePatientSuppressed(patient);
				String operation = updatedExisting ? "updated" : "created";
				org.openmrs.Patient saved = loadLocalPatientOrThrow(uuid);
				SourcePatientIdentifierUpdateResponse response = new SourcePatientIdentifierUpdateResponse();
				response.setStatus("ok");
				response.setPatientUuid(saved.getUuid());
				response.setSourcePatientIdentifierValue(idVal);
				response.setOperation(operation);
				response.setIdentifierTypeUuid(sourcePatientIdTypeUuid);
				response.setIdentifierTypeName(sourcePatientIdTypeName);
				response.setIdentifiers(buildIdentifierSnapshots(saved));
				responseHolder[0] = response;
			}
		});
		return responseHolder[0];
	}
	
	private static String requirePatientUuid(String patientUuid) {
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		return patientUuid.trim();
	}
	
	private static void runWithPatientIdentifierLock(String patientUuid, Runnable work) {
		Object lock = PATIENT_IDENTIFIER_UPSERT_LOCKS.computeIfAbsent(patientUuid,
		    new java.util.function.Function<String, Object>() {
			    
			    @Override
			    public Object apply(String key) {
				    return new Object();
			    }
		    });
		synchronized (lock) {
			work.run();
		}
	}
	
	private static void addSourcePatientIdentifier(org.openmrs.Patient patient, String idVal,
	        PatientIdentifierType sourceIdType, Location location, User creator) {
		PatientIdentifier pid = new PatientIdentifier();
		pid.setIdentifier(idVal);
		pid.setIdentifierType(sourceIdType);
		pid.setLocation(location);
		pid.setPreferred(false);
		applyResolvedCreator(pid, creator, false, creator != null);
		patient.addIdentifier(pid);
	}
	
	private static void savePatientSuppressed(final org.openmrs.Patient patient) {
		FhirSyncSuppressionContext.runSuppressed(new Runnable() {
			
			@Override
			public void run() {
				Context.getPatientService().savePatient(patient);
			}
		});
	}
	
	private org.openmrs.Patient loadLocalPatientOrThrow(String patientUuid) {
		org.openmrs.Patient patient = Context.getPatientService().getPatientByUuid(patientUuid);
		if (patient == null) {
			throw new IllegalStateException("Local Patient not found for uuid=" + patientUuid);
		}
		return patient;
	}
	
	private void upsertMpiIdentifierOnNativePatient(org.openmrs.Patient patient, String mpiIdentifierValue,
	        boolean allowOverwriteExisting) {
		applyMpiIdentifierChangesWithoutSave(patient, mpiIdentifierValue, allowOverwriteExisting);
		savePatientSuppressed(patient);
	}
	
	private void applyMpiIdentifierChangesWithoutSave(org.openmrs.Patient patient, String mpiIdentifierValue,
	        boolean allowOverwriteExisting) {
		if (mpiIdentifierValue == null || mpiIdentifierValue.trim().isEmpty()) {
			throw new IllegalArgumentException("mpiIdentifierValue is required");
		}
		String mpiVal = mpiIdentifierValue.trim();
		PatientIdentifierType mpiType = resolveMpiIdentifierType();
		IdentifierLocationResolution locationResolution = resolveIdentifierLocationForPatient(patient, null);
		User openMrsIdCreator = resolveCreatorFromPatientPreferredOpenMrsIdentifier(patient);
		List<PatientIdentifier> active = collectActiveMpiIdentifiers(patient);
		PatientIdentifier canonical = chooseCanonicalIdentifier(active);
		voidExtraMpiIdentifiers(patient, canonical);
		if (canonical != null) {
			if (!allowOverwriteExisting) {
				throw new LocalMpiAlreadySetException();
			}
			canonical.setIdentifier(mpiVal);
			canonical.setIdentifierType(mpiType);
			applyResolvedLocation(canonical, locationResolution, true);
			applyResolvedCreator(canonical, openMrsIdCreator, true, openMrsIdCreator != null);
		} else {
			PatientIdentifier mpi = new PatientIdentifier();
			mpi.setIdentifier(mpiVal);
			mpi.setIdentifierType(mpiType);
			applyResolvedLocation(mpi, locationResolution, false);
			applyResolvedCreator(mpi, openMrsIdCreator, false, openMrsIdCreator != null);
			mpi.setPreferred(false);
			patient.addIdentifier(mpi);
		}
	}
	
	/**
	 * @return {@code true} when an existing central-source link row was updated; {@code false} when
	 *         a new row was added
	 */
	private boolean applyCentralSourcePatientIdChangesWithoutSave(org.openmrs.Patient patient, String centralLogicalId,
	        String locationUuid) {
		String idVal = centralLogicalId.trim();
		PatientIdentifierType sourceIdType = resolveSourcePatientIdIdentifierType();
		IdentifierLocationResolution locationResolution = resolveIdentifierLocationForPatient(patient, locationUuid);
		User openMrsIdCreator = resolveCreatorFromPatientPreferredOpenMrsIdentifier(patient);
		List<PatientIdentifier> active = collectActiveCentralSourceLinkIdentifiers(patient, idVal);
		PatientIdentifier canonical = chooseCanonicalSourceLinkIdentifier(active);
		voidExtraCentralSourceLinkIdentifiers(patient, canonical, idVal);
		if (canonical != null) {
			canonical.setIdentifier(idVal);
			canonical.setIdentifierType(sourceIdType);
			applyResolvedLocation(canonical, locationResolution, true);
			applyResolvedCreator(canonical, openMrsIdCreator, true, openMrsIdCreator != null);
			return true;
		}
		addSourcePatientIdentifier(patient, idVal, sourceIdType, locationResolution != null ? locationResolution.location
		        : null, openMrsIdCreator);
		return false;
	}
	
	private boolean patientHasMpiOnNativePatient(org.openmrs.Patient patient) {
		return hasNonBlankMpiIdentifier(patient);
	}
	
	private boolean hasNonBlankMpiIdentifier(org.openmrs.Patient patient) {
		PatientIdentifier mpi = findExistingMpiIdentifier(patient);
		return mpi != null && StringUtils.isNotBlank(mpi.getIdentifier());
	}
	
	private boolean hasNonBlankSourcePatientId(org.openmrs.Patient patient) {
		PatientIdentifier source = findExistingSourcePatientIdIdentifier(patient);
		return source != null && StringUtils.isNotBlank(source.getIdentifier());
	}
	
	private PatientIdentifier findExistingMpiIdentifier(org.openmrs.Patient patient) {
		List<PatientIdentifier> active = collectActiveMpiIdentifiers(patient);
		return active.isEmpty() ? null : active.get(0);
	}
	
	private List<PatientIdentifier> collectActiveMpiIdentifiers(org.openmrs.Patient patient) {
		List<PatientIdentifier> found = new ArrayList<PatientIdentifier>();
		if (patient == null || patient.getIdentifiers() == null) {
			return found;
		}
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier == null || identifier.getVoided() || identifier.getIdentifierType() == null) {
				continue;
			}
			if (isMpiIdentifierType(identifier.getIdentifierType())) {
				found.add(identifier);
			}
		}
		return found;
	}
	
	private void voidExtraMpiIdentifiers(org.openmrs.Patient patient, PatientIdentifier keep) {
		for (PatientIdentifier identifier : collectActiveMpiIdentifiers(patient)) {
			if (keep != null && identifier == keep) {
				continue;
			}
			voidPatientIdentifier(identifier);
		}
	}
	
	private boolean isMpiIdentifierType(PatientIdentifierType pit) {
		if (pit == null || pit.getName() == null) {
			return false;
		}
		String typeName = pit.getName();
		return typeName.equalsIgnoreCase(globalIdentifierName) || MPI_TYPE_TEXT.equalsIgnoreCase(typeName);
	}
	
	private PatientIdentifier findExistingSourcePatientIdIdentifier(org.openmrs.Patient patient) {
		List<PatientIdentifier> active = collectActiveCentralSourceLinkIdentifiers(patient, null);
		return active.isEmpty() ? null : active.get(0);
	}
	
	/**
	 * Rows that store the central FHIR {@code Patient.id}: configured Source Patient Id type,
	 * {@link #FHIR_PATIENT_ID_TYPE_NAME}, or any non-MPI/non-OpenMRS-ID row matching
	 * {@code centralLogicalId}.
	 */
	private List<PatientIdentifier> collectActiveCentralSourceLinkIdentifiers(org.openmrs.Patient patient,
	        String centralLogicalId) {
		List<PatientIdentifier> found = new ArrayList<PatientIdentifier>();
		if (patient == null || patient.getIdentifiers() == null) {
			return found;
		}
		String idVal = StringUtils.trimToNull(centralLogicalId);
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier == null || identifier.getVoided() || identifier.getIdentifierType() == null) {
				continue;
			}
			if (isCentralSourceLinkIdentifier(identifier, idVal)) {
				found.add(identifier);
			}
		}
		return found;
	}
	
	private void voidExtraCentralSourceLinkIdentifiers(org.openmrs.Patient patient, PatientIdentifier keep,
	        String centralLogicalId) {
		for (PatientIdentifier identifier : collectActiveCentralSourceLinkIdentifiers(patient, centralLogicalId)) {
			if (keep != null && identifier == keep) {
				continue;
			}
			voidPatientIdentifier(identifier);
		}
	}
	
	private boolean isCentralSourceLinkIdentifier(PatientIdentifier identifier, String centralLogicalId) {
		if (identifier == null || identifier.getVoided() || identifier.getIdentifierType() == null) {
			return false;
		}
		PatientIdentifierType pit = identifier.getIdentifierType();
		if (isMpiIdentifierType(pit) || isOpenMrsIdIdentifierType(pit)) {
			return false;
		}
		if (isSourcePatientIdIdentifierType(pit) || isFhirPatientIdIdentifierType(pit)) {
			return true;
		}
		String value = StringUtils.trimToNull(identifier.getIdentifier());
		return centralLogicalId != null && centralLogicalId.equals(value);
	}
	
	private boolean isFhirPatientIdIdentifierType(PatientIdentifierType pit) {
		return pit != null && pit.getName() != null && FHIR_PATIENT_ID_TYPE_NAME.equalsIgnoreCase(pit.getName());
	}
	
	private boolean isOpenMrsIdIdentifierType(PatientIdentifierType pit) {
		return pit != null && pit.getName() != null && OPENMRS_ID_TYPE_TEXT.equalsIgnoreCase(pit.getName());
	}
	
	private PatientIdentifier chooseCanonicalSourceLinkIdentifier(List<PatientIdentifier> active) {
		if (active == null || active.isEmpty()) {
			return null;
		}
		PatientIdentifierType sourceType = null;
		try {
			sourceType = resolveSourcePatientIdIdentifierType();
		}
		catch (RuntimeException ex) {
			sourceType = null;
		}
		if (sourceType != null) {
			for (PatientIdentifier identifier : active) {
				if (identifier != null && identifier.getIdentifierType() != null
				        && sourceType.getUuid().equals(identifier.getIdentifierType().getUuid())) {
					return identifier;
				}
			}
		}
		for (PatientIdentifier identifier : active) {
			if (identifier != null && identifier.getIdentifierType() != null
			        && isFhirPatientIdIdentifierType(identifier.getIdentifierType())) {
				return identifier;
			}
		}
		return chooseCanonicalIdentifier(active);
	}
	
	private boolean isSourcePatientIdIdentifierType(PatientIdentifierType pit) {
		if (pit == null) {
			return false;
		}
		if (sourcePatientIdTypeUuid != null && sourcePatientIdTypeUuid.equals(pit.getUuid())) {
			return true;
		}
		String typeName = pit.getName();
		return typeName != null && sourcePatientIdTypeName != null && sourcePatientIdTypeName.equalsIgnoreCase(typeName);
	}
	
	/**
	 * Prefer a preferred identifier when multiple active rows exist; otherwise keep the first.
	 */
	private static PatientIdentifier chooseCanonicalIdentifier(List<PatientIdentifier> active) {
		if (active == null || active.isEmpty()) {
			return null;
		}
		for (PatientIdentifier identifier : active) {
			if (identifier != null && Boolean.TRUE.equals(identifier.getPreferred())) {
				return identifier;
			}
		}
		return active.get(0);
	}
	
	private static void voidPatientIdentifier(PatientIdentifier identifier) {
		if (identifier == null || Boolean.TRUE.equals(identifier.getVoided())) {
			return;
		}
		identifier.setVoided(true);
		if (StringUtils.isBlank(identifier.getVoidReason())) {
			identifier.setVoidReason(DUPLICATE_IDENTIFIER_VOID_REASON);
		}
	}
	
	private PatientIdentifierType resolveSourcePatientIdIdentifierType() {
		if (sourcePatientIdTypeUuid != null && !sourcePatientIdTypeUuid.trim().isEmpty()) {
			PatientIdentifierType type = Context.getPatientService().getPatientIdentifierTypeByUuid(
			    sourcePatientIdTypeUuid.trim());
			if (type != null && !type.getRetired()) {
				return type;
			}
		}
		if (sourcePatientIdTypeName != null && !sourcePatientIdTypeName.trim().isEmpty()) {
			PatientIdentifierType type = Context.getPatientService().getPatientIdentifierTypeByName(
			    sourcePatientIdTypeName.trim());
			if (type != null && !type.getRetired()) {
				return type;
			}
		}
		throw new IllegalStateException("Patient identifier type not found for source patient id (uuid="
		        + sourcePatientIdTypeUuid + ", name=" + sourcePatientIdTypeName + ")");
	}
	
	private static List<PatientIdentifierSnapshot> buildIdentifierSnapshots(org.openmrs.Patient patient) {
		List<PatientIdentifierSnapshot> snapshots = new ArrayList<PatientIdentifierSnapshot>();
		if (patient == null || patient.getIdentifiers() == null) {
			return snapshots;
		}
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier == null || identifier.getVoided()) {
				continue;
			}
			PatientIdentifierSnapshot snap = new PatientIdentifierSnapshot();
			snap.setValue(identifier.getIdentifier());
			snap.setPreferred(identifier.getPreferred());
			snap.setVoided(identifier.getVoided());
			if (identifier.getIdentifierType() != null) {
				snap.setIdentifierTypeUuid(identifier.getIdentifierType().getUuid());
				snap.setIdentifierTypeName(identifier.getIdentifierType().getName());
			}
			if (identifier.getLocation() != null) {
				snap.setLocationUuid(identifier.getLocation().getUuid());
			}
			snapshots.add(snap);
		}
		return snapshots;
	}
	
	private PatientIdentifierType resolveMpiIdentifierType() {
		PatientIdentifierType type = Context.getPatientService().getPatientIdentifierTypeByName(globalIdentifierName);
		if (type != null && !type.getRetired()) {
			return type;
		}
		type = Context.getPatientService().getPatientIdentifierTypeByName(MPI_TYPE_TEXT);
		if (type != null && !type.getRetired()) {
			return type;
		}
		List<PatientIdentifierType> allTypes = Context.getPatientService().getAllPatientIdentifierTypes();
		for (PatientIdentifierType candidate : allTypes) {
			if (candidate == null || candidate.getRetired() || candidate.getName() == null) {
				continue;
			}
			if (candidate.getName().equalsIgnoreCase(globalIdentifierName)
			        || candidate.getName().equalsIgnoreCase(MPI_TYPE_TEXT)) {
				return candidate;
			}
		}
		throw new IllegalStateException("Patient identifier type not found for MPI name=" + globalIdentifierName);
	}
	
	/**
	 * Resolves the OpenMRS {@link Location} for MPI / Source Patient Id rows.
	 * <ol>
	 * <li>Explicit {@code locationUuid} from REST API (unchanged behaviour)</li>
	 * <li>Location on the patient's preferred OpenMRS ID identifier (central sync path)</li>
	 * <li>Module default {@link #OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID}</li>
	 * </ol>
	 */
	private IdentifierLocationResolution resolveIdentifierLocationForPatient(org.openmrs.Patient patient, String locationUuid) {
		if (StringUtils.isNotBlank(locationUuid)) {
			Location location = Context.getLocationService().getLocationByUuid(locationUuid.trim());
			if (location == null) {
				throw new IllegalArgumentException("Location not found for UUID: " + locationUuid.trim());
			}
			return new IdentifierLocationResolution(location, false, true);
		}
		Location preferredOpenMrsLocation = resolveLocationFromPatientPreferredOpenMrsIdentifier(patient);
		if (preferredOpenMrsLocation != null) {
			return new IdentifierLocationResolution(preferredOpenMrsLocation, true, false);
		}
		return new IdentifierLocationResolution(getDefaultIdentifierLocation(), false, false);
	}
	
	/**
	 * Preferred OpenMRS ID row first ({@code preferred=true}), then any active OpenMRS ID with a
	 * location.
	 */
	/**
	 * {@code true} when a new Source Patient Id row cannot be created without an explicit
	 * {@code locationUuid} (no preferred OpenMRS ID location and module default location missing).
	 */
	private boolean requiresExplicitLocationUuidForNewSourceIdentifier(org.openmrs.Patient patient) {
		if (resolveLocationFromPatientPreferredOpenMrsIdentifier(patient) != null) {
			return false;
		}
		try {
			getDefaultIdentifierLocation();
			return false;
		}
		catch (IllegalStateException ex) {
			return true;
		}
	}
	
	private Location resolveLocationFromPatientPreferredOpenMrsIdentifier(org.openmrs.Patient patient) {
		PatientIdentifier openMrsId = findPreferredOpenMrsIdentifier(patient);
		if (openMrsId == null || openMrsId.getLocation() == null) {
			return null;
		}
		return openMrsId.getLocation();
	}
	
	private User resolveCreatorFromPatientPreferredOpenMrsIdentifier(org.openmrs.Patient patient) {
		PatientIdentifier openMrsId = findPreferredOpenMrsIdentifier(patient);
		if (openMrsId == null || openMrsId.getCreator() == null) {
			return null;
		}
		return openMrsId.getCreator();
	}
	
	private PatientIdentifier findPreferredOpenMrsIdentifier(org.openmrs.Patient patient) {
		if (patient == null || patient.getIdentifiers() == null) {
			return null;
		}
		PatientIdentifier preferredOpenMrs = null;
		PatientIdentifier anyOpenMrs = null;
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier == null || identifier.getVoided() || identifier.getIdentifierType() == null) {
				continue;
			}
			if (!isOpenMrsIdIdentifierType(identifier.getIdentifierType())) {
				continue;
			}
			if (Boolean.TRUE.equals(identifier.getPreferred())) {
				preferredOpenMrs = identifier;
				break;
			}
			if (anyOpenMrs == null) {
				anyOpenMrs = identifier;
			}
		}
		return preferredOpenMrs != null ? preferredOpenMrs : anyOpenMrs;
	}
	
	/**
	 * Insert: always apply resolved location when present. Update: apply when the caller supplied
	 * {@code locationUuid}, when aligned to preferred OpenMRS ID, or when the row has no location
	 * yet (legacy default-backfill only).
	 */
	static void applyResolvedLocationForTest(PatientIdentifier identifier, IdentifierLocationResolution resolution,
	        boolean isUpdate) {
		applyResolvedLocation(identifier, resolution, isUpdate);
	}
	
	static void applyResolvedCreatorForTest(PatientIdentifier identifier, User creator, boolean isUpdate,
	        boolean fromPreferredOpenMrsIdentifier) {
		applyResolvedCreator(identifier, creator, isUpdate, fromPreferredOpenMrsIdentifier);
	}
	
	boolean requiresExplicitLocationUuidForNewSourceIdentifierForTest(org.openmrs.Patient patient) {
		return requiresExplicitLocationUuidForNewSourceIdentifier(patient);
	}
	
	private static void applyResolvedLocation(PatientIdentifier identifier, IdentifierLocationResolution resolution,
	        boolean isUpdate) {
		if (identifier == null || resolution == null || resolution.location == null) {
			return;
		}
		if (!isUpdate) {
			identifier.setLocation(resolution.location);
			return;
		}
		if (resolution.explicitFromRequest || resolution.fromPreferredOpenMrsIdentifier) {
			identifier.setLocation(resolution.location);
		} else if (identifier.getLocation() == null) {
			identifier.setLocation(resolution.location);
		}
	}
	
	/**
	 * Insert: always apply creator from preferred OpenMRS ID when present. Update: align when the
	 * creator comes from preferred OpenMRS ID, or backfill when the row has no creator yet.
	 */
	private static void applyResolvedCreator(PatientIdentifier identifier, User creator, boolean isUpdate,
	        boolean fromPreferredOpenMrsIdentifier) {
		if (identifier == null || creator == null) {
			return;
		}
		if (!isUpdate) {
			identifier.setCreator(creator);
			return;
		}
		if (fromPreferredOpenMrsIdentifier) {
			identifier.setCreator(creator);
		} else if (identifier.getCreator() == null) {
			identifier.setCreator(creator);
		}
	}
	
	private Location getDefaultIdentifierLocation() {
		Location location = Context.getLocationService().getLocationByUuid(OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID);
		if (location != null) {
			return location;
		}
		throw new IllegalStateException("Location not found for UUID: " + OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID);
	}
	
	static final class IdentifierLocationResolution {
		
		private final Location location;
		
		private final boolean fromPreferredOpenMrsIdentifier;
		
		private final boolean explicitFromRequest;
		
		IdentifierLocationResolution(Location location, boolean fromPreferredOpenMrsIdentifier, boolean explicitFromRequest) {
			this.location = location;
			this.fromPreferredOpenMrsIdentifier = fromPreferredOpenMrsIdentifier;
			this.explicitFromRequest = explicitFromRequest;
		}
	}
}
