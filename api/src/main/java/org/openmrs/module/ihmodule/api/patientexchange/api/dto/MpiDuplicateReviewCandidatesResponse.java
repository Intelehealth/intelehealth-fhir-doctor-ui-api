package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Duplicate-review candidates for one case, split by match origin (OpenMRS vs FHIR/central).
 */
public class MpiDuplicateReviewCandidatesResponse {
	
	private List<MpiDuplicateReviewCandidateDto> openmrsCandidates = new ArrayList<>();
	
	private List<MpiDuplicateReviewCandidateDto> fhirCandidates = new ArrayList<>();
	
	public List<MpiDuplicateReviewCandidateDto> getOpenmrsCandidates() {
		return openmrsCandidates;
	}
	
	public void setOpenmrsCandidates(List<MpiDuplicateReviewCandidateDto> openmrsCandidates) {
		this.openmrsCandidates = openmrsCandidates != null ? openmrsCandidates : new ArrayList<>();
	}
	
	public List<MpiDuplicateReviewCandidateDto> getFhirCandidates() {
		return fhirCandidates;
	}
	
	public void setFhirCandidates(List<MpiDuplicateReviewCandidateDto> fhirCandidates) {
		this.fhirCandidates = fhirCandidates != null ? fhirCandidates : new ArrayList<>();
	}
}
