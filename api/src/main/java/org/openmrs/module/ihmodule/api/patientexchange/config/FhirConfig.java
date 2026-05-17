package org.openmrs.module.ihmodule.api.patientexchange.config;

import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.openmrs.module.ihmodule.api.patientexchange.utils.ModuleClasspathPropertiesLoader;
import org.openmrs.module.ihmodule.api.patientexchange.utils.OpenhimUrlAuthorityExtractor;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	/**
	 * Effective OpenCR / OpenHIM URL after global property resolution (same input as
	 * {@link #getOpenCRFhirContext()}).
	 */
	public String getResolvedOpenCrOpenhimUrl() {
		return resolveStringProperty(opencrOpenhimURL, "opencr.openhim.url", "intelehealth.fhir.opencr.openhim.url");
	}
	
	public IGenericClient getOpenCRFhirContext() {
		String resolvedOpencrUrl = resolveStringProperty(opencrOpenhimURL, "opencr.openhim.url",
		    "intelehealth.fhir.opencr.openhim.url");
		System.err.println("opencrOpenhimURL:" + resolvedOpencrUrl);
		IGenericClient openCr = fhirContext.newRestfulGenericClient(resolvedOpencrUrl);
		String[] credentials = getOpenHimMediatorCredentials();
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
	
	/**
	 * FHIR server base for StructureDefinition extension URLs, e.g.
	 * {@code http://host/fhir/StructureDefinition/Caste}.
	 */
	public String getStructureDefinitionBaseUrl() {
		String fromSdProperty = resolveStringProperty(sdExtensionURL, "intelehealth.fhir.structuredefinition.extension.url",
		    null);
		if (StringUtils.isNotBlank(fromSdProperty) && !fromSdProperty.contains("${")) {
			String trimmed = fromSdProperty.trim();
			int i = trimmed.toLowerCase().indexOf("/structuredefinition");
			if (i > 0) {
				String base = trimmed.substring(0, i).trim();
				while (base.endsWith("/")) {
					base = base.substring(0, base.length() - 1);
				}
				return base;
			}
		}
		String central = resolveStringProperty(centralFhirURL, "intelehealth.fhir.central.url", null);
		if (StringUtils.isNotBlank(central) && !central.contains("${")) {
			String base = central.trim();
			while (base.endsWith("/")) {
				base = base.substring(0, base.length() - 1);
			}
			return base;
		}
		return null;
	}
	
	/**
	 * OpenCR / OpenHIM client channel: {@code opencr.openhim.clientid.password.basic.auth} only (
	 * {@code user:password}, e.g. {@code fhir_app} for {@code POST /fhir/$mdm-match}).
	 */
	public String[] getOpenHimCommonCredentials() {
		String client = resolveStringProperty("", "opencr.openhim.clientid.password.basic.auth", null);
		if (StringUtils.isBlank(client)) {
			throw new IllegalStateException(
			        "Missing opencr.openhim.clientid.password.basic.auth (expected user:password for client channel)");
		}
		return splitCredentials(client.trim(), "opencr.openhim.clientid.password.basic.auth");
	}
	
	/**
	 * OpenCR / OpenHIM mediator channel: {@code opencr.openhim.mediator.password.basic.auth} only (
	 * {@code user:password}).
	 */
	public String[] getOpenHimMediatorCredentials() {
		String mediator = resolveStringProperty("", "opencr.openhim.mediator.password.basic.auth", null);
		if (StringUtils.isBlank(mediator)) {
			throw new IllegalStateException(
			        "Missing opencr.openhim.mediator.password.basic.auth (expected user:password for mediator channel)");
		}
		return splitCredentials(mediator.trim(), "opencr.openhim.mediator.password.basic.auth");
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
	
	/**
	 * When {@code true}, upload import creates {@link org.openmrs.Patient} via
	 * {@code PatientService.savePatient} instead of FHIR2 {@code POST Patient}. Default
	 * {@code false} preserves legacy FHIR2 create behaviour.
	 */
	public boolean isPatientImportNativeCreateEnabled() {
		return resolveBooleanProperty("intelehealth.fhir.patient.import.native.create.enabled", false);
	}
	
	/**
	 * When {@code true}, patient upload import uses OpenMRS fuzzy {@code $match} plus central FHIR
	 * {@code $mdm-match} instead of exact identifier / demographic duplicate checks. Default
	 * {@code false} preserves legacy import behaviour.
	 */
	public boolean isPatientImportFuzzyMatchEnabled() {
		return resolveBooleanProperty("mpi.import.fuzzy.match.enabled", false);
	}
	
	/**
	 * Authority for {@code POST /fhir/$mdm-match} (no path except scheme/host/port).
	 * <ol>
	 * <li>If {@code opencr.openhim.mdm.authority.base.url} is set (GP or module), that value is
	 * normalized to an authority (path stripped).</li>
	 * <li>Otherwise derived from {@code opencr.openhim.url} via
	 * {@link OpenhimUrlAuthorityExtractor#extractAuthorityBase(String)} — e.g.
	 * {@code http://192.168.19.152:6001/openmrs-fhir-mdm/patient-create} →
	 * {@code http://192.168.19.152:6001}, so the MDM URL is
	 * {@code http://192.168.19.152:6001/fhir/$mdm-match}.</li>
	 * </ol>
	 */
	public String resolveMdmMatchAuthorityBaseUrl() {
		String explicit = resolveStringProperty("", "opencr.openhim.mdm.authority.base.url", null);
		String raw = StringUtils.isNotBlank(explicit) ? explicit.trim() : getResolvedOpenCrOpenhimUrl();
		String authority = OpenhimUrlAuthorityExtractor.extractAuthorityBase(raw);
		return stripTrailingSlash(authority);
	}
	
	private static String stripTrailingSlash(String authority) {
		if (StringUtils.isBlank(authority)) {
			return null;
		}
		String t = authority.trim();
		while (t.endsWith("/")) {
			t = t.substring(0, t.length() - 1);
		}
		return t.isEmpty() ? null : t;
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
			cachedModuleProperties = ModuleClasspathPropertiesLoader.loadMergedInOrder("ihmodule.properties",
			    "patientdataexchange-application.properties");
			if (cachedModuleProperties == null) {
				log.warn("No module classpath properties merged (ihmodule.properties / patientdataexchange-application.properties)");
			}
			return cachedModuleProperties;
		}
	}
}
