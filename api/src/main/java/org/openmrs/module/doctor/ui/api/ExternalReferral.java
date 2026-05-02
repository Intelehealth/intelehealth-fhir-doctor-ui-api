package org.openmrs.module.doctor.ui.api;

import org.hibernate.validator.constraints.NotEmpty;
import org.openmrs.BaseOpenmrsData;

import javax.validation.constraints.NotNull;

public class ExternalReferral extends BaseOpenmrsData {
	
	private Integer id;
	
	private String facilityName;
	
	@NotNull(message = "Facility UUID can't be empty")
	@NotEmpty(message = "Facility UUID can't be empty")
	private String facilityUuid;
	
	@NotNull(message = "Patient Name can't be empty")
	@NotEmpty(message = "Patient Name can't be empty")
	private String patientName;
	
	@NotNull(message = "Patient ID (MPI) can't be empty")
	@NotEmpty(message = "Patient ID (MPI) can't be empty")
	private String patientId;
	
	@NotNull(message = "Referred By can't be empty")
	@NotEmpty(message = "Referred By can't be empty")
	private String referredBy;
	
	@NotNull(message = "Referrer Id can't be empty")
	@NotEmpty(message = "Referrer Id can't be empty")
	private String referrerId;
	
	@NotNull(message = "Refer to can't be empty")
	@NotEmpty(message = "Refer to can't be empty")
	private String referTo;
	
	private String priorityOfReferral;
	
	private String reason;
	
	@NotNull(message = "Visit ID can't be empty")
	@NotEmpty(message = "Visit ID can't be empty")
	private String visitId;
	
	@Override
	public Integer getId() {
		return id;
	}
	
	@Override
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
	
	public String getReferTo() {
		return referTo;
	}
	
	public void setReferTo(String referTo) {
		this.referTo = referTo;
	}
	
	public String getReferrerId() {
		return referrerId;
	}
	
	public void setReferrerId(String referrerId) {
		this.referrerId = referrerId;
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
	
	@Override
	public String toString() {
		return "ExternalReferral{" + "id=" + id + ", facilityName='" + facilityName + '\'' + ", facilityUuid='"
		        + facilityUuid + '\'' + ", patientName='" + patientName + '\'' + ", patientId='" + patientId + '\''
		        + ", referredBy='" + referredBy + '\'' + ", referrerId='" + referrerId + '\'' + ", referTo='" + referTo
		        + '\'' + ", priorityOfReferral='" + priorityOfReferral + '\'' + ", reason='" + reason + '\'' + ", visitId='"
		        + visitId + '\'' + '}';
	}
}
