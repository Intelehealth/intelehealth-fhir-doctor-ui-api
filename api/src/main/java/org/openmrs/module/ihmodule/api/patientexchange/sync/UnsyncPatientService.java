package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpTimeoutSupport;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;

/**
 * Persists patients that could not be synced to central FHIR for later resync. Implemented by
 * {@link UnsyncPatientServiceImpl}.
 */
public interface UnsyncPatientService {
	
	void recordForResync(String patientUuid, String errorMessage);
	
	void enqueue(String patientUuid);
	
	void finalizeUnsyncReplayAttempt(Long unsyncPatientRowId, int markerCursor, UnsyncPatientStatus status,
	        String errorMessage);
	
	void finalizeUnsyncReplayAttempt(Long unsyncPatientRowId, int markerCursor, UnsyncPatientStatus status);
	
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
	
}
