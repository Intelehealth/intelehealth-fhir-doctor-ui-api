package org.openmrs.module.ihmodule.api.patientexchange.domain;

public class FhirResponse {
	
	private String response;
	
	private String statusCode;
	
	private String message;
	
	/**
	 * Central FHIR server's {@code Patient.id} from the real write response (HTTP body or
	 * transaction bundle), used for OpenMRS "Source Patient Id". May differ from MPI / MDM golden
	 * identifier values.
	 */
	private String centralServerPatientLogicalId;
	
	/**
	 * HTTP {@code Location} or {@code Content-Location} from the central write response (may hold
	 * {@code Patient/1001}).
	 */
	private String responseLocation;
	
	public String getResponse() {
		return response;
	}
	
	public void setResponse(String response) {
		this.response = response;
	}
	
	public String getStatusCode() {
		return statusCode;
	}
	
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getCentralServerPatientLogicalId() {
		return centralServerPatientLogicalId;
	}
	
	public void setCentralServerPatientLogicalId(String centralServerPatientLogicalId) {
		this.centralServerPatientLogicalId = centralServerPatientLogicalId;
	}
	
	public String getResponseLocation() {
		return responseLocation;
	}
	
	public void setResponseLocation(String responseLocation) {
		this.responseLocation = responseLocation;
	}
	
	@Override
	public String toString() {
		return "HttpResponse [response=" + response + ", statusCode=" + statusCode + ", message=" + message
		        + ", centralServerPatientLogicalId=" + centralServerPatientLogicalId + ", responseLocation="
		        + responseLocation + "]";
	}
	
}
