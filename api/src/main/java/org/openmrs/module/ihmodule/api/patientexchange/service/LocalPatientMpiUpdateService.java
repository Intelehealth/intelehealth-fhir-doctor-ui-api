package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.PatientIdentifierSnapshot;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.SourcePatientIdentifierUpdateResponse;
import org.openmrs.module.ihmodule.api.patientexchange.event.FhirSyncSuppressionContext;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.springframework.stereotype.Service;

/**
 * Applies MPI identifier changes on the facility OpenMRS patient record.
 */
@Service
public class LocalPatientMpiUpdateService extends IHConstant {
	
	/** Response body when {@code POST .../mpi/local} skips because MPI is already present. */
	public static final String MESSAGE_MPI_ALREADY_SET_LOCAL_ENDPOINT = "MPI is already assigned on this local patient. The local MPI identifier was not updated.";
	
	/** Response body when {@code POST .../sync/force} skips because MPI is already present. */
	public static final String MESSAGE_MPI_ALREADY_SET_FORCE_SYNC = "MPI is already assigned on this local patient. Force sync was not performed.";
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	private static final String OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	/**
	 * Same rule as patient export scheduling: {@code true} when any identifier's type text equals
	 * {@code intelehealth.fhir.resource.identifier.name} ({@link #globalIdentifierName}), matching
	 * {@code DataSendToFHIR#hasMPI} semantics (null-safe; no extra heuristics) so operator
	 * endpoints do not diverge from the scheduler.
	 */
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
	
	public void applyMpiIdentifierToLocalPatient(String patientUuid, String mpiIdentifierValue) {
		org.openmrs.Patient patient = loadLocalPatientOrThrow(patientUuid);
		if (patientHasMpiOnNativePatient(patient)) {
			throw new LocalMpiAlreadySetException();
		}
		upsertMpiIdentifierOnNativePatient(patient, mpiIdentifierValue, false);
	}
	
	/**
	 * Scheduler/transfer path: add MPI when absent or update existing MPI to the supplied value.
	 */
	public void upsertMpiIdentifierToLocalPatient(String patientUuid, String mpiIdentifierValue) {
		org.openmrs.Patient patient = loadLocalPatientOrThrow(patientUuid);
		upsertMpiIdentifierOnNativePatient(patient, mpiIdentifierValue, true);
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
		org.openmrs.Patient patient = loadLocalPatientOrThrow(patientUuid);
		String idVal = centralLogicalId.trim();
		PatientIdentifierType sourceIdType = resolveSourcePatientIdIdentifierType();
		Location location = resolveIdentifierLocation();
		PatientIdentifier existing = findExistingSourcePatientIdIdentifier(patient);
		boolean updatedExisting = existing != null;
		if (existing != null) {
			existing.setIdentifier(idVal);
			existing.setIdentifierType(sourceIdType);
			if (existing.getLocation() == null && location != null) {
				existing.setLocation(location);
			}
		} else {
			addSourcePatientIdentifier(patient, idVal, sourceIdType, location);
		}
		savePatientSuppressed(patient);
		return updatedExisting;
	}
	
	/**
	 * REST/operator entry: adds a source patient identifier only when the patient has none for the
	 * configured type. Does not update an existing source identifier.
	 */
	public SourcePatientIdentifierUpdateResponse upsertSourcePatientIdentifier(String patientUuid, String identifierValue) {
		if (identifierValue == null || identifierValue.trim().isEmpty()) {
			throw new IllegalArgumentException("identifierValue is required");
		}
		String idVal = identifierValue.trim();
		org.openmrs.Patient patient = loadLocalPatientOrThrow(patientUuid);
		PatientIdentifier existing = findExistingSourcePatientIdIdentifier(patient);
		String operation;
		String sourceIdValue;
		if (existing != null) {
			operation = "unchanged";
			sourceIdValue = existing.getIdentifier();
		} else {
			PatientIdentifierType sourceIdType = resolveSourcePatientIdIdentifierType();
			Location location = resolveIdentifierLocation();
			addSourcePatientIdentifier(patient, idVal, sourceIdType, location);
			savePatientSuppressed(patient);
			operation = "created";
			sourceIdValue = idVal;
		}
		org.openmrs.Patient saved = loadLocalPatientOrThrow(patientUuid);
		SourcePatientIdentifierUpdateResponse response = new SourcePatientIdentifierUpdateResponse();
		response.setStatus("ok");
		response.setPatientUuid(saved.getUuid());
		response.setSourcePatientIdentifierValue(sourceIdValue);
		response.setOperation(operation);
		response.setIdentifierTypeUuid(sourcePatientIdTypeUuid);
		response.setIdentifierTypeName(sourcePatientIdTypeName);
		response.setIdentifiers(buildIdentifierSnapshots(saved));
		return response;
	}
	
	private static void addSourcePatientIdentifier(org.openmrs.Patient patient, String idVal,
	        PatientIdentifierType sourceIdType, Location location) {
		PatientIdentifier pid = new PatientIdentifier();
		pid.setIdentifier(idVal);
		pid.setIdentifierType(sourceIdType);
		pid.setLocation(location);
		pid.setPreferred(false);
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
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		org.openmrs.Patient patient = Context.getPatientService().getPatientByUuid(patientUuid.trim());
		if (patient == null) {
			throw new IllegalStateException("Local Patient not found for uuid=" + patientUuid.trim());
		}
		return patient;
	}
	
	private void upsertMpiIdentifierOnNativePatient(org.openmrs.Patient patient, String mpiIdentifierValue,
	        boolean allowOverwriteExisting) {
		if (mpiIdentifierValue == null || mpiIdentifierValue.trim().isEmpty()) {
			throw new IllegalArgumentException("mpiIdentifierValue is required");
		}
		String mpiVal = mpiIdentifierValue.trim();
		PatientIdentifierType mpiType = resolveMpiIdentifierType();
		Location location = resolveIdentifierLocation();
		PatientIdentifier existing = findExistingMpiIdentifier(patient);
		if (existing != null) {
			if (!allowOverwriteExisting) {
				throw new LocalMpiAlreadySetException();
			}
			existing.setIdentifier(mpiVal);
			existing.setIdentifierType(mpiType);
			if (existing.getLocation() == null && location != null) {
				existing.setLocation(location);
			}
		} else {
			PatientIdentifier mpi = new PatientIdentifier();
			mpi.setIdentifier(mpiVal);
			mpi.setIdentifierType(mpiType);
			mpi.setLocation(location);
			mpi.setPreferred(false);
			patient.addIdentifier(mpi);
		}
		FhirSyncSuppressionContext.runSuppressed(new Runnable() {
			
			@Override
			public void run() {
				Context.getPatientService().savePatient(patient);
			}
		});
	}
	
	private boolean patientHasMpiOnNativePatient(org.openmrs.Patient patient) {
		return findExistingMpiIdentifier(patient) != null;
	}
	
	private PatientIdentifier findExistingMpiIdentifier(org.openmrs.Patient patient) {
		if (patient == null || patient.getIdentifiers() == null) {
			return null;
		}
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier == null || identifier.getVoided() || identifier.getIdentifierType() == null) {
				continue;
			}
			String typeName = identifier.getIdentifierType().getName();
			if (typeName != null
			        && (typeName.equalsIgnoreCase(globalIdentifierName) || MPI_TYPE_TEXT.equalsIgnoreCase(typeName))) {
				return identifier;
			}
		}
		return null;
	}
	
	private PatientIdentifier findExistingSourcePatientIdIdentifier(org.openmrs.Patient patient) {
		if (patient == null || patient.getIdentifiers() == null) {
			return null;
		}
		for (PatientIdentifier identifier : patient.getIdentifiers()) {
			if (identifier == null || identifier.getVoided() || identifier.getIdentifierType() == null) {
				continue;
			}
			org.openmrs.PatientIdentifierType pit = identifier.getIdentifierType();
			if (sourcePatientIdTypeUuid != null && sourcePatientIdTypeUuid.equals(pit.getUuid())) {
				return identifier;
			}
			String typeName = pit.getName();
			if (typeName != null && sourcePatientIdTypeName != null && sourcePatientIdTypeName.equalsIgnoreCase(typeName)) {
				return identifier;
			}
		}
		return null;
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
		List<PatientIdentifierSnapshot> snapshots = new ArrayList<>();
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
	
	private Location resolveIdentifierLocation() {
		Location location = Context.getLocationService().getLocationByUuid(OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID);
		if (location != null) {
			return location;
		}
		throw new IllegalStateException("Location not found for UUID: " + OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID);
	}
}
