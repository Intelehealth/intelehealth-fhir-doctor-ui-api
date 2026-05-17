package org.openmrs.module.ihmodule.api.patientexchange.telecom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.telecom.PatientTelecomMappingUtil.TelecomValues;

public class PatientTelecomMappingUtilTest {
	
	@Test
	public void applyRankedPhoneTelecom_shouldMapTelephoneAndEmergencyNumbers() {
		Patient patient = new Patient();
		List<PersonAttribute> attributes = Arrays.asList(attribute("Telephone Number", "+913649564994"),
		    attribute("Emergency Contact Number", "+913649564995"));
		
		PatientTelecomMappingUtil.applyRankedPhoneTelecom(patient, attributes);
		
		assertEquals(2, patient.getTelecom().size());
		assertTelecom(patient.getTelecom().get(0), "+913649564994", ContactPoint.ContactPointUse.MOBILE, 1);
		assertTelecom(patient.getTelecom().get(1), "+913649564995", ContactPoint.ContactPointUse.HOME, 2);
	}
	
	@Test
	public void extractRankedPhoneTelecom_shouldPreferRankedEntries() {
		Patient patient = new Patient();
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+913649564995")
		        .setUse(ContactPoint.ContactPointUse.MOBILE).setRank(2);
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+913649564994")
		        .setUse(ContactPoint.ContactPointUse.HOME).setRank(1);
		
		TelecomValues values = PatientTelecomMappingUtil.extractRankedPhoneTelecom(patient);
		
		assertEquals("+913649564994", values.getTelephoneNumber());
		assertEquals("+913649564995", values.getEmergencyContactNumber());
	}
	
	@Test
	public void extractRankedPhoneTelecom_shouldFallBackToUseWhenRankMissing() {
		Patient patient = new Patient();
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+913649564994")
		        .setUse(ContactPoint.ContactPointUse.MOBILE);
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+913649564995")
		        .setUse(ContactPoint.ContactPointUse.HOME);
		
		TelecomValues values = PatientTelecomMappingUtil.extractRankedPhoneTelecom(patient);
		
		assertEquals("+913649564994", values.getTelephoneNumber());
		assertEquals("+913649564995", values.getEmergencyContactNumber());
	}
	
	@Test
	public void applyRankedPhoneTelecom_shouldSkipNumbersWithFewerThanTenDigits() {
		Patient patient = new Patient();
		List<PersonAttribute> attributes = Arrays.asList(attribute("Telephone Number", "123456789"),
		    attribute("Emergency Contact Number", "987654321"));
		
		PatientTelecomMappingUtil.applyRankedPhoneTelecom(patient, attributes);
		
		assertTrue(patient.getTelecom().isEmpty());
	}
	
	@Test
	public void applyRankedPhoneTelecom_shouldIncludeOnlyValidNumbersWhenMixed() {
		Patient patient = new Patient();
		List<PersonAttribute> attributes = Arrays.asList(attribute("Telephone Number", "12345"),
		    attribute("Emergency Contact Number", "+913649564995"));
		
		PatientTelecomMappingUtil.applyRankedPhoneTelecom(patient, attributes);
		
		assertEquals(1, patient.getTelecom().size());
		assertTelecom(patient.getTelecom().get(0), "+913649564995", ContactPoint.ContactPointUse.HOME, 2);
	}
	
	@Test
	public void isValidPhoneNumber_shouldCountDigitsIgnoringFormatting() {
		assertFalse(PatientTelecomMappingUtil.isValidPhoneNumber("123456789"));
		assertTrue(PatientTelecomMappingUtil.isValidPhoneNumber("1234567890"));
		assertTrue(PatientTelecomMappingUtil.isValidPhoneNumber("+91 36495-64994"));
	}
	
	@Test
	public void removeInvalidPhoneTelecom_shouldDropShortPhoneEntriesOnly() {
		Patient patient = new Patient();
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("12345")
		        .setUse(ContactPoint.ContactPointUse.MOBILE).setRank(1);
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("+913649564995")
		        .setUse(ContactPoint.ContactPointUse.HOME).setRank(2);
		patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.EMAIL).setValue("a@b.com");
		
		PatientTelecomMappingUtil.removeInvalidPhoneTelecom(patient);
		
		assertEquals(2, patient.getTelecom().size());
		assertEquals("+913649564995", patient.getTelecom().get(0).getValue());
		assertEquals(ContactPoint.ContactPointSystem.EMAIL, patient.getTelecom().get(1).getSystem());
	}
	
	@Test
	public void extractRankedPhoneTelecom_shouldReturnNullsWhenNoPhoneTelecomExists() {
		TelecomValues values = PatientTelecomMappingUtil.extractRankedPhoneTelecom(new Patient());
		
		assertNull(values.getTelephoneNumber());
		assertNull(values.getEmergencyContactNumber());
	}
	
	private static PersonAttribute attribute(String name, String value) {
		PersonAttribute attribute = new PersonAttribute();
		attribute.setName(name);
		attribute.setValue(value);
		return attribute;
	}
	
	private static void assertTelecom(ContactPoint contactPoint, String value, ContactPoint.ContactPointUse use, int rank) {
		assertEquals(ContactPoint.ContactPointSystem.PHONE, contactPoint.getSystem());
		assertEquals(value, contactPoint.getValue());
		assertEquals(use, contactPoint.getUse());
		assertEquals(rank, contactPoint.getRank());
	}
}
