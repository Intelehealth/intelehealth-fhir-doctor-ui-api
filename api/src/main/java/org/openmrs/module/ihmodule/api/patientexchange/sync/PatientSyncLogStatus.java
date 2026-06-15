package org.openmrs.module.ihmodule.api.patientexchange.sync;

/**
 * Push attempt status for {@code patient_sync_log} (aligned with SHR
 * {@code intelehealth_shr_sync_log}).
 */
public enum PatientSyncLogStatus {
	
	PENDING, SUCCESS, FAILED, FAILED_PERMANENT
}
