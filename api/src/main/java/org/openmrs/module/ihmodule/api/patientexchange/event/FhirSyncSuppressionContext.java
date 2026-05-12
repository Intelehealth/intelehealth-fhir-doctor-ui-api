package org.openmrs.module.ihmodule.api.patientexchange.event;

public final class FhirSyncSuppressionContext {
	
	private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<Integer>();
	
	private FhirSyncSuppressionContext() {
	}
	
	public static boolean isSuppressed() {
		Integer depth = DEPTH.get();
		return depth != null && depth.intValue() > 0;
	}
	
	public static void runSuppressed(Runnable runnable) {
		enter();
		try {
			runnable.run();
		}
		finally {
			exit();
		}
	}
	
	private static void enter() {
		Integer depth = DEPTH.get();
		DEPTH.set(depth == null ? Integer.valueOf(1) : Integer.valueOf(depth.intValue() + 1));
	}
	
	private static void exit() {
		Integer depth = DEPTH.get();
		if (depth == null || depth.intValue() <= 1) {
			DEPTH.remove();
			return;
		}
		DEPTH.set(Integer.valueOf(depth.intValue() - 1));
	}
}
