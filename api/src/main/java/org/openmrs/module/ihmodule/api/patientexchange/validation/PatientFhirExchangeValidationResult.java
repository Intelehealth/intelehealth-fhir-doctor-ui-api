package org.openmrs.module.ihmodule.api.patientexchange.validation;

/**
 * Outcome of {@link PatientFhirExchangeValidationService#validatePatient}.
 */
public final class PatientFhirExchangeValidationResult {
	
	public enum Status {
		/** Validation passed. */
		OK,
		/** Profile validation disabled by configuration. */
		SKIPPED_DISABLED,
		/** HAPI-1758: schema resources missing; caller treats as success (same as legacy import). */
		SKIPPED_HAPI_1758,
		/** Extension or profile / chain failure; see {@link #getFailureReason()}. */
		INVALID
	}
	
	private final Status status;
	
	private final String failureReason;
	
	private PatientFhirExchangeValidationResult(Status status, String failureReason) {
		this.status = status;
		this.failureReason = failureReason;
	}
	
	public static PatientFhirExchangeValidationResult ok() {
		return new PatientFhirExchangeValidationResult(Status.OK, null);
	}
	
	public static PatientFhirExchangeValidationResult skippedDisabled() {
		return new PatientFhirExchangeValidationResult(Status.SKIPPED_DISABLED, null);
	}
	
	public static PatientFhirExchangeValidationResult skippedHapi1758() {
		return new PatientFhirExchangeValidationResult(Status.SKIPPED_HAPI_1758, null);
	}
	
	public static PatientFhirExchangeValidationResult invalid(String reason) {
		return new PatientFhirExchangeValidationResult(Status.INVALID, reason);
	}
	
	public Status getStatus() {
		return status;
	}
	
	public String getFailureReason() {
		return failureReason;
	}
	
	/** {@code true} when the patient may proceed (valid, skipped disabled, or skipped HAPI-1758). */
	public boolean isPermitted() {
		return status != Status.INVALID;
	}
}
