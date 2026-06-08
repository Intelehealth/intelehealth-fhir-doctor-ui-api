package org.openmrs.module.ihmodule.api.patientexchange.service;

import static org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils.toFormattedDateNow;

import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;
import org.openmrs.module.ihmodule.api.patientexchange.repository.IHMarkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("ihmoduleIHMarkerService")
public class IHMarkerServiceImpl implements IHMarkerService {
	
	private final IHMarkerRepository ihRepository;
	
	public IHMarkerServiceImpl(IHMarkerRepository ihRepository) {
		this.ihRepository = ihRepository;
	}
	
	@Override
	public IHMarker save(IHMarker ihMarker) {
		return ihRepository.save(ihMarker);
	}
	
	@Override
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
	
	@Override
	@Transactional
	public void updateMarkerByName(String name) {
		ihRepository.updateLastSyncTimeByName(name);
	}
	
	@Override
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
	
	@Override
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
