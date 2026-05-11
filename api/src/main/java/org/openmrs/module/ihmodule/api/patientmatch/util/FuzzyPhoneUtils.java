package org.openmrs.module.ihmodule.api.patientmatch.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

public final class FuzzyPhoneUtils {
	
	private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();
	
	private FuzzyPhoneUtils() {
	}
	
	public static String normalizeDigits(String value) {
		if (StringUtils.isBlank(value)) {
			return "";
		}
		return value.replaceAll("[^0-9]", "");
	}
	
	public static double similarityPercent(String left, String right) {
		String a = normalizeDigits(left);
		String b = normalizeDigits(right);
		if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) {
			return 0.0d;
		}
		if (StringUtils.equals(a, b)) {
			return 100.0d;
		}
		if (a.length() >= 7 && b.length() >= 7
		        && StringUtils.equals(a.substring(a.length() - 7), b.substring(b.length() - 7))) {
			return 90.0d;
		}
		Integer distance = LEVENSHTEIN.apply(a, b);
		if (distance == null) {
			return 0.0d;
		}
		int maxLength = Math.max(a.length(), b.length());
		if (maxLength <= 0) {
			return 0.0d;
		}
		double similarity = 1.0d - ((double) distance.intValue() / (double) maxLength);
		return FuzzyTextUtils.round(Math.max(0.0d, similarity * 100.0d));
	}
}
