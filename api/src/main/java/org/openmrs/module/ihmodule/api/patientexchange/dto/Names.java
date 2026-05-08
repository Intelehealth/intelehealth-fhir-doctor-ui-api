package org.openmrs.module.ihmodule.api.patientexchange.dto;

import java.util.List;

public class Names {
	
	private List<String> given;
	
	private String family;
	
	public List<String> getGiven() {
		return given;
	}
	
	public void setGiven(List<String> given) {
		this.given = given;
	}
	
	public String getFamily() {
		return family;
	}
	
	public void setFamily(String family) {
		this.family = family;
	}
	
}
