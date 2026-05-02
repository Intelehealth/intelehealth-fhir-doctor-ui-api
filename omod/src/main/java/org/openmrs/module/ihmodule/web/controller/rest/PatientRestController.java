package org.openmrs.module.ihmodule.web.controller.rest;

import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.DataMigrationService;
import org.openmrs.module.ihmodule.utils.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ca.uhn.fhir.context.FhirContext;

@Controller
@RequestMapping("/rest/v1/member")
public class PatientRestController {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	@RequestMapping(value = "/{resourceType}", method = RequestMethod.GET)
	public ResponseEntity<?> getMember(@PathVariable("resourceType") String resourceType, @RequestParam Map<String, String> reqParam)
	        throws Exception {
		
		String data = Context.getService(DataMigrationService.class).getMember(resourceType, reqParam);
		//System.err.println("Data::" + data);
		Response res = new Response();
		res.setData(data);
		try {
			Bundle theBundle = fhirContext.newJsonParser().parseResource(
					Bundle.class, data);
			System.err.println("theBundle:::"+theBundle);
			
			System.err.println("DDD>>>>>>>>"
			        + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(theBundle));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return new ResponseEntity<>(res, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/import/{uuid}", method = RequestMethod.GET)
	public ResponseEntity<?> importMember(@PathVariable("uuid") String uuid)
	        throws Exception {
		System.out.println("UUID:::::::::::::::::::"+uuid);
		String data = Context.getService(DataMigrationService.class).importPatient(uuid);
		System.err.println("Data::" + data);
		Response res = new Response();
		res.setData(uuid);
		
		return new ResponseEntity<>(res, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/import/{uuid}/{location}", method = RequestMethod.GET)
	public ResponseEntity<?> importMemberInLocation(@PathVariable("uuid") String  uuid,@PathVariable("location") String location)
	        throws Exception {
		System.out.println("UUID:::::::::::::::::::"+uuid);
		String data = Context.getService(DataMigrationService.class).importPatient(uuid,location);
		System.err.println("Data::" + data);
		Response res = new Response();
		res.setData(uuid);
		
		return new ResponseEntity<>(res, HttpStatus.OK);

	}
}
