package org.openmrs.module.ihmodule.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Reads {@code ihmodule.properties} on the classpath (from the ihmodule API jar).
 */
public final class IhmoduleProperties {
	
	private static final String BUNDLE = "ihmodule";
	
	private static final String KEY_PATIENT_EXCHANGE_BASE_URL = "patientexchange.baseUrl";
	
	private IhmoduleProperties() {
	}
	
	/**
	 * Base URL of the patient data exchange Spring Boot app (no trailing slash), from
	 * {@code ihmodule.properties}.
	 */
	public static String getPatientExchangeBaseUrl() {
		try {
			ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
			String v = bundle.getString(KEY_PATIENT_EXCHANGE_BASE_URL);
			return v != null ? v.trim() : "";
		}
		catch (MissingResourceException e) {
			return "";
		}
	}
}
