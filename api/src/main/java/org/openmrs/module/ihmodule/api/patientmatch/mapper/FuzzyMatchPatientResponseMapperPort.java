package org.openmrs.module.ihmodule.api.patientmatch.mapper;

import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;

/**
 * Injection-safe contract for fuzzy-match patient response enrichment.
 */
public interface FuzzyMatchPatientResponseMapperPort {
	
	void enrich(Patient patient, FuzzyPatientCandidate candidate);
	
}

