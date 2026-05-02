package org.openmrs.module.doctor.ui.api.api.impl;

import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.db.UserDAO;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.doctor.ui.api.ExternalReferral;
import org.openmrs.module.doctor.ui.api.api.ExternalReferralService;
import org.openmrs.module.doctor.ui.api.api.dao.ExternalAppointmentDao;
import org.openmrs.module.doctor.ui.api.api.dao.ExternalReferralDao;
import org.openmrs.module.doctor.ui.api.api.impl.exp.BeanValidationException;
import org.openmrs.module.doctor.ui.api.dto.ExternalReferralDTO;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class ExternalReferralServiceImpl extends BaseOpenmrsService implements ExternalReferralService {
	
	private ExternalReferralDao dao;
	
	@Autowired
	UserDAO userDao;
	
	private ExternalAppointmentDao externalAppointmentDao;
	
	public void setDao(ExternalReferralDao dao) {
		this.dao = dao;
	}
	
	public void setExternalAppointmentDao(ExternalAppointmentDao externalAppointmentDao) {
		this.externalAppointmentDao = externalAppointmentDao;
	}
	
	private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
	
	public void validateReferral(ExternalReferral referral) throws BeanValidationException {
		Set<ConstraintViolation<ExternalReferral>> violations = validator.validate(referral);
		if (!violations.isEmpty()) {
			for (ConstraintViolation<ExternalReferral> violation : violations) {
				System.out.println("Validation error: " + violation.getMessage());
				throw new BeanValidationException(violation.getMessage());
			}
			throw new BeanValidationException("Validation failed");
		}
	}
	
	@Override
	@Authorized(PrivilegeConstants.ADD_OBS)
	public ExternalReferralDTO save(ExternalReferral externalReferral) {
		validateReferral(externalReferral);
		User user = userDao.getUserByUsername("admin");
		externalReferral.setCreator(user);
		externalReferral.setDateCreated(new Date());
		externalReferral.setVoided(false);
		ExternalReferral saved = dao.save(externalReferral);
		return mapToExternalDTO(saved);
	}
	
	@Override
	public List<ExternalReferralDTO> findReferralByPatientIdAndVisitId(String patientId, String visitId) throws APIException {
		return dao.findReferralByPatientIdAndReferralId(patientId, visitId);
	}
	
	@Override
	public void deleteReferral(String extReferralId) {
		//		ExternalReferral extRef = dao.getExternalReferralById(Integer.parseInt(extReferralId));
		//		if (extRef != null) {
		//			List<ExternalAppointmentDTO> extAppointmentList = externalAppointmentDao.findAppointmentByPatientAndVisitId(
		//			    extRef.getPatientId(), extRef.getVisitId());
		//			if (extAppointmentList != null && !extAppointmentList.isEmpty()) {
		//				for (ExternalAppointmentDTO extAppointment : extAppointmentList) {
		//					externalAppointmentDao.deleteAppointmentById(extAppointment.getId() + "");
		//				}
		//			}
		//		}
		dao.deleteReferralById(extReferralId);
	}
	
	@Override
	public void deleteReferralByUuid(String uuid) {
		dao.deleteReferralByUuid(uuid);
	}
	
	private ExternalReferralDTO mapToExternalDTO(ExternalReferral entity) {
		if (entity == null)
			return null;
		ExternalReferralDTO dto = new ExternalReferralDTO();
		dto.setId(entity.getId());
		dto.setFacilityName(entity.getFacilityName());
		dto.setFacilityUuid(entity.getFacilityUuid());
		dto.setPatientName(entity.getPatientName());
		dto.setPatientId(entity.getPatientId());
		dto.setVisitId(entity.getVisitId());
		dto.setReason(entity.getReason());
		dto.setPriorityOfReferral(entity.getPriorityOfReferral());
		dto.setReferredBy(entity.getReferredBy());
		dto.setReferrerId(entity.getReferrerId());
		dto.setReferTo(entity.getReferTo());
		dto.setUuid(entity.getUuid());
		return dto;
	}
	
}
