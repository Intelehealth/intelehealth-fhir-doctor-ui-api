package org.openmrs.module.ihmodule.api.patientmatch.service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfigService;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;
import org.openmrs.module.ihmodule.api.patientmatch.engine.PatientFuzzyMatchingEngine;
import org.openmrs.module.ihmodule.api.patientmatch.mapper.FuzzyMatchPatientResponseMapper;
import org.openmrs.module.ihmodule.api.patientmatch.repository.PatientCandidateSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

@Service
public class FhirPatientMatchService {
	
	private static final Logger log = LoggerFactory.getLogger(FhirPatientMatchService.class);
	
	private static final String FUZZY_MATCH_DEBUG = "FUZZY_MATCH_DEBUG";
	
	private static final String EXT_MATCH_GRADE = "http://hl7.org/fhir/StructureDefinition/match-grade";
	
	private final MatchRuleComboEvaluator matchRuleComboEvaluator = new MatchRuleComboEvaluator();
	
	@Autowired
	private FuzzyPatientMatchConfigService configService;
	
	@Autowired
	private FhirPatientMatchRequestParser requestParser;
	
	@Autowired
	private PatientCandidateSource candidateSource;
	
	@Autowired
	private PatientFuzzyMatchingEngine matchingEngine;
	
	@Autowired(required = false)
	private FuzzyMatchPatientResponseMapper responseMapper;
	
	@Transactional(readOnly = true)
	public Bundle match(String body) {
		log.error("{} match() start rawBodyLength={} bodyPreview={}", FUZZY_MATCH_DEBUG,
		    body != null ? body.length() : -1, truncateForDebug(body, 800));
		FuzzyPatientMatchConfig config = configService.getConfig();
		if (!config.isEnabled()) {
			log.error("{} match() aborted: fuzzy patient match disabled in module config", FUZZY_MATCH_DEBUG);
			throw new IllegalStateException("FHIR fuzzy patient match is disabled");
		}
		FuzzyPatientMatchRequest request = requestParser.parse(body);
		log.error("{} match() parsed offset={} count={} onlyCertainMatches={} hasIdentifier={} hasNameFields={}",
		    FUZZY_MATCH_DEBUG, request.getOffset(), request.getCount(), request.isOnlyCertainMatches(),
		    StringUtils.isNotBlank(request.getIdentifier()), request.hasAnySearchField());
		List<FuzzyPatientCandidate> candidates = candidateSource.findCandidates(request, config);
		log.error("{} match() candidateSource returned poolSize={}", FUZZY_MATCH_DEBUG, candidates.size());
		List<FuzzyPatientMatchResult> scored = new ArrayList<FuzzyPatientMatchResult>();
		for (FuzzyPatientCandidate candidate : candidates) {
			String comboMatchResult = matchRuleComboEvaluator.resolveMatchResultLabel(request, candidate, config);
			if (comboMatchResult == null && matchRuleComboEvaluator.hasComboRules(config)) {
				continue;
			}
			FuzzyPatientMatchResult result = matchingEngine.score(request, candidate, config);
			result.setRuleMatchResult(comboMatchResult);
			String matchGrade = toMatchGrade(result, config);
			if (matchGrade == null) {
				continue;
			}
			if (request.isOnlyCertainMatches() && !"certain".equals(matchGrade)) {
				continue;
			}
			scored.add(result);
		}
		Collections.sort(scored, Comparator.comparingDouble(FuzzyPatientMatchResult::getOverallMatchScore).reversed()
		        .thenComparing(result -> StringUtils.defaultString(result.getCandidate().getUuid())));
		
		int total = scored.size();
		int fromIndex = Math.min(request.getOffset(), total);
		int toIndex = Math.min(fromIndex + request.getCount(), total);
		List<FuzzyPatientMatchResult> page = scored.subList(fromIndex, toIndex);
		Bundle out = toBundle(page, total, config);
		log.error("{} match() done scoredAfterRules={} bundleTotal={} entriesReturned={}", FUZZY_MATCH_DEBUG, total,
		    out.getTotal(), out.hasEntry() ? out.getEntry().size() : 0);
		return out;
	}
	
	private static String truncateForDebug(String s, int max) {
		if (s == null) {
			return "<null>";
		}
		String t = s.replace('\n', ' ').replace('\r', ' ').trim();
		return t.length() <= max ? t : t.substring(0, max) + "...(truncated)";
	}
	
	public OperationOutcome errorOutcome(String code, String diagnostics) {
		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR).setCode(OperationOutcome.IssueType.EXCEPTION)
		        .setDiagnostics(code + ": " + diagnostics);
		return outcome;
	}
	
	public String encodeResource(FhirContext fhirContext, org.hl7.fhir.r4.model.Resource resource) {
		return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
	}
	
	private Bundle toBundle(List<FuzzyPatientMatchResult> matches, int total, FuzzyPatientMatchConfig config) {
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.setTotal(total);
		for (FuzzyPatientMatchResult match : matches) {
			BundleEntryComponent entry = bundle.addEntry();
			entry.setResource(toPatient(match.getCandidate()));
			entry.getSearch().setMode(Bundle.SearchEntryMode.MATCH);
			entry.getSearch().setScore((float) match.getNormalizedMatchScore());
			entry.getSearch().addExtension(new Extension(EXT_MATCH_GRADE, new CodeType(toMatchGrade(match, config))));
		}
		return bundle;
	}
	
	private Patient toPatient(FuzzyPatientCandidate candidate) {
		Patient patient = new Patient();
		patient.setId(candidate.getUuid());
		patient.setActive(true);
		if (StringUtils.isNotBlank(candidate.getIdentifier())) {
			Identifier identifier = patient.addIdentifier();
			identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
			identifier.setValue(candidate.getIdentifier());
		}
		if (StringUtils.isNotBlank(candidate.getGivenName()) || StringUtils.isNotBlank(candidate.getFamilyName())
		        || StringUtils.isNotBlank(candidate.getName())) {
			HumanName name = patient.addName();
			if (StringUtils.isNotBlank(candidate.getGivenName())) {
				name.addGiven(candidate.getGivenName());
			}
			if (StringUtils.isNotBlank(candidate.getFamilyName())) {
				name.setFamily(candidate.getFamilyName());
			}
			if (StringUtils.isNotBlank(candidate.getName())) {
				name.setText(candidate.getName());
			}
		}
		if (candidate.getBirthDate() != null) {
			patient.setBirthDate(java.util.Date.from(candidate.getBirthDate().atStartOfDay(ZoneId.systemDefault())
			        .toInstant()));
			patient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType(candidate.getBirthDate().toString()));
		}
		if (StringUtils.isNotBlank(candidate.getGender())) {
			patient.setGender(toAdministrativeGender(candidate.getGender()));
		}
		if (StringUtils.isNotBlank(candidate.getPhone())) {
			ContactPoint telecom = patient.addTelecom();
			telecom.setSystem(ContactPoint.ContactPointSystem.PHONE);
			telecom.setValue(candidate.getPhone());
		}
		if (responseMapper != null) {
			responseMapper.enrich(patient, candidate);
		} else if (StringUtils.isNotBlank(candidate.getAddress())) {
			patient.addAddress().setText(candidate.getAddress());
		}
		return patient;
	}
	
	private AdministrativeGender toAdministrativeGender(String value) {
		String normalized = StringUtils.trimToEmpty(value).toLowerCase();
		if ("m".equals(normalized) || "male".equals(normalized)) {
			return AdministrativeGender.MALE;
		}
		if ("f".equals(normalized) || "female".equals(normalized)) {
			return AdministrativeGender.FEMALE;
		}
		if ("other".equals(normalized) || "o".equals(normalized)) {
			return AdministrativeGender.OTHER;
		}
		return AdministrativeGender.UNKNOWN;
	}
	
	private String toMatchGrade(FuzzyPatientMatchResult match, FuzzyPatientMatchConfig config) {
		String scoreGrade = toScoreGrade(match, config);
		String ruleMatchResult = match != null ? StringUtils.upperCase(match.getRuleMatchResult()) : null;
		if ("MATCH".equals(ruleMatchResult)) {
			if ("certain".equals(scoreGrade)) {
				return "certain";
			}
			return "probable";
		}
		if ("POSSIBLE_MATCH".equals(ruleMatchResult)) {
			return scoreGrade != null ? "possible" : null;
		}
		return scoreGrade;
	}
	
	private String toScoreGrade(FuzzyPatientMatchResult match, FuzzyPatientMatchConfig config) {
		double normalizedScore = match != null ? match.getNormalizedMatchScore() : 0.0d;
		double certainThreshold = config != null ? config.getCertainMatchThreshold() / 100.0d : 0.95d;
		double probableThreshold = config != null ? config.getProbableMatchThreshold() / 100.0d : 0.80d;
		double possibleThreshold = config != null ? config.getPossibleMatchThreshold() / 100.0d : 0.60d;
		if (normalizedScore >= certainThreshold) {
			return "certain";
		}
		if (normalizedScore >= probableThreshold) {
			return "probable";
		}
		if (normalizedScore >= possibleThreshold) {
			return "possible";
		}
		return null;
	}
}
