package org.openmrs.module.ihmodule.api.patientmatch.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientMatchRequest;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyPhoneUtils;
import org.openmrs.module.ihmodule.api.patientmatch.util.FuzzyTextUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LocalPatientFuzzyCandidateRepository implements PatientCandidateSource {
	
	@PersistenceContext
	private EntityManager em;
	
	@Transactional(readOnly = true)
	public List<FuzzyPatientCandidate> findCandidates(FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config) {
		StringBuilder sql = new StringBuilder();
		sql.append("select p.person_id as personId, ");
		sql.append("       pt.patient_id as patientId, ");
		sql.append("       p.uuid as uuid, ");
		sql.append("       p.gender as gender, ");
		sql.append("       date(p.birthdate) as birthDate, ");
		sql.append("       (select pi2.identifier from patient_identifier pi2 ");
		sql.append("         where pi2.patient_id = pt.patient_id and pi2.voided = false ");
		sql.append("         order by pi2.preferred desc, pi2.patient_identifier_id asc limit 1) as identifier, ");
		sql.append("       (select trim(concat_ws(' ', coalesce(pn2.given_name,''), coalesce(pn2.middle_name,''), coalesce(pn2.family_name,''))) ");
		sql.append("         from person_name pn2 where pn2.person_id = p.person_id and pn2.voided = false ");
		sql.append("         order by pn2.preferred desc, pn2.person_name_id asc limit 1) as name, ");
		sql.append("       (select trim(coalesce(pn2.given_name,'')) from person_name pn2 ");
		sql.append("         where pn2.person_id = p.person_id and pn2.voided = false ");
		sql.append("         order by pn2.preferred desc, pn2.person_name_id asc limit 1) as givenName, ");
		sql.append("       (select trim(coalesce(pn2.family_name,'')) from person_name pn2 ");
		sql.append("         where pn2.person_id = p.person_id and pn2.voided = false ");
		sql.append("         order by pn2.preferred desc, pn2.person_name_id asc limit 1) as familyName, ");
		sql.append("       (select trim(pa2.value) from person_attribute pa2 ");
		sql.append("         join person_attribute_type pat2 on pat2.person_attribute_type_id = pa2.person_attribute_type_id ");
		sql.append("         where pa2.person_id = p.person_id and pa2.voided = false ");
		sql.append("           and lower(replace(replace(replace(pat2.name,' ',''),'-',''),'_','')) in ('telephonenumber','phonenumber','emergencycontactnumber') ");
		sql.append("         order by pa2.person_attribute_id asc limit 1) as phone, ");
		sql.append("       (select trim(concat_ws(' ', coalesce(ad2.address1,''), coalesce(ad2.address2,''), ");
		sql.append("              coalesce(ad2.city_village,''), coalesce(ad2.state_province,''), ");
		sql.append("              coalesce(ad2.country,''), coalesce(ad2.postal_code,''))) ");
		sql.append("         from person_address ad2 where ad2.person_id = p.person_id and ad2.voided = false ");
		sql.append("         order by ad2.preferred desc, ad2.person_address_id asc limit 1) as address ");
		sql.append("from person p ");
		sql.append("join patient pt on pt.patient_id = p.person_id and pt.voided = false ");
		sql.append("where p.voided = false ");
		
		if (config.isFieldEnabled("identifier") && request.hasIdentifier()) {
			sql.append("and exists (select 1 from patient_identifier pi3 ");
			sql.append("            where pi3.patient_id = pt.patient_id and pi3.voided = false ");
			sql.append("              and lower(trim(pi3.identifier)) like :identifierPrefix) ");
		}
		if (config.isFieldEnabled("dob") && request.hasBirthDate()) {
			sql.append("and date(p.birthdate) = :birthDate ");
		}
		if (config.isFieldEnabled("gender") && request.hasGender()) {
			sql.append("and (lower(trim(p.gender)) = :genderNormalized or lower(trim(p.gender)) = :genderShort) ");
		}
		if (config.isFieldEnabled("phone") && request.hasPhone()) {
			sql.append("and exists (select 1 from person_attribute pa3 ");
			sql.append("            join person_attribute_type pat3 on pat3.person_attribute_type_id = pa3.person_attribute_type_id ");
			sql.append("            where pa3.person_id = p.person_id and pa3.voided = false ");
			sql.append("              and lower(replace(replace(replace(pat3.name,' ',''),'-',''),'_','')) in ('telephonenumber','phonenumber','emergencycontactnumber') ");
			sql.append("              and lower(trim(pa3.value)) like :phonePrefix) ");
		}
		if (config.isFieldEnabled("name") && request.hasName()) {
			sql.append("and exists (select 1 from person_name pnf ");
			sql.append("            where pnf.person_id = p.person_id and pnf.voided = false ");
			sql.append("              and (lower(trim(coalesce(pnf.given_name,''))) like :firstNamePrefix ");
			sql.append("                   or lower(trim(coalesce(pnf.family_name,''))) like :lastNamePrefix ");
			sql.append("                   or lower(trim(concat_ws(' ', coalesce(pnf.given_name,''), coalesce(pnf.family_name,'')))) like :fullNamePrefix ");
			sql.append("                   or soundex(coalesce(pnf.given_name,'')) = soundex(:firstNameToken) ");
			sql.append("                   or soundex(coalesce(pnf.family_name,'')) = soundex(:lastNameToken))) ");
		}
		if (config.isFieldEnabled("address") && request.hasAddress()) {
			sql.append("and exists (select 1 from person_address ad3 ");
			sql.append("            where ad3.person_id = p.person_id and ad3.voided = false ");
			sql.append("              and (lower(trim(coalesce(ad3.city_village,''))) like :addressPrefix ");
			sql.append("                   or lower(trim(coalesce(ad3.state_province,''))) like :addressPrefix ");
			sql.append("                   or lower(trim(coalesce(ad3.address1,''))) like :addressPrefix ");
			sql.append("                   or lower(trim(concat_ws(' ', coalesce(ad3.address1,''), coalesce(ad3.address2,''), ");
			sql.append("                        coalesce(ad3.city_village,''), coalesce(ad3.state_province,''), coalesce(ad3.country,'')))) like :addressPrefix)) ");
		}
		
		sql.append("order by p.person_id desc");
		
		Query query = em.createNativeQuery(sql.toString(), Tuple.class);
		query.setMaxResults(config.getMaxCandidates());
		
		if (config.isFieldEnabled("identifier") && request.hasIdentifier()) {
			query.setParameter("identifierPrefix", FuzzyTextUtils.normalize(request.getIdentifier()) + "%");
		}
		if (config.isFieldEnabled("dob") && request.hasBirthDate()) {
			query.setParameter("birthDate", request.getBirthDate().toString());
		}
		if (config.isFieldEnabled("gender") && request.hasGender()) {
			String normalizedGender = normalizeGender(request.getGender());
			query.setParameter("genderNormalized", normalizedGender);
			query.setParameter("genderShort", normalizedGender.length() > 0 ? normalizedGender.substring(0, 1) : "");
		}
		if (config.isFieldEnabled("phone") && request.hasPhone()) {
			String digits = FuzzyPhoneUtils.normalizeDigits(request.getPhone());
			String prefix = digits.length() >= 5 ? digits.substring(0, 5) : digits;
			query.setParameter("phonePrefix", prefix + "%");
		}
		if (config.isFieldEnabled("name") && request.hasName()) {
			String firstNameToken = FuzzyTextUtils.firstToken(request.getName());
			String lastNameToken = FuzzyTextUtils.lastToken(request.getName());
			query.setParameter("firstNamePrefix", firstNameToken + "%");
			query.setParameter("lastNamePrefix", lastNameToken + "%");
			query.setParameter("fullNamePrefix", FuzzyTextUtils.normalize(request.getName()) + "%");
			query.setParameter("firstNameToken", firstNameToken);
			query.setParameter("lastNameToken", lastNameToken);
		}
		if (config.isFieldEnabled("address") && request.hasAddress()) {
			query.setParameter("addressPrefix", FuzzyTextUtils.normalize(request.getAddress()) + "%");
		}
		
		@SuppressWarnings("unchecked")
		List<Tuple> rows = query.getResultList();
		List<FuzzyPatientCandidate> candidates = new ArrayList<FuzzyPatientCandidate>();
		for (Tuple row : rows) {
			FuzzyPatientCandidate candidate = new FuzzyPatientCandidate();
			candidate.setPersonId(integerValue(row, "personId"));
			candidate.setPatientId(integerValue(row, "patientId"));
			candidate.setUuid(stringValue(row, "uuid"));
			candidate.setIdentifier(stringValue(row, "identifier"));
			candidate.setName(stringValue(row, "name"));
			candidate.setGivenName(stringValue(row, "givenName"));
			candidate.setFamilyName(stringValue(row, "familyName"));
			candidate.setGender(stringValue(row, "gender"));
			candidate.setBirthDate(localDateValue(row, "birthDate"));
			candidate.setPhone(stringValue(row, "phone"));
			candidate.setAddress(stringValue(row, "address"));
			candidates.add(candidate);
		}
		return candidates;
	}
	
	private Integer integerValue(Tuple row, String alias) {
		Object value = row.get(alias);
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return Integer.valueOf(((Number) value).intValue());
		}
		return Integer.valueOf(String.valueOf(value));
	}
	
	private String stringValue(Tuple row, String alias) {
		Object value = row.get(alias);
		return value != null ? StringUtils.trimToNull(String.valueOf(value)) : null;
	}
	
	private LocalDate localDateValue(Tuple row, String alias) {
		Object value = row.get(alias);
		if (value == null) {
			return null;
		}
		if (value instanceof Date) {
			return ((Date) value).toLocalDate();
		}
		return LocalDate.parse(String.valueOf(value));
	}
	
	private String normalizeGender(String value) {
		String normalized = StringUtils.trimToEmpty(value).toLowerCase();
		if ("m".equals(normalized) || "male".equals(normalized)) {
			return "male";
		}
		if ("f".equals(normalized) || "female".equals(normalized)) {
			return "female";
		}
		return normalized;
	}
}
