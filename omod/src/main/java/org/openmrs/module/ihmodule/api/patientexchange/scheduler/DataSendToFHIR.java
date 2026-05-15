package org.openmrs.module.ihmodule.api.patientexchange.scheduler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.datatype.ConfigFacilityDataType;
import org.openmrs.module.ihmodule.api.patientexchange.domain.ConfigDataSync;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PatientDTO;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.model.DataExchangeAuditLog;
import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.CentralPatientDuplicateMatcher;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.ForceSyncDuplicateResolutionContext;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewResolutionService;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCase;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.service.ConfigDataSyncService;
import org.openmrs.module.ihmodule.api.patientexchange.service.DataExchangeAuditLogService;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.OpenmrsPatientUpsertResult;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportService;
import org.openmrs.module.ihmodule.api.patientexchange.service.LocalPatientMpiUpdateService;
import org.openmrs.module.ihmodule.api.patientexchange.service.PatientDataService;
import org.openmrs.module.ihmodule.api.patientexchange.telecom.PatientTelecomMappingUtil;
import org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PatientFhirExchangeValidationService;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PatientProfileExtensionRules;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PersonAttributeToExtensionSuffix;
import org.openmrs.module.ihmodule.api.patientexchange.validationrecord.ValidationRecordContext;
import org.openmrs.module.ihmodule.api.patientexchange.validationrecord.ValidationOutcome;
import org.openmrs.module.ihmodule.api.patientexchange.validationrecord.FhirResourceValidationRecordService;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;

@Component
public class DataSendToFHIR extends IHConstant {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSendToFHIR.class);
	
	private static final String OPENMRS_ID_TYPE_TEXT = "OpenMRS ID";
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	private static final String HAPI_MDM_GOLDEN_ENTERPRISE_ID_SYSTEM = "http://hapifhir.io/fhir/NamingSystem/mdm-golden-resource-enterprise-id";
	
	/**
	 * Mediator minimal create response may include this system (see FHIR_CRUID_SYSTEM in mediator).
	 */
	private static final String CRUID_IDENTIFIER_SYSTEM = "urn:intelehealth:cruid";
	
	/**
	 * OpenMRS patient identifier type "Source Patient Id" (UUID); used as FHIR identifier.system
	 * {@code urn:uuid:...}.
	 */
	private static final String SOURCE_PATIENT_ID_TYPE_TEXT = "Source Patient Id";
	
	private static final String SOURCE_PATIENT_ID_SYSTEM = "urn:uuid:b2f192c2-346a-486c-bcb4-7a35616890ba";
	
	private static final String V2_0203_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
	
	private static final String OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL = "http://fhir.openmrs.org/ext/patient/identifier#location";
	
	private static final String OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	FhirContext fhirContext = FhirContextHolder.R4;
	
	private FhirConfig firFhirConfig;
	
	private IHMarkerService ihMarkerService;
	
	private CommonOperationService commonOperationService;
	
	private ConfigDataSyncService configDataSyncService;
	
	private PatientDataService patientService;
	
	private DataExchangeAuditLogService dataExchangeService;
	
	private FhirResourceValidationRecordService validationRecordService;
	
	private CentralPatientDuplicateMatcher centralPatientDuplicateMatcher;
	
	private MpiDuplicateReviewResolutionService mpiDuplicateReviewResolutionService;
	
	private LocalPatientMpiUpdateService localPatientMpiUpdateService;
	
	private PatientFhirExchangeValidationService patientFhirExchangeValidationService;
	
	private PatientUploadImportService patientUploadImportService;
	
	private void ensureDependencies() {
		if (firFhirConfig == null) {
			firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
		}
		if (ihMarkerService == null) {
			ihMarkerService = Context.getRegisteredComponent("IHMarkerService", IHMarkerService.class);
		}
		if (commonOperationService == null) {
			commonOperationService = Context.getRegisteredComponent("commonOperationService", CommonOperationService.class);
		}
		if (configDataSyncService == null) {
			configDataSyncService = Context.getRegisteredComponent("configDataSyncService", ConfigDataSyncService.class);
		}
		if (patientService == null) {
			patientService = Context.getRegisteredComponent("patientDataService", PatientDataService.class);
		}
		if (dataExchangeService == null) {
			dataExchangeService = Context.getRegisteredComponent("dataExchangeAuditLogService",
			    DataExchangeAuditLogService.class);
		}
		if (validationRecordService == null) {
			validationRecordService = Context.getRegisteredComponent("fhirResourceValidationRecordService",
			    FhirResourceValidationRecordService.class);
		}
		if (centralPatientDuplicateMatcher == null) {
			centralPatientDuplicateMatcher = Context.getRegisteredComponent("centralPatientDuplicateMatcher",
			    CentralPatientDuplicateMatcher.class);
		}
		if (mpiDuplicateReviewResolutionService == null) {
			mpiDuplicateReviewResolutionService = Context.getRegisteredComponent("mpiDuplicateReviewResolutionService",
			    MpiDuplicateReviewResolutionService.class);
		}
		if (localPatientMpiUpdateService == null) {
			localPatientMpiUpdateService = Context.getRegisteredComponent("localPatientMpiUpdateService",
			    LocalPatientMpiUpdateService.class);
		}
		if (patientFhirExchangeValidationService == null) {
			patientFhirExchangeValidationService = Context.getRegisteredComponent("patientFhirExchangeValidationService",
			    PatientFhirExchangeValidationService.class);
		}
		if (patientUploadImportService == null) {
			patientUploadImportService = Context.getRegisteredComponent("patientUploadImportService",
			    PatientUploadImportService.class);
		}
	}
	
	public void scheduleTaskUsingCronExpression() throws ParseException, UnsupportedEncodingException, DataFormatException,
	        JsonProcessingException, JSONException {
		ensureDependencies();
		
		transferCreatedPatient();
		
		//transferModifiedPatient();
	}
	
	public void transferCreatedPatient() {
		ensureDependencies();
		ConfigDataSync patientSync = configDataSyncService.getConfigDataSync(ConfigFacilityDataType.PATIENTS);

		if (patientSync.getStatus()) {

			IHMarker patientMarker = ihMarkerService.findByName(exportCreatedPatient);

			List<PatientDTO> patientList = patientService.getCreatedPatients(patientMarker.getLastSyncTime());

			HashSet<String> patientIdList = new HashSet<>(
					patientList.stream().map(p -> p.getUuid()).collect(Collectors.toSet()));

			System.err.println("Total Patient Found: " + patientIdList.size());

			int patientSendingError = 0;

			for (String patient : patientIdList) {
				try {
					FhirResponse result = send("Patient", patient);
					if (!isSuccessfulStatus(result != null ? result.getStatusCode() : null)) {
						patientSendingError++;
					}
				} catch (Exception e) {
					System.err.println(e);
					patientSendingError++;
				}
			}

			System.err.format("Total patient found: %d, Successfully Send %d, Error %d\n", patientIdList.size(),
					patientIdList.size() - patientSendingError, patientSendingError);

			if (patientIdList.size() > 0) {
				ihMarkerService.updateMarkerByName(exportCreatedPatient);
			}

			System.err.println("Patient Data Sync Done............");
		} else {
			System.err.println("Patient data sending is disabled ............");
		}
	}
	
	private void transferModifiedPatient() {
		ensureDependencies();
		ConfigDataSync patientSync = configDataSyncService.getConfigDataSync(ConfigFacilityDataType.PATIENTS);

		if (patientSync.getStatus()) {

			IHMarker patientMarker = ihMarkerService.findByName(exportModifiedPatient);

			List<PatientDTO> patientList = patientService.getModifiedPatients(patientMarker.getLastSyncTime());

			HashSet<String> patientIdList = new HashSet<>(
					patientList.stream().map(p -> p.getUuid()).collect(Collectors.toSet()));

			System.err.println("Total Patient Found: " + patientIdList.size());

			int patientSendingError = 0;

			for (String patient : patientIdList) {
				try {
					FhirResponse result = send("Patient", patient);
					if (!isSuccessfulStatus(result != null ? result.getStatusCode() : null)) {
						patientSendingError++;
					}
				} catch (Exception e) {
					System.err.println(e);
					patientSendingError++;
				}
			}

			System.err.format("Total patient found: %d, Successfully Send %d, Error %d\n", patientIdList.size(),
					patientIdList.size() - patientSendingError, patientSendingError);

			if (patientIdList.size() > 0) {
				ihMarkerService.updateMarkerByName(exportModifiedPatient);
			}

			System.err.println("Patient Data Sync Done............");
		} else {
			System.err.println("Patient data sending is disabled ............");
		}
	}
	
	public FhirResponse send(String resourceType, String uuid) throws ParseException, DataFormatException, JSONException,
	        ConfigurationException, IOException {
		ensureDependencies();
		System.err.println("resourceType => " + resourceType + " => " + uuid);
		Bundle localBundle = firFhirConfig.getLocalOpenMRSFhirContext().search().byUrl(resourceType + "?_id=" + uuid)
		        .returnBundle(Bundle.class).execute();
		String data = fhirContext.newJsonParser().encodeResourceToString(localBundle);
		
		System.err.println("Local Fhir Bundle => " + data);
		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
		
		return sendFHIRBundle(theBundle, resourceType, false);
	}
	
	/**
	 * Operator add-patient / legacy force-sync: upsert into the <strong>local</strong> OpenMRS DB
	 * only (no central FHIR write). Loads the existing row by UUID, then create-or-update via
	 * identifier rules in {@link PatientUploadImportService}.
	 */
	public FhirResponse forceSendPatientToCentralByUuid(String patientUuid) throws ParseException, DataFormatException,
	        JSONException, ConfigurationException, IOException {
		ensureDependencies();
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		String uuid = patientUuid.trim();
		LOGGER.info("Local OpenMRS upsert for patient uuid={} (no central FHIR sync)", uuid);
		OpenmrsPatientUpsertResult result = patientUploadImportService.forceSyncLocalOpenmrsPatientByUuid(uuid);
		FhirResponse response = new FhirResponse();
		response.setStatusCode("200");
		response.setMessage(result.getMessage());
		response.setResponse(result.getPatientUuid());
		return response;
	}
	
	public FhirResponse sendFHIRBundle(Bundle localTaskBundle, String resourceType) throws ParseException,
	        DataFormatException, JSONException, ConfigurationException, IOException {
		return sendFHIRBundle(localTaskBundle, resourceType, false);
	}
	
	/**
	 * @param skipCentralMpiDuplicateSearch {@code true} for operator force-sync: skips OpenCR
	 *            demographic search, duplicate-review persistence, and 409 deferral.
	 */
	public FhirResponse sendFHIRBundle(Bundle localTaskBundle, String resourceType, boolean skipCentralMpiDuplicateSearch)
	        throws ParseException, DataFormatException, JSONException, ConfigurationException, IOException {
		ensureDependencies();
		
		String localPatientUUID = null;
		
		if (localTaskBundle.hasEntry()) {
			
			Bundle transactionBundle = new Bundle();
			
			Patient localPatient = null;
			transactionBundle.setType(Bundle.BundleType.TRANSACTION);
			for (BundleEntryComponent bundleEntry : localTaskBundle.getEntry()) {
				
				localPatient = (Patient) bundleEntry.getResource();
				localPatientUUID = localPatient.getIdElement().getIdPart();
				applyPatientMetaSource(localPatient);
				addExtension(localPatient, localPatientUUID);
				normalizePatientForIgValidation(localPatient);
				String patientPayloadBeforeValidation = fhirContext.newJsonParser().setPrettyPrint(true)
				        .encodeResourceToString(localPatient);
				ValidationRecordContext.setPayloadJson(patientPayloadBeforeValidation);
				ValidationRecordContext.setFailureReason(null);
				//System.err.println("Patient payload before validation: " + patientPayloadBeforeValidation);
				boolean isValid = patientFhirExchangeValidationService.validatePatient(fhirContext, firFhirConfig,
				    localPatient, localPatientUUID).isPermitted();
				if (!isValid) {
					LOGGER.error("VALIDATION STATUS: INVALID for patient uuid={}", localPatientUUID);
					String reason = ValidationRecordContext.getFailureReason();
					validationRecordService.recordValues(resourceType, localPatientUUID,
					    ValidationOutcome.VALIDATION_FAILED, reason, patientPayloadBeforeValidation);
					throw new ResourceIsNotValid("Patient fhir resource is not valid"
					        + (reason != null ? (": " + reason) : ""));
				}
				LOGGER.info("VALIDATION STATUS: VALID for patient uuid={}", localPatientUUID);
				Bundle.BundleEntryComponent component = transactionBundle.addEntry();
				component.setResource(localPatient);
				
			}
			
			String auditRequestPayload = usesPlainPatientBodyOpenhimChannel() ? fhirContext.newJsonParser()
			        .setPrettyPrint(true).encodeResourceToString(localPatient) : fhirContext.newJsonParser()
			        .setPrettyPrint(true).encodeResourceToString(transactionBundle);
			
			Optional<MpiPatientDuplicateReviewCase> duplicateReview = Optional.empty();
			/*
			 * if (!skipCentralMpiDuplicateSearch) { duplicateReview =
			 * centralPatientDuplicateMatcher.persistIfCentralSearchHasMultipleMatches(
			 * localPatient, localPatientUUID, payload); if (!hasMPI(localPatient) &&
			 * duplicateReview.isPresent()) { MpiPatientDuplicateReviewCase reviewCase =
			 * duplicateReview.get(); LOGGER.warn("Skipping MCI save for patient uuid=" +
			 * localPatientUUID + ": " + reviewCase.getCandidateCount() +
			 * " duplicate MPI candidates stored for manual review (case_uuid=" +
			 * reviewCase.getCaseUuid() + ")"); DataExchangeAuditLog deferLog = new
			 * DataExchangeAuditLog(); deferLog.setResourceName(resourceType);
			 * deferLog.setResourceUuid(localPatientUUID); deferLog.setRequest(payload);
			 * deferLog.setRequestUrl(mciURL + "rest/v1/patient/save");
			 * deferLog.setResponse("Deferred pending duplicate MPI review: case_uuid=" +
			 * reviewCase.getCaseUuid()); deferLog.setResponseStatus("409");
			 * deferLog.setStatus(false); try { DataExchangeAuditLog savedDeferLog =
			 * dataExchangeService.save(deferLog); savedDeferLog.setChangedBy(1);
			 * savedDeferLog.setDateChanged(DateUtils.toFormattedDateNow());
			 * dataExchangeService.update(savedDeferLog); } catch (Exception ex) {
			 * LOGGER.error( "Unable to persist deferred audit log for patient uuid=" +
			 * localPatientUUID + ": " + ex.getMessage(), ex); } FhirResponse deferResponse
			 * = new FhirResponse(); deferResponse.setStatusCode("409");
			 * deferResponse.setResponse(deferLog.getResponse()); return deferResponse; }
			 * 
			 * if (duplicateReview.isPresent()) { LOGGER.info(
			 * "MPI duplicate review case {} present for patient uuid={}; continuing MCI sync because patient already has MPI"
			 * , duplicateReview.get().getCaseUuid(), localPatientUUID); } } else {
			 * LOGGER.info(
			 * "Central MPI duplicate search skipped for patient uuid={} (operator force-sync); proceeding to central FHIR"
			 * , localPatientUUID); }
			 */
			
			DataExchangeAuditLog log = new DataExchangeAuditLog();
			log.setResourceName(resourceType);
			log.setResourceUuid(localPatientUUID);
			log.setRequest(auditRequestPayload);
			log.setRequestUrl(firFhirConfig.getResolvedOpenCrOpenhimUrl());
			
			DataExchangeAuditLog uLog = null;
			try {
				uLog = dataExchangeService.save(log);
			}
			catch (Exception ex) {
				LOGGER.error(
				    "Unable to persist outbound audit log for patient uuid=" + localPatientUUID + ": " + ex.getMessage(), ex);
			}
			
			//System.err.println("Final Patient payload before sending to FHIR server: " + payload);
			/*
			 * LOGGER.info(
			 * "Sending {} to FHIR server (POST {}), patient uuid={}, JSON payload:\n{}",
			 * resourceType, mciURL + "rest/v1/patient/save", localPatientUUID, payload);
			 */
			
			/*
			 * FhirResponse res = HttpWebClient.postWithBasicAuth(shrUrl,
			 * "rest/v1/patient/save", firFhirConfig.getOpenMRSCredentials()[0],
			 * firFhirConfig.getOpenMRSCredentials()[1], payload);
			 */
			FhirResponse res = sendPatientToCentral(localPatient);
			LOGGER.error("res MDM  uuid={}", res);
			if (uLog != null) {
				uLog.setResponse(res.getResponse());
				uLog.setResponseStatus(res.getStatusCode());
			}
			if (isSuccessfulStatus(res.getStatusCode())) {
				Patient remotePatient = parseCentralResponsePatient(res.getResponse());
				if (remotePatient == null) {
					throw new IllegalStateException("Central FHIR response did not contain a Patient resource");
				}
				if (trimToNull(res.getCentralServerPatientLogicalId()) == null) {
					String idFromBody = extractCentralServerPatientLogicalIdFromPatient(remotePatient);
					if (idFromBody != null) {
						res.setCentralServerPatientLogicalId(idFromBody);
					}
				}
				System.err.println("Response from central fhir: " + res.getResponse());
				String returnedMpi = extractMpiIdentifierValueForLocalOpenMrsPatient(remotePatient);
				if (returnedMpi == null || returnedMpi.trim().isEmpty()) {
					throw new IllegalStateException("Central FHIR response did not contain an MPI identifier value");
				}
				localPatientMpiUpdateService.upsertMpiIdentifierToLocalPatient(localPatientUUID, returnedMpi.trim());
				LOGGER.info("Updated local MPI identifier for patient uuid={} from central response", localPatientUUID);
				String mpiTrim = returnedMpi.trim();
				String centralSourcePatientId = trimToNull(res.getCentralServerPatientLogicalId());
				if (centralSourcePatientId != null && centralSourcePatientId.equals(mpiTrim)) {
					centralSourcePatientId = null;
				}
				if (centralSourcePatientId == null) {
					centralSourcePatientId = extractOpenMrsSourcePatientIdFromFhirPatient(remotePatient, mpiTrim);
				}
				if (centralSourcePatientId == null && remotePatient.getIdElement() != null
				        && remotePatient.getIdElement().hasIdPart()) {
					String idPart = trimToNull(remotePatient.getIdElement().getIdPart());
					if (idPart != null && !idPart.equals(mpiTrim)) {
						centralSourcePatientId = idPart;
					}
				}
				if (centralSourcePatientId != null) {
					localPatientMpiUpdateService.upsertCentralSourcePatientIdToLocalPatient(localPatientUUID,
					    centralSourcePatientId);
					LOGGER.info("Updated local Source Patient Id for patient uuid={} centralLogicalId={}", localPatientUUID,
					    centralSourcePatientId);
				}
				if (uLog != null && centralSourcePatientId != null) {
					uLog.setFhirId(centralSourcePatientId);
				} else if (uLog != null && remotePatient.getIdElement() != null && remotePatient.getIdElement().hasIdPart()) {
					uLog.setFhirId(remotePatient.getIdElement().getIdPart());
				}
				if (skipCentralMpiDuplicateSearch) {
					String resolvedBy = ForceSyncDuplicateResolutionContext.peekResolvedBy();
					if (resolvedBy != null) {
						try {
							mpiDuplicateReviewResolutionService.resolvePendingCaseAfterSuccessfulForceSync(localPatientUUID,
							    singlePatientBundle(remotePatient), resolvedBy);
						}
						catch (RuntimeException ex) {
							LOGGER.warn("Duplicate-review resolution after force-sync failed for patient "
							        + localPatientUUID + ": " + ex.getMessage(), ex);
						}
					}
				}
			} else {
				if (uLog != null) {
					uLog.setStatus(false);
				}
			}
			if (uLog != null) {
				uLog.setChangedBy(1); // Admin-OpenMRS
				uLog.setDateChanged(DateUtils.toFormattedDateNow());
				try {
					dataExchangeService.update(uLog);
				}
				catch (Exception ex) {
					LOGGER.error(
					    "Unable to update outbound audit log for patient uuid=" + localPatientUUID + ": " + ex.getMessage(),
					    ex);
				}
			}
			return res;
			
		}
		return null;
	}
	
	/**
	 * Exposes success check for central write responses (HTTP status string starting with {@code 2}
	 * ).
	 */
	public boolean isSuccessfulFhirWrite(FhirResponse res) {
		return res != null && isSuccessfulStatus(res.getStatusCode());
	}
	
	/**
	 * Sends one FHIR Patient through the same central pipeline as {@link #sendFHIRBundle}, with
	 * {@code skipCentralMpiDuplicateSearch=true}. Does not alter {@link #sendPatientToCentral}
	 * implementation.
	 */
	public FhirResponse sendSinglePatientTransactionToCentralSkipMpiDuplicateSearch(Patient patient) throws ParseException,
	        DataFormatException, JSONException, ConfigurationException, IOException {
		if (patient == null) {
			throw new IllegalArgumentException("Patient is required");
		}
		Bundle b = new Bundle();
		b.setType(Bundle.BundleType.TRANSACTION);
		BundleEntryComponent e = b.addEntry();
		e.setResource(patient);
		return sendFHIRBundle(b, "Patient", true);
	}
	
	private void syncPatientToLocal(Bundle remotePatientBundle, Patient localPatient) throws JsonProcessingException,
	        UnsupportedEncodingException, JSONException, ParseException {
		
		if (!hasMPI(localPatient)) {
			// Copy all the remote bundle identifier in locally,
			// to handle data loss when update operation will happend
			List<Identifier> identifiers = getIdentifiers(remotePatientBundle);
			
			for (Identifier identifier : identifiers) {
				
				// ignore local identifier copy to local patient from remote bundle
				if (matchWithLocalIdentifier(localPatient, identifier))
					continue;
				
				if (identifier.getType().getText().equals(globalIdentifierName)) {
					identifier.setSystem(null);
					// identifier.setSystem(centralFhirURL + "/StructureDefinition/MPI");
					localPatient.getIdentifier().add(identifier);
				} else {
					localPatient.getIdentifier().add(identifier);
				}
			}
			
			ensureIdentifierLocationForOpenmrsUpdate(localPatient);
			
			firFhirConfig.getLocalOpenMRSFhirContext().update().resource(localPatient).execute();
			System.err.println("Local patient update with remote MPI identifier");
		} else {
			System.err.println("Local patient Already Have MPI identifier, Nothing to Update");
		}
	}
	
	/**
	 * Outcome of a central Patient POST/PUT: response bundle for downstream parsing and the central
	 * server's {@code Patient.id} from the real HTTP / transaction response (for OpenMRS Source
	 * Patient Id).
	 */
	private static final class CentralPatientWriteOutcome {
		
		private final Bundle bundle;
		
		private final String centralServerPatientLogicalId;
		
		/**
		 * MPI / CRUID / golden value for local MPI sync on mirrored responses (plain HTTP create).
		 * May differ from {@link #centralServerPatientLogicalId} (central {@code Patient.id} /
		 * Source Patient Id). Null when the outcome bundle already carries full identifier context.
		 */
		private final String centralMpiIdentifierValue;
		
		/**
		 * Raw JSON from the central HTTP response body (e.g. OpenHIM → mediator 201). When
		 * non-null, {@link #sendPatientToCentral} should set
		 * {@link FhirResponse#setResponse(String)} to this value so logs and audit match OpenHIM
		 * transaction bodies instead of a synthetic mirrored {@link Bundle}.
		 */
		private final String centralWireResponseBody;
		
		CentralPatientWriteOutcome(Bundle bundle, String centralServerPatientLogicalId) {
			this(bundle, centralServerPatientLogicalId, null, null);
		}
		
		CentralPatientWriteOutcome(Bundle bundle, String centralServerPatientLogicalId, String centralMpiIdentifierValue) {
			this(bundle, centralServerPatientLogicalId, centralMpiIdentifierValue, null);
		}
		
		CentralPatientWriteOutcome(Bundle bundle, String centralServerPatientLogicalId, String centralMpiIdentifierValue,
		    String centralWireResponseBody) {
			this.bundle = bundle;
			this.centralServerPatientLogicalId = centralServerPatientLogicalId;
			this.centralMpiIdentifierValue = centralMpiIdentifierValue;
			this.centralWireResponseBody = centralWireResponseBody;
		}
		
		Bundle getBundle() {
			return bundle;
		}
		
		String getCentralServerPatientLogicalId() {
			return centralServerPatientLogicalId;
		}
		
		String getCentralMpiIdentifierValue() {
			return centralMpiIdentifierValue;
		}
		
		String getCentralWireResponseBody() {
			return centralWireResponseBody;
		}
	}
	
	/**
	 * Direct central FHIR patient write (OpenHIM plain body or HAPI transaction client):
	 * <ul>
	 * <li><strong>Update</strong> when the facility export carries a central
	 * {@code Source Patient Id} ({@link #getSourcePatientIdentifier}): {@code PUT Patient/ id}
	 * where {@code id} is <strong>only</strong> that source logical id. Routing must never use an
	 * MPI / golden value as the path segment: those identify the MDM golden record, not the source
	 * {@code Patient} resource URL.</li>
	 * <li><strong>Optional MPI</strong> ({@link #getMpiGoldenValueForOutboundUpdate}) enriches the
	 * JSON body only; missing MPI does not block update and must not throw.</li>
	 * <li><strong>Create</strong> when no source patient id is present: {@code POST} to central.</li>
	 * </ul>
	 */
	private FhirResponse sendPatientToCentral(Patient sourcePatient) {
		FhirResponse response = new FhirResponse();
		try {
			Patient patient = sourcePatient.copy();
			// FHIR2 may emit multiple names; central IG expects a single HumanName on outbound writes.
			retainFirstHumanNameOnly(patient);
			retainFirstAddressOnly(patient);
			normalizePatientForLatestIg(patient);
			normalizeIdentifierStandards(patient);
			
			String sourcePatientId = trimToNull(getSourcePatientIdentifier(patient));
			if (sourcePatientId != null) {
				String mpiGolden = trimToNull(getMpiGoldenValueForOutboundUpdate(patient));
				if (mpiGolden != null) {
					LOGGER.debug(
					    "Central patient update: sourcePatientId={} with optional MPI/golden present (payload enrichment only, not used for PUT path)",
					    sourcePatientId);
				} else {
					LOGGER.debug(
					    "Central patient update: sourcePatientId={} without MPI/golden on export; PUT proceeds without CRUID/golden identifiers",
					    sourcePatientId);
				}
				applyCentralPutOutboundIdentifiers(patient, mpiGolden);
				CentralPatientWriteOutcome outcome = putCentralPatientBySourcePatientId(patient, sourcePatientId, mpiGolden);
				response.setStatusCode("200");
				String wireUpdate = trimToNull(outcome.getCentralWireResponseBody());
				response.setResponse(wireUpdate != null ? wireUpdate : fhirContext.newJsonParser().encodeResourceToString(
				    outcome.getBundle()));
				response.setCentralServerPatientLogicalId(outcome.getCentralServerPatientLogicalId());
				response.setMessage("Patient updated in central FHIR using source patient id path");
				return response;
			}
			
			CentralPatientWriteOutcome createOutcome = postCreatePatient(patient);
			String mpiForLocalSync = trimToNull(createOutcome.getCentralMpiIdentifierValue());
			if (mpiForLocalSync == null || mpiForLocalSync.isEmpty()) {
				mpiForLocalSync = trimToNull(extractResponseId(createOutcome.getBundle()));
			}
			if (mpiForLocalSync == null || mpiForLocalSync.isEmpty()) {
				response.setStatusCode("502");
				response.setMessage("Patient create succeeded but MPI id was not returned by central FHIR");
				response.setResponse(fhirContext.newJsonParser().encodeResourceToString(createOutcome.getBundle()));
				response.setCentralServerPatientLogicalId(createOutcome.getCentralServerPatientLogicalId());
				return response;
			}
			String centralSourceLogicalId = trimToNull(createOutcome.getCentralServerPatientLogicalId());
			String wireBody = trimToNull(createOutcome.getCentralWireResponseBody());
			if (wireBody != null) {
				// Echo the real mediator/OpenHIM HTTP body in FhirResponse so logs and audit match OpenHIM console.
				response.setStatusCode("200");
				response.setResponse(wireBody);
				response.setCentralServerPatientLogicalId(centralSourceLogicalId != null ? centralSourceLogicalId
				        : createOutcome.getCentralServerPatientLogicalId());
				response.setMessage("Patient created in central FHIR");
				return response;
			}
			Bundle mirroredBundle = buildMirroredCreatedPatientBundle(patient, mpiForLocalSync, centralSourceLogicalId);
			response.setStatusCode("200");
			response.setResponse(fhirContext.newJsonParser().encodeResourceToString(mirroredBundle));
			response.setCentralServerPatientLogicalId(centralSourceLogicalId != null ? centralSourceLogicalId
			        : createOutcome.getCentralServerPatientLogicalId());
			response.setMessage("Patient created in central FHIR");
			return response;
		}
		catch (Exception e) {
			LOGGER.error("Central patient write failed: {}", e.getMessage(), e);
			response.setStatusCode("500");
			response.setMessage(e.getMessage());
			response.setResponse("");
			return response;
		}
	}
	
	/**
	 * OpenHIM routes such as {@code .../patient-create} typically expect a single {@link Patient}
	 * JSON body. A FHIR transaction {@link Bundle} is often stripped or mishandled downstream (
	 * {@code Request body is required}).
	 */
	private boolean usesPlainPatientBodyOpenhimChannel() {
		String url = firFhirConfig.getResolvedOpenCrOpenhimUrl();
		if (url == null || url.trim().isEmpty()) {
			return false;
		}
		return url.toLowerCase().contains("patient-create");
	}
	
	/**
	 * FHIR server base for StructureDefinition URLs in outbound payloads (identifier.system,
	 * extensions), e.g. {@code http://host/fhir}. Derived from
	 * {@code intelehealth.fhir.structuredefinition.extension.url} when
	 * {@code intelehealth.fhir.central.url} is unset.
	 */
	private String getCentralStructureDefinitionBaseUrl() {
		if (sdExtensionURL != null && !sdExtensionURL.trim().isEmpty() && !sdExtensionURL.contains("${")) {
			String s = sdExtensionURL.trim();
			int i = s.toLowerCase().indexOf("/structuredefinition");
			if (i > 0) {
				String b = s.substring(0, i).trim();
				while (b.endsWith("/")) {
					b = b.substring(0, b.length() - 1);
				}
				return b;
			}
		}
		if (centralFhirURL != null && !centralFhirURL.trim().isEmpty() && !centralFhirURL.contains("${")) {
			String b = centralFhirURL.trim();
			while (b.endsWith("/")) {
				b = b.substring(0, b.length() - 1);
			}
			return b;
		}
		return null;
	}
	
	/**
	 * Base URL for central Patient HTTP writes (PUT). Always derived from
	 * {@code opencr.openhim.url} by stripping a {@code /patient-create} suffix so PUT targets the
	 * same OpenHIM/FHIR host as POST create, not {@code intelehealth.fhir.central.url}.
	 */
	private String resolveCentralPatientWriteBaseUrl() {
		String url = firFhirConfig.getResolvedOpenCrOpenhimUrl();
		if (url == null || url.trim().isEmpty()) {
			return null;
		}
		String trimmed = url.trim();
		int idx = trimmed.toLowerCase().indexOf("/patient-create");
		if (idx > 0) {
			return trimmed.substring(0, idx);
		}
		return trimmed;
	}
	
	private Bundle syntheticTransactionResponseBundleForCreatedId(String idPart) {
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.TRANSACTIONRESPONSE);
		BundleEntryResponseComponent resp = new BundleEntryResponseComponent();
		resp.setStatus("201");
		resp.setLocation("Patient/" + idPart);
		bundle.addEntry().setResponse(resp);
		return bundle;
	}
	
	private CentralPatientWriteOutcome postCreatePatient(Patient patient) {
		if (usesPlainPatientBodyOpenhimChannel()) {
			String payload = fhirContext.newJsonParser().encodeResourceToString(patient);
			String[] credentials = firFhirConfig.getOpenHimMediatorCredentials();
			FhirResponse httpRes = HttpWebClient.postWithBasicAuth(firFhirConfig.getResolvedOpenCrOpenhimUrl(), "",
			    credentials[0], credentials[1], payload);
			if (!isSuccessfulStatus(httpRes.getStatusCode())) {
				throw new IllegalStateException("OpenCR patient-create returned HTTP " + httpRes.getStatusCode() + ": "
				        + httpRes.getResponse());
			}
			Patient created = parseCentralResponsePatient(httpRes.getResponse());
			if (created == null) {
				throw new IllegalStateException("OpenCR patient-create response could not be parsed as a FHIR Patient.");
			}
			String mpiResolveHint = trimToNull(extractMpiSyncValueFromCentralCreateResponse(created));
			if (mpiResolveHint == null) {
				mpiResolveHint = trimToNull(extractReturnedMpiValue(created));
			}
			String mpiValueForLocalPersist = trimToNull(extractMpiIdentifierValueForLocalOpenMrsPatient(created));
			if (mpiValueForLocalPersist == null) {
				mpiValueForLocalPersist = mpiResolveHint;
			}
			if (mpiValueForLocalPersist == null) {
				if (created.getIdElement() != null && created.getIdElement().hasIdPart()) {
					mpiValueForLocalPersist = trimToNull(created.getIdElement().getIdPart());
				}
			}
			if (mpiValueForLocalPersist == null) {
				throw new IllegalStateException(
				        "OpenCR patient-create succeeded but no MPI / Patient.id was returned in the response body");
			}
			String mpiPersistTrim = mpiValueForLocalPersist.trim();
			String sourceLogicalIdForLocation = extractCentralServerPatientLogicalIdFromPatient(created);
			if (sourceLogicalIdForLocation == null) {
				sourceLogicalIdForLocation = extractPatientIdFromFhirLocation(httpRes.getResponseLocation());
			}
			String centralLogicalId = resolveCentralServerPatientLogicalId(created, httpRes.getResponseLocation(),
			    mpiResolveHint);
			if (trimToNull(centralLogicalId) == null) {
				centralLogicalId = sourceLogicalIdForLocation;
			}
			if (sourceLogicalIdForLocation == null || sourceLogicalIdForLocation.trim().isEmpty()) {
				sourceLogicalIdForLocation = trimToNull(centralLogicalId);
			}
			if (sourceLogicalIdForLocation == null || sourceLogicalIdForLocation.trim().isEmpty()) {
				throw new IllegalStateException(
				        "OpenCR patient-create succeeded but no central Patient.id could be determined for routing metadata.");
			}
			return new CentralPatientWriteOutcome(
			        syntheticTransactionResponseBundleForCreatedId(sourceLogicalIdForLocation.trim()), centralLogicalId,
			        mpiPersistTrim, httpRes.getResponse());
		}
		Bundle createTransaction = new Bundle();
		createTransaction.setType(Bundle.BundleType.TRANSACTION);
		createTransaction.addEntry().setResource(patient).getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl("Patient");
		Bundle createResponse = firFhirConfig.getOpenCRFhirContext().transaction().withBundle(createTransaction).execute();
		String mpiHint = extractResponseId(createResponse);
		if (mpiHint == null || mpiHint.trim().isEmpty()) {
			Patient first = extractPatientFromBundle(createResponse);
			mpiHint = extractReturnedMpiValue(first);
		}
		String centralLogicalId = resolveCentralServerPatientLogicalIdFromBundle(createResponse, trimToNull(mpiHint));
		return new CentralPatientWriteOutcome(createResponse, centralLogicalId);
	}
	
	/**
	 * Executes {@code PUT Patient/ sourcePatientLogicalId} . {@code sourcePatientLogicalId} must be
	 * the central <strong>source</strong> {@code Patient} logical id from
	 * {@link #getSourcePatientIdentifier} — never an MPI / golden id (wrong resource; breaks MDM).
	 * 
	 * @param mpiGoldenOptional nullable hint when parsing the HTTP response (disambiguate id vs
	 *            golden)
	 */
	private CentralPatientWriteOutcome putCentralPatientBySourcePatientId(Patient patient, String sourcePatientLogicalId,
	        String mpiGoldenOptional) {
		if (sourcePatientLogicalId == null || sourcePatientLogicalId.trim().isEmpty()) {
			throw new IllegalArgumentException("sourcePatientLogicalId is required for central PUT");
		}
		Patient toUpdate = patient.copy();
		toUpdate.setId(sourcePatientLogicalId.trim());
		if (usesPlainPatientBodyOpenhimChannel()) {
			String base = resolveCentralPatientWriteBaseUrl();
			if (base != null) {
				String payload = fhirContext.newJsonParser().encodeResourceToString(toUpdate);
				String[] credentials = firFhirConfig.getOpenHimMediatorCredentials();
				String putUrl = base + "/Patient/" + sourcePatientLogicalId.trim();
				LOGGER.info("Central patient update: HTTP PUT url={} (path uses source patient id only)", putUrl);
				FhirResponse httpRes = HttpWebClient.putWithBasicAuth(putUrl, "", credentials[0], credentials[1], payload);
				if (!isSuccessfulStatus(httpRes.getStatusCode())) {
					throw new IllegalStateException("OpenCR Patient PUT returned HTTP " + httpRes.getStatusCode() + ": "
					        + httpRes.getResponse());
				}
				Patient putReturned = parseCentralResponsePatient(httpRes.getResponse());
				String centralLogicalId = resolveCentralServerPatientLogicalId(putReturned, httpRes.getResponseLocation(),
				    trimToNull(mpiGoldenOptional));
				return new CentralPatientWriteOutcome(singlePatientBundle(toUpdate), centralLogicalId, null,
				        httpRes.getResponse());
			}
		}
		Bundle updateTransaction = new Bundle();
		updateTransaction.setType(Bundle.BundleType.TRANSACTION);
		updateTransaction.addEntry().setResource(toUpdate).getRequest().setMethod(Bundle.HTTPVerb.PUT)
		        .setUrl("Patient/" + sourcePatientLogicalId.trim());
		String txBase = firFhirConfig.getResolvedOpenCrOpenhimUrl();
		LOGGER.info(
		    "Central patient update: FHIR transaction clientBaseUrl={} bundleEntryRequestUrl=Patient/{} (path uses source patient id only)",
		    txBase, sourcePatientLogicalId.trim());
		Bundle txResult = firFhirConfig.getOpenCRFhirContext().transaction().withBundle(updateTransaction).execute();
		String centralLogicalId = resolveCentralServerPatientLogicalIdFromBundle(txResult, trimToNull(mpiGoldenOptional));
		return new CentralPatientWriteOutcome(singlePatientBundle(toUpdate), centralLogicalId);
	}
	
	/**
	 * FHIR2 may return multiple {@code HumanName} rows; central IG expects a single name for
	 * validation and update payloads — keep only the first.
	 */
	private static void retainFirstHumanNameOnly(Patient patient) {
		if (patient == null || !patient.hasName() || patient.getName().size() <= 1) {
			return;
		}
		HumanName first = patient.getName().get(0).copy();
		patient.getName().clear();
		patient.addName(first);
	}
	
	/**
	 * FHIR2 may return multiple {@code Address} rows; central IG expects a single address on
	 * outbound payloads — keep only the first (same approach as {@link #retainFirstHumanNameOnly}).
	 */
	private static void retainFirstAddressOnly(Patient patient) {
		if (patient == null || !patient.hasAddress() || patient.getAddress().size() <= 1) {
			return;
		}
		Address first = patient.getAddress().get(0).copy();
		patient.getAddress().clear();
		patient.addAddress(first);
	}
	
	/**
	 * Builds minimal {@code Patient.identifier} for central {@code PUT}: always the facility
	 * OpenMRS ID row; when {@code mpiGoldenOptional} is present, adds HAPI MDM golden enterprise id
	 * so the source–golden link can be preserved upstream. MPI is optional — callers must not
	 * require it for update routing.
	 */
	private void applyCentralPutOutboundIdentifiers(Patient patient, String mpiGoldenOptional) {
		if (patient == null) {
			return;
		}
		Identifier openmrsId = findFirstOpenMrsIdIdentifier(patient);
		if (openmrsId == null) {
			throw new IllegalStateException(
			        "No OpenMRS ID identifier on patient export after normalization; cannot build minimal central PUT payload.");
		}
		patient.getIdentifier().clear();
		patient.addIdentifier(openmrsId.copy());
		String mpi = trimToNull(mpiGoldenOptional);
		if (mpi == null) {
			return;
		}
		Identifier goldenId = new Identifier();
		goldenId.setSystem(HAPI_MDM_GOLDEN_ENTERPRISE_ID_SYSTEM);
		goldenId.setValue(mpi);
		patient.addIdentifier(goldenId);
	}
	
	private static Identifier findFirstOpenMrsIdIdentifier(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null) {
				continue;
			}
			if (identifier.hasType() && identifier.getType().hasText()
			        && OPENMRS_ID_TYPE_TEXT.equalsIgnoreCase(identifier.getType().getText())) {
				return identifier;
			}
			if (identifier.hasSystem() && identifier.getSystem() != null && identifier.getSystem().contains("OpenMRS-ID")) {
				return identifier;
			}
		}
		return null;
	}
	
	/**
	 * Golden / enterprise MPI value for MDM (HAPI enterprise id system, then MPI-typed identifier),
	 * never Source Patient Id.
	 */
	private String getMpiGoldenValueForOutboundUpdate(Patient patient) {
		String fromExtract = trimToNull(extractReturnedMpiValue(patient));
		if (fromExtract != null) {
			return fromExtract;
		}
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		for (Identifier id : patient.getIdentifier()) {
			if (id == null || !id.hasValue() || !id.hasType() || !id.getType().hasText()) {
				continue;
			}
			String t = id.getType().getText();
			if (t == null) {
				continue;
			}
			if (SOURCE_PATIENT_ID_TYPE_TEXT.equalsIgnoreCase(t)) {
				continue;
			}
			if (globalIdentifierName.equalsIgnoreCase(t) || MPI_TYPE_TEXT.equalsIgnoreCase(t)) {
				return trimToNull(id.getValue());
			}
		}
		return null;
	}
	
	/**
	 * OpenMRS Source Patient Id on the FHIR {@link Patient} (system urn or type text
	 * "Source Patient Id").
	 */
	private String getSourcePatientIdentifier(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue()) {
				continue;
			}
			String value = trimToNull(identifier.getValue());
			if (value == null) {
				continue;
			}
			if (SOURCE_PATIENT_ID_SYSTEM.equals(identifier.getSystem())) {
				return value;
			}
			if (identifier.hasType() && identifier.getType().hasText()
			        && SOURCE_PATIENT_ID_TYPE_TEXT.equalsIgnoreCase(identifier.getType().getText())) {
				return value;
			}
		}
		return null;
	}
	
	private Bundle buildMirroredCreatedPatientBundle(Patient patient, String mpiIdentifierValue,
	        String centralPatientLogicalId) {
		Patient mirrored = patient.copy();
		String logicalId = trimToNull(centralPatientLogicalId);
		if (logicalId == null) {
			logicalId = trimToNull(mpiIdentifierValue);
		}
		if (logicalId != null) {
			mirrored.setId(logicalId);
		}
		if (!hasMPI(mirrored)) {
			Identifier mpiIdentifier = buildMpiIdentifierFromPatient(patient, mpiIdentifierValue);
			if (mpiIdentifier != null) {
				mirrored.addIdentifier(mpiIdentifier);
			}
		}
		normalizeIdentifierStandards(mirrored);
		return singlePatientBundle(mirrored);
	}
	
	private Bundle singlePatientBundle(Patient patient) {
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.COLLECTION);
		bundle.addEntry().setResource(patient);
		return bundle;
	}
	
	private Identifier buildMpiIdentifierFromPatient(Patient sourcePatient, String mpiId) {
		if (sourcePatient != null && sourcePatient.hasIdentifier()) {
			for (Identifier identifier : sourcePatient.getIdentifier()) {
				Identifier mpiIdentifier = identifier.copy();
				mpiIdentifier.setId(UUID.randomUUID().toString());
				mpiIdentifier.setValue(mpiId);
				mpiIdentifier.setUse(IdentifierUse.OFFICIAL);
				if (!mpiIdentifier.hasType()) {
					mpiIdentifier.setType(new CodeableConcept());
				}
				mpiIdentifier.getType().setText(globalIdentifierName);
				String sdBase = getCentralStructureDefinitionBaseUrl();
				if (sdBase != null) {
					mpiIdentifier.setSystem(sdBase + "/StructureDefinition/MPI");
				}
				mpiIdentifier.getType().setCoding(new ArrayList<>());
				mpiIdentifier.getType().addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
				return mpiIdentifier;
			}
		}
		Identifier mpi = new Identifier();
		mpi.setId(UUID.randomUUID().toString());
		mpi.setUse(IdentifierUse.OFFICIAL);
		mpi.setValue(mpiId);
		String sdBase = getCentralStructureDefinitionBaseUrl();
		if (sdBase != null) {
			mpi.setSystem(sdBase + "/StructureDefinition/MPI");
		}
		CodeableConcept type = new CodeableConcept();
		type.setText(globalIdentifierName);
		type.addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
		mpi.setType(type);
		return mpi;
	}
	
	private void enrichCentralPatientWithSourcePatientIdentifierFromLogicalId(Patient patient) {
		if (patient == null || patient.getIdElement() == null || !patient.getIdElement().hasIdPart()) {
			return;
		}
		String logicalId = patient.getIdElement().getIdPart().trim();
		if (logicalId.isEmpty()) {
			return;
		}
		if (patient.hasIdentifier()) {
			for (Identifier existing : patient.getIdentifier()) {
				if (existing == null || !existing.hasValue()) {
					continue;
				}
				if (SOURCE_PATIENT_ID_SYSTEM.equals(existing.getSystem()) && logicalId.equals(existing.getValue())) {
					return;
				}
				if (existing.hasType() && existing.getType().hasText()
				        && SOURCE_PATIENT_ID_TYPE_TEXT.equalsIgnoreCase(existing.getType().getText())
				        && logicalId.equals(existing.getValue())) {
					return;
				}
			}
		}
		Identifier id = new Identifier();
		id.setId(UUID.randomUUID().toString());
		id.setUse(IdentifierUse.OFFICIAL);
		id.setSystem(SOURCE_PATIENT_ID_SYSTEM);
		id.setValue(logicalId);
		CodeableConcept type = new CodeableConcept();
		type.setText(SOURCE_PATIENT_ID_TYPE_TEXT);
		type.addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
		id.setType(type);
		patient.addIdentifier(id);
	}
	
	private void normalizeIdentifierStandards(Patient patient) {
		for (Identifier identifier : patient.getIdentifier()) {
			String typeText = identifier.hasType() ? identifier.getType().getText() : null;
			if (typeText != null && typeText.equalsIgnoreCase(OPENMRS_ID_TYPE_TEXT)) {
				String sdBase = getCentralStructureDefinitionBaseUrl();
				if (sdBase != null) {
					identifier.setSystem(sdBase + "/StructureDefinition/OpenMRS-ID");
				}
			}
			else if (typeText != null && (typeText.equalsIgnoreCase(MPI_TYPE_TEXT)
					|| typeText.equalsIgnoreCase(globalIdentifierName))) {
				String sdBase = getCentralStructureDefinitionBaseUrl();
				if (sdBase != null) {
					identifier.setSystem(sdBase + "/StructureDefinition/MPI");
				}
			}
			else if (typeText != null && typeText.equalsIgnoreCase(SOURCE_PATIENT_ID_TYPE_TEXT)) {
				identifier.setSystem(SOURCE_PATIENT_ID_SYSTEM);
			}
			if (!identifier.hasType()) {
				identifier.setType(new CodeableConcept());
			}
			identifier.getType().setCoding(new ArrayList<>());
			identifier.getType().addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
		}
	}
	
	private void normalizePatientForLatestIg(Patient patient) {
		patient.setLanguage(null);
		patient.setText(null);
		patient.getContained().clear();
	}
	
	private String extractResponseId(Bundle bundle) {
		if (bundle == null || !bundle.hasEntry()) {
			return null;
		}
		BundleEntryResponseComponent response = bundle.getEntryFirstRep().getResponse();
		if (response == null || response.getLocation() == null) {
			return null;
		}
		return extractPatientIdFromFhirLocation(response.getLocation());
	}
	
	private static String trimToNull(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}
	
	/** Central {@code Patient.id} from a parsed Patient resource (real server response body). */
	private static String extractCentralServerPatientLogicalIdFromPatient(Patient patient) {
		if (patient == null || patient.getIdElement() == null || !patient.getIdElement().hasIdPart()) {
			return null;
		}
		return trimToNull(patient.getIdElement().getIdPart());
	}
	
	/**
	 * Extracts the Patient logical id from a FHIR Location string (absolute URL, relative path, or
	 * {@code Patient/id}); strips {@code _history} segments.
	 */
	private static String extractPatientIdFromFhirLocation(String location) {
		if (location == null) {
			return null;
		}
		int q = location.indexOf('?');
		String loc = q >= 0 ? location.substring(0, q) : location;
		loc = loc.trim();
		if (loc.isEmpty()) {
			return null;
		}
		String marker = "/Patient/";
		int idx = loc.indexOf(marker);
		String tail;
		if (idx >= 0) {
			tail = loc.substring(idx + marker.length());
		} else if (loc.regionMatches(true, 0, "Patient/", 0, 8)) {
			tail = loc.substring(8);
		} else {
			return null;
		}
		int slash = tail.indexOf('/');
		if (slash >= 0) {
			tail = tail.substring(0, slash);
		}
		return trimToNull(tail);
	}
	
	/**
	 * OpenMRS "Source Patient Id" as represented on a FHIR {@link Patient} (identifier type text or
	 * {@link #SOURCE_PATIENT_ID_SYSTEM}). When {@code mpiIdentifierValue} is set, prefers a value
	 * that differs from it (avoids using the MDM golden id when the server uses it as
	 * {@code Patient.id}).
	 */
	private static String extractOpenMrsSourcePatientIdFromFhirPatient(Patient patient, String mpiIdentifierValue) {
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		String mpi = trimToNull(mpiIdentifierValue);
		List<String> candidates = new ArrayList<>();
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue()) {
				continue;
			}
			boolean isSourceType = SOURCE_PATIENT_ID_SYSTEM.equals(identifier.getSystem())
			        || (identifier.hasType() && identifier.getType().hasText() && SOURCE_PATIENT_ID_TYPE_TEXT
			                .equalsIgnoreCase(identifier.getType().getText()));
			if (!isSourceType) {
				continue;
			}
			String v = trimToNull(identifier.getValue());
			if (v != null) {
				candidates.add(v);
			}
		}
		if (mpi != null) {
			for (String c : candidates) {
				if (!c.equals(mpi)) {
					return c;
				}
			}
			return null;
		}
		return candidates.isEmpty() ? null : candidates.get(0);
	}
	
	/**
	 * Best-effort central {@code Patient.id} for OpenMRS Source Patient Id (not the MDM golden
	 * identifier value).
	 */
	private static String resolveCentralServerPatientLogicalId(Patient bodyPatient, String locationHeader,
	        String mpiIdentifierValue) {
		String mpi = trimToNull(mpiIdentifierValue);
		String fromPatient = extractCentralServerPatientLogicalIdFromPatient(bodyPatient);
		String fromLoc = extractPatientIdFromFhirLocation(locationHeader);
		if (mpi != null) {
			if (fromPatient != null && !fromPatient.equals(mpi)) {
				return fromPatient;
			}
			if (fromLoc != null && !fromLoc.equals(mpi)) {
				return fromLoc;
			}
			String fromSourceIdentifier = extractOpenMrsSourcePatientIdFromFhirPatient(bodyPatient, mpi);
			if (fromSourceIdentifier != null) {
				return fromSourceIdentifier;
			}
			return null;
		}
		if (fromPatient != null) {
			return fromPatient;
		}
		if (fromLoc != null) {
			return fromLoc;
		}
		return extractOpenMrsSourcePatientIdFromFhirPatient(bodyPatient, null);
	}
	
	/**
	 * Picks a server {@code Patient} logical id from a transaction/batch-response bundle. When the
	 * MPI golden id is known, prefers any {@code Patient.id} or {@code Location} segment that
	 * differs from it (e.g. body {@code id} is {@code 1001} while {@code Location} or MPI is the
	 * golden UUID).
	 */
	private static String resolveCentralServerPatientLogicalIdFromBundle(Bundle bundle, String mpiIdentifierValue) {
		if (bundle == null || !bundle.hasEntry()) {
			return null;
		}
		String mpi = trimToNull(mpiIdentifierValue);
		List<String> patientIds = new ArrayList<>();
		List<String> locationIds = new ArrayList<>();
		for (BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				String id = extractCentralServerPatientLogicalIdFromPatient((Patient) entry.getResource());
				if (id != null) {
					patientIds.add(id);
				}
			}
			BundleEntryResponseComponent resp = entry.getResponse();
			if (resp != null && resp.getLocation() != null) {
				String id = extractPatientIdFromFhirLocation(resp.getLocation());
				if (id != null) {
					locationIds.add(id);
				}
			}
		}
		if (mpi != null) {
			for (String pid : patientIds) {
				if (!pid.equals(mpi)) {
					return pid;
				}
			}
			for (String lid : locationIds) {
				if (!lid.equals(mpi)) {
					return lid;
				}
			}
			for (BundleEntryComponent entry : bundle.getEntry()) {
				if (entry.getResource() instanceof Patient) {
					String sid = extractOpenMrsSourcePatientIdFromFhirPatient((Patient) entry.getResource(), mpi);
					if (sid != null) {
						return sid;
					}
				}
			}
			return null;
		}
		if (!patientIds.isEmpty()) {
			return patientIds.get(0);
		}
		if (!locationIds.isEmpty()) {
			return locationIds.get(0);
		}
		return null;
	}
	
	private Patient parseCentralResponsePatient(String responseBody) {
		if (responseBody == null || responseBody.trim().isEmpty()) {
			return null;
		}
		IBaseResource parsed = fhirContext.newJsonParser().parseResource(responseBody);
		Patient patient = null;
		if (parsed instanceof Patient) {
			patient = (Patient) parsed;
		} else if (parsed instanceof Bundle) {
			patient = extractPatientFromBundle((Bundle) parsed);
		}
		if (patient != null) {
			enrichCentralPatientWithSourcePatientIdentifierFromLogicalId(patient);
		}
		return patient;
	}
	
	private Patient extractPatientFromBundle(Bundle bundle) {
		if (bundle == null || !bundle.hasEntry()) {
			return null;
		}
		for (BundleEntryComponent entry : bundle.getEntry()) {
			if (entry.getResource() instanceof Patient) {
				return (Patient) entry.getResource();
			}
		}
		return null;
	}
	
	private String extractReturnedMpiValue(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue()) {
				continue;
			}
			if (HAPI_MDM_GOLDEN_ENTERPRISE_ID_SYSTEM.equals(identifier.getSystem())) {
				return identifier.getValue();
			}
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue()) {
				continue;
			}
			if (identifier.hasType() && identifier.getType().hasText()) {
				String text = identifier.getType().getText();
				if (text != null && (text.equalsIgnoreCase(globalIdentifierName) || text.equalsIgnoreCase(MPI_TYPE_TEXT))) {
					return identifier.getValue();
				}
			}
		}
		return patient.getIdentifier().size() == 1 ? patient.getIdentifierFirstRep().getValue() : null;
	}
	
	/**
	 * MPI / CRUID / golden value from a minimal central create body (e.g. OpenHIM mediator): prefer
	 * typed MPI and CRUID over HAPI golden so {@link #extractReturnedMpiValue} semantics used
	 * elsewhere (MDM golden resolution on full exports) stay unchanged.
	 */
	private String extractMpiSyncValueFromCentralCreateResponse(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue()) {
				continue;
			}
			if (!identifier.hasType() || !identifier.getType().hasText()) {
				continue;
			}
			String text = identifier.getType().getText();
			if (text != null && (text.equalsIgnoreCase(globalIdentifierName) || text.equalsIgnoreCase(MPI_TYPE_TEXT))) {
				return trimToNull(identifier.getValue());
			}
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue() || !identifier.hasSystem()) {
				continue;
			}
			if (CRUID_IDENTIFIER_SYSTEM.equals(identifier.getSystem())) {
				return trimToNull(identifier.getValue());
			}
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || !identifier.hasValue()) {
				continue;
			}
			if (HAPI_MDM_GOLDEN_ENTERPRISE_ID_SYSTEM.equals(identifier.getSystem())) {
				return trimToNull(identifier.getValue());
			}
		}
		return patient.getIdentifier().size() == 1 ? trimToNull(patient.getIdentifierFirstRep().getValue()) : null;
	}
	
	/**
	 * True when the patient carries a CRUID identifier or an MPI-typed identifier (facility export
	 * shape). Used to decide when the golden-enterprise branch may fall back to the source logical
	 * id for duplicate-safety on fuller central payloads.
	 */
	private boolean hasCruidOrMpiTypedIdentifier(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return false;
		}
		for (Identifier id : patient.getIdentifier()) {
			if (id == null || !id.hasValue()) {
				continue;
			}
			if (id.hasSystem() && CRUID_IDENTIFIER_SYSTEM.equals(id.getSystem())) {
				return true;
			}
			if (id.hasType() && id.getType().hasText()) {
				String text = id.getType().getText();
				if (text != null && (text.equalsIgnoreCase(globalIdentifierName) || text.equalsIgnoreCase(MPI_TYPE_TEXT))) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Value for the facility OpenMRS {@code PatientIdentifier} typed as MPI (
	 * {@link #globalIdentifierName}). OpenMRS enforces <strong>global uniqueness</strong> per
	 * identifier type, so on <strong>full</strong> central exports that include CRUID or typed MPI
	 * plus HAPI golden, the golden UUID may already exist on another facility patient; when
	 * {@code Patient.id} (source logical id) differs from the golden value we then persist that
	 * logical id instead. Minimal HAPI / mediator bodies that only expose the MDM golden enterprise
	 * id (no CRUID, no MPI-typed identifier) must persist that golden UUID as MPI — not the source
	 * {@code Patient.id}, which is not the enterprise MPI and often collides with other OpenMRS
	 * identifier rows.
	 */
	private String extractMpiIdentifierValueForLocalOpenMrsPatient(Patient centralResponsePatient) {
		if (centralResponsePatient == null) {
			return null;
		}
		for (Identifier id : centralResponsePatient.getIdentifier()) {
			if (id != null && id.hasValue() && id.hasSystem() && CRUID_IDENTIFIER_SYSTEM.equals(id.getSystem())) {
				return trimToNull(id.getValue());
			}
		}
		for (Identifier id : centralResponsePatient.getIdentifier()) {
			if (id == null || !id.hasValue() || !id.hasType() || !id.getType().hasText()) {
				continue;
			}
			String text = id.getType().getText();
			if (text != null && (text.equalsIgnoreCase(globalIdentifierName) || text.equalsIgnoreCase(MPI_TYPE_TEXT))) {
				return trimToNull(id.getValue());
			}
		}
		String golden = null;
		for (Identifier id : centralResponsePatient.getIdentifier()) {
			if (id != null && id.hasValue() && HAPI_MDM_GOLDEN_ENTERPRISE_ID_SYSTEM.equals(id.getSystem())) {
				golden = trimToNull(id.getValue());
				break;
			}
		}
		if (golden != null) {
			if (!hasCruidOrMpiTypedIdentifier(centralResponsePatient)) {
				return golden;
			}
			String patientLogicalId = extractCentralServerPatientLogicalIdFromPatient(centralResponsePatient);
			if (patientLogicalId != null && !patientLogicalId.equals(golden)) {
				return patientLogicalId;
			}
			return golden;
		}
		return trimToNull(extractReturnedMpiValue(centralResponsePatient));
	}
	
	private boolean isSuccessfulStatus(String statusCode) {
		return statusCode != null && statusCode.startsWith("2");
	}
	
	private String extractResourceId(Bundle bundle) {
		if (bundle.getEntry().size() != 1)
			return null;
		Resource resource = bundle.getEntryFirstRep().getResource();
		return resource.getIdElement().getIdPart();
	}
	
	private Identifier getMPIIndentifierFromBundle(Bundle bundle) {
		if (bundle.getEntry().size() < 1) {
			return null;
		}
		
		for (BundleEntryComponent bundleEntry : bundle.getEntry()) {
			Patient patient = (Patient) bundleEntry.getResource();
			for (Identifier identifier : patient.getIdentifier()) {
				if (identifier.getType().getText().equals(globalIdentifierName)) {
					return identifier;
				}
			}
		}
		return null;
	}
	
	private List<Identifier> getIdentifiers(Bundle bundle) {
		if (bundle.getEntry().size() < 1) {
			return new ArrayList<>();
		}

		for (BundleEntryComponent bundleEntry : bundle.getEntry()) {
			Patient patient = (Patient) bundleEntry.getResource();
			return patient.getIdentifier();

		}
		return new ArrayList<>();
	}
	
	private boolean hasMPI(Patient patient) {
		return localPatientMpiUpdateService.patientHasMpiPerSchedulerExportRule(patient);
	}
	
	private void ensureIdentifierLocationForOpenmrsUpdate(Patient patient) {
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
			locationExtension.setValue(new Reference("Location/" + OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID));
			identifier.getExtension().add(locationExtension);
		}
	}
	
	private boolean matchWithLocalIdentifier(Patient localPatient, Identifier identifier) {
		
		for (Identifier localIdentifier : localPatient.getIdentifier()) {
			if (localIdentifier.getValue().equals(identifier.getValue()))
				return true;
			
		}
		return false;
		
	}
	
	private void applyPatientMetaSource(Patient patient) {
		if (!patient.hasMeta()) {
			patient.setMeta(new Meta());
		}
		patient.getMeta().setSource("https://intelehealth.org");
		String profileUrl = getPatientProfileUrl();
		if (!patient.getMeta().getProfile().stream().anyMatch(p -> profileUrl.equals(p.getValueAsString()))) {
			patient.getMeta().addProfile(profileUrl);
		}
	}
	
	private String getPatientProfileUrl() {
		return patientProfileUrl;
	}
	
	private Patient addExtension(Patient patient, String patientUUID) {
		List<PersonAttribute> attributes = commonOperationService.findPersonAttributes(patientUUID);
		PatientTelecomMappingUtil.applyRankedPhoneTelecom(patient, attributes);
		// System.err.println("Person attributes found : " + attributes.size());

		List<Extension> extensionList = new ArrayList<Extension>();
		for (PersonAttribute attribute : attributes) {
			String suffix = PersonAttributeToExtensionSuffix.map(attribute.getName());
			if (suffix == null || !PatientProfileExtensionRules.isAllowedStructureDefinitionSuffix(suffix))
				continue;
			if (isIgnorablePersonAttributeValue(attribute.getValue()))
				continue;

			String sdBase = getCentralStructureDefinitionBaseUrl();
			if (sdBase == null) {
				continue;
			}
			Extension extension = new Extension();
			extension.setUrl(sdBase + "/StructureDefinition/" + suffix);
			extension.setValue(new StringType(attribute.getValue()));
			extensionList.add(extension);
		}

		patient.getExtension().addAll(extensionList);
		List<Address> addressList = patient.getAddress();

		for (Address address : addressList) {
		    List<StringType> newLines = new ArrayList<>();

		    for (Extension ext : address.getExtension()) {
		        for (Extension e : ext.getExtension()) {
	            	System.out.println(e.getUrl());
	            	System.out.println((StringType) e.getValue());
		            if (e.getValue() instanceof StringType) {
		            	System.out.println(e.getUrl());
		            	System.out.println((StringType) e.getValue());
		                newLines.add((StringType) e.getValue());
		            } else if (e.getValue() != null) {
		                newLines.add(new StringType(e.getValue().toString()));
		            }
		        }
		    }

		    address.getLine().clear();
		    address.getLine().addAll(newLines);
		    address.getExtension().clear();
		}

		return patient;
	}
	
	private boolean isIgnorablePersonAttributeValue(String value) {
		if (value == null)
			return true;
		String trimmed = value.trim();
		return trimmed.isEmpty();
	}
	
	private void normalizePatientForIgValidation(Patient patient) {
		// Align with IHPatientProfile: these elements are disallowed (0..0).
		patient.setLanguage(null);
		patient.setText(null);
		patient.getContained().clear();

		// Keep only Patient-level extensions declared in the IG profile.
		patient.setExtension(patient.getExtension().stream()
				.filter(ext -> isAllowedPatientExtensionUrl(ext.getUrl()))
				.collect(Collectors.toList()));

		for (Identifier identifier : patient.getIdentifier()) {
			// OpenMRS identifier location extension is not defined in this IG.
			identifier.setExtension(new ArrayList<>());
			ensureIdentifierSystem(identifier);
			ensureIdentifierTypeCoding(identifier);
		}
	}
	
	private boolean isAllowedPatientExtensionUrl(String url) {
		return PatientProfileExtensionRules.isAllowedPatientExtensionUrl(url);
	}
	
	private void ensureIdentifierSystem(Identifier identifier) {
		String typeText = identifier.hasType() ? identifier.getType().getText() : null;
		String sdBase = getCentralStructureDefinitionBaseUrl();
		if (typeText != null && typeText.equalsIgnoreCase(OPENMRS_ID_TYPE_TEXT)) {
			if (sdBase != null) {
				identifier.setSystem(sdBase + "/StructureDefinition/OpenMRS-ID");
			}
			return;
		}
		if (typeText != null
		        && (typeText.equalsIgnoreCase(globalIdentifierName) || typeText.equalsIgnoreCase(MPI_TYPE_TEXT))) {
			if (sdBase != null) {
				identifier.setSystem(sdBase + "/StructureDefinition/MPI");
			}
			return;
		}
		if (typeText != null && typeText.equalsIgnoreCase(SOURCE_PATIENT_ID_TYPE_TEXT)) {
			identifier.setSystem(SOURCE_PATIENT_ID_SYSTEM);
			return;
		}
		/*
		 * if (!identifier.hasSystem()) { identifier.setSystem("urn:ietf:rfc:3986"); }
		 */
	}
	
	private void ensureIdentifierTypeCoding(Identifier identifier) {
		CodeableConcept type = identifier.getType();
		String mappedCode = "MR";
		CodeableConcept ensuredType = type != null ? type : new CodeableConcept();
		ensuredType.setCoding(new ArrayList<>());
		Coding coding = new Coding();
		coding.setSystem("http://terminology.hl7.org/CodeSystem/v2-0203");
		coding.setCode(mappedCode);
		ensuredType.addCoding(coding);
		identifier.setType(ensuredType);
	}
}
