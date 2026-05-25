package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpTimeoutSupport;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists patients that could not be synced to central FHIR for later resync.
 */
@Service("unsyncPatientService")
public class UnsyncPatientService {
	
	private static final Logger log = LoggerFactory.getLogger(UnsyncPatientService.class);
	
	private static final int MAX_ERROR_MESSAGE_LENGTH = 512;
	
	@Autowired
	private UnsyncPatientRepository unsyncPatientRepository;
	
	@Autowired
	private IHMarkerService ihMarkerService;
	
	/**
	 * Records a patient for later resync (sync disabled, central send failure, missing MPI, etc.).
	 */
	@Transactional
	public void recordForResync(String patientUuid, String errorMessage) {
		if (StringUtils.isBlank(patientUuid)) {
			return;
		}
		String uuid = patientUuid.trim();
		String message = truncateErrorMessage(errorMessage);
		UnsyncPatient existing = unsyncPatientRepository.findLatestRetryableByPatientUuid(uuid);
		if (existing != null) {
			unsyncPatientRepository.updateStatusAndErrorMessage(existing.getId(), UnsyncPatientStatus.PENDING, message);
			log.info("Updated unsync_patient row for resync: patientUuid={} id={}", uuid, existing.getId());
			return;
		}
		UnsyncPatient row = new UnsyncPatient();
		row.setPatientUuid(uuid);
		row.setStatusEnum(UnsyncPatientStatus.PENDING);
		row.setErrorMessage(message);
		unsyncPatientRepository.save(row);
		log.info("Recorded unsynced patient for resync: patientUuid={}", uuid);
	}
	
	@Transactional
	public void enqueue(String patientUuid) {
		recordForResync(patientUuid, FhirPatientSendGateService.SKIPPED_MESSAGE);
	}
	
	/**
	 * After one unsync replay attempt: persist row status; advance marker only on success.
	 */
	@Transactional
	public void finalizeUnsyncReplayAttempt(Long unsyncPatientRowId, int markerCursor, UnsyncPatientStatus status,
	        String errorMessage) {
		if (unsyncPatientRowId == null || status == null) {
			return;
		}
		unsyncPatientRepository.updateStatusAndErrorMessage(unsyncPatientRowId, status, truncateErrorMessage(errorMessage));
		if (status == UnsyncPatientStatus.COMPLETED) {
			ihMarkerService.updateUnsyncedPatientMarkerLastId(markerCursor);
		}
		log.info("Unsync replay finalized: unsync_patient.id={} status={} advanceMarker={}", unsyncPatientRowId, status,
		    status == UnsyncPatientStatus.COMPLETED);
	}
	
	@Transactional
	public void finalizeUnsyncReplayAttempt(Long unsyncPatientRowId, int markerCursor, UnsyncPatientStatus status) {
		finalizeUnsyncReplayAttempt(unsyncPatientRowId, markerCursor, status, null);
	}
	
	public static boolean isSuccessfulCentralWrite(FhirResponse response) {
		if (response == null || response.getStatusCode() == null) {
			return false;
		}
		if (FhirPatientSendGateService.SKIPPED_STATUS.equals(response.getStatusCode())) {
			return false;
		}
		return response.getStatusCode().startsWith("2");
	}
	
	public static String formatSyncFailureMessage(FhirResponse response) {
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
	
	public static String formatSyncFailureMessage(Throwable throwable) {
		return HttpTimeoutSupport.formatFailureMessage(throwable, HttpWebClient.getConnectTimeoutMs(),
		    HttpWebClient.getReadTimeoutMs());
	}
	
	public static String truncateErrorMessage(String errorMessage) {
		if (errorMessage == null) {
			return null;
		}
		String trimmed = errorMessage.trim();
		if (trimmed.length() <= MAX_ERROR_MESSAGE_LENGTH) {
			return trimmed;
		}
		return trimmed.substring(0, MAX_ERROR_MESSAGE_LENGTH);
	}
}
