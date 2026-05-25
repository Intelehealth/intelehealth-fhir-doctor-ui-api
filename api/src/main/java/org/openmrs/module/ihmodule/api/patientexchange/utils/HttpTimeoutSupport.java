package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.ResourceAccessException;

/**
 * Classifies outbound HTTP/FHIR timeout failures and builds consistent error text for audit and
 * {@code unsync_patient} replay.
 */
public final class HttpTimeoutSupport {
	
	public static final String TIMEOUT_STATUS_CODE = "TIMEOUT";
	
	private HttpTimeoutSupport() {
	}
	
	public static boolean isTimeout(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException || current instanceof ConnectException
			        || current instanceof TimeoutException) {
				return true;
			}
			String message = current.getMessage();
			if (message != null) {
				String lower = message.toLowerCase();
				if (lower.contains("timed out") || lower.contains("timeout") || lower.contains("read timed out")
				        || lower.contains("connect timed out")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}
	
	public static boolean isConnectTimeout(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof ConnectException) {
				return true;
			}
			String message = current.getMessage();
			if (message != null && message.toLowerCase().contains("connect timed out")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
	
	public static String formatTimeoutMessage(Throwable throwable, int connectTimeoutMs, int readTimeoutMs) {
		if (isConnectTimeout(throwable)) {
			return "Central FHIR connect timeout after " + connectTimeoutMs + "ms";
		}
		if (isTimeout(throwable)) {
			return "Central FHIR read timeout after " + readTimeoutMs + "ms";
		}
		return "Central FHIR request timeout";
	}
	
	public static String formatFailureMessage(Throwable throwable, int connectTimeoutMs, int readTimeoutMs) {
		if (isTimeout(throwable)) {
			return formatTimeoutMessage(throwable, connectTimeoutMs, readTimeoutMs);
		}
		return StringUtils.defaultString(throwable != null ? throwable.getMessage() : null, "Central FHIR request failed");
	}
	
	public static String failureStatusCode(Throwable throwable) {
		return isTimeout(throwable) ? TIMEOUT_STATUS_CODE : "500";
	}
	
	public static Throwable unwrapResourceAccessCause(Throwable throwable) {
		if (throwable instanceof ResourceAccessException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}
