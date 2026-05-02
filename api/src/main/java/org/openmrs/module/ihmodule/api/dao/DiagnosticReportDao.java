package org.openmrs.module.ihmodule.api.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.db.UserDAO;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.ihmodule.DiagnosticReport;
import org.openmrs.module.ihmodule.dto.DiagnosticReportDTO;
import org.openmrs.module.ihmodule.dto.ExternalAppointmentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class DiagnosticReportDao {
	
	@Autowired
	DbSessionFactory sessionFactory;
	
	@Autowired
	UserDAO userDao;
	
	@Transactional
	public void save(DiagnosticReport dreport) throws APIException {
		if (dreport.getId() == null || dreport.getId() < 0) {
			User user = userDao.getUserByUsername("admin");
			dreport.setCreator(user);
			dreport.setDateCreated(new Date());
			dreport.setVoided(false);
			
		} else {
			User user = userDao.getUserByUsername("admin");
			dreport.setDateChanged(new Date());
			dreport.setChangedBy(user);
		}
		sessionFactory.getCurrentSession().saveOrUpdate(dreport);
		
	}
	
	public DiagnosticReport findAllByPatientIdAndResourceId(String patientId, String resourceId) throws APIException {
		try {
			return (DiagnosticReport) sessionFactory.getCurrentSession()
			        .createQuery(" from DiagnosticReport where patientId = :patientId and resourceId = :resourceId ")
			        .setString("patientId", patientId).setString("resourceId", resourceId).uniqueResult();
		}
		catch (Exception e) {
			System.err.println(e);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<DiagnosticReport> findAllByPatientId(String patientId) throws APIException {
		try {
			return sessionFactory.getCurrentSession().createQuery(" from DiagnosticReport where patientId = :patientId")
					.setString("patientId", patientId).list();
		} catch (Exception e) {
			System.err.println(e);
			return new ArrayList<>();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<DiagnosticReportDTO> findAllSQLByPatientId(String patientId) throws APIException {
		try {
			String sql="SELECT dr.patient_id patientId, dr.resource_id resourceId, dr.content_type contentType,"
					+ " dr.file_name fileName, dr.title title from diagnostic_report dr where dr.patient_id = :patientId";
			
			return sessionFactory.getCurrentSession().createSQLQuery(sql)
					.addScalar("patientId", StandardBasicTypes.STRING)
					.addScalar("resourceId", StandardBasicTypes.STRING)
					.addScalar("contentType", StandardBasicTypes.STRING)
					.addScalar("fileName", StandardBasicTypes.STRING)
					.addScalar("title", StandardBasicTypes.STRING)
					.setResultTransformer(new AliasToBeanResultTransformer(DiagnosticReportDTO.class))
					.setParameter("patientId", patientId).list();
			
		} catch (Exception e) {
			System.err.println(e);
			return new ArrayList<>();
		}
	}
	
	public DiagnosticReportDTO findDiagnosticReportByPatientIdAndResourceId(String patientId, String resourceId)
	        throws APIException {
		try {
			String sql = "SELECT dr.patient_id patientId, dr.resource_id resourceId, dr.content_type contentType,"
			        + " dr.file_name fileName, dr.title title from diagnostic_report dr"
			        + " where dr.patient_id = :patientId and dr.resource_id=:resourceId";
			
			return (DiagnosticReportDTO) sessionFactory.getCurrentSession().createSQLQuery(sql)
			        .addScalar("patientId", StandardBasicTypes.STRING).addScalar("resourceId", StandardBasicTypes.STRING)
			        .addScalar("contentType", StandardBasicTypes.STRING).addScalar("fileName", StandardBasicTypes.STRING)
			        .addScalar("title", StandardBasicTypes.STRING)
			        .setResultTransformer(new AliasToBeanResultTransformer(DiagnosticReportDTO.class))
			        .setParameter("patientId", patientId).setParameter("resourceId", resourceId).uniqueResult();
			
		}
		catch (Exception e) {
			System.err.println(e);
			return null;
		}
	}
}
