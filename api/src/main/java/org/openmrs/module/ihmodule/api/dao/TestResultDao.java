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

import org.hibernate.SessionFactory;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ihmodule.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class TestResultDao {
	
	@Autowired
	DbSessionFactory dsessionFactory;
	
	private DbSession getSession() {
		return dsessionFactory.getCurrentSession();
	}
	
	@SuppressWarnings("unchecked")
	public List<TestResult> findByPatientAllTestResult(String uuid) throws APIException {
		
		List<TestResult> persons = new ArrayList<TestResult>();
		String sql = "SELECT  cn.name test_name , CASE WHEN ob.value_numeric IS NOT NULL THEN CAST(ob.value_numeric AS CHAR(50) CHARACTER SET utf8)"
		        + "	WHEN ob.value_text IS NOT NULL THEN ob.value_text 	END as value, 	ob.date_created order_date,"
		        + "   o.date_created result_date, v.uuid  visit_uuid from orders o join concept_name cn on (o.concept_id = cn.concept_id "
		        + "  and cn.concept_name_type = 'FULLY_SPECIFIED'  and cn.locale_preferred =1 ) join encounter e on o.encounter_id =e.encounter_id"
		        + "  join visit v on e.visit_id =v.visit_id join person p on  v.patient_id =p.person_id left join obs ob on ob.encounter_id =o.encounter_id where  ob.value_complex is null "
		        + "  and o.voided =false and o.order_type_id =3 and  p.uuid = :uuid";
		System.out.println(sql);
		
		persons = dsessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("test_name", StandardBasicTypes.STRING)
		        .addScalar("value", StandardBasicTypes.STRING).addScalar("result_date", StandardBasicTypes.STRING)
		        .addScalar("order_date", StandardBasicTypes.STRING).addScalar("visit_uuid", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(TestResult.class)).setParameter("uuid", uuid).list();
		return persons;
		
	}
	
	@SuppressWarnings("unchecked")
	public List<TestResult> findByVisitUUidPatientTestResult(String uuid) throws APIException {
		
		List<TestResult> persons = new ArrayList<TestResult>();
		String sql = "SELECT  cn.name test_name , CASE WHEN ob.value_numeric IS NOT NULL THEN CAST(ob.value_numeric AS CHAR(50) CHARACTER SET utf8)"
		        + "	WHEN ob.value_text IS NOT NULL THEN ob.value_text 	END as value, 	ob.date_created order_date,"
		        + "   o.date_created result_date, v.uuid  visit_uuid from orders o join concept_name cn on o.concept_id = cn.concept_id "
		        + "  and cn.concept_name_type = 'FULLY_SPECIFIED' and cn.locale_preferred =1) join encounter e on (o.encounter_id =e.encounter_id"
		        + "  join visit v on e.visit_id =v.visit_id join person p on  v.patient_id =p.person_id left join obs ob on ob.encounter_id =o.encounter_id  where   ob.value_complex is null "
		        + "  and o.voided =false and o.order_type_id =3 and v.uuid = :uuid";
		System.out.println(sql);
		
		persons = dsessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("test_name", StandardBasicTypes.STRING)
		        .addScalar("value", StandardBasicTypes.STRING).addScalar("result_date", StandardBasicTypes.STRING)
		        .addScalar("order_date", StandardBasicTypes.STRING).addScalar("visit_uuid", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(TestResult.class)).setParameter("uuid", uuid).list();
		return persons;
		
	}
	
	@SuppressWarnings("unchecked")
	public List<TestResult> findByPatientAllTestDocument(String uuid) throws APIException {
		
		List<TestResult> persons = new ArrayList<TestResult>();
		String sql = "SELECT  cn.name test_name , value_complex  as value, 	ob.date_created order_date,"
		        + "   o.date_created result_date, v.uuid  visit_uuid ,interpretation title from orders o join concept_name cn on o.concept_id = cn.concept_id "
		        + "  and cn.concept_name_type = 'FULLY_SPECIFIED'  join encounter e on o.encounter_id =e.encounter_id"
		        + "  join visit v on e.visit_id =v.visit_id join person p on  v.patient_id =p.person_id  left join obs ob on ob.encounter_id =o.encounter_id where  ob.value_complex is not null "
		        + "  and p.uuid = :uuid";
		System.out.println(sql);
		
		persons = dsessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("test_name", StandardBasicTypes.STRING)
		        .addScalar("value", StandardBasicTypes.STRING).addScalar("result_date", StandardBasicTypes.STRING)
		        .addScalar("order_date", StandardBasicTypes.STRING).addScalar("visit_uuid", StandardBasicTypes.STRING)
		        .addScalar("title", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(TestResult.class)).setParameter("uuid", uuid).list();
		return persons;
		
	}
	
	@SuppressWarnings("unchecked")
	public List<TestResult> findByVisitUUidPatientTestDocument(String uuid) throws APIException {
		
		List<TestResult> persons = new ArrayList<TestResult>();
		String sql = " SELECT  cn.name test_name , value_complex  as value, 	ob.date_created order_date,"
		        + "   o.date_created result_date, v.uuid  visit_uuid,interpretation title from orders o join concept_name cn on o.concept_id = cn.concept_id "
		        + "  and cn.concept_name_type = 'FULLY_SPECIFIED'  join encounter e on o.encounter_id =e.encounter_id"
		        + "  join visit v on e.visit_id =v.visit_id join person p on  v.patient_id =p.person_id  left join obs ob on ob.encounter_id =o.encounter_id where   ob.value_complex is not null "
		        + "  and v.uuid = :uuid";
		System.out.println(sql);
		
		persons = dsessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("test_name", StandardBasicTypes.STRING)
		        .addScalar("value", StandardBasicTypes.STRING).addScalar("result_date", StandardBasicTypes.STRING)
		        .addScalar("order_date", StandardBasicTypes.STRING).addScalar("visit_uuid", StandardBasicTypes.STRING)
		        .addScalar("title", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(TestResult.class)).setParameter("uuid", uuid).list();
		return persons;
		
	}
	
}
