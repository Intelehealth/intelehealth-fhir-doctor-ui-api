package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.SourcePatientIdentifierUpdateRequest;
import org.openmrs.module.ihmodule.api.patientexchange.api.dto.SourcePatientIdentifierUpdateResponse;
import org.openmrs.module.ihmodule.api.patientexchange.service.LocalPatientMpiUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST API: upsert facility <b>Source Patient Id</b> (central FHIR Patient logical id).
 * <p>
 * <b>Base URL:</b> {@code openmrsBase}/ws/rest/v1/ihmodule/patient/source-identifier}
 * <p>
 * <table border="1" summary="Active endpoints">
 * <tr>
 * <th>Method</th>
 * <th>Path</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>POST</td>
 * <td>{@code /rest/v1/ihmodule/patient/source-identifier}</td>
 * <td>Create or update Source Patient Id on local patient</td>
 * </tr>
 * </table>
 * Legacy alias: {@code module/ihmodule/patientSourceIdentifier.form}.
 * <p>
 * Request JSON: {@code patientUuid}, {@code identifierValue} (required); {@code locationUuid}
 * (required when creating a new identifier row). Response:
 * {@link SourcePatientIdentifierUpdateResponse} or {@code "error":"..."} .
 * <p>
 * Full reference: {@code docs/ihmodule-rest-api-documentation.md} (section 3).
 * 
 * @see LocalPatientMpiUpdateService#upsertSourcePatientIdentifier
 */
@Controller
public class SourcePatientIdentifierRestController {
	
	private static final Log log = LogFactory.getLog(SourcePatientIdentifierRestController.class);
	
	@Autowired
	private LocalPatientMpiUpdateService localPatientMpiUpdateService;
	
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	@RequestMapping(value = { "module/ihmodule/patientSourceIdentifier.form", "/rest/v1/ihmodule/patient/source-identifier" }, method = RequestMethod.POST, consumes = "application/json")
	public void upsertSourcePatientIdentifier(@RequestBody(required = false) SourcePatientIdentifierUpdateRequest body,
	        HttpServletResponse response) throws IOException {
		if (!Context.isAuthenticated()) {
			writeJson(response, HttpStatus.UNAUTHORIZED.value(), errorBody("Not authenticated"));
			return;
		}
		if (body == null || StringUtils.isBlank(body.getPatientUuid())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("patientUuid is required"));
			return;
		}
		if (StringUtils.isBlank(body.getIdentifierValue())) {
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody("identifierValue is required"));
			return;
		}
		try {
			SourcePatientIdentifierUpdateResponse result = localPatientMpiUpdateService.upsertSourcePatientIdentifier(body
			        .getPatientUuid().trim(), body.getIdentifierValue().trim(), StringUtils.trimToNull(body
			        .getLocationUuid()));
			writeJson(response, HttpStatus.OK.value(), result);
		}
		catch (IllegalArgumentException ex) {
			log.warn("source-patient-identifier bad request: " + ex.getMessage(), ex);
			writeJson(response, HttpStatus.BAD_REQUEST.value(), errorBody(ex.getMessage()));
		}
		catch (IllegalStateException ex) {
			log.warn("source-patient-identifier not found or config error: " + ex.getMessage(), ex);
			int status = ex.getMessage() != null && ex.getMessage().contains("not found") ? HttpStatus.NOT_FOUND.value()
			        : HttpStatus.CONFLICT.value();
			writeJson(response, status, errorBody(ex.getMessage()));
		}
		catch (Exception ex) {
			log.error("source-patient-identifier failed for patientUuid=" + body.getPatientUuid(), ex);
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
