package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * One central MPI duplicate candidate for a review case.
 */
public class MpiDuplicateReviewCandidateDto {
	
	private Long id;
	
	private String fhirPatientLogicalId;
	
	private String mpiIdentifierValue;
	
	private String candidateBirthdate;
	
	private String candidateFamily;
	
	private String candidateGiven;
	
	private String candidateGenderCode;
	
	private String candidateTelecom;
	
	private String candidateAddressCity;
	
	private String candidateAddressSnapshot;
	
	private Double matchScore;
	
	private String matchSource;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getFhirPatientLogicalId() {
		return fhirPatientLogicalId;
	}
	
	public void setFhirPatientLogicalId(String fhirPatientLogicalId) {
		this.fhirPatientLogicalId = fhirPatientLogicalId;
	}
	
	public String getMpiIdentifierValue() {
		return mpiIdentifierValue;
	}
	
	public void setMpiIdentifierValue(String mpiIdentifierValue) {
		this.mpiIdentifierValue = mpiIdentifierValue;
	}
	
	public String getCandidateBirthdate() {
		return candidateBirthdate;
	}
	
	public void setCandidateBirthdate(String candidateBirthdate) {
		this.candidateBirthdate = candidateBirthdate;
	}
	
	public String getCandidateFamily() {
		return candidateFamily;
	}
	
	public void setCandidateFamily(String candidateFamily) {
		this.candidateFamily = candidateFamily;
	}
	
	public String getCandidateGiven() {
		return candidateGiven;
	}
	
	public void setCandidateGiven(String candidateGiven) {
		this.candidateGiven = candidateGiven;
	}
	
	public String getCandidateGenderCode() {
		return candidateGenderCode;
	}
	
	public void setCandidateGenderCode(String candidateGenderCode) {
		this.candidateGenderCode = candidateGenderCode;
	}
	
	public String getCandidateTelecom() {
		return candidateTelecom;
	}
	
	public void setCandidateTelecom(String candidateTelecom) {
		this.candidateTelecom = candidateTelecom;
	}
	
	public String getCandidateAddressCity() {
		return candidateAddressCity;
	}
	
	public void setCandidateAddressCity(String candidateAddressCity) {
		this.candidateAddressCity = candidateAddressCity;
	}
	
	public String getCandidateAddressSnapshot() {
		return candidateAddressSnapshot;
	}
	
	public void setCandidateAddressSnapshot(String candidateAddressSnapshot) {
		this.candidateAddressSnapshot = candidateAddressSnapshot;
	}
	
	public Double getMatchScore() {
		return matchScore;
	}
	
	public void setMatchScore(Double matchScore) {
		this.matchScore = matchScore;
	}
	
	public String getMatchSource() {
		return matchSource;
	}
	
	public void setMatchSource(String matchSource) {
		this.matchSource = matchSource;
	}
}
