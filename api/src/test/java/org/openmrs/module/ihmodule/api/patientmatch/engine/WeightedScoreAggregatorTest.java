package org.openmrs.module.ihmodule.api.patientmatch.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.config.DobRepositoryFilterMode;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;

public class WeightedScoreAggregatorTest {
	
	@Test
	public void aggregate_shouldNormalizeActiveWeightsAndSetConfidence() {
		Map<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
		enabled.put("name", Boolean.TRUE);
		enabled.put("dob", Boolean.TRUE);
		enabled.put("phone", Boolean.TRUE);
		enabled.put("address", Boolean.TRUE);
		enabled.put("gender", Boolean.FALSE);
		enabled.put("identifier", Boolean.FALSE);
		
		Map<String, Double> weights = new LinkedHashMap<String, Double>();
		weights.put("name", Double.valueOf(0.5d));
		weights.put("dob", Double.valueOf(0.3d));
		weights.put("phone", Double.valueOf(0.2d));
		weights.put("address", Double.valueOf(0.1d));
		weights.put("gender", Double.valueOf(0.0d));
		weights.put("identifier", Double.valueOf(0.0d));
		
		FuzzyPatientMatchConfig config = new FuzzyPatientMatchConfig(true, 70, 85, 70, 60, 500, "jaro_winkler",
		        "levenshtein", "token_jaccard", true, "DOUBLE_METAPHONE", 0, 95, 80, 60,
		        DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(enabled.keySet()), null, enabled,
		        weights);
		
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setName("John Smith");
		request.setBirthDate(java.time.LocalDate.parse("1990-01-01"));
		request.setPhone("01712345678");
		
		Map<String, Double> fieldScores = new LinkedHashMap<String, Double>();
		fieldScores.put("name", Double.valueOf(90.0d));
		fieldScores.put("dob", Double.valueOf(100.0d));
		fieldScores.put("phone", Double.valueOf(95.0d));
		fieldScores.put("address", Double.valueOf(10.0d));
		
		FuzzyPatientMatchResult result = new WeightedScoreAggregator().aggregate(new FuzzyPatientCandidate(), request,
		    config, fieldScores);
		
		assertEquals(94.0d, result.getOverallMatchScore(), 0.01d);
		assertEquals("HIGH", result.getConfidence());
		assertTrue(result.getMatchedFields().contains("name"));
		assertTrue(result.getMatchedFields().contains("dob"));
		assertTrue(result.getMatchedFields().contains("phone"));
	}
}
