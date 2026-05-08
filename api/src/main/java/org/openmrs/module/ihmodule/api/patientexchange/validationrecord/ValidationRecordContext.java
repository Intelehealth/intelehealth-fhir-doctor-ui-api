package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

/**
 * Thread-local context for the currently processed resource validation details.
 * Used to pass payload and failure reason from validator flow to audit aspects
 * without tight coupling between modules.
 */
public final class ValidationRecordContext {

	private static final ThreadLocal<String> PAYLOAD_JSON = new ThreadLocal<>();
	private static final ThreadLocal<String> FAILURE_REASON = new ThreadLocal<>();

	private ValidationRecordContext() {
	}

	public static void setPayloadJson(String payloadJson) {
		PAYLOAD_JSON.set(payloadJson);
	}

	public static String getPayloadJson() {
		return PAYLOAD_JSON.get();
	}

	public static void setFailureReason(String failureReason) {
		FAILURE_REASON.set(failureReason);
	}

	public static String getFailureReason() {
		return FAILURE_REASON.get();
	}

	public static void clear() {
		PAYLOAD_JSON.remove();
		FAILURE_REASON.remove();
	}
}
