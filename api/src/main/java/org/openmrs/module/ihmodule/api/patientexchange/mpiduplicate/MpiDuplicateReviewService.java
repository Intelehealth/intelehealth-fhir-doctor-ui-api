package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.openmrs.module.ihmodule.api.patientexchange.config.FhirContextHolder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Persists {@link MpiPatientDuplicateReviewCase} rows when a FHIR Patient search returns more than
 * one {@link Patient} entry. Does not alter sync scheduling; callers invoke this only after
 * detecting multiple matches.
 */
@Service
public class MpiDuplicateReviewService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MpiDuplicateReviewService.class);
	
	private static final String MPI_TYPE_TEXT = "MPI";
	
	private final FhirContext fhirContext = FhirContextHolder.R4;
	
	@Autowired
	private MpiPatientDuplicateReviewCaseRepository caseRepository;
	
	@Value("${intelehealth.fhir.resource.identifier.name}")
	private String mpiIdentifierTypeText;
	
	/**
	 * Parses {@code searchBundleJson} as a FHIR Bundle and delegates to
	 * {@link #persistIfAmbiguous(String, Patient, Bundle, String)}.
	 */
	@Transactional
	public Optional<MpiPatientDuplicateReviewCase> persistIfAmbiguous(String localPatientUuid, Patient sourcePatient,
	        String searchBundleJson, String outboundBundleJson) {
		if (searchBundleJson == null || searchBundleJson.isEmpty()) {
			return Optional.empty();
		}
		final Bundle bundle;
		try {
			bundle = fhirContext.newJsonParser().parseResource(Bundle.class, searchBundleJson);
		}
		catch (RuntimeException ex) {
			LOGGER.error("MPI duplicate review: cannot parse OpenCR search Bundle JSON for patient {}: {}",
			    localPatientUuid, ex.getMessage(), ex);
			return Optional.empty();
		}
		return persistIfAmbiguous(localPatientUuid, sourcePatient, bundle, outboundBundleJson, searchBundleJson);
	}
	
	/**
	 * When the search bundle contains two or more Patient matches, inserts a case plus candidate
	 * rows and returns it. Otherwise returns empty and performs no DB write.
	 */
	@Transactional
	public Optional<MpiPatientDuplicateReviewCase> persistIfAmbiguous(String localPatientUuid, Patient sourcePatient,
	        Bundle searchBundle, String outboundBundleJson) {
		String encodedSearch = fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(searchBundle);
		return persistIfAmbiguous(localPatientUuid, sourcePatient, searchBundle, outboundBundleJson, encodedSearch);
	}
	
	private Optional<MpiPatientDuplicateReviewCase> persistIfAmbiguous(String localPatientUuid, Patient sourcePatient,
	        Bundle searchBundle, String outboundBundleJson, String searchBundleJson) {
		List<BundleEntryComponent> patientEntries = extractPatientEntries(searchBundle);
		if (patientEntries.size() < 2) {
			Integer bundleTotal = searchBundle.hasTotal() ? searchBundle.getTotalElement().getValue() : null;
			LOGGER.info(
			    "MPI duplicate review: patient {} — Patient resources in bundle={}, bundle.entry.size={}, bundle.total={}, no DB row",
			    localPatientUuid, patientEntries.size(), searchBundle.hasEntry() ? searchBundle.getEntry().size() : 0,
			    bundleTotal);
			if (bundleTotal != null && bundleTotal >= 2 && patientEntries.size() < 2) {
				LOGGER.warn(
				    "MPI duplicate review: OpenCR reports total={} matches but bundle contains fewer Patient entries (got {}). Increase intelehealth.fhir.mpi.duplicate.precheck.search.count or fix paging.",
				    bundleTotal, patientEntries.size());
			}
			return Optional.empty();
		}
		
		Optional<MpiPatientDuplicateReviewCase> pendingExisting = caseRepository
		        .findFirstByLocalPatientUuidAndReviewStatusOrderByIdDesc(localPatientUuid, MpiDuplicateReviewStatus.PENDING);
		if (pendingExisting.isPresent()) {
			LOGGER.info("MPI duplicate review: reuse existing pending case_uuid={} for local patient {} ({} candidates)",
			    pendingExisting.get().getCaseUuid(), localPatientUuid, pendingExisting.get().getCandidateCount());
			return pendingExisting;
		}
		
		MpiPatientDuplicateReviewCase reviewCase = new MpiPatientDuplicateReviewCase();
		reviewCase.setLocalPatientUuid(localPatientUuid);
		reviewCase.setCandidateCount(patientEntries.size());
		reviewCase.setReviewStatus(MpiDuplicateReviewStatus.PENDING);
		reviewCase.setOutboundBundleJson(outboundBundleJson);
		reviewCase.setSearchBundleJson(searchBundleJson);
		
		fillSourceDemographics(sourcePatient, reviewCase);
		fillSourceAddress(sourcePatient, reviewCase);
		
		for (BundleEntryComponent entry : patientEntries) {
			Patient match = (Patient) entry.getResource();
			MpiPatientDuplicateReviewCandidate row = new MpiPatientDuplicateReviewCandidate();
			row.setFhirPatientLogicalId(resolveFhirPatientLogicalId(entry, match));
			//row.setMpiIdentifierValue(extractMpiIdentifierValue(match));
			row.setMpiIdentifierValue(resolveFhirPatientLogicalId(entry, match));
			fillCandidateDemographics(match, row);
			fillCandidateAddress(match, row);
			row.setPatientResourceJson(fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(match));
			row.setMatchScore(bundleEntryMatchScore(entry));
			row.setMatchSource(MpiImportDuplicateReviewSource.FHIR.getValue());
			reviewCase.addCandidate(row);
		}
		
		MpiPatientDuplicateReviewCase saved = caseRepository.saveAndFlush(reviewCase);
		LOGGER.info("MPI duplicate review: saved case_uuid={} for local patient {} with {} candidates", saved.getCaseUuid(),
		    localPatientUuid, saved.getCandidateCount());
		return Optional.of(saved);
	}
	
	private void fillSourceDemographics(Patient source, MpiPatientDuplicateReviewCase reviewCase) {
		if (source == null) {
			return;
		}
		if (source.hasBirthDate()) {
			reviewCase.setSourceBirthdate(new SimpleDateFormat("yyyy-MM-dd").format(source.getBirthDate()));
		}
		if (source.hasName()) {
			HumanName n = source.getNameFirstRep();
			reviewCase.setSourceFamily(n.getFamily());
			reviewCase.setSourceGiven(n.getGivenAsSingleString());
		}
		if (source.hasGender()) {
			reviewCase.setSourceGenderCode(source.getGender().toCode());
		}
		if (source.hasTelecom() && !source.getTelecom().isEmpty() && source.getTelecom().get(0).getValue() != null) {
			reviewCase.setSourceTelecom(source.getTelecom().get(0).getValue());
		}
	}
	
	private void fillCandidateDemographics(Patient match, MpiPatientDuplicateReviewCandidate row) {
		if (match.hasBirthDate()) {
			row.setCandidateBirthdate(new SimpleDateFormat("yyyy-MM-dd").format(match.getBirthDate()));
		}
		if (match.hasName()) {
			HumanName n = match.getNameFirstRep();
			row.setCandidateFamily(n.getFamily());
			row.setCandidateGiven(n.getGivenAsSingleString());
		}
		if (match.hasGender()) {
			row.setCandidateGenderCode(match.getGender().toCode());
		}
		if (match.hasTelecom() && !match.getTelecom().isEmpty() && match.getTelecom().get(0).getValue() != null) {
			row.setCandidateTelecom(match.getTelecom().get(0).getValue());
		}
	}
	
	private void fillSourceAddress(Patient source, MpiPatientDuplicateReviewCase reviewCase) {
		if (source == null || !source.hasAddress()) {
			return;
		}
		applyFirstAddressStructure(source.getAddress().get(0), reviewCase::setSourceAddressLines,
				reviewCase::setSourceAddressCity, reviewCase::setSourceAddressDistrict,
				reviewCase::setSourceAddressState, reviewCase::setSourceAddressPostalCode,
				reviewCase::setSourceAddressCountry);
		reviewCase.setSourceAddressSnapshot(formatAllAddressesSnapshot(source.getAddress()));
	}
	
	private void fillCandidateAddress(Patient match, MpiPatientDuplicateReviewCandidate row) {
		if (match == null || !match.hasAddress()) {
			return;
		}
		applyFirstAddressStructure(match.getAddress().get(0), row::setCandidateAddressLines,
				row::setCandidateAddressCity, row::setCandidateAddressDistrict,
				row::setCandidateAddressState, row::setCandidateAddressPostalCode,
				row::setCandidateAddressCountry);
		row.setCandidateAddressSnapshot(formatAllAddressesSnapshot(match.getAddress()));
	}
	
	private void applyFirstAddressStructure(Address addr, Consumer<String> linesSetter,
			Consumer<String> citySetter, Consumer<String> districtSetter, Consumer<String> stateSetter,
			Consumer<String> postalSetter, Consumer<String> countrySetter) {
		if (addr == null) {
			return;
		}
		if (addr.hasLine()) {
			String joined = addr.getLine().stream().map(StringType::getValue).collect(Collectors.joining("\n"));
			linesSetter.accept(joined.isEmpty() ? null : joined);
		}
		citySetter.accept(addr.getCity());
		districtSetter.accept(addr.getDistrict());
		stateSetter.accept(addr.getState());
		postalSetter.accept(addr.getPostalCode());
		countrySetter.accept(addr.getCountry());
	}
	
	private String formatAllAddressesSnapshot(List<Address> addresses) {
		if (addresses == null || addresses.isEmpty()) {
			return null;
		}
		List<String> blocks = new ArrayList<>();
		for (Address a : addresses) {
			StringBuilder sb = new StringBuilder();
			if (a.hasLine()) {
				for (StringType line : a.getLine()) {
					if (line != null && line.getValue() != null && !line.getValue().isEmpty()) {
						sb.append(line.getValue()).append('\n');
					}
				}
			}
			appendIfPresent(sb, "city", a.getCity());
			appendIfPresent(sb, "district", a.getDistrict());
			appendIfPresent(sb, "state", a.getState());
			appendIfPresent(sb, "postalCode", a.getPostalCode());
			appendIfPresent(sb, "country", a.getCountry());
			if (a.hasText()) {
				appendIfPresent(sb, "text", a.getText());
			}
			String block = sb.toString().trim();
			if (!block.isEmpty()) {
				blocks.add(block);
			}
		}
		return blocks.isEmpty() ? null : String.join("\n---\n", blocks);
	}
	
	private void appendIfPresent(StringBuilder sb, String label, String value) {
		if (value != null && !value.isEmpty()) {
			sb.append(label).append(": ").append(value).append("\n");
		}
	}
	
	/**
	 * Patient import fuzzy path: persists a duplicate-review case when at least one candidate
	 * Patient was returned from OpenMRS fuzzy match and/or FHIR MDM {@code $mdm-match}. Unlike
	 * {@link #persistIfAmbiguous(String, Patient, Bundle, String)}, this triggers on a single match
	 * or more.
	 */
	@Transactional
	public Optional<MpiPatientDuplicateReviewCase> persistImportFuzzyDuplicateIfMatches(String importCorrelationLocalKey,
	        Patient sourcePatient, String outboundImportPatientJson,
	        List<MpiDuplicateReviewCandidateMatchRow> mergedMatchRows, String searchAuditsJson) {
		if (importCorrelationLocalKey == null || importCorrelationLocalKey.isEmpty()) {
			LOGGER.warn("MPI import duplicate review: missing import correlation key; skipping persistence");
			return Optional.empty();
		}
		if (mergedMatchRows == null || mergedMatchRows.isEmpty()) {
			return Optional.empty();
		}
		Optional<MpiPatientDuplicateReviewCase> pendingExisting = caseRepository
		        .findFirstByLocalPatientUuidAndReviewStatusOrderByIdDesc(importCorrelationLocalKey,
		            MpiDuplicateReviewStatus.PENDING);
		if (pendingExisting.isPresent()) {
			LOGGER.info("MPI import duplicate review: reuse pending case_uuid={} for correlation {}", pendingExisting.get()
			        .getCaseUuid(), importCorrelationLocalKey);
			return pendingExisting;
		}
		MpiPatientDuplicateReviewCase reviewCase = new MpiPatientDuplicateReviewCase();
		reviewCase.setLocalPatientUuid(importCorrelationLocalKey);
		reviewCase.setCandidateCount(mergedMatchRows.size());
		reviewCase.setReviewStatus(MpiDuplicateReviewStatus.PENDING);
		reviewCase.setOutboundBundleJson(outboundImportPatientJson);
		reviewCase.setSearchBundleJson(searchAuditsJson);
		reviewCase.setSourceOfPatient(null);
		fillSourceDemographics(sourcePatient, reviewCase);
		fillSourceAddress(sourcePatient, reviewCase);
		int idx = 0;
		for (MpiDuplicateReviewCandidateMatchRow matchRow : mergedMatchRows) {
			Patient match = matchRow.getPatient();
			MpiPatientDuplicateReviewCandidate row = new MpiPatientDuplicateReviewCandidate();
			String logicalId = resolveImportCandidateLogicalId(match, idx);
			row.setFhirPatientLogicalId(logicalId);
			row.setMpiIdentifierValue(logicalId);
			fillCandidateDemographics(match, row);
			fillCandidateAddress(match, row);
			row.setPatientResourceJson(fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(match));
			row.setMatchScore(matchRow.getMatchScore());
			row.setMatchSource(matchRow.getMatchSource());
			reviewCase.addCandidate(row);
			idx++;
		}
		MpiPatientDuplicateReviewCase saved = caseRepository.saveAndFlush(reviewCase);
		LOGGER.info("MPI import duplicate review: saved case_uuid={} correlation={} candidates={}", saved.getCaseUuid(),
		    importCorrelationLocalKey, saved.getCandidateCount());
		return Optional.of(saved);
	}
	
	private String resolveImportCandidateLogicalId(Patient patient, int fallbackIndex) {
		if (patient != null && patient.getIdElement() != null && patient.getIdElement().hasIdPart()) {
			String id = patient.getIdElement().getIdPart().trim();
			if (!id.isEmpty()) {
				return id.length() > 128 ? id.substring(0, 128) : id;
			}
		}
		if (patient != null && patient.hasIdentifier()) {
			for (Identifier identifier : patient.getIdentifier()) {
				if (identifier != null && identifier.getValue() != null && !identifier.getValue().trim().isEmpty()) {
					String v = identifier.getValue().trim();
					String candidate = "idval:" + v;
					return candidate.length() > 128 ? candidate.substring(0, 128) : candidate;
				}
			}
		}
		String synthetic = "import-candidate:" + fallbackIndex;
		return synthetic.length() > 128 ? synthetic.substring(0, 128) : synthetic;
	}
	
	private static Double bundleEntryMatchScore(BundleEntryComponent entry) {
		if (entry == null || !entry.hasSearch() || !entry.getSearch().hasScore()) {
			return null;
		}
		return entry.getSearch().getScore().doubleValue();
	}
	
	private List<BundleEntryComponent> extractPatientEntries(Bundle searchBundle) {
		List<BundleEntryComponent> out = new ArrayList<>();
		if (searchBundle == null || !searchBundle.hasEntry()) {
			return out;
		}
		for (BundleEntryComponent entry : searchBundle.getEntry()) {
			Resource resource = entry.hasResource() ? entry.getResource() : null;
			if (resource instanceof Patient) {
				out.add(entry);
			}
		}
		return out;
	}
	
	/**
	 * Resolves logical id from {@link Patient#getIdElement()} or
	 * {@link BundleEntryComponent#getFullUrl()}.
	 */
	private String resolveFhirPatientLogicalId(BundleEntryComponent entry, Patient patient) {
		if (patient != null && patient.getIdElement().hasIdPart()) {
			return patient.getIdElement().getIdPart();
		}
		String fullUrl = entry.getFullUrl();
		if (fullUrl == null || fullUrl.isEmpty()) {
			return "";
		}
		String marker = "/Patient/";
		int idx = fullUrl.indexOf(marker);
		if (idx < 0) {
			return "";
		}
		String tail = fullUrl.substring(idx + marker.length());
		int slash = tail.indexOf('/');
		if (slash >= 0) {
			tail = tail.substring(0, slash);
		}
		int query = tail.indexOf('?');
		if (query >= 0) {
			tail = tail.substring(0, query);
		}
		return tail;
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
