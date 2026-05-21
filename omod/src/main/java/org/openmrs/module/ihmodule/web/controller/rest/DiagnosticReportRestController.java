package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.OutputStream;
import java.util.Base64;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.DiagnosticReport;
import org.openmrs.module.ihmodule.api.DiagnosticReportService;
import org.openmrs.module.ihmodule.utils.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/rest/v1/diagnostic-report")
public class DiagnosticReportRestController {
	
	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> search(@RequestParam Map<String, String> reqParam) throws Exception {
		System.out.println("/search : " + reqParam);
		HttpResponse response = Context.getService(DiagnosticReportService.class).saveDiagnosticReport(reqParam);
		return new ResponseEntity<>(response.getResponse(), HttpStatus.valueOf(response.getStatusCode()));
	}
	
	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public void downloadReport(@RequestParam String patientId, @RequestParam String resourceId,
			HttpServletResponse response) throws Exception {

		System.out.println("/download : " + patientId + " : " + resourceId);

		try {

			if (patientId == null || resourceId == null || patientId.isEmpty() || resourceId.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
				return;
			}

			DiagnosticReport report = Context.getService(DiagnosticReportService.class).getReportData(patientId,
					resourceId);

			if (report == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid report found for this parameters");
				return;
			}

			// Decode the file data from Base64
			byte[] decodedFileData = Base64.getDecoder().decode(report.getFileData());

			// Set the response headers
			response.setContentType(report.getContentType());
			response.setHeader("Content-Disposition", "inline; filename=\"" + report.getFileName() + "\"");
			response.setContentLength(decodedFileData.length);

			try (OutputStream out = response.getOutputStream()) {
				out.write(decodedFileData);
				out.flush();
			}

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid report data.");
			} catch (Exception ignored) {
			}
		} catch (Exception e) {

			e.printStackTrace();

			try {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error occurred.");
			} catch (Exception ignored) {
			}
		}
	}
}
