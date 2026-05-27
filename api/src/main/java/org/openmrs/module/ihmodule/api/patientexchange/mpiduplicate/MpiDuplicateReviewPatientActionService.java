package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.io.IOException;
import java.text.ParseException;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.parser.DataFormatException;
import org.json.JSONException;

/**
 * MPI duplicate-review UI: add patient (pending row or candidate) and skip flows. Implemented by
 * {@link MpiDuplicateReviewPatientActionServiceImpl} in the omod module.
 */
public interface MpiDuplicateReviewPatientActionService {
	
	void skipCandidateByCaseUuid(String caseUuid, long candidateId, String resolvedBy);
	
	void skipCaseByCaseUuid(String caseUuid, String resolvedBy);
	
	void addPatientFromPendingCase(String caseUuid, String patientUuidForLegacyForceSync, String resolvedBy)
	        throws ParseException, DataFormatException, JSONException, ConfigurationException, IOException;
	
	void addPatientFromCandidate(String caseUuid, long candidateId, String resolvedBy) throws ParseException,
	        DataFormatException, JSONException, ConfigurationException, IOException;
}
