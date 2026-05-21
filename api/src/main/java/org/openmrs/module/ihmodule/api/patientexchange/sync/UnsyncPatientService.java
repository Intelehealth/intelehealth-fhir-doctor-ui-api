package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records patients not sent to central FHIR while published-config sync is disabled.
 */
@Service("unsyncPatientService")
public class UnsyncPatientService {
	
	private static final Logger log = LoggerFactory.getLogger(UnsyncPatientService.class);
	
	@Autowired
	private UnsyncPatientRepository unsyncPatientRepository;
	
	@Autowired
	private IHMarkerService ihMarkerService;
	
	@Transactional
	public void enqueue(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return;
		}
		String uuid = patientUuid.trim();
		if (unsyncPatientRepository.findByPatientUuid(uuid) != null) {
			return;
		}
		UnsyncPatient row = new UnsyncPatient();
		row.setPatientUuid(uuid);
		row.setStatusEnum(UnsyncPatientStatus.PENDING);
		unsyncPatientRepository.save(row);
		log.info("Recorded unsynced patient while FHIR sync disabled: patientUuid={}", uuid);
	}
	
	/**
	 * After one unsync replay attempt: persist row status and advance {@code ih_marker.last_id} in
	 * a single transaction (required for scheduler threads that only open a Hibernate session).
	 */
	@Transactional
	public void finalizeUnsyncReplayAttempt(Long unsyncPatientRowId, int markerCursor, UnsyncPatientStatus status) {
		if (unsyncPatientRowId == null || status == null) {
			return;
		}
		ihMarkerService.updateUnsyncedPatientMarkerLastId(markerCursor);
		unsyncPatientRepository.updateStatus(unsyncPatientRowId, status);
		log.info("Unsync replay finalized: unsync_patient.id={} status={} ih_marker.last_id={}", unsyncPatientRowId, status,
		    markerCursor);
	}
}
