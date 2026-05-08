package org.openmrs.module.ihmodule.api.patientexchange.api.dto;

/**
 * Aggregate counts from {@code mpi_patient_duplicate_review_case} by {@code review_status}.
 */
public class MpiDuplicateReviewStatisticsDto {
	
	private long totalCases;
	
	private long pendingCount;
	
	private long resolvedLinkExistingCount;
	
	private long resolvedForceSendCount;
	
	private long cancelledCount;
	
	public long getTotalCases() {
		return totalCases;
	}
	
	public void setTotalCases(long totalCases) {
		this.totalCases = totalCases;
	}
	
	public long getPendingCount() {
		return pendingCount;
	}
	
	public void setPendingCount(long pendingCount) {
		this.pendingCount = pendingCount;
	}
	
	public long getResolvedLinkExistingCount() {
		return resolvedLinkExistingCount;
	}
	
	public void setResolvedLinkExistingCount(long resolvedLinkExistingCount) {
		this.resolvedLinkExistingCount = resolvedLinkExistingCount;
	}
	
	public long getResolvedForceSendCount() {
		return resolvedForceSendCount;
	}
	
	public void setResolvedForceSendCount(long resolvedForceSendCount) {
		this.resolvedForceSendCount = resolvedForceSendCount;
	}
	
	public long getCancelledCount() {
		return cancelledCount;
	}
	
	public void setCancelledCount(long cancelledCount) {
		this.cancelledCount = cancelledCount;
	}
}
