package org.openmrs.module.ihmodule.api;

import java.util.Map;

import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ihmodule.utils.HttpResponse;

public interface BundleService extends OpenmrsService {
	
	HttpResponse getBundle(String resourceType, Map<String, String> reqParam) throws Exception;
	
}
