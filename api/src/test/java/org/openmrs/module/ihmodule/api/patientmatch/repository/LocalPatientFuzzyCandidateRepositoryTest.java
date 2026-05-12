package org.openmrs.module.ihmodule.api.patientmatch.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.config.DobRepositoryFilterMode;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.PatientMatchRules;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;

public class LocalPatientFuzzyCandidateRepositoryTest {
	
	private final LocalPatientFuzzyCandidateRepository repository = new LocalPatientFuzzyCandidateRepository();
	
	@Test
	public void shouldApplyDobRepositoryFilter_shouldUseDobOnlyRequests() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setBirthDate(LocalDate.parse("2025-06-06"));
		
		assertTrue(repository.shouldApplyDobRepositoryFilter(request, config(DobRepositoryFilterMode.ONLY_DOB_REQUESTS)));
	}
	
	@Test
	public void shouldApplyDobRepositoryFilter_shouldSkipDobOnlyFilterWhenOtherFieldsPresent() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setBirthDate(LocalDate.parse("2025-06-06"));
		request.setGivenName("Craig");
		request.setFamilyName("Kaden");
		
		assertFalse(repository.shouldApplyDobRepositoryFilter(request, config(DobRepositoryFilterMode.ONLY_DOB_REQUESTS)));
	}
	
	@Test
	public void shouldApplyDobRepositoryFilter_shouldRequireDob() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setPhone("01712345678");
		
		assertFalse(repository.shouldApplyDobRepositoryFilter(request, config(DobRepositoryFilterMode.ONLY_DOB_REQUESTS)));
	}
	
	@Test
	public void shouldApplyDobRepositoryFilter_shouldRespectAlwaysMode() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setBirthDate(LocalDate.parse("2025-06-06"));
		request.setGivenName("Craig");
		
		assertTrue(repository.shouldApplyDobRepositoryFilter(request, config(DobRepositoryFilterMode.ALWAYS)));
	}
	
	@Test
	public void shouldApplyDobRepositoryFilter_shouldRespectNeverMode() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setBirthDate(LocalDate.parse("2025-06-06"));
		
		assertFalse(repository.shouldApplyDobRepositoryFilter(request, config(DobRepositoryFilterMode.NEVER)));
	}
	
	@Test
	public void applicableCandidateSearchRuleKeys_shouldUseMultipleRulesForNameAndDobRequests() {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		request.setGivenName("Craig");
		request.setFamilyName("Kaden");
		request.setBirthDate(LocalDate.parse("2025-06-06"));
		
		List<String> keys = repository.applicableCandidateSearchRuleKeys(request,
		    config(DobRepositoryFilterMode.ONLY_DOB_REQUESTS));
		
		assertEquals(Arrays.asList("name", "birthdate"), keys);
	}
	
	private FuzzyPatientMatchConfig config(DobRepositoryFilterMode mode) {
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
		        0, 95, 80, 60, mode, new LinkedHashSet<String>(enabled.keySet()), rules(), enabled, weights);
	}
	
	private PatientMatchRules rules() {
		PatientMatchRules rules = new PatientMatchRules();
		PatientMatchRules.CandidateSearchRule name = new PatientMatchRules.CandidateSearchRule();
		name.setResourceType("Patient");
		name.setSearchParams(Arrays.asList("name"));
		PatientMatchRules.CandidateSearchRule birthdate = new PatientMatchRules.CandidateSearchRule();
		birthdate.setResourceType("Patient");
		birthdate.setSearchParams(Arrays.asList("birthdate"));
		rules.setCandidateSearchParams(Arrays.asList(name, birthdate));
		return rules;
	}
}
