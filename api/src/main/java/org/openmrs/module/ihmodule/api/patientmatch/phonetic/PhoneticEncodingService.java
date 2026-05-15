package org.openmrs.module.ihmodule.api.patientmatch.phonetic;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Config-driven phonetic encoding and comparison (Metaphone, Double Metaphone, Soundex).
 */
@Service
public class PhoneticEncodingService {
	
	private static final Logger log = LoggerFactory.getLogger(PhoneticEncodingService.class);
	
	public String getPhoneticCode(String value, PhoneticAlgorithm algorithm) {
		if (algorithm == null) {
			throw new IllegalArgumentException("Phonetic algorithm is required");
		}
		String normalized = FuzzyTextUtils.normalize(value);
		if (StringUtils.isBlank(normalized)) {
			return "";
		}
		log.debug("Computing phonetic code using algorithm={} for value length={}", algorithm, normalized.length());
		switch (algorithm) {
			case METAPHONE:
				return encodeMetaphonePrimary(normalized);
			case DOUBLE_METAPHONE:
				return encodeDoubleMetaphonePrimaryAndAlternate(normalized);
			case SOUNDEX:
				return encodeSoundex(normalized);
			default:
				throw new IllegalArgumentException("Unsupported phonetic algorithm: " + algorithm);
		}
	}
	
	public boolean phoneticMatch(String left, String right, PhoneticAlgorithm algorithm) {
		if (algorithm == null) {
			throw new IllegalArgumentException("Phonetic algorithm is required");
		}
		String a = FuzzyTextUtils.normalize(left);
		String b = FuzzyTextUtils.normalize(right);
		if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
			return false;
		}
		log.debug("Phonetic match using algorithm={}", algorithm);
		switch (algorithm) {
			case METAPHONE:
				return StringUtils.equals(encodeMetaphonePrimary(a), encodeMetaphonePrimary(b));
			case DOUBLE_METAPHONE:
				return new DoubleMetaphone().isDoubleMetaphoneEqual(a, b);
			case SOUNDEX:
				return StringUtils.equals(encodeSoundex(a), encodeSoundex(b));
			default:
				throw new IllegalArgumentException("Unsupported phonetic algorithm: " + algorithm);
		}
	}
	
	public boolean phoneticMatch(String left, String right, String rawAlgorithm) {
		return phoneticMatch(left, right, PhoneticAlgorithm.fromConfig(rawAlgorithm));
	}
	
	private static String encodeMetaphonePrimary(String normalized) {
		return StringUtils.defaultString(new DoubleMetaphone().doubleMetaphone(normalized));
	}
	
	private static String encodeDoubleMetaphonePrimaryAndAlternate(String normalized) {
		DoubleMetaphone encoder = new DoubleMetaphone();
		String primary = StringUtils.defaultString(encoder.doubleMetaphone(normalized));
		String alternate = StringUtils.defaultString(encoder.doubleMetaphone(normalized, true));
		if (StringUtils.isBlank(alternate) || StringUtils.equals(primary, alternate)) {
			return primary;
		}
		return primary + "|" + alternate;
	}
	
	private static String encodeSoundex(String normalized) {
		try {
			return StringUtils.defaultString(new Soundex().encode(normalized));
		}
		catch (IllegalArgumentException ex) {
			log.debug("Soundex encode failed for value; returning empty code", ex);
			return "";
		}
	}
}
