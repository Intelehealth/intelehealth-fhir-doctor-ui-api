package org.openmrs.module.ihmodule.utils;

import java.util.ResourceBundle;

import org.springframework.stereotype.Service;

@Service
public class DeploymentConfProperties {
	
	private static ResourceBundle resourceForOpenmrs = ResourceBundle.getBundle("deploymentConf");
	
	public final static String APPOINTMENTS_URL = resourceForOpenmrs.getString("appointmentBaseURL");
	
	public final static String HEALTH_RECORD_EXCHANGE_BASE_URL = resourceForOpenmrs.getString("healthRecordExchangeBaseURL");
	
	public final static String DOCTOR_BASE_URL = resourceForOpenmrs.getString("doctorBaseURL");
}
