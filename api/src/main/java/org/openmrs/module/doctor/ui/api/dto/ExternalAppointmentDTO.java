package org.openmrs.module.doctor.ui.api.dto;

public class ExternalAppointmentDTO {
	
	private Integer id;
	
	private String facilityName;
	
	private String facilityUuid;
	
	private String status;
	
	private String requestId;
	
	private String requesterId;;
	
	private String patientUuid;
	
	private String patientName;
	
	private String patientIdentifier;
	
	private String requestDate;
	
	private String practitionerName;
	
	private String appointmentDate;
	
	private String appointmentTime;
	
	private String visitId;
	
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
	
	public String getStatus() {
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
	
	public String getRequesterId() {
		return requesterId;
	}
	
	public void setRequesterId(String requesterId) {
		this.requesterId = requesterId;
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
	
	public String getRequestDate() {
		return requestDate;
	}
	
	public void setRequestDate(String requestDate) {
		this.requestDate = requestDate;
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
		return "ExternalAppointmentDTO{" + "id=" + id + ", facilityName='" + facilityName + '\'' + ", facilityUuid='"
		        + facilityUuid + '\'' + ", status='" + status + '\'' + ", requestId='" + requestId + '\''
		        + ", requesterId='" + requesterId + '\'' + ", patientUuid='" + patientUuid + '\'' + ", patientName='"
		        + patientName + '\'' + ", patientIdentifier='" + patientIdentifier + '\'' + ", requestDate='" + requestDate
		        + '\'' + ", practitionerName='" + practitionerName + '\'' + ", appointmentDate='" + appointmentDate + '\''
		        + ", appointmentTime='" + appointmentTime + '\'' + ", visitId='" + visitId + '\'' + '}';
	}
}
