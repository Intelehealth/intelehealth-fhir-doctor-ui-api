package org.openmrs.module.ihmodule.api.patientmatch.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.config.DobRepositoryFilterMode;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.PatientMatchRules;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;

public class FhirPatientMatchServiceTest {
	
	@Test
	public void toBundle_shouldRenderSearchsetEntriesWithMdmGrades() throws Exception {
		FhirPatientMatchService service = new FhirPatientMatchService();
		FuzzyPatientMatchResult certain = result(candidate("patient-certain", "Karim", "Rahman"), 97.0d);
		FuzzyPatientMatchResult probable = result(candidate("patient-probable", "Karim", "Rahaman"), 82.0d);
		FuzzyPatientMatchResult possible = result(candidate("patient-possible", "K", "Rahman"), 61.0d);
		
		Method method = FhirPatientMatchService.class.getDeclaredMethod("toBundle", java.util.List.class, int.class,
		    FuzzyPatientMatchConfig.class);
		method.setAccessible(true);
		Bundle bundle = (Bundle) method.invoke(service, Arrays.asList(certain, probable, possible), 3, config(95, 80, 60));
		
		assertEquals(Bundle.BundleType.SEARCHSET, bundle.getType());
		assertEquals(3, bundle.getEntry().size());
		assertEquals(3, bundle.getTotal());
		assertEntry(bundle.getEntry().get(0), "patient-certain", "certain", 0.97d);
		assertEntry(bundle.getEntry().get(1), "patient-probable", "probable", 0.82d);
		assertEntry(bundle.getEntry().get(2), "patient-possible", "possible", 0.61d);
	}
	
	@Test
	public void toMatchGrade_shouldApplyThresholds() throws Exception {
		FhirPatientMatchService service = new FhirPatientMatchService();
		Method method = FhirPatientMatchService.class.getDeclaredMethod("toMatchGrade", FuzzyPatientMatchResult.class,
		    FuzzyPatientMatchConfig.class);
		method.setAccessible(true);
		
		FuzzyPatientMatchConfig config = config(95, 80, 60);
		assertEquals("certain", method.invoke(service, result(candidate("certain", "Karim", "Rahman"), 95.0d), config));
		assertEquals("probable", method.invoke(service, result(candidate("probable", "Karim", "Rahman"), 80.0d), config));
		assertEquals("possible", method.invoke(service, result(candidate("possible", "Karim", "Rahman"), 60.0d), config));
		assertNull(method.invoke(service, result(candidate("excluded", "Karim", "Rahman"), 59.99d), config));
	}
	
	@Test
	public void toMatchGrade_shouldRespectMatchResultLabels() throws Exception {
		FhirPatientMatchService service = new FhirPatientMatchService();
		Method method = FhirPatientMatchService.class.getDeclaredMethod("toMatchGrade", FuzzyPatientMatchResult.class,
		    FuzzyPatientMatchConfig.class);
		method.setAccessible(true);
		
		FuzzyPatientMatchConfig config = config(95, 80, 60);
		assertEquals("probable",
		    method.invoke(service, result(candidate("match-bumped", "Karim", "Rahman"), 61.0d, "MATCH"), config));
		assertEquals("certain",
		    method.invoke(service, result(candidate("match-certain", "Karim", "Rahman"), 98.0d, "MATCH"), config));
		assertEquals("possible",
		    method.invoke(service, result(candidate("possible-capped", "Karim", "Rahman"), 98.0d, "POSSIBLE_MATCH"), config));
		assertNull(method.invoke(service, result(candidate("possible-too-low", "Karim", "Rahman"), 59.0d, "POSSIBLE_MATCH"),
		    config));
	}
	
	private void assertEntry(BundleEntryComponent entry, String expectedId, String expectedGrade, double expectedScore) {
		assertEquals(expectedId, entry.getResource().getIdElement().getIdPart());
		assertTrue(((org.hl7.fhir.r4.model.Patient) entry.getResource()).getActive());
		assertEquals(Bundle.SearchEntryMode.MATCH, entry.getSearch().getMode());
		assertEquals(expectedScore, entry.getSearch().getScore().doubleValue(), 0.0001d);
		assertEquals(1, entry.getSearch().getExtension().size());
		assertEquals("http://hl7.org/fhir/StructureDefinition/match-grade", entry.getSearch().getExtensionFirstRep()
		        .getUrl());
		assertEquals(expectedGrade, entry.getSearch().getExtensionFirstRep().getValue().primitiveValue());
	}
	
	private FuzzyPatientMatchResult result(FuzzyPatientCandidate candidate, double overallScore) {
		return result(candidate, overallScore, null);
	}
	
	private FuzzyPatientMatchResult result(FuzzyPatientCandidate candidate, double overallScore, String ruleMatchResult) {
		FuzzyPatientMatchResult result = new FuzzyPatientMatchResult(candidate, overallScore, "IGNORED",
		        Collections.<String, Double> emptyMap(), Collections.<String> emptyList());
		result.setRuleMatchResult(ruleMatchResult);
		return result;
	}
	
	private FuzzyPatientCandidate candidate(String uuid, String given, String family) {
		FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
		candidate.setUuid(uuid);
		candidate.setIdentifier("MRN-" + uuid);
		candidate.setName(given + " " + family);
		candidate.setGivenName(given);
		candidate.setFamilyName(family);
		candidate.setBirthDate(LocalDate.parse("1990-01-01"));
		candidate.setPhone("01712345678");
		return candidate;
	}
	
	private FuzzyPatientMatchConfig config(int certain, int probable, int possible) {
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
		return new FuzzyPatientMatchConfig(true, possible, 85, 70, 60, 500, "jaro_winkler", "levenshtein", "token_jaccard",
		        true, 0, certain, probable, possible, DobRepositoryFilterMode.ONLY_DOB_REQUESTS, new LinkedHashSet<String>(
		                enabled.keySet()), rules(), enabled, weights);
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
}
