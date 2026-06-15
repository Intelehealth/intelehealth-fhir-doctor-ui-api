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
import org.openmrs.module.ihmodule.ConfigFacility;
import org.openmrs.module.ihmodule.api.ConfigFacilityService;
import org.openmrs.module.ihmodule.api.dao.ConfigFacilityDao;
import org.openmrs.module.ihmodule.dto.ConfigFacilityDTO;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class ConfigFacilityServiceImpl extends BaseOpenmrsService implements ConfigFacilityService {
	
	ConfigFacilityDao dao;
	
	PlatformTransactionManager transactionManager;
	
	public void setDao(ConfigFacilityDao dao) {
		this.dao = dao;
	}
	
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	@Override
	public ConfigFacilityDTO save(ConfigFacility entity) throws APIException {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.setReadOnly(false);
		return txTemplate.execute(status -> doSave(entity));
	}
	
	private ConfigFacilityDTO doSave(ConfigFacility entity) throws APIException {
		User user = requireAdminUser();
		Date now = new Date();
		if (StringUtils.isBlank(entity.getUuid())) {
			entity.setUuid(UUID.randomUUID().toString());
		}
		
		ConfigFacility toSave = entity;
		if (StringUtils.isNotBlank(entity.getFacilityUuid())) {
			ConfigFacility existing = dao.getByFacilityUuid(entity.getFacilityUuid());
			if (existing != null) {
				applyUpdates(existing, entity);
				existing.setChangedBy(user);
				existing.setDateChanged(now);
				toSave = existing;
			}
		}
		
		if (toSave.getId() == null) {
			toSave.setCreator(user);
			toSave.setDateCreated(now);
			toSave.setVoided(false);
		} else if (toSave.getChangedBy() == null) {
			toSave.setChangedBy(user);
			toSave.setDateChanged(now);
		}
		
		ConfigFacility saved = dao.save(toSave);
		Context.flushSession();
		if (saved == null || saved.getId() == null) {
			return null;
		}
		return mapToDto(dao.getById(saved.getId()));
	}
	
	@Override
	public List<ConfigFacilityDTO> getAll() throws APIException {
		Context.clearSession();
		List<ConfigFacility> items = dao.getAll();
		List<ConfigFacilityDTO> list = new ArrayList<>();
		for (ConfigFacility conf : items) {
			list.add(mapToDto(conf));
		}
		return list;
	}
	
	@Override
	public ConfigFacilityDTO getById(Integer id) throws APIException {
		Context.clearSession();
		ConfigFacility entity = dao.getById(id);
		if (entity == null || entity.isVoided()) {
			throw new APIException("Invalid response found for id " + id);
		}
		return mapToDto(entity);
	}
	
	private User requireAdminUser() throws APIException {
		User user = Context.getUserService().getUserByUsername("admin");
		if (user == null) {
			throw new APIException("Unable to resolve admin user for audit fields");
		}
		return user;
	}
	
	private void applyUpdates(ConfigFacility target, ConfigFacility source) {
		target.setFacilityName(source.getFacilityName());
		target.setPrescriptionApi(source.getPrescriptionApi());
		target.setReferralApi(source.getReferralApi());
		target.setLabApi(source.getLabApi());
		target.setAppointmentApi(source.getAppointmentApi());
		target.setStatus(source.isStatus());
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
