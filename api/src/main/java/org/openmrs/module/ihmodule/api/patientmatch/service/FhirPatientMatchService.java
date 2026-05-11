package org.openmrs.module.ihmodule.api.patientmatch.service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfigService;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchResult;
import org.openmrs.module.ihmodule.api.patientmatch.engine.PatientFuzzyMatchingEngine;
import org.openmrs.module.ihmodule.api.patientmatch.repository.PatientCandidateSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.FhirContext;

@Service
public class FhirPatientMatchService {
	
	private static final String EXT_MATCH_GRADE = "http://hl7.org/fhir/StructureDefinition/match-grade";
	
	private static final String EXT_MATCH_CONFIDENCE = "http://intelehealth.org/fhir/StructureDefinition/match-confidence";
	
	private static final String EXT_FIELD_SCORES = "http://intelehealth.org/fhir/StructureDefinition/match-field-scores";
	
	private static final String EXT_MATCHED_FIELDS = "http://intelehealth.org/fhir/StructureDefinition/matched-fields";
	
	@Autowired
	private FuzzyPatientMatchConfigService configService;
	
	@Autowired
	private FhirPatientMatchRequestParser requestParser;
	
	@Autowired
	private PatientCandidateSource candidateSource;
	
	@Autowired
	private PatientFuzzyMatchingEngine matchingEngine;
	
	@Autowired
	private FhirConfig fhirConfig;
	
	@Transactional(readOnly = true)
	public Bundle match(String body) {
		FuzzyPatientMatchConfig config = configService.getConfig();
		if (!config.isEnabled()) {
			throw new IllegalStateException("FHIR fuzzy patient match is disabled");
		}
		FuzzyPatientMatchRequest request = requestParser.parse(body);
		List<FuzzyPatientCandidate> candidates = candidateSource.findCandidates(request, config);
		List<FuzzyPatientMatchResult> scored = new ArrayList<FuzzyPatientMatchResult>();
		for (FuzzyPatientCandidate candidate : candidates) {
			FuzzyPatientMatchResult result = matchingEngine.score(request, candidate, config);
			if (result.getOverallMatchScore() < config.getThreshold()) {
				continue;
			}
			if (request.isOnlyCertainMatches() && !"HIGH".equals(result.getConfidence())) {
				continue;
			}
			scored.add(result);
		}
		Collections.sort(scored, Comparator.comparingDouble(FuzzyPatientMatchResult::getOverallMatchScore).reversed()
		        .thenComparing(result -> StringUtils.defaultString(result.getCandidate().getUuid())));
		
		int total = scored.size();
		int fromIndex = Math.min(request.getOffset(), total);
		int toIndex = Math.min(fromIndex + request.getCount(), total);
		List<FuzzyPatientMatchResult> page = scored.subList(fromIndex, toIndex);
		return toBundle(page, total);
	}
	
	public OperationOutcome errorOutcome(String code, String diagnostics) {
		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR).setCode(OperationOutcome.IssueType.EXCEPTION)
		        .setDiagnostics(code + ": " + diagnostics);
		return outcome;
	}
	
	public String encodeResource(FhirContext fhirContext, org.hl7.fhir.r4.model.Resource resource) {
		return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(resource);
	}
	
	private Bundle toBundle(List<FuzzyPatientMatchResult> matches, int total) {
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.setTotal(total);
		for (FuzzyPatientMatchResult match : matches) {
			BundleEntryComponent entry = bundle.addEntry();
			entry.setResource(toPatient(match.getCandidate()));
			entry.getSearch().setMode(Bundle.SearchEntryMode.MATCH);
			entry.getSearch().setScore((float) (match.getOverallMatchScore() / 100.0d));
			entry.getSearch()
			        .addExtension(new Extension(EXT_MATCH_GRADE, new CodeType(toMatchGrade(match.getConfidence()))));
			entry.getSearch().addExtension(new Extension(EXT_MATCH_CONFIDENCE, new StringType(match.getConfidence())));
			entry.getSearch().addExtension(toFieldScoresExtension(match.getFieldScores()));
			entry.getSearch().addExtension(toMatchedFieldsExtension(match.getMatchedFields()));
		}
		return bundle;
	}
	
	private Patient toPatient(FuzzyPatientCandidate candidate) {
		Patient patient = new Patient();
		patient.setId(candidate.getUuid());
		if (StringUtils.isNotBlank(candidate.getIdentifier())) {
			Identifier identifier = patient.addIdentifier();
			identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
			identifier.setValue(candidate.getIdentifier());
		}
		if (StringUtils.isNotBlank(candidate.getGivenName()) || StringUtils.isNotBlank(candidate.getFamilyName())
		        || StringUtils.isNotBlank(candidate.getName())) {
			HumanName name = patient.addName();
			if (StringUtils.isNotBlank(candidate.getGivenName())) {
				name.addGiven(candidate.getGivenName());
			}
			if (StringUtils.isNotBlank(candidate.getFamilyName())) {
				name.setFamily(candidate.getFamilyName());
			}
			if (StringUtils.isNotBlank(candidate.getName())) {
				name.setText(candidate.getName());
			}
		}
		if (candidate.getBirthDate() != null) {
			patient.setBirthDate(java.util.Date.from(candidate.getBirthDate().atStartOfDay(ZoneId.systemDefault())
			        .toInstant()));
			patient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType(candidate.getBirthDate().toString()));
		}
		if (StringUtils.isNotBlank(candidate.getGender())) {
			patient.setGender(toAdministrativeGender(candidate.getGender()));
		}
		if (StringUtils.isNotBlank(candidate.getPhone())) {
			ContactPoint telecom = patient.addTelecom();
			telecom.setSystem(ContactPoint.ContactPointSystem.PHONE);
			telecom.setValue(candidate.getPhone());
		}
		if (StringUtils.isNotBlank(candidate.getAddress())) {
			patient.addAddress().setText(candidate.getAddress());
		}
		return patient;
	}
	
	private AdministrativeGender toAdministrativeGender(String value) {
		String normalized = StringUtils.trimToEmpty(value).toLowerCase();
		if ("m".equals(normalized) || "male".equals(normalized)) {
			return AdministrativeGender.MALE;
		}
		if ("f".equals(normalized) || "female".equals(normalized)) {
			return AdministrativeGender.FEMALE;
		}
		if ("other".equals(normalized) || "o".equals(normalized)) {
			return AdministrativeGender.OTHER;
		}
		return AdministrativeGender.UNKNOWN;
	}
	
	private Extension toFieldScoresExtension(Map<String, Double> fieldScores) {
		Extension extension = new Extension();
		extension.setUrl(EXT_FIELD_SCORES);
		Map<String, Double> ordered = new LinkedHashMap<String, Double>(fieldScores);
		for (Map.Entry<String, Double> entry : ordered.entrySet()) {
			Extension nested = new Extension();
			nested.setUrl(entry.getKey());
			nested.setValue(new DecimalType(BigDecimal.valueOf(entry.getValue().doubleValue())));
			extension.addExtension(nested);
		}
		return extension;
	}
	
	private Extension toMatchedFieldsExtension(List<String> matchedFields) {
		Extension extension = new Extension();
		extension.setUrl(EXT_MATCHED_FIELDS);
		for (String field : matchedFields.stream().distinct().collect(Collectors.toList())) {
			Extension nested = new Extension();
			nested.setUrl("field");
			nested.setValue(new StringType(field));
			extension.addExtension(nested);
		}
		return extension;
	}
	
	private String toMatchGrade(String confidence) {
		if ("HIGH".equals(confidence)) {
			return "certain";
		}
		if ("MEDIUM".equals(confidence)) {
			return "probable";
		}
		return "possible";
	}
}
