package org.openmrs.module.ihmodule.api.patientmatch.phonetic;

import org.apache.commons.lang3.StringUtils;

/**
 * Phonetic encoders selectable from {@code patient-match-rules.json} ({@code matcher.algorithm} /
 * {@code similarity.algorithm}).
 */
public enum PhoneticAlgorithm {
	
	METAPHONE,
	
	DOUBLE_METAPHONE,
	
	/** Legacy alias kept for existing rules. */
	SOUNDEX;
	
	public static boolean isPhonetic(String rawAlgorithm) {
		if (StringUtils.isBlank(rawAlgorithm)) {
			return false;
		}
		try {
			fromConfig(rawAlgorithm);
			return true;
		}
		catch (IllegalArgumentException ignored) {
			return false;
		}
	}
	
	public static PhoneticAlgorithm fromConfig(String rawAlgorithm) {
		if (StringUtils.isBlank(rawAlgorithm)) {
			throw new IllegalArgumentException("Phonetic algorithm is required");
		}
		String normalized = rawAlgorithm.trim().toUpperCase().replace('-', '_');
		if ("DOUBLEMETAPHONE".equals(normalized)) {
			normalized = "DOUBLE_METAPHONE";
		}
		try {
			return valueOf(normalized);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unsupported phonetic algorithm: " + rawAlgorithm
			        + ". Supported: METAPHONE, DOUBLE_METAPHONE, SOUNDEX", ex);
		}
	}
}
