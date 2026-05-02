package org.openmrs.module.ihmodule;

import org.openmrs.BaseOpenmrsData;

public class DataExchangeAuditLog extends BaseOpenmrsData {
	
	private Integer id;
	
	private String resourceName;
	
	private String resourceUuid;
	
	private boolean status;
	
	private String fhirId;
	
	private String response;
	
	private String responseStatus;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
	public String getResourceUuid() {
		return resourceUuid;
	}
	
	public void setResourceUuid(String resourceUuid) {
		this.resourceUuid = resourceUuid;
	}
	
	public boolean isStatus() {
		return status;
	}
	
	public void setStatus(boolean status) {
		this.status = status;
	}
	
	public String getFhirId() {
		return fhirId;
	}
	
	public void setFhirId(String fhirId) {
		this.fhirId = fhirId;
	}
	
	public String getResponse() {
		return response;
	}
	
	public void setResponse(String response) {
		this.response = response;
	}
	
	public String getResponseStatus() {
		return responseStatus;
	}
	
	public void setResponseStatus(String responseStatus) {
		this.responseStatus = responseStatus;
	}
	
}
