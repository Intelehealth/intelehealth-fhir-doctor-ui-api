package org.openmrs.module.ihmodule.web.controller.rest;

import java.util.List;
import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.ExternalAppointment;
import org.openmrs.module.ihmodule.api.ExtrnalAppointmentService;
import org.openmrs.module.ihmodule.dto.ExternalAppointmentDTO;
import org.openmrs.module.ihmodule.utils.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ca.uhn.fhir.context.FhirContext;

@Controller
@RequestMapping("/rest/v1/external-appointment")
public class ExternalAppointmentRestController<RequestAppointmentDTO> {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public ResponseEntity<?> save(@RequestBody ExternalAppointment param) throws Exception {

		try {
			ExternalAppointmentDTO dto = Context.getService(ExtrnalAppointmentService.class).save(param);
			if (dto != null) {
				return new ResponseEntity<>(dto, HttpStatus.OK);
			} else {
				return new ResponseEntity<>("Couldn't save the external appointment", HttpStatus.BAD_REQUEST);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("Request failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/change-status", method = RequestMethod.PUT)
	public ResponseEntity<?> changeStatus(@RequestParam Map<String, String> reqParam) throws Exception {
		try {
			Context.getService(ExtrnalAppointmentService.class).changeStatus(reqParam);
			return new ResponseEntity<>("Succesfully send the request", HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>("Request failed", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@RequestMapping(value = "/findAllByPatient/{patientId}", method = RequestMethod.GET)
	public ResponseEntity<?> findAllByPatient(@PathVariable("patientId") String patientId) throws Exception {

		List<ExternalAppointmentDTO> data = Context.getService(ExtrnalAppointmentService.class)
				.findAllByPatient(patientId);
		System.err.println("Data::" + data);

		return new ResponseEntity<>(data, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/search/{requesterId}/{start}/{end}", method = RequestMethod.GET)
	public ResponseEntity<?> search(@PathVariable("requesterId") String patientId, @PathVariable("start") String start,
			@PathVariable("end") String end) throws Exception {

		List<ExternalAppointmentDTO> data = Context.getService(ExtrnalAppointmentService.class)
				.findAllByPatient(patientId);
		System.err.println("Data::" + data);

		return new ResponseEntity<>(data, HttpStatus.OK);

	}
	
	@RequestMapping(value = "/practitioners", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> practitioners(@RequestParam Map<String, String> reqParam) throws Exception {
		HttpResponse response = Context.getService(ExtrnalAppointmentService.class).getAllPractitioners(reqParam);
		return new ResponseEntity<>(response.getResponse(), HttpStatus.valueOf(response.getStatusCode()));
	}
	
	@RequestMapping(value = "/available/schedule", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> availableSchedule(@RequestParam Map<String, String> reqParam) throws Exception {
		HttpResponse response = Context.getService(ExtrnalAppointmentService.class).getAvailableSchedule(reqParam);
		return new ResponseEntity<>(response.getResponse(), HttpStatus.valueOf(response.getStatusCode()));
	}
	
	@RequestMapping(value = "/available/slot", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> availableSlot(@RequestParam Map<String, String> reqParam) throws Exception {
		HttpResponse response = Context.getService(ExtrnalAppointmentService.class).getAvailableSlot(reqParam);
		return new ResponseEntity<>(response.getResponse(), HttpStatus.valueOf(response.getStatusCode()));
	}
}
