package org.openmrs.module.ihmodule.api;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ihmodule.ExternalReferral;
import org.openmrs.module.ihmodule.dto.ExternalReferralDTO;

import java.util.List;

public interface ExternalReferralService extends OpenmrsService {
	
	ExternalReferralDTO save(ExternalReferral externalReferral);
	
	List<ExternalReferralDTO> findReferralByPatientIdAndVisitId(String patientId, String visitId) throws APIException;
	
	void deleteReferral(String extReferralId);
	
	void deleteReferralByUuid(String uuid);
}
