package org.openmrs.module.ihmodule.api.patientmatch.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyTextUtils;
import org.springframework.stereotype.Component;

@Component
public class WeightedScoreAggregator {
	
	public FuzzyPatientMatchResult aggregate(FuzzyPatientCandidate candidate, FuzzyPatientMatchRequest request,
	        FuzzyPatientMatchConfig config, Map<String, Double> fieldScores) {
		double scoreSum = 0.0d;
		double weightSum = 0.0d;
		List<String> matchedFields = new ArrayList<String>();
		
		for (Map.Entry<String, Double> entry : fieldScores.entrySet()) {
			String field = entry.getKey();
			if (!config.isFieldEnabled(field) || !isRequested(field, request)) {
				continue;
			}
			double weight = config.getFieldWeight(field);
			double score = entry.getValue() != null ? entry.getValue().doubleValue() : 0.0d;
			scoreSum += score * weight;
			weightSum += weight;
			if (score >= config.getFieldMatchThreshold()) {
				matchedFields.add(field);
			}
		}
		
		double overall = weightSum > 0.0d ? FuzzyTextUtils.round(scoreSum / weightSum) : 0.0d;
		return new FuzzyPatientMatchResult(candidate, overall, resolveConfidence(overall, config), fieldScores,
		        matchedFields);
	}
	
	private boolean isRequested(String fieldName, FuzzyPatientMatchRequest request) {
		if ("identifier".equals(fieldName)) {
			return request.hasIdentifier();
		}
		if ("name".equals(fieldName)) {
			return request.hasName();
		}
		if ("dob".equals(fieldName)) {
			return request.hasBirthDate();
		}
		if ("phone".equals(fieldName)) {
			return request.hasPhone();
		}
		if ("address".equals(fieldName)) {
			return request.hasAddress();
		}
		if ("gender".equals(fieldName)) {
			return request.hasGender();
		}
		return false;
	}
	
	private String resolveConfidence(double overall, FuzzyPatientMatchConfig config) {
		if (overall >= config.getConfidenceHighThreshold()) {
			return "HIGH";
		}
		if (overall >= config.getConfidenceMediumThreshold()) {
			return "MEDIUM";
		}
		return "LOW";
	}
}
