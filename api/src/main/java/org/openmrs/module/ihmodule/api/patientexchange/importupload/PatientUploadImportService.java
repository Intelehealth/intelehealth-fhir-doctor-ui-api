package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.PersonAttributeType;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.param.ReuestParam;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.telecom.PatientTelecomMappingUtil;
import org.openmrs.module.ihmodule.api.patientexchange.telecom.PatientTelecomMappingUtil.TelecomValues;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PatientFhirExchangeValidationResult;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PatientFhirExchangeValidationService;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PatientProfileExtensionRules;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PatientUploadImportService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientUploadImportService.class);
	
	private static final String OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL = "http://fhir.openmrs.org/ext/patient/identifier#location";
	
	private static final String OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL = "http://fhir.openmrs.org/ext/patient/identifier#preferred";
	
	private static final String OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	private static final String OPENMRS_ID_SYSTEM = "http://intelehealth-central-fhir.mpower-social.com/fhir/StructureDefinition/OpenMRS-ID";
	
	private static final String V2_IDENTIFIER_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
	
	private static final String OPENMRS_IDENTIFIER_TYPE_CODING_SYSTEM = "http://fhir.openmrs.org/code-system/identifier-type";
	
	private static final String GP_PREFERRED_IDENTIFIER_TYPE_UUID = "intelehealth.fhir.patient.import.preferred.identifier.type.uuid";
	
	/**
	 * Reference Application OpenMRS ID type; used when {@link #GP_PREFERRED_IDENTIFIER_TYPE_UUID}
	 * is unset.
	 */
	private static final String DEFAULT_PREFERRED_IDENTIFIER_TYPE_UUID = "05a29f94-c0ed-11e2-94be-8c13b969e334";
	
	private static final String MR_IDENTIFIER_CODE = "MR";
	
	private static final String FHIR_PATIENT_ID_TYPE_NAME = "FHIR Patient ID";
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private FhirConfig fhirConfig;
	
	@Autowired
	private CommonOperationService commonOperationService;
	
	@Autowired
	private PatientFhirExchangeValidationService patientFhirExchangeValidationService;
	
	@Autowired
	private ImportPatientFuzzyDuplicateDetectionService importPatientFuzzyDuplicateDetectionService;
	
	public PatientUploadImportResponse importPatientFile(MultipartFile file, String locationUuid) throws Exception {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}
		
		String content = new String(file.getBytes(), StandardCharsets.UTF_8);
		List<Patient> patients = parsePatients(content);
		String effectiveLocationUuid = resolveIdentifierLocationUuid(locationUuid);
		
		PatientUploadImportResponse response = new PatientUploadImportResponse();
		response.setTotal(patients.size());
		log.error(
		    "IMPORT_UPLOAD_DEBUG batch start patientCount={} mpi.import.fuzzy.match.effective={} (GP overrides classpath; merged ihmodule+patientdataexchange)",
		    patients.size(), fhirConfig.isPatientImportFuzzyMatchEnabled());
		
		for (Patient patient : patients) {
			PatientUploadImportItemResult item = new PatientUploadImportItemResult();
			try {
				ensurePreferredOpenMrsIdentifier(patient, effectiveLocationUuid);
				ensureIdentifierLocation(patient, effectiveLocationUuid);
				normalizeBirthDateDayPrecision(patient);
				item.setInputId(correlationIdForImportItem(patient));
				validatePatientAgainstProfileBeforeCreate(patient);
				String outboundSnapshotForDuplicateReview = fhirContext.newJsonParser().encodeResourceToString(patient);
				if (fhirConfig.isPatientImportFuzzyMatchEnabled()) {
					String importCorrelationKey = buildImportDuplicateCorrelationKey(patient);
					Optional<MpiPatientDuplicateReviewCase> fuzzyDup = importPatientFuzzyDuplicateDetectionService
					        .evaluateAndPersistIfDuplicate(patient, outboundSnapshotForDuplicateReview, importCorrelationKey);
					if (fuzzyDup.isPresent()) {
						item.setStatus("DUPLICATE_REVIEW");
						item.setDuplicateReviewCaseUuid(fuzzyDup.get().getCaseUuid());
						item.setDuplicateDetectedSource(fuzzyDup.get().getCandidates().stream()
						    .map(c -> c.getMatchSource()).filter(Objects::nonNull).distinct().sorted()
						    .collect(Collectors.joining(", ")));
						item.setMessage("Fuzzy duplicate detected; patient not imported. case_uuid="
						        + fuzzyDup.get().getCaseUuid());
						response.setDuplicateReviewDeferred(response.getDuplicateReviewDeferred() + 1);
						response.getItems().add(item);
						continue;
					}
				} else {
					if (existsByIdentifier(patient)) {
						item.setStatus("SKIPPED");
						item.setMessage("Patient already exists by identifier");
						response.setSkipped(response.getSkipped() + 1);
						response.getItems().add(item);
						continue;
					}
					boolean demographicDuplicateCheckEnabled = fhirConfig.isPatientImportDemographicDuplicateCheckEnabled();
					if (demographicDuplicateCheckEnabled && existsByDemographics(patient)) {
						item.setStatus("SKIPPED");
						item.setMessage("Patient already exists (same family, given, gender, birth date)");
						response.setSkipped(response.getSkipped() + 1);
						log.info(
						    "Import duplicate-check: skipped — demographic match in DB (identifier search did not fire first). correlationId={}, family={}, given={}, birthDate={}",
						    item.getInputId(), safeFamily(patient), safeGiven(patient), formatBirthDateYyyyMmDd(patient));
						response.getItems().add(item);
						continue;
					}
					if (!demographicDuplicateCheckEnabled) {
						log.debug(
						    "Import duplicate-check disabled by config intelehealth.fhir.patient.import.demographic.duplicate.check.enabled for correlationId={}",
						    item.getInputId());
					}
				}
				if (!hasPreferredIdentifier(patient)) {
					throw new IllegalArgumentException("No preferred identifier available for create");
				}
				patient.setId((String) null);
				String finalPayload = fhirContext.newJsonParser().encodeResourceToString(patient);
				log.info("Import upload final patient payload for correlationId={}: {}", item.getInputId(), finalPayload);
				if (fhirConfig.isPatientImportNativeCreateEnabled()) {
					String createdUuid = createPatientViaOpenmrsNativeApi(patient, effectiveLocationUuid);
					item.setStatus("CREATED");
					item.setCreatedId(createdUuid);
					persistImportedPatientSupplementalAttributes(createdUuid, patient);
					item.setMessage("Patient created");
				} /*
					 * else { ca.uhn.fhir.rest.api.MethodOutcome outcome =
					 * fhirConfig.getLocalOpenMRSFhirContext().create()
					 * .resource(patient).execute(); item.setStatus("CREATED");
					 * item.setCreatedId(outcome.getId() != null ? outcome.getId().getIdPart() :
					 * null); persistImportedPatientSupplementalAttributes(item.getCreatedId(),
					 * patient); item.setMessage("Patient created"); }
					 */
				response.setCreated(response.getCreated() + 1);
			}
			catch (Exception e) {
				log.error("Import upload failed for inputId={} with message={}", item.getInputId(), e.getMessage(), e);
				try {
					String payload = fhirContext.newJsonParser().encodeResourceToString(patient);
					log.error("Import upload failed payload for inputId={}: {}", item.getInputId(), payload);
				}
				catch (Exception serializationEx) {
					log.warn("Unable to serialize failed patient payload for inputId={}", item.getInputId(), serializationEx);
				}
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
	
	/**
	 * When JSON has no {@code Patient.identifier}, no OpenMRS-ID slot, or an OpenMRS-ID slot with a
	 * blank {@code value}, assigns the next identifier from the preferred
	 * {@link PatientIdentifierType} using the idgen module when installed. Non-empty values from
	 * JSON are left unchanged.
	 */
	private void ensureOpenMrsIdentifierValueFromPreferredTypeIfMissing(Patient patient, String locationUuid) {
		if (patient == null) {
			return;
		}
		Identifier slot = findOpenMrsIdentifierSlotAllowingBlankValue(patient);
		if (slot == null) {
			appendSyntheticOpenMrsIdentifierPlaceholder(patient);
			slot = findOpenMrsIdentifierSlotAllowingBlankValue(patient);
		}
		if (slot == null) {
			throw new IllegalStateException("Unable to attach OpenMRS ID identifier for import");
		}
		if (StringUtils.isNotBlank(StringUtils.trimToNull(slot.getValue()))) {
			log.debug("Import OpenMRS ID value present in JSON; skipping auto-generation");
			return;
		}
		String generated = generateOpenMrsIdentifierValueUsingPreferredType(locationUuid);
		slot.setValue(generated);
		log.info("Import auto-generated OpenMRS ID value='{}' (preferred type uuid={})", generated,
		    resolvePreferredIdentifierTypeUuid());
	}
	
	private Identifier findOpenMrsIdentifierSlotAllowingBlankValue(Patient patient) {
		if (patient == null || patient.getIdentifier() == null) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier != null && isOpenMrsIdentifier(identifier)) {
				return identifier;
			}
		}
		return null;
	}
	
	private void appendSyntheticOpenMrsIdentifierPlaceholder(Patient patient) {
		Identifier id = new Identifier();
		id.setUse(Identifier.IdentifierUse.OFFICIAL);
		org.hl7.fhir.r4.model.CodeableConcept type = new org.hl7.fhir.r4.model.CodeableConcept();
		type.setText("OpenMRS ID");
		type.addCoding().setSystem(V2_IDENTIFIER_SYSTEM).setCode(MR_IDENTIFIER_CODE);
		id.setType(type);
		id.setSystem(OPENMRS_ID_SYSTEM);
		id.setValue("");
		patient.addIdentifier(id);
	}
	
	private String generateOpenMrsIdentifierValueUsingPreferredType(String locationUuid) {
		String typeUuid = resolvePreferredIdentifierTypeUuid();
		if (StringUtils.isBlank(typeUuid)) {
			throw new IllegalArgumentException("Cannot auto-generate OpenMRS ID: preferred identifier type UUID is blank");
		}
		PatientIdentifierType idType = Context.getPatientService().getPatientIdentifierTypeByUuid(typeUuid.trim());
		if (idType == null) {
			throw new IllegalArgumentException("Patient identifier type not found for UUID: " + typeUuid);
		}
		String fromIdgen = invokeIdgenGenerateIdentifier(idType, locationUuid);
		if (StringUtils.isNotBlank(fromIdgen)) {
			return fromIdgen.trim();
		}
		throw new IllegalArgumentException(
		        "Cannot auto-generate OpenMRS ID: idgen returned no identifier (install/configure idgen auto-generation for type "
		                + typeUuid + ") or supply a non-empty identifier.value in JSON.");
	}
	
	@SuppressWarnings("unchecked")
	private String invokeIdgenGenerateIdentifier(PatientIdentifierType idType, String locationUuid) {
		try {
			Class<? extends org.openmrs.api.OpenmrsService> svcClass = (Class<? extends org.openmrs.api.OpenmrsService>) Context
			        .loadClass("org.openmrs.module.idgen.service.IdentifierSourceService");
			Object service = Context.getService(svcClass);
			Location loc = resolveLocationForIdgen(locationUuid);
			if (loc != null) {
				try {
					Method withLoc = svcClass.getMethod("generateIdentifier", PatientIdentifierType.class, Location.class,
					    String.class);
					return (String) withLoc.invoke(service, idType, loc, "ihmodule patient import upload");
				}
				catch (NoSuchMethodException ignored) {
					// older idgen without location overload
				}
			}
			Method simple = svcClass.getMethod("generateIdentifier", PatientIdentifierType.class, String.class);
			return (String) simple.invoke(service, idType, "ihmodule patient import upload");
		}
		catch (ClassNotFoundException e) {
			log.warn("OpenMRS idgen module is not available; cannot auto-generate patient identifiers", e);
			return null;
		}
		catch (InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			log.error("idgen generateIdentifier failed: {}", cause.getMessage(), cause);
			return null;
		}
		catch (Exception e) {
			log.error("idgen generateIdentifier failed: {}", e.getMessage(), e);
			return null;
		}
	}
	
	private Location resolveLocationForIdgen(String locationUuid) {
		String uuid = StringUtils.isNotBlank(locationUuid) ? locationUuid.trim() : OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID;
		try {
			return Context.getLocationService().getLocationByUuid(uuid);
		}
		catch (Exception ex) {
			log.debug("Could not resolve location {} for idgen: {}", uuid, ex.getMessage());
			return null;
		}
	}
	
	private void ensurePreferredOpenMrsIdentifier(Patient patient, String locationUuidForIdgen) {
		ensureOpenMrsIdentifierValueFromPreferredTypeIfMissing(patient, locationUuidForIdgen);
		if (patient == null || patient.getIdentifier() == null || patient.getIdentifier().isEmpty()) {
			throw new IllegalArgumentException("OpenMRS ID identifier is required for import");
		}
		Identifier preferredIdentifier = findOpenMrsIdentifierFromJson(patient);
		if (preferredIdentifier == null || StringUtils.isBlank(preferredIdentifier.getValue())) {
			throw new IllegalArgumentException("OpenMRS ID identifier is required for import");
		}
		preferredIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
		// Local OpenMRS create is identifier-type driven; keeping a remote system URI can break
		// identifier type resolution for required OpenMRS ID.
		preferredIdentifier.setSystem(null);
		if (!preferredIdentifier.hasType()) {
			preferredIdentifier.setType(new org.hl7.fhir.r4.model.CodeableConcept());
		}
		if (StringUtils.isBlank(preferredIdentifier.getType().getText())) {
			preferredIdentifier.getType().setText("OpenMRS ID");
		}
		if (!preferredIdentifier.getType().hasCoding()) {
			preferredIdentifier.getType().addCoding().setSystem(V2_IDENTIFIER_SYSTEM).setCode(MR_IDENTIFIER_CODE);
		} else {
			preferredIdentifier.getType().getCodingFirstRep().setSystem(V2_IDENTIFIER_SYSTEM).setCode(MR_IDENTIFIER_CODE);
		}
		String resolvedTypeUuid = resolvePreferredIdentifierTypeUuid();
		if (StringUtils.isNotBlank(resolvedTypeUuid)) {
			boolean hasTypeUuidCoding = preferredIdentifier.getType().getCoding().stream()
					.anyMatch(coding -> StringUtils.equals(coding.getCode(), resolvedTypeUuid));
			if (!hasTypeUuidCoding) {
				preferredIdentifier.getType().addCoding().setSystem(OPENMRS_IDENTIFIER_TYPE_CODING_SYSTEM)
						.setCode(resolvedTypeUuid);
			}
		}
		enforceSinglePreferredIdentifier(patient, preferredIdentifier);
		log.info("Import identifier-preferred selected OpenMRS ID value='{}' system='{}'",
				preferredIdentifier.getValue(), preferredIdentifier.getSystem());
	}
	
	private String resolvePreferredIdentifierTypeUuid() {
		String preferredIdentifierTypeUuid = fhirConfig.getPatientImportPreferredIdentifierTypeUuid();
		if (StringUtils.isNotBlank(preferredIdentifierTypeUuid)) {
			return preferredIdentifierTypeUuid.trim();
		}
		try {
			String gpValue = Context.getAdministrationService().getGlobalProperty(GP_PREFERRED_IDENTIFIER_TYPE_UUID);
			if (StringUtils.isNotBlank(gpValue)) {
				return gpValue.trim();
			}
		}
		catch (Exception ex) {
			log.warn("Unable to read global property {}", GP_PREFERRED_IDENTIFIER_TYPE_UUID, ex);
		}
		log.debug("Using default OpenMRS ID identifier type UUID for import (set {} to override)",
		    GP_PREFERRED_IDENTIFIER_TYPE_UUID);
		return DEFAULT_PREFERRED_IDENTIFIER_TYPE_UUID;
	}
	
	private void enforceSinglePreferredIdentifier(Patient patient, Identifier preferredIdentifier) {
		if (patient == null || patient.getIdentifier() == null || preferredIdentifier == null) {
			return;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || identifier.getExtension() == null) {
				continue;
			}
			identifier.getExtension()
					.removeIf(ext -> OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL.equals(ext.getUrl()));
		}
		Extension preferredExtension = new Extension();
		preferredExtension.setUrl(OPENMRS_IDENTIFIER_PREFERRED_EXTENSION_URL);
		preferredExtension.setValue(new BooleanType(true));
		preferredIdentifier.getExtension().add(preferredExtension);
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
		String family = safeFamily(patient);
		String given = safeGiven(patient);
		String gender = patient.getGender() != null ? patient.getGender().toCode() : null;
		String birthDate = formatBirthDateYyyyMmDd(patient);
		String phone = safePhone(patient);
		log.info(
		    "Import duplicate-check DB query demographics: family='{}', given='{}', gender='{}', birthDate='{}', phone='{}'",
		    family, given, gender, birthDate, phone);
		try {
			boolean exists = commonOperationService.existsPatientByDemographicsAndPhone(family, given, gender, birthDate,
			    phone);
			if (exists) {
				log.info("Import duplicate-check DB matched existing patient for demographics");
			} else {
				log.info("Import duplicate-check DB found no match for demographics");
			}
			return exists;
		}
		catch (Exception ex) {
			log.warn("Import duplicate-check DB query failed; falling back to local FHIR search", ex);
			List<Patient> candidates = searchLocalCandidatesByDemographics(patient);
			for (Patient found : candidates) {
				if (demographicsEqual(patient, found)) {
					return true;
				}
			}
			return false;
		}
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
		if (!fhirConfig.isPatientImportDemographicDuplicateFallbackEnabled()) {
			log.info("Import duplicate-check fallback query disabled by configuration");
			return new ArrayList<>();
		}
		List<Patient> allFallbackCandidates = new ArrayList<>();
		try {
			String fallbackQuery = "_count=" + safeSearchCount();
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
				if (StringUtils.isBlank(nextUrl) || page >= safeFallbackMaxPages()) {
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
	
	private List<Map<String, String>> buildLocalDemographicSearchCandidates(Patient patient) {
		List<Map<String, String>> candidates = new ArrayList<>();
		HumanName name = patient.getNameFirstRep();
		String birthDate = formatBirthDateYyyyMmDd(patient);
		String family = name.getFamily().trim();
		String given = StringUtils.trimToEmpty(name.getGivenAsSingleString());
		String gender = patient.getGender().toCode();
		String searchCount = Integer.toString(safeSearchCount());

		Map<String, String> strict = new LinkedHashMap<>();
		strict.put("birthdate", birthDate);
		strict.put("family", family);
		strict.put("given", given);
		strict.put("gender", gender);
		strict.put("_count", searchCount);
		candidates.add(strict);

		Map<String, String> mid = new LinkedHashMap<>();
		mid.put("birthdate", birthDate);
		mid.put("family", family);
		mid.put("given", given);
		mid.put("_count", searchCount);
		candidates.add(mid);

		Map<String, String> broad = new LinkedHashMap<>();
		broad.put("birthdate", birthDate);
		broad.put("family", family);
		broad.put("_count", searchCount);
		candidates.add(broad);

		Map<String, String> fallback = new LinkedHashMap<>();
		fallback.put("birthdate", birthDate);
		fallback.put("_count", searchCount);
		candidates.add(fallback);
		return candidates;
	}
	
	private int safeSearchCount() {
		int searchCount = fhirConfig.getPatientImportDemographicDuplicateCheckSearchCount();
		return searchCount > 0 ? searchCount : 20;
	}
	
	private int safeFallbackMaxPages() {
		int maxPages = fhirConfig.getPatientImportDemographicDuplicateFallbackMaxPages();
		return maxPages > 0 ? maxPages : 2;
	}
	
	private void persistImportedPatientSupplementalAttributes(String createdPatientUuid, Patient sourcePatient) {
		if (StringUtils.isBlank(createdPatientUuid) || sourcePatient == null) {
			return;
		}
		org.openmrs.Patient createdPatient = Context.getPatientService().getPatientByUuid(createdPatientUuid);
		if (createdPatient == null) {
			log.warn("Import supplemental attribute mapping skipped: local patient not found for uuid={}",
			    createdPatientUuid);
			return;
		}
		boolean changed = persistImportedPatientExtensions(createdPatient, sourcePatient);
		changed = persistImportedPatientTelecom(createdPatient, sourcePatient) || changed;
		if (changed) {
			Context.getPatientService().savePatient(createdPatient);
		}
	}
	
	private boolean persistImportedPatientExtensions(org.openmrs.Patient createdPatient, Patient sourcePatient) {
		if (createdPatient == null || sourcePatient == null || sourcePatient.getExtension() == null
		        || sourcePatient.getExtension().isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (Extension extension : sourcePatient.getExtension()) {
			String suffix = PatientProfileExtensionRules.extensionSuffixFromStructureDefinitionUrl(extension.getUrl());
			if (StringUtils.isBlank(suffix)) {
				continue;
			}
			if ("Emergency-Contact-Number".equals(suffix)) {
				log.info("Import extension mapping skipped for suffix={} because telecom now carries that value", suffix);
				continue;
			}
			String value = extensionStringValue(extension);
			if (StringUtils.isBlank(value)) {
				continue;
			}
			PersonAttributeType type = resolvePersonAttributeTypeForSuffix(suffix);
			if (type == null) {
				log.warn("Import extension mapping skipped: no person_attribute_type found for suffix={} value={}", suffix,
				    value);
				continue;
			}
			log.info("Import extension mapping resolved: suffix={} -> person_attribute_type='{}' value='{}'", suffix,
			    type.getName(), value);
			org.openmrs.PersonAttribute existing = createdPatient.getAttribute(type);
			if (existing == null) {
				createdPatient.addAttribute(new org.openmrs.PersonAttribute(type, value));
				log.info("Import extension mapping applied: added person_attribute_type='{}' value='{}'", type.getName(),
				    value);
				changed = true;
			} else if (!StringUtils.equals(existing.getValue(), value)) {
				String previousValue = existing.getValue();
				existing.setValue(value);
				log.info("Import extension mapping applied: updated person_attribute_type='{}' oldValue='{}' newValue='{}'",
				    type.getName(), previousValue, value);
				changed = true;
			}
		}
		return changed;
	}
	
	private boolean persistImportedPatientTelecom(org.openmrs.Patient createdPatient, Patient sourcePatient) {
		TelecomValues telecomValues = PatientTelecomMappingUtil.extractRankedPhoneTelecom(sourcePatient);
		boolean changed = false;
		changed = upsertPersonAttribute(createdPatient, attributeTypeNameCandidatesForTelecom("telephoneNumber"),
		    telecomValues.getTelephoneNumber(), "telephoneNumber") || changed;
		changed = upsertPersonAttribute(createdPatient, attributeTypeNameCandidatesForTelecom("emergencycontactnumber"),
		    telecomValues.getEmergencyContactNumber(), "emergencycontactnumber") || changed;
		return changed;
	}
	
	private boolean upsertPersonAttribute(org.openmrs.Patient createdPatient, List<String> typeCandidates, String value,
	        String logicalName) {
		if (createdPatient == null || StringUtils.isBlank(value)) {
			return false;
		}
		PersonAttributeType type = resolvePersonAttributeType(typeCandidates);
		if (type == null) {
			log.warn("Import telecom mapping skipped: no person_attribute_type found for {} using candidates={}",
			    logicalName, typeCandidates);
			return false;
		}
		org.openmrs.PersonAttribute existing = createdPatient.getAttribute(type);
		if (existing == null) {
			createdPatient.addAttribute(new org.openmrs.PersonAttribute(type, value));
			log.info("Import telecom mapping applied: added person_attribute_type='{}' value='{}'", type.getName(), value);
			return true;
		}
		if (!StringUtils.equals(existing.getValue(), value)) {
			String previousValue = existing.getValue();
			existing.setValue(value);
			log.info("Import telecom mapping applied: updated person_attribute_type='{}' oldValue='{}' newValue='{}'",
			    type.getName(), previousValue, value);
			return true;
		}
		return false;
	}
	
	private void normalizeBirthDateDayPrecision(Patient patient) {
		if (patient == null || !patient.hasBirthDateElement()) {
			return;
		}
		String birthDateText = patient.getBirthDateElement().asStringValue();
		if (StringUtils.isBlank(birthDateText)) {
			return;
		}
		// Keep date-only value to avoid timezone rollback (e.g. 1996-05-23 -> 1996-05-22).
		patient.setBirthDateElement(new DateType(birthDateText));
	}
	
	/**
	 * Maps FHIR {@code birthDate} (calendar date) to {@link Date} for OpenMRS
	 * {@code person.birthdate}. Raw {@link Patient#getBirthDate()} can shift the stored calendar
	 * day across JDBC/time zones; parsing the normalized yyyy-MM-dd string and anchoring at noon
	 * UTC avoids off-by-one dates in the DB.
	 */
	private static Date resolveBirthDateForOpenmrs(Patient fhirPatient) {
		if (fhirPatient == null || !fhirPatient.hasBirthDate()) {
			return null;
		}
		if (fhirPatient.hasBirthDateElement()) {
			String raw = fhirPatient.getBirthDateElement().asStringValue();
			if (StringUtils.isNotBlank(raw) && raw.length() >= 10) {
				String ymd = raw.substring(0, 10);
				try {
					LocalDate ld = LocalDate.parse(ymd);
					return Date.from(ld.atTime(12, 0).atZone(ZoneOffset.UTC).toInstant());
				}
				catch (Exception ignored) {
					// partial FHIR dates or non-ISO — fall back
				}
			}
		}
		return fhirPatient.getBirthDate();
	}
	
	/**
	 * Persists a new OpenMRS patient from the prepared FHIR resource. Only used when
	 * {@link FhirConfig#isPatientImportNativeCreateEnabled()} is true; default import continues to
	 * use FHIR2 POST.
	 */
	private String createPatientViaOpenmrsNativeApi(Patient fhirPatient, String locationUuid) {
		String typeUuid = resolvePreferredIdentifierTypeUuid();
		if (StringUtils.isBlank(typeUuid)) {
			throw new IllegalArgumentException(
			        "Preferred identifier type UUID is required for native import (same as FHIR import)");
		}
		PatientIdentifierType idType = Context.getPatientService().getPatientIdentifierTypeByUuid(typeUuid.trim());
		if (idType == null) {
			throw new IllegalArgumentException("Patient identifier type not found for UUID: " + typeUuid);
		}
		String locUuid = StringUtils.isNotBlank(locationUuid) ? locationUuid.trim()
		        : OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID;
		Location location = Context.getLocationService().getLocationByUuid(locUuid);
		if (location == null) {
			throw new IllegalArgumentException("Location not found for UUID: " + locUuid);
		}
		ensurePreferredOpenMrsIdentifier(fhirPatient, locUuid);
		Identifier preferred = findOpenMrsIdentifierFromJson(fhirPatient);
		if (preferred == null || StringUtils.isBlank(preferred.getValue())) {
			throw new IllegalArgumentException("OpenMRS ID identifier is required for native import");
		}
		
		org.openmrs.Patient omrsPatient = new org.openmrs.Patient();
		
		if (fhirPatient.hasName() && fhirPatient.getNameFirstRep() != null) {
			HumanName hn = fhirPatient.getNameFirstRep();
			PersonName pn = new PersonName();
			pn.setFamilyName(StringUtils.trimToNull(hn.getFamily()));
			if (hn.hasGiven() && !hn.getGiven().isEmpty()) {
				pn.setGivenName(StringUtils.trimToNull(hn.getGiven().get(0).getValue()));
				if (hn.getGiven().size() > 1) {
					StringBuilder mid = new StringBuilder();
					for (int i = 1; i < hn.getGiven().size(); i++) {
						if (mid.length() > 0) {
							mid.append(' ');
						}
						mid.append(hn.getGiven().get(i).getValue());
					}
					pn.setMiddleName(StringUtils.trimToNull(mid.toString()));
				}
			}
			pn.setPreferred(true);
			omrsPatient.addName(pn);
		}
		
		if (fhirPatient.hasGender()) {
			String g = fhirPatient.getGender().toCode();
			if ("male".equalsIgnoreCase(g)) {
				omrsPatient.setGender("M");
			} else if ("female".equalsIgnoreCase(g)) {
				omrsPatient.setGender("F");
			} else {
				omrsPatient.setGender("U");
			}
		}
		
		if (fhirPatient.hasBirthDate()) {
			omrsPatient.setBirthdate(resolveBirthDateForOpenmrs(fhirPatient));
		}
		
		if (fhirPatient.hasAddress()) {
			Address fa = fhirPatient.getAddressFirstRep();
			PersonAddress pa = new PersonAddress();
			if (fa.hasLine()) {
				List<StringType> lines = fa.getLine();
				if (!lines.isEmpty() && lines.get(0).getValue() != null) {
					pa.setAddress1(lines.get(0).getValue());
				}
				if (lines.size() > 1 && lines.get(1).getValue() != null) {
					pa.setAddress2(lines.get(1).getValue());
				}
				if (lines.size() > 2) {
					StringBuilder rest = new StringBuilder();
					for (int i = 2; i < lines.size(); i++) {
						if (lines.get(i).getValue() == null) {
							continue;
						}
						if (rest.length() > 0) {
							rest.append(' ');
						}
						rest.append(lines.get(i).getValue());
					}
					pa.setAddress3(rest.toString());
				}
			}
			pa.setCityVillage(fa.getCity());
			pa.setStateProvince(fa.getState());
			pa.setPostalCode(fa.getPostalCode());
			pa.setCountry(fa.getCountry());
			pa.setPreferred(true);
			omrsPatient.addAddress(pa);
		}
		
		PatientIdentifier pid = new PatientIdentifier();
		pid.setIdentifier(preferred.getValue().trim());
		pid.setIdentifierType(idType);
		pid.setLocation(location);
		pid.setPreferred(true);
		omrsPatient.addIdentifier(pid);
		
		Context.getPatientService().savePatient(omrsPatient);
		return omrsPatient.getUuid();
	}
	
	/**
	 * Local OpenMRS upsert from a FHIR Patient snapshot: lookup by identifier (or UUID), update if
	 * found, otherwise create via {@link #createPatientViaOpenmrsNativeApi}.
	 */
	public OpenmrsPatientUpsertResult upsertOpenmrsPatientFromFhirPatient(Patient fhirPatient, String locationUuid) {
		requireNativeCreateEnabled();
		if (fhirPatient == null) {
			throw new IllegalArgumentException("FHIR Patient is required");
		}
		Patient prepared = prepareFhirPatientForNativeUpsert(fhirPatient, locationUuid);
		org.openmrs.Patient existing = findOpenmrsPatientByFhirIdentifiers(prepared);
		if (existing != null) {
			log.info("patient found by identifier: openmrsPatientUuid={}", existing.getUuid());
			String uuid = updatePatientViaOpenmrsNativeApi(existing, prepared);
			persistImportedPatientSupplementalAttributes(uuid, prepared);
			log.info("patient updated: openmrsPatientUuid={}", uuid);
			return new OpenmrsPatientUpsertResult(uuid, OpenmrsPatientUpsertResult.Action.UPDATED, "patient updated");
		}
		String uuid = createPatientViaOpenmrsNativeApi(prepared, resolveIdentifierLocationUuid(locationUuid));
		persistImportedPatientSupplementalAttributes(uuid, prepared);
		log.info("patient created: openmrsPatientUuid={}", uuid);
		return new OpenmrsPatientUpsertResult(uuid, OpenmrsPatientUpsertResult.Action.CREATED, "patient created");
	}
	
	/**
	 * Force-sync / add-patient by OpenMRS UUID: builds a FHIR snapshot from the local DB row and
	 * upserts (no central or FHIR server writes).
	 */
	public OpenmrsPatientUpsertResult forceSyncLocalOpenmrsPatientByUuid(String patientUuid) {
		requireNativeCreateEnabled();
		if (StringUtils.isBlank(patientUuid)) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		org.openmrs.Patient omrs = Context.getPatientService().getPatientByUuid(patientUuid.trim());
		if (omrs == null || Boolean.TRUE.equals(omrs.getVoided())) {
			throw new IllegalArgumentException("OpenMRS patient not found for uuid=" + patientUuid.trim());
		}
		return upsertOpenmrsPatientFromFhirPatient(buildFhirPatientSnapshotFromOpenmrs(omrs), null);
	}
	
	/**
	 * MPI duplicate-review: create or update OpenMRS patient from a stored FHIR Patient snapshot.
	 */
	public String duplicateReviewCreateOpenmrsFromFhirPatient(Patient fhirPatient, String locationUuid) {
		return upsertOpenmrsPatientFromFhirPatient(fhirPatient, locationUuid).getPatientUuid();
	}
	
	private void requireNativeCreateEnabled() {
		if (!fhirConfig.isPatientImportNativeCreateEnabled()) {
			throw new IllegalStateException(
			        "Enable intelehealth.fhir.patient.import.native.create.enabled for local OpenMRS patient upsert.");
		}
	}
	
	private Patient prepareFhirPatientForNativeUpsert(Patient fhirPatient, String locationUuid) {
		Patient copy = fhirPatient.copy();
		copy.setId((String) null);
		String loc = resolveIdentifierLocationUuid(locationUuid);
		ensurePreferredOpenMrsIdentifier(copy, loc);
		ensureIdentifierLocation(copy, loc);
		normalizeBirthDateDayPrecision(copy);
		validatePatientAgainstProfileBeforeCreate(copy);
		return copy;
	}
	
	private org.openmrs.Patient findOpenmrsPatientByFhirIdentifiers(Patient fhirPatient) {
		if (fhirPatient == null) {
			return null;
		}
		String idPart = fhirPatient.getIdElement() != null ? StringUtils.trimToNull(fhirPatient.getIdElement().getIdPart())
		        : null;
		if (idPart != null) {
			org.openmrs.Patient byUuid = Context.getPatientService().getPatientByUuid(idPart);
			if (byUuid != null && !Boolean.TRUE.equals(byUuid.getVoided())) {
				return byUuid;
			}
		}
		Identifier openMrsId = findOpenMrsIdentifierFromJson(fhirPatient);
		if (openMrsId != null && StringUtils.isNotBlank(openMrsId.getValue())) {
			org.openmrs.Patient byOpenMrsId = findOpenmrsPatientByIdentifierValue(openMrsId.getValue().trim(),
			    resolvePreferredIdentifierType());
			if (byOpenMrsId != null) {
				return byOpenMrsId;
			}
		}
		if (fhirPatient.getIdentifier() == null || fhirPatient.getIdentifier().isEmpty()) {
			return null;
		}
		PatientIdentifierType fhirIdType = Context.getPatientService().getPatientIdentifierTypeByName(
		    FHIR_PATIENT_ID_TYPE_NAME);
		for (Identifier identifier : fhirPatient.getIdentifier()) {
			if (identifier == null || StringUtils.isBlank(identifier.getValue()) || isOpenMrsIdentifier(identifier)) {
				continue;
			}
			if (fhirIdType != null) {
				org.openmrs.Patient byFhirId = findOpenmrsPatientByIdentifierValue(identifier.getValue().trim(), fhirIdType);
				if (byFhirId != null) {
					return byFhirId;
				}
			}
		}
		return null;
	}
	
	private PatientIdentifierType resolvePreferredIdentifierType() {
		String typeUuid = resolvePreferredIdentifierTypeUuid();
		if (StringUtils.isBlank(typeUuid)) {
			return null;
		}
		return Context.getPatientService().getPatientIdentifierTypeByUuid(typeUuid.trim());
	}
	
	private org.openmrs.Patient findOpenmrsPatientByIdentifierValue(String identifierValue, PatientIdentifierType idType) {
		if (StringUtils.isBlank(identifierValue) || idType == null) {
			return null;
		}
		List<org.openmrs.Patient> matches = Context.getPatientService().getPatients(null, identifierValue.trim(),
		    Collections.singletonList(idType), false);
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		for (org.openmrs.Patient candidate : matches) {
			if (candidate != null && !Boolean.TRUE.equals(candidate.getVoided())) {
				return candidate;
			}
		}
		return null;
	}
	
	private String updatePatientViaOpenmrsNativeApi(org.openmrs.Patient omrsPatient, Patient fhirPatient) {
		if (omrsPatient == null) {
			throw new IllegalArgumentException("OpenMRS patient is required for update");
		}
		if (fhirPatient == null) {
			throw new IllegalArgumentException("FHIR Patient is required for update");
		}
		applyFhirDemographicsToOpenmrsPatient(omrsPatient, fhirPatient);
		Context.getPatientService().savePatient(omrsPatient);
		return omrsPatient.getUuid();
	}
	
	private void applyFhirDemographicsToOpenmrsPatient(org.openmrs.Patient omrsPatient, Patient fhirPatient) {
		if (fhirPatient.hasName() && fhirPatient.getNameFirstRep() != null) {
			HumanName hn = fhirPatient.getNameFirstRep();
			PersonName pn = omrsPatient.getPersonName();
			if (pn == null) {
				pn = new PersonName();
				pn.setPreferred(true);
				omrsPatient.addName(pn);
			}
			pn.setFamilyName(StringUtils.trimToNull(hn.getFamily()));
			if (hn.hasGiven() && !hn.getGiven().isEmpty()) {
				pn.setGivenName(StringUtils.trimToNull(hn.getGiven().get(0).getValue()));
				if (hn.getGiven().size() > 1) {
					StringBuilder mid = new StringBuilder();
					for (int i = 1; i < hn.getGiven().size(); i++) {
						if (mid.length() > 0) {
							mid.append(' ');
						}
						mid.append(hn.getGiven().get(i).getValue());
					}
					pn.setMiddleName(StringUtils.trimToNull(mid.toString()));
				}
			}
		}
		if (fhirPatient.hasGender()) {
			String g = fhirPatient.getGender().toCode();
			if ("male".equalsIgnoreCase(g)) {
				omrsPatient.setGender("M");
			} else if ("female".equalsIgnoreCase(g)) {
				omrsPatient.setGender("F");
			} else {
				omrsPatient.setGender("U");
			}
		}
		if (fhirPatient.hasBirthDate()) {
			omrsPatient.setBirthdate(resolveBirthDateForOpenmrs(fhirPatient));
		}
		if (fhirPatient.hasAddress()) {
			Address fa = fhirPatient.getAddressFirstRep();
			PersonAddress pa = omrsPatient.getPersonAddress();
			if (pa == null) {
				pa = new PersonAddress();
				pa.setPreferred(true);
				omrsPatient.addAddress(pa);
			}
			if (fa.hasLine()) {
				List<StringType> lines = fa.getLine();
				if (!lines.isEmpty() && lines.get(0).getValue() != null) {
					pa.setAddress1(lines.get(0).getValue());
				}
				if (lines.size() > 1 && lines.get(1).getValue() != null) {
					pa.setAddress2(lines.get(1).getValue());
				}
				if (lines.size() > 2) {
					StringBuilder rest = new StringBuilder();
					for (int i = 2; i < lines.size(); i++) {
						if (lines.get(i).getValue() == null) {
							continue;
						}
						if (rest.length() > 0) {
							rest.append(' ');
						}
						rest.append(lines.get(i).getValue());
					}
					pa.setAddress3(rest.toString());
				}
			}
			pa.setCityVillage(fa.getCity());
			pa.setStateProvince(fa.getState());
			pa.setPostalCode(fa.getPostalCode());
			pa.setCountry(fa.getCountry());
		}
	}
	
	private Patient buildFhirPatientSnapshotFromOpenmrs(org.openmrs.Patient omrs) {
		if (omrs == null) {
			throw new IllegalArgumentException("OpenMRS patient is required");
		}
		Patient p = new Patient();
		p.setId(omrs.getUuid());
		PersonName preferredName = omrs.getPersonName();
		if (preferredName != null) {
			HumanName hn = new HumanName();
			hn.setFamily(preferredName.getFamilyName());
			if (StringUtils.isNotBlank(preferredName.getGivenName())) {
				hn.addGiven(preferredName.getGivenName());
			}
			if (StringUtils.isNotBlank(preferredName.getMiddleName())) {
				hn.addGiven(preferredName.getMiddleName());
			}
			p.addName(hn);
		}
		if (StringUtils.isNotBlank(omrs.getGender())) {
			if ("M".equalsIgnoreCase(omrs.getGender())) {
				p.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);
			} else if ("F".equalsIgnoreCase(omrs.getGender())) {
				p.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE);
			} else {
				p.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.UNKNOWN);
			}
		}
		if (omrs.getBirthdate() != null) {
			p.setBirthDate(omrs.getBirthdate());
		}
		PersonAddress oa = omrs.getPersonAddress();
		if (oa != null) {
			Address fa = new Address();
			if (StringUtils.isNotBlank(oa.getAddress1())) {
				fa.addLine(oa.getAddress1());
			}
			if (StringUtils.isNotBlank(oa.getAddress2())) {
				fa.addLine(oa.getAddress2());
			}
			if (StringUtils.isNotBlank(oa.getAddress3())) {
				fa.addLine(oa.getAddress3());
			}
			fa.setCity(oa.getCityVillage());
			fa.setState(oa.getStateProvince());
			fa.setPostalCode(oa.getPostalCode());
			fa.setCountry(oa.getCountry());
			p.addAddress(fa);
		}
		PatientIdentifierType preferredType = resolvePreferredIdentifierType();
		if (preferredType != null && omrs.getActiveIdentifiers() != null) {
			for (PatientIdentifier pid : omrs.getActiveIdentifiers()) {
				if (pid == null || pid.getIdentifierType() == null || pid.getIdentifierType().getUuid() == null
				        || !pid.getIdentifierType().getUuid().equals(preferredType.getUuid())
				        || StringUtils.isBlank(pid.getIdentifier())) {
					continue;
				}
				Identifier id = new Identifier();
				id.setSystem(OPENMRS_ID_SYSTEM);
				org.hl7.fhir.r4.model.CodeableConcept type = new org.hl7.fhir.r4.model.CodeableConcept();
				type.setText("OpenMRS ID");
				type.addCoding().setSystem(V2_IDENTIFIER_SYSTEM).setCode(MR_IDENTIFIER_CODE);
				id.setType(type);
				id.setValue(pid.getIdentifier());
				p.addIdentifier(id);
				break;
			}
		}
		return p;
	}
	
	private void validatePatientAgainstProfileBeforeCreate(Patient patient) {
		PatientFhirExchangeValidationResult r = patientFhirExchangeValidationService.validatePatient(fhirContext,
		    fhirConfig, patient, correlationIdForImportItem(patient));
		if (!r.isPermitted()) {
			throw new IllegalArgumentException(r.getFailureReason());
		}
	}
	
	private PersonAttributeType resolvePersonAttributeTypeForSuffix(String suffix) {
		return resolvePersonAttributeType(attributeTypeNameCandidatesForExtensionSuffix(suffix));
	}
	
	private List<String> attributeTypeNameCandidatesForExtensionSuffix(String suffix) {
		String defaultName = suffix == null ? "" : suffix.trim();
		String defaultSpaced = defaultName.replace('-', ' ').replace('_', ' ');
		String defaultUnderscore = defaultName.replace('-', '_').replace(' ', '_');
		switch (suffix) {
			case "Emergency-Contact-Number":
				// Extension is emergency contact phone — not the patient's primary Telephone Number (that comes from telecom).
				return Arrays.asList("Emergency Contact Number", "Emergency-Contact-Number");
			case "Emergency-Contact-Type":
				return Arrays.asList("Emergency Contact Type", "Emergency-Contact-Type");
			case "Caste":
				return Arrays.asList("Caste", "caste");
			case "Economic-Status":
				return Arrays.asList("Economic Status", "Economic-Status", "economic_status");
			case "Education-Level":
				return Arrays.asList("Education Level", "Education-Level", "education_level");
			case "occupation":
				return Arrays.asList("occupation", "Occupation", "Occupation Name");
			case "NationalID":
				return Arrays.asList("National ID", "NationalID", "National Id");
			case "Household-Number":
				return Arrays.asList("Household Number", "HouseholdNumber", "Household-Number");
			default:
				// Default behavior: if no static mapping, try same/similar names.
				return Arrays.asList(defaultName, defaultSpaced, defaultUnderscore);
		}
	}
	
	private List<String> attributeTypeNameCandidatesForTelecom(String logicalName) {
		if ("telephoneNumber".equals(logicalName)) {
			return Arrays.asList("Telephone Number", "Phone Number", "telephoneNumber", "phonenumber");
		}
		if ("emergencycontactnumber".equals(logicalName)) {
			return Arrays.asList("Emergency Contact Number", "Emergency-Contact-Number", "emergencycontactnumber");
		}
		return Arrays.asList(logicalName);
	}
	
	private PersonAttributeType resolvePersonAttributeType(List<String> candidates) {
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		for (String candidate : candidates) {
			PersonAttributeType type = Context.getPersonService().getPersonAttributeTypeByName(candidate);
			if (type != null && !type.getRetired()) {
				return type;
			}
		}
		List<PersonAttributeType> allTypes = Context.getPersonService().getAllPersonAttributeTypes();
		for (String candidate : candidates) {
			String normalizedCandidate = normalizeAttributeName(candidate);
			for (PersonAttributeType type : allTypes) {
				if (type == null || type.getRetired() || StringUtils.isBlank(type.getName())) {
					continue;
				}
				if (StringUtils.equals(normalizedCandidate, normalizeAttributeName(type.getName()))) {
					return type;
				}
			}
		}
		return null;
	}
	
	private static String normalizeAttributeName(String name) {
		if (name == null) {
			return "";
		}
		return name.trim().toLowerCase().replaceAll("[\\s_-]+", "");
	}
	
	private static String extensionStringValue(Extension extension) {
		if (extension == null) {
			return null;
		}
		Type value = extension.getValue();
		if (value == null) {
			return null;
		}
		String primitive = value.primitiveValue();
		return StringUtils.trimToNull(primitive);
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
		String inDob = formatBirthDateYyyyMmDd(incoming);
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
	
	private static String safePhone(Patient patient) {
		return StringUtils.defaultString(PatientTelecomMappingUtil.extractRankedPhoneTelecom(patient).getTelephoneNumber());
	}
	
	/**
	 * Stable key for {@code mpi_patient_duplicate_review_case.local_patient_uuid} before an OpenMRS
	 * patient row exists (import pending).
	 */
	private static String buildImportDuplicateCorrelationKey(Patient patient) {
		String suffix = StringUtils.trimToNull(correlationIdForImportItem(patient));
		String raw = "import-upload:" + StringUtils.defaultString(suffix, "unknown");
		return raw.length() > 128 ? raw.substring(0, 128) : raw;
	}
	
	/**
	 * FHIR logical id if present; otherwise preferred OpenMRS identifier value (upload JSON rarely
	 * sets {@code Patient.id}).
	 */
	private static String correlationIdForImportItem(Patient patient) {
		if (patient == null) {
			return null;
		}
		if (patient.getIdElement() != null && StringUtils.isNotBlank(patient.getIdElement().getIdPart())) {
			return patient.getIdElement().getIdPart().trim();
		}
		Identifier omrsId = findOpenMrsIdentifierFromJson(patient);
		if (omrsId != null && StringUtils.isNotBlank(omrsId.getValue())) {
			return omrsId.getValue().trim();
		}
		return null;
	}
	
	/**
	 * Uses FHIR date element string when present so duplicate SQL matches the JSON calendar day
	 * (avoids TZ rollback).
	 */
	private static String formatBirthDateYyyyMmDd(Patient patient) {
		if (patient == null || !patient.hasBirthDate()) {
			return "";
		}
		if (patient.hasBirthDateElement()) {
			String raw = patient.getBirthDateElement().asStringValue();
			if (StringUtils.isNotBlank(raw) && raw.length() >= 10) {
				return raw.substring(0, 10);
			}
		}
		return new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate());
	}
}
