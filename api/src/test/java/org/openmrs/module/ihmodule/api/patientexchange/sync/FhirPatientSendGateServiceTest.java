package org.openmrs.module.ihmodule.api.patientexchange.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.service.LocalPatientMpiUpdateService;

public class FhirPatientSendGateServiceTest {
	
	@Test
	public void handlePatientSend_shouldSkipSyncLogWhenMpiAndSourcePatientIdExist() throws Exception {
		FhirPatientSendGateService gate = new FhirPatientSendGateService();
		RecordingSyncLogService syncLogService = new RecordingSyncLogService();
		setField(gate, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		setField(gate, "patientSyncLogService", syncLogService);
		setField(gate, "localPatientMpiUpdateService", new FixedCentralSyncIdentifiers(true));
		
		FhirResponse central = new FhirResponse();
		central.setStatusCode("200");
		central.setMessage("updated");
		
		FhirResponse response = gate.handlePatientSend("patient-linked", uuid -> central);
		
		assertEquals("200", response.getStatusCode());
		assertEquals(0, syncLogService.createPendingCalls);
		assertEquals(0, syncLogService.completePendingPushCalls);
	}
	
	@Test
	public void handlePatientSend_shouldDeferWithoutSyncLogWhenIdentifiersExistAndFhirSyncDisabled() throws Exception {
		FhirPatientSendGateService gate = new FhirPatientSendGateService();
		RecordingSyncLogService syncLogService = new RecordingSyncLogService();
		setField(gate, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(false));
		setField(gate, "patientSyncLogService", syncLogService);
		setField(gate, "localPatientMpiUpdateService", new FixedCentralSyncIdentifiers(true));
		
		FhirResponse response = gate.handlePatientSend("patient-linked", uuid -> {
			throw new AssertionError("executor must not run when sync disabled");
		});
		
		assertEquals(FhirPatientSendGateService.SKIPPED_STATUS, response.getStatusCode());
		assertEquals(0, syncLogService.createPendingCalls);
		assertEquals(0, syncLogService.markDeferredCalls);
	}
	
	@Test
	public void handlePatientSend_shouldDeferWhenFhirSyncDisabled() throws Exception {
		FhirPatientSendGateService gate = new FhirPatientSendGateService();
		RecordingSyncLogService syncLogService = new RecordingSyncLogService();
		setField(gate, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(false));
		setField(gate, "patientSyncLogService", syncLogService);
		setField(gate, "localPatientMpiUpdateService", new FixedCentralSyncIdentifiers(false));
		
		FhirResponse response = gate.handlePatientSend("patient-1", uuid -> {
			throw new AssertionError("executor must not run when sync disabled");
		});
		
		assertEquals(FhirPatientSendGateService.SKIPPED_STATUS, response.getStatusCode());
		assertEquals(1, syncLogService.createPendingCalls);
		assertEquals(1, syncLogService.markDeferredCalls);
		assertEquals(PatientSyncLogStatus.PENDING, syncLogService.lastPending.getStatusEnum());
		assertNull(syncLogService.lastPending.getNextRetryAt());
	}
	
	@Test
	public void handlePatientSend_shouldCompleteSuccessWhenCentralWriteSucceeds() throws Exception {
		FhirPatientSendGateService gate = new FhirPatientSendGateService();
		RecordingSyncLogService syncLogService = new RecordingSyncLogService();
		setField(gate, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		setField(gate, "patientSyncLogService", syncLogService);
		setField(gate, "localPatientMpiUpdateService", new FixedCentralSyncIdentifiers(false));
		
		FhirResponse central = new FhirResponse();
		central.setStatusCode("200");
		central.setMessage("ok");
		
		FhirResponse response = gate.handlePatientSend("patient-2", uuid -> central);
		
		assertEquals("200", response.getStatusCode());
		assertEquals(1, syncLogService.completePendingPushCalls);
		assertEquals(PatientSyncLogStatus.SUCCESS, syncLogService.lastPending.getStatusEnum());
		assertNull(PatientSyncLogContext.getActiveLogId());
	}
	
	@Test
	public void handlePatientSend_shouldMarkFailedWhenCentralWriteFails() throws Exception {
		FhirPatientSendGateService gate = new FhirPatientSendGateService();
		RecordingSyncLogService syncLogService = new RecordingSyncLogService();
		setField(gate, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		setField(gate, "patientSyncLogService", syncLogService);
		setField(gate, "localPatientMpiUpdateService", new FixedCentralSyncIdentifiers(false));
		
		FhirResponse central = new FhirResponse();
		central.setStatusCode("500");
		central.setMessage("server error");
		
		gate.handlePatientSend("patient-3", uuid -> central);
		
		assertEquals(1, syncLogService.completePendingPushCalls);
		assertEquals(PatientSyncLogStatus.FAILED, syncLogService.lastPending.getStatusEnum());
		assertNotNull(syncLogService.lastPending.getNextRetryAt());
	}
	
	@Test(expected = IllegalStateException.class)
	public void handlePatientSend_shouldMarkFailedAndRethrowWhenExecutorThrows() throws Exception {
		FhirPatientSendGateService gate = new FhirPatientSendGateService();
		RecordingSyncLogService syncLogService = new RecordingSyncLogService();
		setField(gate, "publishedConfigFhirSyncGateService", new FixedFhirSyncGate(true));
		setField(gate, "patientSyncLogService", syncLogService);
		setField(gate, "localPatientMpiUpdateService", new FixedCentralSyncIdentifiers(false));
		
		try {
			gate.handlePatientSend("patient-4", uuid -> {
				throw new IllegalStateException("validation failed");
			});
		}
		finally {
			assertEquals(1, syncLogService.markFailedCalls);
			assertEquals(PatientSyncLogStatus.FAILED, syncLogService.lastPending.getStatusEnum());
			assertNull(PatientSyncLogContext.getActiveLogId());
		}
	}
	
	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
	
	static class FixedFhirSyncGate extends PublishedConfigFhirSyncGateService {
		
		private final boolean enabled;
		
		FixedFhirSyncGate(boolean enabled) {
			this.enabled = enabled;
		}
		
		@Override
		public boolean isFhirSyncEnabled() {
			return enabled;
		}
	}
	
	static class FixedCentralSyncIdentifiers extends LocalPatientMpiUpdateService {
		
		private final boolean linked;
		
		FixedCentralSyncIdentifiers(boolean linked) {
			this.linked = linked;
		}
		
		@Override
		public boolean localPatientHasMpiAndSourcePatientId(String patientUuid) {
			return linked;
		}
	}
	
	static class RecordingSyncLogService implements PatientSyncLogService {
		
		private int createPendingCalls;
		
		private int markDeferredCalls;
		
		private int completePendingPushCalls;
		
		private int markFailedCalls;
		
		private PatientSyncLog lastPending;
		
		@Override
		public PatientSyncLog createPending(String patientUuid) {
			createPendingCalls++;
			lastPending = new PatientSyncLog();
			lastPending.setId(1L);
			lastPending.setPatientUuid(patientUuid);
			lastPending.setAttemptNumber(1);
			lastPending.setStatusEnum(PatientSyncLogStatus.PENDING);
			return lastPending;
		}
		
		@Override
		public void markDeferred(PatientSyncLog row, String reason) {
			markDeferredCalls++;
			row.setFailureReason(reason);
		}
		
		@Override
		public void markSuccess(PatientSyncLog row, FhirResponse response) {
			row.setStatusEnum(PatientSyncLogStatus.SUCCESS);
		}
		
		@Override
		public void markFailed(PatientSyncLog row, FhirResponse response, String failureReason, boolean permanent) {
			markFailedCalls++;
			row.setStatusEnum(permanent ? PatientSyncLogStatus.FAILED_PERMANENT : PatientSyncLogStatus.FAILED);
			row.setFailureReason(failureReason);
			if (!permanent) {
				row.setNextRetryAt(new java.util.Date());
			}
		}
		
		@Override
		public void completePendingPush(PatientSyncLog pending, FhirResponse response) {
			completePendingPushCalls++;
			if (PatientSyncLogService.isSuccessfulCentralWrite(response)) {
				markSuccess(pending, response);
			} else {
				markFailed(pending, response, PatientSyncLogService.formatSyncFailureMessage(response), false);
			}
		}
		
		@Override
		public void tryPushPendingRow(PatientSyncLog pending, PatientSyncPushContract pushContract) {
		}
		
		@Override
		public int runSyncCycle(int limitPerCycle, PatientSyncPushContract pushContract) {
			return 0;
		}
	}
}
