package org.openmrs.module.ihmodule.api.patientexchange.domain;

import java.math.BigDecimal;

public class PrescriptionDomain {
	
	private String action = "NEW";
	
	private String patient;
	
	private String type = "drugorder";
	
	private String careSetting = "6f0c9a92-6f24-11e3-af88-005056821db0";
	
	// for dev o3
	//private String orderer= "de92eec2-fb00-441d-8a1d-66d44509d134";
	private String orderer = "73bbb069-9781-4afc-a9d1-54b6b2270e05";
	
	private String encounter;
	
	private String drug;
	
	private BigDecimal dose;
	
	private String doseUnits;
	
	private String route;
	
	private String frequency;
	
	private Boolean asNeeded = true;
	
	private String asNeededCondition = null;
	
	private Integer numRefills;
	
	private BigDecimal quantity;
	
	private String quantityUnits;
	
	private BigDecimal duration;
	
	private String durationUnits;
	
	private String dosingType = "org.openmrs.SimpleDosingInstructions";
	
	private String dosingInstructions;
	
	private String concept;
	
	private String orderReasonNonCoded;
	
	public String getAction() {
		return action;
	}
	
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getPatient() {
		return patient;
	}
	
	public void setPatient(String patient) {
		this.patient = patient;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
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
	
	public String getDrug() {
		return drug;
	}
	
	public void setDrug(String drug) {
		this.drug = drug;
	}
	
	public BigDecimal getDose() {
		return dose;
	}
	
	public void setDose(BigDecimal dose) {
		this.dose = dose;
	}
	
	public String getDoseUnits() {
		return doseUnits;
	}
	
	public void setDoseUnits(String doseUnits) {
		this.doseUnits = doseUnits;
	}
	
	public String getRoute() {
		return route;
	}
	
	public void setRoute(String route) {
		this.route = route;
	}
	
	public String getFrequency() {
		return frequency;
	}
	
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
	
	public Boolean getAsNeeded() {
		return asNeeded;
	}
	
	public void setAsNeeded(Boolean asNeeded) {
		this.asNeeded = asNeeded;
	}
	
	public String getAsNeededCondition() {
		return asNeededCondition;
	}
	
	public void setAsNeededCondition(String asNeededCondition) {
		this.asNeededCondition = asNeededCondition;
	}
	
	public Integer getNumRefills() {
		return numRefills;
	}
	
	public void setNumRefills(Integer numRefills) {
		this.numRefills = numRefills;
	}
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	public String getQuantityUnits() {
		return quantityUnits;
	}
	
	public void setQuantityUnits(String quantityUnits) {
		this.quantityUnits = quantityUnits;
	}
	
	public BigDecimal getDuration() {
		return duration;
	}
	
	public void setDuration(BigDecimal duration) {
		this.duration = duration;
	}
	
	public String getDurationUnits() {
		return durationUnits;
	}
	
	public void setDurationUnits(String durationUnits) {
		this.durationUnits = durationUnits;
	}
	
	public String getDosingType() {
		return dosingType;
	}
	
	public void setDosingType(String dosingType) {
		this.dosingType = dosingType;
	}
	
	public String getDosingInstructions() {
		return dosingInstructions;
	}
	
	public void setDosingInstructions(String dosingInstructions) {
		this.dosingInstructions = dosingInstructions;
	}
	
	public String getConcept() {
		return concept;
	}
	
	public void setConcept(String concept) {
		this.concept = concept;
	}
	
	public String getOrderReasonNonCoded() {
		return orderReasonNonCoded;
	}
	
	public void setOrderReasonNonCoded(String orderReasonNonCoded) {
		this.orderReasonNonCoded = orderReasonNonCoded;
	}
	
	@Override
	public String toString() {
		return "PrescriptionDomain [action=" + action + ", patient=" + patient + ", type=" + type + ", careSetting="
		        + careSetting + ", orderer=" + orderer + ", encounter=" + encounter + ", drug=" + drug + ", dose=" + dose
		        + ", doseUnits=" + doseUnits + ", route=" + route + ", frequency=" + frequency + ", asNeeded=" + asNeeded
		        + ", asNeededCondition=" + asNeededCondition + ", numRefills=" + numRefills + ", quantity=" + quantity
		        + ", quantityUnits=" + quantityUnits + ", duration=" + duration + ", durationUnits=" + durationUnits
		        + ", dosingType=" + dosingType + ", dosingInstructions=" + dosingInstructions + ", concept=" + concept
		        + ", orderReasonNonCoded=" + orderReasonNonCoded + "]";
	}
	
}
