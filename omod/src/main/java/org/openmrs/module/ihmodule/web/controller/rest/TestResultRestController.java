package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.TestResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/rest/v1/test-result")
public class TestResultRestController {
	
	@RequestMapping(value = "/result/{personUuid}", method = RequestMethod.GET)
	public ResponseEntity<?> findByPatientAllTestResult(@PathVariable String personUuid) throws Exception {
		
		return new ResponseEntity<>(Context.getService(TestResultService.class).findByPatientAllTestResult(personUuid), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/visit/{visit}", method = RequestMethod.GET)
	public ResponseEntity<?> findByVisitUUidPatientTestResult(@PathVariable String visit) throws Exception {
		
		return new ResponseEntity<>(Context.getService(TestResultService.class).findByVisitUUidPatientTestResult(visit), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/document/{personUuid}", method = RequestMethod.GET)
	public ResponseEntity<?> findByPatientAllTestDocument(@PathVariable String personUuid) throws Exception {
		
		return new ResponseEntity<>(Context.getService(TestResultService.class).findByPatientAllTestDocument(personUuid), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/visit/document/{visit}", method = RequestMethod.GET)
	public ResponseEntity<?> findByVisitUUidPatientTestDocument(@PathVariable String visit) throws Exception {
		
		return new ResponseEntity<>(Context.getService(TestResultService.class).findByVisitUUidPatientTestDocument(visit), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/downloadImage/{filename}", method = RequestMethod.GET)
	public void downlaodImage(HttpServletRequest req, HttpServletResponse resp, @PathVariable String filename)
	        throws Exception {
		
		resp.setContentType("application/pdf");
		resp.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		
		// Then copy the stream, for example using IOUtils.copy ...
		// lookup the URL from the bits after /dl/*
		// URL url = new URL("/opt/multimedia/" + filename);
		InputStream in = new FileInputStream("/opt/multimedia/" + filename);
		// in = url.openConnection().getInputStream();
		IOUtils.copy(in, resp.getOutputStream());
		in.close();
		
	}
}
