package org.openmrs.module.ihmodule.api.patientexchange.search;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.dto.Names;
import org.openmrs.module.ihmodule.api.patientexchange.dto.PatientAddress;
import org.openmrs.module.ihmodule.api.patientexchange.dto.SearchPateintDTO;
import org.openmrs.module.ihmodule.api.patientexchange.param.ReuestParam;
import org.json.JSONException;
import org.springframework.stereotype.Service;

@Service
public class PatientSearchService {
	
	private FhirConfig firFhirConfig = Context.getRegisteredComponent("fhirConfig", FhirConfig.class);
	
	public List<SearchPateintDTO> searchPatient(PatientSearchParam param) throws ParseException,
	        UnsupportedEncodingException, JSONException {
		
		Map<String, String> query = buildPatientSearchMap(param);
		String searchParamString = ReuestParam.toQueryParam(query);
		
		Bundle results = firFhirConfig.getOpenCRFhirContext().search().byUrl("Patient?" + searchParamString)
		        .returnBundle(Bundle.class).execute();
		
		return generatePatient(results);
	}
	
	/**
	 * Maps JSON/body fields to FHIR Patient search parameters (including {@code telecom} like
	 * legacy MCI).
	 */
	static Map<String, String> buildPatientSearchMap(PatientSearchParam param) {
		if (param == null) {
			throw new IllegalArgumentException("PatientSearchParam is required");
		}
		Map<String, String> m = new LinkedHashMap<>();
		if (param.getIdentifiers() != null) {
			m.put("identifier", param.getIdentifiers());
		}
		if (param.getGender() != null) {
			m.put("gender", param.getGender());
		}
		if (param.getBirthdate() != null) {
			m.put("birthdate", param.getBirthdate());
		}
		if (param.getFamily() != null) {
			m.put("family", param.getFamily());
		}
		if (param.getGiven() != null) {
			m.put("given", param.getGiven());
		}
		String telecom = param.getTelecom() != null ? param.getTelecom() : param.getPhone();
		if (telecom != null) {
			m.put("telecom", telecom);
		}
		if (param.getId() != null) {
			m.put("_id", param.getId());
		}
		if (m.isEmpty()) {
			throw new IllegalArgumentException("At least one Patient search criterion is required");
		}
		return m;
	}
	
	private List<SearchPateintDTO> generatePatient(Bundle originalTasksBundle) {
		List<SearchPateintDTO> patients = new ArrayList<SearchPateintDTO>();
		
		for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
			SearchPateintDTO patient = new SearchPateintDTO();
			
			Patient patientResource = (Patient) bundleEntry.getResource();
			
			List<HumanName> names = patientResource.getName();
			List<Names> theNames = generateNames(names);
			
			List<Identifier> identifiers = patientResource.getIdentifier();
			List<org.openmrs.module.ihmodule.api.patientexchange.dto.Identifier> theIdentifiers = generateIdentifier(identifiers);
			patient.setIdentifiers(theIdentifiers);
			
			patient.setBirthdate(patientResource.getBirthDate().toInstant());
			patient.setNames(theNames);
			patient.setGender(patientResource.getGender().name());
			List<Address> addresses = patientResource.getAddress();
			List<PatientAddress> thePatientAddresses = generateAddress(addresses);
			
			patient.setAddress(thePatientAddresses);
			
			patients.add(patient);
			
		}
		
		return patients;
	}
	
	private List<org.openmrs.module.ihmodule.api.patientexchange.dto.Identifier> generateIdentifier(
	        List<Identifier> identifiers) {
		List<org.openmrs.module.ihmodule.api.patientexchange.dto.Identifier> theIdentifiers = new ArrayList<org.openmrs.module.ihmodule.api.patientexchange.dto.Identifier>();
		
		for (Identifier identifier : identifiers) {
			org.openmrs.module.ihmodule.api.patientexchange.dto.Identifier theIdentifier = new org.openmrs.module.ihmodule.api.patientexchange.dto.Identifier();
			if (identifier.hasType() && identifier.getType().hasText()) {
				theIdentifier.setText(identifier.getType().getText());
			}
			theIdentifier.setValue(identifier.getValue());
			theIdentifiers.add(theIdentifier);
		}
		return theIdentifiers;
		
	}
	
	private List<PatientAddress> generateAddress(List<Address> addresses) {
		List<PatientAddress> thePatientAddresses = new ArrayList<PatientAddress>();
		for (Address address : addresses) {
			PatientAddress thePatientAddress = new PatientAddress();
			StringBuilder txt = new StringBuilder();
			List<Extension> exts = address.getExtension();
			
			for (Extension extension : exts) {
				List<Extension> exten = extension.getExtension();
				for (Extension extension2 : exten) {
					txt.append(extension2.getValue());
				}
				
			}
			thePatientAddress.setAddress(txt.toString());
			thePatientAddress.setCity(address.getCity());
			thePatientAddresses.add(thePatientAddress);
		}
		
		return thePatientAddresses;
		
	}
	
	private List<Names> generateNames(List<HumanName> names) {
		List<Names> theNames = new ArrayList<Names>();
		for (HumanName humanName : names) {
			Names theName = new Names();
			theName.setFamily(humanName.getFamily());
			List<StringType> givens = humanName.getGiven();
			List<String> givenNamea = new ArrayList<String>();
			for (StringType stringType : givens) {
				givenNamea.add(stringType.getValue());
			}
			theName.setGiven(givenNamea);
			theNames.add(theName);
		}
		return theNames;
	}
	
}
