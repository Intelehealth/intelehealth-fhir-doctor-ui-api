package org.openmrs.module.ihmodule.api.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.ihmodule.PrescriptionShare;
import org.openmrs.module.ihmodule.api.PrescriptionShareService;
import org.openmrs.module.ihmodule.api.dao.PrescriptionShareDao;
import org.openmrs.module.ihmodule.dto.PrescriptionShareDTO;
import org.openmrs.module.ihmodule.dto.PrescriptionShareRequestDTO;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class PrescriptionShareServiceImpl extends BaseOpenmrsService implements PrescriptionShareService {
	
	private PrescriptionShareDao dao;
	
	private PlatformTransactionManager transactionManager;
	
	public void setDao(PrescriptionShareDao dao) {
		this.dao = dao;
	}
	
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	@Override
	public PrescriptionShareDTO createShare(PrescriptionShareRequestDTO request) throws APIException {
		validateCreateRequest(request);
		return createWriteTransactionTemplate().execute(status -> doCreateShare(request));
	}
	
	@Override
	public void deleteShareByUuid(String uuid, String voidReason) throws APIException {
		if (StringUtils.isBlank(uuid)) {
			throw new APIException("uuid is required");
		}
		createWriteTransactionTemplate().execute(status -> {
			doVoidShare(dao.getByUuid(uuid), voidReason);
			return null;
		});
	}
	
	@Override
	public void deleteShareByPatientAndVisit(String patientUuid, String visitUuid, String voidReason)
	        throws APIException {
		if (StringUtils.isBlank(patientUuid) || StringUtils.isBlank(visitUuid)) {
			throw new APIException("patientUuid and visitUuid are required");
		}
		createWriteTransactionTemplate().execute(status -> {
			List<PrescriptionShare> shares = dao.getActiveByPatientAndVisit(patientUuid, visitUuid);
			if (shares == null || shares.isEmpty()) {
				throw new APIException("Prescription share not found");
			}
			doVoidShares(shares, voidReason);
			return null;
		});
	}
	
	private PrescriptionShareDTO doCreateShare(PrescriptionShareRequestDTO request) throws APIException {
		User user = requireUser();
		Date now = new Date();
		List<String> locationUuids = normalizeLocationUuids(request.getLocationUuids());
		
		doVoidShares(
		    dao.getActiveByPatientAndVisit(request.getPatientUuid(), request.getVisitUuid()),
		    "Replaced by updated prescription share locations");
		
		List<PrescriptionShare> created = new ArrayList<>();
		for (String locationUuid : locationUuids) {
			PrescriptionShare share = new PrescriptionShare();
			share.setUuid(UUID.randomUUID().toString());
			share.setPatientUuid(request.getPatientUuid());
			share.setVisitUuid(request.getVisitUuid());
			share.setLocationUuid(locationUuid);
			share.setCreator(user);
			share.setDateCreated(now);
			share.setVoided(false);
			created.add(dao.save(share));
		}
		
		Context.flushSession();
		return mapToDto(created);
	}
	
	private void doVoidShare(PrescriptionShare share, String voidReason) throws APIException {
		if (share == null || share.isVoided()) {
			throw new APIException("Prescription share not found");
		}
		voidShare(share, requireUser(), new Date(), resolveVoidReason(voidReason));
		dao.save(share);
		Context.flushSession();
	}
	
	private void doVoidShares(List<PrescriptionShare> shares, String voidReason) throws APIException {
		if (shares == null || shares.isEmpty()) {
			return;
		}
		User user = requireUser();
		Date now = new Date();
		String reason = resolveVoidReason(voidReason);
		for (PrescriptionShare share : shares) {
			if (share != null && !share.isVoided()) {
				voidShare(share, user, now, reason);
				dao.save(share);
			}
		}
		Context.flushSession();
	}
	
	private void voidShare(PrescriptionShare share, User user, Date now, String reason) {
		share.setVoided(true);
		share.setVoidedBy(user);
		share.setDateVoided(now);
		share.setVoidReason(reason);
		share.setChangedBy(user);
		share.setDateChanged(now);
	}
	
	private String resolveVoidReason(String voidReason) {
		return StringUtils.isBlank(voidReason) ? "Prescription share deleted via API" : voidReason;
	}
	
	private void validateCreateRequest(PrescriptionShareRequestDTO request) throws APIException {
		if (request == null) {
			throw new APIException("Request body is required");
		}
		if (StringUtils.isBlank(request.getPatientUuid())) {
			throw new APIException("patientUuid is required");
		}
		if (StringUtils.isBlank(request.getVisitUuid())) {
			throw new APIException("visitUuid is required");
		}
		if (request.getLocationUuids() == null || request.getLocationUuids().isEmpty()) {
			throw new APIException("At least one locationUuid is required");
		}
	}
	
	private List<String> normalizeLocationUuids(List<String> locationUuids) throws APIException {
		Set<String> unique = new LinkedHashSet<>();
		for (String locationUuid : locationUuids) {
			if (StringUtils.isBlank(locationUuid)) {
				throw new APIException("locationUuids must not contain blank values");
			}
			unique.add(locationUuid.trim());
		}
		if (unique.isEmpty()) {
			throw new APIException("At least one locationUuid is required");
		}
		return new ArrayList<>(unique);
	}
	
	private User requireUser() throws APIException {
		User user = Context.getAuthenticatedUser();
		if (user == null) {
			user = Context.getUserService().getUserByUsername("admin");
		}
		if (user == null) {
			throw new APIException("Unable to resolve user for audit fields");
		}
		return user;
	}
	
	private PrescriptionShareDTO mapToDto(List<PrescriptionShare> shares) {
		if (shares == null || shares.isEmpty()) {
			return null;
		}
		PrescriptionShare first = shares.get(0);
		PrescriptionShareDTO dto = new PrescriptionShareDTO();
		dto.setPatientUuid(first.getPatientUuid());
		dto.setVisitUuid(first.getVisitUuid());
		dto.setDateCreated(first.getDateCreated());
		dto.setDateChanged(first.getDateChanged());
		dto.setVoided(first.isVoided());
		dto.setDateVoided(first.getDateVoided());
		dto.setVoidReason(first.getVoidReason());
		if (first.getCreator() != null) {
			dto.setCreatorUuid(first.getCreator().getUuid());
		}
		if (first.getChangedBy() != null) {
			dto.setChangedByUuid(first.getChangedBy().getUuid());
		}
		if (first.getVoidedBy() != null) {
			dto.setVoidedByUuid(first.getVoidedBy().getUuid());
		}
		List<String> locationUuids = new ArrayList<>();
		for (PrescriptionShare share : shares) {
			if (share != null && !share.isVoided() && StringUtils.isNotBlank(share.getLocationUuid())) {
				locationUuids.add(share.getLocationUuid());
			}
		}
		dto.setLocationUuids(locationUuids);
		return dto;
	}
	
	private TransactionTemplate createWriteTransactionTemplate() {
		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		txTemplate.setReadOnly(false);
		return txTemplate;
	}
	
}
