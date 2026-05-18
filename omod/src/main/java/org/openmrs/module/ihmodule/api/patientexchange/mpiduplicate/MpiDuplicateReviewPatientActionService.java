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
	public void skipCandidateByCaseUuid(String caseUuid, long candidateId, String resolvedBy) {
		MpiPatientDuplicateReviewCase reviewCase = requirePendingCase(caseUuid);
		MpiPatientDuplicateReviewCandidate row = candidateRepository.findById(candidateId);
		if (row == null || row.getReviewCase() == null || !reviewCase.getId().equals(row.getReviewCase().getId())) {
			throw new IllegalArgumentException("candidateId does not belong to caseUuid");
		}
		mpiDuplicateReviewResolutionService.markCandidateSkipped(row, resolvedBy);
	}
	
	/**
	 * Pending source-patient list: marks {@code mpi_patient_duplicate_review_case} and all
	 * candidates as {@link MpiDuplicateReviewStatus#SKIPPED} so the case leaves the pending queue.
	 */
	@Transactional
	public void skipCaseByCaseUuid(String caseUuid, String resolvedBy) {
		MpiPatientDuplicateReviewCase reviewCase = requirePendingCase(caseUuid);
		mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesSkipped(reviewCase, resolvedBy);
		LOG.info("Duplicate-review pending case skipped: case_uuid={} by {}", reviewCase.getCaseUuid(), resolvedBy);
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
		Patient sourcePatient = resolveSourcePatientFromCase(reviewCase);
		String candidateLogicalId = StringUtils.trimToNull(row.getFhirPatientLogicalId());
		String matchSrc = StringUtils.trimToEmpty(row.getMatchSource());
		OpenmrsPatientUpsertResult result;
		if (MpiImportDuplicateReviewSource.OPENMRS.getValue().equalsIgnoreCase(matchSrc)) {
			result = linkAndJoinFromOpenMrsMatchSource(sourcePatient, row, candidateLogicalId);
		} else if (MpiImportDuplicateReviewSource.FHIR.getValue().equalsIgnoreCase(matchSrc)) {
			result = linkAndJoinFromFhirMatchSource(sourcePatient, candidateLogicalId);
		} else {
			throw new IllegalArgumentException("Unsupported match_source for candidate: " + matchSrc);
		}
		LOG.info("Duplicate-review link-and-join ({}): {} openmrsPatientUuid={} case_uuid={} candidateId={}", matchSrc,
		    result.getMessage(), result.getPatientUuid(), caseUuid, candidateId);
		mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesCompleted(reviewCase, resolvedBy);
	}
	
	/**
	 * {@code match_source=openmrs}: resolve existing row by OpenMRS identifier (source, then
	 * candidate snapshot), require a match, update from source import data.
	 */
	private OpenmrsPatientUpsertResult linkAndJoinFromOpenMrsMatchSource(Patient sourcePatient,
	        MpiPatientDuplicateReviewCandidate row, String candidateLogicalId) {
		org.openmrs.Patient existing = resolveExistingPatientByOpenMrsIdentifier(sourcePatient, row);
		if (existing == null) {
			String openMrsId = patientUploadImportService.resolveOpenMrsIdentifierValueFromFhirPatient(sourcePatient);
			throw new IllegalArgumentException("OpenMRS patient not found for OpenMRS identifier: "
			        + StringUtils.defaultString(openMrsId, "(missing)"));
		}
		assertSelectedOpenMrsCandidate(existing, candidateLogicalId);
		return patientUploadImportService.updateOpenmrsPatientFromSourcePatient(sourcePatient, existing, null);
	}
	
	/**
	 * {@code match_source=fhir}: resolve by OpenMRS identifier on source; update if found,
	 * otherwise create from source; attach central FHIR Patient ID when configured.
	 */
	private OpenmrsPatientUpsertResult linkAndJoinFromFhirMatchSource(Patient sourcePatient, String candidateLogicalId) {
		org.openmrs.Patient existing = patientUploadImportService.findOpenmrsPatientBySourceOpenMrsIdentifier(sourcePatient);
		OpenmrsPatientUpsertResult result;
		if (existing != null) {
			result = patientUploadImportService.updateOpenmrsPatientFromSourcePatient(sourcePatient, existing, null);
		} else {
			result = patientUploadImportService.createOpenmrsPatientFromSourcePatient(sourcePatient, null);
		}
		attachFhirPatientIdIdentifierIfConfigured(result.getPatientUuid(), candidateLogicalId);
		return result;
	}
	
	private org.openmrs.Patient resolveExistingPatientByOpenMrsIdentifier(Patient sourcePatient,
	        MpiPatientDuplicateReviewCandidate row) {
		org.openmrs.Patient bySourceId = patientUploadImportService
		        .findOpenmrsPatientBySourceOpenMrsIdentifier(sourcePatient);
		if (bySourceId != null) {
			return bySourceId;
		}
		String candidateJson = StringUtils.trimToNull(row.getPatientResourceJson());
		if (candidateJson != null) {
			Patient candidateFhir = fhirContext.newJsonParser().parseResource(Patient.class, candidateJson);
			org.openmrs.Patient byCandidateId = patientUploadImportService
			        .findOpenmrsPatientBySourceOpenMrsIdentifier(candidateFhir);
			if (byCandidateId != null) {
				return byCandidateId;
			}
		}
		if (StringUtils.isNotBlank(row.getFhirPatientLogicalId())) {
			org.openmrs.Patient byUuid = Context.getPatientService().getPatientByUuid(row.getFhirPatientLogicalId().trim());
			if (byUuid != null && !Boolean.TRUE.equals(byUuid.getVoided())) {
				return byUuid;
			}
		}
		return null;
	}
	
	private void assertSelectedOpenMrsCandidate(org.openmrs.Patient resolved, String candidateLogicalId) {
		if (StringUtils.isBlank(candidateLogicalId)) {
			return;
		}
		if (!candidateLogicalId.trim().equals(resolved.getUuid())) {
			throw new IllegalArgumentException(
			        "Selected OpenMRS duplicate candidate does not match patient resolved by OpenMRS identifier");
		}
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
	
	private Patient resolveSourcePatientFromCase(MpiPatientDuplicateReviewCase reviewCase) {
		String outbound = StringUtils.trimToNull(reviewCase.getOutboundBundleJson());
		if (outbound == null) {
			throw new IllegalArgumentException(
			        "outbound_bundle_json is required on the duplicate-review case to upload source patient data");
		}
		return parsePatientFromOutboundJson(outbound);
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
