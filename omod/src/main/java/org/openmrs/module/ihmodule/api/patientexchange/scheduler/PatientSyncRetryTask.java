package org.openmrs.module.ihmodule.api.patientexchange.scheduler;

import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.sync.PatientSyncLogService;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replays deferred and failed central FHIR patient pushes from {@code patient_sync_log}. Register
 * in OpenMRS Admin → Manage Scheduler with task class
 * {@code org.openmrs.module.ihmodule.api.patientexchange.scheduler.PatientSyncRetryTask} (e.g.
 * every 60 seconds).
 */
public class PatientSyncRetryTask extends AbstractTask {
	
	private static final Logger LOG = LoggerFactory.getLogger(PatientSyncRetryTask.class);
	
	private static final int LIMIT_PER_CYCLE = 20;
	
	@Override
	public void execute() {
		if (isExecuting) {
			LOG.warn("{} is already running; skipping", getClass().getSimpleName());
			return;
		}
		startExecuting();
		try {
			Context.openSession();
			PatientSyncLogService patientSyncLogService = Context.getRegisteredComponent("patientSyncLogService",
			    PatientSyncLogService.class);
			DataSendToFHIR dataSendToFHIR = resolveDataSendToFHIR();
			if (patientSyncLogService == null) {
				LOG.error("patientSyncLogService not available; aborting PatientSyncRetryTask");
				return;
			}
			if (dataSendToFHIR == null) {
				LOG.error("dataSendToFHIR not available; aborting PatientSyncRetryTask");
				return;
			}
			int processed = patientSyncLogService.runSyncCycle(LIMIT_PER_CYCLE, dataSendToFHIR);
			if (processed > 0) {
				LOG.info("PatientSyncRetryTask processed {} deferred or failed patient push(es)", processed);
			}
		}
		catch (Exception ex) {
			LOG.error("PatientSyncRetryTask failed: " + ex.getMessage(), ex);
		}
		finally {
			try {
				Context.closeSession();
			}
			finally {
				stopExecuting();
			}
		}
	}
	
	private DataSendToFHIR resolveDataSendToFHIR() {
		try {
			return Context.getRegisteredComponent("dataSendToFHIR", DataSendToFHIR.class);
		}
		catch (Exception ex) {
			LOG.warn("Falling back to type-based component lookup for DataSendToFHIR: {}", ex.getMessage());
			java.util.List<DataSendToFHIR> components = Context.getRegisteredComponents(DataSendToFHIR.class);
			return (components == null || components.isEmpty()) ? null : components.get(0);
		}
	}
}
