package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Persists validation outcomes. Kept in this package so core exchange code stays unchanged; callers
 * (e.g. AOP) live here.
 */
@Service
public class FhirResourceValidationRecordService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirResourceValidationRecordService.class);
	
	private static final int MAX_MESSAGE = 4000;
	
	private static final int MAX_PAYLOAD = 200000;
	
	private static final java.util.concurrent.atomic.AtomicLong RECORD_ID_SEQUENCE = new java.util.concurrent.atomic.AtomicLong(
	        System.currentTimeMillis());
	
	@Autowired
	private FhirResourceValidationRecordRepository repository;
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(String resourceType, Bundle bundle, ValidationOutcome outcome, String message) {
		recordValues(resourceType, firstResourceLogicalId(bundle), outcome, message, null);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordValues(String resourceType, String resourceLogicalId, ValidationOutcome outcome, String message,
	        String payloadJson) {
		try {
			FhirResourceValidationRecord row = new FhirResourceValidationRecord();
			row.setId(nextRecordId());
			row.setResourceType(StringUtils.isNotBlank(resourceType) ? resourceType : "Unknown");
			row.setResourceLogicalId(resourceLogicalId);
			row.setOutcome(outcome != null ? outcome.name() : ValidationOutcome.PROCESSING_ERROR.name());
			row.setMessage(truncate(message));
			row.setPayloadJson(truncate(payloadJson, MAX_PAYLOAD));
			repository.save(row);
		}
		catch (Exception ex) {
			// Validation audit persistence must not break force-sync/export pipelines.
			LOGGER.error("Unable to persist validation record. resourceType={}, logicalId={}, outcome={}, message={}",
			    resourceType, resourceLogicalId, outcome, ex.getMessage(), ex);
		}
	}
	
	private static String firstResourceLogicalId(Bundle bundle) {
		if (bundle == null || !bundle.hasEntry()) {
			return null;
		}
		for (Bundle.BundleEntryComponent e : bundle.getEntry()) {
			Resource r = e.getResource();
			if (r != null) {
				String id = r.getIdElement().getIdPart();
				if (StringUtils.isNotBlank(id)) {
					return id;
				}
			}
		}
		return null;
	}
	
	private static String truncate(String m) {
		return truncate(m, MAX_MESSAGE);
	}
	
	private static String truncate(String m, int maxLen) {
		if (m == null) {
			return null;
		}
		if (m.length() <= maxLen) {
			return m;
		}
		return m.substring(0, maxLen) + "...";
	}
	
	private static long nextRecordId() {
		return RECORD_ID_SEQUENCE.incrementAndGet();
	}
}
