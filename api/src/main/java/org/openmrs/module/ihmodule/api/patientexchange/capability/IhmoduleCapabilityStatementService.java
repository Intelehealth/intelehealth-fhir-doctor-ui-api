package org.openmrs.module.ihmodule.api.patientexchange.capability;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.utils.ModuleClasspathPropertiesLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serves the ihmodule interface CapabilityStatement (companion to
 * {@code docs/IHMODULE-Interface-CapabilityStatement.json}).
 */
@Service("ihmoduleCapabilityStatementService")
public class IhmoduleCapabilityStatementService {
	
	private static final String TEMPLATE_RESOURCE = "IHMODULE-Interface-CapabilityStatement.json";
	
	private static final String MODULE_VERSION = "1.0.0-SNAPSHOT";
	
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> getInterfaceCapability() {
		Map<String, Object> document = loadTemplate();
		patchDeployment(document);
		return document;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> loadTemplate() {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		if (loader == null) {
			loader = IhmoduleCapabilityStatementService.class.getClassLoader();
		}
		try (InputStream in = loader.getResourceAsStream(TEMPLATE_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + TEMPLATE_RESOURCE);
			}
			return JSON_MAPPER.readValue(in, LinkedHashMap.class);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load " + TEMPLATE_RESOURCE, ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void patchDeployment(Map<String, Object> document) {
		document.put("date", formatTodayUtc());
		
		Map<String, Object> software = (Map<String, Object>) document.get("software");
		if (software != null) {
			software.put("version", MODULE_VERSION);
		}
		
		String openmrsBase = resolveOpenMrsBaseUrl();
		String centralFhir = resolveCentralFhirUrl();
		
		Map<String, Object> implementation = (Map<String, Object>) document.get("implementation");
		if (implementation != null && StringUtils.isNotBlank(openmrsBase)) {
			implementation.put("url", openmrsBase + "/ws/rest/v1/ihmodule");
			implementation.put("description", "Configured OpenMRS ihmodule REST base (local.openmrs.url)");
		}
		
		List<Map<String, Object>> dependencies = (List<Map<String, Object>>) document.get("externalFhirDependencies");
		if (dependencies != null) {
			for (Map<String, Object> dependency : dependencies) {
				String system = String.valueOf(dependency.get("system"));
				if (system.contains("OpenCR") && StringUtils.isNotBlank(centralFhir)) {
					dependency.put("base", centralFhir);
				} else if (system.contains("FHIR2") && StringUtils.isNotBlank(openmrsBase)) {
					dependency.put("base", openmrsBase + "/ws/fhir2/R4");
				}
			}
		}
	}
	
	static String resolveOpenMrsBaseUrl() {
		String fromGp = resolveGlobalProperty("local.openmrs.url", "ihmodule.local.openmrs.url");
		if (StringUtils.isNotBlank(fromGp)) {
			return normalizeBaseUrl(fromGp);
		}
		return normalizeBaseUrl(resolveClasspathProperty("local.openmrs.url", "local.openmrs.openhim.url"));
	}
	
	static String resolveCentralFhirUrl() {
		String fromGp = resolveGlobalProperty("opencr.openhim.url", "intelehealth.fhir.central.url");
		if (StringUtils.isNotBlank(fromGp)) {
			return normalizeBaseUrl(fromGp);
		}
		return normalizeBaseUrl(resolveClasspathProperty("opencr.openhim.url", "intelehealth.fhir.central.url"));
	}
	
	private static String resolveGlobalProperty(String... keys) {
		try {
			if (Context.isSessionOpen()) {
				for (String key : keys) {
					if (StringUtils.isBlank(key)) {
						continue;
					}
					String value = Context.getAdministrationService().getGlobalProperty(key);
					if (StringUtils.isNotBlank(value) && !value.contains("${")) {
						return value.trim();
					}
				}
			}
		}
		catch (Exception ignored) {
			// Public endpoint may run without an authenticated admin context.
		}
		return null;
	}
	
	private static String resolveClasspathProperty(String... keys) {
		Properties props = ModuleClasspathPropertiesLoader.loadModuleProperties();
		if (props == null) {
			return null;
		}
		for (String key : keys) {
			if (StringUtils.isBlank(key)) {
				continue;
			}
			String value = props.getProperty(key);
			if (StringUtils.isNotBlank(value) && !value.contains("${")) {
				return value.trim();
			}
		}
		return null;
	}
	
	static String normalizeBaseUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return "";
		}
		String normalized = url.trim();
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
	
	private static String formatTodayUtc() {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date());
	}
}
