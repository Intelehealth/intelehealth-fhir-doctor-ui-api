package org.openmrs.module.ihmodule.api.patientexchange.sync;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UnsyncPatientRepository {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	public UnsyncPatient findByPatientUuid(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return null;
		}
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "from UnsyncPatient u where u.patientUuid = :patientUuid");
		query.setParameter("patientUuid", patientUuid.trim());
		query.setMaxResults(1);
		return (UnsyncPatient) query.uniqueResult();
	}
	
	public void save(UnsyncPatient row) {
		sessionFactory.getCurrentSession().saveOrUpdate(row);
	}
	
	@SuppressWarnings("unchecked")
	public List<UnsyncPatient> findWhereIdGreaterThan(long lastId) {
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "from UnsyncPatient u where u.id > :lastId order by u.id asc");
		query.setParameter("lastId", lastId);
		return query.list();
	}
	
	public void delete(UnsyncPatient row) {
		if (row != null) {
			sessionFactory.getCurrentSession().delete(row);
		}
	}
}
