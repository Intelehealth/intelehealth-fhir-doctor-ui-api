package org.openmrs.module.ihmodule.api.patientmatch.engine;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticAlgorithm;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticEncodingService;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyPhoneUtils;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyTextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientFuzzyMatchingEngine {
	
	private static final Logger log = LoggerFactory.getLogger(PatientFuzzyMatchingEngine.class);
	
	@Autowired
	private WeightedScoreAggregator weightedScoreAggregator;
	
	@Autowired
	private PhoneticEncodingService phoneticEncodingService;
	
	public FuzzyPatientMatchResult score(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		Map<String, Double> fieldScores = new LinkedHashMap<String, Double>();
		fieldScores.put("identifier", identifierScore(request, candidate, config));
		fieldScores.put("name", nameScore(request, candidate, config));
		fieldScores.put("dob", dobScore(request, candidate, config));
		fieldScores.put("phone", phoneScore(request, candidate, config));
		fieldScores.put("address", addressScore(request, candidate, config));
		fieldScores.put("gender", genderScore(request, candidate, config));
		return weightedScoreAggregator.aggregate(candidate, request, config, fieldScores);
	}
	
	private double identifierScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		if (!config.isFieldEnabled("identifier") || !request.hasIdentifier()
		        || StringUtils.isBlank(candidate.getIdentifier())) {
			return 0.0d;
		}
		String queryIdentifier = FuzzyTextUtils.normalize(request.getIdentifier()); //1234ew
		String candidateIdentifier = FuzzyTextUtils.normalize(candidate.getIdentifier()); // 1436w
		if (StringUtils.equals(queryIdentifier, candidateIdentifier)) {
			return 100.0d;
		}
		if (StringUtils.startsWith(candidateIdentifier, queryIdentifier)
		        || StringUtils.startsWith(queryIdentifier, candidateIdentifier)) {
			return 85.0d;
		}
		return FuzzyTextUtils.jaroWinklerPercent(queryIdentifier, candidateIdentifier);
	}
	
	private double nameScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		if (!config.isFieldEnabled("name") || !request.hasName() || StringUtils.isBlank(candidate.getName())) {
			return 0.0d;
		}
		double fullNameFuzzyScore = fullNameFuzzyScore(request.getName(), candidate.getName());
		double structuredNameScore = structuredNameScore(request, candidate, config);
		double combined;
		if (request.hasGivenName() && request.hasFamilyName()) {
			combined = structuredNameScore;
			if (namePartsAlignForFuzzyMerge(request, candidate, config)) {
				combined = Math.max(combined, fullNameFuzzyScore);
			}
		} else {
			combined = fullNameFuzzyScore;
			if (structuredNameScore > 0.0d) {
				combined = Math.max(combined, structuredNameScore);
			}
		}
		if (config.isPhoneticBoostEnabled() && phoneticBoostEligible(request, candidate, config)) {
			combined = Math.min(100.0d, combined + 10.0d);
		}
		return FuzzyTextUtils.round(combined);
	}
	
	private double fullNameFuzzyScore(String requestName, String candidateName) {
		double baseScore = FuzzyTextUtils.jaroWinklerPercent(requestName, candidateName);
		double tokenScore = FuzzyTextUtils.tokenJaccardPercent(requestName, candidateName);
		return Math.max(baseScore, ((baseScore * 0.7d) + (tokenScore * 0.3d)));
	}
	
	private double structuredNameScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		double givenScore = 0.0d;
		double familyScore = 0.0d;
		int parts = 0;
		if (request.hasGivenName()) {
			givenScore = StringUtils.isNotBlank(candidate.getGivenName()) ? FuzzyTextUtils.jaroWinklerPercent(
			    request.getGivenName(), candidate.getGivenName()) : 0.0d;
			parts++;
		}
		if (request.hasFamilyName()) {
			familyScore = StringUtils.isNotBlank(candidate.getFamilyName()) ? FuzzyTextUtils.jaroWinklerPercent(
			    request.getFamilyName(), candidate.getFamilyName()) : 0.0d;
			parts++;
		}
		if (parts == 0) {
			return 0.0d;
		}
		if (parts == 1) {
			return request.hasGivenName() ? givenScore : familyScore;
		}
		double givenWeight = config != null ? config.getGivenNamePartWeight() : 0.45d;
		double familyWeight = config != null ? config.getFamilyNamePartWeight() : 0.45d;
		return FuzzyTextUtils.round((givenScore * givenWeight) + (familyScore * familyWeight));
	}
	
	private boolean namePartsAlignForFuzzyMerge(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		PhoneticAlgorithm algorithm = resolvePhoneticBoostAlgorithm(config);
		if (request.hasGivenName() && !namePartAligns(request.getGivenName(), candidate.getGivenName(), config, algorithm)) {
			return false;
		}
		return !request.hasFamilyName()
		        || namePartAligns(request.getFamilyName(), candidate.getFamilyName(), config, algorithm);
	}
	
	private boolean phoneticBoostEligible(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		PhoneticAlgorithm algorithm = resolvePhoneticBoostAlgorithm(config);
		log.debug("Phonetic name-score boost using algorithm={} from config", algorithm);
		if (request.hasGivenName() && request.hasFamilyName()) {
			return namePartAligns(request.getGivenName(), candidate.getGivenName(), config, algorithm)
			        && namePartAligns(request.getFamilyName(), candidate.getFamilyName(), config, algorithm);
		}
		if (phoneticEncodingService.phoneticMatch(request.getName(), candidate.getName(), algorithm)) {
			return true;
		}
		if (request.hasGivenName() && namePartAligns(request.getGivenName(), candidate.getGivenName(), config, algorithm)) {
			return true;
		}
		return request.hasFamilyName()
		        && namePartAligns(request.getFamilyName(), candidate.getFamilyName(), config, algorithm);
	}
	
	private boolean namePartAligns(String requestPart, String candidatePart, FuzzyPatientMatchConfig config,
	        PhoneticAlgorithm algorithm) {
		if (StringUtils.isBlank(requestPart)) {
			return true;
		}
		if (StringUtils.isBlank(candidatePart)) {
			return false;
		}
		double threshold = config != null ? config.getFieldMatchThreshold() : 60.0d;
		if (FuzzyTextUtils.jaroWinklerPercent(requestPart, candidatePart) >= threshold) {
			return true;
		}
		return phoneticEncodingService.phoneticMatch(requestPart, candidatePart, algorithm);
	}
	
	private PhoneticAlgorithm resolvePhoneticBoostAlgorithm(FuzzyPatientMatchConfig config) {
		String raw = config != null ? config.getPhoneticBoostAlgorithm() : null;
		if (StringUtils.isBlank(raw)) {
			return PhoneticAlgorithm.DOUBLE_METAPHONE;
		}
		return PhoneticAlgorithm.fromConfig(raw);
	}
	
	private double dobScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate, FuzzyPatientMatchConfig config) {
		if (!config.isFieldEnabled("dob") || !request.hasBirthDate() || candidate.getBirthDate() == null) {
			return 0.0d;
		}
		if (request.getBirthDate().equals(candidate.getBirthDate())) {
			return 100.0d;
		}
		if (config.getDobNearMatchDays() > 0) {
			long days = Math.abs(ChronoUnit.DAYS.between(request.getBirthDate(), candidate.getBirthDate()));
			if (days <= config.getDobNearMatchDays()) {
				return 85.0d;
			}
		}
		return 0.0d;
	}
	
	private double phoneScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		if (!config.isFieldEnabled("phone") || !request.hasPhone() || StringUtils.isBlank(candidate.getPhone())) {
			return 0.0d;
		}
		if (isExactPhoneAlgorithm(config.getPhoneAlgorithm())) {
			return FuzzyPhoneUtils.exactMatchPercent(request.getPhone(), candidate.getPhone());
		}
		return FuzzyPhoneUtils.similarityPercent(request.getPhone(), candidate.getPhone());
	}
	
	private static boolean isExactPhoneAlgorithm(String algorithm) {
		if (StringUtils.isBlank(algorithm)) {
			return true;
		}
		String normalized = algorithm.trim().toLowerCase().replace('-', '_');
		return "string".equals(normalized) || "exact".equals(normalized);
	}
	
	private double addressScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		if (!config.isFieldEnabled("address") || !request.hasAddress() || StringUtils.isBlank(candidate.getAddress())) {
			return 0.0d;
		}
		double tokenScore = FuzzyTextUtils.tokenJaccardPercent(request.getAddress(), candidate.getAddress());
		double jwScore = FuzzyTextUtils.jaroWinklerPercent(request.getAddress(), candidate.getAddress());
		if ("token_jaccard".equalsIgnoreCase(config.getAddressAlgorithm())) {
			return FuzzyTextUtils.round(Math.max(tokenScore, (tokenScore * 0.7d) + (jwScore * 0.3d)));
		}
		return FuzzyTextUtils.round(Math.max(tokenScore, jwScore));
	}
	
	private double genderScore(FuzzyPatientMatchRequest request, FuzzyPatientCandidate candidate,
	        FuzzyPatientMatchConfig config) {
		if (!config.isFieldEnabled("gender") || !request.hasGender() || StringUtils.isBlank(candidate.getGender())) {
			return 0.0d;
		}
		String queryGender = normalizeGender(request.getGender());
		String candidateGender = normalizeGender(candidate.getGender());
		return StringUtils.isNotBlank(queryGender) && StringUtils.equals(queryGender, candidateGender) ? 100.0d : 0.0d;
	}
	
	private String normalizeGender(String value) {
		String normalized = StringUtils.trimToEmpty(value).toLowerCase();
		if ("m".equals(normalized) || "male".equals(normalized)) {
			return "male";
		}
		if ("f".equals(normalized) || "female".equals(normalized)) {
			return "female";
		}
		if ("o".equals(normalized) || "other".equals(normalized)) {
			return "other";
		}
		if ("u".equals(normalized) || "unknown".equals(normalized)) {
			return "unknown";
		}
		return normalized;
	}
}
