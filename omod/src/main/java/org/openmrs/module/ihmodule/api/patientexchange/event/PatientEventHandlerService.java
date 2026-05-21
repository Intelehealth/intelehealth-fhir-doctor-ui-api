package org.openmrs.module.ihmodule.api.patientexchange.event;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Patient;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.ihmodule.APIfordoctorUIActivator;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.openmrs.module.ihmodule.api.patientexchange.scheduler.DataSendToFHIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service("patientEventHandlerService")
public class PatientEventHandlerService {
	
	private static final Logger log = LoggerFactory.getLogger(PatientEventHandlerService.class);
	
	@Autowired
	private FhirSyncConfiguration configuration;
	
	@Autowired
	private DataSendToFHIR dataSendToFHIR;
	
	@Autowired
	@Qualifier("patientEventTaskExecutor")
	private TaskExecutor taskExecutor;
	
	public void handleAfterSave(Patient patient, PatientEventType eventType) {
		if (patient == null || StringUtils.isBlank(patient.getUuid()) || eventType == null) {
			return;
		}
		if (FhirSyncSuppressionContext.isSuppressed()) {
			log.debug("Skipping patient {} event for uuid={} because FHIR sync suppression is active",
			    eventType.getLogLabel(), patient.getUuid());
			return;
		}
		if (!configuration.shouldProcess(eventType)) {
			log.debug("Patient {} event listener disabled for uuid={}", eventType.getLogLabel(), patient.getUuid());
			return;
		}
		final String patientUuid = patient.getUuid().trim();
		Runnable afterCommitWork = new Runnable() {
			
			@Override
			public void run() {
				taskExecutor.execute(new Runnable() {
					
					@Override
					public void run() {
						processEvent(eventType, patientUuid);
					}
				});
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				
				@Override
				public void afterCommit() {
					afterCommitWork.run();
				}
			});
			return;
		}
		afterCommitWork.run();
	}
	
	private void processEvent(PatientEventType eventType, String patientUuid) {
		if (!configuration.shouldProcess(eventType)) {
			return;
		}
		final DaemonToken daemonToken = APIfordoctorUIActivator.getDaemonToken();
		if (daemonToken == null) {
			log.error("Failed to send patient {} because no OpenMRS daemon token is available", patientUuid);
			return;
		}
		try {
			log.info("Patient {} event detected for UUID: {}", eventType.getLogLabel(), patientUuid);
			log.info("Sending patient to FHIR server...");
			Daemon.runInDaemonThread(new Runnable() {
				
				@Override
				public void run() {
					try {
						FhirSyncSuppressionContext.runSuppressed(new Runnable() {
							
							@Override
							public void run() {
								try {
									FhirResponse result = dataSendToFHIR.send("Patient", patientUuid);
									log.info("Patient {} sync completed for uuid={} with status={}",
									    eventType.getLogLabel(), patientUuid, result != null ? result.getStatusCode() : null);
								}
								catch (Exception ex) {
									throw new RuntimeException(ex);
								}
							}
						});
					}
					catch (Exception ex) {
						log.error("Failed to send patient {}", patientUuid, ex);
					}
				}
			}, daemonToken);
		}
		catch (Exception ex) {
			log.error("Failed to send patient {}", patientUuid, ex);
		}
	}
}
