package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
		unsyncPatientRepository.save(row);
		log.info("Recorded unsynced patient while FHIR sync disabled: patientUuid={}", uuid);
	}
}
