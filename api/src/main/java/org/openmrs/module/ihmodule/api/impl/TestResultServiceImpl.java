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

import org.openmrs.api.APIException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.TestResult;
import org.openmrs.module.ihmodule.api.TestResultService;
import org.openmrs.module.ihmodule.api.dao.TestResultDao;

public class TestResultServiceImpl extends BaseOpenmrsService implements TestResultService {
	
	TestResultDao dao;
	
	public void setDao(TestResultDao dao) {
		this.dao = dao;
	}
	
	@Override
	public List<TestResult> findByPatientAllTestResult(String uuid) throws APIException {
		
		return dao.findByPatientAllTestResult(uuid);
	}
	
	@Override
	public List<TestResult> findByVisitUUidPatientTestResult(String uuid) throws APIException {
		
		return dao.findByVisitUUidPatientTestResult(uuid);
	}
	
	@Override
	public List<TestResult> findByPatientAllTestDocument(String uuid) throws APIException {
		
		return dao.findByPatientAllTestDocument(uuid);
	}
	
	@Override
	public List<TestResult> findByVisitUUidPatientTestDocument(String uuid) throws APIException {
		
		return dao.findByVisitUUidPatientTestDocument(uuid);
	}
	
}
