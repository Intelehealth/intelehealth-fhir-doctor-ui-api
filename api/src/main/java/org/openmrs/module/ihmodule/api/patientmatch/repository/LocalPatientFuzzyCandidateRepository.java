package org.openmrs.module.ihmodule.api.patientmatch.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.ihmodule.api.patientmatch.config.FuzzyPatientMatchConfig;
import org.openmrs.module.ihmodule.api.patientmatch.config.PatientMatchRules;
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
		boolean applyDobRepositoryFilter = config.isFieldEnabled("dob") && config.isCandidateSearchParamEnabled("birthdate")
		        && shouldApplyDobRepositoryFilter(request, config);
		QueryBinding hardDobBinding = applyDobRepositoryFilter ? buildFieldBinding("birthdate", request, config, "harddob")
		        : null;
		
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
		
		List<QueryBinding> candidateBindings = candidateSearchBindings(request, config);
		Map<String, Object> queryParameters = new LinkedHashMap<String, Object>();
		if (!candidateBindings.isEmpty()) {
			List<String> candidateClauses = new ArrayList<String>();
			for (QueryBinding binding : candidateBindings) {
				candidateClauses.add("(" + binding.getClause() + ")");
				queryParameters.putAll(binding.getParameters());
			}
			sql.append("and (").append(String.join(" or ", candidateClauses)).append(") ");
		} else {
			QueryBinding fallbackBinding = legacyFallbackBinding(request, config);
			if (fallbackBinding != null && StringUtils.isNotBlank(fallbackBinding.getClause())) {
				sql.append("and (").append(fallbackBinding.getClause()).append(") ");
				queryParameters.putAll(fallbackBinding.getParameters());
			}
		}
		if (hardDobBinding != null && StringUtils.isNotBlank(hardDobBinding.getClause())) {
			sql.append("and ").append(hardDobBinding.getClause()).append(" ");
			queryParameters.putAll(hardDobBinding.getParameters());
		}
		
		if (request.hasBirthDate()) {
			sql.append("order by case when date(p.birthdate) = :sortBirthDateExact then 0 else 1 end, p.person_id desc");
			queryParameters.put("sortBirthDateExact", request.getBirthDate().toString());
		} else {
			sql.append("order by p.person_id desc");
		}
		
		Query query = em.createNativeQuery(sql.toString(), Tuple.class);
		query.setMaxResults(config.getMaxCandidates());
		
		for (Map.Entry<String, Object> entry : queryParameters.entrySet()) {
			query.setParameter(entry.getKey(), entry.getValue());
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
	
	boolean shouldApplyDobRepositoryFilter(FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config) {
		if (request == null || config == null || !request.hasBirthDate()) {
			return false;
		}
		switch (config.getDobRepositoryFilterMode()) {
			case ALWAYS:
				return true;
			case NEVER:
				return false;
			case ONLY_DOB_REQUESTS:
			default:
				return !hasNonDobSearchField(request);
		}
	}
	
	private boolean hasNonDobSearchField(FuzzyPatientMatchRequest request) {
		return request.hasIdentifier() || request.hasName() || request.hasPhone() || request.hasAddress()
		        || request.hasGender();
	}
	
	List<String> applicableCandidateSearchRuleKeys(FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config) {
		List<String> keys = new ArrayList<String>();
		PatientMatchRules rules = config != null ? config.getRules() : null;
		if (rules == null || rules.getCandidateSearchParams() == null) {
			return keys;
		}
		for (PatientMatchRules.CandidateSearchRule rule : rules.getCandidateSearchParams()) {
			QueryBinding binding = buildCandidateSearchRuleBinding(rule, request, config, "inspect");
			if (binding != null && StringUtils.isNotBlank(binding.getClause())) {
				keys.add(String.join(",", rule.getSearchParams()));
			}
		}
		return keys;
	}
	
	private List<QueryBinding> candidateSearchBindings(FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config) {
		List<QueryBinding> bindings = new ArrayList<QueryBinding>();
		PatientMatchRules rules = config != null ? config.getRules() : null;
		if (rules == null || rules.getCandidateSearchParams() == null) {
			return bindings;
		}
		int ruleIndex = 0;
		for (PatientMatchRules.CandidateSearchRule rule : rules.getCandidateSearchParams()) {
			QueryBinding binding = buildCandidateSearchRuleBinding(rule, request, config, "rule" + ruleIndex++);
			if (binding != null && StringUtils.isNotBlank(binding.getClause())) {
				bindings.add(binding);
			}
		}
		return bindings;
	}
	
	private QueryBinding buildCandidateSearchRuleBinding(PatientMatchRules.CandidateSearchRule rule,
	        FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config, String suffix) {
		if (rule == null || rule.getSearchParams() == null || rule.getSearchParams().isEmpty()) {
			return null;
		}
		if (StringUtils.isNotBlank(rule.getResourceType()) && !"Patient".equalsIgnoreCase(rule.getResourceType())) {
			return null;
		}
		List<String> clauses = new ArrayList<String>();
		Map<String, Object> parameters = new LinkedHashMap<String, Object>();
		int fieldIndex = 0;
		for (String searchParam : rule.getSearchParams()) {
			String normalized = normalizeCandidateSearchParam(searchParam);
			if (!requestHasSearchParam(request, normalized) || !isSearchParamEnabled(config, normalized)) {
				return null;
			}
			QueryBinding fieldBinding = buildFieldBinding(normalized, request, config, suffix + "_" + fieldIndex++);
			if (fieldBinding == null || StringUtils.isBlank(fieldBinding.getClause())) {
				return null;
			}
			clauses.add(fieldBinding.getClause());
			parameters.putAll(fieldBinding.getParameters());
		}
		return clauses.isEmpty() ? null : new QueryBinding(String.join(" and ", clauses), parameters);
	}
	
	private QueryBinding legacyFallbackBinding(FuzzyPatientMatchRequest request, FuzzyPatientMatchConfig config) {
		List<String> clauses = new ArrayList<String>();
		Map<String, Object> parameters = new LinkedHashMap<String, Object>();
		String[] searchParams = new String[] { "identifier", "name", "phone", "address", "gender" };
		for (String searchParam : searchParams) {
			if (!requestHasSearchParam(request, searchParam) || !isSearchParamEnabled(config, searchParam)) {
				continue;
			}
			QueryBinding fieldBinding = buildFieldBinding(searchParam, request, config, "fallback_" + searchParam);
			if (fieldBinding == null || StringUtils.isBlank(fieldBinding.getClause())) {
				continue;
			}
			clauses.add(fieldBinding.getClause());
			parameters.putAll(fieldBinding.getParameters());
		}
		return clauses.isEmpty() ? null : new QueryBinding(String.join(" and ", clauses), parameters);
	}
	
	private QueryBinding buildFieldBinding(String searchParam, FuzzyPatientMatchRequest request,
	        FuzzyPatientMatchConfig config, String suffix) {
		if ("identifier".equals(searchParam) && request.hasIdentifier()) {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			String parameterName = "identifierPrefix_" + suffix;
			params.put(parameterName, FuzzyTextUtils.normalize(request.getIdentifier()) + "%");
			return new QueryBinding("exists (select 1 from patient_identifier pi3 "
			        + "where pi3.patient_id = pt.patient_id and pi3.voided = false "
			        + "and lower(trim(pi3.identifier)) like :" + parameterName + ")", params);
		}
		if ("birthdate".equals(searchParam) && request.hasBirthDate()) {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			if (config.getDobNearMatchDays() > 0) {
				String fromName = "birthDateFrom_" + suffix;
				String toName = "birthDateTo_" + suffix;
				params.put(fromName, request.getBirthDate().minusDays(config.getDobNearMatchDays()).toString());
				params.put(toName, request.getBirthDate().plusDays(config.getDobNearMatchDays()).toString());
				return new QueryBinding("date(p.birthdate) between :" + fromName + " and :" + toName, params);
			}
			String exactName = "birthDate_" + suffix;
			params.put(exactName, request.getBirthDate().toString());
			return new QueryBinding("date(p.birthdate) = :" + exactName, params);
		}
		if ("gender".equals(searchParam) && request.hasGender()) {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			String normalizedGender = normalizeGender(request.getGender());
			String normalizedName = "genderNormalized_" + suffix;
			String shortName = "genderShort_" + suffix;
			params.put(normalizedName, normalizedGender);
			params.put(shortName, normalizedGender.length() > 0 ? normalizedGender.substring(0, 1) : "");
			return new QueryBinding("(lower(trim(p.gender)) = :" + normalizedName + " or lower(trim(p.gender)) = :"
			        + shortName + ")", params);
		}
		if ("phone".equals(searchParam) && request.hasPhone()) {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			String digits = FuzzyPhoneUtils.normalizeDigits(request.getPhone());
			String prefix = digits.length() >= 5 ? digits.substring(0, 5) : digits;
			String parameterName = "phonePrefix_" + suffix;
			params.put(parameterName, prefix + "%");
			return new QueryBinding(
			        "exists (select 1 from person_attribute pa3 "
			                + "join person_attribute_type pat3 on pat3.person_attribute_type_id = pa3.person_attribute_type_id "
			                + "where pa3.person_id = p.person_id and pa3.voided = false "
			                + "and lower(replace(replace(replace(pat3.name,' ',''),'-',''),'_','')) in ('telephonenumber','phonenumber','emergencycontactnumber') "
			                + "and lower(trim(pa3.value)) like :" + parameterName + ")", params);
		}
		if ("name".equals(searchParam) && request.hasName()) {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			String firstNameToken = request.hasGivenName() ? FuzzyTextUtils.normalize(request.getGivenName())
			        : FuzzyTextUtils.firstToken(request.getName());
			String lastNameToken = request.hasFamilyName() ? FuzzyTextUtils.normalize(request.getFamilyName())
			        : FuzzyTextUtils.lastToken(request.getName());
			String firstPrefix = "firstNamePrefix_" + suffix;
			String lastPrefix = "lastNamePrefix_" + suffix;
			String fullPrefix = "fullNamePrefix_" + suffix;
			String firstTokenName = "firstNameToken_" + suffix;
			String lastTokenName = "lastNameToken_" + suffix;
			params.put(firstPrefix, firstNameToken + "%");
			params.put(lastPrefix, lastNameToken + "%");
			params.put(fullPrefix, FuzzyTextUtils.normalize(request.getName()) + "%");
			params.put(firstTokenName, firstNameToken);
			params.put(lastTokenName, lastNameToken);
			return new QueryBinding("exists (select 1 from person_name pnf "
			        + "where pnf.person_id = p.person_id and pnf.voided = false "
			        + "and (lower(trim(coalesce(pnf.given_name,''))) like :" + firstPrefix
			        + " or lower(trim(coalesce(pnf.family_name,''))) like :" + lastPrefix
			        + " or lower(trim(concat_ws(' ', coalesce(pnf.given_name,''), coalesce(pnf.family_name,'')))) like :"
			        + fullPrefix + " or soundex(coalesce(pnf.given_name,'')) = soundex(:" + firstTokenName
			        + ") or soundex(coalesce(pnf.family_name,'')) = soundex(:" + lastTokenName + ")))", params);
		}
		if ("address".equals(searchParam) && request.hasAddress()) {
			Map<String, Object> params = new LinkedHashMap<String, Object>();
			String parameterName = "addressPrefix_" + suffix;
			params.put(parameterName, FuzzyTextUtils.normalize(request.getAddress()) + "%");
			return new QueryBinding("exists (select 1 from person_address ad3 "
			        + "where ad3.person_id = p.person_id and ad3.voided = false "
			        + "and (lower(trim(coalesce(ad3.city_village,''))) like :" + parameterName
			        + " or lower(trim(coalesce(ad3.state_province,''))) like :" + parameterName
			        + " or lower(trim(coalesce(ad3.address1,''))) like :" + parameterName
			        + " or lower(trim(concat_ws(' ', coalesce(ad3.address1,''), coalesce(ad3.address2,''), "
			        + "coalesce(ad3.city_village,''), coalesce(ad3.state_province,''), coalesce(ad3.country,'')))) like :"
			        + parameterName + "))", params);
		}
		return null;
	}
	
	private boolean requestHasSearchParam(FuzzyPatientMatchRequest request, String searchParam) {
		if (request == null || searchParam == null) {
			return false;
		}
		if ("identifier".equals(searchParam)) {
			return request.hasIdentifier();
		}
		if ("name".equals(searchParam)) {
			return request.hasName();
		}
		if ("birthdate".equals(searchParam)) {
			return request.hasBirthDate();
		}
		if ("phone".equals(searchParam)) {
			return request.hasPhone();
		}
		if ("address".equals(searchParam)) {
			return request.hasAddress();
		}
		if ("gender".equals(searchParam)) {
			return request.hasGender();
		}
		return false;
	}
	
	private boolean isSearchParamEnabled(FuzzyPatientMatchConfig config, String searchParam) {
		if (config == null || searchParam == null) {
			return false;
		}
		if ("birthdate".equals(searchParam)) {
			return config.isFieldEnabled("dob") && config.isCandidateSearchParamEnabled("birthdate");
		}
		return config.isFieldEnabled(searchParam) && config.isCandidateSearchParamEnabled(searchParam);
	}
	
	private String normalizeCandidateSearchParam(String searchParam) {
		String normalized = StringUtils.trimToNull(searchParam);
		if (normalized == null) {
			return null;
		}
		normalized = normalized.toLowerCase();
		if ("telecom".equals(normalized)) {
			return "phone";
		}
		return normalized;
	}
	
	private static final class QueryBinding {
		
		private final String clause;
		
		private final Map<String, Object> parameters;
		
		private QueryBinding(String clause, Map<String, Object> parameters) {
			this.clause = clause;
			this.parameters = parameters;
		}
		
		private String getClause() {
			return clause;
		}
		
		private Map<String, Object> getParameters() {
			return parameters;
		}
	}
}
