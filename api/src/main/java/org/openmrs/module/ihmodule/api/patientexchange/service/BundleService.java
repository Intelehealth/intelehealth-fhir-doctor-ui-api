package org.openmrs.module.ihmodule.api.patientexchange.service;

import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.param.ReuestParam;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BundleService {
	
	@Autowired
	private FhirConfig firFhirConfig;
	
	FhirContext fhirContext = FhirContextHolder.R4;
	
	public String sendBundle(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			Resource resource = (Resource) bundleEntry.getResource();
			
			Bundle.BundleEntryComponent component = transactionBundle.addEntry();
			component.setResource(resource);
			component.getRequest().setUrl(resource.fhirType() + "/" + resource.getIdElement().getIdPart())
			        .setMethod(Bundle.HTTPVerb.PUT);
			
		}
		
		System.err.println("DDD>>>>>>>>"
		        + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle));
		
		firFhirConfig.getOpenCRFhirContext().transaction().withBundle(transactionBundle).execute();
		return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(transactionBundle);
	}
	
	public Bundle convertToBundle(String bundleString) {
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, bundleString);
		return theBundle;
	}
	
	public String search(String resourecType, Map<String, String> reqParam) {
		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
		        .byUrl(resourecType + "?" + ReuestParam.toQueryParam(reqParam)).returnBundle(Bundle.class).execute();
		
		System.err.println("DDD>>>>>>>>" + fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(results));
		return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(results);
	}
	
}
