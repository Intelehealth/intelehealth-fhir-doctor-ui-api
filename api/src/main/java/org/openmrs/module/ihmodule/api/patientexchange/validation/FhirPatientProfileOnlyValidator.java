package org.openmrs.module.ihmodule.api.patientexchange.validation;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;

/**
 * Profile-only FHIR validation (plain {@link FhirValidator} + configured profile URL). Used for
 * both import and transfer; does not require {@code org.hl7.fhir.common.hapi.validation.*}.
 */
public final class FhirPatientProfileOnlyValidator {
	
	private FhirPatientProfileOnlyValidator() {
	}
	
	public enum Status {
		VALID, INVALID, SKIPPED_HAPI_1758
	}
	
	public static final class Result {
		
		public final Status status;
		
		public final String detail;
		
		public Result(Status status, String detail) {
			this.status = status;
			this.detail = detail;
		}
	}
	
	/**
	 * @param profileUrlOrBlank profile URL from configuration; may be blank
	 */
	public static Result validate(FhirContext fhirContext, String profileUrlOrBlank, Patient patient) {
		FhirValidator validator = fhirContext.newValidator();
		validator.setValidateAgainstStandardSchema(false);
		validator.setValidateAgainstStandardSchematron(false);
		ValidationOptions options = new ValidationOptions();
		if (StringUtils.isNotBlank(profileUrlOrBlank)) {
			options.addProfile(profileUrlOrBlank.trim());
		}
		try {
			ValidationResult result = validator.validateWithResult(patient, options);
			if (result.isSuccessful()) {
				return new Result(Status.VALID, null);
			}
			String message = result.getMessages().stream()
			        .map(msg -> msg.getSeverity() + ": " + msg.getMessage())
			        .collect(Collectors.joining(" | "));
			if (StringUtils.isBlank(message)) {
				message = "FHIR profile validation failed";
			}
			return new Result(Status.INVALID, message);
		}
		catch (Exception ex) {
			if (ex.getMessage() != null && ex.getMessage().contains("HAPI-1758")) {
				return new Result(Status.SKIPPED_HAPI_1758, null);
			}
			return new Result(Status.INVALID, "FHIR validation execution failed: " + ex.getMessage());
		}
	}
}
