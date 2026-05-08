package org.openmrs.module.ihmodule.api.patientexchange.domain;

public class PatientDTO {
	
	private String uuid;
	
	private String dateCreated;
	
	private String dateChanged;
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public String getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(String dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	@Override
	public String toString() {
		return "PatientDTO [uuid=" + uuid + ", dateCreated=" + dateCreated + ", dateChanged=" + dateChanged + "]";
	}
	
}
