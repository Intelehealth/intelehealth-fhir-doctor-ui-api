package org.openmrs.module.ihmodule.api.patientexchange.search;

import java.net.URI;
import java.text.SimpleDateFormat;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the OpenCR Patient GET URI using the same demographic parameters as legacy MCI
 * {@code PatientMpiServiceImpl.makeQueryParam}: {@code birthdate}, {@code family}, {@code given},
 * {@code gender}, {@code telecom}.
 */
public final class PatientDemographicSearchUriBuilder {
	
	private PatientDemographicSearchUriBuilder() {
	}
	
	/**
	 * Same criteria MCI required before issuing the duplicate search request.
	 */
	public static boolean hasFullDemographicsForPatientSearch(Patient patient) {
		if (patient == null || !patient.hasBirthDate()) {
			return false;
		}
		if (!patient.hasName()) {
			return false;
		}
		HumanName name = patient.getNameFirstRep();
		if (name == null || name.getFamily() == null || name.getFamily().trim().isEmpty()) {
			return false;
		}
		String given = name.getGivenAsSingleString();
		if (given == null || given.trim().isEmpty()) {
			return false;
		}
		if (!patient.hasGender()) {
			return false;
		}
		if (!patient.hasTelecom() || patient.getTelecom().isEmpty()) {
			return false;
		}
		String telecom = patient.getTelecom().get(0).getValue();
		return telecom != null && !telecom.trim().isEmpty();
	}
	
	/**
	 * @param patientCollectionUrl e.g. {@code https://host/fhir/Patient} (no trailing slash)
	 */
	public static URI buildUri(String patientCollectionUrl, Patient patient) {
		return buildUri(patientCollectionUrl, patient, 50);
	}
	
	/**
	 * @param maxResults FHIR {@code _count} so the bundle can contain multiple Patient hits (OpenCR
	 *            may page otherwise).
	 */
	public static URI buildUri(String patientCollectionUrl, Patient patient, int maxResults) {
		if (!hasFullDemographicsForPatientSearch(patient)) {
			throw new IllegalArgumentException("Patient is missing demographics required for MPI duplicate search");
		}
		String dob = new SimpleDateFormat("yyyy-MM-dd").format(patient.getBirthDate());
		HumanName name = patient.getNameFirstRep();
		UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(patientCollectionUrl).queryParam("birthdate", dob)
		        .queryParam("family", name.getFamily().replaceAll(" ", "%20"))
		        .queryParam("given", name.getGivenAsSingleString().replaceAll(" ", "%20"))
		        .queryParam("gender", patient.getGender().toCode())
		        .queryParam("telecom", patient.getTelecom().get(0).getValue().replace("+", "%2B"));
		if (maxResults > 0) {
			b.queryParam("_count", Integer.toString(maxResults));
		}
		return b.build(true).toUri();
	}
}
