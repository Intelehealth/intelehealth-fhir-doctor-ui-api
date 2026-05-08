package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates {@link MpiPatientDuplicateReviewCase} when an operator completes force-sync or local MPI
 * assignment.
 */
@Service
public class MpiDuplicateReviewResolutionService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MpiDuplicateReviewResolutionService.class);
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	private MpiPatientDuplicateReviewCaseRepository caseRepository = Context.getRegisteredComponent(
	    "mpiPatientDuplicateReviewCaseRepository", MpiPatientDuplicateReviewCaseRepository.class);
	
	@Value("${intelehealth.fhir.resource.identifier.name}")
	private String mpiIdentifierTypeText;
	
	@Transactional
	public void resolvePendingCaseAfterSuccessfulForceSync(String localPatientUuid, Bundle mciResponseBundle,
	        String resolvedBy) {
		if (localPatientUuid == null || mciResponseBundle == null || resolvedBy == null) {
			return;
		}
		Optional<MpiPatientDuplicateReviewCase> pending = caseRepository
		        .findFirstByLocalPatientUuidAndReviewStatusOrderByIdDesc(localPatientUuid, MpiDuplicateReviewStatus.PENDING);
		if (!pending.isPresent()) {
			LOGGER.debug("No pending duplicate-review case for local patient {}; skip force-sync resolution",
			    localPatientUuid);
			return;
		}
		Patient remotePatient = extractPatientFromBundle(mciResponseBundle);
		String logicalId = remotePatient != null && remotePatient.getIdElement().hasIdPart() ? remotePatient.getIdElement()
		        .getIdPart() : extractLogicalIdFirstEntry(mciResponseBundle);
		String mpiValue = extractMpiIdentifierValue(remotePatient);
		
		MpiPatientDuplicateReviewCase row = pending.get();
		row.setReviewStatus(MpiDuplicateReviewStatus.RESOLVED_FORCE_SEND);
		row.setResolvedBy(resolvedBy);
		row.setChosenFhirPatientLogicalId(logicalId);
		row.setChosenMpiIdentifierValue(mpiValue);
		row.setDateResolved(DateUtils.toFormattedDateNow());
		caseRepository.save(row);
		LOGGER.info("Duplicate-review case {} resolved after force-sync for local patient {} (chosen FHIR id={}, MPI={})",
		    row.getCaseUuid(), localPatientUuid, logicalId, mpiValue);
	}
	
	@Transactional
	public void resolvePendingCaseAfterLocalMpiUpdate(String localPatientUuid, String mpiIdentifierValue,
	        String chosenFhirPatientLogicalId, String resolvedBy) {
		if (localPatientUuid == null || mpiIdentifierValue == null || resolvedBy == null) {
			return;
		}
		Optional<MpiPatientDuplicateReviewCase> pending = caseRepository
		        .findFirstByLocalPatientUuidAndReviewStatusOrderByIdDesc(localPatientUuid, MpiDuplicateReviewStatus.PENDING);
		if (!pending.isPresent()) {
			LOGGER.debug("No pending duplicate-review case for local patient {}; skip local-MPI resolution",
			    localPatientUuid);
			return;
		}
		MpiPatientDuplicateReviewCase row = pending.get();
		row.setReviewStatus(MpiDuplicateReviewStatus.RESOLVED_LINK_EXISTING);
		row.setResolvedBy(resolvedBy);
		row.setChosenMpiIdentifierValue(mpiIdentifierValue.trim());
		if (chosenFhirPatientLogicalId != null && !chosenFhirPatientLogicalId.trim().isEmpty()) {
			row.setChosenFhirPatientLogicalId(chosenFhirPatientLogicalId.trim());
		}
		row.setDateResolved(DateUtils.toFormattedDateNow());
		caseRepository.save(row);
		LOGGER.info(
		    "Duplicate-review case {} resolved after local MPI update for local patient {} (chosen FHIR id={}, MPI={})",
		    row.getCaseUuid(), localPatientUuid, row.getChosenFhirPatientLogicalId(), mpiIdentifierValue.trim());
	}
	
	private Patient extractPatientFromBundle(Bundle bundle) {
		if (bundle == null || !bundle.hasEntry()) {
			return null;
		}
		for (BundleEntryComponent e : bundle.getEntry()) {
			Resource r = e.hasResource() ? e.getResource() : null;
			if (r instanceof Patient) {
				return (Patient) r;
			}
		}
		return null;
	}
	
	private String extractLogicalIdFirstEntry(Bundle bundle) {
		if (bundle == null || bundle.getEntry().size() != 1) {
			return null;
		}
		Resource resource = bundle.getEntryFirstRep().getResource();
		return resource != null && resource.getIdElement().hasIdPart() ? resource.getIdElement().getIdPart() : null;
	}
	
	private String extractMpiIdentifierValue(Patient patient) {
		if (patient == null || !patient.hasIdentifier()) {
			return null;
		}
		for (Identifier identifier : patient.getIdentifier()) {
			if (!identifier.hasType()) {
				continue;
			}
			String text = identifier.getType().getText();
			if (text == null) {
				continue;
			}
			if (text.equalsIgnoreCase(mpiIdentifierTypeText) || MPI_TYPE_TEXT.equalsIgnoreCase(text)) {
				return identifier.getValue();
			}
		}
		return null;
	}
}
