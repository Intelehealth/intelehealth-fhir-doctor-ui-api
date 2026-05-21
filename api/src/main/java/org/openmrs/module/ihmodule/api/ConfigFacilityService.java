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

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.ihmodule.ConfigFacility;
import org.openmrs.module.ihmodule.dto.ConfigFacilityDTO;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface ConfigFacilityService extends OpenmrsService {
	
	ConfigFacilityDTO save(ConfigFacility entity) throws APIException;
	
	List<ConfigFacilityDTO> getAll() throws APIException;
	
	ConfigFacilityDTO getById(Integer id) throws APIException, Exception;
	
}
