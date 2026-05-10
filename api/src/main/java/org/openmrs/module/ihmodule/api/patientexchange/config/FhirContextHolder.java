package org.openmrs.module.ihmodule.api.patientexchange.config;

import ca.uhn.fhir.context.FhirContext;

/**
 * Reuses a single R4 FHIR context across the module.
 */
public final class FhirContextHolder {
	
	public static final FhirContext R4 = FhirContext.forR4();
	
	private FhirContextHolder() {
	}
}
