package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Body for operator-triggered export of one patient through the same pipeline as the scheduler (
 * {@code transferCreatedPatient} → fetch local FHIR Patient → validate → MCI → sync identifiers),
 * except duplicate-review deferral is skipped so POST to MCI always proceeds when validation
 * passes.
 */
public class ForcePatientSyncRequest {
	
	private String patientUuid;
	
	/**
	 * When set with {@link #patientUuid}, pending duplicate-review uses
	 * {@code outbound_bundle_json} on the case (import snapshot) instead of only local FHIR fetch
	 * by UUID.
	 */
	private String caseUuid;
	
	/**
	 * Required; recorded on {@code mpi_patient_duplicate_review_case.resolved_by} when a pending
	 * case exists.
	 */
	private String resolvedBy;
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getCaseUuid() {
		return caseUuid;
	}
	
	public void setCaseUuid(String caseUuid) {
		this.caseUuid = caseUuid;
	}
	
	public String getResolvedBy() {
		return resolvedBy;
	}
	
	public void setResolvedBy(String resolvedBy) {
		this.resolvedBy = resolvedBy;
	}
}
