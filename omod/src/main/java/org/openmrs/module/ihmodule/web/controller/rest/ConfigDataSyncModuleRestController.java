package org.openmrs.module.ihmodule.web.controller.rest;

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.ConfigDataSyncModule;
import org.openmrs.module.ihmodule.api.ConfigDataSyncModuleService;
import org.openmrs.module.ihmodule.dto.ConfigDataSyncModuleDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ca.uhn.fhir.context.FhirContext;

@Controller
@RequestMapping("/rest/v1/config-data-sync-module")
public class ConfigDataSyncModuleRestController<RequestAppointmentDTO> {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public ResponseEntity<?> save(@RequestBody ConfigDataSyncModule param) throws Exception {

		try {
			ConfigDataSyncModuleDTO dto = Context.getService(ConfigDataSyncModuleService.class).save(param);
			if (dto != null) {
				return new ResponseEntity<>(dto, HttpStatus.OK);
			}
			return new ResponseEntity<>("Couldn't save the config data sync", HttpStatus.BAD_REQUEST);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("Request failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/change-status", method = RequestMethod.PUT)
	public ResponseEntity<?> changeStatus(@RequestBody ConfigDataSyncModule param) throws Exception {

		try {
			ConfigDataSyncModuleDTO dto = Context.getService(ConfigDataSyncModuleService.class).changeStatus(param);
			if (dto != null) {
				return new ResponseEntity<>(dto, HttpStatus.OK);
			}
			return new ResponseEntity<>("Couldn't save the config data sync", HttpStatus.BAD_REQUEST);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("Request failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/getAll", method = RequestMethod.GET)
	public ResponseEntity<?> getAll() throws Exception {

		List<ConfigDataSyncModuleDTO> data = Context.getService(ConfigDataSyncModuleService.class).getAll();
		System.err.println("Data::" + data);

		return new ResponseEntity<>(data, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/get/{id}", method = RequestMethod.GET)
	public ResponseEntity<?> getById(@PathVariable("id") Integer id) throws Exception {

		ConfigDataSyncModuleDTO data = Context.getService(ConfigDataSyncModuleService.class).getById(id);
		System.err.println("Data::" + data);

		return new ResponseEntity<>(data, HttpStatus.OK);

	}
}
