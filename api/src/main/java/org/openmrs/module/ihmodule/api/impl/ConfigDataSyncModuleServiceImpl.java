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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.ConfigDataSyncModule;
import org.openmrs.module.ihmodule.api.ConfigDataSyncModuleService;
import org.openmrs.module.ihmodule.api.dao.ConfigDataSyncModuleDao;
import org.openmrs.module.ihmodule.dto.ConfigDataSyncModuleDTO;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class ConfigDataSyncModuleServiceImpl extends BaseOpenmrsService implements ConfigDataSyncModuleService {
	
	ConfigDataSyncModuleDao dao;
	
	PlatformTransactionManager transactionManager;
	
	public void setDao(ConfigDataSyncModuleDao dao) {
		this.dao = dao;
	}
	
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	@Override
	public ConfigDataSyncModuleDTO save(ConfigDataSyncModule entity) throws APIException {
		return createWriteTransactionTemplate().execute(status -> doSave(entity));
	}
	
	@Override
	public ConfigDataSyncModuleDTO changeStatus(ConfigDataSyncModule entity) throws APIException {
		return createWriteTransactionTemplate().execute(status -> {
			ConfigDataSyncModule existing = dao.getById(entity.getId());
			if (existing == null) {
				throw new APIException("Couldn't find config data sync entity using id: " + entity.getId());
			}
			User user = requireAdminUser();
			existing.setDateChanged(new Date());
			existing.setChangedBy(user);
			existing.setStatus(entity.isStatus());
			return doSave(existing);
		});
	}
	
	private ConfigDataSyncModuleDTO doSave(ConfigDataSyncModule entity) throws APIException {
		User user = requireAdminUser();
		Date now = new Date();
		if (StringUtils.isBlank(entity.getUuid())) {
			entity.setUuid(UUID.randomUUID().toString());
		}
		if (entity.getId() == null) {
			entity.setCreator(user);
			entity.setDateCreated(now);
			entity.setVoided(false);
		} else {
			entity.setChangedBy(user);
			entity.setDateChanged(now);
		}
		ConfigDataSyncModule saved = dao.save(entity);
		Context.flushSession();
		if (saved == null || saved.getId() == null) {
			throw new APIException("Couldn't save config data sync entity");
		}
		return mapToConfigDTO(dao.getById(saved.getId()));
	}
	
	@Override
	public List<ConfigDataSyncModuleDTO> getAll() throws APIException {
		Context.clearSession();
		ArrayList<ConfigDataSyncModuleDTO> items = new ArrayList<>();
		List<ConfigDataSyncModule> listItem = dao.getAll();
		for (ConfigDataSyncModule conf : listItem) {
			items.add(mapToConfigDTO(conf));
		}
		return items;
	}
	
	@Override
	public ConfigDataSyncModuleDTO getById(Integer id) throws APIException {
		Context.clearSession();
		ConfigDataSyncModule entity = dao.getById(id);
		if (entity == null || entity.isVoided()) {
			throw new APIException("Invalid entity found for id " + id);
		}
		return mapToConfigDTO(entity);
	}
	
	private TransactionTemplate createWriteTransactionTemplate() {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.setReadOnly(false);
		return txTemplate;
	}
	
	private User requireAdminUser() throws APIException {
		User user = Context.getUserService().getUserByUsername("admin");
		if (user == null) {
			throw new APIException("Unable to resolve admin user for audit fields");
		}
		return user;
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
