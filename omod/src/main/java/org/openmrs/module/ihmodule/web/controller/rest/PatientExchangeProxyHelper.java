package org.openmrs.module.ihmodule.web.controller.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Server-side GET/POST to the IH patient data exchange API (avoids browser CORS to localhost:6001).
 * <p>
 * Writes JSON directly to {@link HttpServletResponse} using
 * {@link HttpServletResponse#setContentLength(int)} so Tomcat 7 / Servlet 3.0 works — Spring's
 * {@code ResponseEntity} handling can call {@code setContentLengthLong}, which does not exist
 * before Servlet 3.1.
 */
public final class PatientExchangeProxyHelper {
	
	private static final Log log = LogFactory.getLog(PatientExchangeProxyHelper.class);
	
	private PatientExchangeProxyHelper() {
	}
	
	public static void proxyGet(HttpServletResponse response, String baseUrl, String path) throws IOException {
		try {
			ProxyResult r = exchange(baseUrl, "GET", path, null);
			writeJson(response, r.statusCode, r.body);
		}
		catch (Exception e) {
			log.error(
			    "Patient exchange proxy GET failed — baseUrl=" + safeUrlLog(baseUrl) + " path=" + path + ": "
			            + e.getMessage(), e);
			String msg = escapeJson(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			writeJson(response, HttpURLConnection.HTTP_BAD_GATEWAY, "{\"error\":\"" + msg + "\"}");
		}
	}
	
	public static void proxyPostJson(HttpServletResponse response, String baseUrl, String path, String jsonBody)
	        throws IOException {
		try {
			ProxyResult r = exchange(baseUrl, "POST", path, jsonBody);
			writeJson(response, r.statusCode, r.body);
		}
		catch (Exception e) {
			log.error(
			    "Patient exchange proxy POST failed — baseUrl=" + safeUrlLog(baseUrl) + " path=" + path + ": "
			            + e.getMessage(), e);
			String msg = escapeJson(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			writeJson(response, HttpURLConnection.HTTP_BAD_GATEWAY, "{\"error\":\"" + msg + "\"}");
		}
	}
	
	/**
	 * Proxies multipart upload ({@code file} field, optional {@code locationUuid}) to upstream;
	 * upstream responds with JSON.
	 */
	public static void proxyPostMultipartFile(HttpServletResponse response, String baseUrl, String path, byte[] fileBytes,
	        String originalFilename, String locationUuid) throws IOException {
		try {
			ProxyResult r = exchangeMultipartPost(baseUrl, path, fileBytes, originalFilename, locationUuid);
			writeJson(response, r.statusCode, r.body);
		}
		catch (Exception e) {
			log.error("Patient exchange proxy multipart POST failed — baseUrl=" + safeUrlLog(baseUrl) + " path=" + path
			        + ": " + e.getMessage(), e);
			String msg = escapeJson(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			writeJson(response, HttpURLConnection.HTTP_BAD_GATEWAY, "{\"error\":\"" + msg + "\"}");
		}
	}
	
	/**
	 * Proxies GET and forwards JSON body plus selected upstream headers (export download + counts).
	 */
	public static void proxyGetForwardHeaders(HttpServletResponse response, String baseUrl, String pathAndQuery)
	        throws IOException {
		try {
			ProxiedBinaryBody r = exchangeGetBinary(baseUrl, pathAndQuery);
			response.resetBuffer();
			response.setStatus(r.statusCode);
			if (r.contentType != null && !r.contentType.isEmpty()) {
				response.setContentType(r.contentType);
			} else {
				response.setContentType("application/json;charset=UTF-8");
			}
			if (r.contentDisposition != null && !r.contentDisposition.isEmpty()) {
				response.setHeader("Content-Disposition", r.contentDisposition);
			}
			if (r.xTotalPatients != null && !r.xTotalPatients.isEmpty()) {
				response.setHeader("X-Total-Patients", r.xTotalPatients);
			}
			if (r.xExportedPatients != null && !r.xExportedPatients.isEmpty()) {
				response.setHeader("X-Exported-Patients", r.xExportedPatients);
			}
			if (r.xValidationFailedPatients != null && !r.xValidationFailedPatients.isEmpty()) {
				response.setHeader("X-Validation-Failed-Patients", r.xValidationFailedPatients);
			}
			response.setCharacterEncoding("UTF-8");
			byte[] body = r.body != null ? r.body : new byte[0];
			response.setContentLength(body.length);
			OutputStream os = response.getOutputStream();
			os.write(body);
			os.flush();
		}
		catch (Exception e) {
			log.error("Patient exchange proxy GET (passthrough) failed — baseUrl=" + safeUrlLog(baseUrl) + " path="
			        + pathAndQuery + ": " + e.getMessage(), e);
			String msg = escapeJson(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			writeJson(response, HttpURLConnection.HTTP_BAD_GATEWAY, "{\"error\":\"" + msg + "\"}");
		}
	}
	
	private static void writeJson(HttpServletResponse response, int statusCode, String body) throws IOException {
		String safeBody = body != null ? body : "";
		byte[] utf8 = safeBody.getBytes(StandardCharsets.UTF_8);
		try {
			response.resetBuffer();
			response.setStatus(statusCode);
			response.setContentType("application/json;charset=UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentLength(utf8.length);
			OutputStream os = response.getOutputStream();
			os.write(utf8);
			os.flush();
		}
		catch (IOException ioe) {
			log.error("Failed writing proxy JSON response to client (status=" + statusCode + "): " + ioe.getMessage(), ioe);
			throw ioe;
		}
	}
	
	private static final int LONG_READ_TIMEOUT_MS = 300000;
	
	private static ProxyResult exchange(String baseUrl, String method, String path, String jsonBody) throws Exception {
		HttpURLConnection conn = null;
		try {
			String normalized = baseUrl != null ? baseUrl.replaceAll("/$", "") : "";
			URL url = new URL(normalized + path);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(method);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(120000);
			conn.setRequestProperty("Accept", "application/json");
			if ("POST".equals(method)) {
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				byte[] bytes = jsonBody != null ? jsonBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
				conn.getOutputStream().write(bytes);
				conn.getOutputStream().flush();
			}
			int code = conn.getResponseCode();
			InputStream stream = code >= HttpURLConnection.HTTP_BAD_REQUEST ? conn.getErrorStream() : conn.getInputStream();
			String body = stream != null ? readAll(stream) : "";
			if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
				log.warn("Patient exchange upstream returned HTTP " + code + " for " + method + " " + normalized + path
				        + " — body snippet: " + truncateForLog(body, 800));
			} else if (log.isDebugEnabled()) {
				log.debug("Patient exchange upstream OK " + method + " " + normalized + path + " -> HTTP " + code);
			}
			return new ProxyResult(code, body);
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	
	private static ProxyResult exchangeMultipartPost(String baseUrl, String path, byte[] fileBytes, String originalFilename,
	        String locationUuid) throws Exception {
		HttpURLConnection conn = null;
		try {
			String normalized = baseUrl != null ? baseUrl.replaceAll("/$", "") : "";
			URL url = new URL(normalized + path);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(LONG_READ_TIMEOUT_MS);
			conn.setDoOutput(true);
			conn.setRequestProperty("Accept", "application/json");
			String boundary = "----IHImportBoundary" + System.currentTimeMillis();
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			String safeName = sanitizeMultipartFilename(originalFilename);
			OutputStream out = conn.getOutputStream();
			String ln = "\r\n";
			out.write(("--" + boundary + ln).getBytes(StandardCharsets.UTF_8));
			if (StringUtils.isNotBlank(locationUuid)) {
				String v = locationUuid.trim();
				out.write(("Content-Disposition: form-data; name=\"locationUuid\"" + ln + ln)
				        .getBytes(StandardCharsets.UTF_8));
				out.write(v.getBytes(StandardCharsets.UTF_8));
				out.write(ln.getBytes(StandardCharsets.UTF_8));
				out.write(("--" + boundary + ln).getBytes(StandardCharsets.UTF_8));
			}
			out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"" + ln)
			        .getBytes(StandardCharsets.UTF_8));
			out.write(("Content-Type: application/octet-stream" + ln + ln).getBytes(StandardCharsets.UTF_8));
			if (fileBytes != null && fileBytes.length > 0) {
				out.write(fileBytes);
			}
			out.write((ln + "--" + boundary + "--" + ln).getBytes(StandardCharsets.UTF_8));
			out.flush();
			int code = conn.getResponseCode();
			InputStream stream = code >= HttpURLConnection.HTTP_BAD_REQUEST ? conn.getErrorStream() : conn.getInputStream();
			String body = stream != null ? readAll(stream) : "";
			if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
				log.warn("Patient exchange upstream returned HTTP " + code + " for multipart POST " + normalized + path
				        + " — body snippet: " + truncateForLog(body, 800));
			} else if (log.isDebugEnabled()) {
				log.debug("Patient exchange upstream OK multipart POST " + normalized + path + " -> HTTP " + code);
			}
			return new ProxyResult(code, body);
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	
	private static ProxiedBinaryBody exchangeGetBinary(String baseUrl, String pathAndQuery) throws Exception {
		HttpURLConnection conn = null;
		try {
			String normalized = baseUrl != null ? baseUrl.replaceAll("/$", "") : "";
			URL url = new URL(normalized + pathAndQuery);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(LONG_READ_TIMEOUT_MS);
			conn.setRequestProperty("Accept", "application/json");
			int code = conn.getResponseCode();
			InputStream stream = code >= HttpURLConnection.HTTP_BAD_REQUEST ? conn.getErrorStream() : conn.getInputStream();
			byte[] raw = stream != null ? readAllBytes(stream) : new byte[0];
			ProxiedBinaryBody b = new ProxiedBinaryBody();
			b.statusCode = code;
			b.body = raw;
			b.contentType = conn.getHeaderField("Content-Type");
			b.contentDisposition = conn.getHeaderField("Content-Disposition");
			b.xTotalPatients = conn.getHeaderField("X-Total-Patients");
			b.xExportedPatients = conn.getHeaderField("X-Exported-Patients");
			b.xValidationFailedPatients = conn.getHeaderField("X-Validation-Failed-Patients");
			if (code >= HttpURLConnection.HTTP_BAD_REQUEST) {
				log.warn("Patient exchange upstream returned HTTP " + code + " for GET " + normalized + pathAndQuery
				        + " — body snippet: " + truncateForLog(new String(raw, StandardCharsets.UTF_8), 800));
			} else if (log.isDebugEnabled()) {
				log.debug("Patient exchange upstream OK GET " + normalized + pathAndQuery + " -> HTTP " + code);
			}
			return b;
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	
	private static String sanitizeMultipartFilename(String name) {
		if (name == null || name.trim().isEmpty()) {
			return "upload.json";
		}
		String n = name.replace('\\', '/');
		int slash = n.lastIndexOf('/');
		if (slash >= 0 && slash < n.length() - 1) {
			n = n.substring(slash + 1);
		}
		n = n.replace("\"", "'");
		return n.length() > 200 ? n.substring(0, 200) : n;
	}
	
	private static byte[] readAllBytes(InputStream is) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = is.read(buf)) != -1) {
			bos.write(buf, 0, n);
		}
		return bos.toByteArray();
	}
	
	private static String readAll(InputStream is) throws java.io.IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = is.read(buf)) != -1) {
			bos.write(buf, 0, n);
		}
		return bos.toString(StandardCharsets.UTF_8.name());
	}
	
	private static String escapeJson(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
	}
	
	private static String safeUrlLog(String baseUrl) {
		if (baseUrl == null || baseUrl.isEmpty()) {
			return "(empty)";
		}
		return baseUrl.replaceAll("([?&])(password|secret|token|auth)=[^&]*", "$1$2=***");
	}
	
	private static String truncateForLog(String s, int max) {
		if (s == null) {
			return "";
		}
		String t = s.replace('\n', ' ').replace('\r', ' ');
		return t.length() <= max ? t : t.substring(0, max) + "...";
	}
	
	private static final class ProxyResult {
		
		final int statusCode;
		
		final String body;
		
		ProxyResult(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body;
		}
	}
	
	private static final class ProxiedBinaryBody {
		
		int statusCode;
		
		byte[] body;
		
		String contentType;
		
		String contentDisposition;
		
		String xTotalPatients;
		
		String xExportedPatients;
		
		String xValidationFailedPatients;
	}
}
