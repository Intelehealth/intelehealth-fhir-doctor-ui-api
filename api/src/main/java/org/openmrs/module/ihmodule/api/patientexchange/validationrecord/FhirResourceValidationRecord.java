package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils;

/**
 * Immutable audit row for a FHIR validation attempt. {@code resourceType} is the FHIR type name
 * (e.g. Patient) so additional resource types can be tracked without a new table.
 */
public class FhirResourceValidationRecord {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(name = "resource_type", nullable = false, length = 64)
	private String resourceType;
	
	/**
	 * Logical id of the resource in the local bundle (e.g. OpenMRS / FHIR2 id part), when known.
	 */
	@Column(name = "resource_logical_id", length = 256)
	private String resourceLogicalId;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false, length = 32)
	private ValidationOutcome outcome;
	
	@Lob
	@Column(name = "message", columnDefinition = "TEXT")
	private String message;
	
	@Lob
	@Column(name = "payload_json", columnDefinition = "LONGTEXT")
	private String payloadJson;
	
	@Column(name = "date_created", length = 32)
	private String dateCreated = DateUtils.toFormattedDateNow();
	
	@Column(name = "record_uuid", length = 64, nullable = false)
	private String recordUuid = UUID.randomUUID().toString();
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getResourceType() {
		return resourceType;
	}
	
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	
	public String getResourceLogicalId() {
		return resourceLogicalId;
	}
	
	public void setResourceLogicalId(String resourceLogicalId) {
		this.resourceLogicalId = resourceLogicalId;
	}
	
	public ValidationOutcome getOutcome() {
		return outcome;
	}
	
	public void setOutcome(ValidationOutcome outcome) {
		this.outcome = outcome;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getPayloadJson() {
		return payloadJson;
	}
	
	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}
	
	public String getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public String getRecordUuid() {
		return recordUuid;
	}
	
	public void setRecordUuid(String recordUuid) {
		this.recordUuid = recordUuid;
	}
}
