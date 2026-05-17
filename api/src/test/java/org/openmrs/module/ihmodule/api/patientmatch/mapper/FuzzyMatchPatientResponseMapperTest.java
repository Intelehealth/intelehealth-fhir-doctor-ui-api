package org.openmrs.module.ihmodule.api.patientmatch.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.openmrs.PersonAddress;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.validation.StructureDefinitionBaseUrlResolver;
import org.openmrs.module.ihmodule.api.patientmatch.dto.AddressAPIDTO;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;

public class FuzzyMatchPatientResponseMapperTest {
	
	private static final String SD_BASE = "http://intelehealth-central-fhir.mpower-social.com/fhir";
	
	private final FuzzyMatchPatientResponseMapper mapper = new FuzzyMatchPatientResponseMapper();
	
	@Test
	public void structureDefinitionBaseUrlResolver_shouldReadBundledProperty() {
		assertEquals(SD_BASE, StructureDefinitionBaseUrlResolver.resolve(null));
	}
	
	@Test
	public void applyProfileExtensionsFromPersonAttributes_shouldUseStructureDefinitionUrls() {
		PersonAttribute caste = personAttribute("Caste", "OBC");
		PersonAttribute economicStatus = personAttribute("Economic Status", "BPL");
		PersonAttribute educationLevel = personAttribute("Education Level", "Graduate");
		PersonAttribute nationalId = personAttribute("NationalID", "ID-123");
		PersonAttribute telephone = personAttribute("Telephone Number", "+919646656523");
		PersonAttribute emergencyNumber = personAttribute("Emergency Contact Number", "+919464659139");
		PersonAttribute emergencyType = personAttribute("Emergency Contact Type", "Spouse");
		
		Patient patient = new Patient();
		mapper.applyProfileExtensionsFromPersonAttributes(patient,
		    Arrays.asList(caste, economicStatus, educationLevel, nationalId, telephone, emergencyNumber, emergencyType));
		
		assertEquals(5, patient.getExtension().size());
		assertExtension(patient.getExtension().get(0), "Caste", "OBC");
		assertExtension(patient.getExtension().get(1), "Economic-Status", "BPL");
		assertExtension(patient.getExtension().get(2), "Education-Level", "Graduate");
		assertExtension(patient.getExtension().get(3), "NationalID", "ID-123");
		assertExtension(patient.getExtension().get(4), "Emergency-Contact-Type", "Spouse");
	}
	
	private static void assertExtension(Extension extension, String suffix, String value) {
		assertEquals(SD_BASE + "/StructureDefinition/" + suffix, extension.getUrl());
		assertEquals(value, extension.getValue().primitiveValue());
	}
	
	@Test
	public void mapPersonAddress_shouldMapStateAndPostalCodeSeparatelyFromLines() throws Exception {
		PersonAddress personAddress = new PersonAddress();
		personAddress.setAddress1("Geyr");
		personAddress.setAddress6("656564");
		personAddress.setStateProvince("Arunachal Pradesh");
		personAddress.setCountyDistrict("NA");
		personAddress.setCountry("India");
		
		AddressAPIDTO dto = invokeMapPersonAddress(personAddress);
		
		assertNotNull(dto);
		assertEquals("Arunachal Pradesh", dto.getStateProvince());
		assertEquals("656564", dto.getPostalCode());
		
		Patient patient = new Patient();
		invokeApplyStructuredAddress(patient, dto, null);
		
		Address address = patient.getAddress().get(0);
		assertEquals(1, address.getLine().size());
		assertEquals("Geyr", address.getLine().get(0).getValue());
		assertEquals("Arunachal Pradesh", address.getState());
		assertEquals("656564", address.getPostalCode());
		assertEquals("India", address.getCountry());
		assertEquals("NA", address.getDistrict());
	}
	
	@Test
	public void applyStructuredAddress_shouldMapAddressApiDtoToFhirAddress() throws Exception {
		AddressAPIDTO dto = new AddressAPIDTO();
		dto.setAddress1("Line 1");
		dto.setAddress2("Line 2");
		dto.setCityVillage("Dhaka");
		dto.setStateProvince("Arunachal Pradesh");
		dto.setPostalCode("1207");
		dto.setCountry("Bangladesh");
		dto.setCountyDistrict("Central");
		
		Patient patient = new Patient();
		invokeApplyStructuredAddress(patient, dto, null);
		
		Address address = patient.getAddress().get(0);
		assertEquals("Line 1", address.getLine().get(0).getValue());
		assertEquals("Line 2", address.getLine().get(1).getValue());
		assertEquals("Dhaka", address.getCity());
		assertEquals("Arunachal Pradesh", address.getState());
		assertEquals("1207", address.getPostalCode());
		assertEquals("Bangladesh", address.getCountry());
		assertEquals("Central", address.getDistrict());
	}
	
	@Test
	public void applyProfileExtensionsFromPersonAttributes_shouldEmitAllSupportedOpenMrsAttributes() {
		Patient patient = new Patient();
		mapper.applyProfileExtensionsFromPersonAttributes(
		    patient,
		    Arrays.asList(personAttribute("Caste", "ST"), personAttribute("Economic Status", "BPL"),
		        personAttribute("Education Level", "Graduate"), personAttribute("NationalID", "ID-123"),
		        personAttribute("Telephone Number", "+919646656523"),
		        personAttribute("Emergency Contact Number", "+919464659139"),
		        personAttribute("Emergency Contact Type", "Spouse"), personAttribute("occupation", "Cricket")));
		
		assertEquals(6, patient.getExtension().size());
		assertExtension(patient.getExtension().get(0), "Caste", "ST");
		assertExtension(patient.getExtension().get(1), "Economic-Status", "BPL");
		assertExtension(patient.getExtension().get(2), "Education-Level", "Graduate");
		assertExtension(patient.getExtension().get(3), "NationalID", "ID-123");
		assertExtension(patient.getExtension().get(4), "Emergency-Contact-Type", "Spouse");
		assertExtension(patient.getExtension().get(5), "occupation", "Cricket");
	}
	
	@Test
	public void enrich_shouldUseFallbackTextAddressWhenStructuredAddressMissing() {
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setAddress("Dhaka, Bangladesh");
		
		Patient patient = new Patient();
		mapper.enrich(patient, candidate);
		
		assertEquals(1, patient.getAddress().size());
		assertEquals("Dhaka, Bangladesh", patient.getAddress().get(0).getText());
	}
	
	private static PersonAttribute personAttribute(String name, String value) {
		PersonAttribute attribute = new PersonAttribute();
		attribute.setName(name);
		attribute.setValue(value);
		return attribute;
	}
	
	private static AddressAPIDTO invokeMapPersonAddress(PersonAddress personAddress) throws Exception {
		Method method = FuzzyMatchPatientResponseMapper.class.getDeclaredMethod("mapPersonAddress", PersonAddress.class);
		method.setAccessible(true);
		return (AddressAPIDTO) method.invoke(null, personAddress);
	}
	
	private static void invokeApplyStructuredAddress(Patient patient, AddressAPIDTO dto, String fallbackText)
	        throws Exception {
		Method method = FuzzyMatchPatientResponseMapper.class.getDeclaredMethod("applyStructuredAddress", Patient.class,
		    AddressAPIDTO.class, String.class);
		method.setAccessible(true);
		method.invoke(new FuzzyMatchPatientResponseMapper(), patient, dto, fallbackText);
	}
}
