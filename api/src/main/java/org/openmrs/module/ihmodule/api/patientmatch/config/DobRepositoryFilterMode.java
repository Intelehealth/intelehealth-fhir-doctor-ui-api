package org.openmrs.module.ihmodule.api.patientmatch.config;

public enum DobRepositoryFilterMode {
	
	ALWAYS, ONLY_DOB_REQUESTS, NEVER;
	
	public static DobRepositoryFilterMode fromValue(String value, DobRepositoryFilterMode defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		for (DobRepositoryFilterMode mode : values()) {
			if (mode.name().equalsIgnoreCase(value.trim())) {
				return mode;
			}
		}
		return defaultValue;
	}
}
