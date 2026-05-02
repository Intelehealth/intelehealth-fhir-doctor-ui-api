package org.openmrs.module.ihmodule.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.module.ihmodule.web.controller.rest.PatientExchangeProxyRestController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * FHIR Patient JSON import/upload and created-patient export UI (proxied via patient-exchange
 * REST).
 */
@Controller("ihmodulePatientImportExport")
public class PatientImportExport {
	
	private static final String CSRF_TOKEN_REQUEST_ATTR = "org.springframework.security.web.csrf.CsrfToken";
	
	private static void exposeCsrfForAjax(HttpServletRequest request, Model model) {
		Object token = request.getAttribute(CSRF_TOKEN_REQUEST_ATTR);
		if (token == null) {
			token = request.getAttribute("_csrf");
		}
		if (token != null) {
			try {
				Object headerName = token.getClass().getMethod("getHeaderName").invoke(token);
				Object tokenValue = token.getClass().getMethod("getToken").invoke(token);
				if (headerName != null && tokenValue != null) {
					model.addAttribute("csrfHeaderName", headerName.toString());
					model.addAttribute("csrfTokenValue", tokenValue.toString());
				}
			}
			catch (ReflectiveOperationException e) {
				/* CSRF unavailable */
			}
		}
	}
	
	@RequestMapping(value = "/module/ihmodule/patientImportExport.form", method = RequestMethod.GET)
	public String patientImportExport(HttpServletRequest request, Model model) {
		exposeCsrfForAjax(request, model);
		String ctx = request.getContextPath();
		model.addAttribute("patientExchangeProxyBase", ctx + "/ws/rest/v1/ihmodule/patient-exchange");
		model.addAttribute("patientExchangeGpKey", PatientExchangeProxyRestController.GP_PATIENT_EXCHANGE_BASE_URL);
		return "/module/ihmodule/patientImportExport";
	}
}
