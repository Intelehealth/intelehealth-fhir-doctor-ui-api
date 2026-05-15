package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

/**
 * Lifecycle for an ambiguous MPI search (multiple FHIR Patient matches). Resolution fields live on
 * {@link MpiPatientDuplicateReviewCase} once an operator acts.
 */
public enum MpiDuplicateReviewStatus {
	
	/** Stored for manual decision; sync to central FHIR/MCI should not proceed automatically. */
	PENDING,
	
	/** Operator chose to link local patient to one FHIR Patient / MPI id. */
	RESOLVED_LINK_EXISTING,
	
	/** Operator chose to force create/send despite duplicates (handled by future workflow). */
	RESOLVED_FORCE_SEND,
	
	/** Duplicate-review UI: patient add path finished successfully. */
	COMPLETED,
	
	/** Duplicate-review UI: operator skipped without creating/updating a patient. */
	SKIPPED,
	
	/** Case voided / superseded without resolution. */
	CANCELLED
}
