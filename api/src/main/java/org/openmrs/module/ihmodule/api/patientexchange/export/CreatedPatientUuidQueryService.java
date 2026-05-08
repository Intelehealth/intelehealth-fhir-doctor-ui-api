package org.openmrs.module.ihmodule.api.patientexchange.export;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Service;

@Service
public class CreatedPatientUuidQueryService {
	
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	@PersistenceContext
	private EntityManager em;
	
	public List<String> findCreatedPatientUuids(String startDate, String endDate) {
		LocalDate start = LocalDate.parse(startDate, DATE_FORMAT);
		LocalDate end = LocalDate.parse(endDate, DATE_FORMAT);
		
		LocalDateTime startDateTime = LocalDateTime.of(start, LocalTime.MIN);
		LocalDateTime endDateTime = LocalDateTime.of(end, LocalTime.MAX.withNano(0));
		
		String sql = "SELECT DISTINCT p.uuid " + "FROM patient pt " + "JOIN person p ON p.person_id = pt.patient_id "
		        + "WHERE p.date_created BETWEEN :startDate AND :endDate " + "ORDER BY p.uuid";
		
		return em.createNativeQuery(sql).setParameter("startDate", DATE_TIME_FORMAT.format(startDateTime))
		        .setParameter("endDate", DATE_TIME_FORMAT.format(endDateTime)).getResultList();
	}
}
