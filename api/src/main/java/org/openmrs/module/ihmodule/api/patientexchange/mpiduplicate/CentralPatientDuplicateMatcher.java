package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientexchange.search.CentralPatientSearchService;
import org.openmrs.module.ihmodule.api.patientexchange.search.PatientDemographicSearchUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Before MCI patient save: demographic Patient search on central OpenCR (same query as legacy MCI).
 * When two or more Patients match, persists duplicate-review rows and skips MCI.
 */
@Service
public class CentralPatientDuplicateMatcher {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CentralPatientDuplicateMatcher.class);
	
	@Autowired
	private CentralPatientSearchService centralPatientSearchService;
	
	@Autowired
	private MpiDuplicateReviewService mpiDuplicateReviewService;
	
	@Value("${intelehealth.fhir.mpi.duplicate.precheck.enabled:true}")
	private boolean duplicatePrecheckEnabled;
	
	/**
	 * @return persisted review case only when central search finds ≥2 Patient entries; otherwise
	 *         empty.
	 */
	public Optional<MpiPatientDuplicateReviewCase> persistIfCentralSearchHasMultipleMatches(Patient patient,
	        String localPatientUuid, String outboundBundleJson) {
		if (!duplicatePrecheckEnabled) {
			return Optional.empty();
		}
		if (!PatientDemographicSearchUriBuilder.hasFullDemographicsForPatientSearch(patient)) {
			return Optional.empty();
		}
		try {
			String searchBody = centralPatientSearchService.searchPatientJsonByDemographics(patient);
			return mpiDuplicateReviewService.persistIfAmbiguous(localPatientUuid, patient, searchBody, outboundBundleJson);
		}
		catch (UnsupportedEncodingException ex) {
			LOGGER.warn("Central duplicate pre-search encoding error for patient {}: {}", localPatientUuid, ex.getMessage(),
			    ex);
			return Optional.empty();
		}
		catch (RuntimeException ex) {
			LOGGER.warn("Central duplicate pre-search failed for patient {}: {}", localPatientUuid, ex.getMessage(), ex);
			return Optional.empty();
		}
	}
}
