package org.openmrs.module.ihmodule.api.patientmatch.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

public final class FuzzyTextUtils {
	
	private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
	
	private static final DoubleMetaphone DOUBLE_METAPHONE = new DoubleMetaphone();
	
	private static final Soundex SOUNDEX = new Soundex();
	
	private FuzzyTextUtils() {
	}
	
	public static String normalize(String value) {
		if (StringUtils.isBlank(value)) {
			return "";
		}
		String trimmed = value.trim().toLowerCase(Locale.ENGLISH);
		return trimmed.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
	}
	
	public static Set<String> tokenize(String value) {
		String normalized = normalize(value);
		if (StringUtils.isBlank(normalized)) {
			return new LinkedHashSet<String>();
		}
		return new LinkedHashSet<String>(Arrays.asList(normalized.split(" ")));
	}
	
	public static String firstToken(String value) {
		Set<String> tokens = tokenize(value);
		return tokens.isEmpty() ? "" : tokens.iterator().next();
	}
	
	public static String lastToken(String value) {
		Set<String> tokens = tokenize(value);
		String last = "";
		for (String token : tokens) {
			last = token;
		}
		return last;
	}
	
	public static double jaroWinklerPercent(String left, String right) {
		String a = normalize(left);
		String b = normalize(right);
		if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
			return 0.0d;
		}
		Double similarity = JARO_WINKLER.apply(a, b);
		return similarity != null ? round(similarity.doubleValue() * 100.0d) : 0.0d;
	}
	
	public static boolean phoneticMatch(String left, String right) {
		String a = normalize(left);
		String b = normalize(right);
		if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
			return false;
		}
		try {
			if (StringUtils.equals(SOUNDEX.encode(a), SOUNDEX.encode(b))) {
				return true;
			}
		}
		catch (IllegalArgumentException ignored) {
			// Soundex can reject some strings; DoubleMetaphone is still attempted.
		}
		return StringUtils.equals(DOUBLE_METAPHONE.doubleMetaphone(a), DOUBLE_METAPHONE.doubleMetaphone(b));
	}
	
	public static double tokenJaccardPercent(String left, String right) {
		Set<String> leftTokens = tokenize(left);
		Set<String> rightTokens = tokenize(right);
		if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
			return 0.0d;
		}
		int intersection = 0;
		for (String token : leftTokens) {
			if (rightTokens.contains(token)) {
				intersection++;
			}
		}
		int union = leftTokens.size() + rightTokens.size() - intersection;
		if (union <= 0) {
			return 0.0d;
		}
		return round(((double) intersection / (double) union) * 100.0d);
	}
	
	public static double round(double value) {
		return Math.round(value * 10.0d) / 10.0d;
	}
}
