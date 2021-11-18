/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import static com.exigen.cm.Constants.TABLE_ACE;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION__COLUMN_NAME;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION__DIRECT;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION__EXPORT_NAME;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION__PERMISSION_NAME;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION__SUB_PERMISSIONS;
import static com.exigen.cm.Constants.TABLE_ACE__CONTEXT_ID;

import java.sql.Types;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.statements.CreateColumn;
import com.exigen.cm.database.statements.DatabaseOperation;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SchemaChanges;
import com.exigen.cm.impl.security.SecurityPermission;

/**
 * UpgradeToVersion10 class.
 */
public class UpgradeToVersion10 extends AbstractUpgradeExecutor {

	/**
	 * Target version
	 * @return version number
	 */
	public Long getTargetVersion() {
		return 10L;
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

		// Automatic update was too complex. Manual script should be done for
		// each customer
		validateTable(conn, TABLE_ACE, getTargetVersion());

		final SchemaChanges changes = new SchemaChanges(conn);

		final TableDefinition tableACE = findTableDefinition(repository, conn, TABLE_ACE);

		// contextID column
		ColumnDefinition colDefinition = new ColumnDefinition(tableACE, TABLE_ACE__CONTEXT_ID, Types.VARCHAR).setLength(64);
		DatabaseOperation operation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(operation);

		// SUPER DENY permission
		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.X_SUPER_DENY.getColumnName(), Types.BOOLEAN);
		operation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(operation);

		// SUPER DENY permission parent
		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.X_SUPER_DENY.getColumnName() + "_PARENT", Types.INTEGER);
		operation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(operation);

		// BROWSE permission
		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.BROWSE.getColumnName(), Types.BOOLEAN);
		operation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(operation);
		// BROWSE permission parent
		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.BROWSE.getColumnName() + "_PARENT", Types.INTEGER);
		operation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(operation);

		changes.execute(conn);

		// 2. create table CM_ACE_PERMISSIONS
		addTable(conn, getTableAcePermission(conn.getDialect()));

		upgradeToVersion(conn);

		conn.commit();
	}

	/**
	 * Ace Permission table
	 * @param dialect Database dialect
	 * @return Ace Permission table definition
	 * @throws RepositoryException Repository exception
	 */
	public static TableDefinition getTableAcePermission(final DatabaseDialect dialect) throws RepositoryException {
		final TableDefinition ace = new TableDefinition(TABLE_ACE_PERMISSION, true);
		ace.addColumn(new ColumnDefinition(ace, TABLE_ACE_PERMISSION__COLUMN_NAME, Types.VARCHAR));
		ace.addColumn(new ColumnDefinition(ace, TABLE_ACE_PERMISSION__DIRECT, Types.BOOLEAN));
		ace.addColumn(new ColumnDefinition(ace, TABLE_ACE_PERMISSION__EXPORT_NAME, Types.VARCHAR));
		ace.addColumn(new ColumnDefinition(ace, TABLE_ACE_PERMISSION__PERMISSION_NAME, Types.VARCHAR));
		ace.addColumn(new ColumnDefinition(ace, TABLE_ACE_PERMISSION__SUB_PERMISSIONS, Types.VARCHAR));
		return ace;

	}

}
