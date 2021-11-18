/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_ACE;
import static com.exigen.cm.Constants.TABLE_ACE2;
import static com.exigen.cm.Constants.TABLE_ACE2___FROM_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE2___PARENT_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE2___SEQUENCE_SUFFIX;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION__ACE_ID;
import static com.exigen.cm.Constants.TABLE_ACE__CONTEXT_ID;
import static com.exigen.cm.Constants.TABLE_ACE__GROUP_ID;
import static com.exigen.cm.Constants.TABLE_ACE__USER_ID;

import java.sql.Types;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.statements.DatabaseOperation;
import com.exigen.cm.database.statements.DropColumn;
import com.exigen.cm.database.statements.ModifyColumn;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SchemaChanges;
import com.exigen.cm.impl.security.SecurityPermission;

/**
 * UpgradeToVersion11 class.
 * 
 */
public final class UpgradeToVersion11 extends AbstractUpgradeExecutor {

	/**
	 * Constant that represents string "_PARENT".
	 */
	private static final String PARENT_POSTFIX = "_PARENT";

	/**
	 * Target version
	 * @return version number
	 */
	public Long getTargetVersion() {
		return 11L;
	}

	/**
	 * Upgrades JCR to 11.
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
		validateTable(conn, TABLE_ACE_PERMISSION, getTargetVersion());

		final SchemaChanges changes = new SchemaChanges(conn);

		final TableDefinition tableACE = findTableDefinition(repository, conn, TABLE_ACE);

		// userId column change to 64 char
		ColumnDefinition colDefinition = new ColumnDefinition(tableACE, TABLE_ACE__USER_ID, Types.VARCHAR).setLength(64);
		DatabaseOperation dbOperation = new ModifyColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		// groupId column change to 64 char
		colDefinition = new ColumnDefinition(tableACE, TABLE_ACE__GROUP_ID, Types.VARCHAR).setLength(64);
		dbOperation = new ModifyColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		// contextId column change to 64 char
		colDefinition = new ColumnDefinition(tableACE, TABLE_ACE__CONTEXT_ID, Types.VARCHAR).setLength(64);
		dbOperation = new ModifyColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.BROWSE.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.READ.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.ADD_NODE.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.REMOVE.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.SET_PROPERTY.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.X_GRANT.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.X_UNLOCK.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		dbOperation = new DropColumn(Constants.TABLE_ACE, SecurityPermission.X_SUPER_DENY.getColumnName() + PARENT_POSTFIX);
		changes.add(dbOperation);

		changes.execute(conn);

		// 2. create table CM_ACE_RESTRICTION
		addTable(conn, getTableAceRestriction(conn.getDialect(), tableACE));
		conn.commit();

		// 3. create table CM_ACE2
		addTable(conn, getTableAce2(conn.getDialect(), tableACE));
		conn.commit();

		upgradeToVersion(conn);

		conn.commit();
	}

	/**
	 * Ace Restriction table.
	 * @param dialect Database dialect
	 * @param tableACE Table definition
	 * @return Ace Restriction table definition
	 * @throws RepositoryException Repository exception
	 */
	private static TableDefinition getTableAceRestriction(final DatabaseDialect dialect, final TableDefinition tableACE) throws RepositoryException {
		TableDefinition aceTable = new TableDefinition(TABLE_ACE_RESTRICTION, true);
		ColumnDefinition colDefinition = new ColumnDefinition(aceTable, TABLE_ACE_RESTRICTION__ACE_ID, Types.INTEGER);
		colDefinition.setForeignKey(tableACE);
		aceTable.addColumn(colDefinition);
		aceTable.addColumn(new ColumnDefinition(aceTable, TABLE_ACE__USER_ID, Types.VARCHAR));
		aceTable.addColumn(new ColumnDefinition(aceTable, TABLE_ACE__GROUP_ID, Types.VARCHAR));
		return aceTable;
	}

	/**
	 * Table Ace2 definition.
	 * @param dialect Database dialect
	 * @param tableACE Table definition
	 * @return Table ACE2 definition
	 * @throws RepositoryException Repository exception
	 */
	private static TableDefinition getTableAce2(final DatabaseDialect dialect, final TableDefinition tableACE) throws RepositoryException {
		final TableDefinition ace2Table = new TableDefinition(TABLE_ACE2, false);

		final ColumnDefinition colDefinition = new ColumnDefinition(ace2Table, FIELD_TYPE_ID, Types.INTEGER, true);
		colDefinition.setNotNull(true);
		boolean autoindex = ace2Table.isAutoCreateIndex();
		ace2Table.setAutoCreateIndex(false);
		colDefinition.setForeignKey(tableACE);
		ace2Table.setAutoCreateIndex(autoindex);
		ace2Table.addColumn(colDefinition);

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.BROWSE.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.BROWSE.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.BROWSE.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.READ.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.READ.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.READ.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.ADD_NODE.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.ADD_NODE.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.ADD_NODE.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.SET_PROPERTY.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.SET_PROPERTY.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.SET_PROPERTY.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.REMOVE.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.REMOVE.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.REMOVE.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_GRANT.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_GRANT.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_GRANT.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_UNLOCK.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_UNLOCK.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_UNLOCK.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_SUPER_DENY.getColumnName() + TABLE_ACE2___PARENT_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_SUPER_DENY.getColumnName() + TABLE_ACE2___SEQUENCE_SUFFIX, Types.INTEGER));
		ace2Table.addColumn(new ColumnDefinition(ace2Table, SecurityPermission.X_SUPER_DENY.getColumnName() + TABLE_ACE2___FROM_SUFFIX, Types.VARCHAR));

		return ace2Table;
	}
}
