package org.openmrs.module.ihmodule.api.patientexchange.export;

public class CreatedPatientExportResult {
	
	private String payload;
	
	private int totalPatients;
	
	private int exportedPatients;
	
	private int validationFailedPatients;
	
	public String getPayload() {
		return payload;
	}
	
	public void setPayload(String payload) {
		this.payload = payload;
	}
	
	public int getTotalPatients() {
		return totalPatients;
	}
	
	public void setTotalPatients(int totalPatients) {
		this.totalPatients = totalPatients;
	}
	
	public int getExportedPatients() {
		return exportedPatients;
	}
	
	public void setExportedPatients(int exportedPatients) {
		this.exportedPatients = exportedPatients;
	}
	
	public int getValidationFailedPatients() {
		return validationFailedPatients;
	}
	
	public void setValidationFailedPatients(int validationFailedPatients) {
		this.validationFailedPatients = validationFailedPatients;
	}
}
