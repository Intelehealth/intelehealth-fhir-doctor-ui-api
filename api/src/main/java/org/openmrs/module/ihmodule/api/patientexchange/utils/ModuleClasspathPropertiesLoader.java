package org.openmrs.module.ihmodule.api.patientexchange.utils;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads {@code ihmodule.properties} and {@code patientdataexchange-application.properties} from the
 * classpath and merges them in order (later files override keys from earlier ones). This replaces
 * "first file wins" behaviour so keys present only in the second file are visible at runtime.
 */
public final class ModuleClasspathPropertiesLoader {
	
	private static final Logger log = LoggerFactory.getLogger(ModuleClasspathPropertiesLoader.class);
	
	private ModuleClasspathPropertiesLoader() {
	}
	
	/**
	 * @return merged properties, or {@code null} if no resource could be loaded
	 */
	public static Properties loadMergedInOrder(String... resourceNames) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Properties merged = new Properties();
		boolean any = false;
		for (String resource : resourceNames) {
			if (StringUtils.isBlank(resource)) {
				continue;
			}
			try (InputStream in = cl.getResourceAsStream(resource)) {
				if (in == null) {
					continue;
				}
				Properties fragment = new Properties();
				fragment.load(in);
				merged.putAll(fragment);
				any = true;
				log.info("Merged module classpath properties from '{}'", resource);
			}
			catch (Exception ex) {
				log.warn("Unable to load module classpath properties '{}': {}", resource, ex.getMessage());
			}
		}
		return any ? merged : null;
	}
}
