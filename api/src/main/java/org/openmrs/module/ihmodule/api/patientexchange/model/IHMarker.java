package org.openmrs.module.ihmodule.api.patientexchange.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.stereotype.Service;

@Service
public class IHMarker {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	
	private String name;
	
	private String lastSyncTime;
	
	/**
	 * High-water mark for {@code unsync_patient.id} replay (marker {@code UNSYNCED_PATIENT}).
	 */
	private Integer lastId;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getLastSyncTime() {
		return lastSyncTime;
	}
	
	public void setLastSyncTime(String lastSyncTime) {
		this.lastSyncTime = lastSyncTime;
	}
	
	public Integer getLastId() {
		return lastId;
	}
	
	public void setLastId(Integer lastId) {
		this.lastId = lastId;
	}
	
	@Override
	public String toString() {
		return "IHMarker [id=" + id + ", name=" + name + ", lastSyncTime=" + lastSyncTime + ", lastId=" + lastId
		        + ", getId()=" + getId() + ", getName()=" + getName() + ", getLastSyncTime()=" + getLastSyncTime()
		        + ", getClass()=" + getClass() + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}
}
