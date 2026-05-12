package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.List;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
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
