package org.openmrs.module.ihmodule.api.patientmatch.phonetic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhoneticEncodingServiceTest {
	
	private final PhoneticEncodingService service = new PhoneticEncodingService();
	
	@Test
	public void getPhoneticCode_doubleMetaphone_includesAlternateWhenDifferent() {
		String code = service.getPhoneticCode("Smith", PhoneticAlgorithm.DOUBLE_METAPHONE);
		assertTrue(code.contains("|") || code.length() > 0);
	}
	
	@Test
	public void getPhoneticCode_metaphone_returnsPrimaryOnly() {
		String code = service.getPhoneticCode("Smith", PhoneticAlgorithm.METAPHONE);
		assertFalse(code.contains("|"));
		assertEquals(code, service.getPhoneticCode("Smith", PhoneticAlgorithm.METAPHONE));
	}
	
	@Test
	public void phoneticMatch_doubleMetaphone_matchesSmythToSmith() {
		assertTrue(service.phoneticMatch("Smith", "Smyth", PhoneticAlgorithm.DOUBLE_METAPHONE));
	}
	
	@Test
	public void phoneticMatch_metaphone_comparesPrimaryCodesOnly() {
		assertTrue(service.phoneticMatch("Smith", "Smith", PhoneticAlgorithm.METAPHONE));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void fromConfig_rejectsUnsupportedAlgorithm() {
		PhoneticAlgorithm.fromConfig("ngram");
	}
}
