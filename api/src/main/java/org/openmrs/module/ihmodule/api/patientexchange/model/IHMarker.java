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
	
	@Override
	public String toString() {
		return "IHMarker [id=" + id + ", name=" + name + ", lastSyncTime=" + lastSyncTime + ", getId()=" + getId()
		        + ", getName()=" + getName() + ", getLastSyncTime()=" + getLastSyncTime() + ", getClass()=" + getClass()
		        + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}
}
