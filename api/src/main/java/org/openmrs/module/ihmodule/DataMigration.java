package org.openmrs.module.ihmodule;

public class DataMigration {
	
	private String visit;
	
	private String person;
	
	private String enccounterdate;
	
	private String location;
	
	private String value;
	
	public String getVisit() {
		return visit;
	}
	
	public void setVisit(String visit) {
		this.visit = visit;
	}
	
	public String getPerson() {
		return person;
	}
	
	public void setPerson(String person) {
		this.person = person;
	}
	
	public String getEnccounterdate() {
		return enccounterdate;
	}
	
	public void setEnccounterdate(String enccounterdate) {
		this.enccounterdate = enccounterdate;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "DataMigration [visit=" + visit + ", person=" + person + ", enccounterdate=" + enccounterdate + ", location="
		        + location + ", value=" + value + "]";
	}
	
}
