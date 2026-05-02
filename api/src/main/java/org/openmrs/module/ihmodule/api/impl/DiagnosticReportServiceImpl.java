package org.openmrs.module.ihmodule.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.DiagnosticReport;
import org.openmrs.module.ihmodule.api.DiagnosticReportService;
import org.openmrs.module.ihmodule.api.dao.DiagnosticReportDao;
import org.openmrs.module.ihmodule.dto.DiagnosticReportDTO;
import org.openmrs.module.ihmodule.utils.DeploymentConfProperties;
import org.openmrs.module.ihmodule.utils.HttpResponse;
import org.openmrs.module.ihmodule.utils.HttpService;
import org.openmrs.module.ihmodule.utils.ReqParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DiagnosticReportServiceImpl extends BaseOpenmrsService implements DiagnosticReportService {
	
	@Autowired
	DeploymentConfProperties deployConf;
	
	DiagnosticReportDao dReportDao;
	
	public void setDao(DiagnosticReportDao dao) {
		this.dReportDao = dao;
	}
	
	@Override
	@Transactional
	public HttpResponse saveDiagnosticReport(Map<String, String> reqParam) throws Exception {

		String param = ReqParam.toQueryParam(reqParam);

		System.out.println(param);

		String patientId = reqParam.getOrDefault("patient", "");

		HttpResponse response = new HttpResponse();

		if (patientId.isEmpty()) {
			response.setStatusCode(400);
			response.setResponse("Invalid patient param");
			return response;
		}

		response = new HttpService().get(deployConf.HEALTH_RECORD_EXCHANGE_BASE_URL + "/diagnostic/get-report", param);

		System.out.println("Status Code: " + response.getStatusCode() + " Message: " + response.getResponse().length());

		if (response.getStatusCode() != 200)
			return response;

		List<DiagnosticReport> reports = convertToDiagnosticReport(response.getResponse());

		System.out.println("Report List From API: " + reports.size());

		List<DiagnosticReportDTO> reportList = dReportDao.findAllSQLByPatientId(patientId);

		System.out.println("Report List From DB: " + reportList.size());

		HashMap<String, DiagnosticReportDTO> reportContainers = listToMap(reportList);

		for (DiagnosticReport report : reports) {
			DiagnosticReportDTO dbReport = reportContainers.getOrDefault(report.getResourceId(), null);
			if (dbReport == null) {
				report.setFileName(report.getPatientId() + "_" + report.getResourceId() + ""
						+ getExtension(report.getContentType()));
				dReportDao.save(report);
			}
		}

		reportList = dReportDao.findAllSQLByPatientId(patientId);

		ArrayList<HashMap> files = new ArrayList<>();

		for (DiagnosticReportDTO report : reportList) {
			HashMap<String, String> file = new HashMap<>();
			file.put("title", report.getTitle());
			file.put("fileName", report.getFileName());
			file.put("contentType", report.getContentType());
			file.put("url", deployConf.DOCTOR_BASE_URL + "/rest/v1/diagnostic-report/download?patientId="
					+ report.getPatientId() + "&resourceId=" + report.getResourceId());
			files.add(file);
		}

		try {
			String json = new ObjectMapper().writeValueAsString(files);
			response.setResponse(json);
			response.setStatusCode(200);
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatusCode(200);
			response.setResponse(new ObjectMapper().writeValueAsString(new ArrayList<>()));
		}

		return response;
	}
	
	private List<DiagnosticReport> convertToDiagnosticReport(String response) {

		ObjectMapper mapper = new ObjectMapper();
		try {
			List<DiagnosticReport> reports = mapper.readValue(response, new TypeReference<List<DiagnosticReport>>() {
			});
			return reports;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}

	}
	
	private String getExtension(String contentType) {
		if (contentType.equals("application/pdf")) {
			return ".pdf";
		}
		
		if (contentType.contains("image")) {
			return "." + contentType.split("/")[1];
		}
		
		if (contentType.contains("video")) {
			return ".mp4";
		}
		
		return ".txt";
	}
	
	@Override
	public DiagnosticReport getReportData(String patientId, String resourceId) throws Exception {
		return dReportDao.findAllByPatientIdAndResourceId(patientId, resourceId);
	}
	
	private HashMap<String, DiagnosticReportDTO> listToMap(List<DiagnosticReportDTO> items) {
		HashMap<String, DiagnosticReportDTO> containers = new HashMap<>();
		for (DiagnosticReportDTO dto : items) {
			containers.put(dto.getResourceId(), dto);
		}
		return containers;
	}
}
