package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.User;
import org.openmrs.module.ihmodule.utils.IhmoduleProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Proxies the Spring Boot patient exchange service for the duplicate-patient UI.
 * <p>
 * Mapped both as legacy {@code *.form} URLs and under {@code /rest/v1/ihmodule/patient-exchange/*},
 * which resolves to {@code /ws/rest/v1/ihmodule/patient-exchange/*} on the server — same
 * DispatcherServlet prefix as {@link PatientRestController}. That path is CSRFGuard-unprotected in
 * OpenMRS so JSON POSTs work.
 * <p>
 * Handlers write JSON via {@link HttpServletResponse} directly (not {@code ResponseEntity}) for
 * Servlet 3.0 / Tomcat 7 compatibility (avoids {@code setContentLengthLong}).
 * <p>
 * Base URL resolution (first non-blank wins): OpenMRS global property
 * {@code ihmodule.patientexchange.baseUrl}, then {@code patientexchange.baseUrl} from bundled
 * {@code ihmodule.properties}, then {@link #DEFAULT_BASE}.
 */
@Controller
public class PatientExchangeProxyRestController {
	
	private static final Log log = LogFactory.getLog(PatientExchangeProxyRestController.class);
	
	public static final String GP_PATIENT_EXCHANGE_BASE_URL = "ihmodule.patientexchange.baseUrl";
	
	private static final String DEFAULT_BASE = "http://localhost:6001";
	
	private static final String MPI_DUP_PREFIX = "/patientinfo/rest/v1/mpi-duplicate-review";
	
	private static final String PATIENT_PREFIX = "/patientinfo/rest/v1/patient";
	
	private String baseUrl() {
		String gp = Context.getAdministrationService().getGlobalProperty(GP_PATIENT_EXCHANGE_BASE_URL);
		if (StringUtils.isNotBlank(gp)) {
			return gp.trim();
		}
		String fromBundle = IhmoduleProperties.getPatientExchangeBaseUrl();
		if (StringUtils.isNotBlank(fromBundle)) {
			return fromBundle.trim();
		}
		return DEFAULT_BASE;
	}
	
	private boolean rejectIfUnauthorized(HttpServletResponse response) throws IOException {
		if (!Context.isAuthenticated()) {
			log.warn("ihmodule patientExchange proxy: request rejected — not authenticated");
			byte[] b = "{\"error\":\"Not authenticated\"}".getBytes(StandardCharsets.UTF_8);
			response.resetBuffer();
			response.setStatus(401);
			response.setContentType("application/json;charset=UTF-8");
			response.setContentLength(b.length);
			response.getOutputStream().write(b);
			response.getOutputStream().flush();
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
			log.debug("ihmodule patientExchange: patientExchangePending.form user=" + (u != null ? u.getUsername() : "?")
			        + " upstreamBase=" + baseUrl());
		}
		PatientExchangeProxyHelper.proxyGet(response, baseUrl(), MPI_DUP_PREFIX + "/cases/pending");
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeCandidates.form",
	        "/rest/v1/ihmodule/patient-exchange/candidates" }, method = RequestMethod.GET)
	public void candidates(@RequestParam("caseUuid") String caseUuid, HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (caseUuid == null || caseUuid.trim().isEmpty()) {
			log.warn("ihmodule patientExchange: patientExchangeCandidates.form missing or empty caseUuid");
			byte[] b = "{\"error\":\"caseUuid is required\"}".getBytes(StandardCharsets.UTF_8);
			response.resetBuffer();
			response.setStatus(400);
			response.setContentType("application/json;charset=UTF-8");
			response.setContentLength(b.length);
			response.getOutputStream().write(b);
			response.getOutputStream().flush();
			return;
		}
		String enc = java.net.URLEncoder.encode(caseUuid.trim(), "UTF-8");
		PatientExchangeProxyHelper.proxyGet(response, baseUrl(), MPI_DUP_PREFIX + "/cases/" + enc + "/candidates");
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeForceSync.form",
	        "/rest/v1/ihmodule/patient-exchange/force-sync" }, method = RequestMethod.POST, consumes = "application/json")
	public void forceSync(@RequestBody(required = false) String body, HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		PatientExchangeProxyHelper.proxyPostJson(response, baseUrl(), PATIENT_PREFIX + "/sync/force", body != null ? body
		        : "{}");
	}
	
	@RequestMapping(value = { "module/ihmodule/patientExchangeMpiLocal.form", "/rest/v1/ihmodule/patient-exchange/mpi-local" }, method = RequestMethod.POST, consumes = "application/json")
	public void mpiLocal(@RequestBody(required = false) String body, HttpServletResponse response) throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		PatientExchangeProxyHelper.proxyPostJson(response, baseUrl(), PATIENT_PREFIX + "/mpi/local", body != null ? body
		        : "{}");
	}
	
	/**
	 * Proxies {@code POST /patientinfo/rest/v1/patient/import/upload} (multipart {@code file} →
	 * FHIR Patient or Bundle JSON).
	 */
	@RequestMapping(value = { "module/ihmodule/patientExchangeImportUpload.form",
	        "/rest/v1/ihmodule/patient-exchange/import-upload" }, method = RequestMethod.POST)
	public void importUpload(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletResponse response)
	        throws IOException {
		if (rejectIfUnauthorized(response)) {
			return;
		}
		if (file == null || file.isEmpty()) {
			byte[] b = "{\"error\":\"file is required\"}".getBytes(StandardCharsets.UTF_8);
			response.resetBuffer();
			response.setStatus(400);
			response.setContentType("application/json;charset=UTF-8");
			response.setContentLength(b.length);
			response.getOutputStream().write(b);
			response.getOutputStream().flush();
			return;
		}
		byte[] bytes = file.getBytes();
		PatientExchangeProxyHelper.proxyPostMultipartFile(response, baseUrl(), PATIENT_PREFIX + "/import/upload", bytes,
		    file.getOriginalFilename());
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
		if (!sd.matches("\\d{4}-\\d{2}-\\d{2}") || !ed.matches("\\d{4}-\\d{2}-\\d{2}")) {
			byte[] b = "{\"error\":\"startDate and endDate must be yyyy-MM-dd\"}".getBytes(StandardCharsets.UTF_8);
			response.resetBuffer();
			response.setStatus(400);
			response.setContentType("application/json;charset=UTF-8");
			response.setContentLength(b.length);
			response.getOutputStream().write(b);
			response.getOutputStream().flush();
			return;
		}
		String q = "?startDate=" + sd + "&endDate=" + ed;
		PatientExchangeProxyHelper.proxyGetForwardHeaders(response, baseUrl(), PATIENT_PREFIX + "/export/created" + q);
	}
}
