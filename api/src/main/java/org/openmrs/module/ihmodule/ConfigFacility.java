package org.openmrs.module.ihmodule;

import java.util.Date;

import org.openmrs.User;

public class ConfigFacility {
	
	private Integer id;
	
	private String uuid;
	
	private User creator;
	
	private Date dateCreated;
	
	private User changedBy;
	
	private Date dateChanged;
	
	private boolean voided;
	
	private User voidedBy;
	
	private Date dateVoided;
	
	private String voidReason;
	
	private String facilityName;
	
	private String facilityUuid;
	
	private boolean status;
	
	private String prescriptionApi;
	
	private String referralApi;
	
	private String labApi;
	
	private String appointmentApi;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public User getCreator() {
		return creator;
	}
	
	public void setCreator(User creator) {
		this.creator = creator;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public User getChangedBy() {
		return changedBy;
	}
	
	public void setChangedBy(User changedBy) {
		this.changedBy = changedBy;
	}
	
	public Date getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(Date dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	public boolean isVoided() {
		return voided;
	}
	
	public void setVoided(boolean voided) {
		this.voided = voided;
	}
	
	public User getVoidedBy() {
		return voidedBy;
	}
	
	public void setVoidedBy(User voidedBy) {
		this.voidedBy = voidedBy;
	}
	
	public Date getDateVoided() {
		return dateVoided;
	}
	
	public void setDateVoided(Date dateVoided) {
		this.dateVoided = dateVoided;
	}
	
	public String getVoidReason() {
		return voidReason;
	}
	
	public void setVoidReason(String voidReason) {
		this.voidReason = voidReason;
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
	
	public boolean isStatus() {
		return status;
	}
	
	public void setStatus(boolean status) {
		this.status = status;
	}
	
	public String getPrescriptionApi() {
		return prescriptionApi;
	}
	
	public void setPrescriptionApi(String prescriptionApi) {
		this.prescriptionApi = prescriptionApi;
	}
	
	public String getReferralApi() {
		return referralApi;
	}
	
	public void setReferralApi(String referralApi) {
		this.referralApi = referralApi;
	}
	
	public String getLabApi() {
		return labApi;
	}
	
	public void setLabApi(String labApi) {
		this.labApi = labApi;
	}
	
	public String getAppointmentApi() {
		return appointmentApi;
	}
	
	public void setAppointmentApi(String appointmentApi) {
		this.appointmentApi = appointmentApi;
	}
	
}
