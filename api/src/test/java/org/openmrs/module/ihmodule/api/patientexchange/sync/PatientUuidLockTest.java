package org.openmrs.module.ihmodule.api.patientexchange.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class PatientUuidLockTest {
	
	@Test
	public void callWithLock_shouldSerializeConcurrentWorkForSamePatient() throws Exception {
		final AtomicInteger concurrent = new AtomicInteger(0);
		final AtomicInteger maxConcurrent = new AtomicInteger(0);
		final CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Callable<Void> work = new Callable<Void>() {
				
				@Override
				public Void call() throws Exception {
					start.await(5, TimeUnit.SECONDS);
					PatientUuidLock.runWithLock("patient-1", new Runnable() {
						
						@Override
						public void run() {
							int active = concurrent.incrementAndGet();
							maxConcurrent.updateAndGet(current -> Math.max(current, active));
							try {
								Thread.sleep(50);
							}
							catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
							}
							concurrent.decrementAndGet();
						}
					});
					return null;
				}
			};
			pool.submit(work);
			pool.submit(work);
			start.countDown();
			pool.shutdown();
			assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
			assertEquals(1, maxConcurrent.get());
		}
		finally {
			pool.shutdownNow();
		}
	}
	
	@Test
	public void callWithLock_shouldAllowParallelWorkForDifferentPatients() throws Exception {
		final CountDownLatch start = new CountDownLatch(1);
		final AtomicInteger maxConcurrent = new AtomicInteger(0);
		final AtomicInteger concurrent = new AtomicInteger(0);
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Callable<Void> work = new Callable<Void>() {
				
				@Override
				public Void call() throws Exception {
					start.await(5, TimeUnit.SECONDS);
					PatientUuidLock.runWithLock("patient-a", new Runnable() {
						
						@Override
						public void run() {
							int active = concurrent.incrementAndGet();
							maxConcurrent.updateAndGet(current -> Math.max(current, active));
							try {
								Thread.sleep(100);
							}
							catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
							}
							concurrent.decrementAndGet();
						}
					});
					return null;
				}
			};
			Callable<Void> otherWork = new Callable<Void>() {
				
				@Override
				public Void call() throws Exception {
					start.await(5, TimeUnit.SECONDS);
					PatientUuidLock.runWithLock("patient-b", new Runnable() {
						
						@Override
						public void run() {
							int active = concurrent.incrementAndGet();
							maxConcurrent.updateAndGet(current -> Math.max(current, active));
							try {
								Thread.sleep(100);
							}
							catch (InterruptedException ex) {
								Thread.currentThread().interrupt();
							}
							concurrent.decrementAndGet();
						}
					});
					return null;
				}
			};
			pool.submit(work);
			pool.submit(otherWork);
			start.countDown();
			pool.shutdown();
			assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
			assertEquals(2, maxConcurrent.get());
		}
		finally {
			pool.shutdownNow();
		}
	}
}
