package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.json.JSONException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;

@Service
public class ImportLocationService extends IHConstant {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
	    CommonOperationService.class);
	
	public void importLocation(String locationId) throws JsonProcessingException, UnsupportedEncodingException,
	        JSONException, ParseException {
		String data = HttpWebClient.get(mciURL, "rest/v1/bundle/Location?_id=" + locationId,
		    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);
		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
		
		System.err.println("locationId::" + locationId);
		
		saveToLocal(theBundle);
		
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			Location r = (Location) bundleEntry.getResource();
			
			Resource resource = (Resource) bundleEntry.getResource();
			
			System.err.println();
			Integer id = commonOperationService.findLocationByUuid("location", resource.getIdElement().getIdPart(),
			    r.getName());
			
			System.err.println("IDL::::" + id);
			
			if (r.getName() != null) {
				if (id == null || id == 0) {
					MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext().create().resource(r).execute();
					id = commonOperationService.findLocationByUuid("location", res.getId().getIdPart(), r.getName());
					commonOperationService
					        .updateResource("location", id, resource.getIdElement().getIdPart(), "location_id");
					
				} else {
					firFhirConfig.getLocalOpenMRSFhirContext().update().resource(r).execute();
					
				}
			}
			
		}
		
		return transactionBundle;
	}
	
}
