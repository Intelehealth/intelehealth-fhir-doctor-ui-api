package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewCandidateDto;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.MpiDuplicateReviewCandidatesResponse;
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
	public MpiDuplicateReviewCandidatesResponse listCandidatesForCaseUuid(String caseUuid) {
		MpiPatientDuplicateReviewCase reviewCase = caseRepository.findByCaseUuid(caseUuid).orElseThrow(
		    () -> new IllegalArgumentException("Unknown caseUuid: " + caseUuid));
		List<MpiPatientDuplicateReviewCandidate> rows = candidateRepository.findByReviewCase_IdOrderByIdAsc(
		    reviewCase.getId());
		List<MpiDuplicateReviewCandidateDto> openmrs = new ArrayList<>();
		List<MpiDuplicateReviewCandidateDto> fhir = new ArrayList<>();
		int hidden = 0;
		for (MpiPatientDuplicateReviewCandidate c : rows) {
			if (!isVisibleInDuplicateReviewList(c)) {
				hidden++;
				continue;
			}
			MpiDuplicateReviewCandidateDto d = toCandidate(c, reviewCase);
			if (isOpenmrsMatchSource(c.getMatchSource())) {
				openmrs.add(d);
			} else {
				fhir.add(d);
			}
		}
		MpiDuplicateReviewCandidatesResponse out = new MpiDuplicateReviewCandidatesResponse();
		out.setOpenmrsCandidates(openmrs);
		out.setFhirCandidates(fhir);
		out.setTotalStoredCount(rows.size());
		out.setVisibleCount(openmrs.size() + fhir.size());
		out.setHiddenCount(hidden);
		return out;
	}
	
	private static boolean isOpenmrsMatchSource(String matchSource) {
		return matchSource != null && MpiImportDuplicateReviewSource.OPENMRS.getValue().equalsIgnoreCase(matchSource.trim());
	}
	
	private int countVisibleCandidatesForCase(Long reviewCaseId, Integer storedCountFallback) {
		if (reviewCaseId == null) {
			return storedCountFallback != null ? storedCountFallback : 0;
		}
		List<MpiPatientDuplicateReviewCandidate> rows = candidateRepository.findByReviewCase_IdOrderByIdAsc(reviewCaseId);
		int visible = 0;
		for (MpiPatientDuplicateReviewCandidate c : rows) {
			if (isVisibleInDuplicateReviewList(c)) {
				visible++;
			}
		}
		return visible;
	}
	
	private static boolean isVisibleInDuplicateReviewList(MpiPatientDuplicateReviewCandidate candidate) {
		if (candidate == null) {
			return false;
		}
		MpiDuplicateReviewStatus status = candidate.getReviewStatus();
		return status == null || status == MpiDuplicateReviewStatus.PENDING;
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
		d.setCandidateCount(countVisibleCandidatesForCase(c.getId(), c.getCandidateCount()));
		d.setReviewStatus(c.getReviewStatus() != null ? c.getReviewStatus().name() : null);
		d.setDateCreated(c.getDateCreated());
		d.setSourceOfPatient(c.getSourceOfPatient());
		return d;
	}
	
	private MpiDuplicateReviewCandidateDto toCandidate(MpiPatientDuplicateReviewCandidate c,
	        MpiPatientDuplicateReviewCase reviewCase) {
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
		d.setMatchType(c.getMatchType());
		d.setSourceOfPatient(resolveSourceOfPatientForCandidate(reviewCase, c));
		return d;
	}
	
	private static String resolveSourceOfPatientForCandidate(MpiPatientDuplicateReviewCase reviewCase,
	        MpiPatientDuplicateReviewCandidate c) {
		if (reviewCase != null && StringUtils.isNotBlank(reviewCase.getSourceOfPatient())) {
			return reviewCase.getSourceOfPatient().trim();
		}
		if (c != null && StringUtils.isNotBlank(c.getMatchSource())) {
			return c.getMatchSource().trim();
		}
		return null;
	}
	
	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}
}
