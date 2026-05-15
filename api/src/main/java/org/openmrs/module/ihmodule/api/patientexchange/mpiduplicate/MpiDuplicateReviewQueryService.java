package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.List;
import java.util.stream.Collectors;

import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewCandidateDto;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewCaseSummaryDto;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewStatisticsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MpiDuplicateReviewQueryService {
	
	@Autowired
	private MpiPatientDuplicateReviewCaseRepository caseRepository;
	
	@Autowired
	private MpiPatientDuplicateReviewCandidateRepository candidateRepository;
	
	@Transactional(readOnly = true)
	public List<MpiDuplicateReviewCaseSummaryDto> listPendingCases() {
		return caseRepository.findByReviewStatusOrderByDateCreatedDesc(MpiDuplicateReviewStatus.PENDING).stream()
				.map(this::toSummary)
				.collect(Collectors.toList());
	}
	
	@Transactional(readOnly = true)
	public MpiDuplicateReviewStatisticsDto getCaseStatistics() {
		MpiDuplicateReviewStatisticsDto d = new MpiDuplicateReviewStatisticsDto();
		d.setTotalCases(caseRepository.count());
		d.setPendingCount(caseRepository.countByReviewStatus(MpiDuplicateReviewStatus.PENDING));
		d.setResolvedLinkExistingCount(caseRepository.countByReviewStatus(MpiDuplicateReviewStatus.RESOLVED_LINK_EXISTING));
		d.setResolvedForceSendCount(caseRepository.countByReviewStatus(MpiDuplicateReviewStatus.RESOLVED_FORCE_SEND));
		d.setCancelledCount(caseRepository.countByReviewStatus(MpiDuplicateReviewStatus.CANCELLED));
		return d;
	}
	
	@Transactional(readOnly = true)
	public List<MpiDuplicateReviewCandidateDto> listCandidatesForCaseUuid(String caseUuid) {
		return caseRepository.findByCaseUuid(caseUuid)
				.map(c -> candidateRepository.findByReviewCase_IdOrderByIdAsc(c.getId()).stream()
						.map(this::toCandidate)
						.collect(Collectors.toList()))
				.orElseThrow(() -> new IllegalArgumentException("Unknown caseUuid: " + caseUuid));
	}
	
	private MpiDuplicateReviewCaseSummaryDto toSummary(MpiPatientDuplicateReviewCase c) {
		MpiDuplicateReviewCaseSummaryDto d = new MpiDuplicateReviewCaseSummaryDto();
		d.setId(c.getId());
		d.setCaseUuid(c.getCaseUuid());
		d.setLocalPatientUuid(c.getLocalPatientUuid());
		d.setSourceBirthdate(c.getSourceBirthdate());
		d.setSourceFamily(c.getSourceFamily());
		d.setSourceGiven(c.getSourceGiven());
		d.setSourceGenderCode(c.getSourceGenderCode());
		d.setSourceTelecom(c.getSourceTelecom());
		d.setCandidateAddressSnapshot(truncate(c.getSourceAddressSnapshot(), 4000));
		d.setCandidateCount(c.getCandidateCount());
		d.setReviewStatus(c.getReviewStatus() != null ? c.getReviewStatus().name() : null);
		d.setDateCreated(c.getDateCreated());
		d.setSourceOfPatient(c.getSourceOfPatient());
		return d;
	}
	
	private MpiDuplicateReviewCandidateDto toCandidate(MpiPatientDuplicateReviewCandidate c) {
		MpiDuplicateReviewCandidateDto d = new MpiDuplicateReviewCandidateDto();
		d.setId(c.getId());
		d.setFhirPatientLogicalId(c.getFhirPatientLogicalId());
		d.setMpiIdentifierValue(c.getMpiIdentifierValue());
		d.setCandidateBirthdate(c.getCandidateBirthdate());
		d.setCandidateFamily(c.getCandidateFamily());
		d.setCandidateGiven(c.getCandidateGiven());
		d.setCandidateGenderCode(c.getCandidateGenderCode());
		d.setCandidateTelecom(c.getCandidateTelecom());
		d.setCandidateAddressCity(c.getCandidateAddressCity());
		d.setCandidateAddressSnapshot(truncate(c.getCandidateAddressSnapshot(), 4000));
		d.setMatchScore(c.getMatchScore());
		d.setMatchSource(c.getMatchSource());
		return d;
	}
	
	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}
}
