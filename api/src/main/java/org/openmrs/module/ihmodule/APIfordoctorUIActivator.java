/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ihmodule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.ihmodule.api.patientexchange.config.CentralFhirHttpTimeoutConfigurer;
import org.openmrs.module.ihmodule.setup.PatientIdentifierTypeBootstrap;

/**
 * This class contains the logic that is run every time this module is either started or shutdown
 */
public class APIfordoctorUIActivator extends BaseModuleActivator implements DaemonTokenAware {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private static volatile DaemonToken daemonToken;
	
	/**
	 * @see #started()
	 */
	public void started() {
		log.info("Started API for doctor UI");
		CentralFhirHttpTimeoutConfigurer.applyConfiguredTimeouts();
		bootstrapPatientIdentifierTypes();
	}
	
	private void bootstrapPatientIdentifierTypes() {
		// During web startup, Context already has an open session from Context.startup().
		// Do not call closeSession() here unless we opened it — that breaks SchedulerUtil.startup().
		boolean openedHere = !Context.isSessionOpen();
		if (openedHere) {
			Context.openSession();
		}
		try {
			PatientIdentifierTypeBootstrap.ensureRequiredIdentifierTypes();
		}
		catch (Exception ex) {
			log.error("ihmodule patient identifier type bootstrap failed", ex);
		}
		finally {
			if (openedHere) {
				Context.closeSession();
			}
		}
	}
	
	/**
	 * @see #shutdown()
	 */
	public void shutdown() {
		log.info("Shutdown API for doctor UI");
	}
	
	@Override
	public void stopped() {
		daemonToken = null;
		log.info("Stopped API for doctor UI");
	}
	
	@Override
	public void setDaemonToken(DaemonToken token) {
		daemonToken = token;
	}
	
	public static DaemonToken getDaemonToken() {
		return daemonToken;
	}
	
}
