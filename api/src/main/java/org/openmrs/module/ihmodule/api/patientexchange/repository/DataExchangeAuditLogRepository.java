package org.openmrs.module.ihmodule.api.patientexchange.repository;

import org.openmrs.module.ihmodule.api.patientexchange.model.DataExchangeAuditLog;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class DataExchangeAuditLogRepository {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataExchangeAuditLogRepository.class);
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	public DataExchangeAuditLog save(DataExchangeAuditLog auditLog) {
		try {
			// Explicit entity name matches patientDataExchangeDataExchangeAuditLog.hbm.xml (avoids
			// "Unknown entity" when the session factory resolves mappings by entity-name).
			sessionFactory.getCurrentSession().saveOrUpdate("PatientExchangeDataExchangeAuditLog", auditLog);
		}
		catch (Exception ex) {
			// In some deployments this entity mapping is not available; do not break sync flow for audit failure.
			LOGGER.warn("Skipping data_exchange_auditlog persistence due to mapping/runtime issue: {}", ex.getMessage());
		}
		return auditLog;
	}
}
