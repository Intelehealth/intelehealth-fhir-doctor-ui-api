package org.openmrs.module.ihmodule.api.patientmatch.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.config.DobRepositoryFilterMode;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfigService;
import org.openmrs.module.ihmodule.api.patientmatch.config.PatientMatchRules;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;
import org.openmrs.module.ihmodule.api.patientmatch.engine.PatientFuzzyMatchingEngine;
import org.openmrs.module.ihmodule.api.patientmatch.engine.WeightedScoreAggregator;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticEncodingService;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MatchResultMapScenarioTest {
	
	private static final String RULES_JSON = "{"
	        + "\"matchFields\":["
	        + "{\"name\":\"given-name-jw\",\"resourcePath\":\"name.given\",\"similarity\":{\"algorithm\":\"JARO_WINKLER\",\"matchThreshold\":0.8}},"
	        + "{\"name\":\"family-name-jw\",\"resourcePath\":\"name.family\",\"similarity\":{\"algorithm\":\"JARO_WINKLER\",\"matchThreshold\":0.8}},"
	        + "{\"name\":\"birthdate-exact\",\"resourcePath\":\"birthDate\",\"matcher\":{\"algorithm\":\"STRING\"}},"
	        + "{\"name\":\"phone-exact\",\"resourcePath\":\"telecom.value\",\"matcher\":{\"algorithm\":\"STRING\"}},"
	        + "{\"name\":\"gender-exact\",\"resourcePath\":\"gender\",\"matcher\":{\"algorithm\":\"STRING\"}}"
	        + "],"
	        + "\"matchResultMap\":{"
	        + "\"national-id-exact\":\"MATCH\","
	        + "\"given-name-jw\":\"POSSIBLE_MATCH\","
	        + "\"family-name-jw\":\"POSSIBLE_MATCH\","
	        + "\"phone-exact\":\"MATCH\","
	        + "\"given-name-jw,family-name-jw\":\"POSSIBLE_MATCH\","
	        + "\"given-name-jw,family-name-jw,birthdate-exact,phone-exact\":\"MATCH\","
	        + "\"given-name-jw,family-name-jw,birthdate-exact,gender-exact\":\"MATCH\","
	        + "\"given-name-jw,family-name-jw,phone-exact\":\"POSSIBLE_MATCH\","
	        + "\"given-name-metaphone,family-name-jw,birthdate-exact,phone-exact\":\"POSSIBLE_MATCH\""
	        + "},"
	        + "\"settings\":{\"fieldMatchThreshold\":60,\"matchGradeThresholds\":{\"certain\":0.95,\"probable\":0.80,\"possible\":0.60}}"
	        + "}";
	
	@Test
	public void mayezScenario_withUserMatchResultMap() throws Exception {
		PatientMatchRules rules = new ObjectMapper().readValue(RULES_JSON, PatientMatchRules.class);
		FuzzyPatientMatchConfig config = new FuzzyPatientMatchConfigService().getConfig();
		config = configWithRules(config, rules);
		
		FuzzyPatientMatchRequest request = mayezRequest();
		MatchRuleComboEvaluator evaluator = new MatchRuleComboEvaluator();
		PatientFuzzyMatchingEngine engine = newEngine();
		FhirPatientMatchService service = new FhirPatientMatchService();
		Method toMatchGrade = FhirPatientMatchService.class.getDeclaredMethod("toMatchGrade", FuzzyPatientMatchResult.class,
		    FuzzyPatientMatchConfig.class);
		toMatchGrade.setAccessible(true);
		
		FuzzyPatientCandidate rafad = candidate("rafad", "Mayez", "Rafad");
		FuzzyPatientCandidate faruk = candidate("faruk", "Mayez", "Faruk");
		
		String rafadRule = evaluator.resolveMatchResultLabel(request, rafad, config);
		FuzzyPatientMatchResult rafadScore = engine.score(request, rafad, config);
		rafadScore.setRuleMatchResult(rafadRule);
		String rafadGrade = (String) toMatchGrade.invoke(service, rafadScore, config);
		
		String farukRule = evaluator.resolveMatchResultLabel(request, faruk, config);
		FuzzyPatientMatchResult farukScore = engine.score(request, faruk, config);
		farukScore.setRuleMatchResult(farukRule);
		String farukGrade = (String) toMatchGrade.invoke(service, farukScore, config);
		
		System.out.println("Rafad rule=" + rafadRule + " score=" + rafadScore.getOverallMatchScore() + " normalized="
		        + rafadScore.getNormalizedMatchScore() + " grade=" + rafadGrade + " nameScore="
		        + rafadScore.getFieldScores().get("name"));
		System.out.println("Faruk rule=" + farukRule + " score=" + farukScore.getOverallMatchScore() + " normalized="
		        + farukScore.getNormalizedMatchScore() + " grade=" + farukGrade + " nameScore="
		        + farukScore.getFieldScores().get("name"));
		
		assertEquals("MATCH", rafadRule);
		assertEquals("MATCH", farukRule);
		assertTrue(rafadScore.getOverallMatchScore() < 95.0d);
		assertTrue(farukScore.getOverallMatchScore() >= 95.0d);
		assertEquals("probable", rafadGrade);
		assertEquals("certain", farukGrade);
	}
	
	private FuzzyPatientMatchRequest mayezRequest() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setGivenName("Mayez");
		request.setFamilyName("Faruk");
		request.setBirthDate(LocalDate.parse("2025-12-25"));
		request.setPhone("+919639393939");
		request.setGender("male");
		return request;
	}
	
	private FuzzyPatientCandidate candidate(String uuid, String given, String family) {
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setUuid(uuid);
		candidate.setName(given + " " + family);
		candidate.setGivenName(given);
		candidate.setFamilyName(family);
		candidate.setBirthDate(LocalDate.parse("2025-12-25"));
		candidate.setPhone("+919639393939");
		candidate.setGender("male");
		return candidate;
	}
	
	private PatientFuzzyMatchingEngine newEngine() throws Exception {
		PatientFuzzyMatchingEngine engine = new PatientFuzzyMatchingEngine();
		Field aggregatorField = PatientFuzzyMatchingEngine.class.getDeclaredField("weightedScoreAggregator");
		aggregatorField.setAccessible(true);
		aggregatorField.set(engine, new WeightedScoreAggregator());
		Field phoneticField = PatientFuzzyMatchingEngine.class.getDeclaredField("phoneticEncodingService");
		phoneticField.setAccessible(true);
		phoneticField.set(engine, new PhoneticEncodingService());
		return engine;
	}
	
	private FuzzyPatientMatchConfig configWithRules(FuzzyPatientMatchConfig base, PatientMatchRules rules) {
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
		return new FuzzyPatientMatchConfig(base.isEnabled(), base.getThreshold(), base.getConfidenceHighThreshold(),
		        base.getConfidenceMediumThreshold(), 60, base.getMaxCandidates(), base.getNameAlgorithm(),
		        base.getPhoneAlgorithm(), base.getAddressAlgorithm(), base.isPhoneticBoostEnabled(),
		        base.getPhoneticBoostAlgorithm(), base.getDobNearMatchDays(), 95, 80, 60,
		        DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(enabled.keySet()), rules, enabled,
		        weights, base.getGivenNamePartWeight(), base.getFamilyNamePartWeight(), false);
	}
}
