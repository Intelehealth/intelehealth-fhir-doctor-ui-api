package org.openmrs.module.ihmodule.api.patientmatch.mapper;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Patient;

/**
 * Keeps fuzzy-match {@link Address#line} at exactly two entries (index 0 = address1, index 1 =
 * address6).
 */
final class FuzzyMatchAddressLineSupport {
	
	private FuzzyMatchAddressLineSupport() {
	}
	
	static String normalizeAddressLineValue(String value) {
		String line = StringUtils.trimToNull(value);
		if (line == null || isPlaceholderAddressToken(line)) {
			return "";
		}
		return line;
	}
	
	static boolean isPlaceholderAddressToken(String value) {
		if (StringUtils.isBlank(value)) {
			return true;
		}
		String trimmed = value.trim();
		return "NA".equalsIgnoreCase(trimmed) || "-".equals(trimmed) || "null".equalsIgnoreCase(trimmed);
	}
	
	static void ensureTwoAddressLines(Address address) {
		if (address == null) {
			return;
		}
		String line0 = address.hasLine() && address.getLine().size() > 0 ? normalizeAddressLineValue(address.getLine()
		        .get(0).getValue()) : "";
		String line1 = address.hasLine() && address.getLine().size() > 1 ? normalizeAddressLineValue(address.getLine()
		        .get(1).getValue()) : "";
		address.getLine().clear();
		address.addLine(line0);
		address.addLine(line1);
	}
	
	static void normalizePatientAddressLines(Patient patient) {
		if (patient == null || !patient.hasAddress()) {
			return;
		}
		for (Address address : patient.getAddress()) {
			ensureTwoAddressLines(address);
		}
	}
}
