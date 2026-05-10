package org.openmrs.module.ihmodule.api.patientexchange.search;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.service.BundleService;
import org.openmrs.module.ihmodule.api.patientexchange.utils.HttpWebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Single entry point for central OpenCR Patient searches (everything that lived in MCI
 * {@code PatientSearchImpl}, bundle generic search, and legacy HTTP demographic search).
 */
@Service
public class CentralPatientSearchService {
	
	@Autowired
	private FhirConfig fhirConfig;
	
	@Autowired
	private BundleService bundleService;
	
	@Value("${intelehealth.fhir.mpi.duplicate.precheck.search.count:50}")
	private int duplicatePrecheckSearchResultCount;
	
	/**
	 * FHIR generic search: {@code Patient?}&lt;encoded query&gt; via HAPI client (same as MCI
	 * {@code PatientSearchImpl}).
	 */
	public String searchPatientJson(Map<String, String> queryParameters) {
		return bundleService.search("Patient", queryParameters);
	}
	
	/**
	 * Legacy HTTP demographic search against {@link FhirConfig#getOpencrOpenhimURL()}
	 * {@code /Patient}, matching old MCI {@code HttpWebClient.searchPatient} +
	 * {@code makeQueryParam} behaviour.
	 */
	public String searchPatientJsonByDemographics(Patient patient) throws UnsupportedEncodingException {
		String[] creds = fhirConfig.getOpenCRCredentials();
		if (creds.length < 2) {
			throw new IllegalStateException("OpenCR credentials not configured");
		}
		String patientBase = fhirConfig.getOpencrOpenhimURL() + "/Patient";
		URI uri = PatientDemographicSearchUriBuilder.buildUri(patientBase, patient, duplicatePrecheckSearchResultCount);
		return HttpWebClient.searchPatient(patientBase, uri, creds[0], creds[1]);
	}
}
