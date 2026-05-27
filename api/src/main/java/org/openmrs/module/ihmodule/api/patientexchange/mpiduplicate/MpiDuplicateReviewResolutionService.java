package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.hl7.fhir.r4.model.Bundle;

/**
 * Updates {@link MpiPatientDuplicateReviewCase} when an operator completes force-sync or local MPI
 * assignment. Implemented by {@link MpiDuplicateReviewResolutionServiceImpl}.
 */
public interface MpiDuplicateReviewResolutionService {
	
	void resolvePendingCaseAfterSuccessfulForceSync(String localPatientUuid, Bundle mciResponseBundle,
	        String resolvedBy);
	
	/**
	 * @deprecated Only used by deprecated {@code POST .../patient-exchange/mpi-local}.
	 */
	@Deprecated
	void resolvePendingCaseAfterLocalMpiUpdate(String localPatientUuid, String mpiIdentifierValue,
	        String chosenFhirPatientLogicalId, String resolvedBy);
	
	void markCaseAndAllCandidatesCompleted(MpiPatientDuplicateReviewCase reviewCase, String resolvedBy);
	
	void markCaseAndAllCandidatesSkipped(MpiPatientDuplicateReviewCase reviewCase, String resolvedBy);
	
	void markCandidateSkipped(MpiPatientDuplicateReviewCandidate candidate, String resolvedBy);
	
	void markAllCandidatesSkippedOnly(MpiPatientDuplicateReviewCase reviewCase, String resolvedBy);
}
