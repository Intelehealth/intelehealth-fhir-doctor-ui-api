package org.openmrs.module.ihmodule.api.patientexchange.sync;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * Patients not sent to central FHIR while sync was disabled ({@code fhir_module.fhir=false}).
 */
public class UnsyncPatient {
	
	@Id
	@Column(name = "id")
	private Long id;
	
	@Column(name = "patient_uuid", nullable = false, length = 38)
	private String patientUuid;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
}
