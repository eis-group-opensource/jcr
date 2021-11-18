/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__SESSION_ID;
import static com.exigen.cm.Constants.TABLE_SESSION_MANAGER;
import static com.exigen.cm.Constants.TABLE_SESSION_MANAGER__DATE;
import static com.exigen.cm.Constants._TABLE_NODE_LOCK_INFO;

import java.sql.Types;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.statements.CreateColumn;
import com.exigen.cm.database.statements.DatabaseOperation;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SchemaChanges;

/**
 * UpgradeToVersion9 class
 */
public final class UpgradeToVersion9 extends AbstractUpgradeExecutor {

	/**
	 * Target version
	 * 
	 * @return version number
	 */
	public Long getTargetVersion() {
		return 9L;
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
		final SchemaChanges changes = new SchemaChanges(conn);
		// 1 add column to CM_NODE_LOCK_INFO

		final TableDefinition nodeLockInfo = findTableDefinition(repository, conn, _TABLE_NODE_LOCK_INFO);

		final ColumnDefinition colDefinition = new ColumnDefinition(nodeLockInfo, TABLE_NODE_LOCK_INFO__SESSION_ID, Types.INTEGER);
		final DatabaseOperation dbOperation = new CreateColumn(Constants._TABLE_NODE_LOCK_INFO, colDefinition);
		changes.add(dbOperation);

		changes.execute(conn);

		// 2. create table CM_SYSTEM_MANAGER
		final TableDefinition sessionManagerTable = new TableDefinition(TABLE_SESSION_MANAGER, true);
		sessionManagerTable
				.addColumn(new ColumnDefinition(sessionManagerTable, TABLE_SESSION_MANAGER__DATE, conn.getDialect().getColumnTypeTimeStampSQLType()));
		addTable(conn, sessionManagerTable);

		upgradeToVersion(conn);

		conn.commit();
	}

}
