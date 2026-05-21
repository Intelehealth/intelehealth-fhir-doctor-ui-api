package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.User;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.DuplicateReviewCaseActionRequest;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.DuplicateReviewCandidateActionRequest;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.ForcePatientSyncRequest;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.LocalMpiUpdateRequest;
import org.openmrs.module.ihmodule.api.patientexchange.export.CreatedPatientExportResult;
import org.openmrs.module.ihmodule.api.patientexchange.export.CreatedPatientExportService;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportResponse;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportService;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.ForceSyncDuplicateResolutionContext;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewPatientActionService;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewQueryService;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewResolutionService;
import org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR;
import org.openmrs.module.ihmodule.api.patientexchange.scheduler.ResourceIsNotValid;
import org.openmrs.module.ihmodule.api.patientexchange.service.LocalMpiAlreadySetException;
import org.openmrs.module.ihmodule.api.patientexchange.service.LocalPatientMpiUpdateService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * REST API: patient exchange proxy (duplicate review, import, export).
 * <p>
 * <b>Base URL:</b> {@code openmrsBase}/ws/rest/v1/ihmodule/patient-exchange/...} (also legacy
 * {@code module/ihmodule/patientExchange*.form}). JSON handlers use {@link HttpServletResponse} for
 * Servlet 3 / Tomcat 7 compatibility.
 * <p>
 * <b>Active endpoints (non-deprecated):</b>
 * <table border="1" summary="Active endpoints">
 * <tr>
 * <th>Method</th>
 * <th>Path</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>GET</td>
 * <td>{@code .../pending}</td>
 * <td>List pending duplicate-review cases</td>
 * </tr>
 * <tr>
 * <td>GET</td>
 * <td>{@code .../candidates?caseUuid=}</td>
 * <td>OpenMRS + FHIR candidates for a case</td>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>{@code .../force-sync}</td>
 * <td>Register new patient (with caseUuid) or local upsert by patientUuid</td>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>{@code .../duplicate-review/add-patient-candidate}</td>
 * <td>Link and join — enrich existing candidate</td>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>{@code .../duplicate-review/skip}</td>
 * <td>Skip case or single candidate</td>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>{@code .../import-upload}</td>
 * <td>Multipart FHIR Patient/Bundle import</td>
 * </tr>
 * <tr>
 * <td>GET</td>
 * <td>{@code .../export-created?startDate=&amp;endDate=}</td>
 * <td>Export created patients (FHIR JSON attachment)</td>
 * </tr>
 * </table>
 * <p>
 * <b>Deprecated (not documented in API reference):</b> {@code GET .../cases/statistics},
 * {@code POST .../mpi-local}.
 * <p>
 * Auth: OpenMRS session. Errors: {@code "error":"..."}} JSON.
 * <p>
 * Full reference: {@code docs/ihmodule-rest-api-documentation.md} (section 2).
 */
@Controller
public class PatientExchangeProxyRestController {
	
	private static final Log log = LogFactory.getLog(PatientExchangeProxyRestController.class);
	
	public static final String GP_PATIENT_EXCHANGE_BASE_URL = "ihmodule.patientexchange.baseUrl";
	
	private static final Pattern DATE_YYYY_MM_DD = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	
	@Autowired
	private MpiDuplicateReviewQueryService mpiDuplicateReviewQueryService;
	
	@Autowired
	private DataSendToFHIR dataSendToFHIR;
	
	@Autowired
	private LocalPatientMpiUpdateService localPatientMpiUpdateService;
	
	@Autowired
	private MpiDuplicateReviewResolutionService mpiDuplicateReviewResolutionService;
	
	@Autowired
	private PatientUploadImportService patientUploadImportService;
	
	@Autowired
	private MpiDuplicateReviewPatientActionService mpiDuplicateReviewPatientActionService;
	
	@Autowired
	private CreatedPatientExportService createdPatientExportService;
	
	private ObjectMapper objectMapper = new ObjectMapper();
	
	private boolean rejectIfUnauthorized(HttpServletResponse response) throws IOException {
		if (!Context.isAuthenticated()) {
			log.warn("ihmodule patientExchange proxy: request rejected — not authenticated");
			writeJson(response, HttpStatus.UNAUTHORIZED.value(), errorBody("Not authenticated"));
			return true;
		}
		return false;
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangePending.form", "/rest/v1/ihmodule/patient-exchange/pending" }, method = RequestMethod.GET)
	public void pendingCases(HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (log.isDebugEnabled()) {
			User u = Context.getAuthenticatedUser();
			log.debug("ihmodule patientExchange: pending cases requested by user=" + (u != null ? u.getUsername() : "?"));
		}
		writeJson(response, HttpStatus.OK.value(), mpiDuplicateReviewQueryService.listPendingCases(), true);
	}
	
	/**
	 * @deprecated Aggregate duplicate-review case counts. Still used by
	 *             {@code duplicatePatientReview.js}; do not use from new integrations. Legacy form
	 *             URL {@code patientExchangeDupStatistics.form} is deprecated as well.
	 */
	@Deprecated
	@RequestMapping(value = { "module/ihmodule/patientExchangeDupStatistics.form",
	        "/rest/v1/ihmodule/patient-exchange/cases/statistics" }, method = RequestMethod.GET)
	public void duplicateReviewStatistics(HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		log.warn("DEPRECATED endpoint cases/statistics called; GET /rest/v1/ihmodule/patient-exchange/cases/statistics and patientExchangeDupStatistics.form are deprecated");
		writeJson(response, HttpStatus.OK.value(), mpiDuplicateReviewQueryService.getCaseStatistics());
	}
	
	/**
	 * Duplicate-review candidates for a case. Response JSON: {@code "openmrsCandidates": [...],
	 * "fhirCandidates": [...] } — grouped by candidate {@code match_source} ({@code openmrs} vs
	 * central / {@code fhir}).
	 */
	@RequestMapping(value = { "module/ihmodule/patientExchangeCandidates.form",
	        "/rest/v1/ihmodule/patient-exchange/candidates" }, method = RequestMethod.GET)
	public void candidates(@RequestParam("caseUuid") String caseUuid, HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (caseUuid == null || caseUuid.trim().isEmpty()) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("caseUuid is required"));
			return;
		}
		try {
			writeJson(response, HttpStatus.OK.value(),
			    mpiDuplicateReviewQueryService.listCandidatesForCaseUuid(caseUuid.trim()), true);
		}
		catch (IllegalArgumentException ex) {
			writeJson(response, HttpStatus.NOT_FOUND.value(), errorBody(ex.getMessage()));
		}
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeForceSync.form",
	        "/rest/v1/ihmodule/patient-exchange/force-sync" }, method = RequestMethod.POST, consumes = "application/json")
	public void forceSync(@RequestBody(required = false) ForcePatientSyncRequest body, HttpServletResponse response)
			throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (body == null || StringUtils.isBlank(body.getResolvedBy())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("resolvedBy is required"));
			return;
		}
		boolean hasCaseUuid = StringUtils.isNotBlank(body.getCaseUuid());
		if (!hasCaseUuid && StringUtils.isBlank(body.getPatientUuid())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("patientUuid is required unless caseUuid is set"));
			return;
		}
		ForceSyncDuplicateResolutionContext.begin(body.getResolvedBy().trim());
		try {
			if (log.isDebugEnabled()) {
				User u = Context.getAuthenticatedUser();
				log.debug("ihmodule patientExchange: add-patient / force-sync requested by user="
				        + (u != null ? u.getUsername() : "?") + ", patientUuid=" + StringUtils.trimToEmpty(body.getPatientUuid())
				        + ", caseUuid=" + StringUtils.trimToEmpty(body.getCaseUuid()));
			}
			org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse res;
			if (hasCaseUuid) {
				mpiDuplicateReviewPatientActionService.addPatientFromPendingCase(body.getCaseUuid().trim(),
				    body.getPatientUuid() != null ? body.getPatientUuid().trim() : null, body.getResolvedBy().trim());
				res = new org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse();
				res.setStatusCode("200");
				res.setMessage("Duplicate-review case completed (add patient from pending)");
				res.setResponse(null);
			} else {
				res = dataSendToFHIR.forceSendPatientToCentralByUuid(body.getPatientUuid().trim());
			}
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("patientUuid", body.getPatientUuid() != null ? body.getPatientUuid().trim() : null);
			ok.put("caseUuid", body.getCaseUuid() != null ? body.getCaseUuid().trim() : null);
			ok.put("statusCode", res.getStatusCode());
			ok.put("message", res.getMessage());
			ok.put("response", res.getResponse());
			if ("skipped".equals(res.getStatusCode())) {
				ok.put("status", "skipped");
			}
			writeJson(response, HttpStatus.OK.value(), ok);
		} catch (IllegalArgumentException ex) {
			log.warn("ihmodule patientExchange: force-sync bad request, message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		} catch (IllegalStateException ex) {
			log.warn("ihmodule patientExchange: force-sync illegal state, message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.CONFLICT.value(), errorBody(ex.getMessage()));
		} catch (ResourceIsNotValid ex) {
			log.warn("ihmodule patientExchange: force-sync unprocessable, patientUuid=" + StringUtils.trimToEmpty(body.getPatientUuid())
			        + ", message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.UNPROCESSABLE_ENTITY.value(), errorBody(ex.getMessage()));
		} catch (Exception ex) {
			log.error("ihmodule patientExchange: force-sync failed, patientUuid=" + StringUtils.trimToEmpty(body.getPatientUuid()), ex);
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		} finally {
			ForceSyncDuplicateResolutionContext.end();
		}
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeDuplicateReviewSkip.form",
	        "/rest/v1/ihmodule/patient-exchange/duplicate-review/skip" }, method = RequestMethod.POST, consumes = "application/json")
	public void duplicateReviewSkip(@RequestBody(required = false) DuplicateReviewCaseActionRequest body,
	        HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (body == null || StringUtils.isBlank(body.getCaseUuid()) || StringUtils.isBlank(body.getResolvedBy())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("caseUuid and resolvedBy are required"));
			return;
		}
		try {
			String caseUuid = body.getCaseUuid().trim();
			String resolvedBy = body.getResolvedBy().trim();
			if (body.getCandidateId() != null) {
				mpiDuplicateReviewPatientActionService.skipCandidateByCaseUuid(caseUuid, body.getCandidateId(), resolvedBy);
			} else {
				mpiDuplicateReviewPatientActionService.skipCaseByCaseUuid(caseUuid, resolvedBy);
			}
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("status", "ok");
			ok.put("caseUuid", caseUuid);
			if (body.getCandidateId() != null) {
				ok.put("candidateId", body.getCandidateId());
				ok.put("scope", "candidate");
			} else {
				ok.put("scope", "case");
			}
			writeJson(response, HttpStatus.OK.value(), ok);
		}
		catch (IllegalArgumentException ex) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (IllegalStateException ex) {
			writeJson(response, HttpStatus.CONFLICT.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			log.error("duplicate-review skip failed", ex);
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		}
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeDuplicateReviewAddCandidate.form",
	        "/rest/v1/ihmodule/patient-exchange/duplicate-review/add-patient-candidate" }, method = RequestMethod.POST, consumes = "application/json")
	public void duplicateReviewAddPatientCandidate(@RequestBody(required = false) DuplicateReviewCandidateActionRequest body,
	        HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (body == null || StringUtils.isBlank(body.getCaseUuid()) || body.getCandidateId() == null
		        || StringUtils.isBlank(body.getResolvedBy())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("caseUuid, candidateId, and resolvedBy are required"));
			return;
		}
		try {
			mpiDuplicateReviewPatientActionService.addPatientFromCandidate(body.getCaseUuid().trim(), body.getCandidateId(),
			    body.getResolvedBy().trim());
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("status", "ok");
			ok.put("caseUuid", body.getCaseUuid().trim());
			ok.put("candidateId", body.getCandidateId());
			writeJson(response, HttpStatus.OK.value(), ok);
		}
		catch (IllegalArgumentException ex) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (IllegalStateException ex) {
			writeJson(response, HttpStatus.CONFLICT.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			log.error("duplicate-review add-patient-candidate failed", ex);
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		}
	}
	
	/**
	 * Manually sets MPI on a local patient and resolves a pending duplicate-review case.
	 * 
	 * @deprecated Not used by {@code duplicatePatientReview.js}. Prefer
	 *             {@link #duplicateReviewAddPatientCandidate} at
	 *             {@code POST /rest/v1/ihmodule/patient-exchange/duplicate-review/add-patient-candidate}
	 *             . Legacy form URL {@code patientExchangeMpiLocal.form} is deprecated as well.
	 */
	@Deprecated
	@RequestMapping(value = { "module/ihmodule/patientExchangeMpiLocal.form", "/rest/v1/ihmodule/patient-exchange/mpi-local" }, method = RequestMethod.POST, consumes = "application/json")
	public void mpiLocal(@RequestBody(required = false) LocalMpiUpdateRequest body, HttpServletResponse response)
	        throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		log.warn("DEPRECATED endpoint mpi-local called; use POST /rest/v1/ihmodule/patient-exchange/duplicate-review/add-patient-candidate instead");
		if (body == null || StringUtils.isBlank(body.getPatientUuid())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("patientUuid is required"));
			return;
		}
		if (StringUtils.isBlank(body.getMpiIdentifierValue())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("mpiIdentifierValue is required"));
			return;
		}
		if (StringUtils.isBlank(body.getResolvedBy())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("resolvedBy is required"));
			return;
		}
		try {
			if (log.isDebugEnabled()) {
				User u = Context.getAuthenticatedUser();
				log.debug("ihmodule patientExchange: mpi-local requested by user=" + (u != null ? u.getUsername() : "?")
				        + ", patientUuid=" + body.getPatientUuid().trim() + ", chosenFhirPatientLogicalId="
				        + body.getChosenFhirPatientLogicalId());
			}
			localPatientMpiUpdateService.applyMpiIdentifierToLocalPatient(body.getPatientUuid().trim(), body
			        .getMpiIdentifierValue().trim());
			try {
				mpiDuplicateReviewResolutionService.resolvePendingCaseAfterLocalMpiUpdate(body.getPatientUuid().trim(), body
				        .getMpiIdentifierValue().trim(), body.getChosenFhirPatientLogicalId(), body.getResolvedBy().trim());
			}
			catch (RuntimeException ex) {
				log.warn("Duplicate-review resolution after local MPI failed for patient " + body.getPatientUuid() + ": "
				        + ex.getMessage(), ex);
			}
			Map<String, Object> ok = mpiLocalDeprecatedResponseBody();
			ok.put("status", "ok");
			ok.put("patientUuid", body.getPatientUuid().trim());
			ok.put("mpiIdentifierValue", body.getMpiIdentifierValue().trim());
			writeJson(response, HttpStatus.OK.value(), ok);
		}
		catch (LocalMpiAlreadySetException ex) {
			log.info("ihmodule patientExchange: mpi-local skipped, patientUuid=" + body.getPatientUuid().trim()
			        + ", message=" + ex.getMessage());
			Map<String, Object> skipped = mpiLocalDeprecatedResponseBody();
			skipped.put("status", "skipped");
			skipped.put("patientUuid", body.getPatientUuid().trim());
			skipped.put("message", ex.getMessage());
			writeJson(response, HttpStatus.OK.value(), skipped);
		}
		catch (IllegalArgumentException ex) {
			log.warn("ihmodule patientExchange: mpi-local bad request, patientUuid=" + body.getPatientUuid().trim()
			        + ", message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (IllegalStateException ex) {
			log.warn("ihmodule patientExchange: mpi-local not found, patientUuid=" + body.getPatientUuid().trim()
			        + ", message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.NOT_FOUND.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			log.error("ihmodule patientExchange: mpi-local failed, patientUuid=" + body.getPatientUuid().trim(), ex);
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		}
	}
	
	/**
	 * Proxies {@code POST /patientinfo/rest/v1/patient/import/upload} (multipart {@code file} →
	 * FHIR Patient or Bundle JSON).
	 */
	@RequestMapping(value = { "module/ihmodule/patientExchangeImportUpload.form",
	        "/rest/v1/ihmodule/patient-exchange/import-upload" }, method = RequestMethod.POST)
	public void importUpload(@RequestParam(value = "file", required = false) MultipartFile file,
	        @RequestParam(value = "locationUuid", required = false) String locationUuid, HttpServletResponse response)
	        throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (file == null || file.isEmpty()) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("file is required"));
			return;
		}
		try {
			if (log.isDebugEnabled()) {
				User u = Context.getAuthenticatedUser();
				log.debug("ihmodule patientExchange: import-upload requested by user=" + (u != null ? u.getUsername() : "?")
				        + ", locationUuid=" + locationUuid + ", filename=" + file.getOriginalFilename());
			}
			PatientUploadImportResponse out = patientUploadImportService.importPatientFile(file, locationUuid);
			writeJson(response, HttpStatus.OK.value(), out);
		}
		catch (IllegalArgumentException ex) {
			log.warn("ihmodule patientExchange: import-upload bad request, locationUuid=" + locationUuid + ", filename="
			        + file.getOriginalFilename() + ", message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			log.error(
			    "ihmodule patientExchange: import-upload failed, locationUuid=" + locationUuid + ", filename="
			            + file.getOriginalFilename(), ex);
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		}
	}
	
	/**
	 * Proxies {@code GET /patientinfo/rest/v1/patient/export/created} ({@code startDate},
	 * {@code endDate} as {@code yyyy-MM-dd}).
	 */
	@RequestMapping(value = { "module/ihmodule/patientExchangeExportCreated.form",
	        "/rest/v1/ihmodule/patient-exchange/export-created" }, method = RequestMethod.GET)
	public void exportCreated(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate,
	        HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		String sd = startDate != null ? startDate.trim() : "";
		String ed = endDate != null ? endDate.trim() : "";
		if (!DATE_YYYY_MM_DD.matcher(sd).matches() || !DATE_YYYY_MM_DD.matcher(ed).matches()) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("startDate and endDate must be yyyy-MM-dd"));
			return;
		}
		try {
			if (log.isDebugEnabled()) {
				User u = Context.getAuthenticatedUser();
				log.debug("ihmodule patientExchange: export-created requested by user="
				        + (u != null ? u.getUsername() : "?") + ", startDate=" + sd + ", endDate=" + ed);
			}
			CreatedPatientExportResult result = createdPatientExportService.exportCreatedPatients(sd, ed);
			byte[] payload = result.getPayload() != null ? result.getPayload().getBytes(StandardCharsets.UTF_8)
			        : new byte[0];
			response.resetBuffer();
			response.setStatus(HttpStatus.OK.value());
			response.setContentType("application/fhir+json;charset=UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"created-patients-" + sd + "-to-" + ed
			        + ".json\"");
			response.setHeader("X-Total-Patients", Integer.toString(result.getTotalPatients()));
			response.setHeader("X-Exported-Patients", Integer.toString(result.getExportedPatients()));
			response.setHeader("X-Validation-Failed-Patients", Integer.toString(result.getValidationFailedPatients()));
			response.setContentLength(payload.length);
			response.getOutputStream().write(payload);
			response.getOutputStream().flush();
		}
		catch (IllegalArgumentException ex) {
			log.warn("ihmodule patientExchange: export-created bad request, startDate=" + sd + ", endDate=" + ed
			        + ", message=" + ex.getMessage(), ex);
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			log.error("ihmodule patientExchange: export-created failed, startDate=" + sd + ", endDate=" + ed, ex);
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		}
	}
	
	private void writeJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
		writeJson(response, statusCode, body, false);
	}
	
	private void writeJson(HttpServletResponse response, int statusCode, Object body, boolean noCache) throws IOException {
		byte[] payload = objectMapper.writeValueAsBytes(body);
		response.resetBuffer();
		response.setStatus(statusCode);
		response.setContentType("application/json;charset=UTF-8");
		if (noCache) {
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
			response.setHeader("Pragma", "no-cache");
		}
		response.setContentLength(payload.length);
		response.getOutputStream().write(payload);
		response.getOutputStream().flush();
	}
	
	private static Map<String, Object> mpiLocalDeprecatedResponseBody() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("deprecated", true);
		m.put("replacement",
		    "/rest/v1/ihmodule/patient-exchange/duplicate-review/add-patient-candidate");
		return m;
	}
	
	private static Map<String, Object> errorBody(String message) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error", message != null ? message : "Unexpected error");
		return m;
	}
}
