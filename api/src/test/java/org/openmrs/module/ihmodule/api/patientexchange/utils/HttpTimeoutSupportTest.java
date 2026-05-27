package org.openmrs.module.ihmodule.api.patientexchange.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.SocketTimeoutException;

import org.junit.Test;
import org.springframework.web.client.ResourceAccessException;

public class HttpTimeoutSupportTest {
	
	@Test
	public void detectsReadTimeoutWrappedInResourceAccessException() {
		ResourceAccessException ex = new ResourceAccessException("I/O error",
		        new SocketTimeoutException("Read timed out"));
		assertTrue(HttpTimeoutSupport.isTimeout(ex));
		assertEquals(HttpTimeoutSupport.TIMEOUT_STATUS_CODE, HttpTimeoutSupport.failureStatusCode(ex));
		assertEquals("Central FHIR read timeout after 120000ms",
		    HttpTimeoutSupport.formatTimeoutMessage(ex, 60_000, 120_000));
	}
}
