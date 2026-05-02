package org.openmrs.module.doctor.ui.api.api.impl;

import java.util.Map;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.doctor.ui.api.api.BundleService;
import org.openmrs.module.doctor.ui.api.utils.DeploymentConfProperties;
import org.openmrs.module.doctor.ui.api.utils.HttpResponse;
import org.openmrs.module.doctor.ui.api.utils.HttpService;
import org.openmrs.module.doctor.ui.api.utils.ReqParam;
import org.springframework.beans.factory.annotation.Autowired;

public class BundleServiceImpl extends BaseOpenmrsService implements BundleService {
	
	@Autowired
	DeploymentConfProperties deployConf;
	
	@Override
	public HttpResponse getBundle(String resourceType, Map<String, String> reqParam) throws Exception {
		String param = ReqParam.toQueryParam(reqParam);
		
		System.out.println(param);
		
		HttpResponse response = new HttpService().get(deployConf.HEALTH_RECORD_EXCHANGE_BASE_URL + "/shr/bundle/"
		        + resourceType, param);
		
		return response;
	}
	
}
