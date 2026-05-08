package org.openmrs.module.ihmodule.api.patientexchange.utils;

import org.springframework.beans.factory.annotation.Value;

public abstract class IHConstant {
	
	@Value("${intelehealth.fhir.resource.location.import}")
	protected String importLocation;
	
	@Value("${intelehealth.fhir.resource.location.export}")
	protected String exportLocation;
	
	@Value("${intelehealth.fhir.resource.patient.created.export}")
	protected String exportCreatedPatient;
	
	@Value("${intelehealth.fhir.resource.patient.modified.export}")
	protected String exportModifiedPatient;
	
	@Value("${intelehealth.fhir.resource.practitioner.export}")
	protected String exportPractitioner;
	
	@Value("${intelehealth.fhir.resource.encounter.export}")
	protected String exportEncounter;
	
	@Value("${intelehealth.fhir.resource.observation.export}")
	protected String exportObservation;
	
	@Value("${intelehealth.fhir.resource.medication.export}")
	protected String exportMedication;
	
	@Value("${intelehealth.fhir.resource.medication.request.export}")
	protected String exportMedicationRequest;
	
	@Value("${intelehealth.fhir.resource.service.request.export}")
	protected String exportServiceRequest;
	
	@Value("${intelehealth.fhir.resource.diagnostic.report.export}")
	protected String exportDiagnosticReport;
	
	@Value("${intelehealth.fhir.resource.identifier.name}")
	protected String globalIdentifierName;
	
	@Value("${local.openmrs.openhim.url}")
	public String localOpenmrsOpenhimURL;
	
	@Value("${local.openmrs.openhim.clientid.password.basic.auth}")
	protected String localOpenmrsOpenhimAuthentication;
	
	@Value("${opencr.openhim.url}")
	protected String opencrOpenhimURL;
	
	@Value("${opencr.openhim.clientid.password.basic.auth}")
	protected String opencrOpenhimAuthentication;
	
	@Value("${gofr.openhim.url}")
	protected String gofrOpenhimURL;
	
	@Value("${gofr.openhim.clientid.password.basic.auth}")
	protected String gofrOpenhimAuthentication;
	
	@Value("${opencr.shr.url}")
	protected String mciURL;
	
	@Value("${intelehealth.fhir.structuredefinition.extension.url}")
	protected String sdExtensionURL;
	
	@Value("${intelehealth.fhir.central.url}")
	protected String centralFhirURL;
	
	@Value("${intelehealth.fhir.patient.profile.url}")
	protected String patientProfileUrl;
	
	@Value("${intelehealth.fhir.patient.profile.definition.path:structureDefinition/StructureDefinition-IH-patient-profile.json}")
	protected String patientProfileDefinitionPath;
	
}
