package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.datatype.QueryTable;
import org.openmrs.module.ihmodule.api.patientexchange.domain.Orders;
import org.openmrs.module.ihmodule.api.patientexchange.domain.ServiceRequestDomain;
import org.openmrs.module.ihmodule.api.patientexchange.dto.ServiceRequestUuids;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.DataParse;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.openmrs.module.ihmodule.api.patientexchange.utils.LabOrderPayload;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.IQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Service
public class ImportLabOrderService extends IHConstant {
	
	private IHMarkerService ihMarkerService = Context.getRegisteredComponent("IHMarkerService", IHMarkerService.class);
	
	FhirContext fhirContext = FhirContext.forR4();
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
	    CommonOperationService.class);
	
	ObjectMapper mapper = new ObjectMapper();
	
	public void importLabOrder(String patientId) throws UnsupportedEncodingException, JSONException, JsonProcessingException {
		IQuery<Bundle> serviceRequest = firFhirConfig.getOpenCRFhirContext().search().forResource(ServiceRequest.class)
		        .where(ServiceRequest.SUBJECT.hasAnyOfIds(patientId)).sort().ascending("_lastUpdated")
		        .returnBundle(Bundle.class);
		Bundle serviceRequestTasksBundle = serviceRequest.execute();
		saveToLocal(serviceRequestTasksBundle);
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle) throws UnsupportedEncodingException, JSONException,
	        JsonProcessingException {
		Bundle transactionBundle = new Bundle();
		String table = QueryTable.ORDERS.value;
		String primaryKey = QueryTable.ORDERS_PK.value;
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			
			LabOrderPayload labOrderPayload = generateOrder(bundleEntry);
			String payload = labOrderPayload.getPayload();
			Resource resource = (Resource) bundleEntry.getResource();
			System.err.println(payload);
			/*
			 * System.err.println("DDD>>>>>>>>" +
			 * fhirContext.newJsonParser().setPrettyPrint(true)
			 * .encodeResourceToString(serviceRequest));
			 */
			
			// extra encounter is created shoud remove
			Integer drugOrderLocalId = commonOperationService.findResourceIdByUuid(table, resource.getIdElement()
			        .getIdPart(), primaryKey);
			if (drugOrderLocalId == null || drugOrderLocalId == 0) {
				try {
					String res = HttpWebClient.postWithBasicAuthV2(localOpenmrsOpenhimURL, "/ws/rest/v1/encounter",
					    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1], payload);
					
					String uuid = DataParse.getOrderIdFromEncounterJson(res);
					
					drugOrderLocalId = commonOperationService.findResourceIdByUuid(table, uuid, primaryKey);
					commonOperationService.updateResource(table, drugOrderLocalId, resource.getIdElement().getIdPart(),
					    primaryKey);
					commonOperationService.updateOrderEncounterId(labOrderPayload.getServiceRequestUuids().getEncounterId(),
					    resource.getIdElement().getIdPart());
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.err.println("found order do nothong............");
				//					firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r).execute();
				
			}
		}
		return transactionBundle;
	}
	
	private LabOrderPayload generateOrder(BundleEntryComponent bundleEntry) throws JsonProcessingException {
		
		ServiceRequest serviceRequest = (ServiceRequest) bundleEntry.getResource();
		
		String conceptUuid = "";
		
		List<Coding> codings = serviceRequest.getCode().getCoding();
		List<String> codes = new ArrayList<String>();
		for (Coding coding : codings) {
			codes.add("'" + coding.getCode() + "'");
		}
		String[] patient = serviceRequest.getSubject().getReference().split("/");
		String[] encounter = serviceRequest.getEncounter().getReference().split("/");
		
		ServiceRequestUuids serviceRequestUuids = commonOperationService
		        .findServiceRequestRelatedUuidsByEncounterUuid(encounter[1]);
		// System.err.println(serviceRequestUuids);
		conceptUuid = commonOperationService.findConceptUuidByMappingCode(String.join(",", codes));
		
		serviceRequest.getCode().getCoding().get(0).setCode(conceptUuid);
		// System.err.println("localOpenMRSUUID::::" + conceptUuid);
		
		ServiceRequestDomain serviceRequestDomain = new ServiceRequestDomain();
		serviceRequestDomain.setPatient(patient[1]);
		serviceRequestDomain.setLocation(serviceRequestUuids.getLocationUuid());
		serviceRequestDomain.setEncounterType(serviceRequestUuids.getEncounterTypeUuid());
		serviceRequestDomain.setEncounterDatetime(serviceRequest.getOccurrencePeriod().getStart().toInstant().toString());
		serviceRequestDomain.setVisit(serviceRequestUuids.getVisitUuid());
		List<Orders> orders = new ArrayList<Orders>();
		Orders order = new Orders();
		order.setPatient(patient[1]);
		order.setConcept(conceptUuid);
		order.setEncounter(encounter[1]);
		orders.add(order);
		serviceRequestDomain.setOrders(orders);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String payload = "";
		
		payload = ow.writeValueAsString(serviceRequestDomain);
		LabOrderPayload labOrderPayload = new LabOrderPayload();
		labOrderPayload.setPayload(payload);
		labOrderPayload.setServiceRequestUuids(serviceRequestUuids);
		return labOrderPayload;
	}
	
}
