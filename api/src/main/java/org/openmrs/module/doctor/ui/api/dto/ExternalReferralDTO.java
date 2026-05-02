package org.openmrs.module.doctor.ui.api.dto;

public class ExternalReferralDTO {
	
	private Integer id;
	
	private String facilityName;
	
	private String facilityUuid;
	
	private String patientName;
	
	private String patientId;
	
	private String referredBy;
	
	private String referrerId;
	
	private String referTo;
	
	private String priorityOfReferral;
	
	private String reason;
	
	private String visitId;
	
	private String uuid;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getFacilityName() {
		return facilityName;
	}
	
	public void setFacilityName(String facilityName) {
		this.facilityName = facilityName;
	}
	
	public String getFacilityUuid() {
		return facilityUuid;
	}
	
	public void setFacilityUuid(String facilityUuid) {
		this.facilityUuid = facilityUuid;
	}
	
	public String getPatientName() {
		return patientName;
	}
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}
	
	public String getPatientId() {
		return patientId;
	}
	
	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}
	
	public String getReferredBy() {
		return referredBy;
	}
	
	public void setReferredBy(String referredBy) {
		this.referredBy = referredBy;
	}
	
	public String getReferrerId() {
		return referrerId;
	}
	
	public void setReferrerId(String referrerId) {
		this.referrerId = referrerId;
	}
	
	public String getReferTo() {
		return referTo;
	}
	
	public void setReferTo(String referTo) {
		this.referTo = referTo;
	}
	
	public String getPriorityOfReferral() {
		return priorityOfReferral;
	}
	
	public void setPriorityOfReferral(String priorityOfReferral) {
		this.priorityOfReferral = priorityOfReferral;
	}
	
	public String getReason() {
		return reason;
	}
	
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public String getVisitId() {
		return visitId;
	}
	
	public void setVisitId(String visitId) {
		this.visitId = visitId;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	@Override
	public String toString() {
		return "ExternalReferralDTO{" + "id=" + id + ", facilityName='" + facilityName + '\'' + ", facilityUuid='"
		        + facilityUuid + '\'' + ", patientName='" + patientName + '\'' + ", patientId='" + patientId + '\''
		        + ", referredBy='" + referredBy + '\'' + ", referrerId='" + referrerId + '\'' + ", referTo='" + referTo
		        + '\'' + ", priorityOfReferral='" + priorityOfReferral + '\'' + ", reason='" + reason + '\'' + ", visitId='"
		        + visitId + '\'' + ", uuid='" + uuid + '\'' + '}';
	}
}
