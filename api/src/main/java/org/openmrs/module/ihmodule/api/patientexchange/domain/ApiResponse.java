package org.openmrs.module.ihmodule.api.patientexchange.domain;

import java.util.Date;

public class ApiResponse {
	
	private String message;
	
	private int responseCode;
	
	private Date responseTime = new Date();
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
	
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
	
	public Date getResponseTime() {
		return responseTime;
	}
	
	public void setResponseTime(Date responseTime) {
		this.responseTime = responseTime;
	}
	
	@Override
	public String toString() {
		return "ApiResponse [message=" + message + ", responseCode=" + responseCode + ", responseTime=" + responseTime + "]";
	}
	
}
