package org.openmrs.module.ihmodule.api.patientmatch.dto;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;

public class FuzzyPatientMatchRequest {
	
	private String resourceType;
	
	private String identifierSystem;
	
	private String identifier;
	
	private String familyName;
	
	private String givenName;
	
	private String name;
	
	private LocalDate birthDate;
	
	private String phone;
	
	private String address;
	
	private String gender;
	
	private int count = 10;
	
	private int offset = 0;
	
	private boolean onlyCertainMatches;
	
	public String getResourceType() {
		return resourceType;
	}
	
	public void setResourceType(String resourceType) {
		this.resourceType = trim(resourceType);
	}
	
	public String getIdentifierSystem() {
		return identifierSystem;
	}
	
	public void setIdentifierSystem(String identifierSystem) {
		this.identifierSystem = trim(identifierSystem);
	}
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = trim(identifier);
	}
	
	public String getFamilyName() {
		return familyName;
	}
	
	public void setFamilyName(String familyName) {
		this.familyName = trim(familyName);
		rebuildNameFromParts();
	}
	
	public String getGivenName() {
		return givenName;
	}
	
	public void setGivenName(String givenName) {
		this.givenName = trim(givenName);
		rebuildNameFromParts();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = trim(name);
		if (this.name == null) {
			rebuildNameFromParts();
		}
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
	
	public boolean hasFamilyName() {
		return StringUtils.isNotBlank(familyName);
	}
	
	public boolean hasGivenName() {
		return StringUtils.isNotBlank(givenName);
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
	
	private void rebuildNameFromParts() {
		if (StringUtils.isNotBlank(name)) {
			return;
		}
		StringBuilder value = new StringBuilder();
		if (StringUtils.isNotBlank(givenName)) {
			value.append(givenName);
		}
		if (StringUtils.isNotBlank(familyName)) {
			if (value.length() > 0) {
				value.append(' ');
			}
			value.append(familyName);
		}
		name = value.length() > 0 ? value.toString() : null;
	}
}
