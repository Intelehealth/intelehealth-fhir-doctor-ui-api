package org.openmrs.module.doctor.ui.api;

import java.util.UUID;

import org.hibernate.validator.constraints.NotEmpty;
import org.openmrs.BaseOpenmrsData;

import javax.validation.constraints.NotNull;

public class ExternalAppointment extends BaseOpenmrsData {
	
	private Integer id;
	
	private String facilityName;
	
	@NotNull(message = "Facility UUID can't be empty")
	@NotEmpty(message = "Facility UUID can't be empty")
	private String facilityUuid;
	
	@NotNull(message = "Status can't be empty")
	@NotEmpty(message = "Status can't be empty")
	private String status;
	
	private String requestId = UUID.randomUUID().toString();
	
	@NotNull(message = "Requester ID can't be empty")
	@NotEmpty(message = "Requester ID can't be empty")
	private String requesterId;;
	
	@NotNull(message = "Patient ID (MPI) can't be empty")
	@NotEmpty(message = "Patient ID (MPI) can't be empty")
	private String patientUuid;
	
	@NotNull(message = "Patient Name can't be empty")
	@NotEmpty(message = "Patient Name can't be empty")
	private String patientName;
	
	@NotNull(message = "Patient Identifier can't be empty")
	@NotEmpty(message = "Patient Identifier can't be empty")
	private String patientIdentifier;
	
	private String serviceCategory;
	
	private String serviceType;
	
	private String specialty;
	
	@NotNull(message = "Slot can't be empty")
	@NotEmpty(message = "Slot can't be empty")
	private String slot;
	
	private int duration;
	
	@NotNull(message = "Practitioner name can't be empty")
	@NotEmpty(message = "Practitioner name can't be empty")
	private String practitionerName;
	
	private String appointmentDate;
	
	private String appointmentTime;
	
	@NotNull(message = "Visit ID can't be empty")
	@NotEmpty(message = "Visit ID can't be empty")
	private String visitId;
	
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
	
	public String getVisitId() {
		return visitId;
	}
	
	public void setVisitId(String visitId) {
		this.visitId = visitId;
	}
	
	@Override
	public String toString() {
		return "ExternalAppointment{" + "id=" + id + ", facilityName='" + facilityName + '\'' + ", facilityUuid='"
		        + facilityUuid + '\'' + ", status='" + status + '\'' + ", requestId='" + requestId + '\''
		        + ", requesterId='" + requesterId + '\'' + ", patientUuid='" + patientUuid + '\'' + ", patientName='"
		        + patientName + '\'' + ", patientIdentifier='" + patientIdentifier + '\'' + ", serviceCategory='"
		        + serviceCategory + '\'' + ", serviceType='" + serviceType + '\'' + ", specialty='" + specialty + '\''
		        + ", slot='" + slot + '\'' + ", duration=" + duration + ", practitionerName='" + practitionerName + '\''
		        + ", appointmentDate='" + appointmentDate + '\'' + ", appointmentTime='" + appointmentTime + '\''
		        + ", visitId='" + visitId + '\'' + '}';
	}
}
