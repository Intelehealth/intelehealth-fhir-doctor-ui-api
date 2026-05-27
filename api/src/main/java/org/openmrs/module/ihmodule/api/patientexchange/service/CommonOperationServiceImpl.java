package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.openmrs.module.ihmodule.api.patientexchange.domain.CompeletdVisit;
import org.openmrs.module.ihmodule.api.patientexchange.domain.CompletedRecord;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.dto.DrugAndConceptDTO;
import org.openmrs.module.ihmodule.api.patientexchange.dto.LocationDTO;
import org.openmrs.module.ihmodule.api.patientexchange.dto.Person;
import org.openmrs.module.ihmodule.api.patientexchange.dto.ServiceRequestUuids;
import org.springframework.stereotype.Service;

@Service("commonOperationService")
public class CommonOperationServiceImpl implements CommonOperationService {
	
	@PersistenceContext
	private EntityManager em;
	
	@Override
	public Integer findLoationByUuid(String table, String uuid, String name) {
		System.err.println(uuid + ":" + name);
		String sql = "select location_id from " + table + " where uuid=:uuid or name=:name ";
		Integer id = null;
		List l = em.createNativeQuery(sql).setParameter("uuid", uuid).setParameter("name", name).getResultList();
		for (Object p : l) {
			id = (Integer) p;
		}
		if (id == null) {
			return 0;
		}
		return id;
	}
	
	@Override
	public Integer findResourceIdByUuid(String table, String uuid, String primaryKey) {
		String sql = "select " + primaryKey + " from " + table + " where uuid=:uuid ";
		Integer id = null;
		List l = em.createNativeQuery(sql).setParameter("uuid", uuid).getResultList();
		for (Object p : l) {
			id = (Integer) p;
		}
		if (id == null) {
			return 0;
		}
		return id;
	}
	
	@Override
	public String findResourceUuidByName(String table, String inputName, String inputFiledName, String outputFiled) {
		String sql = "select " + outputFiled + " from " + table + " where " + inputFiledName + "=:inputName ";
		System.err.println(sql);
		String uuid = null;
		List l = em.createNativeQuery(sql).setParameter("inputName", inputName).getResultList();
		for (Object p : l) {
			uuid = (String) p;
		}
		if (uuid == null) {
			return null;
		}
		return uuid;
	}
	
	@Override
	@Transactional
	public Integer updateResource(String table, Integer id, String uuid, String conditionId) {
		String sql = " update  " + table + " set uuid=:uuid where " + conditionId + "=:id ";
		em.createNativeQuery(sql).setParameter("uuid", uuid).setParameter("id", id).executeUpdate();
		return 0;
	}
	
	@Override
	public Integer findLocationByUuid(String table, String uuid, String name) {
		System.err.println(uuid + ":" + name);
		String sql = "select location_id from " + table + " where uuid=:uuid or name=:name ";
		Integer id = null;
		List l = em.createNativeQuery(sql).setParameter("uuid", uuid).setParameter("name", name).getResultList();
		for (Object p : l) {
			id = (Integer) p;
		}
		if (id == null) {
			return 0;
		}
		return id;
	}
	
	@Override
	public String findConceptUuidByMappingCode(String codes) {
		String sql = " SELECT c.uuid  from concept c join concept_reference_map crm on c.concept_id =crm.concept_id "
		        + " join concept_reference_term crt on crm.concept_reference_term_id =crt.concept_reference_term_id "
		        + " where crt.code in(" + codes + ") ";
		System.err.println(sql);
		String uuid = null;
		List l = em.createNativeQuery(sql)
		// .setParameter("codes", codes)
		        .getResultList();
		for (Object p : l) {
			uuid = (String) p;
		}
		if (uuid == null) {
			return null;
		}
		return uuid;
	}
	
	@Override
	public String findFrequencyUuidByMappingCode(String codes) {
		String sql = " SELECT ofre.uuid  from concept c join concept_reference_map crm on c.concept_id =crm.concept_id "
		        + " join concept_reference_term crt on crm.concept_reference_term_id =crt.concept_reference_term_id "
		        + " join order_frequency ofre on ofre.concept_id =c.concept_id "
		        + " where crt.code in(" + codes + ") ";
		System.err.println(sql);
		String uuid = null;
		List l = em.createNativeQuery(sql)
		// .setParameter("codes", codes)
		        .getResultList();
		for (Object p : l) {
			uuid = (String) p;
		}
		if (uuid == null) {
			return null;
		}
		return uuid;
	}
	
	@Override
	public DrugAndConceptDTO findDrugAndConceptUuidByMappingCode(String codes) {
		String sql = " SELECT d.uuid,c.uuid cuuid from concept c join concept_reference_map crm on c.concept_id =crm.concept_id "
		        + " join concept_reference_term crt on crm.concept_reference_term_id =crt.concept_reference_term_id "
		        + " join drug d on d.concept_id =c.concept_id" + " where crt.code in(" + codes + ") ";
		System.err.println(sql);
		String uuid = null;
		List resultList = em.createNativeQuery(sql)
		// .setParameter("codes", codes)
		        .getResultList();
		DrugAndConceptDTO drugAndConceptDTO = new DrugAndConceptDTO();
		Iterator iter = null;
		String[][] resultSet = new String[resultList.size()][];
		for (iter = resultList.iterator(); iter.hasNext();) {
			Object[] resultArray = (Object[]) iter.next();
			drugAndConceptDTO.setDrugUuid(resultArray[0].toString());
			drugAndConceptDTO.setMedicineUUid(resultArray[1].toString());
		}
		return drugAndConceptDTO;
	}
	
	@Override
	public String findCOnceptUuidByName(String name) {
		String sql = " SELECT c.uuid from concept c join concept_name cn on c.concept_id =cn.concept_id "
		        + "	where cn.locale_preferred =1 and cn.name =:name";
		System.err.println(sql);
		String uuid = null;
		List l = em.createNativeQuery(sql).setParameter("name", name).getResultList();
		for (Object p : l) {
			uuid = (String) p;
		}
		if (uuid == null) {
			return null;
		}
		return uuid;
	}
	
	@Override
	public ServiceRequestUuids findServiceRequestRelatedUuidsByEncounterUuid(String encounterUUid) {
		String sql = " SELECT v.uuid visit,l.uuid location,et.uuid en, e.encounter_id encounter_id from encounter e join visit v on e.visit_id  =v.visit_id "
		        + " join location l on v.location_id =l.location_id join encounter_type et on et.encounter_type_id =e.encounter_type "
		        + " where e.uuid =:encounterUUid ";
		System.err.println(sql);
		String uuid = null;
		List resultList = em.createNativeQuery(sql).setParameter("encounterUUid", encounterUUid).getResultList();
		Iterator iter = null;
		
		ServiceRequestUuids serviceRequestUuid = new ServiceRequestUuids();
		String[][] resultSet = new String[resultList.size()][];
		for (iter = resultList.iterator(); iter.hasNext();) {
			Object[] resultArray = (Object[]) iter.next();
			serviceRequestUuid.setVisitUuid(resultArray[0].toString());
			serviceRequestUuid.setLocationUuid(resultArray[1].toString());
			serviceRequestUuid.setEncounterTypeUuid(resultArray[2].toString());
			serviceRequestUuid.setEncounterId(Integer.parseInt(resultArray[3].toString()));
		}
		return serviceRequestUuid;
	}
	
	@Override
	@Transactional
	public Integer updateObsComplexValue(String table, Integer id, String complex, String title, String conditionId) {
		String sql = " update  " + table + " set value_complex=:complex , interpretation =:title  where " + conditionId
		        + "=:id ";
		em.createNativeQuery(sql).setParameter("title", title).setParameter("complex", complex).setParameter("id", id)
		        .executeUpdate();
		return 0;
	}
	
	@Override
	@Transactional
	public Integer updateOrderEncounterId(Integer encounterId, String uuid) {
		String sql = " update  orders set encounter_id=:encounterId where uuid=:uuid ";
		em.createNativeQuery(sql).setParameter("encounterId", encounterId).setParameter("uuid", uuid).executeUpdate();
		return 0;
	}
	
	@Override
	public LocationDTO findLocationdByUuid(String uuid) {
		String sql = "select name,location_id from location where uuid=:uuid ";
		LocationDTO dto = new LocationDTO();
		List resultList = em.createNativeQuery(sql).setParameter("uuid", uuid).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			Object[] resultArray = (Object[]) iter.next();
			dto.setName(resultArray[0].toString());
		}
		return dto;
	}
	
	@Override
	public Person getPatientInformation(String uuid) {
		String sql = "select gender,TIMESTAMPDIFF(YEAR, birthdate , CURDATE()) AS age, pi2.identifier  from person p join patient_identifier pi2 on p.person_id =pi2.patient_id   where p.uuid=:uuid ";
		Person dto = new Person();
		List resultList = em.createNativeQuery(sql).setParameter("uuid", uuid).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			Object[] resultArray = (Object[]) iter.next();
			dto.setGender(resultArray[0].toString());
			dto.setAge(resultArray[1].toString());
			dto.setIdentifier(resultArray[2].toString());
		}
		return dto;
	}
	
	@Override
	public List<CompeletdVisit> getCompletedVisit(String date) {
		String sql = "select v.uuid visit,p.uuid person,coalesce(e.date_changed , e.date_created) ,v.visit_id from encounter e join  visit v on e.visit_id = v.visit_id join person p on p.person_id =e.patient_id "
		        + " where v.date_stopped is not null and e.date_created > :date or e.date_changed > :date order by e.date_created  asc  limit 100 ";
		
		List<CompeletdVisit> visits = new ArrayList<CompeletdVisit>();
		
		List resultList = em.createNativeQuery(sql).setParameter("date", date).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompeletdVisit theVisit = new CompeletdVisit();
			Object[] resultArray = (Object[]) iter.next();
			theVisit.setVisit(resultArray[0].toString());
			theVisit.setPatient(resultArray[1].toString());
			theVisit.setDate(resultArray[2].toString());
			theVisit.setVisitId(Integer.parseInt(resultArray[3].toString()));
			visits.add(theVisit);
		}
		return visits;
	}
	
	@Override
	public List<CompletedRecord> getCompletedEncounters(List<Integer> ids) {
		String sql = "SELECT e.uuid ,e.encounter_id  from encounter e  WHERE e.visit_id in :ids";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("ids", ids).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<CompletedRecord> getCompletedEncounter(Integer id) {
		String sql = "SELECT e.uuid ,e.encounter_id  from encounter e  WHERE e.visit_id = :id";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("id", id).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<CompletedRecord> getCompletedObs(List<Integer> ids) {
		String sql = "SELECT o.uuid ,o.obs_id  from obs o  WHERE o.encounter_id in :ids";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("ids", ids).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<CompletedRecord> getCompletedObs(Integer id) {
		String sql = "SELECT o.uuid ,o.obs_id  from obs o  WHERE o.encounter_id=:id";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("id", id).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<CompletedRecord> getCompletedServiceRequest(Integer id, int type) {
		String sql = "SELECT o.uuid ,o.order_id  from orders o  WHERE o.encounter_id=:id and order_type_id=:type and voided=false";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("id", id).setParameter("type", type).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<CompletedRecord> getCompletedDiagnosticReport(String date_created) {
		String sql = "SELECT uuid,diagnostic_report_id,date_created from fhir_diagnostic_report where date_created >:date_created ";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("date_created", date_created).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			theRecord.setDateCreated(resultArray[2].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<CompletedRecord> getCompletedMedication(String date_created) {
		String sql = "SELECT d.uuid ,d.drug_id ,coalesce(d.date_changed , d.date_created) date_created  from drug d "
		        + " where  d.date_created > :date_created or d.date_changed > :date_created "
		        + " order by  coalesce(d.date_changed , d.date_created) asc";
		
		List<CompletedRecord> records = new ArrayList<CompletedRecord>();
		
		List resultList = em.createNativeQuery(sql).setParameter("date_created", date_created).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			theRecord.setDateCreated(resultArray[2].toString());
			records.add(theRecord);
		}
		return records;
	}
	
	@Override
	public List<PersonAttribute> findPersonAttributes(String patientUUID) {
		String sql = "select" + "	p.person_id personId, pat.name name , pa.value value" + " from" + "	person_attribute pa"
		        + " join person_attribute_type pat on" + "	pa.person_attribute_type_id = pat.person_attribute_type_id "
		        + " join person p on pa.person_id=p.person_id" + " where p.uuid=:patientUUID and pa.voided = false";
		
		System.err.println(sql);
		
		List<PersonAttribute> extensions = new ArrayList<PersonAttribute>();
		
		List resultList = em.createNativeQuery(sql).setParameter("patientUUID", patientUUID).getResultList();
		
		for (Iterator iter = resultList.iterator(); iter.hasNext();) {
			Object[] resultArray = (Object[]) iter.next();
			PersonAttribute extension = new PersonAttribute();
			extension.setName(resultArray[1].toString());
			extension.setValue(resultArray[2].toString());
			extensions.add(extension);
		}
		return extensions;
	}
	
	@Override
	public boolean existsPatientByDemographicsAndPhone(String familyName, String givenName, String gender, String birthDate,
	        String phone) {
		String normalizedGender = gender == null ? "" : gender.trim().toLowerCase();
		String genderCode = "";
		if ("female".equals(normalizedGender) || "f".equals(normalizedGender)) {
			genderCode = "f";
		} else if ("male".equals(normalizedGender) || "m".equals(normalizedGender)) {
			genderCode = "m";
		}
		StringBuilder sql = new StringBuilder();
		sql.append("select count(distinct p.person_id) ");
		sql.append("from person p ");
		sql.append("join patient pat on pat.patient_id = p.person_id and pat.voided = false ");
		sql.append("join person_name pn on pn.person_id = p.person_id and pn.voided = false ");
		sql.append("where p.voided = false ");
		sql.append("and (lower(trim(p.gender)) = :gender or lower(trim(p.gender)) = :genderCode) ");
		sql.append("and date(p.birthdate) = :birthDate ");
		sql.append("and lower(trim(pn.family_name)) = :familyName ");
		sql.append("and lower(trim(pn.given_name)) = :givenName ");
		if (phone != null && !phone.trim().isEmpty()) {
			sql.append("and exists (");
			sql.append("  select 1 from person_attribute pa ");
			sql.append("  join person_attribute_type paty on paty.person_attribute_type_id = pa.person_attribute_type_id ");
			sql.append("  where pa.person_id = p.person_id and pa.voided = false ");
			sql.append("    and lower(trim(pa.value)) = :phone ");
			sql.append("    and lower(replace(replace(replace(paty.name, ' ', ''), '-', ''), '_', '')) in ");
			sql.append("        ('telephonenumber','emergencycontactnumber','phonenumber')");
			sql.append(") ");
		}
		javax.persistence.Query query = em.createNativeQuery(sql.toString()).setParameter("gender", normalizedGender)
		        .setParameter("genderCode", genderCode).setParameter("birthDate", birthDate)
		        .setParameter("familyName", familyName == null ? "" : familyName.trim().toLowerCase())
		        .setParameter("givenName", givenName == null ? "" : givenName.trim().toLowerCase());
		if (phone != null && !phone.trim().isEmpty()) {
			query.setParameter("phone", phone.trim().toLowerCase());
		}
		Number count = (Number) query.getSingleResult();
		return count != null && count.intValue() > 0;
	}
	
}

