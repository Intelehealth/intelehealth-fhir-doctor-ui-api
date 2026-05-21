package org.openmrs.module.ihmodule.api.patientexchange.domain;

public class CompeletdVisit {
	
	private String visit;
	
	private Integer visitId;
	
	private String patient;
	
	private String date;
	
	public String getVisit() {
		return visit;
	}
	
	public void setVisit(String visit) {
		this.visit = visit;
	}
	
	public String getPatient() {
		return patient;
	}
	
	public void setPatient(String patient) {
		this.patient = patient;
	}
	
	public String getDate() {
		return date;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public Integer getVisitId() {
		return visitId;
	}
	
	public void setVisitId(Integer visitId) {
		this.visitId = visitId;
	}
	
	@Override
	public String toString() {
		return "CompeletdVisit [visit=" + visit + ", patient=" + patient + ", date=" + date + "]";
	}
	
}
