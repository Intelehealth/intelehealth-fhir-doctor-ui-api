/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ihmodule.api.dao;

import java.io.Serializable;
import java.util.List;

import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ihmodule.ConfigDataSyncModule;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigDataSyncModuleDao {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	@SuppressWarnings("unchecked")
	public ConfigDataSyncModule save(ConfigDataSyncModule entity) throws APIException {
		DbSession session = sessionFactory.getCurrentSession();
		Serializable id = null;
		if (entity.getId() == null) {
			id = session.save(entity);
		} else {
			session.saveOrUpdate(entity);
			id = entity.getId();
		}
		if (id != null) {
			ConfigDataSyncModule saved = (ConfigDataSyncModule) session.get(ConfigDataSyncModule.class, id);
			return saved;
		}
		return null;
		
	}
	
	@SuppressWarnings("unchecked")
	public List<ConfigDataSyncModule> getAll() throws APIException {
		
		List<ConfigDataSyncModule> all = sessionFactory.getCurrentSession().createQuery(" from ConfigDataSyncModule").list();
		return all;
		
	}
	
	@SuppressWarnings("unchecked")
	public ConfigDataSyncModule getById(Integer id) throws APIException {
		
		return (ConfigDataSyncModule) sessionFactory.getCurrentSession()
		        .createQuery(" from ConfigDataSyncModule where id = :id ").setInteger("id", id).uniqueResult();
		
	}
	
}
