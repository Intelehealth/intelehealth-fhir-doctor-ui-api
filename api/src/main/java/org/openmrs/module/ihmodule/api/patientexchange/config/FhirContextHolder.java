package org.openmrs.module.ihmodule.api.patientexchange.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

/**
 * Reuses a single R4 FHIR context across the module.
 * <p>
 * Server validation is disabled so the client never issues {@code GET .../metadata} against
 * configured bases (e.g. OpenHIM {@code .../patient-create}) that are not FHIR capability roots.
 * Outbound writes use {@code POST} (e.g. transaction Bundle) per the client operation, not GET
 * metadata.
 * </p>
 */
public final class FhirContextHolder {
	
	public static final FhirContext R4;
	
	static {
		R4 = FhirContext.forR4();
		R4.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
	}
	
	private FhirContextHolder() {
	}
}
