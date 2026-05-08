package org.openmrs.module.ihmodule.api.patientexchange.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SearchPateintDTO {
	
	private List<Identifier> identifiers = new ArrayList<Identifier>();
	
	private List<Names> names = new ArrayList<Names>();
	
	private Instant birthdate;
	
	private String gender;
	
	private String phone;
	
	List<PatientAddress> address = new ArrayList<PatientAddress>();
	
	public List<Identifier> getIdentifiers() {
		return identifiers;
	}
	
	public void setIdentifiers(List<Identifier> identifiers) {
		this.identifiers = identifiers;
	}
	
	public List<Names> getNames() {
		return names;
	}
	
	public void setNames(List<Names> names) {
		this.names = names;
	}
	
	public Instant getBirthdate() {
		return birthdate;
	}
	
	public void setBirthdate(Instant birthdate) {
		this.birthdate = birthdate;
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public String getPhone() {
		return phone;
	}
	
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	public List<PatientAddress> getAddress() {
		return address;
	}
	
	public void setAddress(List<PatientAddress> address) {
		this.address = address;
	}
	
}
