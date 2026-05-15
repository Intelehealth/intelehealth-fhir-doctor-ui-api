package org.openmrs.module.ihmodule.api.patientexchange.importupload;

import java.util.ArrayList;
import java.util.List;

public class PatientUploadImportResponse {

	private int total;
	private int created;
	private int skipped;
	private int failed;
	private int duplicateReviewDeferred;
	private List<PatientUploadImportItemResult> items = new ArrayList<>();

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getCreated() {
		return created;
	}

	public void setCreated(int created) {
		this.created = created;
	}

	public int getSkipped() {
		return skipped;
	}

	public void setSkipped(int skipped) {
		this.skipped = skipped;
	}

	public int getFailed() {
		return failed;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}
	
	public int getDuplicateReviewDeferred() {
		return duplicateReviewDeferred;
	}
	
	public void setDuplicateReviewDeferred(int duplicateReviewDeferred) {
		this.duplicateReviewDeferred = duplicateReviewDeferred;
	}

	public List<PatientUploadImportItemResult> getItems() {
		return items;
	}

	public void setItems(List<PatientUploadImportItemResult> items) {
		this.items = items;
	}
}
