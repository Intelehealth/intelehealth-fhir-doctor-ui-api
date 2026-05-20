package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.utils.ModuleClasspathPropertiesLoader;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads {@code fhir_module.fhir} from the published config API to decide whether outbound patient
 * FHIR sync is enabled.
 */
@Service("publishedConfigFhirSyncGateService")
public class PublishedConfigFhirSyncGateService {
	
	private static final Logger log = LoggerFactory.getLogger(PublishedConfigFhirSyncGateService.class);
	
	public static final String PROP_PUBLISHED_CONFIG_URL = "intelehealth.config.published.url";
	
	public static final String PROP_CACHE_SECONDS = "intelehealth.fhir.sync.published.config.cache.seconds";
	
	private static final int DEFAULT_CACHE_SECONDS = 60;
	
	private volatile CachedDecision cachedDecision;
	
	public boolean isFhirSyncEnabled() {
		return resolveDecision().enabled;
	}
	
	void clearCacheForTests() {
		cachedDecision = null;
	}
	
	private ResolvedDecision resolveDecision() {
		long now = System.currentTimeMillis();
		CachedDecision local = cachedDecision;
		if (local != null && now < local.expiresAtMs) {
			return local.decision;
		}
		ResolvedDecision fresh = fetchFromApi();
		cachedDecision = new CachedDecision(fresh, now + cacheTtlMs());
		return fresh;
	}
	
	private ResolvedDecision fetchFromApi() {
		String url = resolvePublishedConfigUrl();
		if (StringUtils.isBlank(url)) {
			log.error("Published config URL is blank ({}); treating FHIR sync as enabled", PROP_PUBLISHED_CONFIG_URL);
			return enabledDefault("missing-url");
		}
		try {
			String body = HttpWebClient.getJson(url);
			if (StringUtils.isBlank(body)) {
				log.error("Published config API returned empty body; treating FHIR sync as enabled");
				return enabledDefault("empty-body");
			}
			JSONObject root = new JSONObject(body);
			JSONObject fhirModule = root.optJSONObject("fhir_module");
			if (fhirModule == null) {
				log.warn("Published config JSON has no fhir_module object; treating FHIR sync as disabled");
				return new ResolvedDecision(false, "missing-fhir_module");
			}
			boolean fhir = fhirModule.optBoolean("fhir", false);
			log.debug("Published config fhir_module.fhir={} => FHIR sync {}", fhir, fhir ? "enabled" : "disabled");
			return new ResolvedDecision(fhir, "fhir_module.fhir=" + fhir);
		}
		catch (RuntimeException ex) {
			log.warn("Published config API call failed ({}); treating FHIR sync as enabled: {}", url, ex.getMessage());
			return enabledDefault("api-error");
		}
	}
	
	private static ResolvedDecision enabledDefault(String reason) {
		return new ResolvedDecision(true, reason);
	}
	
	private long cacheTtlMs() {
		String raw = null;
		try {
			if (Context.isSessionOpen()) {
				raw = Context.getAdministrationService().getGlobalProperty(PROP_CACHE_SECONDS);
			}
		}
		catch (Exception ignore) {
			/* use properties file */
		}
		if (StringUtils.isBlank(raw)) {
			java.util.Properties props = ModuleClasspathPropertiesLoader.loadMergedInOrder("ihmodule.properties",
			    "patientdataexchange-application.properties");
			if (props != null) {
				raw = props.getProperty(PROP_CACHE_SECONDS);
			}
		}
		int seconds = DEFAULT_CACHE_SECONDS;
		if (StringUtils.isNotBlank(raw)) {
			try {
				seconds = Integer.parseInt(raw.trim());
			}
			catch (NumberFormatException ex) {
				log.warn("Invalid {}='{}'; using {}s", PROP_CACHE_SECONDS, raw, DEFAULT_CACHE_SECONDS);
			}
		}
		if (seconds < 0) {
			seconds = 0;
		}
		return seconds * 1000L;
	}
	
	private String resolvePublishedConfigUrl() {
		String raw = null;
		try {
			if (Context.isSessionOpen()) {
				raw = Context.getAdministrationService().getGlobalProperty(PROP_PUBLISHED_CONFIG_URL);
			}
		}
		catch (Exception ignore) {
			/* use properties file */
		}
		if (StringUtils.isBlank(raw)) {
			java.util.Properties props = ModuleClasspathPropertiesLoader.loadMergedInOrder("ihmodule.properties",
			    "patientdataexchange-application.properties");
			if (props != null) {
				raw = props.getProperty(PROP_PUBLISHED_CONFIG_URL);
			}
		}
		return StringUtils.trimToEmpty(raw);
	}
	
	private static final class ResolvedDecision {
		
		final boolean enabled;
		
		final String reason;
		
		ResolvedDecision(boolean enabled, String reason) {
			this.enabled = enabled;
			this.reason = reason;
		}
	}
	
	private static final class CachedDecision {
		
		final ResolvedDecision decision;
		
		final long expiresAtMs;
		
		CachedDecision(ResolvedDecision decision, long expiresAtMs) {
			this.decision = decision;
			this.expiresAtMs = expiresAtMs;
		}
	}
}
