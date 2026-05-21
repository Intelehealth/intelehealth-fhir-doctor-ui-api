package org.openmrs.module.ihmodule.api.patientexchange.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import static org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils.*;

import java.util.UUID;

public class DataExchangeAuditLog {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	
	private String resourceName;
	
	private Boolean status = true;
	
	private String resourceUuid;
	
	private String fhirId;
	
	private String request;
	
	private String requestUrl;
	
	private String response;
	
	private String responseStatus;
	
	private String dateCreated = toFormattedDateNow();
	
	private Integer creator = 1; // Admin-OpenMRS
	
	private Integer changedBy;
	
	private String dateChanged;
	
	private Boolean voided = false;
	
	private Integer voidedBy;
	
	private String dateVoided;
	
	private String voidReason;
	
	private String uuid = UUID.randomUUID().toString();
	
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
	
	public Boolean getStatus() {
		return status;
	}
	
	public void setStatus(Boolean status) {
		this.status = status;
	}
	
	public String getResourceUuid() {
		return resourceUuid;
	}
	
	public void setResourceUuid(String resourceUuid) {
		this.resourceUuid = resourceUuid;
	}
	
	public String getFhirId() {
		return fhirId;
	}
	
	public void setFhirId(String fhirId) {
		this.fhirId = fhirId;
	}
	
	public String getRequest() {
		return request;
	}
	
	public void setRequest(String request) {
		this.request = request;
	}
	
	public String getRequestUrl() {
		return requestUrl;
	}
	
	public void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
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
	
	public String getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public Integer getCreator() {
		return creator;
	}
	
	public void setCreator(Integer creator) {
		this.creator = creator;
	}
	
	public Integer getChangedBy() {
		return changedBy;
	}
	
	public void setChangedBy(Integer changedBy) {
		this.changedBy = changedBy;
	}
	
	public String getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(String dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	public Boolean getVoided() {
		return voided;
	}
	
	public void setVoided(Boolean voided) {
		this.voided = voided;
	}
	
	public Integer getVoidedBy() {
		return voidedBy;
	}
	
	public void setVoidedBy(Integer voidedBy) {
		this.voidedBy = voidedBy;
	}
	
	public String getDateVoided() {
		return dateVoided;
	}
	
	public void setDateVoided(String dateVoided) {
		this.dateVoided = dateVoided;
	}
	
	public String getVoidReason() {
		return voidReason;
	}
	
	public void setVoidReason(String voidReason) {
		this.voidReason = voidReason;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	@Override
	public String toString() {
		return "DataExchangeAuditLog [id=" + id + ", resourceName=" + resourceName + ", status=" + status
		        + ", resourceUuid=" + resourceUuid + ", fhirId=" + fhirId + ", request=" + request + ", requestUrl="
		        + requestUrl + ", response=" + response + ", responseStatus=" + responseStatus + ", dateCreated="
		        + dateCreated + ", creator=" + creator + ", changedBy=" + changedBy + ", dateChanged=" + dateChanged
		        + ", voided=" + voided + ", voidedBy=" + voidedBy + ", dateVoided=" + dateVoided + ", voidReason="
		        + voidReason + ", uuid=" + uuid + "]";
	}
	
}
