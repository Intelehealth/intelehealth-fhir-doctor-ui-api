package org.openmrs.module.ihmodule.api.patientexchange.datatype;

public enum QueryTable {
	
	ENCOUNTER("encounter"), ENCOUNTER_PK("encounter_id"), VISIT("visit"), VISIT_PK("visit_id"), ORDERS("orders"), ORDERS_PK(
	        "order_id"), OBS("obs"), OBS_PK("obs_id"), PERSON("person"), PERSON_PK("person_id");
	
	public final String value;
	
	private QueryTable(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return this.value;
	}
	
}
