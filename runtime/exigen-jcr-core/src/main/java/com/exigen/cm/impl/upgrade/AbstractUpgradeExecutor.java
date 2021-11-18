/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES__VALUE;
import static com.exigen.cm.Constants.TABLE_SYSTEM_PROPERTIES__VALUE__DB_VERSION;

import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseCountStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.RepositoryImpl;

/**
 * Base class for Upgrade
 */
public abstract class AbstractUpgradeExecutor {

	/**
	 * Upgrade JCR repository
	 * 
	 * @param conn
	 *            Connection
	 * @param repository
	 *            Repository
	 * @throws RepositoryException
	 *             Exception
	 */
	public abstract void upgrade(DatabaseConnection conn, RepositoryImpl repository) throws RepositoryException;

	/**
	 * 
	 * @return Target version
	 */
	public abstract Long getTargetVersion();

	/**
	 * Upgrades JCR repository to target version
	 * 
	 * @param conn
	 *            Connection
	 * @throws RepositoryException
	 *             Exception
	 */
	protected void upgradeToVersion(final DatabaseConnection conn) throws RepositoryException {
		final DatabaseUpdateStatement stmt = DatabaseTools.createUpdateStatement(TABLE_SYSTEM_PROPERTIES);
		stmt.addCondition(Conditions.eq(FIELD_ID, TABLE_SYSTEM_PROPERTIES__VALUE__DB_VERSION));
		stmt.addValue(SQLParameter.create(TABLE_SYSTEM_PROPERTIES__VALUE, getTargetVersion().toString()));

		stmt.execute(conn);
	}

	/**
	 * Finds table definition
	 * 
	 * @param repository
	 *            Repository
	 * @param conn
	 *            Connection
	 * @param tableName
	 *            Table name
	 * @return Table definition
	 * @throws RepositoryException
	 *             Exception
	 */
	protected TableDefinition findTableDefinition(final RepositoryImpl repository, final DatabaseConnection conn, final String tableName)
			throws RepositoryException {
		if (tableName == null) {
			throw new java.lang.NullPointerException("Parameter tableName is null");
		}
		final List<TableDefinition> allDefs = repository.getStaticTableDefenitions(null);

		TableDefinition tableDef = null;
		for (final TableDefinition td : allDefs) {
			if (tableName.equals(td.getTableName())) {
				tableDef = td;
				break;
			}
		}
		return tableDef;
	}

	/**
	 * Adds the table
	 * 
	 * @param conn
	 *            Connection
	 * @param tableDef
	 *            Table definition
	 * @throws RepositoryException
	 *             Exception
	 */
	protected void addTable(final DatabaseConnection conn, final TableDefinition tableDef) throws RepositoryException {
		final String sql = conn.getDialect().buildCreateStatement(tableDef);
		conn.execute(sql);
		conn.registerSysObject(DatabaseObject.TABLE, conn.getDialect().convertTableName(tableDef.getTableName()), false);

		// Add indexes
		final String[][] sqlArray = conn.getDialect().buildCreateIndexStatements(tableDef);
		for (int j = 0; j < sqlArray.length; j++) {
			final String[] sqlIndex = sqlArray[j];
			if (sqlIndex != null && sqlIndex[1].length() > 0) {
				conn.registerSysObject(DatabaseObject.INDEX, sqlIndex[0], false);
				conn.execute(sqlIndex[1]);
			}
		}
		// Add PK-s
		if (tableDef.getPKColumnIterator().hasNext() && !tableDef.isIndexOrganized()) {
			final String sqlpk = conn.getDialect().buildPKAlterStatement(tableDef);
			conn.execute(sqlpk);
		}
		// Add FK-s
		final String sqlfk = conn.getDialect().buildFKAlterStatement(tableDef, conn);
		if (sqlfk != null && sqlfk.length() > 0) {
			conn.execute(sqlfk);
		}
	}

	/**
	 * Validates table
	 * 
	 * @param conn
	 *            Connection
	 * @param tableName
	 *            Table name
	 * @param version
	 *            Version
	 * @throws RepositoryException
	 *             Exception
	 */
	protected void validateTable(final DatabaseConnection conn, final String tableName, final Long version) throws RepositoryException {
		final DatabaseCountStatement stmt = new DatabaseCountStatement(tableName);
		stmt.execute(conn);
		final Long result = stmt.getCount();
		stmt.close();
		if (result > 0) {
			throw new RepositoryException("Cannot automatically upgrade to version " + version + " if at least one record exists in table " + tableName);
		}
	}
}
