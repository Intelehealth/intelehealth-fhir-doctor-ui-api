package org.openmrs.module.ihmodule.api.patientexchange.event;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PatientEventConfiguration {
	
	@Bean(name = "patientEventTaskExecutor")
	public TaskExecutor patientEventTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("ih-patient-fhir-sync-");
		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.initialize();
		return executor;
	}
}
