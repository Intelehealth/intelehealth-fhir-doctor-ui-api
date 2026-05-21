package org.openmrs.module.ihmodule.api.patientmatch.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticAlgorithm;
import org.openmrs.module.ihmodule.api.patientmatch.phonetic.PhoneticEncodingService;

public class FuzzyTextUtilsTest {
	
	private final PhoneticEncodingService phoneticEncodingService = new PhoneticEncodingService();
	
	@Test
	public void deprecatedPhoneticMatch_delegatesToDoubleMetaphone() {
		assertTrue(FuzzyTextUtils.phoneticMatch("Smith", "Smyth"));
		assertTrue(phoneticEncodingService.phoneticMatch("Smith", "Smyth", PhoneticAlgorithm.DOUBLE_METAPHONE));
	}
	
	@Test
	public void doubleMetaphoneMatch_rejectsUnrelatedNames() {
		assertFalse(FuzzyTextUtils.doubleMetaphoneMatch("Smith", "Jones"));
	}
}
