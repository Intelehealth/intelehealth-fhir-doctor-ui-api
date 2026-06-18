package org.openmrs.module.ihmodule.api.patientexchange.capability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("unchecked")
public class IhmoduleCapabilityStatementServiceTest {
	
	@Test
	public void getInterfaceCapability_loadsTemplateAndPatchesMetadata() {
		IhmoduleCapabilityStatementService service = new IhmoduleCapabilityStatementService();
		Map<String, Object> doc = service.getInterfaceCapability();
		
		assertEquals("CapabilityStatement", doc.get("resourceType"));
		assertEquals("capability", doc.get("kind"));
		assertNotNull(doc.get("date"));
		
		Map<String, Object> software = (Map<String, Object>) doc.get("software");
		assertEquals("1.0.0-SNAPSHOT", software.get("version"));
		
		List<Map<String, Object>> endpoints = (List<Map<String, Object>>) doc.get("ihmoduleRestEndpoints");
		assertNotNull(endpoints);
		assertTrue(endpoints.stream().anyMatch(e -> "/ws/rest/v1/ihmodule/capability-statement".equals(e.get("path"))));
		
		Map<String, Object> implementation = (Map<String, Object>) doc.get("implementation");
		String url = String.valueOf(implementation.get("url"));
		if (url.contains("/openmrs")) {
			assertTrue(url.endsWith("/ws/rest/v1/ihmodule"));
		}
	}
	
	@Test
	public void normalizeBaseUrl_stripsTrailingSlash() {
		assertEquals("http://host/openmrs", IhmoduleCapabilityStatementService.normalizeBaseUrl("http://host/openmrs/"));
	}
}
