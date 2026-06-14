package org.openmrs.module.ihmodule.api;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ihmodule.dto.PrescriptionShareDTO;
import org.openmrs.module.ihmodule.dto.PrescriptionShareRequestDTO;

public interface PrescriptionShareService extends OpenmrsService {
	
	PrescriptionShareDTO createShare(PrescriptionShareRequestDTO request) throws APIException;
	
	void deleteShareByUuid(String uuid, String voidReason) throws APIException;
	
	void deleteShareByPatientAndVisit(String patientUuid, String visitUuid, String voidReason) throws APIException;
	
}
