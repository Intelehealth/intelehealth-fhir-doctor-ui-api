package org.openmrs.module.doctor.ui.api.api.dao;

import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.api.APIException;
import org.openmrs.api.db.hibernate.DbSession;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.doctor.ui.api.ExternalReferral;
import org.openmrs.module.doctor.ui.api.api.impl.exp.DuplicateEntryException;
import org.openmrs.module.doctor.ui.api.api.impl.exp.InvalidInputParamException;
import org.openmrs.module.doctor.ui.api.dto.ExternalReferralDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExternalReferralDao {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	@Transactional
	public ExternalReferral save(ExternalReferral referral) throws APIException, DuplicateEntryException {
		DbSession session = sessionFactory.getCurrentSession();
		
		Serializable id = null;
		
		List<ExternalReferralDTO> referralList = findReferralByPatientIdAndReferralId(referral.getPatientId(),
		    referral.getVisitId());
		
		if (referralList != null && !referralList.isEmpty()) {
			throw new DuplicateEntryException("Referral already exists with same patient & visit id "
			        + referral.getVisitId());
		}
		
		if (referral.getId() == null) {
			id = session.save(referral);
		} else {
			session.saveOrUpdate(referral);
			id = referral.getId();
		}
		if (id != null) {
			return (ExternalReferral) session.get(ExternalReferral.class, id);
		}
		return null;
		
	}
	
	public ExternalReferral getExternalReferralById(Integer extRefId) throws APIException {
		DbSession session = sessionFactory.getCurrentSession();
		return (ExternalReferral) session.get(ExternalReferral.class, extRefId);
	}
	
	public List<ExternalReferralDTO> findReferralByPatientIdAndReferralId(String patientId, String visitId)
	        throws APIException {
		
		List<ExternalReferralDTO> referralList = new ArrayList<ExternalReferralDTO>();
		
		String sql = "SELECT  id , facility_name facilityName ,facility_uuid facilityUuid , refer_to referTo,"
		        + " patient_name patientName,patient_id patientId, referred_by referredBy, referrer_id referrerId,"
		        + " priority_of_referral  priorityOfReferral, reason reason, visit_id visitId, uuid "
		        + " from external_referral where patient_id=:patientId and visit_id=:visitId";
		
		System.out.println(sql);
		
		referralList = sessionFactory.getCurrentSession().createSQLQuery(sql).addScalar("id", StandardBasicTypes.INTEGER)
		        .addScalar("facilityName", StandardBasicTypes.STRING).addScalar("facilityUuid", StandardBasicTypes.STRING)
		        .addScalar("referTo", StandardBasicTypes.STRING).addScalar("patientName", StandardBasicTypes.STRING)
		        .addScalar("patientId", StandardBasicTypes.STRING).addScalar("referredBy", StandardBasicTypes.STRING)
		        .addScalar("priorityOfReferral", StandardBasicTypes.STRING).addScalar("reason", StandardBasicTypes.STRING)
		        .addScalar("visitId", StandardBasicTypes.STRING).addScalar("referrerId", StandardBasicTypes.STRING)
		        .addScalar("uuid", StandardBasicTypes.STRING)
		        .setResultTransformer(new AliasToBeanResultTransformer(ExternalReferralDTO.class))
		        .setParameter("patientId", patientId).setParameter("visitId", visitId).list();
		return referralList;
		
	}
	
	@Transactional
	public void deleteReferralById(String externalReferralId) throws InvalidInputParamException {
		DbSession session = sessionFactory.getCurrentSession();
		try {
			session.delete(session.load(ExternalReferral.class, Integer.parseInt(externalReferralId)));
		}
		catch (EntityNotFoundException e) {
			e.printStackTrace();
			throw new InvalidInputParamException("No data found for delete using this id : " + externalReferralId);
		}
	}
	
	@Transactional
	public void deleteExternalReferral(ExternalReferral extRef) throws InvalidInputParamException {
		DbSession session = sessionFactory.getCurrentSession();
		try {
			session.delete(extRef);
		}
		catch (EntityNotFoundException e) {
			e.printStackTrace();
			throw new InvalidInputParamException("Couldn't delete external referral " + extRef.getId());
		}
	}
	
	@Transactional
	public void deleteReferralByUuid(String uuid) {
		DbSession session = sessionFactory.getCurrentSession();
		String hql = "DELETE FROM ExternalReferral WHERE uuid = :uuid";
		try {
			int result = session.createQuery(hql).setParameter("uuid", uuid).executeUpdate();
			if (result == 0) {
				throw new EntityNotFoundException("No external referral found with UUID: " + uuid);
			}
		}
		catch (EntityNotFoundException e) {
			throw new InvalidInputParamException("Couldn't delete external referral with UUID: " + uuid);
		}
	}
}
