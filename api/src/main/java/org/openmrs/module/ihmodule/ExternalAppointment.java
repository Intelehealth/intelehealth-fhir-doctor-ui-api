package org.openmrs.module.ihmodule;

import java.util.UUID;

import org.openmrs.BaseOpenmrsData;

public class ExternalAppointment extends BaseOpenmrsData {
	
	private Integer id;
	
	private String facilityName;
	
	private String facilityUuid;
	
	private String status;
	
	private String requestId = UUID.randomUUID().toString();
	
	private String requesterId;;
	
	private String patientUuid;
	
	private String patientName;
	
	private String patientIdentifier;
	
	private String serviceCategory;
	
	private String serviceType;
	
	private String specialty;
	
	private String slot;
	
	private int duration;
	
	private String practitionerName;
	
	private String appointmentDate;
	
	private String appointmentTime;
	
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
	
	public String isStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getRequestId() {
		return requestId;
	}
	
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getRequesterId() {
		return requesterId;
	}
	
	public void setRequesterId(String requesterId) {
		this.requesterId = requesterId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getPatientName() {
		return patientName;
	}
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}
	
	public String getPatientIdentifier() {
		return patientIdentifier;
	}
	
	public void setPatientIdentifier(String patientIdentifier) {
		this.patientIdentifier = patientIdentifier;
	}
	
	public String getServiceCategory() {
		return serviceCategory;
	}
	
	public void setServiceCategory(String serviceCategory) {
		this.serviceCategory = serviceCategory;
	}
	
	public String getServiceType() {
		return serviceType;
	}
	
	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}
	
	public String getSpecialty() {
		return specialty;
	}
	
	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}
	
	public String getSlot() {
		return slot;
	}
	
	public void setSlot(String slot) {
		this.slot = slot;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public String getPractitionerName() {
		return practitionerName;
	}
	
	public void setPractitionerName(String practitionerName) {
		this.practitionerName = practitionerName;
	}
	
	public String getAppointmentDate() {
		return appointmentDate;
	}
	
	public void setAppointmentDate(String appointmentDate) {
		this.appointmentDate = appointmentDate;
	}
	
	public String getAppointmentTime() {
		return appointmentTime;
	}
	
	public void setAppointmentTime(String appointmentTime) {
		this.appointmentTime = appointmentTime;
	}
	
	@Override
	public String toString() {
		return "ExternalAppointment [id=" + id + ", facilityName=" + facilityName + ", facilityUuid=" + facilityUuid
		        + ", status=" + status + ", requestId=" + requestId + ", requesterId=" + requesterId + ", patientUuid="
		        + patientUuid + ", patientName=" + patientName + ", patientIdentifier=" + patientIdentifier
		        + ", serviceCategory=" + serviceCategory + ", serviceType=" + serviceType + ", specialty=" + specialty
		        + ", slot=" + slot + ", duration=" + duration + ", practitionerName=" + practitionerName
		        + ", appointmentDate=" + appointmentDate + ", appointmentTime=" + appointmentTime + "]";
	}
	
}
