package org.openmrs.module.ihmodule.page.model;

/**
 * One row in the duplicate-patient review table (replace with API-backed results later).
 */
public class DuplicatePatientCandidate {
	
	private final String patientA;
	
	private final String patientB;
	
	private final String sharedIdentifier;
	
	private final String matchScore;
	
	private final String notes;
	
	public DuplicatePatientCandidate(String patientA, String patientB, String sharedIdentifier, String matchScore,
	    String notes) {
		this.patientA = patientA;
		this.patientB = patientB;
		this.sharedIdentifier = sharedIdentifier;
		this.matchScore = matchScore;
		this.notes = notes;
	}
	
	public String getPatientA() {
		return patientA;
	}
	
	public String getPatientB() {
		return patientB;
	}
	
	public String getSharedIdentifier() {
		return sharedIdentifier;
	}
	
	public String getMatchScore() {
		return matchScore;
	}
	
	public String getNotes() {
		return notes;
	}
}
