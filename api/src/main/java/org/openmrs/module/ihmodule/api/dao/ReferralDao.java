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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ihmodule.Referral;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferralDao {
	
	DbSessionFactory sessionFactory;
	
	@SuppressWarnings("unchecked")
	public List<Referral> findAllByPatient(String uuid) throws APIException {
		
		List<Referral> persons = new ArrayList<Referral>();
		String sql = " SELECT p.uuid  patientUuid , p.person_id personId ,CONCAT(pn.given_name,' ',pn.family_name) patientName  , p.birthdate dob ,p.gender ,o.value_text referText,o.date_created referDate  from obs o join person p  on o.person_id =p.person_id "
		        + " join person_name pn on pn.person_id =o.person_id and pn.preferred =true "
		        + " join concept_name cn on cn.concept_id = o.concept_id and cn.locale_preferred =true and cn.concept_name_type ='FULLY_SPECIFIED'"
		        + " where cn.name  ='Referral' and p.uuid=:uuid and o.voided =false";
		System.out.println(sql);
		
		persons = sessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("patientUuid", StandardBasicTypes.STRING)
		        .addScalar("personId", StandardBasicTypes.INTEGER).addScalar("patientName", StandardBasicTypes.STRING)
		        .addScalar("dob", StandardBasicTypes.STRING).addScalar("gender", StandardBasicTypes.STRING)
		        .addScalar("referText", StandardBasicTypes.STRING).addScalar("referDate", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(Referral.class)).setParameter("uuid", uuid).list();
		return persons;
		
	}
	
	@SuppressWarnings("unchecked")
	public List<Referral> search(Map<String, String> reqParam) throws APIException {
		String gender = reqParam.get("gender");
		String startDate = reqParam.get("startDate");
		String endDate = reqParam.get("endDate");
		String name = reqParam.get("name");
		
		List<Referral> persons = new ArrayList<Referral>();
		
		String sql = " SELECT p.uuid  patientUuid , p.person_id personId ,CONCAT(pn.given_name,' ',pn.family_name) patientName  , p.birthdate dob ,p.gender ,o.value_text referText,o.date_created referDate  from obs o join person p  on o.person_id =p.person_id "
		        + " join person_name pn on pn.person_id =o.person_id and pn.preferred =true "
		        + " join concept_name cn on cn.concept_id = o.concept_id and cn.locale_preferred =true and cn.concept_name_type ='FULLY_SPECIFIED'"
		        + " where  " + generateWhereCondition(reqParam);
		System.out.println(sql);
		
		persons = sessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("patientUuid", StandardBasicTypes.STRING)
		        .addScalar("personId", StandardBasicTypes.INTEGER).addScalar("patientName", StandardBasicTypes.STRING)
		        .addScalar("dob", StandardBasicTypes.STRING).addScalar("gender", StandardBasicTypes.STRING)
		        .addScalar("referText", StandardBasicTypes.STRING).addScalar("referDate", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(Referral.class)).setParameter("gender", gender)
		        .setParameter("name", "%" + name + "%").setParameter("start", startDate).setParameter("end", endDate).list();
		return persons;
		
	}
	
	private String generateWhereCondition(Map<String, String> reqParam) {
		String gender = reqParam.get("gender");
		String startDate = reqParam.get("startDate");
		String endDate = reqParam.get("endDate");
		String name = reqParam.get("name");
		StringBuilder sb = new StringBuilder();
		
		if (!StringUtils.isBlank(gender)) {
			sb.append(" gender = :gender");
		} else {
			sb.append(" ''= :gender");
		}
		
		if (!StringUtils.isBlank(name)) {
			sb.append(" and ( pn.given_name ilike :name or pn.family_name ilike :name )");
		} else {
			sb.append(" and '' = :name");
		}
		
		if (!StringUtils.isBlank(startDate)) {
			sb.append(" and DATE(o.date_created) between :start and :end ");
		}
		System.err.println("sb.toString():::" + sb.toString());
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	public List<Referral> sync(String date) throws APIException {
		
		List<Referral> persons = new ArrayList<Referral>();
		String sql = " SELECT p.uuid  patientUuid , p.person_id personId ,CONCAT(pn.given_name,' ',pn.family_name) patientName  , p.birthdate dob ,p.gender ,o.value_text referText,o.date_created referDate  from obs o join person p  on o.person_id =p.person_id "
		        + " join person_name pn on pn.person_id =o.person_id and pn.preferred =true "
		        + " join concept_name cn on cn.concept_id = o.concept_id and cn.locale_preferred =true and cn.concept_name_type ='FULLY_SPECIFIED'"
		        + " where cn.name  ='Referral' and o.date_created>:date and o.voided =false order by o.date_created asc ";
		System.out.println(sql);
		
		persons = sessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("patientUuid", StandardBasicTypes.STRING)
		        .addScalar("personId", StandardBasicTypes.INTEGER).addScalar("patientName", StandardBasicTypes.STRING)
		        .addScalar("dob", StandardBasicTypes.STRING).addScalar("gender", StandardBasicTypes.STRING)
		        .addScalar("referText", StandardBasicTypes.STRING).addScalar("referDate", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(Referral.class)).setParameter("date", date).list();
		return persons;
		
	}
}
