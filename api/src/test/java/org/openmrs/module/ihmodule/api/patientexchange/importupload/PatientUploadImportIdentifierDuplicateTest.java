package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientexchange.importupload.PatientUploadImportService.ImportIdentifierDuplicateMatch;

public class PatientUploadImportIdentifierDuplicateTest {
	
	@Test
	public void findFirstDuplicateIdentifier_shouldMatchExistingBrn() {
		Patient patient = patientWithIdentifiers(identifier("BRN", "MR", "/StructureDefinition/BRN",
		    "07775e00-b9a9-4bf4-9c65-f0b887e266e4"));
		Map<String, String> existing = new HashMap<String, String>();
		existing.put("BRN|07775e00-b9a9-4bf4-9c65-f0b887e266e4", "yes");
		
		Optional<ImportIdentifierDuplicateMatch> match = PatientUploadImportService.findFirstDuplicateIdentifier(patient,
		    (type, value) -> existing.containsKey(type + "|" + value));
		
		assertTrue(match.isPresent());
		assertEquals("BRN", match.get().getIdentifierTypeName());
		assertEquals("07775e00-b9a9-4bf4-9c65-f0b887e266e4", match.get().getIdentifierValue());
	}
	
	@Test
	public void findFirstDuplicateIdentifier_shouldMatchExistingNid() {
		Patient patient = patientWithIdentifiers(identifier("NID", "NID", "/StructureDefinition/NID",
		    "07775e00-b9a9-4bf4-9c65-f0b887e266e5"));
		Map<String, String> existing = new HashMap<String, String>();
		existing.put("NID|07775e00-b9a9-4bf4-9c65-f0b887e266e5", "yes");
		
		Optional<ImportIdentifierDuplicateMatch> match = PatientUploadImportService.findFirstDuplicateIdentifier(patient,
		    (type, value) -> existing.containsKey(type + "|" + value));
		
		assertTrue(match.isPresent());
		assertEquals("NID", match.get().getIdentifierTypeName());
		assertEquals("07775e00-b9a9-4bf4-9c65-f0b887e266e5", match.get().getIdentifierValue());
	}
	
	@Test
	public void findFirstDuplicateIdentifier_shouldMatchWhenOneOfMultipleIdentifiersExists() {
		Patient patient = patientWithIdentifiers(
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "07775e00-b9a9-4bf4-9c65-f0b887e266e4"),
		    identifier("NID", "NID", "/StructureDefinition/NID", "07775e00-b9a9-4bf4-9c65-f0b887e266e5"));
		Map<String, String> existing = new HashMap<String, String>();
		existing.put("NID|07775e00-b9a9-4bf4-9c65-f0b887e266e5", "yes");
		
		Optional<ImportIdentifierDuplicateMatch> match = PatientUploadImportService.findFirstDuplicateIdentifier(patient,
		    (type, value) -> existing.containsKey(type + "|" + value));
		
		assertTrue(match.isPresent());
		assertEquals("NID", match.get().getIdentifierTypeName());
	}
	
	@Test
	public void findFirstDuplicateIdentifier_shouldReturnEmptyWhenNoIdentifiersMatch() {
		Patient patient = patientWithIdentifiers(
		    identifier("BRN", "MR", "/StructureDefinition/BRN", "07775e00-b9a9-4bf4-9c65-f0b887e266e4"),
		    identifier("NID", "NID", "/StructureDefinition/NID", "07775e00-b9a9-4bf4-9c65-f0b887e266e5"));
		
		Optional<ImportIdentifierDuplicateMatch> match = PatientUploadImportService.findFirstDuplicateIdentifier(patient,
		    (type, value) -> false);
		
		assertFalse(match.isPresent());
	}
	
	@Test
	public void findFirstDuplicateIdentifier_shouldNotBlockImportForUnmappedIdentifierType() {
		Patient patient = patientWithIdentifiers(identifier("UNKNOWN_TYPE", "XX", "/StructureDefinition/UNKNOWN_TYPE",
		    "abc-123"));
		
		Optional<ImportIdentifierDuplicateMatch> match = PatientUploadImportService.findFirstDuplicateIdentifier(patient,
		    (type, value) -> false);
		
		assertFalse(match.isPresent());
	}
	
	@Test
	public void findFirstDuplicateIdentifier_shouldSkipBlankIdentifierValues() {
		Identifier blankValue = identifier("BRN", "MR", "/StructureDefinition/BRN", " ");
		Patient patient = patientWithIdentifiers(blankValue);
		
		Optional<ImportIdentifierDuplicateMatch> match = PatientUploadImportService.findFirstDuplicateIdentifier(patient,
		    (type, value) -> true);
		
		assertFalse(match.isPresent());
	}
	
	@Test
	public void extractImportIdentifierTypeName_shouldReadTypeTextAndSystemSuffix() {
		Identifier brn = identifier("BRN", "MR", "/StructureDefinition/BRN", "07775e00-b9a9-4bf4-9c65-f0b887e266e4");
		Identifier nid = identifier("NID", "NID", "/StructureDefinition/NID", "07775e00-b9a9-4bf4-9c65-f0b887e266e5");
		
		assertEquals("BRN", PatientUploadImportService.extractImportIdentifierTypeName(brn));
		assertEquals("NID", PatientUploadImportService.extractImportIdentifierTypeName(nid));
	}
	
	@Test
	public void extractImportIdentifierTypeName_shouldReturnNullForMissingTypeAndValue() {
		Identifier identifier = new Identifier();
		assertNull(PatientUploadImportService.extractImportIdentifierTypeName(identifier));
		assertNull(PatientUploadImportService.extractImportIdentifierTypeName(null));
	}
	
	private static Patient patientWithIdentifiers(Identifier... identifiers) {
		Patient patient = new Patient();
		for (Identifier identifier : identifiers) {
			patient.addIdentifier(identifier);
		}
		return patient;
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
