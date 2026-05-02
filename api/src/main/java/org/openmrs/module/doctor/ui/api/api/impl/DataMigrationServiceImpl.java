/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.doctor.ui.api.api.impl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.doctor.ui.api.DataMigration;
import org.openmrs.module.doctor.ui.api.DataMigrationObject;
import org.openmrs.module.doctor.ui.api.LabOrder;
import org.openmrs.module.doctor.ui.api.LabTest;
import org.openmrs.module.doctor.ui.api.Orders;
import org.openmrs.module.doctor.ui.api.api.DataMigrationService;
import org.openmrs.module.doctor.ui.api.api.dao.DataMigrationDao;
import org.openmrs.module.doctor.ui.api.utils.HttpService;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class DataMigrationServiceImpl extends BaseOpenmrsService implements DataMigrationService {
	
	DataMigrationDao dao;
	
	@Autowired
	private EncounterService encounterService;
	
	@Autowired
	private ConceptService conceptService;
	
	public void setDao(DataMigrationDao dao) {
		this.dao = dao;
	}
	
	@Override
	public List<DataMigration> getMedicineData(String date, Integer concept) throws APIException {
		Integer medication = 4;
		Integer labtest = 3;
		List<DataMigration> medications = dao.getMedicineData(date, medication);
		for (DataMigration dataMigration : medications) {
			System.err.println("DD:" + dataMigration.toString());
			migrateData(dataMigration);
		}
		List<DataMigration> tests = dao.getMedicineData(date, labtest);
		for (DataMigration dataMigration : tests) {
			System.err.println("FF:" + dataMigration.toString());
			convertLabData(dataMigration);
		}
		return dao.getMedicineData(date, concept);
	}
	
	private void migrateData(DataMigration medication) {
		DataMigrationObject obj = new DataMigrationObject();
		obj.setEncounterDatetime(medication.getEnccounterdate());
		obj.setPatient(medication.getPerson());
		obj.setEncounterType(encounterService.getEncounterType("Order").getUuid());
		obj.setVisit(medication.getVisit());
		List<Orders> orders = new ArrayList<Orders>();
		obj.setLocation(medication.getLocation());
		Orders order = new Orders();
		order.setPatient(medication.getPerson());
		List<String> medicineInformation = Arrays.asList(medication.getValue().split(":"));
		Drug drug = conceptService.getDrug(medicineInformation.get(0));
		order.setDrug(drug.getUuid());
		order.setConcept(drug.getConcept().getUuid());
		order.setDuration(BigDecimal.valueOf(Long.parseLong(medicineInformation.get(2))));
		order.setDosingInstructions(medicineInformation.get(3) + ":" + medicineInformation.get(4));
		Concept doseunits = conceptService.getConceptByName(medicineInformation.get(1));
		order.setDoseUnits(doseunits.getUuid());
		order.setQuantityUnits(doseunits.getUuid());
		orders.add(order);
		
		obj.setOrders(orders);
		
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String payload = "";
		try {
			payload = ow.writeValueAsString(obj);
			System.err.println(payload);
		}
		catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.err.println(obj.toString());
	}
	
	private void convertLabData(DataMigration medication) {
		LabTest laboder = new LabTest();
		laboder.setPatient(medication.getPerson());
		laboder.setEncounterType(encounterService.getEncounterType("Order").getUuid());
		laboder.setVisit(medication.getVisit());
		laboder.setEncounterDatetime(medication.getEnccounterdate());
		laboder.setLocation(medication.getLocation());
		List<LabOrder> orders = new ArrayList<LabOrder>();
		LabOrder theOrder = new LabOrder();
		theOrder.setPatient(medication.getPerson());
		List<String> lab = Arrays.asList(medication.getValue().split(":"));
		Concept theLab = conceptService.getConceptByName(lab.get(0));
		theOrder.setConcept(theLab.getUuid());
		orders.add(theOrder);
		laboder.setOrders(orders);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String payload = "";
		try {
			payload = ow.writeValueAsString(laboder);
			System.err.println(payload);
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public List<DataMigration> getTestData(String date) throws APIException {
		return null;
	}
	
	@Override
	public String getMember(String resourceType, Map<String, String> reqParam) throws Exception {
		StringBuilder queryParam = new StringBuilder();
		for (Map.Entry<String, String> entry : reqParam.entrySet()) {
			queryParam.append("&" + entry.getKey() + "=" + URLEncoder.encode(entry.getValue()));
		}
		
		String theSearchParamString = "";
		
		if (queryParam.length() > 1) {
			theSearchParamString = queryParam.substring(1);
		}
		
		// openCR url
		String url = "http://192.168.19.152:5001/shr/rest/v1/bundle/" + resourceType + "?" + theSearchParamString;
		System.err.println("URL::::" + url);
		
		String data = new HttpService().getPatientData(url, "", "openmrs", "Admin123");
		
		return data;
		/*return new HttpService().getPatient("http://localhost:5001/shr/rest/v1/bundle", resourceType + theSearchParamString,
		    "openmrs", "Admin123");*/
		
	}
	
	@Override
	public String importPatient(String uuid) throws Exception {
		// lcoal openmrs url  6001 for o3 openmrs 5001 for IH openrms
		String url = "http://192.168.19.152:6001/patientinfo/rest/v1/patient/import/" + uuid;
		String data = new HttpService().getPatientData(url, "", "openmrs", "Admin123");
		return data;
	}
	
	@Override
	public String importPatient(String uuid, String location) throws Exception {
		// lcoal openmrs url  6001 for o3 openmrs 5001 for IH openrms
		String url = "http://192.168.19.152:5001/patientinfo/rest/v1/patient/import/" + uuid + "/" + location;
		String data = new HttpService().getPatientData(url, "", "openmrs", "Admin123");
		return data;
	}
	
}
