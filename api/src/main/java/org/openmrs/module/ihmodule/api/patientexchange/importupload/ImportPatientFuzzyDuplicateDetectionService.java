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
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewCandidateMatchRow;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewService;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiImportDuplicateReviewSource;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCase;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientmatch.service.FhirPatientMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

/**
 * Runs OpenMRS fuzzy {@code $match} (in-process) and central FHIR {@code $mdm-match} during patient
 * import; persists duplicate-review rows when either API returns candidate Patients.
 * <p>
 * Request shape for both paths: FHIR {@code Parameters} with {@code resource} (Patient),
 * {@code resourceType}, {@code count}, {@code offset}, {@code onlyCertainMatches} — see
 * {@code docs/fhir-mdm-and-openmrs-patient-match-samples.md}.
 */
@Service
public class ImportPatientFuzzyDuplicateDetectionService {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPatientFuzzyDuplicateDetectionService.class);
	
	private static final String FUZZY_IMPORT_DEBUG = "FUZZY_IMPORT_MATCH_DEBUG";
	
	private static final String MDM_MATCH_PATH = "/fhir/$mdm-match";
	
	private static final int DEFAULT_MATCH_COUNT = 10;
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private FhirPatientMatchService fhirPatientMatchService;
	
	@Autowired
	private FhirConfig fhirConfig;
	
	@Autowired
	private MpiDuplicateReviewService mpiDuplicateReviewService;
	
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
		Bundle mdmBundle = invokeFhirMdmMatch(patientForMatch);
		int om = countPatientMatches(openmrsBundle);
		int md = countPatientMatches(mdmBundle);
		log.error("{} evaluate after calls openmrsPatientEntries={} mdmPatientEntries={} correlation={}",
		    FUZZY_IMPORT_DEBUG, om, md, importCorrelationLocalKey);
		boolean openmrsHit = om >= 1;
		boolean mdmHit = md >= 1;
		if (!openmrsHit && !mdmHit) {
			log.error("{} evaluate no duplicate correlation={} (both APIs empty)", FUZZY_IMPORT_DEBUG,
			    importCorrelationLocalKey);
			return Optional.empty();
		}
		List<MpiDuplicateReviewCandidateMatchRow> merged = mergeMatchCandidateRows(openmrsBundle, mdmBundle);
		if (merged.isEmpty()) {
			log.error(
			    "{} evaluate inconsistent state: APIs reported hits (openmrs={} mdm={}) but merge empty correlation={}",
			    FUZZY_IMPORT_DEBUG, om, md, importCorrelationLocalKey);
			return Optional.empty();
		}
		log.error("{} evaluate persisting duplicateReview mergedCandidates={} correlation={}", FUZZY_IMPORT_DEBUG,
		    merged.size(), importCorrelationLocalKey);
		String auditJson = buildCombinedSearchJson(openmrsBundle, mdmBundle);
		return mpiDuplicateReviewService.persistImportFuzzyDuplicateIfMatches(importCorrelationLocalKey, patientForMatch,
		    outboundImportPatientJson, merged, auditJson);
	}
	
	/**
	 * Same {@code Parameters} shape as OpenMRS {@code POST .../patient/$match} and central
	 * {@code POST authority}/fhir/$mdm-match}.
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
	
	private Bundle invokeFhirMdmMatch(Patient patient) {
		String authorityBase = fhirConfig.resolveMdmMatchAuthorityBaseUrl();
		if (StringUtils.isBlank(authorityBase)) {
			log.error("{} mdm-match skipped: resolveMdmMatchAuthorityBaseUrl() blank (check opencr.openhim.url)",
			    FUZZY_IMPORT_DEBUG);
			return null;
		}
		String json = fhirContext.newJsonParser().encodeResourceToString(buildPatientMatchParameters(patient));
		String url = authorityBase + MDM_MATCH_PATH;
		log.error("{} mdm-match POST url={} requestJsonLength={}", FUZZY_IMPORT_DEBUG, url, json.length());
		String[] creds = fhirConfig.getOpenCRCredentials();
		try {
			FhirResponse res = HttpWebClient.postWithBasicAuthFhirJson(authorityBase, MDM_MATCH_PATH, creds[0], creds[1],
			    json);
			if (!isSuccessfulHttp(res.getStatusCode())) {
				log.error("{} mdm-match HTTP failure status={} message={} bodySnippet={}", FUZZY_IMPORT_DEBUG,
				    res.getStatusCode(), res.getMessage(), truncate(res.getResponse(), 1200));
				return null;
			}
			log.error("{} mdm-match HTTP ok status={} responseLength={}", FUZZY_IMPORT_DEBUG, res.getStatusCode(),
			    res.getResponse() != null ? res.getResponse().length() : 0);
			Bundle parsed = parseMdmResponseToSearchBundle(res.getResponse());
			log.error("{} mdm-match parsed bundle entryCount={}", FUZZY_IMPORT_DEBUG,
			    parsed != null && parsed.hasEntry() ? parsed.getEntry().size() : 0);
			return parsed;
		}
		catch (RuntimeException ex) {
			log.error("Import fuzzy duplicate: MDM $mdm-match call failed: {}", ex.getMessage(), ex);
			return null;
		}
	}
	
	private static Bundle emptySearchBundle() {
		Bundle b = new Bundle();
		b.setType(Bundle.BundleType.SEARCHSET);
		b.setTotal(0);
		return b;
	}
	
	private Bundle parseMdmResponseToSearchBundle(String responseBody) {
		if (StringUtils.isBlank(responseBody)) {
			return null;
		}
		try {
			Resource parsed = (Resource) fhirContext.newJsonParser().parseResource(responseBody);
			if (parsed instanceof Bundle) {
				return (Bundle) parsed;
			}
			if (parsed instanceof Patient) {
				Bundle wrap = new Bundle();
				wrap.setType(Bundle.BundleType.SEARCHSET);
				wrap.setTotal(1);
				BundleEntryComponent ent = wrap.addEntry();
				ent.setResource((Patient) parsed);
				ent.getSearch().setMode(Bundle.SearchEntryMode.MATCH);
				return wrap;
			}
			if (parsed instanceof Parameters) {
				return parametersToSearchBundle((Parameters) parsed);
			}
			log.warn("Import fuzzy duplicate: unexpected MDM response type {}", parsed.getClass().getSimpleName());
			log.error("{} parseMdmResponse unexpected resourceType={}", FUZZY_IMPORT_DEBUG, parsed.getClass().getName());
			return null;
		}
		catch (RuntimeException ex) {
			log.error("{} parseMdmResponse failed: {}", FUZZY_IMPORT_DEBUG, ex.getMessage(), ex);
			return null;
		}
	}
	
	private static Bundle parametersToSearchBundle(Parameters parameters) {
		Bundle out = new Bundle();
		out.setType(Bundle.BundleType.SEARCHSET);
		for (Parameters.ParametersParameterComponent p : parameters.getParameter()) {
			if (p == null) {
				continue;
			}
			if (p.hasResource() && p.getResource() instanceof Patient) {
				BundleEntryComponent ent = out.addEntry();
				ent.setResource(p.getResource());
				ent.getSearch().setMode(Bundle.SearchEntryMode.MATCH);
			}
			if ("match".equals(p.getName()) && p.hasPart()) {
				for (Parameters.ParametersParameterComponent part : p.getPart()) {
					if (part != null && part.hasResource() && part.getResource() instanceof Patient) {
						BundleEntryComponent ent = out.addEntry();
						ent.setResource(part.getResource());
						ent.getSearch().setMode(Bundle.SearchEntryMode.MATCH);
					}
				}
			}
		}
		if (!out.hasEntry()) {
			return null;
		}
		out.setTotal(out.getEntry().size());
		return out;
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
	
	private static List<MpiDuplicateReviewCandidateMatchRow> mergeMatchCandidateRows(Bundle openmrsBundle,
	        Bundle mdmBundle) {
		Map<String, MpiDuplicateReviewCandidateMatchRow> ordered = new LinkedHashMap<>();
		addBundleEntries(openmrsBundle, MpiImportDuplicateReviewSource.OPENMRS.getValue(), ordered);
		addBundleEntries(mdmBundle, MpiImportDuplicateReviewSource.FHIR.getValue(), ordered);
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
			MpiDuplicateReviewCandidateMatchRow incoming = new MpiDuplicateReviewCandidateMatchRow(p, matchSource, score);
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
	
	private String buildCombinedSearchJson(Bundle openmrsBundle, Bundle mdmBundle) {
		String om = openmrsBundle != null ? fhirContext.newJsonParser().encodeResourceToString(openmrsBundle) : "null";
		String mdm = mdmBundle != null ? fhirContext.newJsonParser().encodeResourceToString(mdmBundle) : "null";
		return "{\"openmrsFuzzyMatchBundle\":" + om + ",\"fhirMdmMatchBundle\":" + mdm + "}";
	}
	
	private static boolean isSuccessfulHttp(String statusCode) {
		if (StringUtils.isBlank(statusCode)) {
			return false;
		}
		try {
			int c = Integer.parseInt(statusCode.trim());
			return c >= 200 && c < 300;
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}
	
	private static String truncate(String s, int max) {
		if (s == null) {
			return null;
		}
		return s.length() <= max ? s : s.substring(0, max);
	}
}
