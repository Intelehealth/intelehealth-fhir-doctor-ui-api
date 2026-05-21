/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ihmodule.api;

import java.util.List;
import java.util.Map;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ihmodule.DataMigration;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface DataMigrationService extends OpenmrsService {
	
	List<DataMigration> getMedicineData(String date, Integer concept) throws APIException;
	
	List<DataMigration> getTestData(String date) throws APIException;
	
	String getMember(String resourceType, Map<String, String> reqParam) throws APIException, Exception;
	
	String importPatient(String uuid) throws APIException, Exception;
	
	String importPatient(String uuid, String location) throws APIException, Exception;
}
