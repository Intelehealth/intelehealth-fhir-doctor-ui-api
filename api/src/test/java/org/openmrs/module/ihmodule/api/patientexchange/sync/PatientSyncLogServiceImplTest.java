package org.openmrs.module.ihmodule.api.patientexchange.sync;

import static org.junit.Assert.assertEquals;
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
		repository.nextAttemptNumber = 3;
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
		for (PatientSyncLog row : savedRows) {
			if (row.getAttemptNumber() == 2) {
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
		
		private List<PatientSyncLog> pendingRows = Collections.emptyList();
		
		private List<PatientSyncLog> failedRows = Collections.emptyList();
		
		private int findPendingCalls;
		
		private int nextAttemptNumber = 1;
		
		@Override
		public void save(PatientSyncLog row) {
			if (row.getId() == null) {
				row.setId((long) (savedRows.size() + 1));
			}
			if (!savedRows.contains(row)) {
				savedRows.add(row);
			}
		}
		
		@Override
		public int nextAttemptNumberForPatient(String patientUuid) {
			return nextAttemptNumber;
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
