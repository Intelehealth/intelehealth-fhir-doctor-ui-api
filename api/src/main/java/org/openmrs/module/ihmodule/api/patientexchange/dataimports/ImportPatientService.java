package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.datatype.QueryTable;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.service.VisitTypeService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImportPatientService extends IHConstant {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
	    CommonOperationService.class);
	
	private ImportObservationService importObservationService = Context.getRegisteredComponent("importObservationService",
	    ImportObservationService.class);
	
	private ImportEncounterService importEncounterService = Context.getRegisteredComponent("importEncounterService",
	    ImportEncounterService.class);
	
	private ImportLabOrderService importLabOrderService = Context.getRegisteredComponent("importLabOrderService",
	    ImportLabOrderService.class);
	
	private ImportLocationService importLocationService = Context.getRegisteredComponent("importLocationService",
	    ImportLocationService.class);
	
	private VisitTypeService visitType = Context.getRegisteredComponent("visitTypeService", VisitTypeService.class);
	
	ObjectMapper mapper = new ObjectMapper();
	
	public void importPatient(String patientId, String locationUuid) throws JSONException, ParseException, IOException {
		
		System.err.println("PatientId: " + patientId + " ,LocationId: " + locationUuid);
		
		String data = HttpWebClient.get(mciURL, "rest/v1/bundle/Patient?_id=" + patientId,
		    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);
		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
		
		if (theBundle.hasEntry()) {
			System.out.println("Got bundle size" + theBundle.getEntry().size());
		}
		
		saveToLocal(theBundle, QueryTable.PERSON.value, QueryTable.PERSON_PK.value, locationUuid);
		
		try {
			importEncounterService.importEncounter(patientId, locationUuid);
		}
		catch (Exception e) {
			
			e.printStackTrace();
		}
		
		try {
			importObservationService.importObservation(patientId);
		}
		catch (Exception e) {
			
			e.printStackTrace();
		}
		
		try {
			importLabOrderService.importLabOrder(patientId);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		System.err.println("Done");
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle, String table, String primaryKey, String locationUuid)
	        throws JsonProcessingException, UnsupportedEncodingException, JSONException, ParseException {
		Bundle transactionBundle = new Bundle();
		
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			
			Patient patient = (Patient) bundleEntry.getResource();
			
			Resource resource = (Resource) bundleEntry.getResource();
			
			List<Identifier> identifiers = patient.getIdentifier();
			int i = 0;
			//			int removeIndex = -1;
			
			// handle multiple identifier
			for (Identifier identifier : patient.getIdentifier()) {
				
				String code = identifier.getType().getCodingFirstRep().getCode();
				System.out.println("code::::" + code);
				String identifierUUid = commonOperationService.findResourceUuidByName("patient_identifier_type", patient
				        .getIdentifier().get(i).getType().getText(), "name", "uuid");
				if (StringUtils.isBlank(identifierUUid)) {
					identifierUUid = visitType.saveVisitType(patient.getIdentifier().get(i).getType().getText(), code,
					    "patientidentifiertype");
				}
				System.err.println("Text::" + patient.getIdentifier().get(i).getType().getText());
				patient.getIdentifier().get(i).getType().getCoding().get(0).setCode(identifierUUid);
				patient.getIdentifier().get(i).getType().setText(patient.getIdentifier().get(i).getType().getText());
				patient.getIdentifier().get(i).setSystem("");
				
				//				if (patient.getIdentifier().get(i).getType().getText().equals("MPI")) {
				//					
				//					removeIndex = i;
				//				}
				
				Extension ex = patient.getIdentifier().get(i).getExtensionFirstRep();
				
				Reference providerReference = (Reference) ex.getValue();
				
				if (StringUtils.isBlank(locationUuid)) {
					String location = providerReference.getReference().split("/")[1];
					String locationName = providerReference.getDisplay();
					
					String locationuuid = commonOperationService.findResourceUuidByName("location", locationName, "name",
					    "uuid");
					if (!StringUtils.isBlank(locationuuid)) {
						locationUuid = locationuuid;
					} else {
						importLocationService.importLocation(location);
					}
					
				} else {
					providerReference.setReference("Location/" + locationUuid);
				}
				
				i++;
			}
			//			if (removeIndex != -1) {
			//				System.out.println("Removing MPI ID for the patient :"+patient.getIdentifier().get(removeIndex).getValue());
			//				patient.getIdentifier().remove(removeIndex);
			//			}
			
			patient.setTelecom(null);
			Integer patientLocalId = commonOperationService.findResourceIdByUuid(table, resource.getIdElement().getIdPart(),
			    primaryKey);
			System.err.println("patientLocalId::::" + patientLocalId);
			
			System.err.println("DDD>>>>>>>>"
			        + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient));
			if (patient.getName() != null) {
				if (patientLocalId == null || patientLocalId == 0) {
					MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext().create().resource(patient).execute();
					System.err.println("output>>>>>>>" + res);
					patientLocalId = commonOperationService.findResourceIdByUuid(table, res.getId().getIdPart(), primaryKey);
					commonOperationService.updateResource(table, patientLocalId, resource.getIdElement().getIdPart(),
					    primaryKey);
					
				} else {
					
					//					firFhirConfig.getLocalOpenMRSFhirContext().update().resource(patient).execute();
					
					System.out.println("DO nothing-.............");
				}
			}
			
		}
		
		return transactionBundle;
	}
	
}
