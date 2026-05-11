package org.openmrs.module.ihmodule.api.patientmatch.dto;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;

public class FuzzyPatientMatchRequest {
	
	private String identifier;
	
	private String name;
	
	private LocalDate birthDate;
	
	private String phone;
	
	private String address;
	
	private String gender;
	
	private int count = 10;
	
	private int offset = 0;
	
	private boolean onlyCertainMatches;
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = trim(identifier);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = trim(name);
	}
	
	public LocalDate getBirthDate() {
		return birthDate;
	}
	
	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}
	
	public String getPhone() {
		return phone;
	}
	
	public void setPhone(String phone) {
		this.phone = trim(phone);
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = trim(address);
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = trim(gender);
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count < 1 ? 10 : Math.min(count, 100);
	}
	
	public int getOffset() {
		return offset;
	}
	
	public void setOffset(int offset) {
		this.offset = Math.max(0, offset);
	}
	
	public boolean isOnlyCertainMatches() {
		return onlyCertainMatches;
	}
	
	public void setOnlyCertainMatches(boolean onlyCertainMatches) {
		this.onlyCertainMatches = onlyCertainMatches;
	}
	
	public boolean hasIdentifier() {
		return StringUtils.isNotBlank(identifier);
	}
	
	public boolean hasName() {
		return StringUtils.isNotBlank(name);
	}
	
	public boolean hasBirthDate() {
		return birthDate != null;
	}
	
	public boolean hasPhone() {
		return StringUtils.isNotBlank(phone);
	}
	
	public boolean hasAddress() {
		return StringUtils.isNotBlank(address);
	}
	
	public boolean hasGender() {
		return StringUtils.isNotBlank(gender);
	}
	
	public boolean hasAnySearchField() {
		return hasIdentifier() || hasName() || hasBirthDate() || hasPhone() || hasAddress() || hasGender();
	}
	
	private String trim(String value) {
		return StringUtils.trimToNull(value);
	}
}
