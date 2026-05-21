package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Add patient from one duplicate-review candidate row.
 */
public class DuplicateReviewCandidateActionRequest {
	
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
