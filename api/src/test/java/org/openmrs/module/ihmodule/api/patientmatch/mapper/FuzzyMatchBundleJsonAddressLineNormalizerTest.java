package org.openmrs.module.ihmodule.api.patientmatch.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientmatch.service.FhirPatientMatchService;

public class FuzzyMatchBundleJsonAddressLineNormalizerTest {
	
	@Test
	public void normalizeEncodedBundle_shouldForceTwoLineElementsWhenHapiOmitsEmptyOnes() {
		Patient patient = new Patient();
		Address address = patient.addAddress();
		address.addLine("null");
		address.addLine("");
		address.setPostalCode("1216");
		address.setCountry("Bangladesh");
		
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.addEntry().setResource(patient);
		
		String encoded = new FhirPatientMatchService().encodeResource(FhirContextHolder.R4, bundle);
		assertTrue(encoded.contains("\"line\" : [ \"\", \"\" ]"));
		assertTrue(encoded.contains("\"postalCode\" : \"1216\""));
	}
	
	@Test
	public void normalizeEncodedBundle_shouldPadSingleLineToTwoElements() {
		Patient patient = new Patient();
		Address address = patient.addAddress();
		address.addLine("Test");
		address.setCountry("India");
		
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.addEntry().setResource(patient);
		
		String encoded = new FhirPatientMatchService().encodeResource(FhirContextHolder.R4, bundle);
		assertTrue(encoded.contains("\"line\" : [ \"Test\", \"\" ]"));
	}
	
	@Test
	public void normalizeEncodedBundle_shouldAddLineArrayWhenOnlyCountryPresent() {
		Patient patient = new Patient();
		Address address = patient.addAddress();
		address.setCountry("India");
		FuzzyMatchAddressLineSupport.ensureTwoAddressLines(address);
		
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.addEntry().setResource(patient);
		
		String encoded = new FhirPatientMatchService().encodeResource(FhirContextHolder.R4, bundle);
		assertTrue(encoded.contains("\"line\" : [ \"\", \"\" ]"));
		assertEquals(1, encoded.split("\"line\" : \\[ \"\", \"\" \\]", -1).length - 1);
	}
}
