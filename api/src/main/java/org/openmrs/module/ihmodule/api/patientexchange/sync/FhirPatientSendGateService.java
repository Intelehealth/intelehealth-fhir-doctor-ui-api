package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.service.LocalPatientMpiUpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.parser.DataFormatException;

/**
 * Gates {@link org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR#send} for
 * Patient resources using the published config API ({@code fhir_module.fhir}); logs every push
 * attempt in {@code patient_sync_log}.
 */
@Service("fhirPatientSendGateService")
public class FhirPatientSendGateService {
	
	private static final Logger log = LoggerFactory.getLogger(FhirPatientSendGateService.class);
	
	/** Non-2xx so schedulers do not treat disabled sync as a successful central write. */
	public static final String SKIPPED_STATUS = "FHIR_SYNC_DISABLED";
	
	public static final String SKIPPED_MESSAGE = "FHIR sync disabled by published config (fhir_module.fhir=false)";
	
	@Autowired
	private PublishedConfigFhirSyncGateService publishedConfigFhirSyncGateService;
	
	@Autowired
	private PatientSyncLogService patientSyncLogService;
	
	@Autowired
	private LocalPatientMpiUpdateService localPatientMpiUpdateService;
	
	public FhirResponse handlePatientSend(String patientUuid, FhirPatientSendExecutor executor) throws ParseException,
	        DataFormatException, ConfigurationException, IOException {
		if (StringUtils.isBlank(patientUuid)) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		String uuid = patientUuid.trim();
		if (localPatientMpiUpdateService.localPatientHasMpiAndSourcePatientId(uuid)) {
			log.debug("Skipping patient_sync_log for patientUuid={} because MPI and Source Patient Id are already present",
			    uuid);
			if (!publishedConfigFhirSyncGateService.isFhirSyncEnabled()) {
				return buildSkippedResponse(uuid);
			}
			return executor.send(uuid);
		}
		PatientSyncLog pending = patientSyncLogService.createPending(uuid);
		if (!publishedConfigFhirSyncGateService.isFhirSyncEnabled()) {
			patientSyncLogService.markDeferred(pending, SKIPPED_MESSAGE);
			return buildSkippedResponse(uuid);
		}
		try {
			PatientSyncLogContext.bind(pending.getId());
			FhirResponse response = executor.send(uuid);
			patientSyncLogService.completePendingPush(pending, response);
			return response;
		}
		catch (Exception ex) {
			patientSyncLogService.markFailed(pending, null, PatientSyncLogService.formatSyncFailureMessage(ex), false);
			throw ex;
		}
		finally {
			PatientSyncLogContext.clear();
		}
	}
	
	private static FhirResponse buildSkippedResponse(String patientUuid) {
		log.info("FHIR sync disabled; logged deferred patient sync for patientUuid={}", patientUuid);
		FhirResponse response = new FhirResponse();
		response.setStatusCode(SKIPPED_STATUS);
		response.setMessage(SKIPPED_MESSAGE);
		response.setResponse(patientUuid);
		return response;
	}
}
