package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientmatch.service.FhirPatientMatchServicePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ca.uhn.fhir.context.FhirContext;

/**
 * REST API: local OpenMRS FHIR R4 patient {@code $match} (fuzzy duplicate detection).
 * <p>
 * <b>Base URL:</b> {@code openmrsBase}/ws/rest/v1/ihmodule/patient/$match}
 * <p>
 * <table border="1" summary="Active endpoints">
 * <tr>
 * <th>Method</th>
 * <th>Path</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>{@code /rest/v1/ihmodule/patient/$match}</td>
 * <td>Match input Patient/Parameters against local DB; returns FHIR Bundle (searchset)</td>
 * </tr>
 * <tr>
 * <td>GET, etc.</td>
 * <td>same</td>
 * <td>405 — only POST allowed</td>
 * </tr>
 * </table>
 * Legacy alias: {@code module/ihmodule/patientFuzzyMatch.form}.
 * <p>
 * Auth: OpenMRS session (401 if not authenticated). Request: {@code application/fhir+json}.
 * Response: FHIR {@code OperationOutcome} on error.
 * <p>
 * Full reference: {@code docs/ihmodule-rest-api-documentation.md} (section 1).
 * 
 * @see FhirPatientMatchService
 */
@Controller
public class FuzzyPatientMatchRestController {
	
	private static final Log log = LogFactory.getLog(FuzzyPatientMatchRestController.class);
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private FhirPatientMatchServicePort fuzzyPatientMatchService;
	
	@RequestMapping(value = { "module/ihmodule/patientFuzzyMatch.form", "/rest/v1/ihmodule/patient/$match" }, method = RequestMethod.POST)
	public void patientMatch(@RequestBody(required = false) String body, HttpServletResponse response) throws IOException {
		if (!Context.isAuthenticated()) {
			writeFhir(response, HttpStatus.UNAUTHORIZED.value(),
			    fuzzyPatientMatchService.errorOutcome("not-authenticated", "Not authenticated"));
			return;
		}
		try {
			writeFhir(response, HttpStatus.OK.value(), fuzzyPatientMatchService.match(body));
		}
		catch (IllegalArgumentException ex) {
			log.warn("ihmodule fuzzy patient match bad request: " + ex.getMessage(), ex);
			writeFhir(response, HttpStatus.BAD_REQUEST.value(),
			    fuzzyPatientMatchService.errorOutcome("bad-request", ex.getMessage()));
		}
		catch (IllegalStateException ex) {
			log.warn("ihmodule fuzzy patient match unavailable: " + ex.getMessage(), ex);
			writeFhir(response, HttpStatus.SERVICE_UNAVAILABLE.value(),
			    fuzzyPatientMatchService.errorOutcome("service-unavailable", ex.getMessage()));
		}
		catch (Exception ex) {
			log.error("ihmodule fuzzy patient match failed", ex);
			writeFhir(response, HttpStatus.INTERNAL_SERVER_ERROR.value(),
			    fuzzyPatientMatchService.errorOutcome("internal-error", ex.getMessage()));
		}
	}
	
	@RequestMapping(value = { "module/ihmodule/patientFuzzyMatch.form", "/rest/v1/ihmodule/patient/$match" }, method = {
	        RequestMethod.GET, RequestMethod.HEAD, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE })
	public void patientMatchMethodNotAllowed(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String method = request != null ? request.getMethod() : null;
		log.warn("ihmodule fuzzy patient match does not support HTTP method " + method);
		response.setHeader("Allow", "POST");
		writeFhir(response, HttpStatus.METHOD_NOT_ALLOWED.value(),
		    fuzzyPatientMatchService.errorOutcome("method-not-allowed", "Only POST is supported for this endpoint"));
	}
	
	private void writeFhir(HttpServletResponse response, int statusCode, org.hl7.fhir.r4.model.Resource resource)
	        throws IOException {
		byte[] payload = fuzzyPatientMatchService.encodeResource(fhirContext, resource).getBytes(StandardCharsets.UTF_8);
		response.resetBuffer();
		response.setStatus(statusCode);
		response.setContentType("application/fhir+json;charset=UTF-8");
		response.setContentLength(payload.length);
		response.getOutputStream().write(payload);
		response.getOutputStream().flush();
	}
}
