package org.openmrs.module.ihmodule.api.patientexchange.scheduler;

public class ResourceIsNotValid extends RuntimeException {
	
	private static final long serialVersionUID = 4126010310020452968L;
	
	ResourceIsNotValid() {
		super();
	}
	
	ResourceIsNotValid(String msg) {
		super(msg);
	}
	
	ResourceIsNotValid(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}
