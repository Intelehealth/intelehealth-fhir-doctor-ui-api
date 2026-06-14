package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpTimeoutSupport;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;

/**
 * Central FHIR patient push sync log lifecycle (aligned with SHR {@code ShrSyncLogService}).
 */
public interface PatientSyncLogService {
	
	PatientSyncLog createPending(String patientUuid);
	
	void markDeferred(PatientSyncLog row, String reason);
	
	void markSuccess(PatientSyncLog row, FhirResponse response);
	
	void markFailed(PatientSyncLog row, FhirResponse response, String failureReason, boolean permanent);
	
	void completePendingPush(PatientSyncLog pending, FhirResponse response);
	
	void tryPushPendingRow(PatientSyncLog pending, PatientSyncPushContract pushContract);
	
	int runSyncCycle(int limitPerCycle, PatientSyncPushContract pushContract);
	
	static boolean isSuccessfulCentralWrite(FhirResponse response) {
		if (response == null || response.getStatusCode() == null) {
			return false;
		}
		if (FhirPatientSendGateService.SKIPPED_STATUS.equals(response.getStatusCode())) {
			return false;
		}
		return response.getStatusCode().startsWith("2");
	}
	
	static String formatSyncFailureMessage(FhirResponse response) {
		if (response == null) {
			return "Central FHIR patient sync returned no response";
		}
		String message = StringUtils.trimToNull(response.getMessage());
		if (HttpTimeoutSupport.TIMEOUT_STATUS_CODE.equals(response.getStatusCode()) && message != null) {
			return message;
		}
		String status = StringUtils.defaultString(response.getStatusCode(), "unknown");
		if (message != null) {
			return "status=" + status + ": " + message;
		}
		return "status=" + status;
	}
	
	static String formatSyncFailureMessage(Throwable throwable) {
		return HttpTimeoutSupport.formatFailureMessage(throwable, HttpWebClient.getConnectTimeoutMs(),
		    HttpWebClient.getReadTimeoutMs());
	}
	
	static Integer parseHttpStatusCode(FhirResponse response) {
		if (response == null || StringUtils.isBlank(response.getStatusCode())) {
			return null;
		}
		try {
			return Integer.valueOf(response.getStatusCode().trim());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}
}
