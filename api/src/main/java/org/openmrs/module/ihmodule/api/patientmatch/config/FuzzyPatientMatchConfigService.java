package org.openmrs.module.ihmodule.api.patientmatch.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FuzzyPatientMatchConfigService {
	
	private static final Logger log = LoggerFactory.getLogger(FuzzyPatientMatchConfigService.class);
	
	private static final long CACHE_TTL_MS = 60_000L;
	
	private static final String DEFAULT_RULES_RESOURCE = "patient-match-rules.json";
	
	static final String GP_RULES_FILE = "ihmodule.fhir.patient.match.rules.file";
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private static volatile Properties cachedModuleProperties;
	
	private volatile long cacheExpiresAt;
	
	private volatile FuzzyPatientMatchConfig cachedConfig;
	
	public FuzzyPatientMatchConfig getConfig() {
		FuzzyPatientMatchConfig config = cachedConfig;
		long now = System.currentTimeMillis();
		if (config != null && now < cacheExpiresAt) {
			return config;
		}
		synchronized (this) {
			if (cachedConfig != null && now < cacheExpiresAt) {
				return cachedConfig;
			}
			cachedConfig = loadConfig();
			cacheExpiresAt = now + CACHE_TTL_MS;
			return cachedConfig;
		}
	}
	
	private FuzzyPatientMatchConfig loadConfig() {
		boolean enabled = resolveBoolean("intelehealth.fhir.patient.match.enabled", true);
		Map<String, Boolean> fieldEnabled = new LinkedHashMap<String, Boolean>();
		fieldEnabled.put("name", resolveBoolean("intelehealth.fhir.patient.match.field.name.enabled", true));
		fieldEnabled.put("dob", resolveBoolean("intelehealth.fhir.patient.match.field.dob.enabled", true));
		fieldEnabled.put("phone", resolveBoolean("intelehealth.fhir.patient.match.field.phone.enabled", true));
		fieldEnabled.put("address", resolveBoolean("intelehealth.fhir.patient.match.field.address.enabled", true));
		fieldEnabled.put("gender", resolveBoolean("intelehealth.fhir.patient.match.field.gender.enabled", true));
		fieldEnabled.put("identifier", resolveBoolean("intelehealth.fhir.patient.match.field.identifier.enabled", true));
		
		Map<String, Double> fieldWeight = new LinkedHashMap<String, Double>();
		fieldWeight.put("name", resolveDouble("intelehealth.fhir.patient.match.field.name.weight", 0.45d));
		fieldWeight.put("dob", resolveDouble("intelehealth.fhir.patient.match.field.dob.weight", 0.20d));
		fieldWeight.put("phone", resolveDouble("intelehealth.fhir.patient.match.field.phone.weight", 0.20d));
		fieldWeight.put("address", resolveDouble("intelehealth.fhir.patient.match.field.address.weight", 0.10d));
		fieldWeight.put("gender", resolveDouble("intelehealth.fhir.patient.match.field.gender.weight", 0.05d));
		fieldWeight.put("identifier", resolveDouble("intelehealth.fhir.patient.match.field.identifier.weight", 0.05d));
		
		int threshold = normalizeScore(resolveInt("intelehealth.fhir.patient.match.threshold", 70), 70);
		int confidenceHigh = normalizeScore(resolveInt("intelehealth.fhir.patient.match.confidence.high", 85), 85);
		int confidenceMedium = normalizeScore(resolveInt("intelehealth.fhir.patient.match.confidence.medium", 70), 70);
		if (confidenceHigh < confidenceMedium) {
			confidenceHigh = confidenceMedium;
		}
		int fieldMatchThreshold = normalizeScore(resolveInt("intelehealth.fhir.patient.match.field.match.threshold", 60), 60);
		int maxCandidates = Math.max(50, resolveInt("intelehealth.fhir.patient.match.max.candidates", 500));
		String nameAlgorithm = resolveString("intelehealth.fhir.patient.match.algorithm.name", "jaro_winkler");
		String phoneAlgorithm = resolveString("intelehealth.fhir.patient.match.algorithm.phone", "levenshtein");
		String addressAlgorithm = resolveString("intelehealth.fhir.patient.match.algorithm.address", "token_jaccard");
		boolean phoneticBoostEnabled = resolveBoolean("intelehealth.fhir.patient.match.algorithm.phonetic.boost.enabled", true);
		int dobNearMatchDays = Math.max(0, resolveInt("intelehealth.fhir.patient.match.dob.near.match.days", 0));
		int certainMatchThreshold = 95;
		int probableMatchThreshold = 80;
		int possibleMatchThreshold = 60;
		DobRepositoryFilterMode dobRepositoryFilterMode = DobRepositoryFilterMode.ONLY_DOB_REQUESTS;
		Set<String> candidateSearchParams = defaultCandidateSearchParams();
		
		PatientMatchRules rules = loadRules();
		if (rules != null) {
			if (rules.getSettings() != null) {
				if (rules.getSettings().getEnabled() != null) {
					enabled = rules.getSettings().getEnabled().booleanValue();
				}
				if (rules.getSettings().getMaxCandidates() != null) {
					maxCandidates = Math.max(50, rules.getSettings().getMaxCandidates().intValue());
				}
				if (rules.getSettings().getFieldMatchThreshold() != null) {
					fieldMatchThreshold = normalizeScore(rules.getSettings().getFieldMatchThreshold().intValue(), 60);
				}
				if (rules.getSettings().getPhoneticBoostEnabled() != null) {
					phoneticBoostEnabled = rules.getSettings().getPhoneticBoostEnabled().booleanValue();
				}
				if (rules.getSettings().getDobNearMatchDays() != null) {
					dobNearMatchDays = Math.max(0, rules.getSettings().getDobNearMatchDays().intValue());
				}
				dobRepositoryFilterMode = DobRepositoryFilterMode.fromValue(
				    rules.getSettings().getDobRepositoryFilterMode(), dobRepositoryFilterMode);
				if (rules.getSettings().getMatchGradeThresholds() != null) {
					certainMatchThreshold = resolveRuleThreshold(rules.getSettings().getMatchGradeThresholds(), "certain",
					    certainMatchThreshold);
					probableMatchThreshold = resolveRuleThreshold(rules.getSettings().getMatchGradeThresholds(), "probable",
					    probableMatchThreshold);
					possibleMatchThreshold = resolveRuleThreshold(rules.getSettings().getMatchGradeThresholds(), "possible",
					    possibleMatchThreshold);
				}
			}
			applyCandidateSearchRules(rules.getCandidateSearchParams(), candidateSearchParams);
			ResolvedFieldRules resolvedFieldRules = applyFieldRules(rules.getMatchFields(), fieldEnabled, fieldWeight,
			    nameAlgorithm, phoneAlgorithm, addressAlgorithm, phoneticBoostEnabled);
			nameAlgorithm = resolvedFieldRules.getNameAlgorithm();
			phoneAlgorithm = resolvedFieldRules.getPhoneAlgorithm();
			addressAlgorithm = resolvedFieldRules.getAddressAlgorithm();
			phoneticBoostEnabled = resolvedFieldRules.isPhoneticBoostEnabled();
			certainMatchThreshold = resolveRuleThreshold(rules.getMatchResultMap(), "certain", certainMatchThreshold);
			probableMatchThreshold = resolveRuleThreshold(rules.getMatchResultMap(), "probable", probableMatchThreshold);
			possibleMatchThreshold = resolveRuleThreshold(rules.getMatchResultMap(), "possible", possibleMatchThreshold);
			threshold = possibleMatchThreshold;
		}
		if (probableMatchThreshold < possibleMatchThreshold) {
			probableMatchThreshold = possibleMatchThreshold;
		}
		if (certainMatchThreshold < probableMatchThreshold) {
			certainMatchThreshold = probableMatchThreshold;
		}
		normalizeWeights(fieldWeight, fieldEnabled);
		
		return new FuzzyPatientMatchConfig(enabled, threshold, confidenceHigh, confidenceMedium, fieldMatchThreshold,
		        maxCandidates, nameAlgorithm, phoneAlgorithm, addressAlgorithm, phoneticBoostEnabled, dobNearMatchDays,
		        certainMatchThreshold, probableMatchThreshold, possibleMatchThreshold, dobRepositoryFilterMode,
		        candidateSearchParams, rules, fieldEnabled, fieldWeight);
	}
	
	private void normalizeWeights(Map<String, Double> fieldWeight, Map<String, Boolean> fieldEnabled) {
		double sum = 0.0d;
		for (Map.Entry<String, Double> entry : fieldWeight.entrySet()) {
			if (Boolean.TRUE.equals(fieldEnabled.get(entry.getKey()))) {
				sum += Math.max(0.0d, entry.getValue().doubleValue());
			}
		}
		if (sum <= 0.0d) {
			log.warn("Fuzzy patient match weights sum to zero; restoring defaults");
			fieldWeight.put("name", 0.45d);
			fieldWeight.put("dob", 0.20d);
			fieldWeight.put("phone", 0.20d);
			fieldWeight.put("address", 0.10d);
			fieldWeight.put("gender", 0.05d);
			fieldWeight.put("identifier", 0.05d);
			return;
		}
		for (Map.Entry<String, Double> entry : fieldWeight.entrySet()) {
			double normalized = Math.max(0.0d, entry.getValue().doubleValue()) / sum;
			entry.setValue(Double.valueOf(normalized));
		}
	}
	
	private int normalizeScore(int value, int defaultValue) {
		if (value < 0 || value > 100) {
			return defaultValue;
		}
		return value;
	}
	
	protected String resolveString(String key, String defaultValue) {
		String resolved = null;
		try {
			if (Context.isSessionOpen()) {
				resolved = Context.getAdministrationService().getGlobalProperty(key);
			}
		}
		catch (Exception ex) {
			log.debug("Unable to read global property {}", key, ex);
		}
		if (StringUtils.isBlank(resolved)) {
			Properties moduleProps = getModuleProperties();
			if (moduleProps != null) {
				resolved = moduleProps.getProperty(key);
			}
		}
		return StringUtils.isBlank(resolved) ? defaultValue : resolved.trim();
	}
	
	private boolean resolveBoolean(String key, boolean defaultValue) {
		String raw = resolveString(key, null);
		return StringUtils.isBlank(raw) ? defaultValue : Boolean.parseBoolean(raw.trim());
	}
	
	private int resolveInt(String key, int defaultValue) {
		String raw = resolveString(key, null);
		if (StringUtils.isBlank(raw)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(raw.trim());
		}
		catch (NumberFormatException ex) {
			log.warn("Invalid integer for fuzzy patient match property '{}': '{}'", key, raw);
			return defaultValue;
		}
	}
	
	private double resolveDouble(String key, double defaultValue) {
		String raw = resolveString(key, null);
		if (StringUtils.isBlank(raw)) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(raw.trim());
		}
		catch (NumberFormatException ex) {
			log.warn("Invalid decimal for fuzzy patient match property '{}': '{}'", key, raw);
			return defaultValue;
		}
	}
	
	private PatientMatchRules loadRules() {
		PatientMatchRules externalRules = loadRulesFromExternalPath(resolveString(GP_RULES_FILE, null));
		if (externalRules != null) {
			return externalRules;
		}
		return loadBundledRules();
	}
	
	private PatientMatchRules loadRulesFromExternalPath(String path) {
		if (StringUtils.isBlank(path)) {
			return null;
		}
		File file = new File(path.trim());
		if (!file.isFile()) {
			log.warn("FHIR patient match rules file not found at '{}'; using bundled defaults", path);
			return null;
		}
		try (InputStream in = new FileInputStream(file)) {
			log.info("Loading FHIR patient match rules from external file '{}'", file.getAbsolutePath());
			return OBJECT_MAPPER.readValue(in, PatientMatchRules.class);
		}
		catch (Exception ex) {
			log.warn("Unable to load FHIR patient match rules from '{}': {}", path, ex.getMessage());
			return null;
		}
	}
	
	private PatientMatchRules loadBundledRules() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try (InputStream in = cl.getResourceAsStream(DEFAULT_RULES_RESOURCE)) {
			if (in == null) {
				log.warn("Bundled FHIR patient match rules '{}' not found; using property defaults",
				    DEFAULT_RULES_RESOURCE);
				return null;
			}
			return OBJECT_MAPPER.readValue(in, PatientMatchRules.class);
		}
		catch (Exception ex) {
			log.warn("Unable to load bundled FHIR patient match rules '{}': {}", DEFAULT_RULES_RESOURCE,
			    ex.getMessage());
			return null;
		}
	}
	
	private Set<String> defaultCandidateSearchParams() {
		LinkedHashSet<String> params = new LinkedHashSet<String>();
		Collections.addAll(params, "name", "birthdate", "identifier", "phone", "address", "gender");
		return params;
	}
	
	private void applyCandidateSearchRules(List<PatientMatchRules.CandidateSearchRule> rules, Set<String> candidateSearchParams) {
		if (rules == null) {
			return;
		}
		candidateSearchParams.clear();
		for (PatientMatchRules.CandidateSearchRule rule : rules) {
			if (rule == null || rule.getSearchParams() == null) {
				continue;
			}
			if (StringUtils.isNotBlank(rule.getResourceType()) && !"Patient".equalsIgnoreCase(rule.getResourceType())) {
				continue;
			}
			for (String searchParam : rule.getSearchParams()) {
				String normalized = normalizeCandidateSearchParam(searchParam);
				if (normalized != null) {
					candidateSearchParams.add(normalized);
				}
			}
		}
	}
	
	private ResolvedFieldRules applyFieldRules(List<PatientMatchRules.MatchFieldRule> rules,
	        Map<String, Boolean> fieldEnabled, Map<String, Double> fieldWeight, String nameAlgorithm,
	        String phoneAlgorithm, String addressAlgorithm, boolean phoneticBoostEnabled) {
		ResolvedFieldRules resolved = new ResolvedFieldRules(nameAlgorithm, phoneAlgorithm, addressAlgorithm,
		        phoneticBoostEnabled);
		Set<String> weightedFieldsTouched = new LinkedHashSet<String>();
		if (rules == null) {
			return resolved;
		}
		for (PatientMatchRules.MatchFieldRule rule : rules) {
			String fieldName = normalizeFieldName(rule);
			if (fieldName == null) {
				continue;
			}
			if (rule.getEnabled() != null) {
				fieldEnabled.put(fieldName, rule.getEnabled());
			}
			if (rule.getWeight() != null) {
				if (!weightedFieldsTouched.contains(fieldName)) {
					fieldWeight.put(fieldName, Double.valueOf(0.0d));
					weightedFieldsTouched.add(fieldName);
				}
				double nextWeight = Math.max(0.0d, rule.getWeight().doubleValue());
				fieldWeight.put(fieldName, Double.valueOf(fieldWeight.get(fieldName).doubleValue() + nextWeight));
			}
			String algorithm = normalizeAlgorithm(extractAlgorithm(rule));
			if ("name".equals(fieldName) && algorithm != null) {
				if (isPhoneticAlgorithm(algorithm)) {
					resolved.setPhoneticBoostEnabled(true);
				} else {
					resolved.setNameAlgorithm(algorithm);
				}
			}
			if ("phone".equals(fieldName) && algorithm != null) {
				resolved.setPhoneAlgorithm(algorithm);
			}
			if ("address".equals(fieldName) && algorithm != null) {
				resolved.setAddressAlgorithm(algorithm);
			}
		}
		return resolved;
	}
	
	private int resolveRuleThreshold(Map<String, ?> matchResultMap, String key, int defaultValue) {
		if (matchResultMap == null) {
			return defaultValue;
		}
		for (Map.Entry<String, ?> entry : matchResultMap.entrySet()) {
			if (!StringUtils.equalsIgnoreCase(entry.getKey(), key) || entry.getValue() == null) {
				continue;
			}
			if (!(entry.getValue() instanceof Number)) {
				continue;
			}
			double raw = ((Number) entry.getValue()).doubleValue();
			double normalized = raw <= 1.0d ? raw * 100.0d : raw;
			return normalizeScore((int) Math.round(normalized), defaultValue);
		}
		return defaultValue;
	}
	
	private String normalizeCandidateSearchParam(String searchParam) {
		String normalized = StringUtils.trimToNull(searchParam);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase();
		if ("birthdate".equals(normalized)) {
			return "birthdate";
		}
		if ("telecom".equals(normalized)) {
			return "phone";
		}
		return normalized;
	}
	
	private String normalizeFieldName(PatientMatchRules.MatchFieldRule rule) {
		if (rule == null) {
			return null;
		}
		String raw = StringUtils.joinWith(" ", rule.getName(), rule.getResourcePath(), rule.getFhirPath());
		String normalized = StringUtils.trimToEmpty(raw).toLowerCase();
		if (normalized.contains("birth")) {
			return "dob";
		}
		if (normalized.contains("phone") || normalized.contains("telecom")) {
			return "phone";
		}
		if (normalized.contains("identifier")) {
			return "identifier";
		}
		if (normalized.contains("address")) {
			return "address";
		}
		if (normalized.contains("gender")) {
			return "gender";
		}
		if (normalized.contains("name")) {
			return "name";
		}
		return null;
	}
	
	private String extractAlgorithm(PatientMatchRules.MatchFieldRule rule) {
		if (rule == null) {
			return null;
		}
		if (rule.getSimilarity() != null && StringUtils.isNotBlank(rule.getSimilarity().getAlgorithm())) {
			return rule.getSimilarity().getAlgorithm();
		}
		if (rule.getMatcher() != null) {
			return rule.getMatcher().getAlgorithm();
		}
		return null;
	}
	
	private String normalizeAlgorithm(String algorithm) {
		if (StringUtils.isBlank(algorithm)) {
			return null;
		}
		return algorithm.trim().toLowerCase().replace('-', '_');
	}
	
	private boolean isPhoneticAlgorithm(String algorithm) {
		return "metaphone".equalsIgnoreCase(algorithm) || "soundex".equalsIgnoreCase(algorithm);
	}
	
	private Properties getModuleProperties() {
		if (cachedModuleProperties != null) {
			return cachedModuleProperties;
		}
		synchronized (FuzzyPatientMatchConfigService.class) {
			if (cachedModuleProperties != null) {
				return cachedModuleProperties;
			}
			cachedModuleProperties = loadFirstAvailableProperties("ihmodule.properties",
			    "patientdataexchange-application.properties");
			return cachedModuleProperties;
		}
	}
	
	private Properties loadFirstAvailableProperties(String... resourceNames) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		for (String resource : resourceNames) {
			try (InputStream in = cl.getResourceAsStream(resource)) {
				if (in == null) {
					continue;
				}
				Properties p = new Properties();
				p.load(in);
				return p;
			}
			catch (Exception ex) {
				log.warn("Unable to load fuzzy patient match properties '{}': {}", resource, ex.getMessage());
			}
		}
		return null;
	}
	
	private static final class ResolvedFieldRules {
		
		private String nameAlgorithm;
		
		private String phoneAlgorithm;
		
		private String addressAlgorithm;
		
		private boolean phoneticBoostEnabled;
		
		private ResolvedFieldRules(String nameAlgorithm, String phoneAlgorithm, String addressAlgorithm,
		        boolean phoneticBoostEnabled) {
			this.nameAlgorithm = nameAlgorithm;
			this.phoneAlgorithm = phoneAlgorithm;
			this.addressAlgorithm = addressAlgorithm;
			this.phoneticBoostEnabled = phoneticBoostEnabled;
		}
		
		private String getNameAlgorithm() {
			return nameAlgorithm;
		}
		
		private void setNameAlgorithm(String nameAlgorithm) {
			this.nameAlgorithm = nameAlgorithm;
		}
		
		private String getPhoneAlgorithm() {
			return phoneAlgorithm;
		}
		
		private void setPhoneAlgorithm(String phoneAlgorithm) {
			this.phoneAlgorithm = phoneAlgorithm;
		}
		
		private String getAddressAlgorithm() {
			return addressAlgorithm;
		}
		
		private void setAddressAlgorithm(String addressAlgorithm) {
			this.addressAlgorithm = addressAlgorithm;
		}
		
		private boolean isPhoneticBoostEnabled() {
			return phoneticBoostEnabled;
		}
		
		private void setPhoneticBoostEnabled(boolean phoneticBoostEnabled) {
			this.phoneticBoostEnabled = phoneticBoostEnabled;
		}
	}
}
