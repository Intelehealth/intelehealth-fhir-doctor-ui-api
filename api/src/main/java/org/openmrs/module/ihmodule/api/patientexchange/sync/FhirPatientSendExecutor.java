package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.io.IOException;
import java.text.ParseException;

import org.json.JSONException;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.parser.DataFormatException;

/**
 * Performs the actual central FHIR patient send (used by {@link FhirPatientSendGateService}).
 */
@FunctionalInterface
public interface FhirPatientSendExecutor {
	
	FhirResponse send(String patientUuid) throws ParseException, DataFormatException, JSONException, ConfigurationException,
	        IOException;
}
