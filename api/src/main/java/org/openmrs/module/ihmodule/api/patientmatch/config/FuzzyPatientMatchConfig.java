package org.openmrs.module.ihmodule.api.patientmatch.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class FuzzyPatientMatchConfig {
	
	private final boolean enabled;
	
	private final int threshold;
	
	private final int confidenceHighThreshold;
	
	private final int confidenceMediumThreshold;
	
	private final int fieldMatchThreshold;
	
	private final int maxCandidates;
	
	private final String nameAlgorithm;
	
	private final String phoneAlgorithm;
	
	private final String addressAlgorithm;
	
	private final boolean phoneticBoostEnabled;
	
	private final int dobNearMatchDays;
	
	private final Map<String, Boolean> fieldEnabled;
	
	private final Map<String, Double> fieldWeight;
	
	public FuzzyPatientMatchConfig(boolean enabled, int threshold, int confidenceHighThreshold,
	        int confidenceMediumThreshold, int fieldMatchThreshold, int maxCandidates, String nameAlgorithm,
	        String phoneAlgorithm, String addressAlgorithm, boolean phoneticBoostEnabled, int dobNearMatchDays,
	        Map<String, Boolean> fieldEnabled, Map<String, Double> fieldWeight) {
		this.enabled = enabled;
		this.threshold = threshold;
		this.confidenceHighThreshold = confidenceHighThreshold;
		this.confidenceMediumThreshold = confidenceMediumThreshold;
		this.fieldMatchThreshold = fieldMatchThreshold;
		this.maxCandidates = maxCandidates;
		this.nameAlgorithm = nameAlgorithm;
		this.phoneAlgorithm = phoneAlgorithm;
		this.addressAlgorithm = addressAlgorithm;
		this.phoneticBoostEnabled = phoneticBoostEnabled;
		this.dobNearMatchDays = dobNearMatchDays;
		this.fieldEnabled = new LinkedHashMap<>(fieldEnabled);
		this.fieldWeight = new LinkedHashMap<>(fieldWeight);
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public int getThreshold() {
		return threshold;
	}
	
	public int getConfidenceHighThreshold() {
		return confidenceHighThreshold;
	}
	
	public int getConfidenceMediumThreshold() {
		return confidenceMediumThreshold;
	}
	
	public int getFieldMatchThreshold() {
		return fieldMatchThreshold;
	}
	
	public int getMaxCandidates() {
		return maxCandidates;
	}
	
	public String getNameAlgorithm() {
		return nameAlgorithm;
	}
	
	public String getPhoneAlgorithm() {
		return phoneAlgorithm;
	}
	
	public String getAddressAlgorithm() {
		return addressAlgorithm;
	}
	
	public boolean isPhoneticBoostEnabled() {
		return phoneticBoostEnabled;
	}
	
	public int getDobNearMatchDays() {
		return dobNearMatchDays;
	}
	
	public boolean isFieldEnabled(String fieldName) {
		Boolean enabledField = fieldEnabled.get(fieldName);
		return enabledField != null && enabledField.booleanValue();
	}
	
	public double getFieldWeight(String fieldName) {
		Double weight = fieldWeight.get(fieldName);
		return weight != null ? weight.doubleValue() : 0.0d;
	}
	
	public Map<String, Boolean> getFieldEnabled() {
		return new LinkedHashMap<>(fieldEnabled);
	}
	
	public Map<String, Double> getFieldWeight() {
		return new LinkedHashMap<>(fieldWeight);
	}
}
