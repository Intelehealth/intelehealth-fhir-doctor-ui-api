/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ihmodule.api.impl;

import java.util.List;
import java.util.Map;

import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.Referral;
import org.openmrs.module.ihmodule.api.ReferralService;
import org.openmrs.module.ihmodule.api.dao.ReferralDao;

public class ReferralServiceImpl extends BaseOpenmrsService implements ReferralService {
	
	ReferralDao dao;
	
	public void setDao(ReferralDao dao) {
		this.dao = dao;
	}
	
	@Override
	public List<Referral> findAllByPatient(String uuid) throws APIException {
		
		return dao.findAllByPatient(uuid);
	}
	
	@Override
	public List<Referral> search(Map<String, String> reqParam) throws APIException {
		
		return dao.search(reqParam);
	}
	
	@Override
	public List<Referral> sync(String date) throws APIException {
		// TODO Auto-generated method stub
		return dao.sync(date);
	}
	
}
