package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;

import org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils;

/**
 * Header row when a FHIR Patient search returns more than one match for the same
 * demographic query (birthdate, family, given, gender, telecom). Holds the
 * &quot;searching&quot; patient snapshot plus optional audit payloads for replay.
 */
public class MpiPatientDuplicateReviewCase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "case_uuid", nullable = false, unique = true, length = 64)
	private String caseUuid = UUID.randomUUID().toString();

	/** Local OpenMRS / FHIR2 Patient logical id being synced (source). */
	@Column(name = "local_patient_uuid", nullable = false, length = 128)
	private String localPatientUuid;

	@Column(name = "source_birthdate", length = 32)
	private String sourceBirthdate;

	@Column(name = "source_family", length = 512)
	private String sourceFamily;

	@Column(name = "source_given", length = 512)
	private String sourceGiven;

	@Column(name = "source_gender_code", length = 16)
	private String sourceGenderCode;

	@Column(name = "source_telecom", length = 256)
	private String sourceTelecom;

	/** Street / lines from source Patient.address (first row); multiple lines joined with newline. */
	@Lob
	@Column(name = "source_address_lines", columnDefinition = "LONGTEXT")
	private String sourceAddressLines;

	@Column(name = "source_address_city", length = 256)
	private String sourceAddressCity;

	@Column(name = "source_address_district", length = 256)
	private String sourceAddressDistrict;

	@Column(name = "source_address_state", length = 256)
	private String sourceAddressState;

	@Column(name = "source_address_postal_code", length = 64)
	private String sourceAddressPostalCode;

	@Column(name = "source_address_country", length = 128)
	private String sourceAddressCountry;

	/** Human-readable snapshot of all Address elements on source Patient (blocks separated by "---"). */
	@Lob
	@Column(name = "source_address_snapshot", columnDefinition = "LONGTEXT")
	private String sourceAddressSnapshot;

	@Column(name = "candidate_count", nullable = false)
	private Integer candidateCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "review_status", nullable = false, length = 32)
	private MpiDuplicateReviewStatus reviewStatus = MpiDuplicateReviewStatus.PENDING;

	/** FHIR logical id chosen by operator when linking to an existing Patient. */
	@Column(name = "chosen_fhir_patient_logical_id", length = 128)
	private String chosenFhirPatientLogicalId;

	@Column(name = "chosen_mpi_identifier_value", length = 256)
	private String chosenMpiIdentifierValue;

	@Lob
	@Column(name = "resolution_notes", columnDefinition = "LONGTEXT")
	private String resolutionNotes;

	@Column(name = "resolved_by", length = 128)
	private String resolvedBy;

	@Column(name = "date_created", nullable = false, length = 32)
	private String dateCreated = DateUtils.toFormattedDateNow();

	@Column(name = "date_resolved", length = 32)
	private String dateResolved;

	/** Optional: outbound transaction bundle JSON sent toward MCI (for replay after decision). */
	@Lob
	@Column(name = "outbound_bundle_json", columnDefinition = "LONGTEXT")
	private String outboundBundleJson;

	/** Optional: raw FHIR search Bundle JSON returned from central server. */
	@Lob
	@Column(name = "search_bundle_json", columnDefinition = "LONGTEXT")
	private String searchBundleJson;
	
	/**
	 * Legacy: duplicate detector first hit; new import rows leave this null — use
	 * {@link MpiPatientDuplicateReviewCandidate#getMatchSource()} per candidate instead.
	 */
	@Column(name = "source_of_patient", length = 32)
	private String sourceOfPatient;

	@OneToMany(mappedBy = "reviewCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<MpiPatientDuplicateReviewCandidate> candidates = new ArrayList<>();

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

	public String getSourceAddressLines() {
		return sourceAddressLines;
	}

	public void setSourceAddressLines(String sourceAddressLines) {
		this.sourceAddressLines = sourceAddressLines;
	}

	public String getSourceAddressCity() {
		return sourceAddressCity;
	}

	public void setSourceAddressCity(String sourceAddressCity) {
		this.sourceAddressCity = sourceAddressCity;
	}

	public String getSourceAddressDistrict() {
		return sourceAddressDistrict;
	}

	public void setSourceAddressDistrict(String sourceAddressDistrict) {
		this.sourceAddressDistrict = sourceAddressDistrict;
	}

	public String getSourceAddressState() {
		return sourceAddressState;
	}

	public void setSourceAddressState(String sourceAddressState) {
		this.sourceAddressState = sourceAddressState;
	}

	public String getSourceAddressPostalCode() {
		return sourceAddressPostalCode;
	}

	public void setSourceAddressPostalCode(String sourceAddressPostalCode) {
		this.sourceAddressPostalCode = sourceAddressPostalCode;
	}

	public String getSourceAddressCountry() {
		return sourceAddressCountry;
	}

	public void setSourceAddressCountry(String sourceAddressCountry) {
		this.sourceAddressCountry = sourceAddressCountry;
	}

	public String getSourceAddressSnapshot() {
		return sourceAddressSnapshot;
	}

	public void setSourceAddressSnapshot(String sourceAddressSnapshot) {
		this.sourceAddressSnapshot = sourceAddressSnapshot;
	}

	public Integer getCandidateCount() {
		return candidateCount;
	}

	public void setCandidateCount(Integer candidateCount) {
		this.candidateCount = candidateCount;
	}

	public MpiDuplicateReviewStatus getReviewStatus() {
		return reviewStatus;
	}

	public void setReviewStatus(MpiDuplicateReviewStatus reviewStatus) {
		this.reviewStatus = reviewStatus;
	}

	public String getChosenFhirPatientLogicalId() {
		return chosenFhirPatientLogicalId;
	}

	public void setChosenFhirPatientLogicalId(String chosenFhirPatientLogicalId) {
		this.chosenFhirPatientLogicalId = chosenFhirPatientLogicalId;
	}

	public String getChosenMpiIdentifierValue() {
		return chosenMpiIdentifierValue;
	}

	public void setChosenMpiIdentifierValue(String chosenMpiIdentifierValue) {
		this.chosenMpiIdentifierValue = chosenMpiIdentifierValue;
	}

	public String getResolutionNotes() {
		return resolutionNotes;
	}

	public void setResolutionNotes(String resolutionNotes) {
		this.resolutionNotes = resolutionNotes;
	}

	public String getResolvedBy() {
		return resolvedBy;
	}

	public void setResolvedBy(String resolvedBy) {
		this.resolvedBy = resolvedBy;
	}

	public String getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getDateResolved() {
		return dateResolved;
	}

	public void setDateResolved(String dateResolved) {
		this.dateResolved = dateResolved;
	}

	public String getOutboundBundleJson() {
		return outboundBundleJson;
	}

	public void setOutboundBundleJson(String outboundBundleJson) {
		this.outboundBundleJson = outboundBundleJson;
	}

	public String getSearchBundleJson() {
		return searchBundleJson;
	}

	public void setSearchBundleJson(String searchBundleJson) {
		this.searchBundleJson = searchBundleJson;
	}
	
	public String getSourceOfPatient() {
		return sourceOfPatient;
	}
	
	public void setSourceOfPatient(String sourceOfPatient) {
		this.sourceOfPatient = sourceOfPatient;
	}

	public List<MpiPatientDuplicateReviewCandidate> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<MpiPatientDuplicateReviewCandidate> candidates) {
		this.candidates = candidates;
	}

	public void addCandidate(MpiPatientDuplicateReviewCandidate c) {
		c.setReviewCase(this);
		candidates.add(c);
	}
}
