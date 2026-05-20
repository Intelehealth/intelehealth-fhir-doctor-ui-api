package org.openmrs.module.ihmodule.api.patientexchange.service;

import static org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils.toFormattedDateNow;

import javax.transaction.Transactional;

import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;
import org.openmrs.module.ihmodule.api.patientexchange.repository.IHMarkerRepository;
import org.springframework.stereotype.Service;

@Service
public class IHMarkerService {
	
	/**
	 * Progress marker for
	 * {@link org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR#transferUnsyncedPatient()}
	 * .
	 */
	public static final String MARKER_UNSYNCED_PATIENT = "UNSYNCED_PATIENT";
	
	private final IHMarkerRepository ihRepository;
	
	public IHMarkerService(IHMarkerRepository ihRepository) {
		this.ihRepository = ihRepository;
	}
	
	public IHMarker save(IHMarker ihMarker) {
		return ihRepository.save(ihMarker);
	}
	
	public IHMarker findByName(String name) {
		
		IHMarker marker = ihRepository.findByName(name);
		
		if (marker == null) {
			marker = new IHMarker();
			marker.setName(name);
			marker.setLastSyncTime(toFormattedDateNow("yyyy-MM-dd HH:mm:ss"));
			save(marker);
		}
		return marker;
	}
	
	@Transactional
	public void updateMarkerByName(String name) {
		ihRepository.updateLastSyncTimeByName(name);
	}
	
	/**
	 * Marker row for draining {@code unsync_patient} by primary key cursor ({@code last_id}).
	 */
	@Transactional
	public IHMarker getOrCreateUnsyncedPatientProgressMarker() {
		IHMarker marker = ihRepository.findByName(MARKER_UNSYNCED_PATIENT);
		if (marker == null) {
			marker = new IHMarker();
			marker.setName(MARKER_UNSYNCED_PATIENT);
			marker.setLastSyncTime(toFormattedDateNow("yyyy-MM-dd HH:mm:ss"));
			marker.setLastId(0);
			return save(marker);
		}
		if (marker.getLastId() == null) {
			marker.setLastId(0);
			return save(marker);
		}
		return marker;
	}
	
	@Transactional
	public void updateUnsyncedPatientMarkerLastId(int lastUnsyncPatientRowId) {
		IHMarker marker = ihRepository.findByName(MARKER_UNSYNCED_PATIENT);
		if (marker == null) {
			return;
		}
		marker.setLastId(lastUnsyncPatientRowId);
		marker.setLastSyncTime(toFormattedDateNow("yyyy-MM-dd HH:mm:ss"));
		ihRepository.save(marker);
	}
	
}
