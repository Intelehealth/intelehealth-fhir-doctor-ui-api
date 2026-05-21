package org.openmrs.module.ihmodule.api.patientmatch.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientMatchRules {
	
	private String version;
	
	private List<String> mdmTypes;
	
	private List<CandidateSearchRule> candidateSearchParams;
	
	private List<MatchFieldRule> matchFields;
	
	private Map<String, Object> matchResultMap;
	
	private Settings settings;
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public List<String> getMdmTypes() {
		return mdmTypes;
	}
	
	public void setMdmTypes(List<String> mdmTypes) {
		this.mdmTypes = mdmTypes;
	}
	
	public List<CandidateSearchRule> getCandidateSearchParams() {
		return candidateSearchParams;
	}
	
	public void setCandidateSearchParams(List<CandidateSearchRule> candidateSearchParams) {
		this.candidateSearchParams = candidateSearchParams;
	}
	
	public List<MatchFieldRule> getMatchFields() {
		return matchFields;
	}
	
	public void setMatchFields(List<MatchFieldRule> matchFields) {
		this.matchFields = matchFields;
	}
	
	public Map<String, Object> getMatchResultMap() {
		return matchResultMap;
	}
	
	public void setMatchResultMap(Map<String, Object> matchResultMap) {
		this.matchResultMap = matchResultMap;
	}
	
	public Settings getSettings() {
		return settings;
	}
	
	public void setSettings(Settings settings) {
		this.settings = settings;
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CandidateSearchRule {
		
		private String resourceType;
		
		private List<String> searchParams;
		
		public String getResourceType() {
			return resourceType;
		}
		
		public void setResourceType(String resourceType) {
			this.resourceType = resourceType;
		}
		
		public List<String> getSearchParams() {
			return searchParams;
		}
		
		public void setSearchParams(List<String> searchParams) {
			this.searchParams = searchParams;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class MatchFieldRule {
		
		private String name;
		
		private String resourceType;
		
		private String resourcePath;
		
		private String fhirPath;
		
		private Boolean enabled;
		
		private Double weight;
		
		private AlgorithmRule matcher;
		
		private AlgorithmRule similarity;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getResourceType() {
			return resourceType;
		}
		
		public void setResourceType(String resourceType) {
			this.resourceType = resourceType;
		}
		
		public String getResourcePath() {
			return resourcePath;
		}
		
		public void setResourcePath(String resourcePath) {
			this.resourcePath = resourcePath;
		}
		
		public String getFhirPath() {
			return fhirPath;
		}
		
		public void setFhirPath(String fhirPath) {
			this.fhirPath = fhirPath;
		}
		
		public Boolean getEnabled() {
			return enabled;
		}
		
		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}
		
		public Double getWeight() {
			return weight;
		}
		
		public void setWeight(Double weight) {
			this.weight = weight;
		}
		
		public AlgorithmRule getMatcher() {
			return matcher;
		}
		
		public void setMatcher(AlgorithmRule matcher) {
			this.matcher = matcher;
		}
		
		public AlgorithmRule getSimilarity() {
			return similarity;
		}
		
		public void setSimilarity(AlgorithmRule similarity) {
			this.similarity = similarity;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class AlgorithmRule {
		
		private String algorithm;
		
		private Double matchThreshold;
		
		public String getAlgorithm() {
			return algorithm;
		}
		
		public void setAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}
		
		public Double getMatchThreshold() {
			return matchThreshold;
		}
		
		public void setMatchThreshold(Double matchThreshold) {
			this.matchThreshold = matchThreshold;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Settings {
		
		private Boolean enabled;
		
		private Integer maxCandidates;
		
		private Integer fieldMatchThreshold;
		
		private Boolean phoneticBoostEnabled;
		
		private String phoneticBoostAlgorithm;
		
		private Integer dobNearMatchDays;
		
		private String dobRepositoryFilterMode;
		
		private Map<String, Double> matchGradeThresholds;
		
		public Boolean getEnabled() {
			return enabled;
		}
		
		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}
		
		public Integer getMaxCandidates() {
			return maxCandidates;
		}
		
		public void setMaxCandidates(Integer maxCandidates) {
			this.maxCandidates = maxCandidates;
		}
		
		public Integer getFieldMatchThreshold() {
			return fieldMatchThreshold;
		}
		
		public void setFieldMatchThreshold(Integer fieldMatchThreshold) {
			this.fieldMatchThreshold = fieldMatchThreshold;
		}
		
		public Boolean getPhoneticBoostEnabled() {
			return phoneticBoostEnabled;
		}
		
		public void setPhoneticBoostEnabled(Boolean phoneticBoostEnabled) {
			this.phoneticBoostEnabled = phoneticBoostEnabled;
		}
		
		public String getPhoneticBoostAlgorithm() {
			return phoneticBoostAlgorithm;
		}
		
		public void setPhoneticBoostAlgorithm(String phoneticBoostAlgorithm) {
			this.phoneticBoostAlgorithm = phoneticBoostAlgorithm;
		}
		
		public Integer getDobNearMatchDays() {
			return dobNearMatchDays;
		}
		
		public void setDobNearMatchDays(Integer dobNearMatchDays) {
			this.dobNearMatchDays = dobNearMatchDays;
		}
		
		public String getDobRepositoryFilterMode() {
			return dobRepositoryFilterMode;
		}
		
		public void setDobRepositoryFilterMode(String dobRepositoryFilterMode) {
			this.dobRepositoryFilterMode = dobRepositoryFilterMode;
		}
		
		public Map<String, Double> getMatchGradeThresholds() {
			return matchGradeThresholds;
		}
		
		public void setMatchGradeThresholds(Map<String, Double> matchGradeThresholds) {
			this.matchGradeThresholds = matchGradeThresholds;
		}
	}
}
