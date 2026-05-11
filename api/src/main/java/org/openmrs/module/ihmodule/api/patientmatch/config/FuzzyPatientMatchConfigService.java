package org.openmrs.module.ihmodule.api.patientmatch.config;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FuzzyPatientMatchConfigService {
	
	private static final Logger log = LoggerFactory.getLogger(FuzzyPatientMatchConfigService.class);
	
	private static final long CACHE_TTL_MS = 60_000L;
	
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
		normalizeWeights(fieldWeight, fieldEnabled);
		
		int threshold = normalizeScore(resolveInt("intelehealth.fhir.patient.match.threshold", 70), 70);
		int confidenceHigh = normalizeScore(resolveInt("intelehealth.fhir.patient.match.confidence.high", 85), 85);
		int confidenceMedium = normalizeScore(resolveInt("intelehealth.fhir.patient.match.confidence.medium", 70), 70);
		if (confidenceHigh < confidenceMedium) {
			confidenceHigh = confidenceMedium;
		}
		
		return new FuzzyPatientMatchConfig(resolveBoolean("intelehealth.fhir.patient.match.enabled", true), threshold,
		        confidenceHigh, confidenceMedium,
		        normalizeScore(resolveInt("intelehealth.fhir.patient.match.field.match.threshold", 60), 60),
		        Math.max(50, resolveInt("intelehealth.fhir.patient.match.max.candidates", 500)),
		        resolveString("intelehealth.fhir.patient.match.algorithm.name", "jaro_winkler"),
		        resolveString("intelehealth.fhir.patient.match.algorithm.phone", "levenshtein"),
		        resolveString("intelehealth.fhir.patient.match.algorithm.address", "token_jaccard"),
		        resolveBoolean("intelehealth.fhir.patient.match.algorithm.phonetic.boost.enabled", true),
		        Math.max(0, resolveInt("intelehealth.fhir.patient.match.dob.near.match.days", 0)), fieldEnabled,
		        fieldWeight);
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
	
	private String resolveString(String key, String defaultValue) {
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
}
