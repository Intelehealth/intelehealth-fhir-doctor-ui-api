package org.openmrs.module.ihmodule.api.patientexchange.config;

import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Properties;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import org.apache.commons.lang3.StringUtils;

@Component
public class FhirConfig extends IHConstant {
	
	private static final Logger log = LoggerFactory.getLogger(FhirConfig.class);
	
	private static volatile Properties cachedModuleProperties;
	
	FhirContext fhirContext = FhirContextHolder.R4;
	
	public String getOpencrOpenhimURL() {
		return opencrOpenhimURL;
	}
	
	public IGenericClient getOpenCRFhirContext() {
		String resolvedOpencrUrl = resolveStringProperty(opencrOpenhimURL, "opencr.openhim.url",
		    "intelehealth.fhir.opencr.openhim.url");
		System.err.println("opencrOpenhimURL:" + resolvedOpencrUrl);
		IGenericClient openCr = fhirContext.newRestfulGenericClient(resolvedOpencrUrl);
		String resolvedOpencrAuth = resolveStringProperty(opencrOpenhimAuthentication,
		    "opencr.openhim.clientid.password.basic.auth", "intelehealth.fhir.opencr.openhim.authentication");
		String[] credentials = splitCredentials(resolvedOpencrAuth, "opencr.openhim.clientid.password.basic.auth");
		BasicAuthInterceptor b = new BasicAuthInterceptor(credentials[0], credentials[1]);
		openCr.registerInterceptor(b);
		return openCr;
		
	}
	
	public IGenericClient getLocalOpenMRSFhirContext() {
		String resolvedLocalUrl = getResolvedLocalOpenmrsBaseUrl();
		IGenericClient openMRSServer = fhirContext.newRestfulGenericClient(resolvedLocalUrl + "/ws/fhir2/R4");
		String resolvedLocalAuth = resolveStringProperty(localOpenmrsOpenhimAuthentication,
		    "local.openmrs.openhim.clientid.password.basic.auth", "intelehealth.fhir.local.openmrs.openhim.authentication");
		String[] credentials = splitCredentials(resolvedLocalAuth, "local.openmrs.openhim.clientid.password.basic.auth");
		BasicAuthInterceptor openmrsAuthentication = new BasicAuthInterceptor(credentials[0], credentials[1]);
		openMRSServer.registerInterceptor(openmrsAuthentication);
		return openMRSServer;
		
	}
	
	public IGenericClient getGOFRFhirContext() {
		String resolvedGofrUrl = resolveStringProperty(gofrOpenhimURL, "gofr.openhim.url",
		    "intelehealth.fhir.gofr.openhim.url");
		IGenericClient openMRSServer = fhirContext.newRestfulGenericClient(resolvedGofrUrl);
		String resolvedGofrAuth = resolveStringProperty(gofrOpenhimAuthentication,
		    "gofr.openhim.clientid.password.basic.auth", "intelehealth.fhir.gofr.openhim.authentication");
		String[] credentials = splitCredentials(resolvedGofrAuth, "gofr.openhim.clientid.password.basic.auth");
		BasicAuthInterceptor openmrsAuthentication = new BasicAuthInterceptor(credentials[0], credentials[1]);
		openMRSServer.registerInterceptor(openmrsAuthentication);
		return openMRSServer;
		
	}
	
	public String[] getOpenMRSCredentials() {
		String resolvedLocalAuth = resolveStringProperty(localOpenmrsOpenhimAuthentication,
		    "local.openmrs.openhim.clientid.password.basic.auth", "intelehealth.fhir.local.openmrs.openhim.authentication");
		return splitCredentials(resolvedLocalAuth, "local.openmrs.openhim.clientid.password.basic.auth");
	}
	
	/** Basic-auth parts for central OpenCR FHIR server ({@link #opencrOpenhimURL}). */
	public String[] getOpenCRCredentials() {
		String resolvedOpencrAuth = resolveStringProperty(opencrOpenhimAuthentication,
		    "opencr.openhim.clientid.password.basic.auth", "intelehealth.fhir.opencr.openhim.authentication");
		return splitCredentials(resolvedOpencrAuth, "opencr.openhim.clientid.password.basic.auth");
	}
	
	public String getResolvedLocalOpenmrsBaseUrl() {
		return resolveStringProperty(localOpenmrsOpenhimURL, "local.openmrs.openhim.url",
		    "intelehealth.fhir.local.openmrs.openhim.url");
	}
	
	public String getPatientImportPreferredIdentifierTypeUuid() {
		return resolveStringProperty("", "intelehealth.fhir.patient.import.preferred.identifier.type.uuid", null);
	}
	
	public boolean isPatientImportDemographicDuplicateCheckEnabled() {
		return resolveBooleanProperty("intelehealth.fhir.patient.import.demographic.duplicate.check.enabled", false);
	}
	
	public int getPatientImportDemographicDuplicateCheckSearchCount() {
		return resolveIntProperty("intelehealth.fhir.patient.import.demographic.duplicate.check.search.count", 20);
	}
	
	public boolean isPatientImportDemographicDuplicateFallbackEnabled() {
		return resolveBooleanProperty("intelehealth.fhir.patient.import.demographic.duplicate.check.fallback.enabled", false);
	}
	
	public int getPatientImportDemographicDuplicateFallbackMaxPages() {
		return resolveIntProperty("intelehealth.fhir.patient.import.demographic.duplicate.check.fallback.max.pages", 2);
	}
	
	public boolean isPatientImportProfileValidationEnabled() {
		return resolveBooleanProperty("intelehealth.fhir.patient.import.profile.validation.enabled", true);
	}
	
	public String getPatientProfileUrl() {
		return patientProfileUrl;
	}
	
	private String[] splitCredentials(String rawCredential, String propertyName) {
		log.info("Credential debug property={} rawLength={} maskedValue={}", propertyName,
		    rawCredential != null ? rawCredential.length() : 0, maskCredential(rawCredential));
		String[] parts = StringUtils.defaultString(rawCredential).split(":", 2);
		if (parts.length != 2 || StringUtils.isBlank(parts[0])) {
			log.warn("Invalid basic-auth credential format for property={} rawLength={} maskedValue={}", propertyName,
			    rawCredential != null ? rawCredential.length() : 0, maskCredential(rawCredential));
			throw new IllegalStateException("Invalid basic-auth config for " + propertyName
			        + ". Expected format: username:password");
		}
		return parts;
	}
	
	private String maskCredential(String rawCredential) {
		if (rawCredential == null) {
			return "<null>";
		}
		String[] parts = rawCredential.split(":", 2);
		if (parts.length != 2) {
			return "<invalid-format>";
		}
		String username = StringUtils.defaultString(parts[0]);
		String password = StringUtils.defaultString(parts[1]);
		if (password.isEmpty()) {
			return username + ":<empty>";
		}
		if (password.length() <= 2) {
			return username + ":" + StringUtils.repeat('*', password.length());
		}
		return username + ":" + StringUtils.repeat('*', password.length() - 2) + password.substring(password.length() - 2);
	}
	
	private boolean resolveBooleanProperty(String key, boolean defaultValue) {
		String raw = resolveStringProperty("", key, null);
		if (StringUtils.isBlank(raw)) {
			return defaultValue;
		}
		return Boolean.parseBoolean(raw.trim());
	}
	
	private int resolveIntProperty(String key, int defaultValue) {
		String raw = resolveStringProperty("", key, null);
		if (StringUtils.isBlank(raw)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(raw.trim());
		}
		catch (NumberFormatException ex) {
			log.warn("Invalid integer for property '{}': '{}'. Using default={}", key, raw, defaultValue);
			return defaultValue;
		}
	}
	
	private String resolveStringProperty(String inMemoryValue, String primaryKey, String legacyKey) {
		if (StringUtils.isNotBlank(inMemoryValue) && !inMemoryValue.contains("${")) {
			return inMemoryValue.trim();
		}
		String resolved = null;
		try {
			if (Context.isSessionOpen()) {
				resolved = Context.getAdministrationService().getGlobalProperty(primaryKey);
				if (StringUtils.isBlank(resolved) && StringUtils.isNotBlank(legacyKey)) {
					resolved = Context.getAdministrationService().getGlobalProperty(legacyKey);
				}
			}
		}
		catch (Exception ex) {
			log.debug("Unable to resolve property from OpenMRS global properties. key={}, legacyKey={}", primaryKey,
			    legacyKey, ex);
		}
		if (StringUtils.isBlank(resolved)) {
			Properties moduleProps = getModuleProperties();
			if (moduleProps != null) {
				resolved = moduleProps.getProperty(primaryKey);
				if (StringUtils.isBlank(resolved) && StringUtils.isNotBlank(legacyKey)) {
					resolved = moduleProps.getProperty(legacyKey);
				}
			}
		}
		return StringUtils.defaultString(resolved).trim();
	}
	
	private Properties getModuleProperties() {
		if (cachedModuleProperties != null) {
			return cachedModuleProperties;
		}
		synchronized (FhirConfig.class) {
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
				log.info("Loaded module properties fallback from classpath resource '{}'", resource);
				return p;
			}
			catch (Exception ex) {
				log.warn("Unable to load module properties fallback resource '{}': {}", resource, ex.getMessage());
			}
		}
		log.warn("No module properties fallback resource found in classpath");
		return null;
	}
}
