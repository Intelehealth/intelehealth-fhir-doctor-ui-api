package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Body for adding or updating the facility patient's source patient identifier (central FHIR
 * Patient logical id).
 */
public class SourcePatientIdentifierUpdateRequest {
	
	private String patientUuid;
	
	private String identifierValue;
	
	/** OpenMRS location UUID for the new source patient identifier (required when one is created). */
	private String locationUuid;
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getIdentifierValue() {
		return identifierValue;
	}
	
	public void setIdentifierValue(String identifierValue) {
		this.identifierValue = identifierValue;
	}
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
}
