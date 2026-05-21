package org.openmrs.module.ihmodule.web.controller.rest;

import java.util.List;
import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.Referral;
import org.openmrs.module.ihmodule.api.ReferralService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/rest/v1/referral")
public class ReferralRestController {
	
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ResponseEntity<?> search(@RequestParam Map<String, String> reqParam)
	        throws Exception {
		
		List<Referral> data = Context.getService(ReferralService.class).search( reqParam);
		
		return new ResponseEntity<>(data, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/list/{uuid}", method = RequestMethod.GET)
	public ResponseEntity<?> list(@PathVariable("uuid") String uuid)
	        throws Exception {
		
		List<Referral> data = Context.getService(ReferralService.class).findAllByPatient(uuid);
		return new ResponseEntity<>(data, HttpStatus.OK);

	}
}
