package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCandidate;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCase;

public class PatientUploadImportFuzzyDuplicateThresholdTest {
	
	@Test
	public void shouldDeferImportForFuzzyDuplicate_shouldDeferWhenScoreAboveThreshold() {
		assertTrue(PatientUploadImportService.shouldDeferImportForFuzzyDuplicate(Double.valueOf(85.0d), 60.0d));
	}
	
	@Test
	public void shouldDeferImportForFuzzyDuplicate_shouldDeferWhenScoreEqualsThreshold() {
		assertTrue(PatientUploadImportService.shouldDeferImportForFuzzyDuplicate(Double.valueOf(60.0d), 60.0d));
	}
	
	@Test
	public void shouldDeferImportForFuzzyDuplicate_shouldContinueWhenScoreBelowThreshold() {
		assertFalse(PatientUploadImportService.shouldDeferImportForFuzzyDuplicate(Double.valueOf(59.9d), 60.0d));
	}
	
	@Test
	public void shouldDeferImportForFuzzyDuplicate_shouldContinueWhenHighestScoreMissing() {
		assertFalse(PatientUploadImportService.shouldDeferImportForFuzzyDuplicate(null, 60.0d));
	}
	
	@Test
	public void resolveHighestCandidateMatchScorePercent_shouldReturnMaxAcrossCandidates() {
		MpiPatientDuplicateReviewCase reviewCase = reviewCaseWithScores(0.45d, 0.82d, 0.61d);
		assertEquals(82.0d, PatientUploadImportService.resolveHighestCandidateMatchScorePercent(reviewCase), 0.01d);
	}
	
	@Test
	public void resolveHighestCandidateMatchScorePercent_shouldReturnNullForEmptyCandidates() {
		MpiPatientDuplicateReviewCase reviewCase = new MpiPatientDuplicateReviewCase();
		reviewCase.setCandidates(Collections.emptyList());
		assertNull(PatientUploadImportService.resolveHighestCandidateMatchScorePercent(reviewCase));
	}
	
	@Test
	public void resolveHighestCandidateMatchScorePercent_shouldReturnNullWhenAllScoresMissing() {
		MpiPatientDuplicateReviewCandidate withoutScore = new MpiPatientDuplicateReviewCandidate();
		MpiPatientDuplicateReviewCase reviewCase = new MpiPatientDuplicateReviewCase();
		reviewCase.setCandidates(Collections.singletonList(withoutScore));
		assertNull(PatientUploadImportService.resolveHighestCandidateMatchScorePercent(reviewCase));
	}
	
	@Test
	public void normalizeThresholdToPercent_shouldAcceptFractionAndPercentForms() {
		assertEquals(60.0d, PatientUploadImportService.normalizeThresholdToPercent(0.6d), 0.01d);
		assertEquals(60.0d, PatientUploadImportService.normalizeThresholdToPercent(60.0d), 0.01d);
	}
	
	@Test
	public void getPatientImportFuzzyDuplicateThreshold_shouldDefaultWhenPropertyMissing() {
		FhirConfig config = new FhirConfig();
		assertEquals(60.0d, config.getPatientImportFuzzyDuplicateThreshold(), 0.01d);
	}
	
	private static MpiPatientDuplicateReviewCase reviewCaseWithScores(Double... normalizedScores) {
		MpiPatientDuplicateReviewCase reviewCase = new MpiPatientDuplicateReviewCase();
		reviewCase.setCandidates(Arrays.stream(normalizedScores).map(score -> {
			MpiPatientDuplicateReviewCandidate candidate = new MpiPatientDuplicateReviewCandidate();
			candidate.setMatchScore(score);
			return candidate;
		}).collect(Collectors.toList()));
		return reviewCase;
	}
}
