package org.openmrs.module.ihmodule.api.patientmatch.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.PersonAddress;
import org.openmrs.api.context.Context;
import org.openmrs.module.ihmodule.api.patientexchange.config.FhirConfig;
import org.openmrs.module.ihmodule.api.patientexchange.domain.PersonAttribute;
import org.openmrs.module.ihmodule.api.patientexchange.service.CommonOperationService;
import org.openmrs.module.ihmodule.api.patientexchange.telecom.PatientTelecomMappingUtil;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PatientProfileExtensionRules;
import org.openmrs.module.ihmodule.api.patientexchange.validation.PersonAttributeToExtensionSuffix;
import org.openmrs.module.ihmodule.api.patientexchange.validation.StructureDefinitionBaseUrlResolver;
import org.openmrs.module.ihmodule.api.patientmatch.dto.AddressAPIDTO;
import org.openmrs.module.ihmodule.api.patientmatch.dto.FuzzyPatientCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enriches fuzzy-match {@link Patient} bundle entries with IG profile extensions (from
 * {@link PersonAttributeToExtensionSuffix}), ranked phone {@code telecom}, and structured
 * {@link Address} data converted from {@link AddressAPIDTO}.
 */
@Component
public class FuzzyMatchPatientResponseMapper {
	
	private static final Logger log = LoggerFactory.getLogger(FuzzyMatchPatientResponseMapper.class);
	
	@PersistenceContext
	private EntityManager em;
	
	@Autowired
	private CommonOperationService commonOperationService;
	
	@Autowired(required = false)
	private FhirConfig fhirConfig;
	
	@Transactional(readOnly = true)
	public void enrich(Patient patient, FuzzyPatientCandidate candidate) {
		if (patient == null || candidate == null) {
			return;
		}
		List<PersonAttribute> attributes = loadPersonAttributes(candidate.getUuid());
		PatientTelecomMappingUtil.applyRankedPhoneTelecom(patient, attributes);
		applyProfileExtensionsFromPersonAttributes(patient, attributes);
		AddressAPIDTO address = loadPreferredAddress(candidate.getPersonId(), candidate.getUuid());
		applyStructuredAddress(patient, address, candidate.getAddress());
	}
	
	List<PersonAttribute> loadPersonAttributes(String patientUuid) {
		if (StringUtils.isBlank(patientUuid)) {
			return new ArrayList<PersonAttribute>();
		}
		String uuid = patientUuid.trim();
		if (commonOperationService != null) {
			List<PersonAttribute> fromDatabase = commonOperationService.findPersonAttributes(uuid);
			if (fromDatabase != null && !fromDatabase.isEmpty()) {
				return fromDatabase;
			}
		}
		return loadPersonAttributesFromOpenMrs(uuid);
	}
	
	private List<PersonAttribute> loadPersonAttributesFromOpenMrs(String patientUuid) {
		List<PersonAttribute> attributes = new ArrayList<PersonAttribute>();
		if (StringUtils.isBlank(patientUuid) || !Context.isSessionOpen()) {
			return attributes;
		}
		try {
			org.openmrs.Patient patient = Context.getPatientService().getPatientByUuid(patientUuid.trim());
			if (patient == null || patient.getAttributes() == null) {
				return attributes;
			}
			Collection<org.openmrs.PersonAttribute> activeAttributes = patient.getAttributes();
			for (org.openmrs.PersonAttribute attribute : activeAttributes) {
				if (attribute == null || attribute.getVoided() || attribute.getAttributeType() == null) {
					continue;
				}
				PersonAttribute mapped = new PersonAttribute();
				mapped.setName(attribute.getAttributeType().getName());
				mapped.setValue(attribute.getValue());
				attributes.add(mapped);
			}
		}
		catch (RuntimeException ex) {
			return attributes;
		}
		return attributes;
	}
	
	void applyProfileExtensionsFromPersonAttributes(Patient patient, List<PersonAttribute> attributes) {
		if (patient == null || attributes == null || attributes.isEmpty()) {
			return;
		}
		String structureDefinitionBaseUrl = StructureDefinitionBaseUrlResolver.resolve(fhirConfig);
		if (StringUtils.isBlank(structureDefinitionBaseUrl)) {
			log.warn("Skipping patient profile extensions for fuzzy match: StructureDefinition base URL is not configured");
			return;
		}
		String sdBase = structureDefinitionBaseUrl.trim();
		while (sdBase.endsWith("/")) {
			sdBase = sdBase.substring(0, sdBase.length() - 1);
		}
		for (PersonAttribute attribute : attributes) {
			if (attribute == null || StringUtils.isBlank(attribute.getName())
			        || isIgnorablePersonAttributeValue(attribute.getValue())) {
				continue;
			}
			String suffix = PersonAttributeToExtensionSuffix.map(attribute.getName());
			if (suffix == null || !PatientProfileExtensionRules.isAllowedStructureDefinitionSuffix(suffix)) {
				continue;
			}
			Extension extension = new Extension();
			extension.setUrl(sdBase + "/StructureDefinition/" + suffix);
			extension.setValue(new StringType(attribute.getValue().trim()));
			patient.addExtension(extension);
		}
	}
	
	AddressAPIDTO loadPreferredAddress(Integer personId, String patientUuid) {
		AddressAPIDTO fromOpenMrs = loadPreferredAddressFromOpenMrs(patientUuid);
		if (fromOpenMrs != null) {
			return fromOpenMrs;
		}
		return loadPreferredAddressFromDatabase(personId, patientUuid);
	}
	
	private AddressAPIDTO loadPreferredAddressFromOpenMrs(String patientUuid) {
		if (StringUtils.isBlank(patientUuid) || !Context.isSessionOpen()) {
			return null;
		}
		try {
			org.openmrs.Patient patient = Context.getPatientService().getPatientByUuid(patientUuid.trim());
			if (patient == null) {
				return null;
			}
			PersonAddress personAddress = patient.getPersonAddress();
			if (personAddress == null) {
				return null;
			}
			return mapPersonAddress(personAddress);
		}
		catch (RuntimeException ex) {
			return null;
		}
	}
	
	private AddressAPIDTO loadPreferredAddressFromDatabase(Integer personId, String patientUuid) {
		if (em == null) {
			return null;
		}
		String sql = "select trim(coalesce(ad.address1,'')) as address1, " + "trim(coalesce(ad.address2,'')) as address2, "
		        + "trim(coalesce(ad.address3,'')) as address3, " + "trim(coalesce(ad.address4,'')) as address4, "
		        + "trim(coalesce(ad.address5,'')) as address5, " + "trim(coalesce(ad.address6,'')) as address6, "
		        + "trim(coalesce(ad.city_village,'')) as city_village, "
		        + "trim(coalesce(ad.state_province,'')) as state_province, " + "trim(coalesce(ad.country,'')) as country, "
		        + "trim(coalesce(ad.postal_code,'')) as postal_code, "
		        + "trim(coalesce(ad.county_district,'')) as county_district " + "from person_address ad ";
		if (personId != null) {
			sql += "where ad.person_id = :personId and ad.voided = false ";
		} else if (StringUtils.isNotBlank(patientUuid)) {
			sql += "join person p on p.person_id = ad.person_id and p.voided = false "
			        + "where p.uuid = :patientUuid and ad.voided = false ";
		} else {
			return null;
		}
		sql += "order by ad.preferred desc, ad.person_address_id asc limit 1";
		javax.persistence.Query query = em.createNativeQuery(sql, Tuple.class);
		if (personId != null) {
			query.setParameter("personId", personId);
		} else {
			query.setParameter("patientUuid", patientUuid.trim());
		}
		@SuppressWarnings("unchecked")
		List<Tuple> rows = query.getResultList();
		if (rows == null || rows.isEmpty()) {
			return null;
		}
		return tupleToAddressDto(rows.get(0));
	}
	
	private static AddressAPIDTO mapPersonAddress(PersonAddress personAddress) {
		AddressAPIDTO dto = new AddressAPIDTO();
		dto.setAddress1(trimToNull(personAddress.getAddress1()));
		dto.setAddress2(trimToNull(personAddress.getAddress2()));
		dto.setAddress3(trimToNull(personAddress.getAddress3()));
		dto.setAddress4(trimToNull(personAddress.getAddress4()));
		dto.setAddress5(trimToNull(personAddress.getAddress5()));
		dto.setAddress6(trimToNull(personAddress.getAddress6()));
		dto.setCityVillage(trimToNull(personAddress.getCityVillage()));
		dto.setStateProvince(resolveStateProvince(personAddress));
		dto.setCountry(trimToNull(personAddress.getCountry()));
		dto.setPostalCode(resolvePostalCode(personAddress));
		dto.setCountyDistrict(trimToNull(personAddress.getCountyDistrict()));
		return hasAnyAddressField(dto) ? dto : null;
	}
	
	private static String resolveStateProvince(PersonAddress personAddress) {
		if (personAddress == null) {
			return null;
		}
		String state = trimToNull(personAddress.getStateProvince());
		if (state != null) {
			return state;
		}
		state = trimToNull(personAddress.getAddress5());
		if (state != null && !isPlaceholderAddressToken(state)) {
			return state;
		}
		state = trimToNull(personAddress.getAddress2());
		if (state != null && !isPlaceholderAddressToken(state)) {
			return state;
		}
		return null;
	}
	
	private static String resolvePostalCode(PersonAddress personAddress) {
		if (personAddress == null) {
			return null;
		}
		String postalCode = trimToNull(personAddress.getPostalCode());
		if (postalCode != null) {
			return postalCode;
		}
		return trimToNull(personAddress.getAddress6());
	}
	
	private void applyStructuredAddress(Patient patient, AddressAPIDTO addressDto, String fallbackText) {
		if (patient == null) {
			return;
		}
		patient.getAddress().clear();
		if (addressDto != null && hasAnyAddressField(addressDto)) {
			patient.addAddress(toFhirAddress(addressDto));
			return;
		}
		if (StringUtils.isNotBlank(fallbackText)) {
			patient.addAddress().setText(fallbackText.trim());
		}
	}
	
	private static boolean isIgnorablePersonAttributeValue(String value) {
		return StringUtils.isBlank(value);
	}
	
	private static boolean isPlaceholderAddressToken(String value) {
		return "NA".equalsIgnoreCase(value.trim()) || "-".equals(value.trim());
	}
	
	private static Address toFhirAddress(AddressAPIDTO dto) {
		Address address = new Address();
		addLine(address, dto.getAddress1());
		addLine(address, dto.getAddress2());
		addLine(address, dto.getAddress3());
		address.setCity(trimToNull(dto.getCityVillage()));
		address.setState(trimToNull(dto.getStateProvince()));
		address.setCountry(trimToNull(dto.getCountry()));
		address.setPostalCode(resolvePostalCodeFromDto(dto));
		address.setDistrict(trimToNull(dto.getCountyDistrict()));
		return address;
	}
	
	private static String resolvePostalCodeFromDto(AddressAPIDTO dto) {
		if (dto == null) {
			return null;
		}
		String postalCode = trimToNull(dto.getPostalCode());
		if (postalCode != null) {
			return postalCode;
		}
		return trimToNull(dto.getAddress6());
	}
	
	private static void addLine(Address address, String value) {
		String line = trimToNull(value);
		if (line != null && !isPlaceholderAddressToken(line)) {
			address.addLine(line);
		}
	}
	
	private static AddressAPIDTO tupleToAddressDto(Tuple row) {
		AddressAPIDTO dto = new AddressAPIDTO();
		dto.setAddress1(tupleString(row, "address1"));
		dto.setAddress2(tupleString(row, "address2"));
		dto.setAddress3(tupleString(row, "address3"));
		dto.setAddress4(tupleString(row, "address4"));
		dto.setAddress5(tupleString(row, "address5"));
		dto.setAddress6(tupleString(row, "address6"));
		dto.setCityVillage(tupleString(row, "city_village", "cityVillage"));
		dto.setStateProvince(resolveStateProvinceFromDto(dto, row));
		dto.setCountry(tupleString(row, "country"));
		dto.setPostalCode(tupleString(row, "postal_code", "postalCode"));
		if (dto.getPostalCode() == null) {
			dto.setPostalCode(trimToNull(dto.getAddress6()));
		}
		dto.setCountyDistrict(tupleString(row, "county_district", "countyDistrict"));
		return hasAnyAddressField(dto) ? dto : null;
	}
	
	private static String resolveStateProvinceFromDto(AddressAPIDTO dto, Tuple row) {
		String state = tupleString(row, "state_province", "stateProvince");
		if (state != null) {
			dto.setStateProvince(state);
			return state;
		}
		state = trimToNull(dto.getAddress5());
		if (state != null && !isPlaceholderAddressToken(state)) {
			dto.setStateProvince(state);
			return state;
		}
		state = trimToNull(dto.getAddress2());
		if (state != null && !isPlaceholderAddressToken(state)) {
			dto.setStateProvince(state);
			return state;
		}
		return null;
	}
	
	private static String tupleString(Tuple row, String... aliases) {
		if (row == null || aliases == null) {
			return null;
		}
		for (String alias : aliases) {
			if (StringUtils.isBlank(alias)) {
				continue;
			}
			try {
				Object value = row.get(alias);
				if (value != null) {
					String trimmed = StringUtils.trimToNull(value.toString());
					if (trimmed != null) {
						return trimmed;
					}
				}
			}
			catch (IllegalArgumentException ex) {
				// try next alias / case-insensitive match below
			}
		}
		for (String alias : aliases) {
			if (StringUtils.isBlank(alias)) {
				continue;
			}
			String normalizedAlias = alias.trim().toLowerCase();
			for (TupleElement<?> element : row.getElements()) {
				if (element.getAlias() == null) {
					continue;
				}
				if (element.getAlias().equalsIgnoreCase(alias) || element.getAlias().toLowerCase().equals(normalizedAlias)) {
					Object value = row.get(element);
					if (value != null) {
						String trimmed = StringUtils.trimToNull(value.toString());
						if (trimmed != null) {
							return trimmed;
						}
					}
				}
			}
		}
		return null;
	}
	
	private static boolean hasAnyAddressField(AddressAPIDTO dto) {
		return dto != null
		        && (trimToNull(dto.getAddress1()) != null || trimToNull(dto.getAddress2()) != null
		                || trimToNull(dto.getAddress3()) != null || trimToNull(dto.getAddress4()) != null
		                || trimToNull(dto.getAddress5()) != null || trimToNull(dto.getAddress6()) != null
		                || trimToNull(dto.getCityVillage()) != null || trimToNull(dto.getStateProvince()) != null
		                || trimToNull(dto.getCountry()) != null || trimToNull(dto.getPostalCode()) != null || trimToNull(dto
		                .getCountyDistrict()) != null);
	}
	
	private static String trimToNull(String value) {
		return StringUtils.trimToNull(value);
	}
}
