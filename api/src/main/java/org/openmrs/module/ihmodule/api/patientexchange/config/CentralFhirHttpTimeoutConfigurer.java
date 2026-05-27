package org.openmrs.module.ihmodule.api.patientexchange.config;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.ModuleClasspathPropertiesLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Applies connect/read timeouts for central FHIR writes. Must not call OpenMRS {@link Context}
 * services during Spring module context refresh (causes startup deadlock); defaults are applied
 * lazily from classpath properties, and global properties are applied from
 * {@link org.openmrs.module.ihmodule.APIfordoctorUIActivator#started()}.
 */
public final class CentralFhirHttpTimeoutConfigurer {
	
	private static final Logger log = LoggerFactory.getLogger(CentralFhirHttpTimeoutConfigurer.class);
	
	private static final Object LOCK = new Object();
	
	public static final String PROP_CONNECT_TIMEOUT_MS = "intelehealth.fhir.central.http.connect.timeout.ms";
	
	public static final String PROP_READ_TIMEOUT_MS = "intelehealth.fhir.central.http.read.timeout.ms";
	
	public static final int DEFAULT_CONNECT_TIMEOUT_MS = 60_000;
	
	public static final int DEFAULT_READ_TIMEOUT_MS = 120_000;
	
	private static final int MIN_TIMEOUT_MS = 1_000;
	
	private static final int MAX_TIMEOUT_MS = 600_000;
	
	private static volatile boolean defaultsApplied;
	
	private static volatile Properties cachedModuleProperties;
	
	private CentralFhirHttpTimeoutConfigurer() {
	}
	
	/**
	 * Classpath-only timeouts for early HTTP/FHIR use (safe during Spring init).
	 */
	public static void ensureDefaultsConfigured() {
		if (defaultsApplied) {
			return;
		}
		synchronized (LOCK) {
			if (defaultsApplied) {
				return;
			}
			applyTimeouts(resolveTimeoutMs(PROP_CONNECT_TIMEOUT_MS, DEFAULT_CONNECT_TIMEOUT_MS, false),
			    resolveTimeoutMs(PROP_READ_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, false));
			defaultsApplied = true;
		}
	}
	
	/**
	 * Re-reads OpenMRS global properties after the platform is up (call from module activator).
	 */
	public static void applyConfiguredTimeouts() {
		synchronized (LOCK) {
			int connectMs = resolveTimeoutMs(PROP_CONNECT_TIMEOUT_MS, DEFAULT_CONNECT_TIMEOUT_MS, true);
			int readMs = resolveTimeoutMs(PROP_READ_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, true);
			applyTimeouts(connectMs, readMs);
			defaultsApplied = true;
			log.info("Central FHIR HTTP timeouts: connect={}ms read={}ms", connectMs, readMs);
		}
	}
	
	private static void applyTimeouts(int connectMs, int readMs) {
		HttpWebClient.configureTimeouts(connectMs, readMs);
		FhirContextHolder.R4.getRestfulClientFactory().setConnectTimeout(connectMs);
		FhirContextHolder.R4.getRestfulClientFactory().setSocketTimeout(readMs);
	}
	
	private static int resolveTimeoutMs(String key, int defaultValue, boolean includeGlobalProperties) {
		String raw = resolveStringProperty(key, includeGlobalProperties);
		if (StringUtils.isBlank(raw)) {
			return defaultValue;
		}
		try {
			int value = Integer.parseInt(raw.trim());
			if (value < MIN_TIMEOUT_MS) {
				log.warn("Property {}={} below minimum {}; using {}", key, value, MIN_TIMEOUT_MS, MIN_TIMEOUT_MS);
				return MIN_TIMEOUT_MS;
			}
			if (value > MAX_TIMEOUT_MS) {
				log.warn("Property {}={} above maximum {}; using {}", key, value, MAX_TIMEOUT_MS, MAX_TIMEOUT_MS);
				return MAX_TIMEOUT_MS;
			}
			return value;
		}
		catch (NumberFormatException ex) {
			log.warn("Invalid integer for property '{}': '{}'. Using default={}", key, raw, defaultValue);
			return defaultValue;
		}
	}
	
	private static String resolveStringProperty(String key, boolean includeGlobalProperties) {
		String resolved = null;
		if (includeGlobalProperties) {
			try {
				if (Context.isSessionOpen()) {
					resolved = Context.getAdministrationService().getGlobalProperty(key);
				}
			}
			catch (Exception ex) {
				log.debug("Unable to resolve global property {}", key, ex);
			}
		}
		if (StringUtils.isBlank(resolved)) {
			Properties moduleProps = getModuleProperties();
			if (moduleProps != null) {
				resolved = moduleProps.getProperty(key);
			}
		}
		return StringUtils.trimToNull(resolved);
	}
	
	private static Properties getModuleProperties() {
		if (cachedModuleProperties != null) {
			return cachedModuleProperties;
		}
		synchronized (CentralFhirHttpTimeoutConfigurer.class) {
			if (cachedModuleProperties != null) {
				return cachedModuleProperties;
			}
			cachedModuleProperties = ModuleClasspathPropertiesLoader.loadMergedInOrder("ihmodule.properties",
			    "patientdataexchange-application.properties");
			return cachedModuleProperties;
		}
	}
}
