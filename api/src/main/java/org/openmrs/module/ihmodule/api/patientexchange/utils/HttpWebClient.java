package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.openmrs.module.ihmodule.api.patientexchange.config.CentralFhirHttpTimeoutConfigurer;
import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP helpers for outbound calls. Uses {@link RestTemplate} (spring-web) so the module runs on
 * OpenMRS where Spring WebFlux is not on the classpath.
 */
public class HttpWebClient {
	
	private static volatile int connectTimeoutMs = CentralFhirHttpTimeoutConfigurer.DEFAULT_CONNECT_TIMEOUT_MS;
	
	private static volatile int readTimeoutMs = CentralFhirHttpTimeoutConfigurer.DEFAULT_READ_TIMEOUT_MS;
	
	private static volatile RestTemplate REST_TEMPLATE = createRestTemplate();
	
	private static RestTemplate restTemplate() {
		CentralFhirHttpTimeoutConfigurer.ensureDefaultsConfigured();
		return REST_TEMPLATE;
	}
	
	/**
	 * Applies connect/read timeouts to the shared {@link RestTemplate} (called at module startup).
	 */
	public static void configureTimeouts(int connectTimeoutMsValue, int readTimeoutMsValue) {
		connectTimeoutMs = connectTimeoutMsValue;
		readTimeoutMs = readTimeoutMsValue;
		REST_TEMPLATE = createRestTemplate();
	}
	
	public static int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}
	
	public static int getReadTimeoutMs() {
		return readTimeoutMs;
	}
	
	private static RestTemplate createRestTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(connectTimeoutMs);
		requestFactory.setReadTimeout(readTimeoutMs);
		BufferingClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(requestFactory);
		RestTemplate template = new RestTemplate(bufferingFactory);
		for (int i = 0; i < template.getMessageConverters().size(); i++) {
			if (template.getMessageConverters().get(i) instanceof StringHttpMessageConverter) {
				((StringHttpMessageConverter) template.getMessageConverters().get(i))
				        .setDefaultCharset(StandardCharsets.UTF_8);
				break;
			}
		}
		return template;
	}
	
	/**
	 * GET against an absolute {@link URI} (including query string), same pattern as MCI OpenCR
	 * search.
	 */
	public static String searchPatient(String baseURL, URI uri, String username, String password)
	        throws UnsupportedEncodingException {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		try {
			ResponseEntity<String> response = restTemplate().exchange(uri, HttpMethod.GET, entity, String.class);
			return response.getBody();
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}
	
	/**
	 * GET JSON from an absolute URL (no authentication).
	 */
	public static String getJson(String absoluteUrl) {
		if (absoluteUrl == null || absoluteUrl.trim().isEmpty()) {
			throw new IllegalArgumentException("absoluteUrl is required");
		}
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		try {
			return restTemplate().exchange(absoluteUrl.trim(), HttpMethod.GET, entity, String.class).getBody();
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}
	
	public static String get(String baseURL, String APIURL, String username, String password)
	        throws UnsupportedEncodingException {
		String url = concatBaseAndPath(baseURL, APIURL);
		System.err.println(url);
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<Void> entity = new HttpEntity<>(headers);
		try {
			return restTemplate().exchange(url, HttpMethod.GET, entity, String.class).getBody();
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}
	
	public static String post(String baseURL, String APIURL, String username, String password, String paylaod)
	        throws UnsupportedEncodingException {
		String url = concatBaseAndPath(baseURL, APIURL);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(paylaod, headers);
		try {
			return restTemplate().exchange(url, HttpMethod.POST, entity, String.class).getBody();
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}
	
	/**
	 * POST with {@code application/fhir+json} (FHIR operations such as {@code $mdm-match}).
	 */
	public static FhirResponse postWithBasicAuthFhirJson(String baseURL, String APIURL, String username, String password,
	        String payload) {
		String url = concatBaseAndPath(baseURL, APIURL);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/fhir+json;charset=UTF-8"));
		headers.add(HttpHeaders.ACCEPT, "application/fhir+json");
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(payload, headers);
		FhirResponse response = new FhirResponse();
		try {
			ResponseEntity<String> entityResponse = restTemplate().exchange(url, HttpMethod.POST, entity, String.class);
			response.setResponse(entityResponse.getBody());
			response.setStatusCode(String.valueOf(entityResponse.getStatusCode().value()));
			response.setMessage(null);
			URI loc = entityResponse.getHeaders().getLocation();
			if (loc != null) {
				response.setResponseLocation(loc.toString());
			} else {
				String cl = entityResponse.getHeaders().getFirst(HttpHeaders.CONTENT_LOCATION);
				if (cl != null && !cl.trim().isEmpty()) {
					response.setResponseLocation(cl.trim());
				}
			}
		}
		catch (HttpStatusCodeException e) {
			response.setMessage(e.getMessage());
			response.setStatusCode(String.valueOf(e.getStatusCode().value()));
			response.setResponse(e.getResponseBodyAsString());
		}
		catch (Exception e) {
			populateFailureResponse(response, e);
		}
		return response;
	}
	
	public static FhirResponse postWithBasicAuth(String baseURL, String APIURL, String username, String password,
	        String payload) {
		String url = concatBaseAndPath(baseURL, APIURL);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(payload, headers);
		
		FhirResponse response = new FhirResponse();
		try {
			ResponseEntity<String> entityResponse = restTemplate().exchange(url, HttpMethod.POST, entity, String.class);
			response.setResponse(entityResponse.getBody());
			response.setStatusCode(String.valueOf(entityResponse.getStatusCode().value()));
			response.setMessage(null);
			URI loc = entityResponse.getHeaders().getLocation();
			if (loc != null) {
				response.setResponseLocation(loc.toString());
			} else {
				String cl = entityResponse.getHeaders().getFirst(HttpHeaders.CONTENT_LOCATION);
				if (cl != null && !cl.trim().isEmpty()) {
					response.setResponseLocation(cl.trim());
				}
			}
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			response.setMessage(e.getMessage());
			response.setStatusCode(String.valueOf(e.getStatusCode().value()));
			response.setResponse(e.getResponseBodyAsString());
		}
		catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			populateFailureResponse(response, e);
		}
		return response;
	}
	
	/**
	 * PUT with JSON body (e.g. FHIR {@code Patient} update) and basic auth.
	 */
	public static FhirResponse putWithBasicAuth(String baseURL, String APIURL, String username, String password,
	        String payload) {
		String url = concatBaseAndPath(baseURL, APIURL);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(payload, headers);
		
		FhirResponse response = new FhirResponse();
		try {
			ResponseEntity<String> entityResponse = restTemplate().exchange(url, HttpMethod.PUT, entity, String.class);
			response.setResponse(entityResponse.getBody());
			response.setStatusCode(String.valueOf(entityResponse.getStatusCode().value()));
			response.setMessage(null);
			URI loc = entityResponse.getHeaders().getLocation();
			if (loc != null) {
				response.setResponseLocation(loc.toString());
			} else {
				String cl = entityResponse.getHeaders().getFirst(HttpHeaders.CONTENT_LOCATION);
				if (cl != null && !cl.trim().isEmpty()) {
					response.setResponseLocation(cl.trim());
				}
			}
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			response.setMessage(e.getMessage());
			response.setStatusCode(String.valueOf(e.getStatusCode().value()));
			response.setResponse(e.getResponseBodyAsString());
		}
		catch (Exception e) {
			System.err.println("Unexpected error: " + e.getMessage());
			populateFailureResponse(response, e);
		}
		return response;
	}
	
	private static void populateFailureResponse(FhirResponse response, Exception exception) {
		Throwable root = HttpTimeoutSupport.unwrapResourceAccessCause(exception);
		response.setStatusCode(HttpTimeoutSupport.failureStatusCode(root));
		response.setMessage(HttpTimeoutSupport.formatFailureMessage(root, connectTimeoutMs, readTimeoutMs));
		response.setResponse(null);
	}
	
	public static String postWithBasicAuthV2(String baseURL, String APIURL, String username, String password,
	        String paylaod) throws UnsupportedEncodingException {
		String url = concatBaseAndPath(baseURL, APIURL);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(paylaod, headers);
		return restTemplate().exchange(url, HttpMethod.POST, entity, String.class).getBody();
	}
	
	static String concatBaseAndPath(String baseURL, String pathOrRelativeUri) {
		if (pathOrRelativeUri == null || pathOrRelativeUri.isEmpty()) {
			return baseURL;
		}
		if (pathOrRelativeUri.startsWith("http://") || pathOrRelativeUri.startsWith("https://")) {
			return pathOrRelativeUri;
		}
		boolean baseEndsWithSlash = baseURL.endsWith("/");
		boolean pathStartsWithSlash = pathOrRelativeUri.startsWith("/");
		if (baseEndsWithSlash && pathStartsWithSlash) {
			return baseURL + pathOrRelativeUri.substring(1);
		}
		if (!baseEndsWithSlash && !pathStartsWithSlash) {
			return baseURL + "/" + pathOrRelativeUri;
		}
		return baseURL + pathOrRelativeUri;
	}
}
