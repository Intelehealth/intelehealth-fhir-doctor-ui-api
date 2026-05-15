package org.openmrs.module.ihmodule.api.patientexchange.importupload;

public class PatientUploadImportItemResult {
	
	private String inputId;
	
	private String status;
	
	private String message;
	
	private String createdId;
	
	private String duplicateReviewCaseUuid;
	
	/**
	 * Comma-separated distinct {@code match_source} values from saved candidates (e.g. {@code fhir}
	 * , {@code openmrs}).
	 */
	private String duplicateDetectedSource;
	
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
	
	public String getDuplicateReviewCaseUuid() {
		return duplicateReviewCaseUuid;
	}
	
	public void setDuplicateReviewCaseUuid(String duplicateReviewCaseUuid) {
		this.duplicateReviewCaseUuid = duplicateReviewCaseUuid;
	}
	
	public String getDuplicateDetectedSource() {
		return duplicateDetectedSource;
	}
	
	public void setDuplicateDetectedSource(String duplicateDetectedSource) {
		this.duplicateDetectedSource = duplicateDetectedSource;
	}
}
