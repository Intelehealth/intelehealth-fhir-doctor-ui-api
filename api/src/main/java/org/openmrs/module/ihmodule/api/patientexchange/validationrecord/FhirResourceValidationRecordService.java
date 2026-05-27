package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import org.hl7.fhir.r4.model.Bundle;

/**
 * Persists FHIR validation outcomes. Implemented by {@link FhirResourceValidationRecordServiceImpl}.
 */
public interface FhirResourceValidationRecordService {
	
	void record(String resourceType, Bundle bundle, ValidationOutcome outcome, String message);
	
	void recordValues(String resourceType, String resourceLogicalId, ValidationOutcome outcome, String message,
	        String payloadJson);
}
