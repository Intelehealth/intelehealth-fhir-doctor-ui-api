package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Type;

/**
 * Reads FHIR {@code Bundle.entry.search.extension} match-grade ({@code valueCode}) from MDM /
 * $match responses.
 */
public final class BundleSearchMatchGradeExtractor {
	
	public static final String MATCH_GRADE_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/match-grade";
	
	private BundleSearchMatchGradeExtractor() {
	}
	
	/**
	 * @return trimmed {@code valueCode} for the match-grade extension, or {@code null} if absent
	 */
	public static String extractMatchGradeCode(BundleEntryComponent entry) {
		if (entry == null || !entry.hasSearch() || !entry.getSearch().hasExtension()) {
			return null;
		}
		for (Extension ext : entry.getSearch().getExtension()) {
			if (ext == null || !ext.hasUrl()) {
				continue;
			}
			if (!MATCH_GRADE_EXTENSION_URL.equals(ext.getUrl())) {
				continue;
			}
			if (!ext.hasValue()) {
				return null;
			}
			Type v = ext.getValue();
			if (v instanceof CodeType) {
				String code = ((CodeType) v).getCode();
				return StringUtils.isNotBlank(code) ? code.trim() : null;
			}
			if (v != null && v.isPrimitive()) {
				String p = v.primitiveValue();
				return StringUtils.isNotBlank(p) ? p.trim() : null;
			}
		}
		return null;
	}
}
