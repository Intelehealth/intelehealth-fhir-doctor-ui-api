package org.openmrs.module.ihmodule.api.patientexchange.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

public abstract class IHConstant {
	
	private static final Properties MODULE_PROPERTIES = ModuleClasspathPropertiesLoader.loadModuleProperties();
	
	protected String importLocation = resolveProperty("intelehealth.fhir.resource.location.import", null);
	
	protected String exportLocation = resolveProperty("intelehealth.fhir.resource.location.export", null);
	
	protected String exportCreatedPatient = resolveProperty("intelehealth.fhir.resource.patient.created.export", null);
	
	protected String exportModifiedPatient = resolveProperty("intelehealth.fhir.resource.patient.modified.export", null);
	
	protected String exportPractitioner = resolveProperty("intelehealth.fhir.resource.practitioner.export", null);
	
	protected String exportEncounter = resolveProperty("intelehealth.fhir.resource.encounter.export", null);
	
	protected String exportObservation = resolveProperty("intelehealth.fhir.resource.observation.export", null);
	
	protected String exportMedication = resolveProperty("intelehealth.fhir.resource.medication.export", null);
	
	protected String exportMedicationRequest = resolveProperty("intelehealth.fhir.resource.medication.request.export", null);
	
	protected String exportServiceRequest = resolveProperty("intelehealth.fhir.resource.service.request.export", null);
	
	protected String exportDiagnosticReport = resolveProperty("intelehealth.fhir.resource.diagnostic.report.export", null);
	
	protected String globalIdentifierName = resolveProperty("intelehealth.fhir.resource.identifier.name", null);
	
	protected String sourcePatientIdTypeUuid = resolveProperty("intelehealth.fhir.patient.source.identifier.type.uuid",
	    null, "b2f192c2-346a-486c-bcb4-7a35616890ba");
	
	protected String sourcePatientIdTypeName = resolveProperty("intelehealth.fhir.patient.source.identifier.type.name",
	    null, "Source Patient Id");
	
	public String localOpenmrsOpenhimURL = resolveLocalOpenmrsUrl();
	
	protected String localOpenmrsOpenhimAuthentication = resolveLocalOpenmrsAuthentication();
	
	protected String opencrOpenhimURL = resolveProperty("opencr.openhim.url", "intelehealth.fhir.opencr.openhim.url");
	
	protected String opencrOpenhimAuthentication = resolveOpenCrCredentialFromModuleProperties();
	
	protected String gofrOpenhimURL = resolveProperty("gofr.openhim.url", "intelehealth.fhir.gofr.openhim.url");
	
	protected String gofrOpenhimAuthentication = resolveProperty("gofr.openhim.clientid.password.basic.auth",
	    "intelehealth.fhir.gofr.openhim.authentication");
	
	protected String mciURL = resolveProperty("opencr.shr.url", null);
	
	protected String sdExtensionURL = resolveProperty("intelehealth.fhir.structuredefinition.extension.url", null);
	
	protected String centralFhirURL = resolveProperty("intelehealth.fhir.central.url", null);
	
	protected String patientProfileUrl = resolveProperty("intelehealth.fhir.patient.profile.url", null);
	
	protected String patientProfileDefinitionPath = resolveProperty("intelehealth.fhir.patient.profile.definition.path",
	    null, "structureDefinition/StructureDefinition-IH-patient-profile.json");
	
	private static String resolveProperty(String primaryKey, String legacyKey) {
		return resolveProperty(primaryKey, legacyKey, "");
	}
	
	private static String resolveProperty(String primaryKey, String legacyKey, String defaultValue) {
		String value = null;
		if (MODULE_PROPERTIES != null) {
			value = MODULE_PROPERTIES.getProperty(primaryKey);
			if (StringUtils.isBlank(value) && StringUtils.isNotBlank(legacyKey)) {
				value = MODULE_PROPERTIES.getProperty(legacyKey);
			}
		}
		return StringUtils.defaultIfBlank(StringUtils.trimToEmpty(value), defaultValue);
	}
	
	/**
	 * Classpath-only resolution;
	 * {@link org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig} merges global
	 * properties using the same key order.
	 */
	private static String resolveOpenCrCredentialFromModuleProperties() {
		String v = resolveProperty("opencr.openhim.mediator.password.basic.auth",
		    "opencr.openhim.clientid.password.basic.auth");
		if (StringUtils.isNotBlank(StringUtils.trimToEmpty(v))) {
			return v.trim();
		}
		return resolveProperty("intelehealth.fhir.opencr.openhim.authentication", null);
	}
	
	private static String resolveLocalOpenmrsUrl() {
		String v = resolveProperty("local.openmrs.url", "local.openmrs.openhim.url");
		if (StringUtils.isNotBlank(v)) {
			return v;
		}
		return resolveProperty("intelehealth.fhir.local.openmrs.openhim.url", null);
	}
	
	private static String resolveLocalOpenmrsAuthentication() {
		String v = resolveProperty("local.openmrs.password.basic.auth", "local.openmrs.openhim.clientid.password.basic.auth");
		if (StringUtils.isNotBlank(v)) {
			return v;
		}
		return resolveProperty("intelehealth.fhir.local.openmrs.openhim.authentication", null);
	}
	
}
