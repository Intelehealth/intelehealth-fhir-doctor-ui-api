package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class FhirResourceValidationRecordRepository {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	public FhirResourceValidationRecord save(FhirResourceValidationRecord row) {
		sessionFactory.getCurrentSession().saveOrUpdate(row);
		return row;
	}
}
