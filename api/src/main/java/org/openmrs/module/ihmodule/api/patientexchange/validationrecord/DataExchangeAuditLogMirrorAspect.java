package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.model.DataExchangeAuditLog;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fallback recorder that mirrors existing data-exchange audit writes into
 * fhir_resource_validation_record.
 * <p>
 * This keeps existing business classes untouched and still records outcomes when direct method
 * interception on DataSendToFHIR is skipped due proxy boundaries.
 */
@Aspect
@Component
@Order(1)
public class DataExchangeAuditLogMirrorAspect {
	
	private FhirResourceValidationRecordService validationRecordService = Context.getRegisteredComponent(
	    "fhirResourceValidationRecordService", FhirResourceValidationRecordService.class);
	
	@AfterReturning(pointcut = "execution(public * org.openmrs.module.ihmodule.api.patientexchange.service.DataExchangeAuditLogService.save(..))", returning = "saved")
	public void afterAuditSave(JoinPoint joinPoint, Object saved) {
		DataExchangeAuditLog log = asAuditLog(saved);
		if (log == null) {
			return;
		}
		validationRecordService.recordValues(log.getResourceName(), log.getResourceUuid(),
		    ValidationOutcome.VALIDATION_PASSED, "Validation passed", log.getRequest());
	}
	
	@AfterReturning(pointcut = "execution(public * org.openmrs.module.ihmodule.api.patientexchange.service.DataExchangeAuditLogService.update(..))", returning = "saved")
	public void afterAuditUpdate(JoinPoint joinPoint, Object saved) {
		// Intentionally no-op:
		// send/pipeline errors are already persisted in data_exchange_auditlog.
		// Keep fhir_resource_validation_record only for validation outcomes.
	}
	
	private DataExchangeAuditLog asAuditLog(Object value) {
		if (value instanceof DataExchangeAuditLog) {
			return (DataExchangeAuditLog) value;
		}
		return null;
	}
}
