package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.openmrs.module.ihmodule.api.patientexchange.domain.FhirResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

public class HttpWebClient {

	static ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10000000)).build();

	/**
	 * GET against an absolute {@link URI} (including query string), same pattern as MCI OpenCR search.
	 */
	public static String searchPatient(String baseURL, URI uri, String username, String password)
			throws UnsupportedEncodingException {
		WebClient webClient = WebClient.builder().baseUrl(baseURL)
				//.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password))
				.exchangeStrategies(exchangeStrategies).build();
		try {
			return webClient.get().uri(uri)
					.headers(httpHeaders -> httpHeaders.setBasicAuth(username, password)).retrieve()
					.bodyToMono(String.class).block();
		} catch (WebClientResponseException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}

	public static String get(String baseURL, String APIURL, String username, String password)
			throws UnsupportedEncodingException {
		System.err.println(baseURL + "/" + APIURL);
		WebClient webClient = WebClient.builder().baseUrl(baseURL)
				.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password))
				.exchangeStrategies(exchangeStrategies).build();
		try {
			return webClient.get().uri(APIURL)

					.headers(httpHeaders -> httpHeaders.setBasicAuth(username, password)).retrieve()
					.bodyToMono(String.class).block();
		} catch (WebClientResponseException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}
	}

	public static String post(String baseURL, String APIURL, String username, String password, String paylaod)
			throws UnsupportedEncodingException {

		WebClient webClient = WebClient.builder().baseUrl(baseURL)
				// .defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username,
				// password))
				.exchangeStrategies(exchangeStrategies).build();
		try {
			return webClient.post().uri(APIURL).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body(Mono.just(paylaod), String.class).retrieve().bodyToMono(String.class).block();
		} catch (WebClientResponseException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			throw e;
		}

	}

	public static FhirResponse postWithBasicAuth(String baseURL, String APIURL, String username, String password,
			String payload) {
		System.err.println(baseURL + "" + APIURL + "-" + username + "-" + password + "-" + payload);
		WebClient webClient = WebClient.builder().baseUrl(baseURL)
				.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password))
				.exchangeStrategies(exchangeStrategies).build();

		FhirResponse response = new FhirResponse();

		try {
			
			webClient.post().uri(APIURL).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.body(Mono.just(payload), String.class).retrieve().toEntity(String.class) // Captures both the body
																								// and status code
					.map(entity -> {
						// Populate FhirResponse on success
						response.setResponse(entity.getBody());
						response.setStatusCode(String.valueOf(entity.getStatusCodeValue()));
						response.setMessage(null);
						return response;
					}).block();
			
		} catch (WebClientResponseException e) {
			System.err.println(e);
			System.err.println(e.getStatusCode());
			System.err.println(e.getResponseBodyAsString());
			response.setMessage(e.getMessage());
			response.setStatusCode(e.getStatusCode().toString());
			response.setResponse(e.getResponseBodyAsString());

		} catch (Exception e) {
			// Handle other unexpected errors
			System.err.println("Unexpected error: " + e.getMessage());
			response.setMessage(e.getMessage());
			response.setStatusCode("500"); // Internal Server Error
			response.setResponse(null);
		}
		return response;

	}

	public static String postWithBasicAuthV2(String baseURL, String APIURL, String username, String password,
			String paylaod) throws UnsupportedEncodingException {
		System.err.println(baseURL + "" + APIURL + "-" + username + "-" + password + "-" + paylaod);
		WebClient webClient = WebClient.builder().baseUrl(baseURL)
				.defaultHeaders(httpHeaders -> httpHeaders.setBasicAuth(username, password))
				.exchangeStrategies(exchangeStrategies).build();
		return webClient.post().uri(APIURL).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(Mono.just(paylaod), String.class).retrieve().bodyToMono(String.class).block();

	}

}
