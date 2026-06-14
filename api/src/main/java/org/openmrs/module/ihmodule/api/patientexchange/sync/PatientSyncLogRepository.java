package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PatientSyncLogRepository {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	public void save(PatientSyncLog row) {
		sessionFactory.getCurrentSession().saveOrUpdate(row);
	}
	
	public PatientSyncLog findById(Long id) {
		if (id == null) {
			return null;
		}
		return (PatientSyncLog) sessionFactory.getCurrentSession().get(PatientSyncLog.class, id);
	}
	
	public int nextAttemptNumberForPatient(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return 1;
		}
		String sql = "SELECT COALESCE(MAX(attempt_number), 0) + 1 FROM patient_sync_log WHERE patient_uuid = :patientUuid";
		Number result = (Number) sessionFactory.getCurrentSession().createSQLQuery(sql)
		        .setString("patientUuid", patientUuid.trim()).uniqueResult();
		return result == null ? 1 : result.intValue();
	}
	
	@SuppressWarnings("unchecked")
	public PatientSyncLog findLatestByPatientUuid(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return null;
		}
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "from PatientSyncLog l where l.patientUuid = :patientUuid order by l.attemptNumber desc, l.id desc");
		query.setParameter("patientUuid", patientUuid.trim());
		query.setMaxResults(1);
		List<PatientSyncLog> rows = query.list();
		return rows == null || rows.isEmpty() ? null : rows.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public List<PatientSyncLog> findPendingAwaitingPush(int limit) {
		String hql = "from PatientSyncLog l where l.status = :status and l.completedAt is null "
		        + "and l.httpStatusCode is null order by l.startedAt asc, l.id asc";
		Query query = sessionFactory.getCurrentSession().createQuery(hql);
		query.setParameter("status", PatientSyncLogStatus.PENDING.name());
		query.setMaxResults(Math.max(1, limit));
		return query.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<PatientSyncLog> findFailedDueForRetry(int limit) {
		String hql = "from PatientSyncLog l where l.status = :status and l.nextRetryAt is not null "
		        + "and l.nextRetryAt <= :now order by l.nextRetryAt asc, l.id asc";
		Query query = sessionFactory.getCurrentSession().createQuery(hql);
		query.setParameter("status", PatientSyncLogStatus.FAILED.name());
		query.setTimestamp("now", new Date());
		query.setMaxResults(Math.max(1, limit));
		return query.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<PatientSyncLog> findFailedPermanentEligibleForRetry(int limit) {
		String hql = "from PatientSyncLog l where l.status = :status and l.attemptNumber < :maxAttempts "
		        + "order by l.completedAt asc, l.id asc";
		Query query = sessionFactory.getCurrentSession().createQuery(hql);
		query.setParameter("status", PatientSyncLogStatus.FAILED_PERMANENT.name());
		query.setParameter("maxAttempts", PatientSyncRetryPolicy.MAX_ATTEMPTS);
		query.setMaxResults(Math.max(1, limit));
		return query.list();
	}
	
	@SuppressWarnings("unchecked")
	public List<PatientSyncLog> findDueForRetry(int limit) {
		Set<Long> seenIds = new LinkedHashSet<Long>();
		List<PatientSyncLog> due = new ArrayList<PatientSyncLog>();
		int remaining = Math.max(1, limit);
		for (PatientSyncLog row : findPendingAwaitingPush(remaining)) {
			if (seenIds.add(row.getId())) {
				due.add(row);
			}
		}
		remaining = Math.max(0, limit - due.size());
		if (remaining > 0) {
			for (PatientSyncLog row : findFailedDueForRetry(remaining)) {
				if (seenIds.add(row.getId())) {
					due.add(row);
				}
			}
		}
		remaining = Math.max(0, limit - due.size());
		if (remaining > 0) {
			for (PatientSyncLog row : findFailedPermanentEligibleForRetry(remaining)) {
				if (seenIds.add(row.getId())) {
					due.add(row);
				}
			}
		}
		return due;
	}
}
