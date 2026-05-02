package org.openmrs.module.doctor.ui.api.dto;

public class ApiResponse {
	
	private String message;
	
	private int status;
	
	public ApiResponse() {
	}
	
	public ApiResponse(String message, String status) {
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	@Override
	public String toString() {
		return "ApiResponse{" + "message='" + message + '\'' + ", status=" + status + '}';
	}
}
