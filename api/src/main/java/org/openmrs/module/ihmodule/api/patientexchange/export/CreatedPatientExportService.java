package org.openmrs.module.ihmodule.api.patientexchange.export;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;

@Service
public class CreatedPatientExportService extends IHConstant {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreatedPatientExportService.class);
	private static final String OPENMRS_ID_TYPE_TEXT = "OpenMRS ID";
	private static final String MPI_TYPE_TEXT = "MPI";
	private static final Set<String> ALLOWED_PATIENT_EXTENSION_URLS = new HashSet<>(Arrays.asList(
			"Economic-Status",
			"Education-Level",
			"NationalID",
			"occupation",
			"Emergency-Contact-Number",
			"Household-Number",
			"Caste"));

	private final FhirContext fhirContext = FhirContext.forR4();

	private CreatedPatientUuidQueryService queryService = Context.getRegisteredComponent("createdPatientUuidQueryService",
		CreatedPatientUuidQueryService.class);

	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
		CommonOperationService.class);

	@Value("${intelehealth.fhir.patient.export.created.parallelism:6}")
	private int exportParallelism;

	public CreatedPatientExportResult exportCreatedPatients(String startDate, String endDate)
			throws UnsupportedEncodingException, ConfigurationException, DataFormatException, IOException {
		List<String> uuids = queryService.findCreatedPatientUuids(startDate, endDate);
		Bundle outBundle = new Bundle();
		outBundle.setType(Bundle.BundleType.COLLECTION);

		ValidationRuntime runtime = buildValidationRuntime();
		List<ExportItem> items = exportPatientsInParallel(uuids, runtime);

		int exported = 0;
		int failed = 0;
		for (ExportItem item : items) {
			if (item.getPatient() != null) {
				outBundle.addEntry().setResource(item.getPatient());
				exported++;
			}
			if (item.isValidationFailed()) {
				failed++;
			}
		}

		CreatedPatientExportResult result = new CreatedPatientExportResult();
		result.setPayload(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(outBundle));
		result.setTotalPatients(uuids.size());
		result.setExportedPatients(exported);
		result.setValidationFailedPatients(failed);
		return result;
	}

	private List<ExportItem> exportPatientsInParallel(List<String> uuids, ValidationRuntime runtime) {
		if (uuids.isEmpty()) {
			return Collections.emptyList();
		}
		int poolSize = Math.max(1, Math.min(exportParallelism, uuids.size()));
		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		try {
			List<Callable<ExportItem>> tasks = uuids.stream()
					.map(uuid -> (Callable<ExportItem>) () -> exportOneUuid(uuid, runtime))
					.collect(Collectors.toList());
			List<Future<ExportItem>> futures = executor.invokeAll(tasks);
			List<ExportItem> results = new ArrayList<>(futures.size());
			for (Future<ExportItem> future : futures) {
				try {
					results.add(future.get());
				} catch (ExecutionException e) {
					LOGGER.warn("Created export task failed: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
					results.add(ExportItem.processingFailure());
				}
			}
			return results;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Created export interrupted: {}", e.getMessage());
			return Collections.emptyList();
		} finally {
			executor.shutdown();
		}
	}

	private ExportItem exportOneUuid(String uuid, ValidationRuntime runtime) {
		try {
			String data = HttpWebClient.get(localOpenmrsOpenhimURL, "/ws/fhir2/R4/Patient?_id=" + uuid,
					getOpenmrsUsername(), getOpenmrsPassword());
			Bundle localBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
			if (!localBundle.hasEntry()) {
				return ExportItem.none();
			}
			for (BundleEntryComponent entry : localBundle.getEntry()) {
				Resource resource = (Resource) entry.getResource();
				if (!(resource instanceof Patient)) {
					continue;
				}
				Patient patient = (Patient) resource;
				String patientUUID = patient.getIdElement().getIdPart();
				applyPatientMetaSource(patient);
				addExtension(patient, patientUUID);
				normalizePatientForIgValidation(patient);
				ValidationResult validation = validateResource(patient, patientUUID, runtime);
				if (!validation.isSuccessful()) {
					return ExportItem.validationFailure();
				}
				return ExportItem.success(patient);
			}
			return ExportItem.none();
		} catch (Exception e) {
			LOGGER.warn("Created export processing failed for uuid={}: {}", uuid, e.getMessage());
			return ExportItem.processingFailure();
		}
	}

	private String getOpenmrsUsername() {
		return localOpenmrsOpenhimAuthentication.split(":")[0];
	}

	private String getOpenmrsPassword() {
		String[] credentials = localOpenmrsOpenhimAuthentication.split(":", 2);
		return credentials.length > 1 ? credentials[1] : "";
	}

	private void applyPatientMetaSource(Patient patient) {
		if (!patient.hasMeta()) {
			patient.setMeta(new Meta());
		}
		patient.getMeta().setSource("intelehealth");
		if (!patient.getMeta().getProfile().stream().anyMatch(p -> patientProfileUrl.equals(p.getValueAsString()))) {
			patient.getMeta().addProfile(patientProfileUrl);
		}
	}

	private Patient addExtension(Patient patient, String patientUUID) {
		List<PersonAttribute> attributes = commonOperationService.findPersonAttributes(patientUUID);
		List<Extension> extensionList = new ArrayList<Extension>();
		for (PersonAttribute attribute : attributes) {
			String suffix = mapPersonAttributeToExtensionSuffix(attribute.getName());
			if (suffix == null || !ALLOWED_PATIENT_EXTENSION_URLS.contains(suffix)) {
				continue;
			}
			if (isIgnorablePersonAttributeValue(attribute.getValue())) {
				continue;
			}
			Extension extension = new Extension();
			String url = centralFhirURL + "/StructureDefinition/" + suffix;
			extension.setUrl(url);
			extension.setValue(new StringType(attribute.getValue()));
			extensionList.add(extension);
		}
		patient.getExtension().addAll(extensionList);

		for (Address address : patient.getAddress()) {
			List<StringType> newLines = new ArrayList<>();
			for (Extension ext : address.getExtension()) {
				for (Extension nested : ext.getExtension()) {
					if (nested.getValue() instanceof StringType) {
						newLines.add((StringType) nested.getValue());
					} else if (nested.getValue() != null) {
						newLines.add(new StringType(nested.getValue().toString()));
					}
				}
			}
			address.getLine().clear();
			address.getLine().addAll(newLines);
			address.getExtension().clear();
		}

		if (patient.getTelecom().size() > 0) {
			ContactPoint contact = patient.getTelecom().get(0);
			contact.setSystem(ContactPointSystem.PHONE);
			patient.getTelecom().set(0, contact);
		}
		return patient;
	}

	private void normalizePatientForIgValidation(Patient patient) {
		patient.setLanguage(null);
		patient.setText(null);
		patient.getContained().clear();
		patient.setExtension(patient.getExtension().stream()
				.filter(ext -> isAllowedPatientExtensionUrl(ext.getUrl()))
				.collect(Collectors.toList()));

		for (Identifier identifier : patient.getIdentifier()) {
			identifier.setExtension(new ArrayList<>());
			ensureIdentifierSystem(identifier);
			ensureIdentifierTypeCoding(identifier);
		}
	}

	private boolean isAllowedPatientExtensionUrl(String url) {
		if (url == null) {
			return false;
		}
		int lastSlash = url.lastIndexOf('/');
		String suffix = lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
		return ALLOWED_PATIENT_EXTENSION_URLS.contains(suffix);
	}

	private void ensureIdentifierSystem(Identifier identifier) {
		String typeText = identifier.hasType() ? identifier.getType().getText() : null;
		if (typeText != null && typeText.equalsIgnoreCase(OPENMRS_ID_TYPE_TEXT)) {
			identifier.setSystem(centralFhirURL + "/StructureDefinition/OpenMRS-ID");
			return;
		}
		if (typeText != null && (typeText.equalsIgnoreCase(globalIdentifierName) || typeText.equalsIgnoreCase(MPI_TYPE_TEXT))) {
			identifier.setSystem(centralFhirURL + "/StructureDefinition/MPI");
			return;
		}
		if (!identifier.hasSystem()) {
			identifier.setSystem("urn:ietf:rfc:3986");
		}
	}

	private void ensureIdentifierTypeCoding(Identifier identifier) {
		CodeableConcept type = identifier.getType();
		CodeableConcept ensuredType = type != null ? type : new CodeableConcept();
		ensuredType.setCoding(new ArrayList<>());
		Coding coding = new Coding();
		coding.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203");
		coding.setCode("MR");
		ensuredType.addCoding(coding);
		identifier.setType(ensuredType);
	}

	private String mapPersonAttributeToExtensionSuffix(String attributeName) {
		if (attributeName == null) {
			return null;
		}
		String normalized = attributeName.trim().toLowerCase().replaceAll("[\\s_-]+", "");
		switch (normalized) {
		case "telephonenumber":
			return "Emergency-Contact-Number";
		case "caste":
			return "Caste";
		case "economicstatus":
			return "Economic-Status";
		case "educationlevel":
			return "Education-Level";
		case "occupation":
			return "occupation";
		case "nationalid":
			return "NationalID";
		case "householdnumber":
			return "Household-Number";
		default:
			return null;
		}
	}

	private boolean isIgnorablePersonAttributeValue(String value) {
		if (value == null) {
			return true;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() || "not provided".equalsIgnoreCase(trimmed);
	}

	private ValidationResult validateResource(Patient patient, String patientUuid, ValidationRuntime runtime) {
		ValidationResult result = runtime.getValidator().validateWithResult(patient, runtime.getOptions());
		if (!result.isSuccessful()) {
			LOGGER.warn("Validation failed during created export for patient uuid={}", patientUuid);
			result.getMessages().forEach(msg -> LOGGER.warn(" - {}: {}", msg.getSeverity(), msg.getMessage()));
		}
		return result;
	}

	private ValidationRuntime buildValidationRuntime() throws ConfigurationException, DataFormatException, IOException {
		FhirValidator validator = fhirContext.newValidator();
		ValidationSupportChain validationSupport = new ValidationSupportChain();
		DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(fhirContext);
		InMemoryTerminologyServerValidationSupport inMemSupport = new InMemoryTerminologyServerValidationSupport(fhirContext);
		SnapshotGeneratingValidationSupport snapshotSupport = new SnapshotGeneratingValidationSupport(fhirContext);

		validationSupport.addValidationSupport(inMemSupport);
		validationSupport.addValidationSupport(defaultSupport);
		validationSupport.addValidationSupport(getCustomSupport());
		validationSupport.addValidationSupport(snapshotSupport);

		FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
		validator.registerValidatorModule(instanceValidator);

		ValidationOptions options = new ValidationOptions();
		options.addProfile(patientProfileUrl);
		return new ValidationRuntime(validator, options);
	}

	private PrePopulatedValidationSupport getCustomSupport()
			throws ConfigurationException, DataFormatException, IOException {
		PrePopulatedValidationSupport customSupport = new PrePopulatedValidationSupport(fhirContext);
		loadStructureDefinitions(customSupport, "structureDefinition/structureDefinition.json");
		loadStructureDefinitions(customSupport, "structureDefinition/StructureDefinition-Emergency-Contact-Number.json");
		loadStructureDefinitions(customSupport, "structureDefinition/StructureDefinition-Household-Number.json");
		loadStructureDefinitions(customSupport, patientProfileDefinitionPath);
		return customSupport;
	}

	private void loadStructureDefinitions(PrePopulatedValidationSupport customSupport, String classpathFile)
			throws IOException {
		ClassPathResource definitionResource = new ClassPathResource(classpathFile);
		IBaseResource parsed = fhirContext.newJsonParser()
				.parseResource(new InputStreamReader(definitionResource.getInputStream()));
		if (parsed instanceof Bundle) {
			Bundle bundle = (Bundle) parsed;
			for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				IBaseResource resource = entry.getResource();
				if (resource instanceof StructureDefinition) {
					customSupport.addStructureDefinition((StructureDefinition) resource);
				}
			}
			return;
		}
		if (parsed instanceof StructureDefinition) {
			customSupport.addStructureDefinition((StructureDefinition) parsed);
		}
	}

	private static final class ValidationRuntime {
		private final FhirValidator validator;
		private final ValidationOptions options;

		private ValidationRuntime(FhirValidator validator, ValidationOptions options) {
			this.validator = validator;
			this.options = options;
		}

		private FhirValidator getValidator() {
			return validator;
		}

		private ValidationOptions getOptions() {
			return options;
		}
	}

	private static final class ExportItem {
		private final Patient patient;
		private final boolean validationFailed;

		private ExportItem(Patient patient, boolean validationFailed) {
			this.patient = patient;
			this.validationFailed = validationFailed;
		}

		private static ExportItem success(Patient patient) {
			return new ExportItem(patient, false);
		}

		private static ExportItem validationFailure() {
			return new ExportItem(null, true);
		}

		private static ExportItem processingFailure() {
			return new ExportItem(null, false);
		}

		private static ExportItem none() {
			return new ExportItem(null, false);
		}

		private Patient getPatient() {
			return patient;
		}

		private boolean isValidationFailed() {
			return validationFailed;
		}
	}
}
