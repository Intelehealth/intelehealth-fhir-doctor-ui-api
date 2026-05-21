package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Derives scheme + host + port (no path) from an OpenHIM-style URL so MDM operations can target
 * {@code base}/fhir/$mdm-match} without using the full patient-create channel URL.
 */
public final class OpenhimUrlAuthorityExtractor {
	
	private static final Logger log = LoggerFactory.getLogger(OpenhimUrlAuthorityExtractor.class);
	
	private OpenhimUrlAuthorityExtractor() {
	}
	
	/**
	 * @param openhimUrl e.g. {@code http://192.168.19.152:6001/openmrs-fhir-mdm/patient-create}
	 * @return e.g. {@code http://192.168.19.152:6001}, or {@code null} if not parseable
	 */
	public static String extractAuthorityBase(String openhimUrl) {
		if (StringUtils.isBlank(openhimUrl) || openhimUrl.contains("${")) {
			return null;
		}
		String trimmed = openhimUrl.trim();
		try {
			URI u = URI.create(trimmed);
			String scheme = u.getScheme();
			String host = u.getHost();
			if (StringUtils.isBlank(scheme) || StringUtils.isBlank(host)) {
				log.warn("Cannot derive MDM base from OpenHIM URL (missing scheme or host): {}", trimmed);
				return null;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(scheme).append("://").append(host);
			int port = u.getPort();
			if (port >= 0) {
				sb.append(':').append(port);
			}
			return sb.toString();
		}
		catch (IllegalArgumentException ex) {
			log.warn("Cannot derive MDM base from OpenHIM URL: {} — {}", trimmed, ex.getMessage());
			return null;
		}
	}
}
