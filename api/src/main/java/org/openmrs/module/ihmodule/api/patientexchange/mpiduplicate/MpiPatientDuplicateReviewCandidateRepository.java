package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.List;

import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class MpiPatientDuplicateReviewCandidateRepository {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	@SuppressWarnings("unchecked")
	public List<MpiPatientDuplicateReviewCandidate> findByReviewCase_IdOrderByIdAsc(Long reviewCaseId) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from MpiPatientDuplicateReviewCandidate where reviewCase.id = :reviewCaseId order by id asc")
		        .setParameter("reviewCaseId", reviewCaseId).list();
	}
}
