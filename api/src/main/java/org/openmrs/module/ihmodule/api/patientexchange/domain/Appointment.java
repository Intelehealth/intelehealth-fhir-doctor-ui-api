package org.openmrs.module.ihmodule.api.patientexchange.domain;

import java.util.Date;

public class Appointment {
	
	private Long id;
	
	private String slotDay;
	
	private String slotDate;
	
	private Integer slotDuration;
	
	private String slotDurationUnit;
	
	private String slotTime;
	
	private String speciality;
	
	private String userUuid;
	
	private String drName;
	
	private String locationUuid;
	
	private String hwUUID;
	
	private String visitUuid;
	
	private String patientName;
	
	private String openMrsId;
	
	private String patientId;
	
	private String status;
	
	private String slotJsDate;
	
	private Date updatedAt;
	
	private String createdBy;
	
	private String uuid;
	
	private String age;
	
	private String gender;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getSlotDay() {
		return slotDay;
	}
	
	public void setSlotDay(String slotDay) {
		this.slotDay = slotDay;
	}
	
	public String getSlotDate() {
		return slotDate;
	}
	
	public void setSlotDate(String slotDate) {
		this.slotDate = slotDate;
	}
	
	public Integer getSlotDuration() {
		return slotDuration;
	}
	
	public void setSlotDuration(Integer slotDuration) {
		this.slotDuration = slotDuration;
	}
	
	public String getSlotDurationUnit() {
		return slotDurationUnit;
	}
	
	public void setSlotDurationUnit(String slotDurationUnit) {
		this.slotDurationUnit = slotDurationUnit;
	}
	
	public String getSlotTime() {
		return slotTime;
	}
	
	public void setSlotTime(String slotTime) {
		this.slotTime = slotTime;
	}
	
	public String getSpeciality() {
		return speciality;
	}
	
	public void setSpeciality(String speciality) {
		this.speciality = speciality;
	}
	
	public String getUserUuid() {
		return userUuid;
	}
	
	public void setUserUuid(String userUuid) {
		this.userUuid = userUuid;
	}
	
	public String getDrName() {
		return drName;
	}
	
	public void setDrName(String drName) {
		this.drName = drName;
	}
	
	public String getLocationUuid() {
		return locationUuid;
	}
	
	public void setLocationUuid(String locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	public String getHwUUID() {
		return hwUUID;
	}
	
	public void setHwUUID(String hwUUID) {
		this.hwUUID = hwUUID;
	}
	
	public String getVisitUuid() {
		return visitUuid;
	}
	
	public void setVisitUuid(String visitUuid) {
		this.visitUuid = visitUuid;
	}
	
	public String getPatientName() {
		return patientName;
	}
	
	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}
	
	public String getOpenMrsId() {
		return openMrsId;
	}
	
	public void setOpenMrsId(String openMrsId) {
		this.openMrsId = openMrsId;
	}
	
	public String getPatientId() {
		return patientId;
	}
	
	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public String getSlotJsDate() {
		return slotJsDate;
	}
	
	public void setSlotJsDate(String slotJsDate) {
		this.slotJsDate = slotJsDate;
	}
	
	public Date getUpdatedAt() {
		return updatedAt;
	}
	
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public String getCreatedBy() {
		return createdBy;
	}
	
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getAge() {
		return age;
	}
	
	public void setAge(String age) {
		this.age = age;
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	@Override
	public String toString() {
		return "Appointment [id=" + id + ", slotDay=" + slotDay + ", slotDate=" + slotDate + ", slotDuration="
		        + slotDuration + ", slotDurationUnit=" + slotDurationUnit + ", slotTime=" + slotTime + ", speciality="
		        + speciality + ", userUuid=" + userUuid + ", drName=" + drName + ", locationUuid=" + locationUuid
		        + ", hwUUID=" + hwUUID + ", visitUuid=" + visitUuid + ", patientName=" + patientName + ", openMrsId="
		        + openMrsId + ", patientId=" + patientId + ", status=" + status + ", slotJsDate=" + slotJsDate
		        + ", updatedAt=" + updatedAt + ", createdBy=" + createdBy + ", uuid=" + uuid + ", getId()=" + getId()
		        + ", getSlotDay()=" + getSlotDay() + ", getSlotDate()=" + getSlotDate() + ", getSlotDuration()="
		        + getSlotDuration() + ", getSlotDurationUnit()=" + getSlotDurationUnit() + ", getSlotTime()="
		        + getSlotTime() + ", getSpeciality()=" + getSpeciality() + ", getUserUuid()=" + getUserUuid()
		        + ", getDrName()=" + getDrName() + ", getLocationUuid()=" + getLocationUuid() + ", getHwUUID()="
		        + getHwUUID() + ", getVisitUuid()=" + getVisitUuid() + ", getPatientName()=" + getPatientName()
		        + ", getOpenMrsId()=" + getOpenMrsId() + ", getPatientId()=" + getPatientId() + ", getStatus()="
		        + getStatus() + ", getSlotJsDate()=" + getSlotJsDate() + ", getUpdatedAt()=" + getUpdatedAt()
		        + ", getCreatedBy()=" + getCreatedBy() + ", getUuid()=" + getUuid() + ", getClass()=" + getClass()
		        + ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + "]";
	}
	
}
