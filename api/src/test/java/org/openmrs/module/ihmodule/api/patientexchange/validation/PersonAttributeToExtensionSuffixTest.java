package org.openmrs.module.ihmodule.api.patientexchange.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PersonAttributeToExtensionSuffixTest {
	
	@Test
	public void map_shouldMapOpenMrsProfileAttributesToStructureDefinitionSuffixes() {
		assertEquals("Caste", PersonAttributeToExtensionSuffix.map("Caste"));
		assertEquals("Economic-Status", PersonAttributeToExtensionSuffix.map("Economic Status"));
		assertEquals("Education-Level", PersonAttributeToExtensionSuffix.map("Education Level"));
		assertEquals("NationalID", PersonAttributeToExtensionSuffix.map("NationalID"));
		assertEquals("NationalID", PersonAttributeToExtensionSuffix.map("National ID"));
		assertEquals("Emergency-Contact-Type", PersonAttributeToExtensionSuffix.map("Emergency Contact Type"));
	}
	
	@Test
	public void map_shouldReturnNullForPhoneAttributes() {
		assertNull(PersonAttributeToExtensionSuffix.map("Telephone Number"));
		assertNull(PersonAttributeToExtensionSuffix.map("Emergency Contact Number"));
	}
}
