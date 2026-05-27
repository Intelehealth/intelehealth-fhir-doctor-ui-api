package org.openmrs.module.ihmodule.api.patientexchange.service;

import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;

/**
 * Sync progress markers ({@code ih_marker}). Implemented by {@link IHMarkerServiceImpl}.
 */
public interface IHMarkerService {
	
	/**
	 * Progress marker for
	 * {@link org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR#transferUnsyncedPatient()}.
	 */
	String MARKER_UNSYNCED_PATIENT = "UNSYNCED_PATIENT";
	
	IHMarker save(IHMarker ihMarker);
	
	IHMarker findByName(String name);
	
	void updateMarkerByName(String name);
	
	IHMarker getOrCreateUnsyncedPatientProgressMarker();
	
	void updateUnsyncedPatientMarkerLastId(int lastUnsyncPatientRowId);
	
}
