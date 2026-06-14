package org.openmrs.module.ihmodule.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrescriptionShareRequestDTO implements Serializable {
	
	private String patientUuid;
	
	private String visitUuid;
	
	private List<String> locationUuids = new ArrayList<>();
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getVisitUuid() {
		return visitUuid;
	}
	
	public void setVisitUuid(String visitUuid) {
		this.visitUuid = visitUuid;
	}
	
	public List<String> getLocationUuids() {
		return locationUuids;
	}
	
	public void setLocationUuids(List<String> locationUuids) {
		this.locationUuids = locationUuids;
	}
	
}
