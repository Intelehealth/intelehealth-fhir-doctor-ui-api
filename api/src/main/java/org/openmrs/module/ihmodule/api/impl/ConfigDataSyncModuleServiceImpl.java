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
import org.openmrs.api.context.Context;
import org.openmrs.api.db.UserDAO;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.ConfigDataSyncModule;
import org.openmrs.module.ihmodule.api.ConfigDataSyncModuleService;
import org.openmrs.module.ihmodule.api.dao.ConfigDataSyncModuleDao;
import org.openmrs.module.ihmodule.dto.ConfigDataSyncModuleDTO;
import org.springframework.transaction.annotation.Transactional;

public class ConfigDataSyncModuleServiceImpl extends BaseOpenmrsService implements ConfigDataSyncModuleService {
	
	ConfigDataSyncModuleDao dao;
	
	UserDAO userDao = Context.getRegisteredComponent("userDAO", UserDAO.class);
	
	public void setDao(ConfigDataSyncModuleDao dao) {
		this.dao = dao;
	}
	
	@Override
	@Transactional
	public ConfigDataSyncModuleDTO save(ConfigDataSyncModule entity) throws APIException {
		User user = userDao.getUserByUsername("admin");
		entity.setCreator(user);
		entity.setDateCreated(new Date());
		entity.setVoided(false);
		ConfigDataSyncModule e = dao.save(entity);
		if (e != null) {
			return mapToConfigDTO(e);
		}
		throw new APIException("Couldn't find config data sync entity ");
		
	}
	
	@Override
	@Transactional
	public ConfigDataSyncModuleDTO changeStatus(ConfigDataSyncModule entity) throws APIException {
		ConfigDataSyncModule e = dao.getById(entity.getId());
		if (e == null)
			throw new APIException("Couldn't find config data sync entity using id: " + entity.getId());
		User user = userDao.getUserByUsername("admin");
		e.setDateChanged(new Date());
		e.setChangedBy(user);
		e.setStatus(entity.isStatus());
		return save(e);
	}
	
	@Override
	public List<ConfigDataSyncModuleDTO> getAll() throws APIException {
		ArrayList<ConfigDataSyncModuleDTO> items = new ArrayList<>();
		List<ConfigDataSyncModule> listItem = dao.getAll();
		for (ConfigDataSyncModule conf : listItem) {
			items.add(mapToConfigDTO(conf));
		}
		return items;
	}
	
	@Override
	public ConfigDataSyncModuleDTO getById(Integer id) throws APIException {
		ConfigDataSyncModule entity = dao.getById(id);
		if (entity == null)
			throw new APIException("Invalid entity found for id " + id);
		
		return mapToConfigDTO(entity);
	}
	
	private ConfigDataSyncModuleDTO mapToConfigDTO(ConfigDataSyncModule conf) {
		ConfigDataSyncModuleDTO dto = new ConfigDataSyncModuleDTO();
		dto.setId(conf.getId());
		dto.setName(conf.getName());
		dto.setStatus(conf.isStatus());
		dto.setProcessId(conf.getProcessId());
		return dto;
	}
	
}
