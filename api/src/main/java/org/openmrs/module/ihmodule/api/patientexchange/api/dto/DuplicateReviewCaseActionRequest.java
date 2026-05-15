package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Skip or reference a duplicate-review case by {@code case_uuid}.
 */
public class DuplicateReviewCaseActionRequest {
	
	private String caseUuid;
	
	private String resolvedBy;
	
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
