package org.openmrs.module.ihmodule.api.patientexchange.event;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepts successful {@link org.openmrs.api.PatientService#savePatient(org.openmrs.Patient)} and
 * dispatches async FHIR sync for both <strong>create</strong> and <strong>update</strong> flows.
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
		boolean createEvent = isCreateEvent(incoming);
		Object result = invocation.proceed();
		if (FhirSyncSuppressionContext.isSuppressed()) {
			return result;
		}
		Patient savedPatient = result instanceof Patient ? (Patient) result : incoming;
		if (savedPatient == null || StringUtils.isBlank(savedPatient.getUuid())) {
			return result;
		}
		try {
			PatientEventType eventType = createEvent ? PatientEventType.CREATE : PatientEventType.UPDATE;
			log.debug("Dispatching patient {} for uuid={}", eventType.getLogLabel(), savedPatient.getUuid());
			Context.getRegisteredComponent("patientEventHandlerService", PatientEventHandlerService.class).handleAfterSave(
			    savedPatient, eventType);
		}
		catch (Exception ex) {
			log.error("Unable to dispatch patient save event for uuid={}", savedPatient.getUuid(), ex);
		}
		return result;
	}
	
	/**
	 * {@code true} for first-time insert (no PK / Hibernate id and no existing row for UUID).
	 * Otherwise {@link PatientEventType#UPDATE} is used so edits to existing patients always sync
	 * when {@code ihmodule.fhir.event.listener.update.enabled} is true.
	 */
	private boolean isCreateEvent(Patient incoming) {
		if (incoming == null) {
			return false;
		}
		if (incoming.getPatientId() != null) {
			return false;
		}
		if (incoming.getId() != null) {
			return false;
		}
		String uuid = incoming.getUuid();
		if (StringUtils.isBlank(uuid)) {
			return true;
		}
		try {
			if (Context.isSessionOpen() && Context.getPatientService().getPatientByUuid(uuid.trim()) != null) {
				return false;
			}
		}
		catch (Exception ex) {
			log.debug("Could not resolve patient by uuid for create/update classification: {}", ex.getMessage());
		}
		return true;
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
