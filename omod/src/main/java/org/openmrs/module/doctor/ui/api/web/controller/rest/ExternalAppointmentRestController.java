package org.openmrs.module.doctor.ui.api.web.controller.rest;

import org.openmrs.api.context.Context;
import org.openmrs.module.doctor.ui.api.ExternalAppointment;
import org.openmrs.module.doctor.ui.api.api.ExtrnalAppointmentService;
import org.openmrs.module.doctor.ui.api.dto.ApiResponse;
import org.openmrs.module.doctor.ui.api.dto.ExternalAppointmentDTO;
import org.openmrs.module.doctor.ui.api.utils.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rest/v1/external-appointment")
public class ExternalAppointmentRestController<RequestAppointmentDTO> {
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)
    public ResponseEntity<?> save(@Valid @RequestBody ExternalAppointment param) throws Exception {
        ApiResponse errorResponse = new ApiResponse();
        ExternalAppointmentDTO dto = Context.getService(ExtrnalAppointmentService.class).save(param);
        if (dto != null) {
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } else {
            errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            errorResponse.setMessage("Couldn't save the external appointment");
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
	
	@RequestMapping(value = "/change-status", method = RequestMethod.PUT)
    public ResponseEntity<?> changeStatus(@RequestParam Map<String, String> reqParam) throws Exception {
        Context.getService(ExtrnalAppointmentService.class).changeStatus(reqParam);
        return new ResponseEntity<>("Successfully send the request", HttpStatus.OK);
    }
	
	@RequestMapping(value = "/delete-appointment/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> changeStatus(@PathVariable("id") String extAppointmentId) throws Exception {
        ApiResponse errorResponse = new ApiResponse();
        Context.getService(ExtrnalAppointmentService.class).deleteAppointment(extAppointmentId);
        errorResponse.setStatus(HttpStatus.OK.value());
        errorResponse.setMessage("Successfully deleted the external appointment");
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }
	
	@RequestMapping(value = "/findAllByPatient/{patientId}", method = RequestMethod.GET)
    public ResponseEntity<?> findAllByPatient(@PathVariable("patientId") String patientId) throws Exception {
        List<ExternalAppointmentDTO> data = Context.getService(ExtrnalAppointmentService.class)
                .findAllByPatient(patientId);
        System.err.println("Data::" + data);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }
	
	@RequestMapping(value = "/findAppointmentByPatientAndVisitId/{patientId}/{visitId}", method = RequestMethod.GET)
    public ResponseEntity<?> findAppointmentByPatientAndVisitId(@PathVariable("patientId") String patientId, @PathVariable("visitId") String visitId) throws Exception {
        List<ExternalAppointmentDTO> data = Context.getService(ExtrnalAppointmentService.class)
                .findAppointmentByPatientAndVisitId(patientId, visitId);
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
