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
	 * Operator export: same steps as {@link #send(String, String)} but performs <strong>no</strong>
	 * central OpenCR demographic duplicate search (no {@link CentralPatientDuplicateMatcher}).
	 * Still loads the Patient via local FHIR {@code GET Patient?_id=} — that read-by-id is required
	 * and is not an MPI duplicate search.
	 */
	public FhirResponse forceSendPatientToCentralByUuid(String patientUuid) throws ParseException, DataFormatException,
	        JSONException, ConfigurationException, IOException {
		ensureDependencies();
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		String uuid = patientUuid.trim();
		System.err.println("resourceType => Patient => " + uuid + " (force-sync, skip central duplicate search)");
		String localBaseUrl = firFhirConfig.getResolvedLocalOpenmrsBaseUrl();
		LOGGER.error("force-sync config check localOpenmrsOpenhimURL='{}'", localBaseUrl);
		if (localBaseUrl == null || localBaseUrl.trim().isEmpty() || localBaseUrl.contains("${")) {
			throw new IllegalStateException(
			        "Invalid local OpenMRS base URL config: local.openmrs.openhim.url is blank or unresolved");
		}
		Bundle localBundle = firFhirConfig.getLocalOpenMRSFhirContext().search().byUrl("Patient?_id=" + uuid)
		        .returnBundle(Bundle.class).execute();
		String data = fhirContext.newJsonParser().encodeResourceToString(localBundle);
		
		System.err.println("Local Fhir Bundle => " + data);
		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
		
		if (!theBundle.hasEntry()) {
			throw new IllegalArgumentException("Local Patient bundle empty for uuid=" + uuid);
		}
		Resource firstRes = theBundle.getEntryFirstRep().getResource();
		if (!(firstRes instanceof Patient)) {
			throw new IllegalArgumentException("Local FHIR entry is not a Patient for uuid=" + uuid);
		}
		Patient localPatientFromFetch = (Patient) firstRes;
		if (localPatientMpiUpdateService.patientHasMpiPerSchedulerExportRule(localPatientFromFetch)) {
			LOGGER.info("Force sync skipped for patient uuid={}: local MPI identifier already present", uuid);
			FhirResponse skipped = new FhirResponse();
			skipped.setStatusCode("skipped");
			skipped.setMessage(LocalPatientMpiUpdateService.MESSAGE_MPI_ALREADY_SET_FORCE_SYNC);
			skipped.setResponse(null);
			return skipped;
		}
		
		return sendFHIRBundle(theBundle, "Patient", false);
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
			
			String payload = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle)
			        .toString();
			
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
			log.setRequest(payload);
			log.setRequestUrl(opencrOpenhimURL);
			
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
				System.err.println("Response from central fhir: " + res.getResponse());
				String returnedMpi = extractReturnedMpiValue(remotePatient);
				if (remotePatient == null) {
					throw new IllegalStateException("Central FHIR response did not contain a Patient resource");
				}
				if (returnedMpi == null || returnedMpi.trim().isEmpty()) {
					throw new IllegalStateException("Central FHIR response did not contain an MPI identifier value");
				}
				localPatientMpiUpdateService.upsertMpiIdentifierToLocalPatient(localPatientUUID, returnedMpi.trim());
				if (uLog != null && remotePatient.getIdElement() != null && remotePatient.getIdElement().hasIdPart()) {
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
	 * Direct OpenCR patient write (without MCI application-layer hop):
	 * <ul>
	 * <li>If patient already has MPI identifier text -> PUT Patient/{mpiId}</li>
	 * <li>If patient has no MPI identifier -> POST Patient (single call), then mirror returned id
	 * as MPI for local OpenMRS update payload only</li>
	 * </ul>
	 */
	private FhirResponse sendPatientToCentral(Patient sourcePatient) {
		FhirResponse response = new FhirResponse();
		try {
			Patient patient = sourcePatient.copy();
			normalizePatientForLatestIg(patient);
			normalizeIdentifierStandards(patient);
			
			String payload = fhirContext.newJsonParser().encodeResourceToString(patient);
			String[] credentials = firFhirConfig.getOpenCRCredentials();
			FhirResponse createResponse = HttpWebClient.postWithBasicAuth(opencrOpenhimURL, "", credentials[0],
			    credentials[1], payload);
			if (isSuccessfulStatus(createResponse.getStatusCode())) {
				Patient createdPatient = parseCentralResponsePatient(createResponse.getResponse());
				String createdMpi = extractReturnedMpiValue(createdPatient);
				if (createdPatient == null || createdMpi == null || createdMpi.trim().isEmpty()) {
					response.setStatusCode("502");
					response.setMessage("Patient create succeeded but MPI id was not returned by central FHIR");
					response.setResponse(createResponse.getResponse());
					return response;
				}
			}
			return createResponse;
		}
		catch (Exception e) {
			LOGGER.error("Central patient write failed: {}", e.getMessage(), e);
			response.setStatusCode("500");
			response.setMessage(e.getMessage());
			response.setResponse("");
			return response;
		}
	}
	
	private Bundle buildMirroredCreatedPatientBundle(Patient patient, String mpiId) {
		Patient mirrored = patient.copy();
		mirrored.setId(mpiId);
		if (!hasMPI(mirrored)) {
			Identifier mpiIdentifier = buildMpiIdentifierFromPatient(patient, mpiId);
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
				mpiIdentifier.setSystem(centralFhirURL + "/StructureDefinition/MPI");
				mpiIdentifier.getType().setCoding(new ArrayList<>());
				mpiIdentifier.getType().addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
				return mpiIdentifier;
			}
		}
		Identifier mpi = new Identifier();
		mpi.setId(UUID.randomUUID().toString());
		mpi.setUse(IdentifierUse.OFFICIAL);
		mpi.setValue(mpiId);
		mpi.setSystem(centralFhirURL + "/StructureDefinition/MPI");
		CodeableConcept type = new CodeableConcept();
		type.setText(globalIdentifierName);
		type.addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
		mpi.setType(type);
		return mpi;
	}
	
	private void normalizeIdentifierStandards(Patient patient) {
		for (Identifier identifier : patient.getIdentifier()) {
			String typeText = identifier.hasType() ? identifier.getType().getText() : null;
			if (typeText != null && typeText.equalsIgnoreCase(OPENMRS_ID_TYPE_TEXT)) {
				identifier.setSystem(centralFhirURL + "/StructureDefinition/OpenMRS-ID");
			}
			else if (typeText != null && (typeText.equalsIgnoreCase(MPI_TYPE_TEXT)
					|| typeText.equalsIgnoreCase(globalIdentifierName))) {
				identifier.setSystem(centralFhirURL + "/StructureDefinition/MPI");
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
		String[] parts = response.getLocation().split("/");
		return parts.length > 1 ? parts[1] : null;
	}
	
	private Patient parseCentralResponsePatient(String responseBody) {
		if (responseBody == null || responseBody.trim().isEmpty()) {
			return null;
		}
		IBaseResource parsed = fhirContext.newJsonParser().parseResource(responseBody);
		if (parsed instanceof Patient) {
			return (Patient) parsed;
		}
		if (parsed instanceof Bundle) {
			return extractPatientFromBundle((Bundle) parsed);
		}
		return null;
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

			Extension extension = new Extension();
			String url = centralFhirURL + "/StructureDefinition/" + suffix;
			extension.setUrl(url);
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
		if (typeText != null && typeText.equalsIgnoreCase(OPENMRS_ID_TYPE_TEXT)) {
			identifier.setSystem(centralFhirURL + "/StructureDefinition/OpenMRS-ID");
			return;
		}
		if (typeText != null
		        && (typeText.equalsIgnoreCase(globalIdentifierName) || typeText.equalsIgnoreCase(MPI_TYPE_TEXT))) {
			identifier.setSystem(centralFhirURL + "/StructureDefinition/MPI");
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
