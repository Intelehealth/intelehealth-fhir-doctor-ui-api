package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.PatientIdentifier;
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
		OpenmrsPatientUpsertResult result = linkAndJoinFromOpenMrsMatchSource(sourcePatient, row, candidateLogicalId);
		LOG.info("Duplicate-review link-and-join (openmrs): {} openmrsPatientUuid={} case_uuid={} candidateId={}",
		    result.getMessage(), result.getPatientUuid(), caseUuid, candidateId);
		mpiDuplicateReviewResolutionService.markCaseAndAllCandidatesCompleted(reviewCase, resolvedBy);
	}
	
	/**
	 * {@code match_source=openmrs}: resolve existing row by OpenMRS identifier (source, then
	 * candidate snapshot), require a match, update from source import data.
	 */
	private OpenmrsPatientUpsertResult linkAndJoinFromOpenMrsMatchSource(Patient sourcePatient,
	        MpiPatientDuplicateReviewCandidate row, String candidateLogicalId) {
		org.openmrs.Patient existing = resolveExistingOpenMrsDuplicateCandidatePatient(row, candidateLogicalId);
		if (existing == null) {
			String candidateId = resolveCandidateIdentifierHint(row);
			throw new IllegalArgumentException("OpenMRS patient not found for candidate identifier: "
			        + StringUtils.defaultString(candidateId, "(missing)"));
		}
		assertSelectedOpenMrsCandidate(existing, row, candidateLogicalId);
		return patientUploadImportService.updateOpenmrsPatientFromSourcePatient(sourcePatient, existing, null);
	}
	
	/**
	 * {@code match_source=openmrs}: resolve the duplicate candidate row first (by candidate
	 * {@code identifier.value}, then stored logical id / {@code Patient.id}), not by the import
	 * source OpenMRS ID.
	 */
	private org.openmrs.Patient resolveExistingOpenMrsDuplicateCandidatePatient(MpiPatientDuplicateReviewCandidate row,
	        String candidateLogicalId) {
		String candidateJson = StringUtils.trimToNull(row.getPatientResourceJson());
		Patient candidateFhir = candidateJson != null ? fhirContext.newJsonParser().parseResource(Patient.class,
		    candidateJson) : null;
		return patientUploadImportService.findOpenmrsPatientByDuplicateReviewCandidateSnapshot(candidateFhir,
		    candidateLogicalId);
	}
	
	private String resolveCandidateIdentifierHint(MpiPatientDuplicateReviewCandidate row) {
		String candidateJson = StringUtils.trimToNull(row.getPatientResourceJson());
		if (candidateJson != null) {
			Patient candidateFhir = fhirContext.newJsonParser().parseResource(Patient.class, candidateJson);
			String id = patientUploadImportService.resolveDuplicateReviewCandidateIdentifierValue(candidateFhir);
			if (StringUtils.isNotBlank(id)) {
				return id;
			}
		}
		return StringUtils.trimToNull(row.getFhirPatientLogicalId());
	}
	
	private void assertSelectedOpenMrsCandidate(org.openmrs.Patient resolved, MpiPatientDuplicateReviewCandidate row,
	        String candidateLogicalId) {
		if (resolved == null) {
			return;
		}
		String resolvedUuid = resolved.getUuid();
		if (StringUtils.isNotBlank(candidateLogicalId)) {
			String lid = candidateLogicalId.trim();
			if (lid.equals(resolvedUuid)) {
				return;
			}
			if (lid.regionMatches(true, 0, "idval:", 0, 6)) {
				String idValue = lid.substring(6).trim();
				if (openMrsIdentifierValueMatches(resolved, idValue)) {
					return;
				}
			}
		}
		String candidateJson = StringUtils.trimToNull(row.getPatientResourceJson());
		if (candidateJson != null) {
			Patient candidateFhir = fhirContext.newJsonParser().parseResource(Patient.class, candidateJson);
			String expectedId = patientUploadImportService.resolveDuplicateReviewCandidateIdentifierValue(candidateFhir);
			if (openMrsIdentifierValueMatches(resolved, expectedId)) {
				return;
			}
			if (candidateFhir.getIdElement() != null && candidateFhir.getIdElement().hasIdPart()
			        && candidateFhir.getIdElement().getIdPart().trim().equals(resolvedUuid)) {
				return;
			}
		}
		if (StringUtils.isBlank(candidateLogicalId)) {
			return;
		}
		throw new IllegalArgumentException(
		        "Selected OpenMRS duplicate candidate does not match patient resolved by candidate identifier");
	}
	
	private static boolean openMrsIdentifierValueMatches(org.openmrs.Patient resolved, String identifierValue) {
		if (resolved == null || StringUtils.isBlank(identifierValue) || resolved.getIdentifiers() == null) {
			return false;
		}
		String expected = identifierValue.trim();
		for (PatientIdentifier pid : resolved.getIdentifiers()) {
			if (pid == null || pid.getVoided() || pid.getIdentifier() == null) {
				continue;
			}
			if (expected.equals(pid.getIdentifier().trim())) {
				return true;
			}
		}
		return false;
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
}
