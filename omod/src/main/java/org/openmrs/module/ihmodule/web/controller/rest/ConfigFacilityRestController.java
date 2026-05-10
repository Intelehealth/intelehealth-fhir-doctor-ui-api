package org.openmrs.module.ihmodule.web.controller.rest;

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.ConfigFacility;
import org.openmrs.module.ihmodule.api.ConfigFacilityService;
import org.openmrs.module.ihmodule.dto.ConfigFacilityDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ca.uhn.fhir.context.FhirContext;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;

@Controller
@RequestMapping("/rest/v1/config-facility")
public class ConfigFacilityRestController<RequestAppointmentDTO> {
	
	FhirContext fhirContext = FhirContextHolder.R4;
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public ResponseEntity<?> save(@RequestBody ConfigFacility param) throws Exception {

		try {
			ConfigFacilityDTO dto = Context.getService(ConfigFacilityService.class).save(param);
			if (dto != null)
				return new ResponseEntity<>(dto, HttpStatus.OK);
			else {
				return new ResponseEntity<>("Couldn't save the config facility", HttpStatus.BAD_REQUEST);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("Request failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/getAll", method = RequestMethod.GET)
	public ResponseEntity<?> getAll() throws Exception {

		List<ConfigFacilityDTO> data = Context.getService(ConfigFacilityService.class).getAll();
		System.err.println("Data::" + data);

		return new ResponseEntity<>(data, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
	public ResponseEntity<?> getById(@PathVariable("id") Integer id) throws Exception {

		ConfigFacilityDTO data = Context.getService(ConfigFacilityService.class).getById(id);
		System.err.println("Data::" + data);

		return new ResponseEntity<>(data, HttpStatus.OK);

	}
}
