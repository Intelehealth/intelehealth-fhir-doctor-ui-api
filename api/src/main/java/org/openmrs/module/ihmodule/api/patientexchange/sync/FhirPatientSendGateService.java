package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.parser.DataFormatException;

/**
 * Gates {@link org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR#send} for
 * Patient resources using the published config API ({@code fhir_module.fhir}); records skipped
 * patients in {@code unsync_patient}.
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
	private UnsyncPatientService unsyncPatientService;
	
	public FhirResponse handlePatientSend(String patientUuid, FhirPatientSendExecutor executor) throws ParseException,
	        DataFormatException, JSONException, ConfigurationException, IOException {
		if (StringUtils.isBlank(patientUuid)) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		String uuid = patientUuid.trim();
		log.error("publishedConfigFhirSyncGateService.isFhirSyncEnabled():"
		        + publishedConfigFhirSyncGateService.isFhirSyncEnabled());
		if (!publishedConfigFhirSyncGateService.isFhirSyncEnabled()) {
			unsyncPatientService.enqueue(uuid);
			return buildSkippedResponse(uuid);
		}
		return executor.send(uuid);
	}
	
	private static FhirResponse buildSkippedResponse(String patientUuid) {
		log.error("FHIR sync disabled; queued patientUuid={} in unsync_patient", patientUuid);
		FhirResponse response = new FhirResponse();
		response.setStatusCode(SKIPPED_STATUS);
		response.setMessage(SKIPPED_MESSAGE);
		response.setResponse(patientUuid);
		return response;
	}
}
