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

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.api.APIException;
import org.openmrs.module.ihmodule.ConfigFacility;

public class ConfigFacilityDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public ConfigFacility save(ConfigFacility entity) throws APIException {
		Session session = sessionFactory.getCurrentSession();
		ConfigFacility persisted;
		if (entity.getId() == null) {
			session.save(entity);
			persisted = entity;
		} else {
			persisted = (ConfigFacility) session.merge(entity);
		}
		session.flush();
		session.refresh(persisted);
		return persisted;
	}
	
	@SuppressWarnings("unchecked")
	public List<ConfigFacility> getAll() throws APIException {
		return sessionFactory.getCurrentSession()
		        .createQuery("from ConfigFacility cf where cf.voided = false order by cf.id asc").list();
	}
	
	public ConfigFacility getById(Integer id) throws APIException {
		return (ConfigFacility) sessionFactory.getCurrentSession().get(ConfigFacility.class, id);
	}
	
	public ConfigFacility getByFacilityUuid(String facilityUuid) throws APIException {
		return (ConfigFacility) sessionFactory.getCurrentSession()
		        .createQuery("from ConfigFacility cf where cf.facilityUuid = :facilityUuid")
		        .setParameter("facilityUuid", facilityUuid).uniqueResult();
	}
	
}
