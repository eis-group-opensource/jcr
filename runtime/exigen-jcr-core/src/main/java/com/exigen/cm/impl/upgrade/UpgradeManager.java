/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.RepositoryImpl;

/**
 * Upgrade Manager class. 
 */
public final class UpgradeManager {

	/**
	 * Upgrade executors
	 */
	private final static Map<Long, AbstractUpgradeExecutor> EXECUTORS;

	static {
		EXECUTORS = new HashMap<Long, AbstractUpgradeExecutor>();

		// v9
		final UpgradeToVersion9 upgrade9 = new UpgradeToVersion9();
		EXECUTORS.put(upgrade9.getTargetVersion(), upgrade9);

		// v10
		final UpgradeToVersion10 upgrade10 = new UpgradeToVersion10();
		EXECUTORS.put(upgrade10.getTargetVersion(), upgrade10);

		// v11
		final UpgradeToVersion11 upgrade11 = new UpgradeToVersion11();
		EXECUTORS.put(upgrade11.getTargetVersion(), upgrade11);

		// v12
		final UpgradeToVersion12 upgrade12 = new UpgradeToVersion12();
		EXECUTORS.put(upgrade12.getTargetVersion(), upgrade12);

		// v13
		final UpgradeToVersion13 upgrade13 = new UpgradeToVersion13();
		EXECUTORS.put(upgrade13.getTargetVersion(), upgrade13);

		// v14
		final UpgradeToVersion14 upgrade14 = new UpgradeToVersion14();
		EXECUTORS.put(upgrade14.getTargetVersion(), upgrade14);

	}

	/**
	 * Repository implementation
	 */
	private final RepositoryImpl repository;
	
	/**
	 * Database connection
	 */
	private final DatabaseConnection conn;

	/**
	 * 
	 * @param repository Repository
	 * @param conn connection
	 */
	public UpgradeManager(final RepositoryImpl repository, final DatabaseConnection conn) {
		this.repository = repository;
		this.conn = conn;
	}

	/**
	 * 
	 * @param activeVersion Active version
	 * @param buildVersion build version
	 * @return true if success
	 * @throws RepositoryException Exception
	 */
	public boolean upgrade(final String activeVersion, final String buildVersion) throws RepositoryException {
		if (!repository.isAllowUpgrade()) {
			return false;
		}
		long old;
		try {
			old = Long.parseLong(activeVersion);
		} catch (NumberFormatException e) {
			old = -1L;
		}

		final long current = Long.parseLong(buildVersion);

		if (current < old) {
			throw new RepositoryException("Cannot downgrade from " + old + " to version " + current);
		}

		while (old != current) {
			old++;
			final AbstractUpgradeExecutor executor = EXECUTORS.get(old);
			if (executor == null) {
				throw new RepositoryException("Unable to upgrade to version " + old + " from " + (old - 1));
			}
			executor.upgrade(conn, repository);
		}

		return true;

	}

}
