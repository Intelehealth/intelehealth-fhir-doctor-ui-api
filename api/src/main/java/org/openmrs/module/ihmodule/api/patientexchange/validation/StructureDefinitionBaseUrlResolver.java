package org.openmrs.module.ihmodule.api.patientexchange.validation;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.utils.ModuleClasspathPropertiesLoader;

/**
 * Resolves the FHIR server base used for {@code .../StructureDefinition/...} extension URLs.
 */
public final class StructureDefinitionBaseUrlResolver {
	
	private static final String PROP_STRUCTURE_DEFINITION_EXTENSION_URL = "intelehealth.fhir.structuredefinition.extension.url";
	
	private static final String PROP_CENTRAL_FHIR_URL = "intelehealth.fhir.central.url";
	
	private StructureDefinitionBaseUrlResolver() {
	}
	
	public static String resolve(FhirConfig fhirConfig) {
		if (fhirConfig != null) {
			String fromBean = fhirConfig.getStructureDefinitionBaseUrl();
			if (StringUtils.isNotBlank(fromBean)) {
				return fromBean;
			}
		}
		Properties properties = ModuleClasspathPropertiesLoader.loadMergedInOrder("ihmodule.properties",
		    "patientdataexchange-application.properties");
		if (properties != null) {
			String fromSd = parseBaseFromStructureDefinitionProperty(properties
			        .getProperty(PROP_STRUCTURE_DEFINITION_EXTENSION_URL));
			if (StringUtils.isNotBlank(fromSd)) {
				return fromSd;
			}
			String fromCentral = trimTrailingSlashes(properties.getProperty(PROP_CENTRAL_FHIR_URL));
			if (StringUtils.isNotBlank(fromCentral)) {
				return fromCentral;
			}
		}
		return null;
	}
	
	private static String parseBaseFromStructureDefinitionProperty(String raw) {
		if (StringUtils.isBlank(raw) || raw.contains("${")) {
			return null;
		}
		String trimmed = raw.trim();
		int i = trimmed.toLowerCase().indexOf("/structuredefinition");
		if (i > 0) {
			return trimTrailingSlashes(trimmed.substring(0, i));
		}
		return trimTrailingSlashes(trimmed);
	}
	
	private static String trimTrailingSlashes(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed.isEmpty() ? null : trimmed;
	}
}
