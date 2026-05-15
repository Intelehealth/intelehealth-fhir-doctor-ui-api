package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.OpenmrsPatientUpsertResult;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;

/**
 * MPI duplicate-review UI: add patient (pending row or candidate) and skip flows.
 */
@Service
public class MpiDuplicateReviewPatientActionService {
	
	private static final Logger LOG = LoggerFactory.getLogger(MpiDuplicateReviewPatientActionService.class);
	
	private static final String FHIR_PATIENT_ID_TYPE_NAME = "FHIR Patient ID";
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private MpiPatientDuplicateReviewCaseRepository caseRepository;
	
	@Autowired
	private MpiPatientDuplicateReviewCandidateRepository candidateRepository;
	
	@Autowired
	private MpiDuplicateReviewResolutionService mpiDuplicateReviewResolutionService;
	
	@Autowired
	private PatientUploadImportService patientUploadImportService;
	
	@Transactional
	public void skipCaseByCaseUuid(String caseUuid, String resolvedBy) {
		MpiPatientDuplicateReviewCase c = requirePendingCase(caseUuid);
		mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesSkipped(c, resolvedBy);
	}
	
	@Transactional
	public void addPatientFromPendingCase(String caseUuid, String patientUuidForLegacyForceSync, String resolvedBy)
	        throws ParseException, DataFormatException, JSONException, ConfigurationException, IOException {
		MpiPatientDuplicateReviewCase reviewCase = requirePendingCase(caseUuid);
		String outbound = StringUtils.trimToNull(reviewCase.getOutboundBundleJson());
		if (outbound != null) {
			Patient p = parsePatientFromOutboundJson(outbound);
			OpenmrsPatientUpsertResult result = patientUploadImportService.upsertOpenmrsPatientFromFhirPatient(p, null);
			LOG.info("Duplicate-review pending add: {} openmrsPatientUuid={} case_uuid={}", result.getMessage(),
			    result.getPatientUuid(), reviewCase.getCaseUuid());
			mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesCompleted(reviewCase, resolvedBy);
			return;
		}
		if (StringUtils.isBlank(patientUuidForLegacyForceSync)) {
			throw new IllegalArgumentException(
			        "No outbound_bundle_json on case and patientUuid is blank; cannot add patient for case_uuid=" + caseUuid);
		}
		OpenmrsPatientUpsertResult result = patientUploadImportService
		        .forceSyncLocalOpenmrsPatientByUuid(patientUuidForLegacyForceSync.trim());
		LOG.info("Duplicate-review pending add (legacy uuid): {} openmrsPatientUuid={} case_uuid={}", result.getMessage(),
		    result.getPatientUuid(), reviewCase.getCaseUuid());
		mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesCompleted(reviewCase, resolvedBy);
	}
	
	@Transactional
	public void addPatientFromCandidate(String caseUuid, long candidateId, String resolvedBy) throws ParseException,
	        DataFormatException, JSONException, ConfigurationException, IOException {
		MpiPatientDuplicateReviewCase reviewCase = requirePendingCase(caseUuid);
		MpiPatientDuplicateReviewCandidate row = candidateRepository.findById(candidateId);
		if (row == null || row.getReviewCase() == null || !reviewCase.getId().equals(row.getReviewCase().getId())) {
			throw new IllegalArgumentException("candidateId does not belong to caseUuid");
		}
		String json = StringUtils.trimToNull(row.getPatientResourceJson());
		if (json == null) {
			throw new IllegalArgumentException("patient_resource_json is empty for candidate " + candidateId);
		}
		Patient source = fhirContext.newJsonParser().parseResource(Patient.class, json);
		Patient prepared = source.copy();
		String logical = StringUtils.trimToNull(row.getFhirPatientLogicalId());
		String matchSrc = StringUtils.trimToEmpty(row.getMatchSource());
		if (MpiImportDuplicateReviewSource.FHIR.getValue().equalsIgnoreCase(matchSrc)) {
			prepared.setId((String) null);
		} else if (MpiImportDuplicateReviewSource.OPENMRS.getValue().equalsIgnoreCase(matchSrc)) {
			if (logical == null) {
				throw new IllegalArgumentException("fhir_patient_logical_id is required for OpenMRS candidate update");
			}
			prepared.setId(logical);
		} else {
			throw new IllegalArgumentException("Unsupported match_source for candidate: " + row.getMatchSource());
		}
		OpenmrsPatientUpsertResult result = patientUploadImportService.upsertOpenmrsPatientFromFhirPatient(prepared, null);
		if (MpiImportDuplicateReviewSource.FHIR.getValue().equalsIgnoreCase(matchSrc)) {
			attachFhirPatientIdIdentifierIfConfigured(result.getPatientUuid(), logical);
		}
		LOG.info("Duplicate-review candidate add ({}): {} openmrsPatientUuid={} case_uuid={} candidateId={}", matchSrc,
		    result.getMessage(), result.getPatientUuid(), caseUuid, candidateId);
		mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesCompleted(reviewCase, resolvedBy);
	}
	
	private MpiPatientDuplicateReviewCase requirePendingCase(String caseUuid) {
		if (StringUtils.isBlank(caseUuid)) {
			throw new IllegalArgumentException("caseUuid is required");
		}
		MpiPatientDuplicateReviewCase c = caseRepository.findByCaseUuid(caseUuid.trim()).orElseThrow(
		    () -> new IllegalArgumentException("Unknown caseUuid: " + caseUuid));
		if (c.getReviewStatus() != MpiDuplicateReviewStatus.PENDING) {
			throw new IllegalStateException("Case is not PENDING: " + c.getReviewStatus());
		}
		return c;
	}
	
	private Patient parsePatientFromOutboundJson(String outbound) {
		// Do not use parseResource(Resource.class, …): Resource is abstract and HAPI throws HAPI-1682.
		IBaseResource base = fhirContext.newJsonParser().parseResource(outbound);
		if (base instanceof Patient) {
			return (Patient) base;
		}
		if (base instanceof Bundle) {
			Bundle b = (Bundle) base;
			if (b.hasEntry()) {
				for (Bundle.BundleEntryComponent e : b.getEntry()) {
					if (e != null && e.hasResource() && e.getResource() instanceof Patient) {
						return (Patient) e.getResource();
					}
				}
			}
		}
		throw new IllegalArgumentException("outbound_bundle_json must be a FHIR Patient or a Bundle containing a Patient");
	}
	
	private void attachFhirPatientIdIdentifierIfConfigured(String openmrsPatientUuid, String fhirLogicalIdValue) {
		if (StringUtils.isBlank(openmrsPatientUuid) || StringUtils.isBlank(fhirLogicalIdValue)) {
			return;
		}
		org.openmrs.Patient p = Context.getPatientService().getPatientByUuid(openmrsPatientUuid.trim());
		if (p == null) {
			LOG.warn("Cannot attach FHIR Patient ID: OpenMRS patient not found uuid={}", openmrsPatientUuid);
			return;
		}
		PatientIdentifierType t = Context.getPatientService().getPatientIdentifierTypeByName(FHIR_PATIENT_ID_TYPE_NAME);
		if (t == null) {
			LOG.warn("Patient identifier type '{}' not found; skipping secondary identifier", FHIR_PATIENT_ID_TYPE_NAME);
			return;
		}
		Location loc = Context.getLocationService().getDefaultLocation();
		for (PatientIdentifier existing : p.getActiveIdentifiers()) {
			if (existing.getIdentifier() != null && existing.getIdentifier().equals(fhirLogicalIdValue.trim())
			        && existing.getIdentifierType() != null && existing.getIdentifierType().getId() != null
			        && existing.getIdentifierType().getId().equals(t.getId())) {
				return;
			}
		}
		PatientIdentifier pid = new PatientIdentifier();
		pid.setIdentifier(fhirLogicalIdValue.trim());
		pid.setIdentifierType(t);
		pid.setLocation(loc);
		pid.setPreferred(false);
		p.addIdentifier(pid);
		Context.getPatientService().savePatient(p);
	}
}
