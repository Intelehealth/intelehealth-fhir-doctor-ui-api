package org.openmrs.module.ihmodule.api.patientmatch.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;

public class FhirPatientMatchRequestParserTest {
	
	private final FhirPatientMatchRequestParser parser = new FhirPatientMatchRequestParser();
	
	@Test
	public void parse_shouldExtractPatientFieldsFromParameters() {
		String body = "{" + "\"resourceType\":\"Parameters\"," + "\"parameter\":[" + "{\"name\":\"resource\",\"resource\":{"
		        + "\"resourceType\":\"Patient\","
		        + "\"identifier\":[{\"system\":\"http://hospital.example.org/mrn\",\"value\":\"10001A\"}],"
		        + "\"name\":[{\"family\":\"Smith\",\"given\":[\"John\"]}]," + "\"birthDate\":\"1990-01-01\","
		        + "\"gender\":\"male\"," + "\"telecom\":[{\"system\":\"phone\",\"value\":\"01712345678\"}],"
		        + "\"address\":[{\"text\":\"Dhaka\"}]" + "}}," + "{\"name\":\"resourceType\",\"valueString\":\"Patient\"},"
		        + "{\"name\":\"count\",\"valueInteger\":5}," + "{\"name\":\"offset\",\"valueInteger\":10},"
		        + "{\"name\":\"onlyCertainMatches\",\"valueBoolean\":true}" + "]" + "}";
		
		FuzzyPatientMatchRequest request = parser.parse(body);
		
		assertEquals("Patient", request.getResourceType());
		assertEquals("http://hospital.example.org/mrn", request.getIdentifierSystem());
		assertEquals("10001A", request.getIdentifier());
		assertEquals("John", request.getGivenName());
		assertEquals("Smith", request.getFamilyName());
		assertEquals("John Smith", request.getName());
		assertEquals(LocalDate.parse("1990-01-01"), request.getBirthDate());
		assertEquals("male", request.getGender());
		assertEquals("01712345678", request.getPhone());
		assertEquals("Dhaka", request.getAddress());
		assertEquals(5, request.getCount());
		assertEquals(10, request.getOffset());
		assertTrue(request.isOnlyCertainMatches());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void parse_shouldRejectParametersWithoutSearchFields() {
		parser.parse("{\"resourceType\":\"Parameters\",\"parameter\":[{\"name\":\"resource\",\"resource\":{\"resourceType\":\"Patient\"}}]}");
	}
	
	@Test
	public void parse_shouldSupportDirectPatientBodies() {
		FuzzyPatientMatchRequest request = parser.parse("{" + "\"resourceType\":\"Patient\","
		        + "\"name\":[{\"family\":\"Smith\",\"given\":[\"John\"]}],"
		        + "\"telecom\":[{\"system\":\"phone\",\"value\":\"01712345678\"}]" + "}");
		
		assertEquals("Patient", request.getResourceType());
		assertEquals("John", request.getGivenName());
		assertEquals("Smith", request.getFamilyName());
		assertEquals("John Smith", request.getName());
		assertEquals("01712345678", request.getPhone());
		assertFalse(request.isOnlyCertainMatches());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void parse_shouldRejectUnsupportedParametersResourceType() {
		parser.parse("{"
		        + "\"resourceType\":\"Parameters\","
		        + "\"parameter\":["
		        + "{\"name\":\"resource\",\"resource\":{\"resourceType\":\"Patient\",\"name\":[{\"text\":\"John Smith\"}]}},"
		        + "{\"name\":\"resourceType\",\"valueString\":\"Observation\"}" + "]}");
	}
}
