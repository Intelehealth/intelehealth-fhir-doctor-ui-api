package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

import java.util.List;
import java.util.Optional;

import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class MpiPatientDuplicateReviewCaseRepository {
	
	@Autowired
	private DbSessionFactory sessionFactory;
	
	public MpiPatientDuplicateReviewCase saveAndFlush(MpiPatientDuplicateReviewCase reviewCase) {
		sessionFactory.getCurrentSession().saveOrUpdate(reviewCase);
		sessionFactory.getCurrentSession().flush();
		return reviewCase;
	}
	
	public MpiPatientDuplicateReviewCase save(MpiPatientDuplicateReviewCase reviewCase) {
		sessionFactory.getCurrentSession().saveOrUpdate(reviewCase);
		return reviewCase;
	}
	
	public Optional<MpiPatientDuplicateReviewCase> findByCaseUuid(String caseUuid) {
		MpiPatientDuplicateReviewCase result = (MpiPatientDuplicateReviewCase) sessionFactory.getCurrentSession()
		        .createQuery("from MpiPatientDuplicateReviewCase where caseUuid = :caseUuid")
		        .setParameter("caseUuid", caseUuid).uniqueResult();
		return Optional.ofNullable(result);
	}
	
	@SuppressWarnings("unchecked")
	public List<MpiPatientDuplicateReviewCase> findByLocalPatientUuidOrderByDateCreatedDesc(String localPatientUuid) {
		return sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from MpiPatientDuplicateReviewCase where localPatientUuid = :localPatientUuid order by dateCreated desc")
		        .setParameter("localPatientUuid", localPatientUuid).list();
	}
	
	@SuppressWarnings("unchecked")
	public List<MpiPatientDuplicateReviewCase> findByReviewStatusOrderByDateCreatedDesc(MpiDuplicateReviewStatus status) {
		return sessionFactory.getCurrentSession()
		        .createQuery("from MpiPatientDuplicateReviewCase where reviewStatus = :status order by dateCreated desc")
		        .setParameter("status", status).list();
	}
	
	public Optional<MpiPatientDuplicateReviewCase> findFirstByLocalPatientUuidAndReviewStatusOrderByIdDesc(
	        String localPatientUuid, MpiDuplicateReviewStatus reviewStatus) {
		MpiPatientDuplicateReviewCase result = (MpiPatientDuplicateReviewCase) sessionFactory
		        .getCurrentSession()
		        .createQuery(
		            "from MpiPatientDuplicateReviewCase where localPatientUuid = :localPatientUuid and reviewStatus = :reviewStatus order by id desc")
		        .setParameter("localPatientUuid", localPatientUuid).setParameter("reviewStatus", reviewStatus)
		        .setMaxResults(1).uniqueResult();
		return Optional.ofNullable(result);
	}
	
	public long countByReviewStatus(MpiDuplicateReviewStatus reviewStatus) {
		Long count = (Long) sessionFactory.getCurrentSession()
		        .createQuery("select count(*) from MpiPatientDuplicateReviewCase where reviewStatus = :reviewStatus")
		        .setParameter("reviewStatus", reviewStatus).uniqueResult();
		return count == null ? 0L : count;
	}
	
	public long count() {
		Long count = (Long) sessionFactory.getCurrentSession()
		        .createQuery("select count(*) from MpiPatientDuplicateReviewCase").uniqueResult();
		return count == null ? 0L : count;
	}
}
