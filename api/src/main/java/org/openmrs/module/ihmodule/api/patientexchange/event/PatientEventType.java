package org.openmrs.module.ihmodule.api.patientexchange.event;

enum PatientEventType {
	
	CREATE("create"), UPDATE("update");
	
	private final String logLabel;
	
	PatientEventType(String logLabel) {
		this.logLabel = logLabel;
	}
	
	public String getLogLabel() {
		return logLabel;
	}
}
