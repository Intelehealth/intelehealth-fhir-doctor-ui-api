package org.openmrs.module.ihmodule.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrescriptionShareDTO implements Serializable {
	
	private Integer id;
	
	private String uuid;
	
	private String patientUuid;
	
	private String visitUuid;
	
	private List<String> locationUuids = new ArrayList<>();
	
	private Date dateCreated;
	
	private String creatorUuid;
	
	private Date dateChanged;
	
	private String changedByUuid;
	
	private boolean voided;
	
	private Date dateVoided;
	
	private String voidedByUuid;
	
	private String voidReason;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getPatientUuid() {
		return patientUuid;
	}
	
	public void setPatientUuid(String patientUuid) {
		this.patientUuid = patientUuid;
	}
	
	public String getVisitUuid() {
		return visitUuid;
	}
	
	public void setVisitUuid(String visitUuid) {
		this.visitUuid = visitUuid;
	}
	
	public List<String> getLocationUuids() {
		return locationUuids;
	}
	
	public void setLocationUuids(List<String> locationUuids) {
		this.locationUuids = locationUuids;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public String getCreatorUuid() {
		return creatorUuid;
	}
	
	public void setCreatorUuid(String creatorUuid) {
		this.creatorUuid = creatorUuid;
	}
	
	public Date getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(Date dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	public String getChangedByUuid() {
		return changedByUuid;
	}
	
	public void setChangedByUuid(String changedByUuid) {
		this.changedByUuid = changedByUuid;
	}
	
	public boolean isVoided() {
		return voided;
	}
	
	public void setVoided(boolean voided) {
		this.voided = voided;
	}
	
	public Date getDateVoided() {
		return dateVoided;
	}
	
	public void setDateVoided(Date dateVoided) {
		this.dateVoided = dateVoided;
	}
	
	public String getVoidedByUuid() {
		return voidedByUuid;
	}
	
	public void setVoidedByUuid(String voidedByUuid) {
		this.voidedByUuid = voidedByUuid;
	}
	
	public String getVoidReason() {
		return voidReason;
	}
	
	public void setVoidReason(String voidReason) {
		this.voidReason = voidReason;
	}
	
}
