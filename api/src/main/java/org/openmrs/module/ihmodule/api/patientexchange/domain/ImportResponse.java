package org.openmrs.module.ihmodule.api.patientexchange.domain;

public class ImportResponse {
	
	private String message;
	
	private String uuid;
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
}
