package org.openmrs.module.ihmodule.api.patientexchange.domain;

public class Orders {
	
	private String action = "NEW";
	
	private String type = "testorder";
	
	private String patient;
	
	private String careSetting = "6f0c9a92-6f24-11e3-af88-005056821db0";
	
	// for dev o3
	private String orderer = "111cdab9-f058-4ef6-9034-991826164e28";
	
	//	private String orderer = "73bbb069-9781-4afc-a9d1-54b6b2270e05";
	private String encounter = null;
	
	private String concept;
	
	private String instructions = "";
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getPatient() {
		return patient;
	}
	
	public void setPatient(String patient) {
		this.patient = patient;
	}
	
	public String getCareSetting() {
		return careSetting;
	}
	
	public void setCareSetting(String careSetting) {
		this.careSetting = careSetting;
	}
	
	public String getOrderer() {
		return orderer;
	}
	
	public void setOrderer(String orderer) {
		this.orderer = orderer;
	}
	
	public String getEncounter() {
		return encounter;
	}
	
	public void setEncounter(String encounter) {
		this.encounter = encounter;
	}
	
	public String getConcept() {
		return concept;
	}
	
	public void setConcept(String concept) {
		this.concept = concept;
	}
	
	public String getInstructions() {
		return instructions;
	}
	
	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}
	
	@Override
	public String toString() {
		return "Orders [action=" + action + ", type=" + type + ", patient=" + patient + ", careSetting=" + careSetting
		        + ", orderer=" + orderer + ", encounter=" + encounter + ", concept=" + concept + ", instructions="
		        + instructions + "]";
	}
	
}
