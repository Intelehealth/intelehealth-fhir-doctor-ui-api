package org.openmrs.module.ihmodule.api.patientexchange.scheduler;

import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenMRS scheduled task that delegates to {@link DataSendToFHIR#transferCreatedPatient()}.
 * <p>
 * Configure the schedule from the OpenMRS Admin UI (Manage Scheduler) by registering a task with
 * task class
 * {@code org.openmrs.module.ihmodule.api.patientexchange.scheduler.TransferCreatedPatientTask} and
 * the desired repeat interval / start time.
 */
public class TransferCreatedPatientTask extends AbstractTask {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TransferCreatedPatientTask.class);
	
	private static final String DATA_SEND_TO_FHIR_BEAN = "dataSendToFHIR";
	
	@Override
	public void execute() {
		if (isExecuting) {
			LOGGER.warn("{} is already running; skipping this trigger", getClass().getSimpleName());
			return;
		}
		
		startExecuting();
		try {
			Context.openSession();
			
			DataSendToFHIR dataSendToFHIR = resolveDataSendToFHIR();
			if (dataSendToFHIR == null) {
				LOGGER.error("DataSendToFHIR component is not available; aborting TransferCreatedPatientTask");
				return;
			}
			
			LOGGER.info("TransferCreatedPatientTask started");
			dataSendToFHIR.transferCreatedPatient();
			LOGGER.info("TransferCreatedPatientTask completed");
		}
		catch (Exception ex) {
			LOGGER.error("TransferCreatedPatientTask failed: " + ex.getMessage(), ex);
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
			return Context.getRegisteredComponent(DATA_SEND_TO_FHIR_BEAN, DataSendToFHIR.class);
		}
		catch (Exception ex) {
			LOGGER.warn("Falling back to type-based component lookup for DataSendToFHIR: {}", ex.getMessage());
			java.util.List<DataSendToFHIR> components = Context.getRegisteredComponents(DataSendToFHIR.class);
			return (components == null || components.isEmpty()) ? null : components.get(0);
		}
	}
}
