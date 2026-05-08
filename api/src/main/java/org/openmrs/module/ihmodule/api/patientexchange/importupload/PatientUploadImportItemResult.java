package org.openmrs.module.ihmodule.api.patientexchange.importupload;

public class PatientUploadImportItemResult {
	
	private String inputId;
	
	private String status;
	
	private String message;
	
	private String createdId;
	
	public String getInputId() {
		return inputId;
	}
	
	public void setInputId(String inputId) {
		this.inputId = inputId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getCreatedId() {
		return createdId;
	}
	
	public void setCreatedId(String createdId) {
		this.createdId = createdId;
	}
}
