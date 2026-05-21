package org.openmrs.module.ihmodule.api.patientexchange.service;

import org.openmrs.module.ihmodule.api.patientexchange.model.DataExchangeAuditLog;
import org.openmrs.module.ihmodule.api.patientexchange.repository.DataExchangeAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DataExchangeAuditLogService {
	
	@Autowired
	private DataExchangeAuditLogRepository deAuditLogRepo;
	
	public DataExchangeAuditLog save(DataExchangeAuditLog auditLog) {
		DataExchangeAuditLog deLog = deAuditLogRepo.save(auditLog);
		return deLog;
	}
	
	public DataExchangeAuditLog update(DataExchangeAuditLog auditLog) {
		DataExchangeAuditLog deLog = deAuditLogRepo.save(auditLog);
		return deLog;
	}
}
