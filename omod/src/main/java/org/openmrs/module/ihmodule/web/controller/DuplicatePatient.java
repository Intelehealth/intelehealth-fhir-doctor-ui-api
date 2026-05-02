package org.openmrs.module.ihmodule.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.web.controller.rest.PatientExchangeProxyRestController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/** Distinct Spring bean name (shared OpenMRS web application context). */
@Controller("ihmoduleDuplicatePatient")
public class DuplicatePatient {
	
	/** Spring Security CsrfToken request attribute (no compile dependency on spring-security-web). */
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
				/* CSRF shape differs or unavailable — AJAX posts skip header */
			}
		}
	}
	
	@RequestMapping(value = "/module/ihmodule/duplicatePatient.form", method = RequestMethod.GET)
	public void report(HttpServletRequest request, HttpSession session, Model model,
	        @RequestParam(required = false) Integer id) {
		
		exposeCsrfForAjax(request, model);
		String ctx = request.getContextPath();
		/* Under /ws/* CSRFGuard is off (OpenMRS csrfguard.properties); *.form POSTs get 302 without OWASP token. */
		model.addAttribute("patientExchangeProxyBase", ctx + "/ws/rest/v1/ihmodule/patient-exchange");
		model.addAttribute("resolvedByUsername", Context.getAuthenticatedUser() != null ? Context.getAuthenticatedUser()
		        .getUsername() : "");
		model.addAttribute("patientExchangeGpKey", PatientExchangeProxyRestController.GP_PATIENT_EXCHANGE_BASE_URL);
	}
	
}
