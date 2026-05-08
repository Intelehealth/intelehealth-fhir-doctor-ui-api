package org.openmrs.module.ihmodule.api.patientexchange.domain;

public class ConfigFacility {
	
	private int id;
	
	private String facilityName;
	
	private String facilityUuid;
	
	private boolean status;
	
	private String prescriptionApi;
	
	private String referralApi;
	
	private String labApi;
	
	private String appointmentApi;
	
	private String uuid;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
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
	
	public String getUuid() {
		return this.uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	@Override
	public String toString() {
		return "ConfigFacility [id=" + id + ", facilityName=" + facilityName + ", facilityUuid=" + facilityUuid
		        + ", status=" + status + ", prescriptionApi=" + prescriptionApi + ", referralApi=" + referralApi
		        + ", labApi=" + labApi + ", appointmentApi=" + appointmentApi + ", uuid=" + uuid + "]";
	}
	
}
