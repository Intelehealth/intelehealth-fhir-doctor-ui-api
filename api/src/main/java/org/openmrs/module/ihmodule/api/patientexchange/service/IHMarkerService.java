package org.openmrs.module.ihmodule.api.patientexchange.service;

import static org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils.toFormattedDateNow;

import javax.transaction.Transactional;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;
import org.openmrs.module.ihmodule.api.patientexchange.repository.IHMarkerRepository;
import org.springframework.stereotype.Service;

@Service
public class IHMarkerService {
	
	private IHMarkerRepository ihRepository = Context.getRegisteredComponent("iHMarkerRepository", IHMarkerRepository.class);
	
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
	
}
