package org.openmrs.module.ihmodule.web.controller.rest;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.DataMigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/rest/v1/data-migration")
public class DataMigrationRestController {
	
	@RequestMapping(value = "/{date}", method = RequestMethod.GET)
	public ResponseEntity<?> dataMigration(@PathVariable String date) throws Exception {
		
		return new ResponseEntity<>(Context.getService(DataMigrationService.class).getMedicineData(date, 0), HttpStatus.OK);
	}
}
