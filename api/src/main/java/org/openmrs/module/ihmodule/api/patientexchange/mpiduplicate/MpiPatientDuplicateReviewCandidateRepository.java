package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.List;

import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Repository;

@Repository
public class MpiPatientDuplicateReviewCandidateRepository {
	
	private SessionFactory sessionFactory = Context.getRegisteredComponent("sessionFactory", SessionFactory.class);
	
	@SuppressWarnings("unchecked")
	public List<MpiPatientDuplicateReviewCandidate> findByReviewCase_IdOrderByIdAsc(Long reviewCaseId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from MpiPatientDuplicateReviewCandidate where reviewCase.id = :reviewCaseId order by id asc")
		        .setParameter("reviewCaseId", reviewCaseId).list();
	}
}
