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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ihmodule.DataMigration;

public class DataMigrationDao {
	
	DbSessionFactory sessionFactory = Context.getRegisteredComponent("dbSessionFactory", DbSessionFactory.class);
	
	/*private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}*/
	
	@SuppressWarnings("unchecked")
	public List<DataMigration> getMedicineData(String date, Integer concept) throws APIException {
		//163202
		List<DataMigration> persons = new ArrayList<DataMigration>();
		String sql = " SELECT p.uuid person,e.encounter_datetime  enccounterdate ,v.uuid visit,o.value_text value,l.uuid location from obs o "
		        + "  join person p on o.person_id =p.person_id join encounter e on e.encounter_id =o.encounter_id join visit v on  v.visit_id =e.visit_id "
		        + " join location l on l.location_id =v.location_id  where concept_id = :concept and value_text like '%:%'";
		System.out.println(sql);
		
		persons = sessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("person", StandardBasicTypes.STRING)
		        .addScalar("enccounterdate", StandardBasicTypes.STRING).addScalar("visit", StandardBasicTypes.STRING)
		        .addScalar("value", StandardBasicTypes.STRING).addScalar("location", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(DataMigration.class))
		        .setParameter("concept", concept).list();
		return persons;
		
	}
	
}
