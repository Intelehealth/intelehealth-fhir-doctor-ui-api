package org.openmrs.module.ihmodule.dto;

public class DiagnosticReportDTO {
	
	private String title;
	
	private String patientId;
	
	private String resourceId;
	
	private String contentType;
	
	private String fileName;
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getPatientId() {
		return patientId;
	}
	
	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}
	
	public String getResourceId() {
		return resourceId;
	}
	
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	@Override
	public String toString() {
		return "DiagnosticReportDTO [title=" + title + ", patientId=" + patientId + ", resourceId=" + resourceId
		        + ", contentType=" + contentType + ", fileName=" + fileName + "]";
	}
	
}
