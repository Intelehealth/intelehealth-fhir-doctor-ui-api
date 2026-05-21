package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Skip duplicate-review via {@code POST .../duplicate-review/skip}. When {@code candidateId} is
 * set, only that {@code mpi_patient_duplicate_review_candidate} row is updated (case unchanged).
 * When {@code candidateId} is omitted, the {@code mpi_patient_duplicate_review_case} and all its
 * candidates are marked {@code SKIPPED} (removes the case from the pending source-patient list).
 */
public class DuplicateReviewCaseActionRequest {
	
	private String caseUuid;
	
	private Long candidateId;
	
	private String resolvedBy;
	
	public String getCaseUuid() {
		return caseUuid;
	}
	
	public void setCaseUuid(String caseUuid) {
		this.caseUuid = caseUuid;
	}
	
	public Long getCandidateId() {
		return candidateId;
	}
	
	public void setCandidateId(Long candidateId) {
		this.candidateId = candidateId;
	}
	
	public String getResolvedBy() {
		return resolvedBy;
	}
	
	public void setResolvedBy(String resolvedBy) {
		this.resolvedBy = resolvedBy;
	}
}
