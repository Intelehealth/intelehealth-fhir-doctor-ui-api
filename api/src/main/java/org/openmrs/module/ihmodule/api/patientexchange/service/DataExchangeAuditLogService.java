package org.openmrs.module.ihmodule.api.patientexchange.service;

import org.openmrs.module.ihmodule.api.patientexchange.model.DataExchangeAuditLog;
import org.openmrs.module.ihmodule.api.patientexchange.repository.DataExchangeAuditLogRepository;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Service;

@Service
public class DataExchangeAuditLogService {
	
	private DataExchangeAuditLogRepository deAuditLogRepo = Context.getRegisteredComponent("dataExchangeAuditLogRepository",
	    DataExchangeAuditLogRepository.class);
	
	public DataExchangeAuditLog save(DataExchangeAuditLog auditLog) {
		DataExchangeAuditLog deLog = deAuditLogRepo.save(auditLog);
		return deLog;
	}
	
	public DataExchangeAuditLog update(DataExchangeAuditLog auditLog) {
		DataExchangeAuditLog deLog = deAuditLogRepo.save(auditLog);
		return deLog;
	}
}
