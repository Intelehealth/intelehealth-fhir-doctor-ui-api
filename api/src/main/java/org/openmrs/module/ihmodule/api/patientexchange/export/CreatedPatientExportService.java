package org.openmrs.module.ihmodule.api.patientexchange.export;

import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.telecom.PatientTelecomMappingUtil;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PersonAttributeToExtensionSuffix;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.openmrs.module.ihmodule.utils.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CreatedPatientExportService extends IHConstant {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CreatedPatientExportService.class);
	
	private static final String OPENMRS_ID_TYPE_TEXT = "OpenMRS ID";
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private CreatedPatientUuidQueryService queryService;
	
	@Autowired
	private CommonOperationService commonOperationService;
	
	@Autowired
	private FhirConfig fhirConfig;
	
	@Value("${intelehealth.fhir.patient.export.created.parallelism:6}")
	private int exportParallelism;
	
	public CreatedPatientExportResult exportCreatedPatients(String startDate, String endDate)
	        throws UnsupportedEncodingException {
		List<String> uuids = queryService.findCreatedPatientUuids(startDate, endDate);
		if (uuids == null) {
			LOGGER.warn("Created export returned null patient UUID list for startDate={}, endDate={}; treating as empty",
			    startDate, endDate);
			uuids = Collections.emptyList();
		}
		Bundle outBundle = new Bundle();
		outBundle.setType(Bundle.BundleType.COLLECTION);
		List<ExportItem> items = exportPatientsInParallel(uuids);
		
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
	
	private List<ExportItem> exportPatientsInParallel(List<String> uuids) {
		if (uuids.isEmpty()) {
			return Collections.emptyList();
		}
		int poolSize = Math.max(1, Math.min(exportParallelism, uuids.size()));
		ExecutorService executor = Executors.newFixedThreadPool(poolSize);
		try {
			List<Callable<ExportItem>> tasks = uuids.stream()
					.map(uuid -> (Callable<ExportItem>) () -> exportOneUuid(uuid))
					.collect(Collectors.toList());
			List<Future<ExportItem>> futures = executor.invokeAll(tasks);
			List<ExportItem> results = new ArrayList<>(futures.size());
			for (Future<ExportItem> future : futures) {
				try {
					results.add(future.get());
				} catch (ExecutionException e) {
					Throwable cause = e.getCause();
					if (cause != null) {
						LOGGER.warn("Created export task failed: {}: {}", cause.getClass().getName(), cause.getMessage(), cause);
					} else {
						LOGGER.warn("Created export task failed: {}", e.getMessage(), e);
					}
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
	
	private ExportItem exportOneUuid(String uuid) {
		try {
			String data = fetchPatientBundleJson(uuid);
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
				// Export should be resilient in runtime environments where
				// custom validation supports are unavailable.
				return ExportItem.success(patient);
			}
			return ExportItem.none();
		}
		catch (Throwable e) {
			LOGGER.warn("Created export processing failed for uuid={} with {}: {}", uuid, e.getClass().getName(),
			    e.getMessage(), e);
			return ExportItem.processingFailure();
		}
	}
	
	private String fetchPatientBundleJson(String uuid) throws Exception {
		try {
			return HttpWebClient.get(localOpenmrsOpenhimURL, "/ws/fhir2/R4/Patient?_id=" + uuid, getOpenmrsUsername(),
			    getOpenmrsPassword());
		}
		catch (NoClassDefFoundError err) {
			if (isMissingReactiveStack(err)) {
				LOGGER.warn("WebClient/reactive dependency missing ({}); falling back to HttpURLConnection for uuid={}",
				    err.getMessage(), uuid);
				if (isUnresolvedPropertyPlaceholder(localOpenmrsOpenhimURL)) {
					LOGGER.warn(
					    "Property local.openmrs.openhim.url appears unresolved (value='{}'); using HAPI client fallback",
					    localOpenmrsOpenhimURL);
					Bundle bundle = fhirConfig.getLocalOpenMRSFhirContext().search().byUrl("Patient?_id=" + uuid)
					        .returnBundle(Bundle.class).execute();
					return fhirContext.newJsonParser().encodeResourceToString(bundle);
				}
				String api = localOpenmrsOpenhimURL + "/ws/fhir2/R4/Patient?_id=" + uuid;
				return new HttpService().getPatientData(api, "", getOpenmrsUsername(), getOpenmrsPassword());
			}
			throw err;
		}
	}
	
	private boolean isMissingReactiveStack(NoClassDefFoundError err) {
		String message = err.getMessage();
		if (message == null) {
			return false;
		}
		return message.contains("org/reactivestreams/") || message.contains("org/springframework/web/reactive/");
	}
	
	private boolean isUnresolvedPropertyPlaceholder(String value) {
		return value != null && value.startsWith("${") && value.endsWith("}");
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
		PatientTelecomMappingUtil.applyRankedPhoneTelecom(patient, attributes);
		List<Extension> extensionList = new ArrayList<Extension>();
		for (PersonAttribute attribute : attributes) {
			String suffix = PersonAttributeToExtensionSuffix.map(attribute.getName());
			if (suffix == null) {
				continue;
			}
			if (isIgnorablePersonAttributeValue(attribute.getValue())) {
				continue;
			}
			LOGGER.info("Export extension mapping resolved: person_attribute='{}' -> suffix={} value='{}'",
					attribute.getName(), suffix, attribute.getValue());
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
		return StringUtils.isNotBlank(suffix);
	}
	
	private void ensureIdentifierSystem(Identifier identifier) {
		String typeText = identifier.hasType() ? identifier.getType().getText() : null;
		if (typeText != null && typeText.equalsIgnoreCase(OPENMRS_ID_TYPE_TEXT)) {
			identifier.setSystem(centralFhirURL + "/StructureDefinition/OpenMRS-ID");
			return;
		}
		if (typeText != null
		        && (typeText.equalsIgnoreCase(globalIdentifierName) || typeText.equalsIgnoreCase(MPI_TYPE_TEXT))) {
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
	
	private boolean isIgnorablePersonAttributeValue(String value) {
		if (value == null) {
			return true;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty();
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
