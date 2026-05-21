package org.openmrs.module.ihmodule.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HttpService {
	
	public static void doTrustToCertificates() throws Exception {
		Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			
			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
				return;
			}
			
			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
				return;
			}
		} };
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier hv = new HostnameVerifier() {
			
			@Override
			public boolean verify(String urlHostName, SSLSession session) {
				if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
					System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '"
					        + session.getPeerHost() + "'.");
				}
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	
	private HttpResponse readResponse(HttpURLConnection con, String queryParams) throws IOException {

		try {
			if (!StringUtils.isBlank(queryParams)) {
				try (OutputStream os = con.getOutputStream()) {
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
					writer.write(queryParams);
					writer.flush();
					writer.close();
				}
			}

			int statusCode = con.getResponseCode();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			con.disconnect();

			HttpResponse res = new HttpResponse();
			res.setStatusCode(statusCode);
			res.setResponse(String.valueOf(content));
			return res;
		} catch (Exception e) {

			int statusCode = con.getResponseCode();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}

			in.close();
			con.disconnect();

			HttpResponse res = new HttpResponse();
			res.setStatusCode(statusCode);
			res.setResponse(String.valueOf(content));
			if (res.getResponse() == null || res.getResponse().isEmpty()) {
				res.setResponse(e.getMessage());
			}
			return res;
		}
	}
	
	private String readResponse(HttpURLConnection con, String queryParams, String bodyParams) throws IOException {

		try {
			if (!StringUtils.isBlank(queryParams)) {
				try (OutputStream os = con.getOutputStream()) {
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
					writer.write(queryParams);
					writer.flush();
					writer.close();
				}
			}

			if (!StringUtils.isBlank(bodyParams)) {
				try (OutputStream os = con.getOutputStream()) {
					byte[] input = bodyParams.getBytes("utf-8");
					os.write(input, 0, input.length);
				}
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer content = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			con.disconnect();

			return String.valueOf(content);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public HttpResponse get(String api, String queryParams) throws Exception {
		doTrustToCertificates();
		URL url;
		if (queryParams != null || !queryParams.isEmpty()) {
			url = new URL(api + "?" + queryParams);
		} else {
			url = new URL(api);
		}
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setUseCaches(false);
		con.setRequestMethod("GET");
		
		con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
		con.setRequestProperty("Accept", "application/json");
		// con.setRequestProperty("Authorization", token);
		con.setDoOutput(true);
		return readResponse(con, "");
	}
	
	public JsonObject getPatient(String api, String queryParams, String username, String password) throws Exception {
		doTrustToCertificates();
		URL url = new URL(api);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		String authString = username + ":" + password;
		String encoded = authString.matches(".+:.+") ? new String(Base64.encodeBase64(authString.getBytes())) : authString;
		System.err.println("encoded:::::::::::::::::::;" + encoded);
		con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Authorization", "Basic " + encoded);
		con.setDoOutput(true);
		return new JsonParser().parse(readResponse(con, queryParams, "")).getAsJsonObject();
	}
	
	public String getPatientData(String api, String queryParams, String username, String password) throws Exception {
		doTrustToCertificates();
		URL url = new URL(api);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		String authString = username + ":" + password;
		String encoded = authString.matches(".+:.+") ? new String(Base64.encodeBase64(authString.getBytes())) : authString;
		System.err.println("encoded:::::::::::::::::::;" + encoded);
		con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Authorization", "Basic " + encoded);
		con.setDoOutput(true);
		return readResponse(con, queryParams, "");
	}
	
	public JsonObject post(String api, String queryParams, String bodyParams, String token) throws Exception {
		doTrustToCertificates();
		URL url = new URL(api);
		
		try {
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Authorization", "Basic ZG9jdG9yMTpEb2N0b3JAMTIz");
			con.setDoOutput(true);
			return new JsonParser().parse(readResponse(con, queryParams, bodyParams)).getAsJsonObject();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		// return "OKkkk";
	}
	
	public String callAPIForService(String api, String queryParams, String bodyParams, String token) throws Exception {
		doTrustToCertificates();
		URL url = new URL(api);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		
		// con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Authorization", token);
		con.setDoOutput(true);
		return readResponse(con, queryParams, bodyParams);
	}
	
	public JSONObject doPost(String api, String payload, String token) throws Exception {
		doTrustToCertificates();

		URL url = new URL(
				"https://hris.mohfw.gov.bd/api/1.0/sso/signin?email=vaxApi.OpenSRP@gmail.com&password=Mpower1@3");

		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");

		con.setRequestProperty("X-Auth-Token", "39fa74129c4d8e125c0b531a0e83e9253b4cada61ffe094ea9e9464b2f98ac8f");
		con.setRequestProperty("client-id", "190820");
		// con.setRequestProperty("Authorization", token);
		con.setDoOutput(true);

		try (OutputStream os = con.getOutputStream()) {
			byte[] input = payload.getBytes("utf-8");

			os.write(input, 0, input.length);
		}

		int code = con.getResponseCode();
		System.out.println("code" + code);

		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
			StringBuilder response = new StringBuilder();
			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}

			JSONObject result = new JSONObject(response.toString());
			return result;
		}

	}
}
