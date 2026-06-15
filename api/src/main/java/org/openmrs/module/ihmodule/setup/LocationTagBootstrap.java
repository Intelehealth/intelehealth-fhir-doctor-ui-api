package org.openmrs.module.ihmodule.setup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.LocationTag;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;

/**
 * Ensures the Facility {@link LocationTag} required by ihmodule exists after install. Idempotent:
 * creates only when the configured UUID is absent.
 */
public final class LocationTagBootstrap {
	
	private static final Log LOG = LogFactory.getLog(LocationTagBootstrap.class);
	
	public static final String FACILITY_TAG_NAME = "Facility";
	
	public static final String FACILITY_TAG_UUID = "43605a1a-e1a7-40da-9e71-f97fe5883593";
	
	private LocationTagBootstrap() {
	}
	
	public static void ensureFacilityLocationTag() {
		if (!Context.isSessionOpen()) {
			LOG.warn("ihmodule location tag bootstrap skipped: no OpenMRS session");
			return;
		}
		LocationService locationService = Context.getLocationService();
		if (locationService == null) {
			LOG.warn("ihmodule location tag bootstrap skipped: LocationService not available");
			return;
		}
		ensureTag(locationService, FACILITY_TAG_UUID, FACILITY_TAG_NAME, "Location tag used to identify facility locations");
	}
	
	private static void ensureTag(LocationService locationService, String uuid, String name, String description) {
		LocationTag existing = locationService.getLocationTagByUuid(uuid);
		if (existing != null) {
			if (Boolean.TRUE.equals(existing.getRetired())) {
				LOG.warn("ihmodule location tag uuid=" + uuid + " name=" + name
				        + " exists but is retired; create manually or un-retire in OpenMRS admin");
			} else {
				LOG.info("ihmodule location tag already present: " + name + " (" + uuid + ")");
			}
			return;
		}
		LocationTag byName = locationService.getLocationTagByName(name);
		if (byName != null && !uuid.equals(byName.getUuid())) {
			LOG.error("ihmodule location tag bootstrap: name '" + name + "' already used by another tag uuid="
			        + byName.getUuid() + "; expected uuid=" + uuid + " — not creating a duplicate");
			return;
		}
		LocationTag tag = new LocationTag();
		tag.setUuid(uuid);
		tag.setName(name);
		tag.setDescription(description);
		locationService.saveLocationTag(tag);
		LOG.info("ihmodule created location tag: " + name + " uuid=" + uuid);
	}
}
