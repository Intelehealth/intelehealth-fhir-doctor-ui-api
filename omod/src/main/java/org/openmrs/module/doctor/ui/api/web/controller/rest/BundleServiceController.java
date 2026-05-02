package org.openmrs.module.doctor.ui.api.web.controller.rest;

import org.openmrs.api.context.Context;
import org.openmrs.module.doctor.ui.api.api.BundleService;
import org.openmrs.module.doctor.ui.api.utils.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/rest/v1/shr/bundle")
public class BundleServiceController {
	
	@RequestMapping(value = "/{resourceType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> search(@PathVariable String resourceType, @RequestParam Map<String, String> reqParam)
			throws Exception {
		System.err.println("/resourceType : {"+resourceType+"} - " + reqParam);

		HttpResponse response = Context.getService(BundleService.class).getBundle(resourceType, reqParam);

		return new ResponseEntity<>(response.getResponse(), HttpStatus.valueOf(response.getStatusCode()));
	}
}
