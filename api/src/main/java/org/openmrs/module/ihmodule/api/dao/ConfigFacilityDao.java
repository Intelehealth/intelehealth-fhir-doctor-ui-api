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
import org.openmrs.module.ihmodule.ConfigFacility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ConfigFacilityDao {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	@SuppressWarnings("unchecked")
	@Transactional
	public ConfigFacility save(ConfigFacility entity) throws APIException {
		DbSession session = sessionFactory.getCurrentSession();
		Serializable id = null;
		if (entity.getId() == null) {
			id = session.save(entity);
		} else {
			session.saveOrUpdate(entity);
			id = entity.getId();
		}
		
		if (id != null) {
			ConfigFacility saved = (ConfigFacility) session.get(ConfigFacility.class, id);
			return saved;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public List<ConfigFacility> getAll() throws APIException {
		
		List<ConfigFacility> all = sessionFactory.getCurrentSession().createQuery(" from ConfigFacility").list();
		return all;
		
	}
	
	@SuppressWarnings("unchecked")
	public ConfigFacility getById(Integer id) throws APIException {
		return (ConfigFacility) sessionFactory.getCurrentSession().createQuery(" from ConfigFacility where id = :id ")
		        .setInteger("id", id).uniqueResult();
	}
	
}
