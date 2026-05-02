package org.openmrs.module.ihmodule.api.impl.exp;

public class BeanValidationException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public BeanValidationException(String message) {
		super(message);
	}
}
