package org.openmrs.module.ihmodule.api.impl;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.api.BundleService;
import org.openmrs.module.ihmodule.utils.DeploymentConfProperties;
import org.openmrs.module.ihmodule.utils.HttpResponse;
import org.openmrs.module.ihmodule.utils.HttpService;
import org.openmrs.module.ihmodule.utils.ReqParam;

public class BundleServiceImpl extends BaseOpenmrsService implements BundleService {
	
	DeploymentConfProperties deployConf = Context.getRegisteredComponent("ihmodule.DeploymentConfProperties",
	    DeploymentConfProperties.class);
	
	@Override
	public HttpResponse getBundle(String resourceType, Map<String, String> reqParam) throws Exception {
		String param = ReqParam.toQueryParam(reqParam);
		
		System.out.println(param);
		
		HttpResponse response = new HttpService().get(deployConf.HEALTH_RECORD_EXCHANGE_BASE_URL + "/shr/bundle/"
		        + resourceType, param);
		
		return response;
	}
	
}
