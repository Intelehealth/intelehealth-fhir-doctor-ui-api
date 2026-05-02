package org.openmrs.module.doctor.ui.api.web.controller.rest;

import org.openmrs.api.context.Context;
import org.openmrs.module.doctor.ui.api.ExternalReferral;
import org.openmrs.module.doctor.ui.api.api.ExternalReferralService;
import org.openmrs.module.doctor.ui.api.dto.ApiResponse;
import org.openmrs.module.doctor.ui.api.dto.ExternalReferralDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
@RequestMapping("/rest/v1/external-referral")
public class ExternalReferralRestController {
	
	@RequestMapping(value = "/save", method = RequestMethod.POST)

    public ResponseEntity<?> save(@RequestBody ExternalReferral referral) {
        ExternalReferralDTO dto = Context.getService(ExternalReferralService.class).save(referral);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
	
	@RequestMapping(value = "/findReferralByPatientIdAndVisitId/{patientId}/{visitId}", method = RequestMethod.GET)
    public ResponseEntity<?> findReferralByPatientIdAndVisitId(@PathVariable("patientId") String patientId, @PathVariable("visitId") String visitId) {
        List<ExternalReferralDTO> data = Context.getService(ExternalReferralService.class)
                .findReferralByPatientIdAndVisitId(patientId, visitId);
        System.err.println("Data::" + data);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }
	
	@RequestMapping(value = "/delete-referral/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteReferralById(@PathVariable("id") String id) {
        ApiResponse errorResponse = new ApiResponse();
        Context.getService(ExternalReferralService.class).deleteReferral(id);
        errorResponse.setStatus(HttpStatus.OK.value());
        errorResponse.setMessage("Successfully deleted the external referral");
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }
	
	@RequestMapping(value = "/delete-referral-by-uuid/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteReferralByUuid(@PathVariable("uuid") String uuid) {
        ApiResponse errorResponse = new ApiResponse();
        Context.getService(ExternalReferralService.class).deleteReferralByUuid(uuid);
        errorResponse.setStatus(HttpStatus.OK.value());
        errorResponse.setMessage("Successfully deleted the external referral");
        return new ResponseEntity<>(errorResponse, HttpStatus.OK);
    }
}
