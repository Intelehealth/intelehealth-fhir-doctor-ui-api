package org.openmrs.module.ihmodule.api.patientexchange.sync;

/**
 * Binds the active {@link PatientSyncLog} row during an in-flight central FHIR patient send so
 * downstream code does not create duplicate log rows.
 */
public final class PatientSyncLogContext {
	
	private static final ThreadLocal<Long> ACTIVE_LOG_ID = new ThreadLocal<Long>();
	
	private PatientSyncLogContext() {
	}
	
	public static void bind(Long syncLogId) {
		ACTIVE_LOG_ID.set(syncLogId);
	}
	
	public static Long getActiveLogId() {
		return ACTIVE_LOG_ID.get();
	}
	
	public static boolean isActive() {
		return ACTIVE_LOG_ID.get() != null;
	}
	
	public static void clear() {
		ACTIVE_LOG_ID.remove();
	}
	
	public static void runWithLog(Long syncLogId, Runnable work) {
		bind(syncLogId);
		try {
			work.run();
		}
		finally {
			clear();
		}
	}
}
