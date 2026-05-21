package org.openmrs.module.ihmodule.api.patientexchange.dto;

public class Person {
	
	private String identifier;
	
	private String age;
	
	private String gender;
	
	public String getIdentifier() {
		return identifier;
	}
	
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
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
		return "Person [identifier=" + identifier + ", age=" + age + ", gender=" + gender + "]";
	}
	
}
