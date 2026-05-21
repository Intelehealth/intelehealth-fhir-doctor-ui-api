package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.datatype.QueryTable;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PrescriptionDomain;
import org.openmrs.module.ihmodule.api.patientexchange.dto.DrugAndConceptDTO;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.DataParse;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ImportPrescriptionService extends IHConstant {
	
	@Autowired
	private IHMarkerService ihMarkerService;
	
	FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private FhirConfig firFhirConfig;
	
	@Autowired
	private CommonOperationService commonOperationService;
	
	@Autowired
	private ImportObservationService importObservationService;
	
	@Autowired
	private ImportEncounterService importEncounterService;
	
	@Autowired
	private ImportLabOrderService importLabOrderService;
	
	ObjectMapper mapper = new ObjectMapper();
	
	public void importPrescription(String patientId) throws JsonProcessingException, UnsupportedEncodingException,
	        JSONException {
		IQuery<Bundle> medicationRequest = firFhirConfig.getOpenCRFhirContext().search()
		        .forResource(MedicationRequest.class).where(MedicationRequest.SUBJECT.hasAnyOfIds(patientId)).sort()
		        .ascending("_lastUpdated").returnBundle(Bundle.class);
		Bundle medicationRequestRequestTasksBundle = medicationRequest.execute();
		saveToLocal(medicationRequestRequestTasksBundle);
		
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle) throws UnsupportedEncodingException, JSONException,
	        JsonProcessingException {
		Bundle transactionBundle = new Bundle();
		
		String table = QueryTable.ORDERS.value;
		String primaryKey = QueryTable.ORDERS_PK.value;
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			
			MedicationRequest medicationRequest = (MedicationRequest) bundleEntry.getResource();
			String medicationId = medicationRequest.getMedicationReference().getReference().split("/")[1];
			/*
			 * System.err.println("medicationId::" + medicationId + ":" +
			 * medicationRequest.getId());
			 */
			IGenericClient client = firFhirConfig.getOpenCRFhirContext();
			Bundle results = client.search().byUrl("Medication?_id=" + medicationId).returnBundle(Bundle.class).execute();
			List<BundleEntryComponent> entires = results.getEntry();
			
			Resource resource = (Resource) bundleEntry.getResource();
			
			/*
			 * System.err.println("resource.getIdElement().getIdPart()" +
			 * resource.getIdElement().getIdPart());
			 */
			if (entires.size() != 0) {
				
				String payload = generatePayload(medicationRequest, entires);
				
				// System.err.println("DDD>>>>>>>>"+fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(r));
				Integer drugOrderLocalId = commonOperationService.findResourceIdByUuid(table, resource.getIdElement()
				        .getIdPart(), primaryKey);
				if (drugOrderLocalId == null || drugOrderLocalId == 0) {
					String res = HttpWebClient.postWithBasicAuthV2(localOpenmrsOpenhimURL, "/ws/rest/v1/order",
					    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1], payload);
					String uuid = DataParse.getOrderIdFromOrderJson(res);
					System.out.println("uuid" + uuid);
					drugOrderLocalId = commonOperationService.findResourceIdByUuid(table, uuid, primaryKey);
					commonOperationService.updateResource(table, drugOrderLocalId, resource.getIdElement().getIdPart(),
					    primaryKey);
					
				} else {
					System.err.println("found order do nothong............");
					// firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r).execute();
					
				}
				
			}
		}
		return transactionBundle;
	}
	
	private String generatePayload(MedicationRequest medicationRequest, List<BundleEntryComponent> entires)
	        throws JsonProcessingException {
		List<String> codes = new ArrayList<String>();
		List<String> formCodes = new ArrayList<String>();
		List<String> routeCodes = new ArrayList<String>();
		List<String> frequencies = new ArrayList<String>();
		List<String> durationUnitCode = new ArrayList<String>();
		for (BundleEntryComponent bundleEntryComponent : entires) {
			
			/*
			 * System.err.println("RE:::" + resources.getResourceType());
			 */
			Medication medication = (Medication) bundleEntryComponent.getResource();
			List<Coding> codings = medication.getCode().getCoding();
			for (Coding coding : codings) {
				codes.add("'" + coding.getCode() + "'");
			}
			
			List<Coding> formCcodings = medication.getForm().getCoding();
			for (Coding coding : formCcodings) {
				formCodes.add("'" + coding.getCode() + "'");
			}
			
		}
		
		/*List<Coding> durationUnitCodes = medicationRequest
				.getDosageInstructionFirstRep().getTiming().getCode().getCoding();
		for (Coding coding : durationUnitCodes) {
			durationUnitCode.add("'" + coding.getCode() + "'");
		}*/
		
		List<Coding> routeCcodings = medicationRequest.getDosageInstructionFirstRep().getRoute().getCoding();
		for (Coding coding : routeCcodings) {
			routeCodes.add("'" + coding.getCode() + "'");
		}
		List<Coding> frequencyCcodings = medicationRequest.getDosageInstructionFirstRep().getTiming().getCode().getCoding();
		for (Coding coding : frequencyCcodings) {
			frequencies.add("'" + coding.getCode() + "'");
		}
		durationUnitCode.add("'"
		        + medicationRequest.getDosageInstructionFirstRep().getTiming().getRepeat().getDurationUnit() + "'");
		
		BigDecimal doseValue = medicationRequest.getDosageInstructionFirstRep().getDoseAndRate().get(0).getDoseQuantity()
		        .getValue();
		medicationRequest.getDosageInstructionFirstRep().getDoseAndRate().get(0).getDoseQuantity().getCode();
		
		// importRemoteMedicationToLocal(originalTasksBundle);
		String patient = medicationRequest.getSubject().getReference().split("/")[1];
		String encounter = medicationRequest.getEncounter().getReference().split("/")[1];
		PrescriptionDomain prescriptionDomain = new PrescriptionDomain();
		prescriptionDomain.setPatient(patient);
		prescriptionDomain.setEncounter(encounter);
		System.out.println("LLLL" + String.join(",", codes));
		DrugAndConceptDTO drugAndConceptDTO = commonOperationService.findDrugAndConceptUuidByMappingCode(String.join(",",
		    codes));
		// String formDoseUnitUuid =
		// commonOperationService.findConceptUuidByMappingCode(String.join(",",
		// formCodes));
		System.err.println(routeCodes.toString());
		String routeUuid = commonOperationService.findConceptUuidByMappingCode(String.join(",", routeCodes));
		String frequencyUuid = commonOperationService.findFrequencyUuidByMappingCode(String.join(",", frequencies));
		String durationUnitUuid = commonOperationService.findConceptUuidByMappingCode(String.join(",", durationUnitCode));
		
		System.err.println("durationUnitCode:::" + durationUnitCode);
		String quantityUnitUuid = commonOperationService.findCOnceptUuidByName(medicationRequest.getDispenseRequest()
		        .getQuantity().getUnit());
		String doseUnitUuid = commonOperationService.findCOnceptUuidByName(medicationRequest.getDosageInstructionFirstRep()
		        .getDoseAndRate().get(0).getDoseQuantity().getUnit());
		prescriptionDomain.setDrug(drugAndConceptDTO.getDrugUuid());
		prescriptionDomain.setConcept(drugAndConceptDTO.getMedicineUUid());
		
		System.err.println("routeUuid:" + routeUuid);
		prescriptionDomain.setDose(doseValue);
		
		prescriptionDomain.setRoute(routeUuid);
		prescriptionDomain.setFrequency(frequencyUuid);
		prescriptionDomain.setDoseUnits(doseUnitUuid);
		
		prescriptionDomain.setQuantity(medicationRequest.getDispenseRequest().getQuantity().getValue());
		prescriptionDomain.setQuantityUnits(quantityUnitUuid);
		prescriptionDomain.setNumRefills(medicationRequest.getDispenseRequest().getNumberOfRepeatsAllowed());
		prescriptionDomain.setDuration(medicationRequest.getDosageInstructionFirstRep().getTiming().getRepeat()
		        .getDuration());
		
		prescriptionDomain.setDurationUnits(durationUnitUuid);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String payload = "";
		
		payload = ow.writeValueAsString(prescriptionDomain);
		return payload;
	}
}
