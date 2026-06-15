package org.openmrs.module.ihmodule.web.controller.rest;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.PrescriptionShareService;
import org.openmrs.module.ihmodule.dto.ApiResponse;
import org.openmrs.module.ihmodule.dto.PrescriptionShareDTO;
import org.openmrs.module.ihmodule.dto.PrescriptionShareRequestDTO;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/rest/v1/prescription-share")
public class PrescriptionShareRestController {
	
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public ResponseEntity<?> create(@RequestBody PrescriptionShareRequestDTO request) {
		try {
			PrescriptionShareDTO dto = Context.getService(PrescriptionShareService.class).createShare(request);
			return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dto);
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/delete/{uuid}", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteByUuid(@PathVariable("uuid") String uuid,
	        @RequestParam(value = "voidReason", required = false) String voidReason) {
		try {
			Context.getService(PrescriptionShareService.class).deleteShareByUuid(uuid, voidReason);
			return successDeleteResponse();
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public ResponseEntity<?> deleteByPatientAndVisit(@RequestParam("patientUuid") String patientUuid,
	        @RequestParam("visitUuid") String visitUuid,
	        @RequestParam(value = "voidReason", required = false) String voidReason) {
		try {
			if (StringUtils.isBlank(patientUuid) || StringUtils.isBlank(visitUuid)) {
				return new ResponseEntity<>("patientUuid and visitUuid are required", HttpStatus.BAD_REQUEST);
			}
			Context.getService(PrescriptionShareService.class).deleteShareByPatientAndVisit(patientUuid, visitUuid,
			    voidReason);
			return successDeleteResponse();
		}
		catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}
	
	private ResponseEntity<ApiResponse> successDeleteResponse() {
		ApiResponse response = new ApiResponse();
		response.setStatus(HttpStatus.OK.value());
		response.setMessage("Successfully deleted the prescription share");
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(response);
	}
	
}
