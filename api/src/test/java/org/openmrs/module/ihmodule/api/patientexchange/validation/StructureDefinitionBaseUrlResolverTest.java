package org.openmrs.module.ihmodule.api.patientexchange.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StructureDefinitionBaseUrlResolverTest {
	
	@Test
	public void resolve_shouldParseStructureDefinitionPropertyToBaseUrl() {
		assertEquals("http://intelehealth-central-fhir.mpower-social.com/fhir",
		    StructureDefinitionBaseUrlResolver.resolve(null));
	}
}
