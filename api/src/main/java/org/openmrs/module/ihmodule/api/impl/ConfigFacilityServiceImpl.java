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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.db.UserDAO;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.ConfigFacility;
import org.openmrs.module.ihmodule.api.ConfigFacilityService;
import org.openmrs.module.ihmodule.api.dao.ConfigFacilityDao;
import org.openmrs.module.ihmodule.dto.ConfigFacilityDTO;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigFacilityServiceImpl extends BaseOpenmrsService implements ConfigFacilityService {
	
	ConfigFacilityDao dao;
	
	@Autowired
	UserDAO userDao;
	
	public void setDao(ConfigFacilityDao dao) {
		this.dao = dao;
	}
	
	@Override
	public ConfigFacilityDTO save(ConfigFacility entity) throws APIException {
		User user = userDao.getUserByUsername("admin");
		entity.setCreator(user);
		entity.setDateCreated(new Date());
		entity.setVoided(false);
		ConfigFacility e = dao.save(entity);
		if (e == null)
			return null;
		return mapToDto(e);
		
	}
	
	@Override
	public List<ConfigFacilityDTO> getAll() throws APIException {
		List<ConfigFacility> items = dao.getAll();
		List<ConfigFacilityDTO> list = new ArrayList<>();
		for (ConfigFacility conf : items) {
			list.add(mapToDto(conf));
		}
		return list;
	}
	
	@Override
	public ConfigFacilityDTO getById(Integer id) throws APIException {
		
		ConfigFacility entity = dao.getById(id);
		
		if (entity == null)
			throw new APIException("Invalid response found for id " + id);
		
		return mapToDto(entity);
		
	}
	
	private ConfigFacilityDTO mapToDto(ConfigFacility conf) {
		ConfigFacilityDTO dto = new ConfigFacilityDTO();
		dto.setId(conf.getId());
		dto.setFacilityName(conf.getFacilityName());
		dto.setFacilityUuid(conf.getFacilityUuid());
		dto.setAppointmentApi(conf.getAppointmentApi());
		dto.setReferralApi(conf.getReferralApi());
		dto.setPrescriptionApi(conf.getPrescriptionApi());
		dto.setLabApi(conf.getLabApi());
		dto.setStatus(conf.isStatus());
		return dto;
	}
	
}
