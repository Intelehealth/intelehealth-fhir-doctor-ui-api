package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Duplicate-review candidates for one case, split by match origin (OpenMRS vs FHIR/central).
 */
public class MpiDuplicateReviewCandidatesResponse {
	
	private List<MpiDuplicateReviewCandidateDto> openmrsCandidates = new ArrayList<>();
	
	private List<MpiDuplicateReviewCandidateDto> fhirCandidates = new ArrayList<>();
	
	/** All rows stored for the case (including removed/skipped). */
	private int totalStoredCount;
	
	/** Rows returned in {@link #openmrsCandidates} and {@link #fhirCandidates}. */
	private int visibleCount;
	
	/** Stored rows hidden from the duplicate-review list (removed/skipped or completed). */
	private int hiddenCount;
	
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
	
	public int getTotalStoredCount() {
		return totalStoredCount;
	}
	
	public void setTotalStoredCount(int totalStoredCount) {
		this.totalStoredCount = totalStoredCount;
	}
	
	public int getVisibleCount() {
		return visibleCount;
	}
	
	public void setVisibleCount(int visibleCount) {
		this.visibleCount = visibleCount;
	}
	
	public int getHiddenCount() {
		return hiddenCount;
	}
	
	public void setHiddenCount(int hiddenCount) {
		this.hiddenCount = hiddenCount;
	}
}
