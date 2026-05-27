package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.util.List;

import org.openmrs.module.ihmodule.api.patientexchange.domain.CompeletdVisit;
import org.openmrs.module.ihmodule.api.patientexchange.domain.CompletedRecord;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.dto.DrugAndConceptDTO;
import org.openmrs.module.ihmodule.api.patientexchange.dto.LocationDTO;
import org.openmrs.module.ihmodule.api.patientexchange.dto.Person;
import org.openmrs.module.ihmodule.api.patientexchange.dto.ServiceRequestUuids;

/**
 * Common cross-cutting DB operations used by multiple patient exchange workflows.
 * <p>
 * This is an interface so Spring transaction proxies remain assignable during injection.
 */
public interface CommonOperationService {
	
	Integer findLoationByUuid(String table, String uuid, String name);
	
	Integer findResourceIdByUuid(String table, String uuid, String primaryKey);
	
	String findResourceUuidByName(String table, String inputName, String inputFiledName, String outputFiled);
	
	Integer updateResource(String table, Integer id, String uuid, String conditionId);
	
	Integer findLocationByUuid(String table, String uuid, String name);
	
	String findConceptUuidByMappingCode(String codes);
	
	String findFrequencyUuidByMappingCode(String codes);
	
	DrugAndConceptDTO findDrugAndConceptUuidByMappingCode(String codes);
	
	String findCOnceptUuidByName(String name);
	
	ServiceRequestUuids findServiceRequestRelatedUuidsByEncounterUuid(String encounterUUid);
	
	Integer updateObsComplexValue(String table, Integer id, String complex, String title, String conditionId);
	
	Integer updateOrderEncounterId(Integer encounterId, String uuid);
	
	LocationDTO findLocationdByUuid(String uuid);
	
	Person getPatientInformation(String uuid);
	
	List<CompeletdVisit> getCompletedVisit(String date);
	
	List<CompletedRecord> getCompletedEncounters(List<Integer> ids);
	
	List<CompletedRecord> getCompletedEncounter(Integer id);
	
	List<CompletedRecord> getCompletedObs(List<Integer> ids);
	
	List<CompletedRecord> getCompletedObs(Integer id);
	
	List<CompletedRecord> getCompletedServiceRequest(Integer id, int type);
	
	List<CompletedRecord> getCompletedDiagnosticReport(String date_created);
	
	List<CompletedRecord> getCompletedMedication(String date_created);
	
	List<PersonAttribute> findPersonAttributes(String patientUUID);
	
	boolean existsPatientByDemographicsAndPhone(String familyName, String givenName, String gender, String birthDate,
	        String phone);
	
}
