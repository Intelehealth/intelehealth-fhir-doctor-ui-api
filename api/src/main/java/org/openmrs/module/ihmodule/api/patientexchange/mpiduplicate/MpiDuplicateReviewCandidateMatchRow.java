package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.hl7.fhir.r4.model.Patient;

/**
 * One fuzzy / MDM match row before persistence (patient + Bundle.search score + originating API).
 */
public final class MpiDuplicateReviewCandidateMatchRow {
	
	private final Patient patient;
	
	private final String matchSource;
	
	private final Double matchScore;
	
	/** FHIR match-grade {@code valueCode}; null when not supplied (e.g. OpenMRS fuzzy path). */
	private final String matchType;
	
	public MpiDuplicateReviewCandidateMatchRow(Patient patient, String matchSource, Double matchScore, String matchType) {
		this.patient = patient;
		this.matchSource = matchSource;
		this.matchScore = matchScore;
		this.matchType = matchType;
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
	
	public String getMatchType() {
		return matchType;
	}
}
