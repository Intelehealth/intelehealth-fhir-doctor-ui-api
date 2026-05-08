package org.openmrs.module.ihmodule.api.patientexchange.repository;

import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils;
import org.springframework.stereotype.Repository;

@Repository
public class IHMarkerRepository {
	
	private SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
	
	public IHMarker save(IHMarker marker) {
		sessionFactory.getCurrentSession().saveOrUpdate(marker);
		return marker;
	}
	
	public IHMarker findByName(String name) {
		return (IHMarker) sessionFactory.getCurrentSession().createQuery("from IHMarker where name = :name")
		        .setParameter("name", name).uniqueResult();
	}
	
	public int updateLastSyncTimeByName(String name) {
		return sessionFactory.getCurrentSession()
		        .createQuery("update IHMarker set lastSyncTime = :lastSyncTime where name = :name")
		        .setParameter("lastSyncTime", DateUtils.toFormattedDateNow("yyyy-MM-dd HH:mm:ss"))
		        .setParameter("name", name).executeUpdate();
	}
}
