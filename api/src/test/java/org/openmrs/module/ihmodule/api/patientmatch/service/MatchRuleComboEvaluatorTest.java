package org.openmrs.module.ihmodule.api.patientmatch.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.config.DobRepositoryFilterMode;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.PatientMatchRules;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;

public class MatchRuleComboEvaluatorTest {
	
	private final MatchRuleComboEvaluator evaluator = new MatchRuleComboEvaluator();
	
	@Test
	public void isEligible_shouldRejectPhoneOnlyFalsePositiveWithoutConfiguredCombo() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setGivenName("Craig");
		request.setFamilyName("Kaden");
		request.setBirthDate(LocalDate.parse("2024-07-06"));
		request.setPhone("01712345678");
		
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setGivenName("Momina");
		candidate.setFamilyName("Khatun");
		candidate.setName("Momina Khatun");
		candidate.setBirthDate(LocalDate.parse("1995-01-07"));
		candidate.setPhone("01712345678");
		
		assertFalse(evaluator.isEligible(request, candidate, config()));
	}
	
	@Test
	public void isEligible_shouldAcceptStrongNameAndPhoneCombo() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setGivenName("Craig");
		request.setFamilyName("Kaden");
		request.setBirthDate(LocalDate.parse("2024-07-06"));
		request.setPhone("01712345678");
		
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setGivenName("Craig");
		candidate.setFamilyName("Kaden");
		candidate.setName("Craig Kaden");
		candidate.setBirthDate(LocalDate.parse("2023-07-06"));
		candidate.setPhone("01712345678");
		
		assertTrue(evaluator.isEligible(request, candidate, config()));
	}
	
	@Test
	public void isEligible_shouldAcceptCompoundFamilyNameWhenBestTokenAndNearDobMatch() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setGivenName("Craig");
		request.setFamilyName("khaden");
		request.setBirthDate(LocalDate.parse("2024-06-06"));
		
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setGivenName("Craig");
		candidate.setFamilyName("Kaden Ratliff");
		candidate.setName("Craig Renee Dejesus Kaden Ratliff");
		candidate.setBirthDate(LocalDate.parse("2024-06-05"));
		
		assertTrue(evaluator.isEligible(request, candidate, nearDobConfig()));
	}
	
	@Test
	public void resolveMatchResultLabel_shouldPreferMatchOverPossibleMatch() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setGivenName("Craig");
		request.setFamilyName("Kaden");
		request.setBirthDate(LocalDate.parse("2024-06-06"));
		request.setPhone("01712345678");
		
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setGivenName("Craig");
		candidate.setFamilyName("Kaden");
		candidate.setName("Craig Kaden");
		candidate.setBirthDate(LocalDate.parse("2024-06-06"));
		candidate.setPhone("01712345678");
		
		assertEquals("MATCH", evaluator.resolveMatchResultLabel(request, candidate, matchAndPossibleConfig()));
	}
	
	private FuzzyPatientMatchConfig config() {
		LinkedHashMap<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
		enabled.put("name", Boolean.TRUE);
		enabled.put("dob", Boolean.TRUE);
		enabled.put("phone", Boolean.TRUE);
		enabled.put("address", Boolean.TRUE);
		enabled.put("gender", Boolean.TRUE);
		enabled.put("identifier", Boolean.TRUE);
		LinkedHashMap<String, Double> weights = new LinkedHashMap<String, Double>();
		weights.put("name", Double.valueOf(0.45d));
		weights.put("dob", Double.valueOf(0.20d));
		weights.put("phone", Double.valueOf(0.20d));
		weights.put("address", Double.valueOf(0.10d));
		weights.put("gender", Double.valueOf(0.05d));
		weights.put("identifier", Double.valueOf(0.05d));
		return new FuzzyPatientMatchConfig(true, 60, 85, 70, 60, 500, "jaro_winkler", "levenshtein", "token_jaccard", true,
		        0, 95, 80, 60, DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(enabled.keySet()),
		        rules(), enabled, weights);
	}
	
	private PatientMatchRules rules() {
		PatientMatchRules rules = new PatientMatchRules();
		PatientMatchRules.MatchFieldRule first = new PatientMatchRules.MatchFieldRule();
		first.setName("firstname-jaro");
		PatientMatchRules.AlgorithmRule firstSimilarity = new PatientMatchRules.AlgorithmRule();
		firstSimilarity.setAlgorithm("JARO_WINKLER");
		firstSimilarity.setMatchThreshold(Double.valueOf(0.8d));
		first.setSimilarity(firstSimilarity);
		PatientMatchRules.MatchFieldRule last = new PatientMatchRules.MatchFieldRule();
		last.setName("lastname-jaro");
		PatientMatchRules.AlgorithmRule lastSimilarity = new PatientMatchRules.AlgorithmRule();
		lastSimilarity.setAlgorithm("JARO_WINKLER");
		lastSimilarity.setMatchThreshold(Double.valueOf(0.8d));
		last.setSimilarity(lastSimilarity);
		PatientMatchRules.MatchFieldRule phone = new PatientMatchRules.MatchFieldRule();
		phone.setName("phone");
		PatientMatchRules.AlgorithmRule phoneSimilarity = new PatientMatchRules.AlgorithmRule();
		phoneSimilarity.setAlgorithm("LEVENSHTEIN");
		phone.setSimilarity(phoneSimilarity);
		PatientMatchRules.MatchFieldRule birthday = new PatientMatchRules.MatchFieldRule();
		birthday.setName("birthday");
		PatientMatchRules.AlgorithmRule birthdayMatcher = new PatientMatchRules.AlgorithmRule();
		birthdayMatcher.setAlgorithm("STRING");
		birthday.setMatcher(birthdayMatcher);
		rules.setMatchFields(Arrays.asList(first, last, phone, birthday));
		LinkedHashMap<String, Object> matchResultMap = new LinkedHashMap<String, Object>();
		matchResultMap.put("firstname-jaro,lastname-jaro", "POSSIBLE_MATCH");
		matchResultMap.put("firstname-jaro,lastname-jaro,phone", "POSSIBLE_MATCH");
		matchResultMap.put("firstname-jaro,lastname-jaro,birthday", "POSSIBLE_MATCH");
		rules.setMatchResultMap(matchResultMap);
		return rules;
	}
	
	private FuzzyPatientMatchConfig nearDobConfig() {
		LinkedHashMap<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
		enabled.put("name", Boolean.TRUE);
		enabled.put("dob", Boolean.TRUE);
		enabled.put("phone", Boolean.TRUE);
		enabled.put("address", Boolean.TRUE);
		enabled.put("gender", Boolean.TRUE);
		enabled.put("identifier", Boolean.TRUE);
		LinkedHashMap<String, Double> weights = new LinkedHashMap<String, Double>();
		weights.put("name", Double.valueOf(0.45d));
		weights.put("dob", Double.valueOf(0.20d));
		weights.put("phone", Double.valueOf(0.20d));
		weights.put("address", Double.valueOf(0.10d));
		weights.put("gender", Double.valueOf(0.05d));
		weights.put("identifier", Double.valueOf(0.05d));
		return new FuzzyPatientMatchConfig(true, 60, 85, 70, 60, 500, "jaro_winkler", "levenshtein", "token_jaccard", true,
		        1, 95, 80, 60, DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(enabled.keySet()),
		        rules(), enabled, weights);
	}
	
	private FuzzyPatientMatchConfig matchAndPossibleConfig() {
		LinkedHashMap<String, Boolean> enabled = new LinkedHashMap<String, Boolean>();
		enabled.put("name", Boolean.TRUE);
		enabled.put("dob", Boolean.TRUE);
		enabled.put("phone", Boolean.TRUE);
		enabled.put("address", Boolean.TRUE);
		enabled.put("gender", Boolean.TRUE);
		enabled.put("identifier", Boolean.TRUE);
		LinkedHashMap<String, Double> weights = new LinkedHashMap<String, Double>();
		weights.put("name", Double.valueOf(0.45d));
		weights.put("dob", Double.valueOf(0.20d));
		weights.put("phone", Double.valueOf(0.20d));
		weights.put("address", Double.valueOf(0.10d));
		weights.put("gender", Double.valueOf(0.05d));
		weights.put("identifier", Double.valueOf(0.05d));
		PatientMatchRules rules = rules();
		LinkedHashMap<String, Object> matchResultMap = new LinkedHashMap<String, Object>();
		matchResultMap.put("firstname-jaro,lastname-jaro", "POSSIBLE_MATCH");
		matchResultMap.put("firstname-jaro,lastname-jaro,birthday", "MATCH");
		rules.setMatchResultMap(matchResultMap);
		return new FuzzyPatientMatchConfig(true, 60, 85, 70, 60, 500, "jaro_winkler", "levenshtein", "token_jaccard", true,
		        1, 95, 80, 60, DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(enabled.keySet()),
		        rules, enabled, weights);
	}
}
