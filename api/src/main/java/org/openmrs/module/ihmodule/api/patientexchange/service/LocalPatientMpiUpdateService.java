package org.openmrs.module.ihmodule.api.patientexchange.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.utils.IHConstant;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Applies MPI identifier changes directly on the facility OpenMRS FHIR2 Patient resource (same
 * database as the scheduler export source).
 */
@Service
public class LocalPatientMpiUpdateService extends IHConstant {
	
	/** Response body when {@code POST .../mpi/local} skips because MPI is already present. */
	public static final String MESSAGE_MPI_ALREADY_SET_LOCAL_ENDPOINT = "MPI is already assigned on this local patient. The local MPI identifier was not updated.";
	
	/** Response body when {@code POST .../sync/force} skips because MPI is already present. */
	public static final String MESSAGE_MPI_ALREADY_SET_FORCE_SYNC = "MPI is already assigned on this local patient. Force sync was not performed.";
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	private static final String V2_0203_SYSTEM = "http://terminology.hl7.org/CodeSystem/v2-0203";
	
	private static final String OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL = "http://fhir.openmrs.org/ext/patient/identifier#location";
	
	private static final String OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	@Autowired
	private FhirConfig firFhirConfig;
	
	/**
	 * Same rule as patient export scheduling: {@code true} when any identifier's type text equals
	 * {@code intelehealth.fhir.resource.identifier.name} ({@link #globalIdentifierName}), matching
	 * {@code DataSendToFHIR#hasMPI} semantics (null-safe; no extra heuristics) so operator
	 * endpoints do not diverge from the scheduler.
	 */
	public boolean patientHasMpiPerSchedulerExportRule(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return false;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (!identifier.hasType() || !identifier.getType().hasText()) {
				continue;
			}
			String text = identifier.getType().getText();
			if (text != null && text.equals(globalIdentifierName)) {
				return true;
			}
		}
		return false;
	}
	
	public void applyMpiIdentifierToLocalPatient(String patientUuid, String mpiIdentifierValue) {
		if (patientUuid == null || patientUuid.trim().isEmpty()) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		if (mpiIdentifierValue == null || mpiIdentifierValue.trim().isEmpty()) {
			throw new IllegalArgumentException("mpiIdentifierValue is required");
		}
		String uuid = patientUuid.trim();
		String mpiVal = mpiIdentifierValue.trim();
		Bundle bundle = firFhirConfig.getLocalOpenMRSFhirContext().search().byUrl("Patient?_id=" + uuid)
		        .returnBundle(Bundle.class).execute();
		if (bundle == null || !bundle.hasEntry()) {
			throw new IllegalStateException("Local Patient not found for uuid=" + uuid);
		}
		
		BundleEntryComponent first = bundle.getEntryFirstRep();
		if (!(first.getResource() instanceof Patient)) {
			throw new IllegalStateException("Expected Patient resource in bundle for uuid=" + uuid);
		}
		
		Patient patient = (Patient) first.getResource();
		if (patientHasMpiPerSchedulerExportRule(patient)) {
			throw new LocalMpiAlreadySetException();
		}
		upsertMpiIdentifier(patient, mpiVal);
		
		ensureIdentifierLocationForOpenmrsUpdate(patient);
		
		firFhirConfig.getLocalOpenMRSFhirContext().update().resource(patient).execute();
	}
	
	private void upsertMpiIdentifier(Patient patient, String mpiVal) {
		for (Identifier identifier : patient.getIdentifier()) {
			if (!identifier.hasType()) {
				continue;
			}
			String text = identifier.getType().getText();
			if (text != null && (text.equalsIgnoreCase(globalIdentifierName) || MPI_TYPE_TEXT.equalsIgnoreCase(text))) {
				ensureMpiIdentifierShape(identifier, mpiVal);
				return;
			}
		}
		
		Identifier mpi = new Identifier();
		mpi.setUse(IdentifierUse.OFFICIAL);
		CodeableConcept type = new CodeableConcept();
		type.setText(globalIdentifierName);
		Coding coding = new Coding();
		coding.setSystem(V2_0203_SYSTEM);
		coding.setCode("MR");
		type.addCoding(coding);
		mpi.setType(type);
		mpi.setSystem(null);
		mpi.setValue(mpiVal);
		patient.addIdentifier(mpi);
	}
	
	private void ensureMpiIdentifierShape(Identifier identifier, String mpiVal) {
		identifier.setValue(mpiVal);
		identifier.setUse(IdentifierUse.OFFICIAL);
		if (!identifier.hasType()) {
			identifier.setType(new CodeableConcept());
		}
		identifier.getType().setText(globalIdentifierName);
		if (!identifier.getType().hasCoding()) {
			identifier.getType().addCoding().setSystem(V2_0203_SYSTEM).setCode("MR");
		}
		identifier.setSystem(null);
	}
	
	private void ensureIdentifierLocationForOpenmrsUpdate(Patient patient) {
		if (patient == null || patient.getIdentifier() == null) {
			return;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			boolean hasLocation = identifier.getExtension().stream()
					.anyMatch(ext -> OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL.equals(ext.getUrl()));
			if (hasLocation) {
				continue;
			}
			Extension locationExtension = new Extension();
			locationExtension.setUrl(OPENMRS_IDENTIFIER_LOCATION_EXTENSION_URL);
			locationExtension.setValue(new Reference("Location/" + OPENMRS_DEFAULT_IDENTIFIER_LOCATION_UUID));
			identifier.getExtension().add(locationExtension);
		}
	}
}
