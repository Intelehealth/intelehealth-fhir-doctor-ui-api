package org.openmrs.module.ihmodule.api.patientmatch.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FuzzyPatientMatchResult {
	
	private final FuzzyPatientCandidate candidate;
	
	private final double overallMatchScore;
	
	private final String confidence;
	
	private final Map<String, Double> fieldScores;
	
	private final List<String> matchedFields;
	
	private String ruleMatchResult;
	
	public FuzzyPatientMatchResult(FuzzyPatientCandidate candidate, double overallMatchScore, String confidence,
	    Map<String, Double> fieldScores, List<String> matchedFields) {
		this.candidate = candidate;
		this.overallMatchScore = overallMatchScore;
		this.confidence = confidence;
		this.fieldScores = new LinkedHashMap<String, Double>(fieldScores);
		this.matchedFields = new ArrayList<String>(matchedFields);
	}
	
	public FuzzyPatientCandidate getCandidate() {
		return candidate;
	}
	
	public double getOverallMatchScore() {
		return overallMatchScore;
	}
	
	public double getNormalizedMatchScore() {
		return overallMatchScore / 100.0d;
	}
	
	public String getConfidence() {
		return confidence;
	}
	
	public Map<String, Double> getFieldScores() {
		return new LinkedHashMap<String, Double>(fieldScores);
	}
	
	public List<String> getMatchedFields() {
		return new ArrayList<String>(matchedFields);
	}
	
	public String getRuleMatchResult() {
		return ruleMatchResult;
	}
	
	public void setRuleMatchResult(String ruleMatchResult) {
		this.ruleMatchResult = ruleMatchResult;
	}
}
