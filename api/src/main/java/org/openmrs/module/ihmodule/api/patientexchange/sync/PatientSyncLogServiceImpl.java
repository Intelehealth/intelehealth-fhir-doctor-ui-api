package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.service.IHMarkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("patientSyncLogService")
public class PatientSyncLogServiceImpl implements PatientSyncLogService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientSyncLogServiceImpl.class);
	
	private static final int MAX_FAILURE_REASON_LENGTH = 512;
	
	@Autowired
	private PatientSyncLogRepository repository;
	
	@Autowired
	@Qualifier("ihmoduleIHMarkerService")
	private IHMarkerService ihMarkerService;
	
	@Autowired
	private PublishedConfigFhirSyncGateService publishedConfigFhirSyncGateService;
	
	@Override
	@Transactional
	public PatientSyncLog createPending(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		Date now = new Date();
		PatientSyncLog row = new PatientSyncLog();
		row.setPatientUuid(patientUuid.trim());
		row.setAttemptNumber(repository.nextAttemptNumberForPatient(patientUuid.trim()));
		row.setStatusEnum(PatientSyncLogStatus.PENDING);
		row.setStartedAt(now);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);
		repository.save(row);
		log.info("Patient sync log created: id={} patientUuid={} attempt={}", row.getId(), row.getPatientUuid(),
		    row.getAttemptNumber());
		return row;
	}
	
	@Override
	@Transactional
	public void markDeferred(PatientSyncLog row, String reason) {
		if (row == null) {
			return;
		}
		row.setStatusEnum(PatientSyncLogStatus.PENDING);
		row.setFailureReason(truncate(reason));
		row.setNextRetryAt(null);
		row.setCompletedAt(null);
		row.setHttpStatusCode(null);
		row.setUpdatedAt(new Date());
		repository.save(row);
		log.info("Patient sync deferred (FHIR sync disabled): logId={} patientUuid={} attempt={}", row.getId(),
		    row.getPatientUuid(), row.getAttemptNumber());
	}
	
	@Override
	@Transactional
	public void markSuccess(PatientSyncLog row, FhirResponse response) {
		if (row == null) {
			return;
		}
		Date now = new Date();
		row.setStatusEnum(PatientSyncLogStatus.SUCCESS);
		row.setCompletedAt(now);
		row.setNextRetryAt(null);
		row.setFailureReason(null);
		row.setHttpStatusCode(parseHttpStatusCode(response));
		row.setUpdatedAt(now);
		repository.save(row);
		if (row.getId() != null) {
			int cursor = row.getId() > Integer.MAX_VALUE ? Integer.MAX_VALUE : row.getId().intValue();
			ihMarkerService.updateUnsyncedPatientMarkerLastId(cursor);
		}
		log.info("Patient sync success: logId={} patientUuid={} attempt={} httpStatus={}", row.getId(),
		    row.getPatientUuid(), row.getAttemptNumber(), row.getHttpStatusCode());
	}
	
	@Override
	@Transactional
	public void markFailed(PatientSyncLog row, FhirResponse response, String failureReason, boolean permanent) {
		if (row == null) {
			return;
		}
		Date now = new Date();
		row.setCompletedAt(now);
		row.setFailureReason(truncate(failureReason));
		row.setHttpStatusCode(parseHttpStatusCode(response));
		row.setUpdatedAt(now);
		if (permanent || !PatientSyncRetryPolicy.canRetry(row.getAttemptNumber())) {
			row.setStatusEnum(PatientSyncLogStatus.FAILED_PERMANENT);
			row.setNextRetryAt(null);
			log.error("Patient sync permanently failed: logId={} patientUuid={} attempt={} reason={}", row.getId(),
			    row.getPatientUuid(), row.getAttemptNumber(), row.getFailureReason());
		} else {
			row.setStatusEnum(PatientSyncLogStatus.FAILED);
			row.setNextRetryAt(PatientSyncRetryPolicy.computeNextRetryAt(row.getAttemptNumber()));
			log.warn("Patient sync failed: logId={} patientUuid={} attempt={} nextRetryAt={} reason={}", row.getId(),
			    row.getPatientUuid(), row.getAttemptNumber(), row.getNextRetryAt(), row.getFailureReason());
		}
		repository.save(row);
	}
	
	@Override
	@Transactional
	public void completePendingPush(PatientSyncLog pending, FhirResponse response) {
		if (PatientSyncLogService.isSuccessfulCentralWrite(response)) {
			markSuccess(pending, response);
			return;
		}
		String reason = PatientSyncLogService.formatSyncFailureMessage(response);
		markFailed(pending, response, reason, false);
	}
	
	@Override
	@Transactional
	public void tryPushPendingRow(PatientSyncLog pending, PatientSyncPushContract pushContract) {
		if (pending == null || pushContract == null) {
			return;
		}
		if (!publishedConfigFhirSyncGateService.isFhirSyncEnabled()) {
			markDeferred(pending, FhirPatientSendGateService.SKIPPED_MESSAGE);
			return;
		}
		try {
			PatientSyncLogContext.bind(pending.getId());
			pushContract.pushPatientForSyncLog(pending, "immediate patient sync");
		}
		catch (Exception ex) {
			markFailed(pending, null, PatientSyncLogService.formatSyncFailureMessage(ex), false);
		}
		finally {
			PatientSyncLogContext.clear();
		}
	}
	
	@Override
	@Transactional
	public int runSyncCycle(int limitPerCycle, PatientSyncPushContract pushContract) {
		if (!publishedConfigFhirSyncGateService.isFhirSyncEnabled()) {
			return 0;
		}
		if (pushContract == null) {
			log.error("patientSyncPush not available; aborting patient sync retry cycle");
			return 0;
		}
		int remaining = Math.max(1, limitPerCycle);
		int processed = 0;
		processed += pushDeferredPendingRows(Math.min(remaining, limitPerCycle), pushContract);
		remaining = Math.max(0, limitPerCycle - processed);
		if (remaining > 0) {
			processed += replayFailedRows(remaining, pushContract);
		}
		return processed;
	}
	
	private int pushDeferredPendingRows(int limit, PatientSyncPushContract pushContract) {
		List<PatientSyncLog> pending = repository.findPendingAwaitingPush(limit);
		int processed = 0;
		for (PatientSyncLog row : pending) {
			if (pushPatientForSyncLog(row, pushContract, "deferred pending push")) {
				processed++;
			}
		}
		return processed;
	}
	
	private int replayFailedRows(int limit, PatientSyncPushContract pushContract) {
		Set<Long> seenIds = new LinkedHashSet<Long>();
		List<PatientSyncLog> due = new ArrayList<PatientSyncLog>();
		for (PatientSyncLog row : repository.findFailedDueForRetry(limit)) {
			if (seenIds.add(row.getId())) {
				due.add(row);
			}
		}
		int remaining = Math.max(0, limit - due.size());
		if (remaining > 0) {
			for (PatientSyncLog row : repository.findFailedPermanentEligibleForRetry(remaining)) {
				if (seenIds.add(row.getId())) {
					due.add(row);
				}
			}
		}
		int processed = 0;
		for (PatientSyncLog failed : due) {
			PatientSyncLog attempt = null;
			try {
				supersedeRetrySource(failed);
				attempt = startRetryAttempt(failed);
				if (pushPatientForSyncLog(attempt, pushContract, "sync retry")) {
					processed++;
				}
			}
			catch (Exception ex) {
				log.error("Patient sync retry failed for log id {}: {}", failed.getId(), ex.getMessage(), ex);
				PatientSyncLog row = attempt != null ? attempt : failed;
				markFailed(row, null, PatientSyncLogService.formatSyncFailureMessage(ex), false);
			}
		}
		return processed;
	}
	
	private boolean pushPatientForSyncLog(PatientSyncLog pushRow, PatientSyncPushContract pushContract, String operationLabel) {
		if (StringUtils.isBlank(pushRow.getPatientUuid())) {
			markFailed(pushRow, null, "Missing patient UUID for " + operationLabel, true);
			return false;
		}
		try {
			PatientSyncLogContext.bind(pushRow.getId());
			return pushContract.pushPatientForSyncLog(pushRow, operationLabel);
		}
		finally {
			PatientSyncLogContext.clear();
		}
	}
	
	private PatientSyncLog startRetryAttempt(PatientSyncLog failed) {
		PatientSyncLog attempt = new PatientSyncLog();
		attempt.setPatientUuid(failed.getPatientUuid());
		attempt.setAttemptNumber(failed.getAttemptNumber() + 1);
		attempt.setStatusEnum(PatientSyncLogStatus.PENDING);
		Date now = new Date();
		attempt.setStartedAt(now);
		attempt.setCreatedAt(now);
		attempt.setUpdatedAt(now);
		repository.save(attempt);
		return attempt;
	}
	
	private void supersedeRetrySource(PatientSyncLog source) {
		source.setNextRetryAt(null);
		if (source.getStatusEnum() == PatientSyncLogStatus.FAILED_PERMANENT) {
			source.setStatusEnum(PatientSyncLogStatus.FAILED);
		}
		source.setUpdatedAt(new Date());
		repository.save(source);
	}
	
	private static Integer parseHttpStatusCode(FhirResponse response) {
		return PatientSyncLogService.parseHttpStatusCode(response);
	}
	
	private static String truncate(String message) {
		if (message == null) {
			return null;
		}
		String trimmed = message.trim();
		if (trimmed.length() <= MAX_FAILURE_REASON_LENGTH) {
			return trimmed;
		}
		return trimmed.substring(0, MAX_FAILURE_REASON_LENGTH);
	}
}
