package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

/**
 * Indicates which fuzzy duplicate detector first produced matches during patient import.
 */
public enum MpiImportDuplicateReviewSource {
	
	OPENMRS("openmrs"), FHIR("fhir");
	
	private final String value;
	
	MpiImportDuplicateReviewSource(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
}
