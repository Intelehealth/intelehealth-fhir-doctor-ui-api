package org.openmrs.module.ihmodule.api.patientexchange.validation;

/**
 * Maps OpenMRS {@code PersonAttributeType} display names to FHIR StructureDefinition id suffixes
 * (kebab-case) used in Patient extensions. Used by transfer and export when building FHIR
 * extensions from person attributes.
 */
public final class PersonAttributeToExtensionSuffix {
	
	private PersonAttributeToExtensionSuffix() {
	}
	
	/**
	 * @return extension suffix for
	 *         {@link org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute#getName()}
	 *         , or {@code null} if the attribute should not become a Patient extension (e.g.
	 *         primary phone).
	 */
	public static String map(String attributeName) {
		if (attributeName == null) {
			return null;
		}
		String normalized = attributeName.trim().toLowerCase().replaceAll("[\\s_-]+", "");
		switch (normalized) {
			case "telephonenumber":
			case "phonenumber":
			case "emergencycontactnumber":
			case "emergencycontactname":
				// Patient phone numbers are represented in Patient.telecom, not as custom extensions.
				return null;
			case "caste":
				return "Caste";
			case "economicstatus":
				return "Economic-Status";
			case "educationlevel":
				return "Education-Level";
			case "occupation":
				return "occupation";
			case "nationalid":
				return "NationalID";
			case "householdnumber":
				return "Household-Number";
			default:
				return null;
		}
	}
}
