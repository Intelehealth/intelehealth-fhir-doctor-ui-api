package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Body for manually attaching or correcting the MPI identifier on the local OpenMRS FHIR Patient,
 * mirroring what happens after a successful central sync when {@code syncPatientToLocal} merges
 * identifiers.
 */
public class LocalMpiUpdateRequest {
	
	private String patientUuid;
	
	private String mpiIdentifierValue;
	
	/** Optional central FHIR Patient logical id when known (stored on duplicate-review case). */
	private String chosenFhirPatientLogicalId;
	
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
	
	public String getMpiIdentifierValue() {
		return mpiIdentifierValue;
	}
	
	public void setMpiIdentifierValue(String mpiIdentifierValue) {
		this.mpiIdentifierValue = mpiIdentifierValue;
	}
	
	public String getChosenFhirPatientLogicalId() {
		return chosenFhirPatientLogicalId;
	}
	
	public void setChosenFhirPatientLogicalId(String chosenFhirPatientLogicalId) {
		this.chosenFhirPatientLogicalId = chosenFhirPatientLogicalId;
	}
	
	public String getResolvedBy() {
		return resolvedBy;
	}
	
	public void setResolvedBy(String resolvedBy) {
		this.resolvedBy = resolvedBy;
	}
}
