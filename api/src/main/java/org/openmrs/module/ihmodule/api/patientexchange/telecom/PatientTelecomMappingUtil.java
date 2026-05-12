package org.openmrs.module.ihmodule.api.patientexchange.telecom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;

public final class PatientTelecomMappingUtil {
	
	private static final String ATTR_TELEPHONE_NUMBER = "telephonenumber";
	
	private static final String ATTR_PHONE_NUMBER = "phonenumber";
	
	private static final String ATTR_EMERGENCY_CONTACT_NUMBER = "emergencycontactnumber";
	
	private PatientTelecomMappingUtil() {
	}
	
	public static void applyRankedPhoneTelecom(Patient patient, List<PersonAttribute> attributes) {
		if (patient == null) {
			return;
		}
		String telephoneNumber = firstAttributeValue(attributes, ATTR_TELEPHONE_NUMBER, ATTR_PHONE_NUMBER);
		String emergencyContactNumber = firstAttributeValue(attributes, ATTR_EMERGENCY_CONTACT_NUMBER);
		
		List<ContactPoint> telecom = new ArrayList<>();
		if (patient.hasTelecom()) {
			for (ContactPoint existing : patient.getTelecom()) {
				if (existing == null || isPhoneTelecom(existing)) {
					continue;
				}
				telecom.add(existing.copy());
			}
		}
		if (StringUtils.isNotBlank(telephoneNumber)) {
			telecom.add(0, buildPhoneContact(telephoneNumber.trim(), ContactPointUse.MOBILE, 1));
		}
		if (StringUtils.isNotBlank(emergencyContactNumber)) {
			telecom.add(Math.min(telecom.size(), 1), buildPhoneContact(emergencyContactNumber.trim(), ContactPointUse.HOME, 2));
		}
		patient.setTelecom(telecom);
	}
	
	public static TelecomValues extractRankedPhoneTelecom(Patient patient) {
		List<ContactPoint> phoneTelecom = phoneTelecom(patient);
		boolean[] used = new boolean[phoneTelecom.size()];
		
		String telephoneNumber = valueOf(markFirstMatch(phoneTelecom, used, cp -> cp.hasRank() && cp.getRank() == 1));
		if (telephoneNumber == null) {
			telephoneNumber = valueOf(markFirstMatch(phoneTelecom, used, cp -> cp.getUse() == ContactPointUse.MOBILE));
		}
		if (telephoneNumber == null) {
			telephoneNumber = valueOf(markFirstMatch(phoneTelecom, used, cp -> true));
		}
		
		String emergencyContactNumber = valueOf(markFirstMatch(phoneTelecom, used, cp -> cp.hasRank() && cp.getRank() == 2));
		if (emergencyContactNumber == null) {
			emergencyContactNumber = valueOf(markFirstMatch(phoneTelecom, used, cp -> cp.getUse() == ContactPointUse.HOME));
		}
		if (emergencyContactNumber == null) {
			emergencyContactNumber = valueOf(markFirstMatch(phoneTelecom, used, cp -> true));
		}
		
		return new TelecomValues(telephoneNumber, emergencyContactNumber);
	}
	
	private static ContactPoint buildPhoneContact(String value, ContactPointUse use, int rank) {
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPointSystem.PHONE);
		contactPoint.setValue(value);
		contactPoint.setUse(use);
		contactPoint.setRank(rank);
		return contactPoint;
	}
	
	private static ContactPoint markFirstMatch(List<ContactPoint> contacts, boolean[] used, Predicate<ContactPoint> predicate) {
		for (int i = 0; i < contacts.size(); i++) {
			if (used[i]) {
				continue;
			}
			ContactPoint contactPoint = contacts.get(i);
			if (predicate.test(contactPoint)) {
				used[i] = true;
				return contactPoint;
			}
		}
		return null;
	}
	
	private static List<ContactPoint> phoneTelecom(Patient patient) {
		List<ContactPoint> contacts = new ArrayList<>();
		if (patient == null || patient.getTelecom() == null) {
			return contacts;
		}
		for (ContactPoint contactPoint : patient.getTelecom()) {
			if (contactPoint == null || !isPhoneTelecom(contactPoint) || StringUtils.isBlank(contactPoint.getValue())) {
				continue;
			}
			contacts.add(contactPoint);
		}
		return contacts;
	}
	
	private static boolean isPhoneTelecom(ContactPoint contactPoint) {
		return contactPoint != null && (!contactPoint.hasSystem() || contactPoint.getSystem() == ContactPointSystem.PHONE);
	}
	
	private static String valueOf(ContactPoint contactPoint) {
		return contactPoint != null ? StringUtils.trimToNull(contactPoint.getValue()) : null;
	}
	
	private static String firstAttributeValue(List<PersonAttribute> attributes, String... normalizedNames) {
		if (attributes == null || normalizedNames == null) {
			return null;
		}
		for (PersonAttribute attribute : attributes) {
			if (attribute == null || StringUtils.isBlank(attribute.getName())) {
				continue;
			}
			String normalizedName = normalizeAttributeName(attribute.getName());
			for (String candidate : normalizedNames) {
				if (candidate.equals(normalizedName)) {
					return StringUtils.trimToNull(attribute.getValue());
				}
			}
		}
		return null;
	}
	
	private static String normalizeAttributeName(String value) {
		return value.trim().toLowerCase().replaceAll("[\\s_-]+", "");
	}
	
	public static final class TelecomValues {
		
		private final String telephoneNumber;
		
		private final String emergencyContactNumber;
		
		public TelecomValues(String telephoneNumber, String emergencyContactNumber) {
			this.telephoneNumber = telephoneNumber;
			this.emergencyContactNumber = emergencyContactNumber;
		}
		
		public String getTelephoneNumber() {
			return telephoneNumber;
		}
		
		public String getEmergencyContactNumber() {
			return emergencyContactNumber;
		}
	}
}
