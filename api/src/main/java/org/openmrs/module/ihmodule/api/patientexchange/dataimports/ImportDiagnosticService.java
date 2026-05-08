package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.openmrs.module.ihmodule.api.patientexchange.service.VisitTypeService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ImportDiagnosticService extends IHConstant {
	
	private IHMarkerService ihMarkerService = Context.getRegisteredComponent("IHMarkerService", IHMarkerService.class);
	
	FhirContext fhirContext = FhirContext.forR4();
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
	    CommonOperationService.class);
	
	private VisitTypeService visitType = Context.getRegisteredComponent("visitTypeService", VisitTypeService.class);
	
	ObjectMapper mapper = new ObjectMapper();
	
	private ImportLocationService importLocationService = Context.getRegisteredComponent("importLocationService",
	    ImportLocationService.class);
	
	public void importDiagnostic(String patientId, String locationUuid) throws JSONException, ParseException, IOException {
		/*IQuery<Bundle> encounter = firFhirConfig.getOpenCRFhirContext()
				.search().forResource(Encounter.class)
				.where(Encounter.SUBJECT.hasAnyOfIds(patientId)).sort()
				.ascending("_lastUpdated").returnBundle(Bundle.class);
		Bundle encounterTasksBundle = encounter.execute();*/
		
		String response = HttpWebClient.get(mciURL, "rest/v1/bundle/DiagnosticReport?subject=" + patientId
		        + "&_sort=_lastUpdated", firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, response);
		
		if (theBundle.hasEntry()) {
			System.out.println("Got diagnostics bundle size" + theBundle.getEntry().size());
		}
		JSONObject ob = new JSONObject(response);
		
		JSONArray entries = ob.getJSONArray("entry");
		/*for (int i = 0; i < entries.length(); i++) {
			JSONObject entry = entries.optJSONObject(i);
			JSONObject resource = entry.getJSONObject("resource");
			if(resource.has("presentedForm")){
				JSONObject presentedForm = resource.getJSONArray("presentedForm").optJSONObject(0);
				if(presentedForm.has("data")){
					saveToLocal(theBundle,locationUuid);
				}
				
			}
		}*/
		saveToLocal(theBundle, locationUuid);
		// System.err.println(resou);
		
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle, String locationUuid) throws JSONException, ParseException,
	        IOException {
		Bundle transactionBundle = new Bundle();
		
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			
			DiagnosticReport r = (DiagnosticReport) bundleEntry.getResource();
			
			if (r.getPresentedForm().size() != 0) {
				System.err.println("Form::" + r.getPresentedForm().get(0).getContentType());
				String data = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(r);
				JSONObject ob = new JSONObject(data);
				JSONObject presentedForms = ob.getJSONArray("presentedForm").getJSONObject(0);
				
				Resource resource = (Resource) bundleEntry.getResource();
				String patient = r.getSubject().getReference().split("/")[1];
				String encounter = r.getEncounter().getReference().split("/")[1];
				
				String title = r.getPresentedForm().get(0).getTitle();
				//System.err.println(presentedForms.get("data"));
				//System.err.println("Forms::"+r.getPresentedForm().get(0).getData());
				String fileNameFormat = r.getPresentedForm().get(0).getContentType().split("/")[1];
				String fileName = System.currentTimeMillis() + "." + fileNameFormat;
				title = title + "_" + fileName;
				JSONObject obs = new JSONObject();
				obs.put("person", patient);
				obs.put("encounter", encounter);
				obs.put("obsDatetime", new Date().toInstant());
				obs.put("value", fileName);
				
				List<Coding> codings = r.getCode().getCoding();
				List<String> codes = new ArrayList<String>();
				for (Coding coding : codings) {
					codes.add("'" + coding.getCode() + "'");
				}
				
				String concetpUUid = commonOperationService.findConceptUuidByMappingCode(String.join(",", codes));
				
				obs.put("concept", concetpUUid);
				byte[] decoded = java.util.Base64.getDecoder().decode(presentedForms.get("data").toString());
				System.err.println(obs);
				
				Integer id = commonOperationService.findResourceIdByUuid("obs", resource.getIdElement().getIdPart(),
				    "obs_id");
				System.err.println("IDL::::" + id);
				
				if (id == null || id == 0) {
					FileOutputStream fos;
					try {
						fos = new FileOutputStream("/opt/multimedia/" + fileName);
						fos.write(decoded);
						fos.flush();
						fos.close();
					}
					catch (FileNotFoundException e) {
						
						e.printStackTrace();
					}
					
					String res = HttpWebClient.postWithBasicAuthV2(localOpenmrsOpenhimURL, "/ws/rest/v1/obs",
					    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1], obs.toString());
					System.err.println(res);
					JSONObject response = new JSONObject(res);
					System.err.println("response.getString:::" + response.getString("uuid"));
					
					id = commonOperationService.findResourceIdByUuid("obs", response.getString("uuid"), "obs_id");
					
					System.err.println("ID:::" + id);
					commonOperationService.updateResource("obs", id, resource.getIdElement().getIdPart(), "obs_id");
					commonOperationService.updateObsComplexValue("obs", id, fileName, title, "obs_id");
					
				} else {
					System.err.println("Do nothing");
				}
				
			}
			//System.err.println(r.getResourceType());
			//System.err.println(r.getPresentedForm().get(0).getData().length);
			//System.err.println(r.getId());
			//System.err.println(r.getPresentedForm().get(0).getContentType());
			
			//Resource resource = (Resource) bundleEntry.getResource();
			//String patient = r.getSubject().getReference().split("/")[1];
			//String encounter = r.getEncounter().getReference().split("/")[1];
			/*System.err.println("DDDccc>>>>>>>>"
					+ fhirContext.newJsonParser().setPrettyPrint(true)
							.encodeResourceToString(r));*/
			
		}
		return transactionBundle;
	}
	
}
