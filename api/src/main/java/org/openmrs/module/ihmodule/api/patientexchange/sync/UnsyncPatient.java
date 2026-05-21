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
	
	@Column(name = "status", nullable = false, length = 32)
	private String status = UnsyncPatientStatus.PENDING.name();
	
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
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public UnsyncPatientStatus getStatusEnum() {
		if (status == null) {
			return UnsyncPatientStatus.PENDING;
		}
		try {
			return UnsyncPatientStatus.valueOf(status);
		}
		catch (IllegalArgumentException ex) {
			return UnsyncPatientStatus.PENDING;
		}
	}
	
	public void setStatusEnum(UnsyncPatientStatus statusEnum) {
		this.status = statusEnum != null ? statusEnum.name() : UnsyncPatientStatus.PENDING.name();
	}
}
