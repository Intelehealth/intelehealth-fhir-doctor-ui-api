package org.openmrs.module.ihmodule.api.patientexchange.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Patient;

/**
 * Single definition of allowed Patient extension suffixes (IG / profile) for import and transfer.
 */
public final class PatientProfileExtensionRules {
	
	private PatientProfileExtensionRules() {
	}
	
	public static final Set<String> ALLOWED_STRUCTURE_DEFINITION_SUFFIXES = Collections.unmodifiableSet(new HashSet<>(
	        Arrays.asList("Economic-Status", "Education-Level", "NationalID", "occupation", "Emergency-Contact-Number",
	            "Household-Number", "Caste")));
	
	public static String extensionSuffixFromStructureDefinitionUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		}
		int lastSlash = url.lastIndexOf('/');
		return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
	}
	
	public static boolean isAllowedStructureDefinitionSuffix(String suffix) {
		return suffix != null && ALLOWED_STRUCTURE_DEFINITION_SUFFIXES.contains(suffix);
	}
	
	public static boolean isAllowedPatientExtensionUrl(String url) {
		return isAllowedStructureDefinitionSuffix(extensionSuffixFromStructureDefinitionUrl(url));
	}
	
	/**
	 * @return {@code null} if all patient-level extensions are allowed; otherwise a message suitable for
	 *         {@link IllegalArgumentException} or audit.
	 */
	public static String unknownPatientExtensionViolationMessage(Patient patient) {
		if (patient == null || patient.getExtension() == null) {
			return null;
		}
		List<String> parts = patient.getExtension().stream()
		        .map(ext -> ext != null ? extensionSuffixFromStructureDefinitionUrl(ext.getUrl()) : null)
		        .filter(StringUtils::isNotBlank)
		        .filter(suffix -> !ALLOWED_STRUCTURE_DEFINITION_SUFFIXES.contains(suffix))
		        .map(suffix -> "Unknown extension suffix: " + suffix)
		        .collect(Collectors.toList());
		if (parts.isEmpty()) {
			return null;
		}
		return "FHIR profile validation failed: " + String.join("; ", parts);
	}
	
	public static void assertKnownPatientProfileExtensions(Patient patient) {
		String msg = unknownPatientExtensionViolationMessage(patient);
		if (msg != null) {
			throw new IllegalArgumentException(msg);
		}
	}
}
