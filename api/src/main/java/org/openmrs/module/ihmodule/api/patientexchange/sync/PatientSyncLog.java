package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * Row in {@code patient_sync_log}: one record per central FHIR patient push attempt.
 */
public class PatientSyncLog {
	
	@Id
	@Column(name = "id")
	private Long id;
	
	@Column(name = "patient_uuid", nullable = false, length = 38)
	private String patientUuid;
	
	@Column(name = "attempt_number", nullable = false)
	private int attemptNumber = 1;
	
	@Column(name = "status", nullable = false, length = 32)
	private String status = PatientSyncLogStatus.PENDING.name();
	
	@Column(name = "failure_reason", length = 512)
	private String failureReason;
	
	@Column(name = "http_status_code")
	private Integer httpStatusCode;
	
	@Column(name = "started_at")
	private Date startedAt;
	
	@Column(name = "completed_at")
	private Date completedAt;
	
	@Column(name = "next_retry_at")
	private Date nextRetryAt;
	
	@Column(name = "created_at")
	private Date createdAt;
	
	@Column(name = "updated_at")
	private Date updatedAt;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public int getAttemptNumber() {
		return attemptNumber;
	}
	
	public void setAttemptNumber(int attemptNumber) {
		this.attemptNumber = attemptNumber;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public PatientSyncLogStatus getStatusEnum() {
		if (status == null) {
			return PatientSyncLogStatus.PENDING;
		}
		if ("COMPLETED".equalsIgnoreCase(status)) {
			return PatientSyncLogStatus.SUCCESS;
		}
		try {
			return PatientSyncLogStatus.valueOf(status);
		}
		catch (IllegalArgumentException ex) {
			return PatientSyncLogStatus.PENDING;
		}
	}
	
	public void setStatusEnum(PatientSyncLogStatus statusEnum) {
		this.status = statusEnum != null ? statusEnum.name() : PatientSyncLogStatus.PENDING.name();
	}
	
	public String getFailureReason() {
		return failureReason;
	}
	
	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}
	
	public Integer getHttpStatusCode() {
		return httpStatusCode;
	}
	
	public void setHttpStatusCode(Integer httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}
	
	public Date getStartedAt() {
		return startedAt;
	}
	
	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}
	
	public Date getCompletedAt() {
		return completedAt;
	}
	
	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}
	
	public Date getNextRetryAt() {
		return nextRetryAt;
	}
	
	public void setNextRetryAt(Date nextRetryAt) {
		this.nextRetryAt = nextRetryAt;
	}
	
	public Date getCreatedAt() {
		return createdAt;
	}
	
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	
	public Date getUpdatedAt() {
		return updatedAt;
	}
	
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
}
