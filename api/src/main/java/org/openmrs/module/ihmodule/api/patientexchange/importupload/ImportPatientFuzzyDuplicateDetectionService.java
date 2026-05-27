package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.BundleSearchMatchGradeExtractor;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewCandidateMatchRow;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewServicePort;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiImportDuplicateReviewSource;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCase;
import org.openmrs.module.ihmodule.api.patientmatch.service.FhirPatientMatchServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

/**
 * Runs OpenMRS fuzzy {@code $match} (in-process) during patient import; persists duplicate-review
 * rows when the match returns candidate Patients.
 * <p>
 * Request shape: FHIR {@code Parameters} with {@code resource} (Patient), {@code resourceType},
 * {@code count}, {@code offset}, {@code onlyCertainMatches} — see
 * {@code docs/fhir-mdm-and-openmrs-patient-match-samples.md}.
 */
@Service
public class ImportPatientFuzzyDuplicateDetectionService {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPatientFuzzyDuplicateDetectionService.class);
	
	private static final String FUZZY_IMPORT_DEBUG = "FUZZY_IMPORT_MATCH_DEBUG";
	
	private static final int DEFAULT_MATCH_COUNT = 10;
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private FhirPatientMatchServicePort fhirPatientMatchService;
	
	@Autowired
	private MpiDuplicateReviewServicePort mpiDuplicateReviewService;
	
	/**
	 * @param importCorrelationLocalKey stable key before patient exists (e.g.
	 *            {@code import-upload:}<em>openMrsId</em>)
	 * @param outboundImportPatientJson FHIR Patient JSON used for audit / replay
	 * @return saved or existing pending duplicate-review case when a fuzzy match exists
	 */
	public Optional<MpiPatientDuplicateReviewCase> evaluateAndPersistIfDuplicate(Patient patientForMatch,
	        String outboundImportPatientJson, String importCorrelationLocalKey) {
		log.error("{} evaluate start correlation={} outboundJsonLength={}", FUZZY_IMPORT_DEBUG, importCorrelationLocalKey,
		    outboundImportPatientJson != null ? outboundImportPatientJson.length() : -1);
		Bundle openmrsBundle = invokeOpenMrsFuzzyMatch(patientForMatch);
		int matchCount = countPatientMatches(openmrsBundle);
		log.error("{} evaluate after openmrs fuzzy match patientEntries={} correlation={}", FUZZY_IMPORT_DEBUG, matchCount,
		    importCorrelationLocalKey);
		if (matchCount < 1) {
			log.error("{} evaluate no duplicate correlation={}", FUZZY_IMPORT_DEBUG, importCorrelationLocalKey);
			return Optional.empty();
		}
		List<MpiDuplicateReviewCandidateMatchRow> candidates = buildMatchCandidateRows(openmrsBundle);
		if (candidates.isEmpty()) {
			log.error("{} evaluate inconsistent state: bundle had patients but merge empty correlation={}",
			    FUZZY_IMPORT_DEBUG, importCorrelationLocalKey);
			return Optional.empty();
		}
		log.error("{} evaluate persisting duplicateReview candidates={} correlation={}", FUZZY_IMPORT_DEBUG,
		    candidates.size(), importCorrelationLocalKey);
		String auditJson = buildSearchAuditJson(openmrsBundle);
		return mpiDuplicateReviewService.persistImportFuzzyDuplicateIfMatches(importCorrelationLocalKey, patientForMatch,
		    outboundImportPatientJson, candidates, auditJson);
	}
	
	/**
	 * Same {@code Parameters} shape as OpenMRS {@code POST .../patient/$match}.
	 */
	static Parameters buildPatientMatchParameters(Patient patientForResource) {
		Patient copy = patientForResource.copy();
		copy.setId((String) null);
		Parameters parameters = new Parameters();
		parameters.addParameter().setName("resource").setResource(copy);
		parameters.addParameter().setName("resourceType").setValue(new StringType("Patient"));
		parameters.addParameter().setName("count").setValue(new IntegerType(DEFAULT_MATCH_COUNT));
		parameters.addParameter().setName("offset").setValue(new IntegerType(0));
		parameters.addParameter().setName("onlyCertainMatches").setValue(new BooleanType(false));
		return parameters;
	}
	
	private Bundle invokeOpenMrsFuzzyMatch(Patient patient) {
		try {
			String body = fhirContext.newJsonParser().encodeResourceToString(buildPatientMatchParameters(patient));
			log.error("{} openmrs fuzzy invoking in-process match requestJsonLength={}", FUZZY_IMPORT_DEBUG, body.length());
			Bundle result = fhirPatientMatchService.match(body);
			log.error("{} openmrs fuzzy returned bundleTotal={} entryCount={}", FUZZY_IMPORT_DEBUG,
			    result != null && result.hasTotal() ? result.getTotalElement().getValue() : null,
			    result != null && result.hasEntry() ? result.getEntry().size() : 0);
			return result;
		}
		catch (IllegalStateException ex) {
			String msg = ex.getMessage() != null ? ex.getMessage() : "";
			if (msg.toLowerCase().contains("disabled")) {
				log.error("{} openmrs fuzzy disabled in config: {}", FUZZY_IMPORT_DEBUG, msg);
				return emptySearchBundle();
			}
			log.error("Import fuzzy duplicate: OpenMRS fuzzy match unavailable: {}", msg, ex);
			return emptySearchBundle();
		}
		catch (IllegalArgumentException ex) {
			log.error("{} openmrs fuzzy bad request (no match): {}", FUZZY_IMPORT_DEBUG, ex.getMessage(), ex);
			return emptySearchBundle();
		}
		catch (RuntimeException ex) {
			log.error("Import fuzzy duplicate: OpenMRS fuzzy match failed: {}", ex.getMessage(), ex);
			return emptySearchBundle();
		}
	}
	
	private static Bundle emptySearchBundle() {
		Bundle b = new Bundle();
		b.setType(Bundle.BundleType.SEARCHSET);
		b.setTotal(0);
		return b;
	}
	
	private static int countPatientMatches(Bundle bundle) {
		if (bundle == null || !bundle.hasEntry()) {
			return 0;
		}
		int n = 0;
		for (BundleEntryComponent e : bundle.getEntry()) {
			if (e != null && e.hasResource() && e.getResource() instanceof Patient) {
				n++;
			}
		}
		return n;
	}
	
	private static List<MpiDuplicateReviewCandidateMatchRow> buildMatchCandidateRows(Bundle openmrsBundle) {
		Map<String, MpiDuplicateReviewCandidateMatchRow> ordered = new LinkedHashMap<>();
		addBundleEntries(openmrsBundle, MpiImportDuplicateReviewSource.OPENMRS.getValue(), ordered);
		return new ArrayList<>(ordered.values());
	}
	
	private static void addBundleEntries(Bundle bundle, String matchSource,
	        Map<String, MpiDuplicateReviewCandidateMatchRow> sink) {
		if (bundle == null || !bundle.hasEntry()) {
			return;
		}
		for (BundleEntryComponent e : bundle.getEntry()) {
			if (e == null || !e.hasResource() || !(e.getResource() instanceof Patient)) {
				continue;
			}
			Patient p = (Patient) e.getResource();
			String key = dedupeKey(p);
			Double score = bundleEntryMatchScore(e);
			String matchType = BundleSearchMatchGradeExtractor.extractMatchGradeCode(e);
			MpiDuplicateReviewCandidateMatchRow incoming = new MpiDuplicateReviewCandidateMatchRow(p, matchSource, score,
			        matchType);
			MpiDuplicateReviewCandidateMatchRow existing = sink.get(key);
			if (existing == null || scoreComparesHigher(score, existing.getMatchScore())) {
				sink.put(key, incoming);
			}
		}
	}
	
	private static boolean scoreComparesHigher(Double candidate, Double incumbent) {
		double c = candidate != null ? candidate : -1.0d;
		double i = incumbent != null ? incumbent : -1.0d;
		return c > i;
	}
	
	private static Double bundleEntryMatchScore(BundleEntryComponent entry) {
		if (entry == null || !entry.hasSearch() || !entry.getSearch().hasScore()) {
			return null;
		}
		return entry.getSearch().getScore().doubleValue();
	}
	
	private static String dedupeKey(Patient p) {
		if (p.getIdElement() != null && StringUtils.isNotBlank(p.getIdElement().getIdPart())) {
			return "id:" + p.getIdElement().getIdPart().trim();
		}
		if (p.hasIdentifier()) {
			for (org.hl7.fhir.r4.model.Identifier id : p.getIdentifier()) {
				if (id != null && StringUtils.isNotBlank(id.getValue())) {
					String sys = StringUtils.trimToEmpty(id.getSystem());
					return "ident:" + sys + "|" + id.getValue().trim();
				}
			}
		}
		return "anon:" + System.identityHashCode(p);
	}
	
	private String buildSearchAuditJson(Bundle openmrsBundle) {
		String om = openmrsBundle != null ? fhirContext.newJsonParser().encodeResourceToString(openmrsBundle) : "null";
		return "{\"openmrsFuzzyMatchBundle\":" + om + "}";
	}
}
