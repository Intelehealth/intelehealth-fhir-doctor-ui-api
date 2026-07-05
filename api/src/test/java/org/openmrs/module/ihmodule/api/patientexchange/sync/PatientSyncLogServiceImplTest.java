package org.openmrs.module.ihmodule.api.patientexchange.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;

public class PatientSyncLogServiceImplTest {
	
	@Test
	public void createPending_shouldAssignAttemptNumber() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		repository.nextAttemptNumberOverride = 3;
		setField(service, "repository", repository);
		setField(service, "ihMarkerService", new NoOpMarkerService());
		
		PatientSyncLog row = service.createPending("patient-1");
		
		assertEquals("patient-1", row.getPatientUuid());
		assertEquals(3, row.getAttemptNumber());
		assertEquals(PatientSyncLogStatus.PENDING, row.getStatusEnum());
	}
	
	@Test
	public void markDeferred_shouldKeepPendingWithoutRetrySchedule() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		setField(service, "repository", repository);
		
		PatientSyncLog row = pendingRow(1);
		service.markDeferred(row, FhirPatientSendGateService.SKIPPED_MESSAGE);
		
		assertEquals(PatientSyncLogStatus.PENDING, row.getStatusEnum());
		assertNull(row.getNextRetryAt());
		assertNull(row.getCompletedAt());
	}
	
	@Test
	public void completePendingPush_shouldScheduleBackoffForHttp500() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		setField(service, "repository", repository);
		
		PatientSyncLog row = pendingRow(1);
		FhirResponse response = new FhirResponse();
		response.setStatusCode("500");
		response.setMessage("server error");
		
		service.completePendingPush(row, response);
		
		assertEquals(PatientSyncLogStatus.FAILED, row.getStatusEnum());
		assertNotNull(row.getNextRetryAt());
		assertEquals(Integer.valueOf(500), row.getHttpStatusCode());
	}
	
	@Test
	public void markFailed_shouldMarkPermanentAfterMaxAttempts() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		setField(service, "repository", repository);
		
		PatientSyncLog row = pendingRow(10);
		service.markFailed(row, null, "Max attempts exhausted", false);
		
		assertEquals(PatientSyncLogStatus.FAILED_PERMANENT, row.getStatusEnum());
		assertNull(row.getNextRetryAt());
	}
	
	@Test
	public void markSuccess_shouldAdvanceMarker() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		RecordingMarkerService markerService = new RecordingMarkerService();
		setField(service, "repository", repository);
		setField(service, "ihMarkerService", markerService);
		
		PatientSyncLog row = pendingRow(1);
		row.setId(5L);
		FhirResponse response = new FhirResponse();
		response.setStatusCode("200");
		
		service.markSuccess(row, response);
		
		assertEquals(PatientSyncLogStatus.SUCCESS, row.getStatusEnum());
		assertNotNull(row.getCompletedAt());
		assertNull(row.getNextRetryAt());
		assertEquals(5, markerService.lastMarkerCursor);
	}
	
	@Test
	public void runSyncCycle_skipsWhenFhirSyncDisabled() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		setField(service, "repository", repository);
		setField(service, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(false));
		
		int processed = service.runSyncCycle(10, new RecordingPatientSyncPush());
		
		assertEquals(0, processed);
		assertEquals(0, repository.findPendingCalls);
	}
	
	@Test
	public void runSyncCycle_replaysOnlyLatestFailedRowPerPatient() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		RecordingPatientSyncPush push = new RecordingPatientSyncPush();
		setField(service, "repository", repository);
		setField(service, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		
		PatientSyncLog olderFailed = failedRow(2);
		olderFailed.setId(39L);
		olderFailed.setPatientUuid("shared-patient");
		PatientSyncLog newerFailed = failedRow(4);
		newerFailed.setId(168L);
		newerFailed.setPatientUuid("shared-patient");
		repository.pendingRows = Collections.emptyList();
		repository.failedRows = java.util.Arrays.asList(olderFailed, newerFailed);
		repository.nextAttemptNumberOverride = 5;
		
		int processed = service.runSyncCycle(10, push);
		
		assertEquals(1, processed);
		assertEquals(1, push.pushCalls);
		assertNull(olderFailed.getNextRetryAt());
		assertNull(newerFailed.getNextRetryAt());
		assertEquals(PatientSyncLogStatus.SUCCESS, findSavedRetryAttempt(repository.savedRows, 5).getStatusEnum());
	}
	
	@Test
	public void runSyncCycle_reusesExistingPendingAttemptInsteadOfDuplicateInsert() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		RecordingPatientSyncPush push = new RecordingPatientSyncPush();
		setField(service, "repository", repository);
		setField(service, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		
		PatientSyncLog failed = failedRow(2);
		failed.setId(39L);
		failed.setPatientUuid("shared-patient");
		PatientSyncLog pendingAttempt = pendingRow(3);
		pendingAttempt.setId(100L);
		pendingAttempt.setPatientUuid("shared-patient");
		repository.pendingRows = Collections.emptyList();
		repository.failedRows = Collections.singletonList(failed);
		repository.latestByPatient.put("shared-patient", pendingAttempt);
		
		int processed = service.runSyncCycle(10, push);
		
		assertEquals(1, processed);
		assertEquals(1, push.pushCalls);
		assertEquals(PatientSyncLogStatus.SUCCESS, pendingAttempt.getStatusEnum());
		assertFalse(repository.savedRows.stream().anyMatch(row -> row.getAttemptNumber() > 3));
	}
	
	@Test
	public void runSyncCycle_replaysDeferredPendingAndFailedRows() throws Exception {
		PatientSyncLogServiceImpl service = new PatientSyncLogServiceImpl();
		TrackingRepository repository = new TrackingRepository();
		RecordingPatientSyncPush push = new RecordingPatientSyncPush();
		setField(service, "repository", repository);
		setField(service, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		
		PatientSyncLog deferred = pendingRow(1);
		deferred.setId(1L);
		PatientSyncLog failed = failedRow(1);
		failed.setId(2L);
		repository.pendingRows = Collections.singletonList(deferred);
		repository.failedRows = Collections.singletonList(failed);
		
		int processed = service.runSyncCycle(10, push);
		
		assertEquals(2, processed);
		assertEquals(2, push.pushCalls);
		assertEquals(PatientSyncLogStatus.SUCCESS, deferred.getStatusEnum());
		assertNull(failed.getNextRetryAt());
		assertEquals(PatientSyncLogStatus.SUCCESS, findSavedRetryAttempt(repository.savedRows).getStatusEnum());
	}
	
	private static PatientSyncLog pendingRow(int attemptNumber) {
		PatientSyncLog row = new PatientSyncLog();
		row.setPatientUuid("patient-uuid");
		row.setAttemptNumber(attemptNumber);
		row.setStatusEnum(PatientSyncLogStatus.PENDING);
		row.setStartedAt(new Date());
		return row;
	}
	
	private static PatientSyncLog failedRow(int attemptNumber) {
		PatientSyncLog row = pendingRow(attemptNumber);
		row.setStatusEnum(PatientSyncLogStatus.FAILED);
		row.setNextRetryAt(new Date(0));
		row.setCompletedAt(new Date());
		return row;
	}
	
	private static PatientSyncLog findSavedRetryAttempt(List<PatientSyncLog> savedRows) {
		return findSavedRetryAttempt(savedRows, 2);
	}
	
	private static PatientSyncLog findSavedRetryAttempt(List<PatientSyncLog> savedRows, int attemptNumber) {
		for (PatientSyncLog row : savedRows) {
			if (row.getAttemptNumber() == attemptNumber) {
				return row;
			}
		}
		throw new AssertionError("retry attempt not saved");
	}
	
	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
	
	private static class NoOpMarkerService implements IHMarkerService {
		
		@Override
		public org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker save(
		        org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker ihMarker) {
			return ihMarker;
		}
		
		@Override
		public org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker findByName(String name) {
			return null;
		}
		
		@Override
		public void updateMarkerByName(String name) {
		}
		
		@Override
		public org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker getOrCreateUnsyncedPatientProgressMarker() {
			return null;
		}
		
		@Override
		public void updateUnsyncedPatientMarkerLastId(int lastUnsyncPatientRowId) {
		}
	}
	
	private static final class RecordingMarkerService extends NoOpMarkerService {
		
		private int lastMarkerCursor;
		
		@Override
		public void updateUnsyncedPatientMarkerLastId(int lastUnsyncPatientRowId) {
			lastMarkerCursor = lastUnsyncPatientRowId;
		}
	}
	
	private static final class FixedFhirSyncGate extends PublishedConfigFhirSyncGateService {
		
		private final boolean enabled;
		
		FixedFhirSyncGate(boolean enabled) {
			this.enabled = enabled;
		}
		
		@Override
		public boolean isFhirSyncEnabled() {
			return enabled;
		}
	}
	
	private static final class RecordingPatientSyncPush implements PatientSyncPushContract {
		
		private int pushCalls;
		
		@Override
		public boolean pushPatientForSyncLog(PatientSyncLog pushRow, String operationLabel) {
			pushCalls++;
			pushRow.setStatusEnum(PatientSyncLogStatus.SUCCESS);
			pushRow.setCompletedAt(new Date());
			pushRow.setNextRetryAt(null);
			return true;
		}
	}
	
	private static final class TrackingRepository extends PatientSyncLogRepository {
		
		private final List<PatientSyncLog> savedRows = new ArrayList<PatientSyncLog>();
		
		private final java.util.Map<String, PatientSyncLog> latestByPatient = new java.util.HashMap<String, PatientSyncLog>();
		
		private List<PatientSyncLog> pendingRows = Collections.emptyList();
		
		private List<PatientSyncLog> failedRows = Collections.emptyList();
		
		private int findPendingCalls;
		
		private int nextAttemptNumberOverride;
		
		@Override
		public void save(PatientSyncLog row) {
			if (row.getId() == null) {
				row.setId((long) (savedRows.size() + 1));
			}
			if (!savedRows.contains(row)) {
				savedRows.add(row);
			}
			PatientSyncLog existing = latestByPatient.get(row.getPatientUuid());
			if (existing == null || isNewerRow(row, existing)) {
				latestByPatient.put(row.getPatientUuid(), row);
			}
		}
		
		@Override
		public void evict(PatientSyncLog row) {
		}
		
		@Override
		public int nextAttemptNumberForPatient(String patientUuid) {
			if (nextAttemptNumberOverride > 0) {
				return nextAttemptNumberOverride;
			}
			int max = 0;
			for (PatientSyncLog row : savedRows) {
				if (patientUuid.equals(row.getPatientUuid())) {
					max = Math.max(max, row.getAttemptNumber());
				}
			}
			for (PatientSyncLog row : latestByPatient.values()) {
				if (patientUuid.equals(row.getPatientUuid()) && !savedRows.contains(row)) {
					max = Math.max(max, row.getAttemptNumber());
				}
			}
			return max + 1;
		}
		
		@Override
		public PatientSyncLog findLatestByPatientUuid(String patientUuid) {
			return latestByPatient.get(patientUuid);
		}
		
		private static boolean isNewerRow(PatientSyncLog candidate, PatientSyncLog current) {
			if (candidate.getAttemptNumber() != current.getAttemptNumber()) {
				return candidate.getAttemptNumber() > current.getAttemptNumber();
			}
			Long candidateId = candidate.getId();
			Long currentId = current.getId();
			if (candidateId == null) {
				return false;
			}
			if (currentId == null) {
				return true;
			}
			return candidateId > currentId;
		}
		
		@Override
		public List<PatientSyncLog> findPendingAwaitingPush(int limit) {
			findPendingCalls++;
			return pendingRows;
		}
		
		@Override
		public List<PatientSyncLog> findFailedDueForRetry(int limit) {
			return failedRows;
		}
		
		@Override
		public List<PatientSyncLog> findFailedPermanentEligibleForRetry(int limit) {
			return Collections.emptyList();
		}
	}
}
