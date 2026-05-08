package org.openmrs.module.ihmodule.api.patientexchange.service;

import java.io.UnsupportedEncodingException;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class VisitTypeService extends IHConstant {
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	public String saveVisitType(String name, String uuid, String type) throws JSONException, UnsupportedEncodingException {
		JSONObject visitType = new JSONObject();
		visitType.put("name", name);
		visitType.put("uuid", uuid);
		visitType.put("description", name);
		
		FhirResponse res = HttpWebClient.postWithBasicAuth(localOpenmrsOpenhimURL, "/ws/rest/v1/" + type,
		    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1], visitType.toString());
		return uuid;
	}
	
	public void saveResource(String data, String type) throws JSONException, UnsupportedEncodingException {
		
		FhirResponse res = HttpWebClient.postWithBasicAuth(localOpenmrsOpenhimURL, "/ws/rest/v1/" + type,
		    firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1], data.toString());
		
	}
	
}
