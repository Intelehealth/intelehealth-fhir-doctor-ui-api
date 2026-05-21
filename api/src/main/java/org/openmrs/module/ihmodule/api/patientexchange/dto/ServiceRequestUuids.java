package org.openmrs.module.ihmodule.api.patientexchange.dto;

public class ServiceRequestUuids {
	
	private Integer encounterId;
	
	private String visitUuid;
	
	private String locationUuid;
	
	private String encounterTypeUuid;
	
	public String getVisitUuid() {
		return visitUuid;
	}
	
	public void setVisitUuid(String visitUuid) {
		this.visitUuid = visitUuid;
	}
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	public String getEncounterTypeUuid() {
		return encounterTypeUuid;
	}
	
	public void setEncounterTypeUuid(String encounterTypeUuid) {
		this.encounterTypeUuid = encounterTypeUuid;
	}
	
	public Integer getEncounterId() {
		return encounterId;
	}
	
	public void setEncounterId(Integer encounterId) {
		this.encounterId = encounterId;
	}
	
	@Override
	public String toString() {
		return "ServiceRequestUuids [visitUuid=" + visitUuid + ", locationUuid=" + locationUuid + ", encounterTypeUuid="
		        + encounterTypeUuid + "]";
	}
	
}
