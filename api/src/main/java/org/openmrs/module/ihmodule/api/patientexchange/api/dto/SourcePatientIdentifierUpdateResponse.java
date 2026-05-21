package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

import java.util.List;

/**
 * Result of upserting a source patient identifier on a facility OpenMRS patient.
 */
public class SourcePatientIdentifierUpdateResponse {
	
	private String status;
	
	private String patientUuid;
	
	private String sourcePatientIdentifierValue;
	
	/** {@code created} or {@code updated}. */
	private String operation;
	
	private String identifierTypeUuid;
	
	private String identifierTypeName;
	
	private List<PatientIdentifierSnapshot> identifiers;
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getSourcePatientIdentifierValue() {
		return sourcePatientIdentifierValue;
	}
	
	public void setSourcePatientIdentifierValue(String sourcePatientIdentifierValue) {
		this.sourcePatientIdentifierValue = sourcePatientIdentifierValue;
	}
	
	public String getOperation() {
		return operation;
	}
	
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public String getIdentifierTypeUuid() {
		return identifierTypeUuid;
	}
	
	public void setIdentifierTypeUuid(String identifierTypeUuid) {
		this.identifierTypeUuid = identifierTypeUuid;
	}
	
	public String getIdentifierTypeName() {
		return identifierTypeName;
	}
	
	public void setIdentifierTypeName(String identifierTypeName) {
		this.identifierTypeName = identifierTypeName;
	}
	
	public List<PatientIdentifierSnapshot> getIdentifiers() {
		return identifiers;
	}
	
	public void setIdentifiers(List<PatientIdentifierSnapshot> identifiers) {
		this.identifiers = identifiers;
	}
}
