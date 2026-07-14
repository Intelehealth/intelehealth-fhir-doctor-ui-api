package org.openmrs.module.ihmodule.api.patientexchange.sync;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

/**
 * Per-patient reentrant lock so concurrent central-sync threads cannot POST-create the same
 * facility patient twice or insert duplicate MPI / Source Patient Id rows.
 */
public final class PatientUuidLock {
	
	private static final ConcurrentMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<String, ReentrantLock>();
	
	private PatientUuidLock() {
	}
	
	public static void runWithLock(String patientUuid, Runnable work) {
		callWithLock(patientUuid, new Callable<Void>() {
			
			@Override
			public Void call() {
				work.run();
				return null;
			}
		});
	}
	
	public static <T> T callWithLock(String patientUuid, Callable<T> work) {
		if (StringUtils.isBlank(patientUuid)) {
			throw new IllegalArgumentException("patientUuid is required");
		}
		ReentrantLock lock = LOCKS.computeIfAbsent(patientUuid.trim(),
		    new java.util.function.Function<String, ReentrantLock>() {
			    
			    @Override
			    public ReentrantLock apply(String key) {
				    return new ReentrantLock();
			    }
		    });
		lock.lock();
		try {
			try {
				return work.call();
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		finally {
			lock.unlock();
		}
	}
}
