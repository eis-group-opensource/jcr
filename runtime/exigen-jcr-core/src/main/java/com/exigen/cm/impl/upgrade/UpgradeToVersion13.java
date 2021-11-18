/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.RepositoryImpl;

/**
 * UpgradeToVersion13 class.
 */
public final class UpgradeToVersion13 extends AbstractUpgradeExecutor {

	/**
	 * Target version
	 * @return version number
	 */
	public Long getTargetVersion() {
		return 13L;
	}

	/**
	 * Upgrades JCR
	 * 
	 * @param conn
	 *            Database connection
	 * @param repository
	 *            JCR Repository implementation
	 * @throws RepositoryException
	 *             Repository exception
	 */
	public void upgrade(final DatabaseConnection conn, final RepositoryImpl repository) throws RepositoryException {

		upgradeToVersion(conn);

		conn.commit();
	}

}
