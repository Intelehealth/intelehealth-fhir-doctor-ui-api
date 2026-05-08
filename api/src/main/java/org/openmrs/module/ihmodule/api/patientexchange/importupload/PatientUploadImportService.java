package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.param.ReuestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;

@Service
public class PatientUploadImportService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientUploadImportService.class);
	
	private static final String OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL = "http://fhir.openmrs.org/ext/patient/identifier#location";
	
	private static final String OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL = "http://fhir.openmrs.org/ext/patient/identifier#preferred";
	
	private static final String OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	private static final String OPENMRS_ID_SYSTEM = "http://intelehealth-central-fhir.mpower-social.com/fhir/StructureDefinition/OpenMRS-ID";
	
	private static final String V2_IDENTIFIER_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
	
	private static final String MR_IDENTIFIER_CODE = "MR";
	
	private final FhirContext fhirContext = FhirContext.forR4();
	
	@Value("${intelehealth.fhir.patient.import.preferred.identifier.type.uuid}")
	private String preferredIdentifierTypeUuid;
	
	@Value("${intelehealth.fhir.patient.import.preferred.identifier.type.name:OpenEMPI ID}")
	private String preferredIdentifierTypeName;
	
	private FhirConfig fhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	public PatientUploadImportResponse importPatientFile(MultipartFile file, String locationUuid) throws Exception {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}
		
		String content = new String(file.getBytes(), StandardCharsets.UTF_8);
		List<Patient> patients = parsePatients(content);
		String effectiveLocationUuid = resolveIdentifierLocationUuid(locationUuid);
		
		PatientUploadImportResponse response = new PatientUploadImportResponse();
		response.setTotal(patients.size());
		
		for (Patient patient : patients) {
			PatientUploadImportItemResult item = new PatientUploadImportItemResult();
			item.setInputId(patient.getIdElement() != null ? patient.getIdElement().getIdPart() : null);
			try {
				ensurePreferredOpenEmiIdentifier(patient);
				ensureIdentifierLocation(patient, effectiveLocationUuid);
				if (existsByIdentifier(patient)) {
					item.setStatus("SKIPPED");
					item.setMessage("Patient already exists by identifier");
					response.setSkipped(response.getSkipped() + 1);
				} else if (existsByDemographics(patient)) {
					item.setStatus("SKIPPED");
					item.setMessage("Patient already exists (same family, given, gender, birth date)");
					response.setSkipped(response.getSkipped() + 1);
				} else {
					if (!hasPreferredIdentifier(patient)) {
						throw new IllegalArgumentException("No preferred identifier available for create");
					}
					patient.setId((String) null);
					MethodOutcome outcome = fhirConfig.getLocalOpenMRSFhirContext().create().resource(patient).execute();
					item.setStatus("CREATED");
					item.setCreatedId(outcome.getId() != null ? outcome.getId().getIdPart() : null);
					item.setMessage("Patient created");
					response.setCreated(response.getCreated() + 1);
				}
			}
			catch (Exception e) {
				item.setStatus("FAILED");
				item.setMessage(e.getMessage());
				response.setFailed(response.getFailed() + 1);
			}
			response.getItems().add(item);
		}
		return response;
	}
	
	private List<Patient> parsePatients(String content) {
		Resource parsed = (Resource) fhirContext.newJsonParser().parseResource(content);
		List<Patient> patients = new ArrayList<>();
		if (parsed instanceof Patient) {
			patients.add((Patient) parsed);
			return patients;
		}
		if (parsed instanceof Bundle) {
			Bundle bundle = (Bundle) parsed;
			for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				if (entry.getResource() instanceof Patient) {
					patients.add((Patient) entry.getResource());
				}
			}
			return patients;
		}
		throw new IllegalArgumentException("Unsupported resource type. Upload Patient or Bundle JSON.");
	}
	
	private String resolveIdentifierLocationUuid(String locationUuid) {
		if (StringUtils.isNotBlank(locationUuid)) {
			return locationUuid.trim();
		}
		return OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID;
	}
	
	private void ensureIdentifierLocation(Patient patient, String locationUuidForExtension) {
		if (patient == null || patient.getIdentifier() == null) {
			return;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			boolean hasLocation = identifier.getExtension().stream()
					.anyMatch(ext -> OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL.equals(ext.getUrl()));
			if (hasLocation) {
				continue;
			}
			Extension locationExtension = new Extension();
			locationExtension.setUrl(OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL);
			locationExtension.setValue(new Reference("Location/" + locationUuidForExtension));
			identifier.getExtension().add(locationExtension);
		}
	}
	
	private void ensurePreferredOpenEmiIdentifier(Patient patient) {
		if (patient == null || patient.getIdentifier() == null || patient.getIdentifier().isEmpty()) {
			return;
		}

		Identifier preferredIdentifier = null;
		for (Identifier identifier : patient.getIdentifier()) {
			String code = identifier.hasType() && identifier.getType().hasCoding()
					? identifier.getType().getCodingFirstRep().getCode()
					: null;
			if (StringUtils.equals(code, preferredIdentifierTypeUuid)) {
				preferredIdentifier = identifier;
				break;
			}
		}

		if (preferredIdentifier == null) {
			Identifier sourceIdentifier = findOpenMrsIdentifierFromJson(patient);
			if (sourceIdentifier == null) {
				sourceIdentifier = findFirstNonEmptyIdentifier(patient);
				if (sourceIdentifier != null) {
					log.info("Import identifier-preferred fallback: using first non-empty identifier value='{}'",
							sourceIdentifier.getValue());
				}
			}
			if (sourceIdentifier == null || StringUtils.isBlank(sourceIdentifier.getValue())) {
				log.info("Import identifier-preferred skipped: no usable identifier found");
				return;
			}
			String sourceValue = sourceIdentifier.getValue();
			preferredIdentifier = new Identifier();
			preferredIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
			preferredIdentifier.setValue(sourceValue);
			preferredIdentifier.getType().setText(preferredIdentifierTypeName);
			preferredIdentifier.getType().addCoding().setCode(preferredIdentifierTypeUuid);
			patient.getIdentifier().add(preferredIdentifier);
		}

		boolean hasPreferredFlag = preferredIdentifier.getExtension().stream()
				.anyMatch(ext -> OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL.equals(ext.getUrl()));
		if (!hasPreferredFlag) {
			Extension preferredExtension = new Extension();
			preferredExtension.setUrl(OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL);
			preferredExtension.setValue(new BooleanType(true));
			preferredIdentifier.getExtension().add(preferredExtension);
		}
		log.info("Import identifier-preferred selected value='{}' system='{}'",
				preferredIdentifier.getValue(), preferredIdentifier.getSystem());
	}
	
	private boolean hasPreferredIdentifier(Patient patient) {
		if (patient == null || patient.getIdentifier() == null) {
			return false;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || identifier.getExtension() == null) {
				continue;
			}
			for (Extension extension : identifier.getExtension()) {
				if (OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL.equals(extension.getUrl())
				        && extension.getValue() instanceof BooleanType
				        && ((BooleanType) extension.getValue()).booleanValue()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean existsByIdentifier(Patient patient) {
		if (patient == null || patient.getIdentifier() == null || patient.getIdentifier().isEmpty()) {
			return false;
		}
		Identifier jsonOpenMrsId = findOpenMrsIdentifierFromJson(patient);
		if (jsonOpenMrsId == null || StringUtils.isBlank(jsonOpenMrsId.getValue())) {
			log.info("Import identifier-check skipped: JSON OpenMRS-ID not present");
			return false;
		}
		String identifierValue = jsonOpenMrsId.getValue();
		log.info("Import identifier-check using JSON OpenMRS-ID value='{}'", identifierValue);
		String encoded = UriUtils.encodeQueryParam(identifierValue, StandardCharsets.UTF_8.name());
		Bundle result = fhirConfig.getLocalOpenMRSFhirContext().search().byUrl("Patient?identifier=" + encoded)
		        .returnBundle(Bundle.class).execute();
		if (result != null && result.hasEntry()) {
			return true;
		}
		log.info("Import identifier-check no match for JSON OpenMRS-ID value='{}'", identifierValue);
		return false;
	}
	
	private static Identifier findOpenMrsIdentifierFromJson(Patient patient) {
		if (patient == null || patient.getIdentifier() == null) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || StringUtils.isBlank(identifier.getValue())) {
				continue;
			}
			if (isOpenMrsIdentifier(identifier)) {
				return identifier;
			}
		}
		return null;
	}
	
	private static boolean isOpenMrsIdentifier(Identifier identifier) {
		if (identifier == null) {
			return false;
		}
		boolean systemMatch = StringUtils.equals(StringUtils.trimToEmpty(identifier.getSystem()), OPENMRS_ID_SYSTEM);
		boolean mrCodeMatch = identifier.hasType() && identifier.getType().hasCoding()
				&& identifier.getType().getCoding().stream()
						.anyMatch(coding -> StringUtils.equals(coding.getSystem(), V2_IDENTIFIER_SYSTEM)
								&& StringUtils.equals(coding.getCode(), MR_IDENTIFIER_CODE));
		return systemMatch || mrCodeMatch;
	}
	
	private static Identifier findFirstNonEmptyIdentifier(Patient patient) {
		if (patient == null || patient.getIdentifier() == null) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier != null && StringUtils.isNotBlank(identifier.getValue())) {
				return identifier;
			}
		}
		return null;
	}
	
	/**
	 * When family, given, gender, and birth date are all present, searches the local OpenMRS FHIR
	 * store and skips create if a patient matches all of those fields. Does not alter
	 * {@link #existsByIdentifier(Patient)} behavior.
	 * <p>
	 * The server query uses only {@code birthdate}, {@code family}, and {@code _count}: OpenMRS
	 * FHIR2 returns HTTP 400 for compound searches that include {@code telecom} (and similar
	 * multi-param combinations). Given and gender are matched only in {@link #demographicsEqual}.
	 */
	private boolean existsByDemographics(Patient patient) {
		if (!hasAllDemographicsForDuplicateCheck(patient)) {
			log.info("Import duplicate-check skipped: missing required demographics for inputId={}", patient != null
			        && patient.getIdElement() != null ? patient.getIdElement().getIdPart() : null);
			return false;
		}
		log.info("Import duplicate-check input demographics: family='{}', given='{}', gender='{}', birthDate='{}'",
		    safeFamily(patient), safeGiven(patient), patient.getGender() != null ? patient.getGender().toCode() : null,
		    new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate()));
		List<Patient> candidates = searchLocalCandidatesByDemographics(patient);
		if (candidates.isEmpty()) {
			log.info("Import duplicate-check: no local candidates found");
			return false;
		}
		log.info("Import duplicate-check: local candidates found count={}", candidates.size());
		for (Patient found : candidates) {
			boolean matched = demographicsEqual(patient, found);
			if (matched) {
				log.info("Import duplicate-check matched existing patientId={}", found.getIdElement() != null ? found
				        .getIdElement().getIdPart() : null);
				return true;
			}
			log.info(
			    "Import duplicate-check candidate mismatch patientId={} family='{}' given='{}' gender='{}' birthDate='{}'",
			    found.getIdElement() != null ? found.getIdElement().getIdPart() : null, safeFamily(found), safeGiven(found),
			    found.getGender() != null ? found.getGender().toCode() : null, found.hasBirthDate() ? new SimpleDateFormat(
			            "yyyy-MM-dd").format(found.getBirthDate()) : null);
		}
		return false;
	}
	
	/**
	 * Tries supported local OpenMRS FHIR search combinations from narrow to broad, so we still get
	 * candidate rows even when a specific compound search is unsupported in some deployments.
	 */
	private List<Patient> searchLocalCandidatesByDemographics(Patient patient) {
		List<Map<String, String>> queries = buildLocalDemographicSearchCandidates(patient);
		for (Map<String, String> query : queries) {
			try {
				String searchParamString = ReuestParam.toQueryParam(query);
				log.info("Import duplicate-check trying local search query: {}", searchParamString);
				Bundle result = fhirConfig.getLocalOpenMRSFhirContext().search()
						.byUrl("Patient?" + searchParamString)
						.returnBundle(Bundle.class)
						.execute();
				log.info("Import duplicate-check query result entries={} for query={}",
						result != null && result.hasEntry() ? result.getEntry().size() : 0, searchParamString);
				if (result != null && result.hasEntry()) {
					return extractPatientEntries(result);
				}
			} catch (Exception ignored) {
				log.warn("Import duplicate-check query failed; trying broader query. query={}", query, ignored);
			}
		}
		// Final fallback for OpenMRS setups where demographic search params return empty bundles:
		// fetch multiple pages and rely on in-memory demographic comparison.
		List<Patient> allFallbackCandidates = new ArrayList<>();
		try {
			String fallbackQuery = "_count=200";
			log.info("Import duplicate-check trying local fallback query: {}", fallbackQuery);
			Bundle currentPage = fhirConfig.getLocalOpenMRSFhirContext().search()
					.byUrl("Patient?" + fallbackQuery)
					.returnBundle(Bundle.class)
					.execute();
			int page = 1;
			while (currentPage != null) {
				List<Patient> pagePatients = extractPatientEntries(currentPage);
				log.info("Import duplicate-check fallback query page={} entries={}", page, pagePatients.size());
				allFallbackCandidates.addAll(pagePatients);
				String nextUrl = currentPage.getLink(Bundle.LINK_NEXT) != null
						? currentPage.getLink(Bundle.LINK_NEXT).getUrl()
						: null;
				if (StringUtils.isBlank(nextUrl) || page >= 10) {
					break;
				}
				currentPage = fhirConfig.getLocalOpenMRSFhirContext().search()
						.byUrl(nextUrl)
						.returnBundle(Bundle.class)
						.execute();
				page++;
			}
		} catch (Exception e) {
			log.warn("Import duplicate-check fallback query failed", e);
		}
		return allFallbackCandidates;
	}
	
	private static List<Patient> extractPatientEntries(Bundle bundle) {
		List<Patient> patients = new ArrayList<>();
		if (bundle == null || !bundle.hasEntry()) {
			return patients;
		}
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				patients.add((Patient) entry.getResource());
			}
		}
		return patients;
	}
	
	private static boolean hasAllDemographicsForDuplicateCheck(Patient patient) {
		if (patient == null || !patient.hasBirthDate() || !patient.hasGender() || !patient.hasName()) {
			return false;
		}
		HumanName name = patient.getNameFirstRep();
		if (name == null || StringUtils.isBlank(name.getFamily())) {
			return false;
		}
		if (StringUtils.isBlank(name.getGivenAsSingleString())) {
			return false;
		}
		return true;
	}
	
	private static List<Map<String, String>> buildLocalDemographicSearchCandidates(Patient patient) {
		List<Map<String, String>> candidates = new ArrayList<>();
		HumanName name = patient.getNameFirstRep();
		String birthDate = new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate());
		String family = name.getFamily().trim();
		String given = StringUtils.trimToEmpty(name.getGivenAsSingleString());
		String gender = patient.getGender().toCode();

		Map<String, String> strict = new LinkedHashMap<>();
		strict.put("birthdate", birthDate);
		strict.put("family", family);
		strict.put("given", given);
		strict.put("gender", gender);
		strict.put("_count", "50");
		candidates.add(strict);

		Map<String, String> mid = new LinkedHashMap<>();
		mid.put("birthdate", birthDate);
		mid.put("family", family);
		mid.put("given", given);
		mid.put("_count", "50");
		candidates.add(mid);

		Map<String, String> broad = new LinkedHashMap<>();
		broad.put("birthdate", birthDate);
		broad.put("family", family);
		broad.put("_count", "50");
		candidates.add(broad);

		Map<String, String> fallback = new LinkedHashMap<>();
		fallback.put("birthdate", birthDate);
		fallback.put("_count", "50");
		candidates.add(fallback);
		return candidates;
	}
	
	private static boolean demographicsEqual(Patient incoming, Patient found) {
		if (!hasAllDemographicsForDuplicateCheck(incoming) || found == null || !found.hasBirthDate() || !found.hasGender()
		        || !found.hasName()) {
			return false;
		}
		HumanName inName = incoming.getNameFirstRep();
		HumanName fnName = found.getNameFirstRep();
		if (!StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(inName.getFamily()),
		    StringUtils.trimToEmpty(fnName.getFamily()))) {
			return false;
		}
		if (!StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(inName.getGivenAsSingleString()),
		    StringUtils.trimToEmpty(fnName.getGivenAsSingleString()))) {
			return false;
		}
		if (incoming.getGender() != found.getGender()) {
			return false;
		}
		String inDob = new SimpleDateFormat("yyyy-MM-dd").format(incoming.getBirthDate());
		String fdDob = new SimpleDateFormat("yyyy-MM-dd").format(found.getBirthDate());
		if (!StringUtils.equals(inDob, fdDob)) {
			return false;
		}
		return true;
	}
	
	private static String safeFamily(Patient patient) {
		if (patient == null || !patient.hasName() || patient.getNameFirstRep() == null) {
			return "";
		}
		return StringUtils.trimToEmpty(patient.getNameFirstRep().getFamily());
	}
	
	private static String safeGiven(Patient patient) {
		if (patient == null || !patient.hasName() || patient.getNameFirstRep() == null) {
			return "";
		}
		return StringUtils.trimToEmpty(patient.getNameFirstRep().getGivenAsSingleString());
	}
}
