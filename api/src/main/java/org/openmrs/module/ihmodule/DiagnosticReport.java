package org.openmrs.module.ihmodule;

import org.openmrs.BaseOpenmrsData;

public class DiagnosticReport extends BaseOpenmrsData {
	
	private Integer id;
	
	private String title;
	
	private String patientId;
	
	private String resourceId;
	
	private String fileName;
	
	private String contentType;
	
	private String fileData;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
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
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public String getFileData() {
		return fileData;
	}
	
	public void setFileData(String fileData) {
		this.fileData = fileData;
	}
	
	@Override
	public String toString() {
		return "DiagnosticReport [id=" + id + ", patientId=" + patientId + ", resourceId=" + resourceId + ", fileName="
		        + fileName + ", contentType=" + contentType + "]";
	}
	
}
