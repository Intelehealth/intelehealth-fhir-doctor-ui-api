package org.openmrs.module.ihmodule.api.patientmatch.service;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.PatientMatchRules;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticAlgorithm;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticEncodingService;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyPhoneUtils;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MatchRuleComboEvaluator {
	
	private static final Logger log = LoggerFactory.getLogger(MatchRuleComboEvaluator.class);
	
	private final PhoneticEncodingService phoneticEncodingService;
	
	MatchRuleComboEvaluator() {
		this(new PhoneticEncodingService());
	}
	
	MatchRuleComboEvaluator(PhoneticEncodingService phoneticEncodingService) {
		this.phoneticEncodingService = phoneticEncodingService != null ? phoneticEncodingService
		        : new PhoneticEncodingService();
	}
	
	boolean isEligible(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate, FuzzyPatientMatchConfig config) {
		PatientMatchRules rules = config != null ? config.getRules() : null;
		if (rules == null || rules.getMatchFields() == null || rules.getMatchFields().isEmpty()) {
			return true;
		}
		if (!hasComboRules(rules)) {
			return true;
		}
		return resolveMatchResultLabel(request, candidate, config) != null;
	}
	
	boolean hasComboRules(FuzzyPatientMatchConfig config) {
		return hasComboRules(config != null ? config.getRules() : null);
	}
	
	String resolveMatchResultLabel(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		PatientMatchRules rules = config != null ? config.getRules() : null;
		if (rules == null || rules.getMatchFields() == null || rules.getMatchFields().isEmpty() || !hasComboRules(rules)) {
			return null;
		}
		Set<String> matchedRules = matchedRuleNames(request, candidate, config);
		String bestLabel = null;
		for (Map.Entry<String, Object> entry : rules.getMatchResultMap().entrySet()) {
			if (!(entry.getValue() instanceof String)) {
				continue;
			}
			if (comboSatisfied(entry.getKey(), matchedRules)) {
				bestLabel = strongerLabel(bestLabel, normalizeMatchResultLabel((String) entry.getValue()));
			}
		}
		return bestLabel;
	}
	
	Set<String> matchedRuleNames(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		LinkedHashSet<String> matched = new LinkedHashSet<String>();
		PatientMatchRules rules = config != null ? config.getRules() : null;
		if (rules == null || rules.getMatchFields() == null) {
			return matched;
		}
		for (PatientMatchRules.MatchFieldRule rule : rules.getMatchFields()) {
			if (rule == null || Boolean.FALSE.equals(rule.getEnabled()) || StringUtils.isBlank(rule.getName())) {
				continue;
			}
			if (matches(rule, request, candidate, config)) {
				matched.add(rule.getName().trim());
			}
		}
		return matched;
	}
	
	private boolean comboSatisfied(String comboKey, Set<String> matchedRules) {
		if (StringUtils.isBlank(comboKey) || matchedRules == null || matchedRules.isEmpty()) {
			return false;
		}
		String[] required = comboKey.split(",");
		for (String item : required) {
			String normalized = StringUtils.trimToNull(item);
			if (normalized == null || !matchedRules.contains(normalized)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean hasComboRules(PatientMatchRules rules) {
		if (rules == null || rules.getMatchResultMap() == null || rules.getMatchResultMap().isEmpty()) {
			return false;
		}
		for (Object value : rules.getMatchResultMap().values()) {
			if (value instanceof String) {
				return true;
			}
		}
		return false;
	}
	
	private boolean matches(PatientMatchRules.MatchFieldRule rule, FuzzyPatientMatchRequest request,
	        FuzzyPatientCandidate candidate, FuzzyPatientMatchConfig config) {
		String ruleName = StringUtils.trimToEmpty(rule.getName()).toLowerCase(Locale.ENGLISH);
		if (ruleName.contains("birth") || pathContains(rule, "birthdate")) {
			return birthDateMatch(request, candidate, config);
		}
		if (ruleName.contains("phone") || pathContains(rule, "telecom")) {
			return phoneMatch(rule, request, candidate, config);
		}
		if (ruleName.contains("identifier") || pathContains(rule, "identifier")) {
			return exactNormalizedMatch(request != null ? request.getIdentifier() : null,
			    candidate != null ? candidate.getIdentifier() : null);
		}
		if (ruleName.contains("firstname") || pathContains(rule, "name.given")) {
			return namePartMatch(rule, request != null ? request.getGivenName() : null,
			    candidate != null ? candidate.getGivenName() : null, config);
		}
		if (ruleName.contains("lastname") || pathContains(rule, "name.family")) {
			return namePartMatch(rule, request != null ? request.getFamilyName() : null,
			    candidate != null ? candidate.getFamilyName() : null, config);
		}
		if (ruleName.contains("address")) {
			return stringMatch(rule, request != null ? request.getAddress() : null,
			    candidate != null ? candidate.getAddress() : null, config);
		}
		if (ruleName.contains("gender")) {
			return exactNormalizedMatch(request != null ? request.getGender() : null,
			    candidate != null ? candidate.getGender() : null);
		}
		if (ruleName.contains("name")) {
			return stringMatch(rule, request != null ? request.getName() : null, candidate != null ? candidate.getName()
			        : null, config);
		}
		return false;
	}
	
	private boolean birthDateMatch(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		if (request == null || candidate == null || !request.hasBirthDate() || candidate.getBirthDate() == null) {
			return false;
		}
		if (request.getBirthDate().equals(candidate.getBirthDate())) {
			return true;
		}
		return config != null
		        && config.getDobNearMatchDays() > 0
		        && Math.abs(ChronoUnit.DAYS.between(request.getBirthDate(), candidate.getBirthDate())) <= config
		                .getDobNearMatchDays();
	}
	
	private boolean phoneMatch(PatientMatchRules.MatchFieldRule rule, FuzzyPatientMatchRequest request,
	        FuzzyPatientCandidate candidate, FuzzyPatientMatchConfig config) {
		String requestPhone = request != null ? request.getPhone() : null;
		String candidatePhone = candidate != null ? candidate.getPhone() : null;
		if (StringUtils.isBlank(requestPhone) || StringUtils.isBlank(candidatePhone)) {
			return false;
		}
		String algorithm = algorithm(rule);
		if ("string".equals(algorithm)) {
			return exactNormalizedMatch(FuzzyPhoneUtils.normalizeDigits(requestPhone),
			    FuzzyPhoneUtils.normalizeDigits(candidatePhone));
		}
		double threshold = thresholdPercent(rule, config);
		return FuzzyPhoneUtils.similarityPercent(requestPhone, candidatePhone) >= threshold;
	}
	
	private boolean stringMatch(PatientMatchRules.MatchFieldRule rule, String left, String right,
	        FuzzyPatientMatchConfig config) {
		if (StringUtils.isBlank(left) || StringUtils.isBlank(right)) {
			return false;
		}
		String algorithm = algorithm(rule);
		if (PhoneticAlgorithm.isPhonetic(algorithm)) {
			return phoneticMatchForRule(left, right, algorithm, rule);
		}
		if ("token_jaccard".equals(algorithm)) {
			return FuzzyTextUtils.tokenJaccardPercent(left, right) >= thresholdPercent(rule, config);
		}
		if ("string".equals(algorithm)) {
			return exactNormalizedMatch(left, right);
		}
		return FuzzyTextUtils.jaroWinklerPercent(left, right) >= thresholdPercent(rule, config);
	}
	
	private boolean namePartMatch(PatientMatchRules.MatchFieldRule rule, String left, String right,
	        FuzzyPatientMatchConfig config) {
		if (StringUtils.isBlank(left) || StringUtils.isBlank(right)) {
			return false;
		}
		String algorithm = algorithm(rule);
		if (PhoneticAlgorithm.isPhonetic(algorithm)) {
			return phoneticMatchForRule(left, right, algorithm, rule);
		}
		if ("token_jaccard".equals(algorithm)) {
			return bestTokenJaccardPercent(left, right) >= thresholdPercent(rule, config);
		}
		if ("string".equals(algorithm)) {
			return exactNamePartMatch(left, right);
		}
		return bestTokenJaroWinklerPercent(left, right) >= thresholdPercent(rule, config);
	}
	
	private boolean phoneticMatchForRule(String left, String right, String rawAlgorithm,
	        PatientMatchRules.MatchFieldRule rule) {
		try {
			PhoneticAlgorithm phonetic = PhoneticAlgorithm.fromConfig(rawAlgorithm);
			log.debug("Applying phonetic algorithm {} from config for match rule '{}'", phonetic,
			    rule != null ? rule.getName() : "?");
			return phoneticNamePartMatch(left, right, phonetic);
		}
		catch (IllegalArgumentException ex) {
			log.warn("Unsupported phonetic algorithm '{}' on match rule '{}': {}", rawAlgorithm,
			    rule != null ? rule.getName() : "?", ex.getMessage());
			return false;
		}
	}
	
	private boolean phoneticNamePartMatch(String left, String right, PhoneticAlgorithm algorithm) {
		if (phoneticEncodingService.phoneticMatch(left, right, algorithm)) {
			return true;
		}
		for (String leftToken : FuzzyTextUtils.tokenize(left)) {
			for (String rightToken : FuzzyTextUtils.tokenize(right)) {
				if (phoneticEncodingService.phoneticMatch(leftToken, rightToken, algorithm)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean exactNamePartMatch(String left, String right) {
		if (exactNormalizedMatch(left, right)) {
			return true;
		}
		for (String leftToken : FuzzyTextUtils.tokenize(left)) {
			for (String rightToken : FuzzyTextUtils.tokenize(right)) {
				if (exactNormalizedMatch(leftToken, rightToken)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private double bestTokenJaroWinklerPercent(String left, String right) {
		double best = FuzzyTextUtils.jaroWinklerPercent(left, right);
		for (String leftToken : FuzzyTextUtils.tokenize(left)) {
			for (String rightToken : FuzzyTextUtils.tokenize(right)) {
				best = Math.max(best, FuzzyTextUtils.jaroWinklerPercent(leftToken, rightToken));
			}
		}
		return best;
	}
	
	private double bestTokenJaccardPercent(String left, String right) {
		double best = FuzzyTextUtils.tokenJaccardPercent(left, right);
		for (String leftToken : FuzzyTextUtils.tokenize(left)) {
			for (String rightToken : FuzzyTextUtils.tokenize(right)) {
				best = Math.max(best, FuzzyTextUtils.tokenJaccardPercent(leftToken, rightToken));
			}
		}
		return best;
	}
	
	private boolean exactNormalizedMatch(String left, String right) {
		return StringUtils.equals(FuzzyTextUtils.normalize(left), FuzzyTextUtils.normalize(right));
	}
	
	private String normalizeMatchResultLabel(String value) {
		return StringUtils.defaultIfBlank(value, "").trim().toUpperCase(Locale.ENGLISH);
	}
	
	private String strongerLabel(String left, String right) {
		if (labelPriority(right) > labelPriority(left)) {
			return right;
		}
		return left;
	}
	
	private int labelPriority(String value) {
		if ("MATCH".equals(value)) {
			return 2;
		}
		if ("POSSIBLE_MATCH".equals(value)) {
			return 1;
		}
		return 0;
	}
	
	private boolean pathContains(PatientMatchRules.MatchFieldRule rule, String token) {
		return StringUtils.containsIgnoreCase(StringUtils.defaultString(rule.getResourcePath()), token)
		        || StringUtils.containsIgnoreCase(StringUtils.defaultString(rule.getFhirPath()), token);
	}
	
	private String algorithm(PatientMatchRules.MatchFieldRule rule) {
		String value = null;
		if (rule.getSimilarity() != null && StringUtils.isNotBlank(rule.getSimilarity().getAlgorithm())) {
			value = rule.getSimilarity().getAlgorithm();
		} else if (rule.getMatcher() != null) {
			value = rule.getMatcher().getAlgorithm();
		}
		return StringUtils.defaultIfBlank(value, "jaro_winkler").trim().toLowerCase(Locale.ENGLISH).replace('-', '_');
	}
	
	private double thresholdPercent(PatientMatchRules.MatchFieldRule rule, FuzzyPatientMatchConfig config) {
		Double raw = rule.getSimilarity() != null ? rule.getSimilarity().getMatchThreshold() : null;
		if (raw == null) {
			return config != null ? config.getFieldMatchThreshold() : 60.0d;
		}
		double value = raw.doubleValue();
		return value <= 1.0d ? value * 100.0d : value;
	}
}
