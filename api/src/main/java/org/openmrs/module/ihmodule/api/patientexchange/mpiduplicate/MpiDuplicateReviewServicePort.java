package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiDuplicateReviewCandidateMatchRow;
import org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate.MpiPatientDuplicateReviewCase;
import org.springframework.stereotype.Service;

/**
 * Injection-safe contract for MPI duplicate review.
 * <p>
 * Exists to make Spring's JDK transactional proxies assignable during autowiring.
 */
public interface MpiDuplicateReviewServicePort {
	
	Optional<MpiPatientDuplicateReviewCase> persistIfAmbiguous(String localPatientUuid, Patient sourcePatient,
	        String searchBundleJson, String outboundBundleJson);
	
	Optional<MpiPatientDuplicateReviewCase> persistImportFuzzyDuplicateIfMatches(String importCorrelationLocalKey,
	        Patient sourcePatient, String outboundImportPatientJson,
	        List<MpiDuplicateReviewCandidateMatchRow> mergedMatchRows, String searchAuditsJson);
	
}

