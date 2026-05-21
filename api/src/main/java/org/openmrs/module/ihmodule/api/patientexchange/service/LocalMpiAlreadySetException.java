package org.openmrs.module.ihmodule.api.patientexchange.service;

/**
 * Local FHIR Patient already carries a non-empty MPI-type identifier; operator endpoints skip work.
 */
public class LocalMpiAlreadySetException extends IllegalStateException {
	
	private static final long serialVersionUID = 1L;
	
	public LocalMpiAlreadySetException() {
		super(LocalPatientMpiUpdateService.MESSAGE_MPI_ALREADY_SET_LOCAL_ENDPOINT);
	}
}
