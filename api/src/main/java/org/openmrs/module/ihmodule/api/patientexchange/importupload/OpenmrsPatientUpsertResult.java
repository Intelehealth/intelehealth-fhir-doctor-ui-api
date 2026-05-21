package org.openmrs.module.ihmodule.api.patientexchange.importupload;

/**
 * Outcome of a local OpenMRS patient create or update from a FHIR Patient snapshot.
 */
public class OpenmrsPatientUpsertResult {
	
	public enum Action {
		CREATED, UPDATED
	}
	
	private final String patientUuid;
	
	private final Action action;
	
	private final String message;
	
	public OpenmrsPatientUpsertResult(String patientUuid, Action action, String message) {
		this.patientUuid = patientUuid;
		this.action = action;
		this.message = message;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public Action getAction() {
		return action;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean isCreated() {
		return action == Action.CREATED;
	}
	
	public boolean isUpdated() {
		return action == Action.UPDATED;
	}
}
