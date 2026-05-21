package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Intercepts the public entry point that performs validation + send, without modifying
 * {@link org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR}. Only registered
 * when {@code intelehealth.fhir.validation.audit.store.enabled} is true.
 * <p>
 * For multi-entry bundles, the stored {@code resourceLogicalId} is taken from the first entry;
 * failed validation may apply to a later entry in rare cases.
 */
@Aspect
@Component
@Order(0)
public class ResourceValidationAuditAspect {
	
	@Autowired
	private FhirResourceValidationRecordService validationRecordService;
	
	@Around("(execution(public * org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR.sendFHIRBundle(org.hl7.fhir.r4.model.Bundle, java.lang.String)) || execution(public * org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR.sendFHIRBundle(org.hl7.fhir.r4.model.Bundle, java.lang.String, boolean)))")
	public Object aroundSendFhirBundle(ProceedingJoinPoint joinPoint) throws Throwable {
		Bundle bundle = (Bundle) joinPoint.getArgs()[0];
		String resourceType = (String) joinPoint.getArgs()[1];
		ValidationRecordContext.clear();
		Object result;
		try {
			result = joinPoint.proceed();
		}
		catch (Throwable t) {
			// Keep this table focused on validation state only.
			// Validation failures are persisted directly in DataSendToFHIR and
			// pipeline/send failures remain in data_exchange_auditlog.
			ValidationRecordContext.clear();
			throw t;
		}
		if (result == null && (bundle == null || !bundle.hasEntry())) {
			ValidationRecordContext.clear();
			return null;
		}
		validationRecordService.recordValues(safeType(resourceType), firstResourceLogicalId(bundle),
		    ValidationOutcome.VALIDATION_PASSED, "Validation passed", ValidationRecordContext.getPayloadJson());
		ValidationRecordContext.clear();
		return result;
	}
	
	private static String firstResourceLogicalId(Bundle bundle) {
		if (bundle == null || !bundle.hasEntry() || bundle.getEntryFirstRep().getResource() == null) {
			return null;
		}
		return bundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
	}
	
	private static String safeType(String resourceType) {
		return StringUtils.isNotBlank(resourceType) ? resourceType : "Unknown";
	}
}
