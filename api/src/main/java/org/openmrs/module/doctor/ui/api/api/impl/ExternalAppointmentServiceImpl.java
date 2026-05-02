/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.doctor.ui.api.api.impl;

import org.apache.http.client.utils.DateUtils;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.db.UserDAO;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.doctor.ui.api.ExternalAppointment;
import org.openmrs.module.doctor.ui.api.api.ExtrnalAppointmentService;
import org.openmrs.module.doctor.ui.api.api.dao.ExternalAppointmentDao;
import org.openmrs.module.doctor.ui.api.api.impl.exp.BeanValidationException;
import org.openmrs.module.doctor.ui.api.api.impl.exp.DuplicateEntryException;
import org.openmrs.module.doctor.ui.api.dto.ExternalAppointmentDTO;
import org.openmrs.module.doctor.ui.api.utils.DeploymentConfProperties;
import org.openmrs.module.doctor.ui.api.utils.HttpResponse;
import org.openmrs.module.doctor.ui.api.utils.HttpService;
import org.openmrs.module.doctor.ui.api.utils.ReqParam;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExternalAppointmentServiceImpl extends BaseOpenmrsService implements ExtrnalAppointmentService {
	
	ExternalAppointmentDao dao;
	
	@Autowired
	UserDAO userDao;
	
	@Autowired
	DeploymentConfProperties deployConf;
	
	public void setDao(ExternalAppointmentDao dao) {
		this.dao = dao;
	}
	
	private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	
	private static final Validator validator = factory.getValidator();
	
	public void validateAppointment(ExternalAppointment appointment) throws BeanValidationException {
		Set<ConstraintViolation<ExternalAppointment>> violations = validator.validate(appointment);
		if (!violations.isEmpty()) {
			for (ConstraintViolation<ExternalAppointment> violation : violations) {
				System.out.println("Validation error: " + violation.getMessage());
				
				throw new BeanValidationException(violation.getMessage());
			}
			throw new BeanValidationException("Validation failed");
		}
	}
	
	@Override
	public ExternalAppointmentDTO save(ExternalAppointment appointment) throws APIException, BeanValidationException,
	        DuplicateEntryException {
		validateAppointment(appointment);
		
		User user = userDao.getUserByUsername("admin");
		appointment.setCreator(user);
		appointment.setDateCreated(new Date());
		appointment.setVoided(false);
		ExternalAppointment savedAppointment = dao.save(appointment);
		return mapToExternalDTO(savedAppointment);
		
	}
	
	private ExternalAppointmentDTO mapToExternalDTO(ExternalAppointment entity) {
		if (entity == null)
			return null;
		ExternalAppointmentDTO dto = new ExternalAppointmentDTO();
		dto.setId(entity.getId());
		dto.setAppointmentDate(entity.getAppointmentDate());
		dto.setAppointmentTime(entity.getAppointmentTime());
		dto.setFacilityName(entity.getFacilityName());
		dto.setFacilityUuid(entity.getFacilityUuid());
		dto.setPatientIdentifier(entity.getPatientIdentifier());
		dto.setPatientName(entity.getPatientName());
		dto.setPatientUuid(entity.getPatientUuid());
		dto.setPractitionerName(entity.getPractitionerName());
		dto.setRequestDate(DateUtils.formatDate(entity.getDateCreated(), "yyyy-MM-dd HH:mm:ss"));
		dto.setRequesterId(entity.getRequesterId());
		dto.setRequestId(entity.getRequestId());
		dto.setStatus(entity.getStatus());
		dto.setVisitId(entity.getVisitId());
		return dto;
		
	}
	
	@Override
	public ExternalAppointment update(ExternalAppointment appointment) throws APIException {
		
		return null;
	}
	
	@Override
	public List<ExternalAppointmentDTO> findAllByPatient(String uuid) throws APIException {
		return dao.findAllByPatient(uuid);
	}
	
	@Override
	public List<ExternalAppointmentDTO> findAppointmentByPatientAndVisitId(String patientId, String visitId)
	        throws APIException {
		return dao.findAppointmentByPatientAndVisitId(patientId, visitId);
	}
	
	@Override
	public List<ExternalAppointment> findAllByRequestId(String uuid) throws APIException {
		
		return dao.findAllByRequestId(uuid);
	}
	
	@Override
	public List<ExternalAppointment> search(String startDate, String endDate, String requester) throws APIException {
		
		return dao.search(startDate, endDate, requester);
	}
	
	@Override
	public HttpResponse getAllPractitioners(Map<String, String> reqParam) throws Exception {
		String param = ReqParam.toQueryParam(reqParam);
		return new HttpService().get(deployConf.APPOINTMENTS_URL + "/practitioners", param);
	}
	
	@Override
	public HttpResponse getAvailableSchedule(Map<String, String> reqParam) throws Exception {
		String param = ReqParam.toQueryParam(reqParam);
		return new HttpService().get(deployConf.APPOINTMENTS_URL + "/available/schedule", param);
	}
	
	@Override
	public HttpResponse getAvailableSlot(Map<String, String> reqParam) throws Exception {
		String param = ReqParam.toQueryParam(reqParam);
		return new HttpService().get(deployConf.APPOINTMENTS_URL + "/available/slot", param);
	}
	
	@Override
	public void deleteAppointment(String extAppointmentId) {
		dao.deleteAppointmentById(extAppointmentId);
	}
	
	@Override
	public void changeStatus(Map<String, String> reqParam) throws APIException {
		String requestId = reqParam.getOrDefault("requestId", null);
		String status = reqParam.getOrDefault("status", null);
		
		if (requestId == null || status == null || status.isEmpty())
			throw new InvalidParameterException("Invalid parameter or value");
		
		List<ExternalAppointment> appointments = dao.findAllByRequestId(requestId);
		
		if (appointments == null || appointments.isEmpty()) {
			throw new InvalidParameterException("Couldn't update any appointment using this requestId " + requestId);
		}
		
		System.err.println("requestId:" + requestId + " , status: " + status + ", appointments: " + appointments.size());
		
		User user = userDao.getUserByUsername("admin");
		
		for (ExternalAppointment ex : appointments) {
			ex.setStatus(status);
			ex.setDateChanged(new Date());
			ex.setChangedBy(user);
			dao.save(ex);
		}
		
	}
	
}
