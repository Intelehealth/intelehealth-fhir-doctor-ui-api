package org.openmrs.module.ihmodule.api.patientmatch.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.config.DobRepositoryFilterMode;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticEncodingService;

public class PatientFuzzyMatchingEngineTest {
	
	@Test
	public void score_shouldRewardStrongNameDobAndPhoneMatches() throws Exception {
		PatientFuzzyMatchingEngine engine = new PatientFuzzyMatchingEngine();
		Field aggregatorField = PatientFuzzyMatchingEngine.class.getDeclaredField("weightedScoreAggregator");
		aggregatorField.setAccessible(true);
		aggregatorField.set(engine, new WeightedScoreAggregator());
		Field phoneticField = PatientFuzzyMatchingEngine.class.getDeclaredField("phoneticEncodingService");
		phoneticField.setAccessible(true);
		phoneticField.set(engine, new PhoneticEncodingService());
		
		Map<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
		enabled.put("name", Boolean.TRUE);
		enabled.put("dob", Boolean.TRUE);
		enabled.put("phone", Boolean.TRUE);
		enabled.put("address", Boolean.TRUE);
		enabled.put("gender", Boolean.TRUE);
		enabled.put("identifier", Boolean.TRUE);
		
		Map<String, Double> weights = new LinkedHashMap<String, Double>();
		weights.put("name", Double.valueOf(0.45d));
		weights.put("dob", Double.valueOf(0.20d));
		weights.put("phone", Double.valueOf(0.20d));
		weights.put("address", Double.valueOf(0.10d));
		weights.put("gender", Double.valueOf(0.05d));
		weights.put("identifier", Double.valueOf(0.05d));
		
		FuzzyPatientMatchConfig config = new FuzzyPatientMatchConfig(true, 70, 85, 70, 60, 500, "jaro_winkler",
		        "levenshtein", "token_jaccard", true, "DOUBLE_METAPHONE", 0, 95, 80, 60,
		        DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(enabled.keySet()), null, enabled,
		        weights);
		
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setName("John Smith");
		request.setBirthDate(LocalDate.parse("1990-01-01"));
		request.setPhone("01712345678");
		request.setAddress("Dhaka");
		request.setGender("male");
		
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setUuid("patient-uuid");
		candidate.setName("Jon Smyth");
		candidate.setGivenName("Jon");
		candidate.setFamilyName("Smyth");
		candidate.setBirthDate(LocalDate.parse("1990-01-01"));
		candidate.setPhone("01712345678");
		candidate.setAddress("Dhaka Bangladesh");
		candidate.setGender("M");
		
		FuzzyPatientMatchResult result = engine.score(request, candidate, config);
		
		assertTrue(result.getOverallMatchScore() >= 85.0d);
		assertEquals("HIGH", result.getConfidence());
		assertTrue(result.getFieldScores().get("dob").doubleValue() >= 100.0d);
		assertTrue(result.getFieldScores().get("phone").doubleValue() >= 100.0d);
		assertTrue(result.getFieldScores().get("name").doubleValue() >= 75.0d);
	}
}
