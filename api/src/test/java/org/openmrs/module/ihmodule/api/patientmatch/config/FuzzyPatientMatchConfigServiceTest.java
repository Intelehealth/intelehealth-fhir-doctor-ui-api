package org.openmrs.module.ihmodule.api.patientmatch.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class FuzzyPatientMatchConfigServiceTest {
	
	@Test
	public void getConfig_shouldLoadBundledDefaults() {
		FuzzyPatientMatchConfig config = new FuzzyPatientMatchConfigService().getConfig();
		
		assertTrue(config.isEnabled());
		assertTrue(config.getThreshold() > 0);
		assertTrue(config.getMaxCandidates() >= 50);
		assertTrue(config.isFieldEnabled("name"));
		assertTrue(config.getFieldWeight("name") > 0.0d);
		assertEquals(95, config.getCertainMatchThreshold());
		assertEquals(80, config.getProbableMatchThreshold());
		assertEquals(60, config.getPossibleMatchThreshold());
		assertEquals(0, config.getDobNearMatchDays());
		assertEquals(DobRepositoryFilterMode.ONLY_DOB_REQUESTS, config.getDobRepositoryFilterMode());
		assertTrue(config.isCandidateSearchParamEnabled("birthdate"));
		assertTrue(config.isCandidateSearchParamEnabled("identifier"));
		assertTrue(config.getRules().getMatchResultMap().containsKey("national-id-exact"));
		assertTrue(config.getRules().getMatchResultMap()
		        .containsKey("given-name-jw,family-name-jw,birthdate-exact,phone-exact"));
		assertEquals(false, config.getRules().getMatchResultMap().containsKey("identifier,birthday"));
	}
	
	@Test
	public void getConfig_shouldAllowExternalRulesOverride() throws Exception {
		File tempFile = File.createTempFile("patient-match-rules", ".json");
		tempFile.deleteOnExit();
		Files.write(tempFile.toPath(), ("{" + "\"settings\":{\"dobRepositoryFilterMode\":\"NEVER\"},"
		        + "\"candidateSearchParams\":[{\"resourceType\":\"Patient\",\"searchParams\":[\"identifier\"]}],"
		        + "\"matchFields\":[{\"name\":\"name\",\"enabled\":true,\"weight\":0.60,"
		        + "\"similarity\":{\"algorithm\":\"JARO_WINKLER\"}}]," + "\"matchResultMap\":{\"certain\":0.99,"
		        + "\"probable\":0.88,\"possible\":0.55}" + "}").getBytes(StandardCharsets.UTF_8));
		
		FuzzyPatientMatchConfig config = new TestableConfigService(tempFile.getAbsolutePath()).getConfig();
		
		assertEquals(DobRepositoryFilterMode.NEVER, config.getDobRepositoryFilterMode());
		assertTrue(config.isCandidateSearchParamEnabled("identifier"));
		assertEquals(false, config.isCandidateSearchParamEnabled("birthdate"));
		assertEquals(99, config.getCertainMatchThreshold());
		assertEquals(88, config.getProbableMatchThreshold());
		assertEquals(55, config.getPossibleMatchThreshold());
	}
	
	private static final class TestableConfigService extends FuzzyPatientMatchConfigService {
		
		private final String rulesPath;
		
		private TestableConfigService(String rulesPath) {
			this.rulesPath = rulesPath;
		}
		
		@Override
		protected String resolveString(String key, String defaultValue) {
			if (GP_RULES_FILE.equals(key)) {
				return rulesPath;
			}
			return super.resolveString(key, defaultValue);
		}
	}
}
