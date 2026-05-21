package org.openmrs.module.ihmodule.api.patientexchange.dto;

public class DrugAndConceptDTO {
	
	private String drugUuid;
	
	private String medicineUUid;
	
	public String getDrugUuid() {
		return drugUuid;
	}
	
	public void setDrugUuid(String drugUuid) {
		this.drugUuid = drugUuid;
	}
	
	public String getMedicineUUid() {
		return medicineUUid;
	}
	
	public void setMedicineUUid(String medicineUUid) {
		this.medicineUUid = medicineUUid;
	}
	
}
