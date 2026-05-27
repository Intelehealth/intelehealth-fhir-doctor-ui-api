package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("unsyncPatientService")
public class UnsyncPatientServiceImpl implements UnsyncPatientService {
	
	private static final Logger log = LoggerFactory.getLogger(UnsyncPatientServiceImpl.class);
	
	private static final int MAX_ERROR_MESSAGE_LENGTH = 512;
	
	@Autowired
	private UnsyncPatientRepository unsyncPatientRepository;
	
	@Autowired
	@Qualifier("ihmoduleIHMarkerService")
	private IHMarkerService ihMarkerService;
	
	@Override
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
	
	@Override
	@Transactional
	public void enqueue(String patientUuid) {
		recordForResync(patientUuid, FhirPatientSendGateService.SKIPPED_MESSAGE);
	}
	
	@Override
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
	
	@Override
	@Transactional
	public void finalizeUnsyncReplayAttempt(Long unsyncPatientRowId, int markerCursor, UnsyncPatientStatus status) {
		finalizeUnsyncReplayAttempt(unsyncPatientRowId, markerCursor, status, null);
	}
	
	private static String truncateErrorMessage(String errorMessage) {
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
