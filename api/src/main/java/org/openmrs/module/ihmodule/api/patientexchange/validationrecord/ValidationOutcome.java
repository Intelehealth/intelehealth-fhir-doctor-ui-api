package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

/**
 * Outcome of the FHIR validation / processing step. Extensible for new resource types using the
 * same storage without schema changes.
 */
public enum ValidationOutcome {
	/**
	 * Validation ran and the resource passed (method completed after validation without a
	 * validation failure for this call).
	 */
	VALIDATION_PASSED,
	/**
	 * The configured validator rejected the instance (e.g. profile constraints).
	 */
	VALIDATION_FAILED,
	/**
	 * Error during fetch, transformation, or send after the validation step, or a non-validation
	 * failure before completion.
	 */
	PROCESSING_ERROR
}
