package org.openmrs.module.ihmodule.api.patientexchange.repository;

import org.openmrs.module.ihmodule.api.patientexchange.model.DataExchangeAuditLog;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Repository;

@Repository
public class DataExchangeAuditLogRepository {
	
	private SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
	
	public DataExchangeAuditLog save(DataExchangeAuditLog auditLog) {
		sessionFactory.getCurrentSession().saveOrUpdate(auditLog);
		return auditLog;
	}
}
