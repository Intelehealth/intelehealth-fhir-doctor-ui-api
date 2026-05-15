package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.hl7.fhir.r4.model.Patient;

/**
 * One fuzzy / MDM match row before persistence (patient + Bundle.search score + originating API).
 */
public final class MpiDuplicateReviewCandidateMatchRow {
	
	private final Patient patient;
	
	private final String matchSource;
	
	private final Double matchScore;
	
	public MpiDuplicateReviewCandidateMatchRow(Patient patient, String matchSource, Double matchScore) {
		this.patient = patient;
		this.matchSource = matchSource;
		this.matchScore = matchScore;
	}
	
	public Patient getPatient() {
		return patient;
	}
	
	public String getMatchSource() {
		return matchSource;
	}
	
	public Double getMatchScore() {
		return matchScore;
	}
}
