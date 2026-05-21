package org.openmrs.module.ihmodule.api.patientmatch.repository;

import java.util.List;

import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;

public interface PatientCandidateSource {
	
	List<FuzzyPatientCandidate> findCandidates(FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config);
}
