package org.openmrs.module.ihmodule.api.patientexchange.utils;

public enum AppointmentStatus {
	booked("Scheduled"), proposed("Requested"), arrived("CheckedIn"), fulfilled("Completed"), cancelled("Completed"), pending(
	        "Requested");
	
	public final String label;
	
	private AppointmentStatus(String label) {
		this.label = label;
	}
	
	public static AppointmentStatus valueOfLabel(String label) {
		for (AppointmentStatus e : values()) {
			if (e.label.equals(label)) {
				return e;
			}
		}
		return null;
	}
	
}
