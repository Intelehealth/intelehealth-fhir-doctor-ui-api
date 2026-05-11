package org.openmrs.module.ihmodule.api.patientmatch.config;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FuzzyPatientMatchConfigServiceTest {
	
	@Test
	public void getConfig_shouldLoadBundledDefaults() {
		FuzzyPatientMatchConfig config = new FuzzyPatientMatchConfigService().getConfig();
		
		assertTrue(config.isEnabled());
		assertTrue(config.getThreshold() > 0);
		assertTrue(config.getMaxCandidates() >= 50);
		assertTrue(config.isFieldEnabled("name"));
		assertTrue(config.getFieldWeight("name") > 0.0d);
	}
}
