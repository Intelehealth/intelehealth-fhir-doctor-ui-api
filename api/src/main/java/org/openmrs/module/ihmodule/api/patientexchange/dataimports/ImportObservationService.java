package org.openmrs.module.ihmodule.api.patientexchange.dataimports;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.datatype.QueryTable;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;

@Service
public class ImportObservationService extends IHConstant {
	
	FhirContext fhirContext = FhirContext.forR4();
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	private CommonOperationService commonOperationService = Context.getRegisteredComponent("commonOperationService",
	    CommonOperationService.class);
	
	public void importObservation(String patientId) throws UnsupportedEncodingException {
		
		String data = HttpWebClient.get(mciURL, "rest/v1/bundle/Observation?subject=" + patientId + "&_sort=_lastUpdated",
		    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);
		
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);
		
		saveToLocal(theBundle);
		if (theBundle.hasEntry()) {
			System.out.println("Got Obs bundle size" + theBundle.getEntry().size());
		}
	}
	
	private Bundle saveToLocal(Bundle originalTasksBundle) {
		Bundle transactionBundle = new Bundle();
		String table = QueryTable.OBS.value;
		String primaryKey = QueryTable.OBS_PK.value;
		transactionBundle.setType(Bundle.BundleType.TRANSACTION);
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			
			Observation obs = (Observation) bundleEntry.getResource();
			
			String conceptUuid = "";
			
			List<Coding> codings = obs.getCode().getCoding();
			
			List<String> codes = new ArrayList<String>();
			
			for (Coding coding : codings) {
				
				codes.add("'" + coding.getCode() + "'");
				codes.add("'" + coding.getDisplay() + "'");
			}
			
			conceptUuid = commonOperationService.findConceptUuidByMappingCode(String.join(",", codes));
			obs.getCode().getCoding().get(0).setCode(conceptUuid);
			
			Resource resource = (Resource) bundleEntry.getResource();
			
			Integer obsLocalId = commonOperationService.findResourceIdByUuid(table, resource.getIdElement().getIdPart(),
			    primaryKey);
			
			if (obsLocalId == null || obsLocalId == 0) {
				try {
					MethodOutcome res = firFhirConfig.getLocalOpenMRSFhirContext().create().resource(obs).execute();
					
					obsLocalId = commonOperationService.findResourceIdByUuid(table, res.getId().getIdPart(), primaryKey);
					
					commonOperationService
					        .updateResource(table, obsLocalId, resource.getIdElement().getIdPart(), primaryKey);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				
			} else {
				try {
					firFhirConfig.getLocalOpenMRSFhirContext().update().resource(obs).execute();
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
