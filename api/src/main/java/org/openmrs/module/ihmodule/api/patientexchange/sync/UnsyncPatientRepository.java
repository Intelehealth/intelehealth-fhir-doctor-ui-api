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
	
	@SuppressWarnings("unchecked")
	public UnsyncPatient findLatestRetryableByPatientUuid(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return null;
		}
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "from UnsyncPatient u where u.patientUuid = :patientUuid and u.status in (:statuses) order by u.id desc");
		query.setParameter("patientUuid", patientUuid.trim());
		query.setParameterList("statuses",
		    new String[] { UnsyncPatientStatus.PENDING.name(), UnsyncPatientStatus.FAILED.name() });
		query.setMaxResults(1);
		return (UnsyncPatient) query.uniqueResult();
	}
	
	public void save(UnsyncPatient row) {
		sessionFactory.getCurrentSession().saveOrUpdate(row);
	}
	
	public void updateStatus(Long id, UnsyncPatientStatus status) {
		updateStatusAndErrorMessage(id, status, null);
	}
	
	public void updateStatusAndErrorMessage(Long id, UnsyncPatientStatus status, String errorMessage) {
		if (id == null || status == null) {
			return;
		}
		String hql = "update UnsyncPatient u set u.status = :status";
		if (errorMessage != null) {
			hql += ", u.errorMessage = :errorMessage";
		}
		hql += " where u.id = :id";
		Query query = sessionFactory.getCurrentSession().createQuery(hql);
		query.setParameter("status", status.name());
		query.setParameter("id", id);
		if (errorMessage != null) {
			query.setParameter("errorMessage", errorMessage);
		}
		query.executeUpdate();
	}
	
	@SuppressWarnings("unchecked")
	public List<UnsyncPatient> findPendingWhereIdGreaterThan(long lastId) {
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "from UnsyncPatient u where u.id > :lastId and u.status = :status order by u.id asc");
		query.setParameter("lastId", lastId);
		query.setParameter("status", UnsyncPatientStatus.PENDING.name());
		query.setMaxResults(100);
		return query.list();
	}
	
	public void delete(UnsyncPatient row) {
		if (row != null) {
			sessionFactory.getCurrentSession().delete(row);
		}
	}
}
