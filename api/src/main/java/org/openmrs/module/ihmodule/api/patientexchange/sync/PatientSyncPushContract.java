package org.openmrs.module.ihmodule.api.patientexchange.sync;

/**
 * Executes a central FHIR patient push for a {@link PatientSyncLog} row (scheduler replay).
 */
public interface PatientSyncPushContract {
	
	boolean pushPatientForSyncLog(PatientSyncLog pushRow, String operationLabel);
}
