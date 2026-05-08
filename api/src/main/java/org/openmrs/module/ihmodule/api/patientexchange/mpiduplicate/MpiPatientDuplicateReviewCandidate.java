package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 * One FHIR Patient match from an ambiguous MPI search. Demographics mirror the search dimensions
 * used against the central server (birthdate, family, given, gender, telecom), plus server
 * identifiers for manual mapping.
 */
public class MpiPatientDuplicateReviewCandidate {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_case_id", nullable = false)
	private MpiPatientDuplicateReviewCase reviewCase;
	
	@Column(name = "fhir_patient_logical_id", nullable = false, length = 128)
	private String fhirPatientLogicalId;
	
	@Column(name = "mpi_identifier_value", length = 256)
	private String mpiIdentifierValue;
	
	@Column(name = "candidate_birthdate", length = 32)
	private String candidateBirthdate;
	
	@Column(name = "candidate_family", length = 512)
	private String candidateFamily;
	
	@Column(name = "candidate_given", length = 512)
	private String candidateGiven;
	
	@Column(name = "candidate_gender_code", length = 16)
	private String candidateGenderCode;
	
	@Column(name = "candidate_telecom", length = 256)
	private String candidateTelecom;
	
	@Lob
	@Column(name = "candidate_address_lines", columnDefinition = "LONGTEXT")
	private String candidateAddressLines;
	
	@Column(name = "candidate_address_city", length = 256)
	private String candidateAddressCity;
	
	@Column(name = "candidate_address_district", length = 256)
	private String candidateAddressDistrict;
	
	@Column(name = "candidate_address_state", length = 256)
	private String candidateAddressState;
	
	@Column(name = "candidate_address_postal_code", length = 64)
	private String candidateAddressPostalCode;
	
	@Column(name = "candidate_address_country", length = 128)
	private String candidateAddressCountry;
	
	@Lob
	@Column(name = "candidate_address_snapshot", columnDefinition = "LONGTEXT")
	private String candidateAddressSnapshot;
	
	@Lob
	@Column(name = "patient_resource_json", columnDefinition = "LONGTEXT")
	private String patientResourceJson;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public MpiPatientDuplicateReviewCase getReviewCase() {
		return reviewCase;
	}
	
	public void setReviewCase(MpiPatientDuplicateReviewCase reviewCase) {
		this.reviewCase = reviewCase;
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
	
	public String getCandidateAddressLines() {
		return candidateAddressLines;
	}
	
	public void setCandidateAddressLines(String candidateAddressLines) {
		this.candidateAddressLines = candidateAddressLines;
	}
	
	public String getCandidateAddressCity() {
		return candidateAddressCity;
	}
	
	public void setCandidateAddressCity(String candidateAddressCity) {
		this.candidateAddressCity = candidateAddressCity;
	}
	
	public String getCandidateAddressDistrict() {
		return candidateAddressDistrict;
	}
	
	public void setCandidateAddressDistrict(String candidateAddressDistrict) {
		this.candidateAddressDistrict = candidateAddressDistrict;
	}
	
	public String getCandidateAddressState() {
		return candidateAddressState;
	}
	
	public void setCandidateAddressState(String candidateAddressState) {
		this.candidateAddressState = candidateAddressState;
	}
	
	public String getCandidateAddressPostalCode() {
		return candidateAddressPostalCode;
	}
	
	public void setCandidateAddressPostalCode(String candidateAddressPostalCode) {
		this.candidateAddressPostalCode = candidateAddressPostalCode;
	}
	
	public String getCandidateAddressCountry() {
		return candidateAddressCountry;
	}
	
	public void setCandidateAddressCountry(String candidateAddressCountry) {
		this.candidateAddressCountry = candidateAddressCountry;
	}
	
	public String getCandidateAddressSnapshot() {
		return candidateAddressSnapshot;
	}
	
	public void setCandidateAddressSnapshot(String candidateAddressSnapshot) {
		this.candidateAddressSnapshot = candidateAddressSnapshot;
	}
	
	public String getPatientResourceJson() {
		return patientResourceJson;
	}
	
	public void setPatientResourceJson(String patientResourceJson) {
		this.patientResourceJson = patientResourceJson;
	}
}
