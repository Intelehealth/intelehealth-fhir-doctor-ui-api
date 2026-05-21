package org.openmrs.module.ihmodule.api.patientexchange.mpiduplicate;

/**
 * Holds {@code resolved_by} from {@code POST .../sync/force} for the duration of that HTTP request only,
 * so {@link DataSendToFHIR} can persist it on duplicate-review rows without threading through every layer.
 */
public final class ForceSyncDuplicateResolutionContext {

	private static final ThreadLocal<String> RESOLVED_BY = new ThreadLocal<>();

	private ForceSyncDuplicateResolutionContext() {
	}

	/** @param resolvedBy non-null, non-blank (validated by REST controller before calling). */
	public static void begin(String resolvedBy) {
		RESOLVED_BY.set(resolvedBy.trim());
	}

	public static String peekResolvedBy() {
		return RESOLVED_BY.get();
	}

	public static void end() {
		RESOLVED_BY.remove();
	}
}
