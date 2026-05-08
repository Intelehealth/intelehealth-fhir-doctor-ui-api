package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * API view of a duplicate-review header row (no large JSON blobs).
 */
public class MpiDuplicateReviewCaseSummaryDto {
	
	private Long id;
	
	private String caseUuid;
	
	private String localPatientUuid;
	
	private String sourceBirthdate;
	
	private String sourceFamily;
	
	private String sourceGiven;
	
	private String sourceGenderCode;
	
	private String sourceTelecom;
	
	/**
	 * Address snapshot for the source (local) patient — same shape as
	 * {@code candidate_address_snapshot} on candidate rows (multi-line text).
	 */
	private String candidateAddressSnapshot;
	
	private Integer candidateCount;
	
	private String reviewStatus;
	
	private String dateCreated;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getCaseUuid() {
		return caseUuid;
	}
	
	public void setCaseUuid(String caseUuid) {
		this.caseUuid = caseUuid;
	}
	
	public String getLocalPatientUuid() {
		return localPatientUuid;
	}
	
	public void setLocalPatientUuid(String localPatientUuid) {
		this.localPatientUuid = localPatientUuid;
	}
	
	public String getSourceBirthdate() {
		return sourceBirthdate;
	}
	
	public void setSourceBirthdate(String sourceBirthdate) {
		this.sourceBirthdate = sourceBirthdate;
	}
	
	public String getSourceFamily() {
		return sourceFamily;
	}
	
	public void setSourceFamily(String sourceFamily) {
		this.sourceFamily = sourceFamily;
	}
	
	public String getSourceGiven() {
		return sourceGiven;
	}
	
	public void setSourceGiven(String sourceGiven) {
		this.sourceGiven = sourceGiven;
	}
	
	public String getSourceGenderCode() {
		return sourceGenderCode;
	}
	
	public void setSourceGenderCode(String sourceGenderCode) {
		this.sourceGenderCode = sourceGenderCode;
	}
	
	public String getSourceTelecom() {
		return sourceTelecom;
	}
	
	public void setSourceTelecom(String sourceTelecom) {
		this.sourceTelecom = sourceTelecom;
	}
	
	public String getCandidateAddressSnapshot() {
		return candidateAddressSnapshot;
	}
	
	public void setCandidateAddressSnapshot(String candidateAddressSnapshot) {
		this.candidateAddressSnapshot = candidateAddressSnapshot;
	}
	
	public Integer getCandidateCount() {
		return candidateCount;
	}
	
	public void setCandidateCount(Integer candidateCount) {
		this.candidateCount = candidateCount;
	}
	
	public String getReviewStatus() {
		return reviewStatus;
	}
	
	public void setReviewStatus(String reviewStatus) {
		this.reviewStatus = reviewStatus;
	}
	
	public String getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}
}
