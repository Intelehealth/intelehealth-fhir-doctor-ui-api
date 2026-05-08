package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.openmrs.module.ihmodule.api.patientexchange.domain.PatientDTO;
import org.springframework.stereotype.Service;

@Service
public class PatientDataService {
	
	@PersistenceContext
	private EntityManager em;
	
	public List<PatientDTO> getCreatedPatients(String date) {
		String sql = "SELECT p2.uuid FROM "
				+ " patient p join "
				+ " person p2 on p2.person_id = p.patient_id  join "
				+ " patient_identifier pi2 on pi2.patient_id = p.patient_id left join"
				+ " person_name p3 on p3.person_id = p2.person_id left join "
				+ " person_address pa on pa.person_id = p2.person_id left join "
				+ " person_attribute pea on pea.person_id = p2.person_id"
				+ " where "
				+ "	p.date_created >= :date or"
				+ "	p2.date_created >= :date or"
				+ "	pi2.date_created >= :date or"
				+ "	p3.date_created >= :date or"
				+ "	pa.date_created >= :date or"
				+ "	pea.date_created >= :date";

		Query q = em.createNativeQuery(sql, Tuple.class);
		q.setParameter("date", date);

		List<Tuple> rows = q.getResultList();

		ArrayList<PatientDTO> patientList = new ArrayList<>();

		for (Tuple row : rows) {
			PatientDTO dto = new PatientDTO();
			dto.setUuid(row.get("uuid").toString());
			patientList.add(dto);
		}
		return patientList;
	}
	
	public List<PatientDTO> getModifiedPatients(String date) {
		String sql = "SELECT p2.uuid FROM "
				+ " patient p join "
				+ " person p2 on p2.person_id = p.patient_id  join "
				+ " patient_identifier pi2 on pi2.patient_id = p.patient_id left join"
				+ " person_name p3 on p3.person_id = p2.person_id left join "
				+ " person_address pa on pa.person_id = p2.person_id left join"
				+ " person_attribute pea on pea.person_id = p2.person_id"
				+ " where "
				+ " p.date_changed >= :date 	or "
				+ "	p2.date_changed >= :date 	or "
				+ "	pi2.date_changed >= :date 	or "
				+ "	p3.date_changed >= :date 	or "
				+ "	pa.date_changed >= :date 	or "
				+ "	pea.date_changed >= :date";

		Query q = em.createNativeQuery(sql, Tuple.class);
		q.setParameter("date", date);

		List<Tuple> rows = q.getResultList();

		ArrayList<PatientDTO> patientList = new ArrayList<>();

		for (Tuple row : rows) {
			PatientDTO dto = new PatientDTO();
			dto.setUuid(row.get("uuid").toString());
			patientList.add(dto);
		}
		return patientList;
	}
}
