package org.openmrs.module.ihmodule.api.patientexchange.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

/**
 * Guards the ihmodule.properties consolidation: runtime must still expose keys that previously
 * lived only in patientdataexchange-application.properties.
 */
public class ModuleClasspathPropertiesLoaderTest {
	
	@Test
	public void loadModuleProperties_shouldExposeBundledRuntimeKeys() {
		Properties props = ModuleClasspathPropertiesLoader.loadModuleProperties();
		assertNotNull(props);
		
		assertKeyPresent(props, "local.openmrs.url");
		assertKeyPresent(props, "opencr.openhim.url");
		assertKeyPresent(props, "opencr.openhim.mediator.password.basic.auth");
		assertKeyPresent(props, "opencr.openhim.clientid.password.basic.auth");
		assertKeyPresent(props, "intelehealth.fhir.patient.source.identifier.type.uuid");
		assertKeyPresent(props, "intelehealth.fhir.central.http.connect.timeout.ms");
		assertKeyPresent(props, "intelehealth.fhir.resource.identifier.name");
	}
	
	private static void assertKeyPresent(Properties props, String key) {
		assertTrue("Missing bundled property: " + key, props.containsKey(key) && !props.getProperty(key).trim().isEmpty());
	}
}
