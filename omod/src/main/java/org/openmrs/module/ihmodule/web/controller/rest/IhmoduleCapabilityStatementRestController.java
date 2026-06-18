package org.openmrs.module.ihmodule.web.controller.rest;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.capability.IhmoduleCapabilityStatementService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Publishes the ihmodule interface CapabilityStatement (FHIR-shaped JSON).
 * <p>
 * ihmodule is not a FHIR server; this documents REST and outbound FHIR client capabilities for
 * integration planning.
 */
@Controller
public class IhmoduleCapabilityStatementRestController {
	
	@RequestMapping(value = { "/rest/v1/ihmodule/capability-statement", "module/ihmodule/capabilityStatement.form" }, method = RequestMethod.GET, produces = {
	        MediaType.APPLICATION_JSON_VALUE, "application/fhir+json" })
	public ResponseEntity<?> getCapabilityStatement() {
		try {
			IhmoduleCapabilityStatementService service = Context.getRegisteredComponent(
			    "ihmoduleCapabilityStatementService", IhmoduleCapabilityStatementService.class);
			return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(service.getInterfaceCapability());
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Object>("Request failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
