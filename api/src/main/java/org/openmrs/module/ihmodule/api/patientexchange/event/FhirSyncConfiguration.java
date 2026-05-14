package org.openmrs.module.ihmodule.api.patientexchange.event;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("fhirSyncConfiguration")
public class FhirSyncConfiguration {
	
	public static final String GP_EVENT_LISTENER_ENABLED = "ihmodule.fhir.event.listener.enabled";
	
	public static final String GP_EVENT_LISTENER_CREATE_ENABLED = "ihmodule.fhir.event.listener.create.enabled";
	
	public static final String GP_EVENT_LISTENER_UPDATE_ENABLED = "ihmodule.fhir.event.listener.update.enabled";
	
	private static final Logger log = LoggerFactory.getLogger(FhirSyncConfiguration.class);
	
	public boolean shouldProcess(PatientEventType eventType) {
		if (!isEventListenerEnabled()) {
			return false;
		}
		if (eventType == PatientEventType.CREATE) {
			return isCreateEnabled();
		}
		return isUpdateEnabled();
	}
	
	public boolean isEventListenerEnabled() {
		return resolveBooleanGlobalProperty(GP_EVENT_LISTENER_ENABLED, true);
	}
	
	public boolean isCreateEnabled() {
		return resolveBooleanGlobalProperty(GP_EVENT_LISTENER_CREATE_ENABLED, true);
	}
	
	public boolean isUpdateEnabled() {
		return resolveBooleanGlobalProperty(GP_EVENT_LISTENER_UPDATE_ENABLED, true);
	}
	
	private boolean resolveBooleanGlobalProperty(String propertyName, boolean defaultValue) {
		try {
			String raw = Context.getAdministrationService().getGlobalProperty(propertyName);
			if (StringUtils.isBlank(raw)) {
				return defaultValue;
			}
			return Boolean.parseBoolean(raw.trim());
		}
		catch (Exception ex) {
			log.debug("Unable to read global property {}. Using default={}", propertyName, defaultValue, ex);
			return defaultValue;
		}
	}
}
