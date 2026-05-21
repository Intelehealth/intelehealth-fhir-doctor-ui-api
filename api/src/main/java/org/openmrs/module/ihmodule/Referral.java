package org.openmrs.module.ihmodule;

public class Referral {
	
	private Integer personId;
	
	private String referText;
	
	private String patientName;
	
	private String gender;
	
	private String patientUuid;
	
	private String referDate;
	
	private String dob;
	
	public String getReferText() {
		return referText;
	}
	
	public void setReferText(String referText) {
		this.referText = referText;
	}
	
	public String getPatientName() {
		return patientName;
	}
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getReferDate() {
		return referDate;
	}
	
	public void setReferDate(String referDate) {
		this.referDate = referDate;
	}
	
	public String getDob() {
		return dob;
	}
	
	public void setDob(String dob) {
		this.dob = dob;
	}
	
	public Integer getPersonId() {
		return personId;
	}
	
	public void setPersonId(Integer personId) {
		this.personId = personId;
	}
	
}
