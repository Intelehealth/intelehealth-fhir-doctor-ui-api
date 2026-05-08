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
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.ForcePatientSyncRequest;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.LocalMpiUpdateRequest;
import org.openmrs.module.ihmodule.api.patientexchange.export.CreatedPatientExportResult;
import org.openmrs.module.ihmodule.api.patientexchange.export.CreatedPatientExportService;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportResponse;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportService;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.ForceSyncDuplicateResolutionContext;
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

/**
 * Serves patient exchange endpoints for duplicate-patient UI directly from ihmodule services.
 * <p>
 * Mapped both as legacy {@code *.form} URLs and under {@code /rest/v1/ihmodule/patient-exchange/*},
 * which resolves to {@code /ws/rest/v1/ihmodule/patient-exchange/*} on the server — same
 * DispatcherServlet prefix as {@link PatientRestController}. That path is CSRFGuard-unprotected in
 * OpenMRS so JSON POSTs work.
 * <p>
 * Handlers write JSON via {@link HttpServletResponse} directly (not {@code ResponseEntity}) for
 * Servlet 3.0 / Tomcat 7 compatibility (avoids {@code setContentLengthLong}).
 */
@Controller
public class PatientExchangeProxyRestController {
	
	private static final Log log = LogFactory.getLog(PatientExchangeProxyRestController.class);
	
	public static final String GP_PATIENT_EXCHANGE_BASE_URL = "ihmodule.patientexchange.baseUrl";
	
	private static final Pattern DATE_YYYY_MM_DD = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	
	private MpiDuplicateReviewQueryService mpiDuplicateReviewQueryService = Context.getRegisteredComponent(
	    "mpiDuplicateReviewQueryService", MpiDuplicateReviewQueryService.class);
	
	private DataSendToFHIR dataSendToFHIR = Context.getRegisteredComponent("dataSendToFHIR", DataSendToFHIR.class);
	
	private LocalPatientMpiUpdateService localPatientMpiUpdateService = Context.getRegisteredComponent(
	    "localPatientMpiUpdateService", LocalPatientMpiUpdateService.class);
	
	private MpiDuplicateReviewResolutionService mpiDuplicateReviewResolutionService = Context.getRegisteredComponent(
	    "mpiDuplicateReviewResolutionService", MpiDuplicateReviewResolutionService.class);
	
	private PatientUploadImportService patientUploadImportService = Context.getRegisteredComponent(
	    "patientUploadImportService", PatientUploadImportService.class);
	
	private CreatedPatientExportService createdPatientExportService = Context.getRegisteredComponent(
	    "createdPatientExportService", CreatedPatientExportService.class);
	
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
		writeJson(response, HttpStatus.OK.value(), mpiDuplicateReviewQueryService.listPendingCases());
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeDupStatistics.form",
	        "/rest/v1/ihmodule/patient-exchange/cases/statistics" }, method = RequestMethod.GET)
	public void duplicateReviewStatistics(HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		writeJson(response, HttpStatus.OK.value(), mpiDuplicateReviewQueryService.getCaseStatistics());
	}
	
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
			    mpiDuplicateReviewQueryService.listCandidatesForCaseUuid(caseUuid.trim()));
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
		if (body == null || StringUtils.isBlank(body.getPatientUuid())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("patientUuid is required"));
			return;
		}
		if (StringUtils.isBlank(body.getResolvedBy())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("resolvedBy is required"));
			return;
		}
		ForceSyncDuplicateResolutionContext.begin(body.getResolvedBy().trim());
		try {
			org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse res = dataSendToFHIR
					.forceSendPatientToCentralByUuid(body.getPatientUuid().trim());
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("patientUuid", body.getPatientUuid().trim());
			ok.put("statusCode", res.getStatusCode());
			ok.put("message", res.getMessage());
			ok.put("response", res.getResponse());
			if ("skipped".equals(res.getStatusCode())) {
				ok.put("status", "skipped");
			}
			writeJson(response, HttpStatus.OK.value(), ok);
		} catch (IllegalArgumentException ex) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		} catch (ResourceIsNotValid ex) {
			writeJson(response, HttpStatus.UNPROCESSABLE_ENTITY.value(), errorBody(ex.getMessage()));
		} catch (Exception ex) {
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		} finally {
			ForceSyncDuplicateResolutionContext.end();
		}
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeMpiLocal.form", "/rest/v1/ihmodule/patient-exchange/mpi-local" }, method = RequestMethod.POST, consumes = "application/json")
	public void mpiLocal(@RequestBody(required = false) LocalMpiUpdateRequest body, HttpServletResponse response)
			throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
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
			localPatientMpiUpdateService.applyMpiIdentifierToLocalPatient(body.getPatientUuid().trim(),
					body.getMpiIdentifierValue().trim());
			try {
				mpiDuplicateReviewResolutionService.resolvePendingCaseAfterLocalMpiUpdate(body.getPatientUuid().trim(),
						body.getMpiIdentifierValue().trim(), body.getChosenFhirPatientLogicalId(),
						body.getResolvedBy().trim());
			} catch (RuntimeException ex) {
				log.warn("Duplicate-review resolution after local MPI failed for patient "
						+ body.getPatientUuid() + ": " + ex.getMessage(), ex);
			}
			Map<String, Object> ok = new LinkedHashMap<>();
			ok.put("status", "ok");
			ok.put("patientUuid", body.getPatientUuid().trim());
			ok.put("mpiIdentifierValue", body.getMpiIdentifierValue().trim());
			writeJson(response, HttpStatus.OK.value(), ok);
		} catch (LocalMpiAlreadySetException ex) {
			Map<String, Object> skipped = new LinkedHashMap<>();
			skipped.put("status", "skipped");
			skipped.put("patientUuid", body.getPatientUuid().trim());
			skipped.put("message", ex.getMessage());
			writeJson(response, HttpStatus.OK.value(), skipped);
		} catch (IllegalArgumentException ex) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		} catch (IllegalStateException ex) {
			writeJson(response, HttpStatus.NOT_FOUND.value(), errorBody(ex.getMessage()));
		} catch (Exception ex) {
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
			PatientUploadImportResponse out = patientUploadImportService.importPatientFile(file, locationUuid);
			writeJson(response, HttpStatus.OK.value(), out);
		}
		catch (IllegalArgumentException ex) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
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
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody(ex.getMessage()));
		}
	}
	
	private void writeJson(HttpServletResponse response, int statusCode, Object body) throws IOException {
		byte[] payload = objectMapper.writeValueAsBytes(body);
		response.resetBuffer();
		response.setStatus(statusCode);
		response.setContentType("application/json;charset=UTF-8");
		response.setContentLength(payload.length);
		response.getOutputStream().write(payload);
		response.getOutputStream().flush();
	}
	
	private static Map<String, Object> errorBody(String message) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("error", message != null ? message : "Unexpected error");
		return m;
	}
}
