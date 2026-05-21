package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * One non-voided {@link org.openmrs.PatientIdentifier} on a patient for REST responses.
 */
public class PatientIdentifierSnapshot {
	
	private String identifierTypeUuid;
	
	private String identifierTypeName;
	
	private String value;
	
	private Boolean preferred;
	
	private Boolean voided;
	
	private String locationUuid;
	
	public String getIdentifierTypeUuid() {
		return identifierTypeUuid;
	}
	
	public void setIdentifierTypeUuid(String identifierTypeUuid) {
		this.identifierTypeUuid = identifierTypeUuid;
	}
	
	public String getIdentifierTypeName() {
		return identifierTypeName;
	}
	
	public void setIdentifierTypeName(String identifierTypeName) {
		this.identifierTypeName = identifierTypeName;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public Boolean getPreferred() {
		return preferred;
	}
	
	public void setPreferred(Boolean preferred) {
		this.preferred = preferred;
	}
	
	public Boolean getVoided() {
		return voided;
	}
	
	public void setVoided(Boolean voided) {
		this.voided = voided;
	}
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
}
