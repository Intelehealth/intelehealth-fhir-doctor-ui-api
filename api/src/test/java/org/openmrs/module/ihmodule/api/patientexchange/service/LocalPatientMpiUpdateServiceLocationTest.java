package org.openmrs.module.ihmodule.api.patientexchange.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.User;

/**
 * Unit tests for preferred OpenMRS ID location alignment (no OpenMRS context required).
 */
public class LocalPatientMpiUpdateServiceLocationTest {
	
	@Test
	public void applyResolvedLocation_insert_alwaysSetsLocation() {
		PatientIdentifier identifier = new PatientIdentifier();
		Location facility = location("facility-loc");
		LocalPatientMpiUpdateService.IdentifierLocationResolution resolution = new LocalPatientMpiUpdateService.IdentifierLocationResolution(
		        facility, true, false);
		LocalPatientMpiUpdateService.applyResolvedLocationForTest(identifier, resolution, false);
		assertEquals(facility, identifier.getLocation());
	}
	
	@Test
	public void applyResolvedLocation_update_alignsWhenFromPreferredOpenMrsId() {
		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setLocation(location("old-default-loc"));
		Location facility = location("facility-loc");
		LocalPatientMpiUpdateService.IdentifierLocationResolution resolution = new LocalPatientMpiUpdateService.IdentifierLocationResolution(
		        facility, true, false);
		LocalPatientMpiUpdateService.applyResolvedLocationForTest(identifier, resolution, true);
		assertEquals(facility, identifier.getLocation());
	}
	
	@Test
	public void applyResolvedLocation_update_keepsExistingWhenOnlyDefaultFallback() {
		PatientIdentifier identifier = new PatientIdentifier();
		Location existing = location("existing-loc");
		identifier.setLocation(existing);
		Location defaultLoc = location("unknown-loc");
		LocalPatientMpiUpdateService.IdentifierLocationResolution resolution = new LocalPatientMpiUpdateService.IdentifierLocationResolution(
		        defaultLoc, false, false);
		LocalPatientMpiUpdateService.applyResolvedLocationForTest(identifier, resolution, true);
		assertEquals(existing, identifier.getLocation());
	}
	
	@Test
	public void applyResolvedLocation_update_backfillsNullFromDefault() {
		PatientIdentifier identifier = new PatientIdentifier();
		Location defaultLoc = location("unknown-loc");
		LocalPatientMpiUpdateService.IdentifierLocationResolution resolution = new LocalPatientMpiUpdateService.IdentifierLocationResolution(
		        defaultLoc, false, false);
		LocalPatientMpiUpdateService.applyResolvedLocationForTest(identifier, resolution, true);
		assertEquals(defaultLoc, identifier.getLocation());
	}
	
	@Test
	public void applyResolvedCreator_insert_alwaysSetsCreator() {
		PatientIdentifier identifier = new PatientIdentifier();
		User creator = user("creator-uuid");
		LocalPatientMpiUpdateService.applyResolvedCreatorForTest(identifier, creator, false, true);
		assertEquals(creator, identifier.getCreator());
	}
	
	@Test
	public void applyResolvedCreator_update_alignsWhenFromPreferredOpenMrsId() {
		PatientIdentifier identifier = new PatientIdentifier();
		identifier.setCreator(user("old-creator"));
		User creator = user("openmrs-id-creator");
		LocalPatientMpiUpdateService.applyResolvedCreatorForTest(identifier, creator, true, true);
		assertEquals(creator, identifier.getCreator());
	}
	
	@Test
	public void applyResolvedCreator_update_keepsExistingWhenNotFromPreferredOpenMrsId() {
		PatientIdentifier identifier = new PatientIdentifier();
		User existing = user("existing-creator");
		identifier.setCreator(existing);
		User creator = user("other-creator");
		LocalPatientMpiUpdateService.applyResolvedCreatorForTest(identifier, creator, true, false);
		assertEquals(existing, identifier.getCreator());
	}
	
	@Test
	public void applyResolvedCreator_update_backfillsNullCreator() {
		PatientIdentifier identifier = new PatientIdentifier();
		User creator = user("openmrs-id-creator");
		LocalPatientMpiUpdateService.applyResolvedCreatorForTest(identifier, creator, true, false);
		assertEquals(creator, identifier.getCreator());
	}
	
	@Test
	public void requiresExplicitLocationUuid_falseWhenPreferredOpenMrsIdHasLocation() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PatientIdentifierType type = new PatientIdentifierType();
		type.setName("OpenMRS ID");
		PatientIdentifier openMrsId = new PatientIdentifier();
		openMrsId.setIdentifierType(type);
		openMrsId.setPreferred(true);
		openMrsId.setLocation(location("facility-loc"));
		patient.addIdentifier(openMrsId);
		LocalPatientMpiUpdateService service = new LocalPatientMpiUpdateService();
		assertFalse(service.requiresExplicitLocationUuidForNewSourceIdentifierForTest(patient));
	}
	
	private static Location location(String uuid) {
		Location location = new Location();
		location.setUuid(uuid);
		return location;
	}
	
	private static User user(String uuid) {
		User user = new User();
		user.setUuid(uuid);
		return user;
	}
}
