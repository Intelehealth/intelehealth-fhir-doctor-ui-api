package org.openmrs.module.ihmodule.api.patientmatch.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HAPI FHIR omits empty {@code address.line} values when encoding JSON. This restores the contract
 * of exactly two line elements for fuzzy-match bundle responses.
 */
public final class FuzzyMatchBundleJsonAddressLineNormalizer {
	
	private static final Logger log = LoggerFactory.getLogger(FuzzyMatchBundleJsonAddressLineNormalizer.class);
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private FuzzyMatchBundleJsonAddressLineNormalizer() {
	}
	
	public static String normalizeEncodedBundle(String json, boolean prettyPrint) {
		if (json == null || json.isEmpty()) {
			return json;
		}
		try {
			JsonNode root = MAPPER.readTree(json);
			if (!"Bundle".equals(root.path("resourceType").asText())) {
				return json;
			}
			JsonNode entries = root.path("entry");
			if (!entries.isArray()) {
				return json;
			}
			for (JsonNode entry : entries) {
				JsonNode resource = entry.path("resource");
				if (!"Patient".equals(resource.path("resourceType").asText()) || !resource.isObject()) {
					continue;
				}
				normalizePatientAddresses((ObjectNode) resource);
			}
			return prettyPrint ? MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root) : MAPPER
			        .writeValueAsString(root);
		}
		catch (RuntimeException ex) {
			log.warn("Failed to normalize fuzzy-match address lines in bundle JSON; returning original payload", ex);
			return json;
		}
		catch (Exception ex) {
			log.warn("Failed to normalize fuzzy-match address lines in bundle JSON; returning original payload", ex);
			return json;
		}
	}
	
	private static void normalizePatientAddresses(ObjectNode patient) {
		JsonNode addresses = patient.path("address");
		if (!addresses.isArray() || addresses.isEmpty()) {
			return;
		}
		ArrayNode addressArray = (ArrayNode) addresses;
		for (int i = 0; i < addressArray.size(); i++) {
			JsonNode addressNode = addressArray.get(i);
			if (addressNode instanceof ObjectNode) {
				normalizeAddressLines((ObjectNode) addressNode);
			}
		}
	}
	
	private static void normalizeAddressLines(ObjectNode address) {
		String line0 = "";
		String line1 = "";
		JsonNode existing = address.path("line");
		if (existing.isArray()) {
			if (existing.size() > 0) {
				line0 = FuzzyMatchAddressLineSupport.normalizeAddressLineValue(existing.get(0).asText(""));
			}
			if (existing.size() > 1) {
				line1 = FuzzyMatchAddressLineSupport.normalizeAddressLineValue(existing.get(1).asText(""));
			}
		}
		ArrayNode lines = MAPPER.createArrayNode();
		lines.add(line0);
		lines.add(line1);
		address.set("line", lines);
	}
}
