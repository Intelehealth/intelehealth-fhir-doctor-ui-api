package org.openmrs.module.ihmodule.api.patientexchange.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class PatientSyncRetryPolicyTest {
	
	@Test
	public void canRetry_stopsAtMaxAttempts() {
		assertTrue(PatientSyncRetryPolicy.canRetry(1));
		assertTrue(PatientSyncRetryPolicy.canRetry(9));
		assertFalse(PatientSyncRetryPolicy.canRetry(10));
	}
	
	@Test
	public void computeNextRetryAt_usesDocumentedBackoff() {
		assertBackoffMinutes(1, 1);
		assertBackoffMinutes(6, 720);
		assertBackoffMinutes(7, 1440);
		assertBackoffMinutes(10, 1440);
	}
	
	private static void assertBackoffMinutes(int failedAttemptNumber, long expectedMinutes) {
		Date nextRetryAt = PatientSyncRetryPolicy.computeNextRetryAt(failedAttemptNumber);
		Calendar cal = Calendar.getInstance();
		cal.setTime(nextRetryAt);
		cal.add(Calendar.MINUTE, (int) -expectedMinutes);
		long deltaMinutes = TimeUnit.MILLISECONDS.toMinutes(nextRetryAt.getTime() - cal.getTimeInMillis());
		assertEquals(expectedMinutes, deltaMinutes);
	}
}
