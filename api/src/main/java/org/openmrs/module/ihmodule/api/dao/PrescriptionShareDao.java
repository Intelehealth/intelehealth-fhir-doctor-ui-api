package org.openmrs.module.ihmodule.api.dao;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.api.APIException;
import org.openmrs.module.ihmodule.PrescriptionShare;

public class PrescriptionShareDao {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public PrescriptionShare save(PrescriptionShare share) throws APIException {
		Session session = sessionFactory.getCurrentSession();
		if (share.getId() == null) {
			session.save(share);
		} else {
			share = (PrescriptionShare) session.merge(share);
		}
		session.flush();
		session.refresh(share);
		return share;
	}
	
	public PrescriptionShare getByUuid(String uuid) throws APIException {
		return uniqueShareQuery("from PrescriptionShare ps where ps.uuid = :uuid", "uuid", uuid);
	}
	
	@SuppressWarnings("unchecked")
	public List<PrescriptionShare> getActiveByPatientAndVisit(String patientUuid, String visitUuid) throws APIException {
		return sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from PrescriptionShare ps where ps.patientUuid = :patientUuid and ps.visitUuid = :visitUuid and ps.voided = false")
		        .setParameter("patientUuid", patientUuid).setParameter("visitUuid", visitUuid).list();
	}
	
	@SuppressWarnings("unchecked")
	private PrescriptionShare uniqueShareQuery(String hql, String param, Object value) throws APIException {
		List<PrescriptionShare> results = sessionFactory.getCurrentSession().createQuery(hql).setParameter(param, value)
		        .list();
		return results.isEmpty() ? null : results.get(0);
	}
	
}
