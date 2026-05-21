package org.openmrs.module.ihmodule.api.patientexchange.repository;

import org.openmrs.module.ihmodule.api.patientexchange.model.IHMarker;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ihmodule.api.patientexchange.utils.DateUtils;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class IHMarkerRepository {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
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
