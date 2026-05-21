package org.openmrs.module.ihmodule.api.patientexchange.validation;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.validationrecord.ValidationRecordContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

/**
 * Single entry point for FHIR Patient validation: extension whitelist + profile-only HAPI
 * validation (same path for import upload and transfer/scheduler). Does not use
 * {@code org.hl7.fhir.common.hapi.validation.*} instance-validator JARs.
 */
@Service
public class PatientFhirExchangeValidationService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientFhirExchangeValidationService.class);
	
	/**
	 * @param patientUuidForLog optional logical id for logs / failure context (import may use
	 *            correlation id)
	 */
	public PatientFhirExchangeValidationResult validatePatient(FhirContext fhirContext, FhirConfig fhirConfig,
	        Patient patient, String patientUuidForLog) {
		String logUuid = StringUtils.isNotBlank(patientUuidForLog) ? patientUuidForLog.trim() : "(no-uuid)";
		if (!fhirConfig.isPatientImportProfileValidationEnabled()) {
			log.debug(
			    "Skipping FHIR profile validation for patient uuid={} (intelehealth.fhir.patient.import.profile.validation.enabled=false)",
			    logUuid);
			return PatientFhirExchangeValidationResult.skippedDisabled();
		}
		String extensionViolation = PatientProfileExtensionRules.unknownPatientExtensionViolationMessage(patient);
		if (extensionViolation != null) {
			ValidationRecordContext.setFailureReason(extensionViolation);
			log.error("Patient extension whitelist failed for patient uuid={}: {}", logUuid, extensionViolation);
			return PatientFhirExchangeValidationResult.invalid(extensionViolation);
		}
		return mapProfileResult(
		    FhirPatientProfileOnlyValidator.validate(fhirContext, fhirConfig.getPatientProfileUrl(), patient), logUuid);
	}
	
	private PatientFhirExchangeValidationResult mapProfileResult(FhirPatientProfileOnlyValidator.Result profileResult,
	        String logUuid) {
		switch (profileResult.status) {
			case SKIPPED_HAPI_1758:
				log.warn(
				    "FHIR schema resources not available at runtime; skipping schema-based profile validation for uuid={}",
				    logUuid);
				return PatientFhirExchangeValidationResult.skippedHapi1758();
			case INVALID:
				ValidationRecordContext.setFailureReason(profileResult.detail);
				log.error("Validation failed for patient uuid={}", logUuid);
				return PatientFhirExchangeValidationResult.invalid(profileResult.detail);
			case VALID:
			default:
				log.info("Validation passed for patient uuid={}", logUuid);
				return PatientFhirExchangeValidationResult.ok();
		}
	}
}
