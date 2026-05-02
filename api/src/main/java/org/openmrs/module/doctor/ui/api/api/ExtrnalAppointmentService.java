/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.doctor.ui.api.api;

import java.util.List;
import java.util.Map;

import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.doctor.ui.api.ExternalAppointment;
import org.openmrs.module.doctor.ui.api.dto.ExternalAppointmentDTO;
import org.openmrs.module.doctor.ui.api.utils.HttpResponse;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface ExtrnalAppointmentService extends OpenmrsService {
	
	ExternalAppointmentDTO save(ExternalAppointment appointment) throws APIException;
	
	void changeStatus(Map<String, String> reqParam) throws APIException;
	
	ExternalAppointment update(ExternalAppointment appointment) throws APIException;
	
	List<ExternalAppointmentDTO> findAllByPatient(String patientId) throws APIException;
	
	List<ExternalAppointmentDTO> findAppointmentByPatientAndVisitId(String patientId, String visitId) throws APIException;
	
	List<ExternalAppointment> findAllByRequestId(String uuid) throws APIException;
	
	List<ExternalAppointment> search(String start, String end, String requester) throws APIException;
	
	HttpResponse getAllPractitioners(Map<String, String> reqParam) throws Exception;
	
	HttpResponse getAvailableSchedule(Map<String, String> reqParam) throws Exception;
	
	HttpResponse getAvailableSlot(Map<String, String> reqParam) throws Exception;
	
	void deleteAppointment(String extAppointmentId);
}
