package org.openmrs.module.ihmodule.utils;

public class HttpResponse {
	
	private int statusCode;
	
	private String response;
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	
	public String getResponse() {
		return response;
	}
	
	public void setResponse(String response) {
		this.response = response;
	}
	
	@Override
	public String toString() {
		return "HttpResponse [statusCode=" + statusCode + ", response=" + response + "]";
	}
	
}
