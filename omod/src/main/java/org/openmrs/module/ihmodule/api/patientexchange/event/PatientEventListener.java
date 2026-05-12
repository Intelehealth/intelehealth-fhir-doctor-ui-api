package org.openmrs.module.ihmodule.api.patientexchange.event;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event-driven alternative to scheduled patient export: intercepts successful
 * {@code PatientService.savePatient(...)} calls and dispatches async FHIR sync.
 */
public class PatientEventListener implements MethodInterceptor {
	
	private static final Logger log = LoggerFactory.getLogger(PatientEventListener.class);
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (invocation == null) {
			return null;
		}
		if (invocation.getMethod() == null || !"savePatient".equals(invocation.getMethod().getName())) {
			return invocation.proceed();
		}
		Patient incoming = extractPatient(invocation.getArguments());
		boolean createEvent = incoming != null && incoming.getPatientId() == null && incoming.getId() == null;
		Object result = invocation.proceed();
		if (FhirSyncSuppressionContext.isSuppressed()) {
			return result;
		}
		Patient savedPatient = result instanceof Patient ? (Patient) result : incoming;
		if (savedPatient == null || StringUtils.isBlank(savedPatient.getUuid())) {
			return result;
		}
		try {
			Context.getRegisteredComponent("patientEventHandlerService", PatientEventHandlerService.class).handleAfterSave(
			    savedPatient, createEvent ? PatientEventType.CREATE : PatientEventType.UPDATE);
		}
		catch (Exception ex) {
			log.error("Unable to dispatch patient save event for uuid={}", savedPatient.getUuid(), ex);
		}
		return result;
	}
	
	private Patient extractPatient(Object[] arguments) {
		if (arguments == null) {
			return null;
		}
		for (Object argument : arguments) {
			if (argument instanceof Patient) {
				return (Patient) argument;
			}
		}
		return null;
	}
}
