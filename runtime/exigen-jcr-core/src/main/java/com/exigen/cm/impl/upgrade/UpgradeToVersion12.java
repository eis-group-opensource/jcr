/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.upgrade;

import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_ACE;
import static com.exigen.cm.Constants.TABLE_ACE2;
import static com.exigen.cm.Constants.TABLE_ACE_PERMISSION;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION;
import static com.exigen.cm.Constants.TABLE_ACE___DIRECT_SUFFIX;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__PROP_DEF;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.statements.CreateColumn;
import com.exigen.cm.database.statements.DatabaseOperation;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SchemaChanges;
import com.exigen.cm.impl.security.SecurityPermission;

/**
 * UpgradeToVersion12 class.
 */
public final class UpgradeToVersion12 extends AbstractUpgradeExecutor {

	private static final Log LOG = LogFactory.getLog(UpgradeToVersion12.class);

	/**
	 * Target version
	 * 
	 * @return version number
	 */
	public Long getTargetVersion() {
		return 12L;
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
		validateTable(conn, TABLE_ACE2, getTargetVersion());
		validateTable(conn, TABLE_ACE_PERMISSION, getTargetVersion());
		validateTable(conn, TABLE_ACE_RESTRICTION, getTargetVersion());

		SchemaChanges changes = new SchemaChanges(conn);

		// 1.Add direct fields to ACE
		final TableDefinition tableACE = findTableDefinition(repository, conn, TABLE_ACE);
		final TableDefinition nodeTypeProperty = findTableDefinition(repository, conn, TABLE_NODETYPE_PROPERTY);
		final TableDefinition node = findTableDefinition(repository, conn, TABLE_NODE);

		ColumnDefinition colDefinition = new ColumnDefinition(tableACE, SecurityPermission.BROWSE.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		DatabaseOperation dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.ADD_NODE.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.READ.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.REMOVE.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.SET_PROPERTY.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.X_GRANT.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.X_UNLOCK.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableACE, SecurityPermission.X_SUPER_DENY.getColumnName() + TABLE_ACE___DIRECT_SUFFIX, Types.BOOLEAN);
		dbOperation = new CreateColumn(Constants.TABLE_ACE, colDefinition);
		changes.add(dbOperation);

		changes.execute(conn);
		/*
		 * //2. add direct to permissions changes = new SchemaChanges(conn);
		 * TableDefinition tableAcePermission = findTableDefinition(repository,
		 * conn, TABLE_ACE_PERMISSION);
		 * 
		 * c1 = new ColumnDefinition(tableAcePermission,
		 * TABLE_ACE_PERMISSION__DIRECT,Types.BOOLEAN); op1 = new
		 * CreateColumn(Constants.TABLE_ACE_PERMISSION, c1); changes.add(op1);
		 * 
		 * changes.execute(conn);
		 */
		// 3. Extend CM_NODE_UNSTRUCT_VALUES
		changes = new SchemaChanges(conn);
		final TableDefinition tableUnstructValues = findTableDefinition(repository, conn, TABLE_NODE_UNSTRUCTURED_VALUES);

		colDefinition = new ColumnDefinition(tableUnstructValues, TABLE_NODE_UNSTRUCTURED__PROP_DEF, Types.INTEGER);
		colDefinition.setForeignKey(nodeTypeProperty);
		dbOperation = new CreateColumn(Constants.TABLE_NODE_UNSTRUCTURED_VALUES, colDefinition);
		changes.add(dbOperation);

		colDefinition = new ColumnDefinition(tableUnstructValues, FIELD_TYPE_ID, Types.INTEGER);
		colDefinition.setForeignKey(node);
		dbOperation = new CreateColumn(Constants.TABLE_NODE_UNSTRUCTURED_VALUES, colDefinition);
		changes.add(dbOperation);

		changes.execute(conn);

		// workaround to add new two foreign keys to CM_NODE_UNSTRUCT_VALUES
		// table
		final TableDefinition tableUnstructValuesX = new TableDefinition(TABLE_NODE_UNSTRUCTURED_VALUES, false);

		colDefinition = new ColumnDefinition(tableUnstructValuesX, TABLE_NODE_UNSTRUCTURED__PROP_DEF, Types.INTEGER);
		colDefinition.setForeignKey(nodeTypeProperty);
		tableUnstructValuesX.addColumn(colDefinition);

		colDefinition = new ColumnDefinition(tableUnstructValuesX, FIELD_TYPE_ID, Types.INTEGER);
		colDefinition.setForeignKey(node);
		tableUnstructValuesX.addColumn(colDefinition);

		String sqlfk = conn.getDialect().buildFKAlterStatement(tableUnstructValuesX, conn);
		sqlfk = sqlfk.replace(TABLE_NODE_UNSTRUCTURED_VALUES + "_FK2", TABLE_NODE_UNSTRUCTURED_VALUES + "_FK3");
		sqlfk = sqlfk.replace(TABLE_NODE_UNSTRUCTURED_VALUES + "_FK1", TABLE_NODE_UNSTRUCTURED_VALUES + "_FK2");

		if (sqlfk != null && sqlfk.length() > 0) {
			conn.execute(sqlfk);
		}
		// add indexes to new fields
		final String[][] sqls = conn.getDialect().buildCreateIndexStatements(tableUnstructValuesX);
		for (int j = 0; j < sqls.length; j++) {
			final String[] sqlIndex = sqls[j];
			if (sqlIndex != null && sqlIndex[1].length() > 0) {
				sqlIndex[0] = sqlIndex[0].replace(TABLE_NODE_UNSTRUCTURED_VALUES + "_I2", TABLE_NODE_UNSTRUCTURED_VALUES + "_I6");
				sqlIndex[1] = sqlIndex[1].replace(TABLE_NODE_UNSTRUCTURED_VALUES + "_I2", TABLE_NODE_UNSTRUCTURED_VALUES + "_I6");
				sqlIndex[0] = sqlIndex[0].replace(TABLE_NODE_UNSTRUCTURED_VALUES + "_I0", TABLE_NODE_UNSTRUCTURED_VALUES + "_I4");
				sqlIndex[1] = sqlIndex[1].replace(TABLE_NODE_UNSTRUCTURED_VALUES + "_I0", TABLE_NODE_UNSTRUCTURED_VALUES + "_I4");
				conn.registerSysObject(DatabaseObject.INDEX, sqlIndex[0], false);
				conn.execute(sqlIndex[1]);
			}
		}

		try {
			updateTableNodeUnstructValues(conn);
		} catch (SQLException e) {
			throw new RepositoryException(e);			
		}
		
		upgradeToVersion(conn);

		conn.commit();
	}

	/**
	 * Updates table content after table's structure change.
	 * It uses raw SQL 
	 * @param conn Connection
	 * @throws RepositoryException Repository exception
	 * @throws SQLException SQL exception
	 */
	protected void updateTableNodeUnstructValues(final DatabaseConnection conn) throws RepositoryException, SQLException {

		final DatabaseDialect dialect = conn.getDialect();

		String schemaName = dialect.getSchemaName(conn);
		if (schemaName != null && schemaName.length() > 0) {
			schemaName = schemaName + '.';
		} else {
			schemaName = "";
		}

		final Statement stmt = conn.createStatement();
		String sqlUpdate = null;
		try {
			sqlUpdate = dialect.isMSSQL() 
					? "UPDATE " + schemaName + "CM_NODE_UNSTRUCT_VALUES SET"
						+ " PROP_DEF_ID = U.PROP_DEF_ID, NODE_ID = U.NODE_ID"
						+ " FROM " + schemaName + "CM_NODE_UNSTRUCT_VALUES AS V" 
						+ " INNER JOIN " + schemaName
						+ "CM_NODE_UNSTRUCTURED AS U ON U.ID = V.PROPERTY_ID" 
					: "UPDATE " + schemaName + "CM_NODE_UNSTRUCT_VALUES V SET"
						+ " PROP_DEF_ID = (SELECT PROP_DEF_ID FROM " + schemaName + "CM_NODE_UNSTRUCTURED WHERE ID = V.PROPERTY_ID),"
						+ " NODE_ID = (SELECT NODE_ID FROM " + schemaName + "CM_NODE_UNSTRUCTURED WHERE ID = V.PROPERTY_ID)";
		
			if (LOG.isDebugEnabled()) {
				LOG.debug("Executing statement: " + sqlUpdate);
			}
			
			final int recordsUpdated = stmt.executeUpdate(sqlUpdate);

			if (LOG.isInfoEnabled()) {
				LOG.info("Updated " + recordsUpdated + " in " + TABLE_NODE_UNSTRUCTURED_VALUES);
			}
		} catch (SQLException e) {
			LOG.error("Failed executing statement: " + sqlUpdate);
			LOG.error(e.getMessage(), e);
		} finally {
			stmt.close();
		}
	}
}
