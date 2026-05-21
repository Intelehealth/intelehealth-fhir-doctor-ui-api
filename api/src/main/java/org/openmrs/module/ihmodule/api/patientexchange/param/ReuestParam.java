package org.openmrs.module.ihmodule.api.patientexchange.param;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * Builds FHIR search query strings from request parameters (aligned with MCI {@code ReuestParam}):
 * keys and values are URL-encoded per parameter value.
 */
public class ReuestParam {
	
	public static String toQueryParam(@RequestParam Map<String, String> reqParam) {
		if (reqParam == null || reqParam.isEmpty()) {
			throw new IllegalArgumentException("Search requires at least one query parameter");
		}
		try {
			StringBuilder queryParam = new StringBuilder();
			for (Map.Entry<String, String> entry : reqParam.entrySet()) {
				if (entry.getKey() == null || entry.getValue() == null) {
					continue;
				}
				queryParam.append("&").append(entry.getKey()).append("=")
				        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
			}
			if (queryParam.length() == 0) {
				throw new IllegalArgumentException("Search requires at least one non-null query parameter");
			}
			return queryParam.substring(1);
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
