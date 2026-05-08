package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.openmrs.module.ihmodule.api.patientexchange.datatype.ConfigFacilityDataType;
import org.openmrs.module.ihmodule.api.patientexchange.domain.ConfigDataSync;
import org.springframework.stereotype.Service;

@Service
public class ConfigDataSyncService {
	
	@PersistenceContext
	private EntityManager em;
	
	public ConfigDataSync getConfigDataSync(ConfigFacilityDataType type) {
		String sql = "select c.id, c.name, c.status from config_data_sync_module c where id=:id";
		
		Query q = em.createNativeQuery(sql, Tuple.class);
		q.setParameter("id", type.getValue());
		
		List<Tuple> rows = q.getResultList();
		
		ConfigDataSync dto = new ConfigDataSync();
		
		for (Tuple row : rows) {
			dto.setId(Integer.parseInt(row.get("id").toString()));
			dto.setName(row.get("name").toString());
			dto.setStatus(Boolean.parseBoolean(row.get("status").toString()));
			break;
		}
		return dto;
	}
	
}
