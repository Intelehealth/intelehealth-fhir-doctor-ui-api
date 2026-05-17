package org.openmrs.module.ihmodule.api.patientexchange.validation;

/**
 * Maps OpenMRS {@code PersonAttributeType} display names to FHIR StructureDefinition id suffixes
 * (kebab-case) used in Patient extensions. Used by transfer, export, and fuzzy-match responses.
 * <p>
 * OpenMRS attribute types commonly configured:
 * <ul>
 * <li>Telephone Number, Emergency Contact Number → {@code Patient.telecom} (not extensions)</li>
 * <li>Economic Status, Education Level, Caste, NationalID, Emergency Contact Type → profile
 * extensions</li>
 * </ul>
 */
public final class PersonAttributeToExtensionSuffix {
	
	private PersonAttributeToExtensionSuffix() {
	}
	
	/**
	 * @return extension suffix for
	 *         {@link org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute#getName()}
	 *         , or {@code null} if the attribute should not become a Patient extension (e.g. phone
	 *         numbers and types).
	 */
	public static String map(String attributeName) {
		if (attributeName == null) {
			return null;
		}
		String normalized = normalizeAttributeName(attributeName);
		switch (normalized) {
			case "telephonenumber":
			case "phonenumber":
			case "emergencycontactnumber":
				// Telephone Number / Emergency Contact Number → Patient.telecom
				return null;
			case "emergencycontacttype":
				// Emergency Contact Type
				return "Emergency-Contact-Type";
			case "emergencycontactname":
				return null;
			case "caste":
				// Caste
				return "Caste";
			case "economicstatus":
				// Economic Status
				return "Economic-Status";
			case "educationlevel":
				// Education Level
				return "Education-Level";
			case "nationalid":
				// NationalID (also matches "National ID")
				return "NationalID";
			case "occupation":
				return "occupation";
			case "householdnumber":
				return "Household-Number";
			default:
				return null;
		}
	}
	
	static String normalizeAttributeName(String attributeName) {
		return attributeName.trim().toLowerCase().replaceAll("[\\s_-]+", "");
	}
}
