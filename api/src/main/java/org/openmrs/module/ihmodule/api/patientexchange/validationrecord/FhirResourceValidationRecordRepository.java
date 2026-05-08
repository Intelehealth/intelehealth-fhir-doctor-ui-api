package org.openmrs.module.ihmodule.api.patientexchange.validationrecord;

import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Repository;

@Repository
public class FhirResourceValidationRecordRepository {
	
	private SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
	
	public FhirResourceValidationRecord save(FhirResourceValidationRecord row) {
		sessionFactory.getCurrentSession().saveOrUpdate(row);
		return row;
	}
}
