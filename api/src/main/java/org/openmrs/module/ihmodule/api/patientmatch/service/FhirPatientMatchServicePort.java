package org.openmrs.module.ihmodule.api.patientmatch.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;

import ca.uhn.fhir.context.FhirContext;

/**
 * Injection-safe contract for fuzzy patient match operations.
 */
public interface FhirPatientMatchServicePort {
	
	Bundle match(String body);
	
	OperationOutcome errorOutcome(String code, String diagnostics);
	
	String encodeResource(FhirContext fhirContext, org.hl7.fhir.r4.model.Resource resource);
	
}

