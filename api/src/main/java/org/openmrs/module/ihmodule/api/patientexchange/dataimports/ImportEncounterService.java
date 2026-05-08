package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterLocationComponent;
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

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;

@Service
public class ImportEncounterService extends IHConstant {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
	    CommonOperationService.class);
	
	private VisitTypeService visitType = Context.getRegisteredComponent("visitTypeService", VisitTypeService.class);
	
	private ImportLocationService importLocationService = Context.getRegisteredComponent("importLocationService",
	    ImportLocationService.class);
	
	public void importEncounter(String patientId, String locationUuid) throws UnsupportedEncodingException, JSONException,
	        JsonProcessingException, ParseException {
		String data = HttpWebClient.get(mciURL, "rest/v1/bundle/Encounter?subject=" + patientId + "&_sort=_lastUpdated",
		    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);
		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
		
		saveToLocal(theBundle, locationUuid);
		
		if (theBundle.hasEntry()) {
			System.out.println("Got Encounter bundle size" + theBundle.getEntry().size());
		}
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle, String locationUuid) throws UnsupportedEncodingException,
	        JSONException, JsonProcessingException, ParseException {
		Bundle transactionBundle = new Bundle();
		String table = "";
		String primaryKey = "";
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			// todo handle multiple indetifier
			Encounter encounter = (Encounter) bundleEntry.getResource();
			// System.err.println("part::"+encounter.getPartOf());
			System.err.println(encounter.getMeta().getTagFirstRep().getCode());
			String encounterOrVisitUuid = "";
			String typeName = encounter.getType().get(0).getCoding().get(0).getDisplay();
			String typeUUid = encounter.getType().get(0).getCoding().get(0).getCode();
			// System.err.println(typeName+":"+typeUUid);
			//
			// System.err.println("typeName:" + typeName);
			if (encounter.getMeta().getTagFirstRep().getCode().equalsIgnoreCase("encounter")) {
				table = QueryTable.ENCOUNTER.value;
				primaryKey = QueryTable.ENCOUNTER_PK.value;
				encounterOrVisitUuid = commonOperationService.findResourceUuidByName(table + "_type", typeName, "name",
				    "uuid");
				if (StringUtils.isBlank(encounterOrVisitUuid)) {
					
					visitType.saveVisitType(typeName, typeUUid, "encountertype");
					encounterOrVisitUuid = typeUUid;
				}
				
			} else if (encounter.getMeta().getTagFirstRep().getCode().equalsIgnoreCase("visit")) {
				
				table = QueryTable.VISIT.value;
				primaryKey = QueryTable.VISIT_PK.value;
				encounterOrVisitUuid = commonOperationService.findResourceUuidByName(table + "_type", typeName, "name",
				    "uuid");
				
				if (StringUtils.isBlank(encounterOrVisitUuid)) {
					
					visitType.saveVisitType(typeName, typeUUid, "visittype");
					encounterOrVisitUuid = typeUUid;
				}
			}
			
			Resource resource = (Resource) bundleEntry.getResource();
			encounter.getType().get(0).getCoding().get(0).setCode(encounterOrVisitUuid);
			System.err.println("Size::" + encounter.getLocation().size());
			int i = 0;
			for (EncounterLocationComponent location : encounter.getLocation()) {
				Reference l = location.getLocation();
				String uuid = l.getReferenceElement().getIdPart();
				System.err.println(l.getDisplay());
				if (StringUtils.isBlank(locationUuid)) {
					String locationuuid = commonOperationService.findResourceUuidByName("location", l.getDisplay(), "name",
					    "uuid");
					if (!StringUtils.isBlank(locationuuid)) {
						locationUuid = locationuuid;
					} else {
						importLocationService.importLocation(uuid);
					}
				} else {
					encounter.getLocation().get(i).getLocation().setReference("Location/" + locationUuid);
				}
				
				i++;
			}
			if (StringUtils.isBlank(locationUuid)) {
				encounter.getLocation();
			}
			
			// partitipant doen not support
			if (encounter.getParticipantFirstRep() != null) {
				encounter.setParticipant(null);
			}
			
			System.err.println("DDD>>>>>>>>"
			        + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(encounter));
			Integer encounterLocalId = commonOperationService.findResourceIdByUuid(table, resource.getIdElement()
			        .getIdPart(), primaryKey);
			System.err.println("IDL::::" + encounterLocalId);
			if (encounterLocalId == null || encounterLocalId == 0) {
				try {
					MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext().create().resource(encounter).execute();
					encounterLocalId = commonOperationService.findResourceIdByUuid(table, res.getId().getIdPart(),
					    primaryKey);
					commonOperationService.updateResource(table, encounterLocalId, resource.getIdElement().getIdPart(),
					    primaryKey);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					firFhirConfig.getLocalOpenMRSFhirContext().update().resource(encounter).execute();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
				System.err.println("Do nothing");
				
			}
		}
		return transactionBundle;
	}
}
