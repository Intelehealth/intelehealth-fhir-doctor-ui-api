package org.openmrs.module.ihmodule.api.patientexchange.domain;

import java.util.ArrayList;
import java.util.List;

public class ServiceRequestDomain {
	
	private String patient;
	
	private String location;
	
	private String encounterType;
	
	private String encounterDatetime;
	
	private String visit;
	
	private List<String> obs = new ArrayList<String>();
	
	private List<Orders> orders;
	
	public String getPatient() {
		return patient;
	}
	
	public void setPatient(String patient) {
		this.patient = patient;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getEncounterType() {
		return encounterType;
	}
	
	public void setEncounterType(String encounterType) {
		this.encounterType = encounterType;
	}
	
	public String getEncounterDatetime() {
		return encounterDatetime;
	}
	
	public void setEncounterDatetime(String encounterDatetime) {
		this.encounterDatetime = encounterDatetime;
	}
	
	public String getVisit() {
		return visit;
	}
	
	public void setVisit(String visit) {
		this.visit = visit;
	}
	
	public List<String> getObs() {
		return obs;
	}
	
	public void setObs(List<String> obs) {
		this.obs = obs;
	}
	
	public List<Orders> getOrders() {
		return orders;
	}
	
	public void setOrders(List<Orders> orders) {
		this.orders = orders;
	}
	
}
