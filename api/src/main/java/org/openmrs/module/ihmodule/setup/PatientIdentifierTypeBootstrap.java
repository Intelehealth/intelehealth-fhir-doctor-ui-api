package org.openmrs.module.ihmodule.setup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;

/**
 * Ensures facility {@link PatientIdentifierType} rows required by ihmodule exist after install.
 * Idempotent: creates only when the configured UUID is absent.
 */
public final class PatientIdentifierTypeBootstrap {
	
	private static final Log LOG = LogFactory.getLog(PatientIdentifierTypeBootstrap.class);
	
	public static final String MPI_NAME = "MPI";
	
	public static final String MPI_UUID = "44f037f6-d9ca-4d44-88d6-2d9fcf26cb76";
	
	public static final String SOURCE_PATIENT_ID_NAME = "Source Patient Id";
	
	public static final String SOURCE_PATIENT_ID_UUID = "b2f192c2-346a-486c-bcb4-7a35616890ba";
	
	private PatientIdentifierTypeBootstrap() {
	}
	
	public static void ensureRequiredIdentifierTypes() {
		if (!Context.isSessionOpen()) {
			LOG.warn("ihmodule identifier bootstrap skipped: no OpenMRS session");
			return;
		}
		PatientService patientService = Context.getPatientService();
		if (patientService == null) {
			LOG.warn("ihmodule identifier bootstrap skipped: PatientService not available");
			return;
		}
		ensureType(patientService, MPI_UUID, MPI_NAME, "Master Patient Index (MPI) identifier for central sync");
		ensureType(patientService, SOURCE_PATIENT_ID_UUID, SOURCE_PATIENT_ID_NAME,
		    "Central FHIR Patient logical id (source patient routing)");
	}
	
	private static void ensureType(PatientService patientService, String uuid, String name, String description) {
		PatientIdentifierType existing = patientService.getPatientIdentifierTypeByUuid(uuid);
		if (existing != null) {
			if (Boolean.TRUE.equals(existing.getRetired())) {
				LOG.warn("ihmodule identifier type uuid=" + uuid + " name=" + name
				        + " exists but is retired; create manually or un-retire in OpenMRS admin");
			} else {
				LOG.info("ihmodule identifier type already present: " + name + " (" + uuid + ")");
			}
			return;
		}
		PatientIdentifierType byName = patientService.getPatientIdentifierTypeByName(name);
		if (byName != null && !uuid.equals(byName.getUuid())) {
			LOG.error("ihmodule identifier bootstrap: name '" + name + "' already used by another type uuid="
			        + byName.getUuid() + "; expected uuid=" + uuid + " — not creating a duplicate");
			return;
		}
		PatientIdentifierType type = new PatientIdentifierType();
		type.setUuid(uuid);
		type.setName(name);
		type.setDescription(description);
		type.setRequired(false);
		type.setUniquenessBehavior(PatientIdentifierType.UniquenessBehavior.NON_UNIQUE);
		patientService.savePatientIdentifierType(type);
		LOG.info("ihmodule created patient identifier type: " + name + " uuid=" + uuid + " (Non Unique)");
	}
}
