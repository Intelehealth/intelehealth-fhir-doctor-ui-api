package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.openmrs.PatientIdentifierType;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportService.ImportFhirIdentifierSpec;

public class PatientUploadImportIdentifierHandlingTest {
	
	private static final String OPENMRS_ID_SYSTEM = "http://intelehealth-central-fhir.mpower-social.com/fhir/StructureDefinition/OpenMRS-ID";
	
	@Test
	public void collectImportIdentifierSpecs_shouldIncludeOpenMrsIdWhenPresentInPayload() {
		Patient patient = patientWithIdentifiers(
		    openMrsIdentifier("OMRS-12345"),
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "07775e00-b9a9-4bf4-9c65-f0b887e266e4"));
		
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patient);
		
		assertEquals(2, specs.size());
		assertTrue(specs.stream().anyMatch(spec -> spec.isOpenMrsId() && "OMRS-12345".equals(spec.getValue())));
		assertTrue(specs.stream().anyMatch(spec -> "BRN".equals(spec.getTypeName()) && !spec.isOpenMrsId()));
	}
	
	@Test
	public void collectImportIdentifierSpecs_shouldExcludeOpenMrsIdWhenAbsentFromPayload() {
		Patient patient = patientWithIdentifiers(
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "07775e00-b9a9-4bf4-9c65-f0b887e266e4"),
		    identifier("NID", "NID", "/StructureDefinition/NID", "07775e00-b9a9-4bf4-9c65-f0b887e266e5"));
		
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patient);
		
		assertEquals(2, specs.size());
		assertFalse(specs.stream().anyMatch(ImportFhirIdentifierSpec::isOpenMrsId));
	}
	
	@Test
	public void validateImportIdentifierTypesResolvable_shouldIgnoreOpenMrsIdWhenCheckingTypes() {
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patientWithIdentifiers(
		    openMrsIdentifier("OMRS-1")));
		
		PatientUploadImportService.validateImportIdentifierTypesResolvable(specs, name -> null);
	}
	
	@Test
	public void validateImportIdentifierTypesResolvable_shouldPassForResolvableTypes() {
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patientWithIdentifiers(
		    openMrsIdentifier("OMRS-1"),
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "brn-1"),
		    identifier("NID", "NID", "/StructureDefinition/NID", "nid-1")));
		Map<String, PatientIdentifierType> types = new HashMap<String, PatientIdentifierType>();
		types.put("BRN", new PatientIdentifierType());
		types.put("NID", new PatientIdentifierType());
		
		PatientUploadImportService.validateImportIdentifierTypesResolvable(specs, name -> types.get(name));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void validateImportIdentifierTypesResolvable_shouldFailForMissingIdentifierType() {
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patientWithIdentifiers(
		    openMrsIdentifier("OMRS-1"),
		    identifier("UNKNOWN_TYPE", "XX", "/StructureDefinition/UNKNOWN_TYPE", "abc-123")));
		
		PatientUploadImportService.validateImportIdentifierTypesResolvable(specs, name -> null);
	}
	
	@Test
	public void validateImportIdentifierTypesResolvable_shouldFailWithClearMessageForMissingType() {
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patientWithIdentifiers(
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "brn-1"),
		    identifier("FOO", "XX", "/StructureDefinition/FOO", "foo-1")));
		Map<String, PatientIdentifierType> types = new HashMap<String, PatientIdentifierType>();
		types.put("BRN", new PatientIdentifierType());
		
		try {
			PatientUploadImportService.validateImportIdentifierTypesResolvable(specs, name -> types.get(name));
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Unknown OpenMRS patient identifier type: FOO", ex.getMessage());
			return;
		}
		throw new AssertionError("Expected IllegalArgumentException for missing identifier type");
	}
	
	@Test
	public void validateImportIdentifierTypesResolvable_shouldFailOnFirstMissingTypeInMixedPayload() {
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patientWithIdentifiers(
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "brn-1"),
		    identifier("NID", "NID", "/StructureDefinition/NID", "nid-1"),
		    identifier("BAD", "XX", "/StructureDefinition/BAD", "bad-1")));
		Map<String, PatientIdentifierType> types = new HashMap<String, PatientIdentifierType>();
		types.put("BRN", new PatientIdentifierType());
		types.put("NID", new PatientIdentifierType());
		
		try {
			PatientUploadImportService.validateImportIdentifierTypesResolvable(specs, name -> types.get(name));
		}
		catch (IllegalArgumentException ex) {
			assertEquals("Unknown OpenMRS patient identifier type: BAD", ex.getMessage());
			return;
		}
		throw new AssertionError("Expected IllegalArgumentException for mixed validity payload");
	}
	
	@Test
	public void collectImportIdentifierSpecs_shouldSkipBlankValuesAndUnresolvableTypeNames() {
		Identifier blankValue = identifier("BRN", "MR", "/StructureDefinition/BRN", " ");
		Identifier blankType = new Identifier();
		blankType.setValue("value-without-type");
		Patient patient = patientWithIdentifiers(blankValue, blankType,
		    identifier("NID", "NID", "/StructureDefinition/NID", "nid-ok"));
		
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService.collectImportIdentifierSpecs(patient);
		
		assertEquals(1, specs.size());
		assertEquals("NID", specs.get(0).getTypeName());
	}
	
	@Test
	public void isOpenMrsIdentifier_shouldTreatBrnWithMrCodeAsNonOpenMrsId() {
		Identifier brn = identifier("BRN", "MR", "/StructureDefinition/BRN", "07775e00-b9a9-4bf4-9c65-f0b887e266e4");
		
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService
		        .collectImportIdentifierSpecs(patientWithIdentifiers(brn));
		
		assertEquals(1, specs.size());
		assertEquals("BRN", specs.get(0).getTypeName());
		assertFalse(specs.get(0).isOpenMrsId());
	}
	
	@Test
	public void isOpenMrsIdentifier_shouldTreatOpenMrsSystemAsOpenMrsId() {
		List<ImportFhirIdentifierSpec> specs = PatientUploadImportService
		        .collectImportIdentifierSpecs(patientWithIdentifiers(openMrsIdentifier("OMRS-99")));
		
		assertEquals(1, specs.size());
		assertTrue(specs.get(0).isOpenMrsId());
		assertEquals("OpenMRS ID", specs.get(0).getTypeName());
	}
	
	private static Patient patientWithIdentifiers(Identifier... identifiers) {
		Patient patient = new Patient();
		patient.getIdentifier().addAll(Arrays.asList(identifiers));
		return patient;
	}
	
	private static Identifier openMrsIdentifier(String value) {
		Identifier identifier = new Identifier();
		identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
		CodeableConcept type = new CodeableConcept();
		type.setText("OpenMRS ID");
		type.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203").setCode("MR"));
		identifier.setType(type);
		identifier.setSystem(OPENMRS_ID_SYSTEM);
		identifier.setValue(value);
		return identifier;
	}
	
	private static Identifier identifier(String typeText, String code, String system, String value) {
		Identifier identifier = new Identifier();
		identifier.setUse(Identifier.IdentifierUse.USUAL);
		CodeableConcept type = new CodeableConcept();
		type.setText(typeText);
		type.addCoding(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203").setCode(code));
		identifier.setType(type);
		identifier.setSystem(system);
		identifier.setValue(value);
		return identifier;
	}
}
