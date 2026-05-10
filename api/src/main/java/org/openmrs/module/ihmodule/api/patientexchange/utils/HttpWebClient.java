package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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
	
	private static final RestTemplate REST_TEMPLATE = createRestTemplate();
	
	private static RestTemplate createRestTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(60_000);
		requestFactory.setReadTimeout(120_000);
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
			ResponseEntity<String> response = REST_TEMPLATE.exchange(uri, HttpMethod.GET, entity, String.class);
			return response.getBody();
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
			return REST_TEMPLATE.exchange(url, HttpMethod.GET, entity, String.class).getBody();
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
			return REST_TEMPLATE.exchange(url, HttpMethod.POST, entity, String.class).getBody();
		}
		catch (HttpStatusCodeException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}
	
	public static FhirResponse postWithBasicAuth(String baseURL, String APIURL, String username, String password,
	        String payload) {
		String url = concatBaseAndPath(baseURL, APIURL);
		System.err.println(url + "-" + username + "-" + password + "-" + payload);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(payload, headers);
		
		FhirResponse response = new FhirResponse();
		try {
			ResponseEntity<String> entityResponse = REST_TEMPLATE.exchange(url, HttpMethod.POST, entity, String.class);
			response.setResponse(entityResponse.getBody());
			response.setStatusCode(String.valueOf(entityResponse.getStatusCode().value()));
			response.setMessage(null);
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
			response.setMessage(e.getMessage());
			response.setStatusCode("500");
			response.setResponse(null);
		}
		return response;
	}
	
	public static String postWithBasicAuthV2(String baseURL, String APIURL, String username, String password,
	        String paylaod) throws UnsupportedEncodingException {
		String url = concatBaseAndPath(baseURL, APIURL);
		System.err.println(url + "-" + username + "-" + password + "-" + paylaod);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBasicAuth(username, password, StandardCharsets.UTF_8);
		HttpEntity<String> entity = new HttpEntity<>(paylaod, headers);
		return REST_TEMPLATE.exchange(url, HttpMethod.POST, entity, String.class).getBody();
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
