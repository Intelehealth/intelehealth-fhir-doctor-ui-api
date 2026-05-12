package org.openmrs.module.ihmodule.api.patientmatch.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;

@Component
public class FhirPatientMatchRequestParser {
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	public FuzzyPatientMatchRequest parse(String body) {
		if (StringUtils.isBlank(body)) {
			throw new IllegalArgumentException("FHIR Parameters body is required");
		}
		IBaseResource parsed = fhirContext.newJsonParser().parseResource(body);
		if (parsed instanceof Parameters) {
			return parseParameters((Parameters) parsed);
		}
		if (parsed instanceof Patient) {
			return fromPatient((Patient) parsed, new FuzzyPatientMatchRequest());
		}
		throw new IllegalArgumentException("Request body must be FHIR Parameters or Patient");
	}
	
	private FuzzyPatientMatchRequest parseParameters(Parameters parameters) {
		FuzzyPatientMatchRequest request = new FuzzyPatientMatchRequest();
		Patient patient = null;
		String resourceType = null;
		for (Parameters.ParametersParameterComponent parameter : parameters.getParameter()) {
			if (parameter == null || StringUtils.isBlank(parameter.getName())) {
				continue;
			}
			if ("resource".equals(parameter.getName()) && parameter.getResource() instanceof Patient) {
				patient = (Patient) parameter.getResource();
			}
			if ("count".equals(parameter.getName()) && parameter.getValue() instanceof IntegerType) {
				request.setCount(((IntegerType) parameter.getValue()).getValue());
			}
			if ("offset".equals(parameter.getName()) && parameter.getValue() instanceof IntegerType) {
				request.setOffset(((IntegerType) parameter.getValue()).getValue());
			}
			if ("onlyCertainMatches".equals(parameter.getName()) && parameter.getValue() instanceof BooleanType) {
				request.setOnlyCertainMatches(((BooleanType) parameter.getValue()).booleanValue());
			}
			if ("resourceType".equals(parameter.getName()) && parameter.getValue() instanceof StringType) {
				resourceType = StringUtils.trimToNull(((StringType) parameter.getValue()).getValue());
			}
		}
		if (patient == null) {
			throw new IllegalArgumentException("Parameters.resource Patient is required");
		}
		if (StringUtils.isNotBlank(resourceType) && !"Patient".equals(resourceType)) {
			throw new IllegalArgumentException("Parameters.resourceType must be Patient");
		}
		request.setResourceType(StringUtils.defaultIfBlank(resourceType, "Patient"));
		return fromPatient(patient, request);
	}
	
	private FuzzyPatientMatchRequest fromPatient(Patient patient, FuzzyPatientMatchRequest request) {
		if (patient == null) {
			throw new IllegalArgumentException("Patient resource is required");
		}
		request.setResourceType(StringUtils.defaultIfBlank(request.getResourceType(), patient.getResourceType().name()));
		IdentifierHolder identifierHolder = extractIdentifier(patient);
		request.setIdentifierSystem(identifierHolder.getSystem());
		request.setIdentifier(identifierHolder.getValue());
		NameHolder nameHolder = extractName(patient);
		request.setGivenName(nameHolder.getGivenName());
		request.setFamilyName(nameHolder.getFamilyName());
		request.setName(nameHolder.getDisplayName());
		request.setBirthDate(extractBirthDate(patient));
		request.setPhone(extractPhone(patient));
		request.setAddress(extractAddress(patient));
		if (patient.hasGender()) {
			request.setGender(patient.getGender().toCode());
		}
		if (!request.hasAnySearchField()) {
			throw new IllegalArgumentException("Patient resource must contain at least one search field");
		}
		return request;
	}
	
	private NameHolder extractName(Patient patient) {
		if (!patient.hasName()) {
			return new NameHolder(null, null, null);
		}
		String given = StringUtils.trimToNull(patient.getNameFirstRep().getGivenAsSingleString());
		String family = StringUtils.trimToNull(patient.getNameFirstRep().getFamily());
		if (StringUtils.isNotBlank(patient.getNameFirstRep().getText())) {
			return new NameHolder(given, family, patient.getNameFirstRep().getText());
		}
		StringBuilder name = new StringBuilder();
		for (StringType givenPart : patient.getNameFirstRep().getGiven()) {
			if (givenPart == null || StringUtils.isBlank(givenPart.getValue())) {
				continue;
			}
			if (name.length() > 0) {
				name.append(' ');
			}
			name.append(givenPart.getValue().trim());
		}
		if (StringUtils.isNotBlank(patient.getNameFirstRep().getFamily())) {
			if (name.length() > 0) {
				name.append(' ');
			}
			name.append(patient.getNameFirstRep().getFamily().trim());
		}
		return new NameHolder(given, family, name.length() > 0 ? name.toString() : null);
	}
	
	private IdentifierHolder extractIdentifier(Patient patient) {
		for (org.hl7.fhir.r4.model.Identifier identifier : patient.getIdentifier()) {
			if (identifier == null || StringUtils.isBlank(identifier.getValue())) {
				continue;
			}
			return new IdentifierHolder(StringUtils.trimToNull(identifier.getSystem()), StringUtils.trimToNull(identifier
			        .getValue()));
		}
		return new IdentifierHolder(null, null);
	}
	
	private LocalDate extractBirthDate(Patient patient) {
		if (!patient.hasBirthDateElement()) {
			return null;
		}
		String birthDate = patient.getBirthDateElement().asStringValue();
		if (StringUtils.isBlank(birthDate) || birthDate.length() < 10) {
			return null;
		}
		try {
			return LocalDate.parse(birthDate.substring(0, 10));
		}
		catch (DateTimeParseException ex) {
			throw new IllegalArgumentException("Patient.birthDate must be yyyy-MM-dd");
		}
	}
	
	private String extractPhone(Patient patient) {
		for (ContactPoint telecom : patient.getTelecom()) {
			if (telecom == null || StringUtils.isBlank(telecom.getValue())) {
				continue;
			}
			if (!telecom.hasSystem() || telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
				return telecom.getValue();
			}
		}
		return null;
	}
	
	private String extractAddress(Patient patient) {
		if (!patient.hasAddress()) {
			return null;
		}
		Address address = patient.getAddressFirstRep();
		if (StringUtils.isNotBlank(address.getText())) {
			return address.getText();
		}
		StringBuilder value = new StringBuilder();
		for (StringType line : address.getLine()) {
			if (line == null || StringUtils.isBlank(line.getValue())) {
				continue;
			}
			if (value.length() > 0) {
				value.append(' ');
			}
			value.append(line.getValue().trim());
		}
		appendPart(value, address.getCity());
		appendPart(value, address.getState());
		appendPart(value, address.getCountry());
		return value.length() > 0 ? value.toString() : null;
	}
	
	private void appendPart(StringBuilder sb, String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(' ');
		}
		sb.append(value.trim());
	}
	
	private static final class IdentifierHolder {
		
		private final String system;
		
		private final String value;
		
		private IdentifierHolder(String system, String value) {
			this.system = system;
			this.value = value;
		}
		
		private String getSystem() {
			return system;
		}
		
		private String getValue() {
			return value;
		}
	}
	
	private static final class NameHolder {
		
		private final String givenName;
		
		private final String familyName;
		
		private final String displayName;
		
		private NameHolder(String givenName, String familyName, String displayName) {
			this.givenName = givenName;
			this.familyName = familyName;
			this.displayName = displayName;
		}
		
		private String getGivenName() {
			return givenName;
		}
		
		private String getFamilyName() {
			return familyName;
		}
		
		private String getDisplayName() {
			return displayName;
		}
	}
}
