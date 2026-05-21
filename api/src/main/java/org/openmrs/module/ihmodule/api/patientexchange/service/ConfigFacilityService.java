package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.openmrs.module.ihmodule.api.patientexchange.domain.ConfigFacility;
import org.springframework.stereotype.Service;

@Service
public class ConfigFacilityService {
	
	@PersistenceContext
	private EntityManager em;
	
	public ConfigFacility getConfigFacitlity(int id) {
		String sql = "select id, facility_name, facility_uuid, status, "
		        + "	prescription_api, referral_api,	lab_api, appointment_api, uuid " + " from config_fcility where id:id";
		
		Query q = em.createNativeQuery(sql, Tuple.class);
		q.setParameter("id", id);
		
		List<Tuple> rows = q.getResultList();
		
		ConfigFacility dto = new ConfigFacility();
		
		for (Tuple row : rows) {
			dto.setId(Integer.parseInt(row.get("id").toString()));
			dto.setFacilityName(row.get("facility_name").toString());
			dto.setFacilityUuid(row.get("facility_uuid").toString());
			dto.setStatus(Boolean.parseBoolean(row.get("status").toString()));
			dto.setPrescriptionApi(row.get("prescription_api").toString());
			dto.setReferralApi(row.get("referral_api").toString());
			dto.setLabApi(row.get("lab_api").toString());
			dto.setAppointmentApi(row.get("appointment_api").toString());
			dto.setUuid(row.get("uuid").toString());
			break;
		}
		return dto;
	}
	
}
