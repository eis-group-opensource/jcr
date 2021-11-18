/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_ALL;
import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_TABLE;
import static com.exigen.cm.Constants.DB_TRIGGER_UNSTRUCTURED;
import static com.exigen.cm.Constants.DB_TRIGGER_UNSTRUCTURED_MULTIPLE;
import static com.exigen.cm.Constants.DEFAULT_ID_RANGE;
import static com.exigen.cm.Constants.DEFAULT_SEQUENCE_NAME;
import static com.exigen.cm.Constants.FIELD_BLOB;
import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_URL;
import static com.exigen.cm.Constants.PROPERTY_ORACLE_CTXSYS_PASSWORD;
import static com.exigen.cm.Constants.RC_FTS_CONV_COMPRESS_ERR;
import static com.exigen.cm.Constants.RC_FTS_CONV_DELETE_ERR;
import static com.exigen.cm.Constants.RC_FTS_CONV_EXTRACT_ERR;
import static com.exigen.cm.Constants.RC_FTS_CONV_NO_ROWS;
import static com.exigen.cm.Constants.RC_FTS_CONV_OK;
import static com.exigen.cm.Constants.RC_FTS_CONV_TOO_MANY_ROWS;
import static com.exigen.cm.Constants.RC_FTS_CONV_UPDATE_ERR;
import static com.exigen.cm.Constants.TABLE_FTS_DATA;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE_CONV;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE__FILENAME;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORATXT_DYN_UNCOMPRESS_PREF;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORATXT_SCHEMA;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORATXT_STAGE_CONV_SGP;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_DYN_UNCOMPRESS_PROC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTSUTIL_PKG;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_CONVERT_FUNC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_ERR_LOG_PROC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_REC_PROCESS_FUNC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_FTS_ZIP_FUNC;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_LOB_COMPRESS_JAVA_CLASS;
import static com.exigen.cm.database.dialect.oracle.OracleConstants.ORA_SECURITY_PKG;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.IndexDefinition;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.dialect.IndexingProcessor;
import com.exigen.cm.database.dialect.oracle.objdef.OracleObjectPrivilegeDef;
import com.exigen.cm.database.dialect.oracle.objdef.OraclePackageDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleStoredFunctionDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleStoredJavaClassDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleStoredProcedureDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleTextPreferenceDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleTextSectionGroupDef;
import com.exigen.cm.database.dialect.oracle.objdef.OracleTriggerDef;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.StoredProcedureDatabaseCondition;
import com.exigen.cm.query.BasicSecurityFilter;
import com.exigen.cm.query.predicate.FTSQueryBuilder;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.vf.commons.logging.LogUtils;

/**
 * Oracle database dialect 
 */
public class OracleDatabaseDialect extends AbstractDatabaseDialect {

	public static final char STRING_PREFIX = '.';	

	/** Log for this class */
	private static final Log LOG = LogFactory.getLog(OracleDatabaseDialect.class);

	/**
	 * @return Boolean
	 */
	public String getColumnTypeBoolean() {
		return "CHAR(1)";
	}
	
	public int getColumnTypeBooleanSQLType() {
		return Types.CHAR;
	}

	@Override
	public String getColumnTypeString() {
		return "VARCHAR2";
	}

	@Override
	public int getColumnTypeStringSQLType() {
		return Types.VARCHAR;
	}
	
	/**
	 * @return Timestamp
	 */
	public String getColumnTypeTimeStamp() {
		return "TIMESTAMP";
	}

	public String getColumnTypeFloat() {
		return "FLOAT";
	}

	@Override
	public String getColumnTypeLong() {
		return "NUMBER(19,0)";
	}

	/**
	 * Build after table statement
	 * @param tableName Table name
	 * @param columnDefs Column definitions
	 */
	@Override
	public String buildAlterTableStatement(final String tableName, 
			final ColumnDefinition[] columnDefs) throws RepositoryException {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("ALTER TABLE ");
		buffer.append(tableName);
		buffer.append(" ADD (");
		for (int i = 0; i < columnDefs.length; i++) {
			ColumnDefinition columnDef = columnDefs[i];
			// id varchar(32) NOT NULL
			buffer.append(columnDef.getColumnName());
			buffer.append(' ');
			buffer.append(getSQLType(columnDef.getColumnType(), columnDef.getLength()));
			if (columnDef.isNotNull()) {
				buffer.append(" NOT NULL ");
			}
			if (i != (columnDefs.length - 1)) {
				buffer.append(',');
			}
		}
		buffer.append(" )");
		if (isSemicolumnAtEndNeccesary()) {
			buffer.append(';');
		}
		return buffer.toString();
	}

	/**
	 * @param tableName
	 *            Table name
	 * @param columnDefs
	 *            Column definitions
	 */
	@Override
	public String buildAlterTableModifyColumnStatement(final String tableName, 
			final ColumnDefinition[] columnDefs) throws RepositoryException {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("ALTER TABLE ");
		buffer.append(convertTableName(tableName));
		buffer.append(" MODIFY (");
		for (int i = 0; i < columnDefs.length; i++) {
			final ColumnDefinition columnDef = columnDefs[i];
			// id varchar(32) NOT NULL
			buffer.append(columnDef.getColumnName());
			buffer.append(' ');
			buffer.append(getSQLType(columnDef.getColumnType(), columnDef.getLength()));
			if (columnDef.isNotNull()) {
				buffer.append(" NOT NULL ");
			}
			if (i != (columnDefs.length - 1)) {
				buffer.append(',');
			}
		}
		buffer.append(')');
		if (isSemicolumnAtEndNeccesary()) {
			buffer.append(';');
		}
		return buffer.toString();
	}

	/**
	 * @param conn Connection
	 * @param tableName Table name
	 * @param pkPropertyName PK property name
	 * @param pkValue PK value
	 */
	@Override
	public void _lockRow(final DatabaseConnection conn, final String tableName, 
			final String pkPropertyName, final Object pkValue) throws RepositoryException {
		try {
			// SELECT id FROM Entity WHERE id =? FOR UPDATE
			final StringBuilder sql = new StringBuilder();
			sql.append("SELECT ");
			sql.append(pkPropertyName);
			sql.append(" FROM ");
			sql.append(tableName);
			sql.append(" WHERE ");
			sql.append(pkPropertyName);
			// max waiting time?
			sql.append("=? FOR UPDATE");
			
			LogUtils.debug(LOG, sql.toString());
			
			final boolean useCache = true;
			final PreparedStatement stmt = conn.prepareStatement(sql.toString(), useCache);
			// bind id
			DatabaseTools.bindParameter(stmt, conn.getDialect(), 1, pkValue, false);
			try {
				stmt.execute();
			} finally {
				// If statement caching is used, connection should not be closed.
				if (!useCache) {
					stmt.close();
				}
			}
		} catch (SQLException exc) {
			throw new RepositoryException("Error locking row", exc);
		}

	}

	
	/**
	 * @param sb String buffer
	 */
	@Override
	public void addForUpdateAfterStatement(final StringBuffer sb) {
		sb.append(" FOR UPDATE ");
	}

	/**
	 * copied from AbstractDatabaseDialect - required for buildFKAlterStatement
	 * @param tableDef Table definition
	 */
	private String getIdColumnNames(final TableDefinition tableDef) {
		final StringBuilder buffer = new StringBuilder();
		for (final Iterator<ColumnDefinition> it = tableDef.getPKColumnIterator(); it.hasNext();) {
			final ColumnDefinition columnDef = it.next();
			buffer.append(columnDef.getColumnName());
			if (it.hasNext()) {
				buffer.append(',');
			}
		}
		return buffer.toString();
	}

	/**
	 * @param tableDef Table definition
	 * @param conn Connection
	 */
	@Override
	public String buildFKAlterStatement(final TableDefinition tableDef, 
			final DatabaseConnection conn) throws RepositoryException {
		final StringBuilder builder = new StringBuilder();
		// ALTER TABLE TESTTAB ADD CONSTRAINT TESTTAB_FK2 FOREIGN KEY (t1)
		// REFERENCES city (id);
		int i = 0; // for shorten FK names - count them and name as table_fkN
		for (Iterator<ColumnDefinition> it = tableDef.getColumnIterator(); it.hasNext();) {
			final ColumnDefinition columnDef = (ColumnDefinition) it.next();
			final TableDefinition fkTable = columnDef.getForeignKey();
			if (fkTable != null) {
				if (i++ == 0) { // single SQL statement with multiple FKs
					builder.append("ALTER TABLE ");
					builder.append(tableDef.getTableName());
				}
				builder.append(" ADD CONSTRAINT ");
				String fkName = conn.getDialect().convertConstraintName(tableDef.getTableName() + "_FK" + i);
				// sb.append(tableDef.getTableName());
				// sb.append("_FK");
				// sb.append(i);
				builder.append(fkName);
				builder.append(" FOREIGN KEY (");
				builder.append(columnDef.getColumnName());
				builder.append(") REFERENCES ");
				builder.append(fkTable.getTableName());
				builder.append(" (");
				builder.append(getIdColumnNames(fkTable));
				builder.append(')');
				// There is no .. ON UPDATE in Oracle
				// restriction ON DELETE is by default
				conn.registerSysObject(DatabaseObject.CONSTRAINT, 
						convertTableName(tableDef.getTableName()) + "." + fkName, false);

			}
		}
		return builder.toString();
	}

	/**
	 * @return Alter Action Restrict
	 */
	public String getAlterActionRestrict() {
		return "????"; // throw exception ?
	}

	/**
	 * @return true is sequence supported
	 */
	public boolean isSequenceSupported() {
		return true;
	}

	/**
	 * Adds sequence
	 * @param conn Connection
	 */
	@Override
	protected void addSequence(final DatabaseConnection conn) throws RepositoryException {
		try {
			conn.execute("CREATE SEQUENCE " + DEFAULT_SEQUENCE_NAME + " CACHE 50  INCREMENT BY " + DEFAULT_ID_RANGE);
		} catch (RepositoryException e) {
			throw new RepositoryException("Error creating sequence", e);
		}
	}

	/**
	 * @param Connection
	 */
	@Override
	public void dropSequence(final DatabaseConnection conn) {
		try {
			conn.execute("DROP SEQUENCE " + DEFAULT_SEQUENCE_NAME);
		} catch (RepositoryException e) {
			LOG.error(e.getMessage());
		}
	}

	/**
	 * @param Connection
	 */
	@Override
	protected Long reserveIdRangeSequence(DatabaseConnection conn) throws RepositoryException {
		Long result = null;
		DatabaseConnection localConn = null;

		if (conn == null) {
			localConn = connectionProvider.createConnection();
			conn = localConn;
		}
		try {
			final PreparedStatement stmt = conn.prepareStatement("SELECT " + DEFAULT_SEQUENCE_NAME + ".NEXTVAL SEQ FROM DUAL", false);
			try {
				stmt.execute();

				ResultSet resultSet = stmt.getResultSet();
				try {
					resultSet.next();
					result = Long.valueOf(resultSet.getLong("SEQ"));
				} finally {
					resultSet.close();
				}
			} finally {
				stmt.close();
			}
		} catch (SQLException exc) {
			throw new RepositoryException("Error getting Oracle sequence nextval", exc);
		} finally {
			if (localConn != null) {
				localConn.close();
			}
		}
		return result;
	}

	/**
	 * @param value value to convert
	 */
	@Override
	public String convertStringToSQL(final String value) {
		return STRING_PREFIX + value;
	}

	/**
	 * @param value Value
	 * @param columnName Column name
	 */
	@Override
	public String convertStringFromSQL(final String value, final String columnName) throws RepositoryException {
		if (value == null || "SNIPPET".equalsIgnoreCase(columnName)) {
			return value;
		} else {
			if (LOG.isDebugEnabled()) {
				if (value.indexOf(STRING_PREFIX) != 0) {
					throw new RepositoryException("Incorrect string prefix in database");
				}
			}
			return value.substring(1);
		}
	}

	/**
	 * Adds condition to limit returned result count.
	 */
	private static final String LIMIT_ROWNUM_ALIAS = "ROWNUM_ ";
	private static final String LIMIT_ROWNUM_DEF = ", ROWNUM ".concat(LIMIT_ROWNUM_ALIAS);

	/**
	 * Limits result
	 * @param querySelect Query select
	 * @param startFrom Start from
	 * @param limit Limit
	 * @param hasForUpdate To update?
	 * @param params Parameters
	 */  
	public Long[] limitResults(final StringBuilder querySelect, final int startFrom, final int limit, 
			final boolean hasForUpdate, final List<Object> params) {
		// In Oracle first row has number 1 in opposite to HSQL for example
		// where rows numbering starts from 0
		int localStartFrom = (startFrom < 1) ? 1 : startFrom + 1;

		int obIdx = querySelect.lastIndexOf("ORDER BY"); // last because FTS
														 // subquery can be
														 // ordered too

		final StringBuilder result = new StringBuilder();
		String originalQuery = querySelect.toString();
		String originalQueryU = originalQuery.toUpperCase();

		if (obIdx > 0 && hasForUpdate) {
			int fromIndex = originalQueryU.indexOf("FROM");

			String select = originalQuery.substring(0, fromIndex);
			String selectU = originalQueryU.substring(0, fromIndex);
			int distinctIndex = selectU.indexOf("DISTINCT");
			if (distinctIndex > 0) {
				throw new RuntimeException("DISTINCT cannot be used in queries marked FOR UPDATE");
			}

			int whereIndex = originalQueryU.indexOf("WHERE", fromIndex);

			int tableNameStart = fromIndex + 5;
			int tableNameEnd = whereIndex - 1;
			int fromClauseEnd = whereIndex - 1;
			int res = originalQuery.indexOf("LEFT", tableNameStart);
			if (res > tableNameStart && res < tableNameEnd)
				tableNameEnd = res;

			res = originalQuery.indexOf("JOIN", tableNameStart);
			if (res > tableNameStart && res < tableNameEnd)
				tableNameEnd = res;

			res = originalQuery.indexOf(',', tableNameStart);
			if (res > tableNameStart && res < tableNameEnd)
				tableNameEnd = res;

			String tableName = originalQuery.substring(tableNameStart, tableNameEnd).trim();
			String fromClause = originalQuery.substring(tableNameStart, fromClauseEnd).trim();
			int aliasIdx = tableName.lastIndexOf(' ');
			String mainTableAlias = aliasIdx < 0 ? "" : tableName.substring(aliasIdx).concat(".");

			int orderByIndex = originalQueryU.lastIndexOf("ORDER BY");

			String whereStatement = originalQuery.substring(whereIndex, orderByIndex);
			String fullFromStatement = originalQuery.substring(fromIndex, whereIndex);
			String orderBy = originalQuery.substring(orderByIndex);

			result.append(select).append(" FROM ").append(fromClause).append(" WHERE " + mainTableAlias + "ROWID IN ( SELECT rid__ FROM ( SELECT ").append(
					mainTableAlias).append("ROWID rid__, ROW_NUMBER() OVER(").append(orderBy).append(") row_num ").append(fullFromStatement).append(
					whereStatement).append(") WHERE row_num >=? AND row_num < ? ) ").append(orderBy);

			// */
			/*
			 * String originalQuery = querySelect.toString(); String
			 * originalQueryU = originalQuery.toUpperCase(); int resultStart =
			 * originalQueryU.indexOf("SELECT")+6; int resultEnd =
			 * originalQueryU.indexOf("FROM"); String result =
			 * originalQuery.substring(resultStart,resultEnd);
			 * querySelect.replace(resultStart, resultEnd, " ROWID ROW_ID ");
			 * querySelect.insert(0, "SELECT ROW_ID FROM ( ");
			 * querySelect.append(" ) WHERE ROWNUM >= ? AND ROWNUM <= ?");
			 * 
			 * 
			 * 
			 * resultStart = originalQueryU.indexOf("FROM ")+5; int whereStart
			 * =originalQueryU.indexOf("WHERE", resultStart); int p1 =
			 * originalQueryU.indexOf("LEFT ",resultStart); if (p1 < whereStart
			 * && p1 > 0){ whereStart = p1; } p1 =
			 * originalQueryU.indexOf("JOIN ",resultStart); if (p1 < whereStart
			 * && p1 > 0 ){ whereStart = p1; } resultEnd = resultStart; char[]
			 * cc = originalQuery.toCharArray(); while (true){ if (cc[resultEnd]
			 * == ','){ resultEnd --; break; } resultEnd++; if (resultEnd ==
			 * whereStart){ resultEnd --; break; } } String tableName =
			 * originalQuery.substring(resultStart, resultEnd);
			 * 
			 * 
			 * querySelect.insert(0,
			 * "SELECT  "+result+" FROM "+tableName+" WHERE ROWID IN (");
			 * querySelect.append(")");
			 * 
			 * resultStart = originalQueryU.indexOf("ORDER BY"); String order =
			 * originalQuery.substring(resultStart);
			 * 
			 * querySelect.append(order); //
			 */
		} else {
			result.append("SELECT * FROM ( SELECT rows__.*").append(LIMIT_ROWNUM_DEF).append(" FROM (");

			result.append(querySelect);
			result.append(" ) rows__ ) WHERE ").append(LIMIT_ROWNUM_ALIAS).append(" >=? ");

			if (limit > 0) {
				result.append(" AND ").append(LIMIT_ROWNUM_ALIAS).append(" < ?");
			}
		}

		querySelect.delete(0, querySelect.length());
		querySelect.append(result);

		if (params != null) {
			params.add(localStartFrom);
			if (limit > 0) {
				params.add(localStartFrom + limit);
			}
		}

		return new Long[] { Long.valueOf(localStartFrom), Long.valueOf(localStartFrom + limit) };// default
																			   // is
																			   // true
	}

	/**
	 * Applies + ALL_ROWS Oracle hint to main SELECT. Helpfull in case Order BY
	 * is required. Otherwise correct indexes aren't used.
	 * @param sql SQL buffer
	 */
	@Override
	public void applyHints(final StringBuilder sql) {
		final int selectIdx = "SELECT".length();
		final String hint = " /*+ ALL_ROWS */";

		sql.insert(selectIdx, hint);
	}

	/**
	 * Returns Data type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return SQL date converted
	 */
	@Override
	public StringBuilder getDateColumnToStringConversion(final String columnName) {
		return new StringBuilder().append("TO_CHAR(").append(columnName).append(", 'YYYY-MM-DD HH24:MI:SS')");
	}

	/**
	 * Returns Long type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	@Override
	public StringBuilder getLongColumnToStringConversion(final String columnName) {
		return getDoubleColumnToStringConversion(columnName);
	}

	/**
	 * Returns Double type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return Converted value
	 */
	@Override
	public StringBuilder getDoubleColumnToStringConversion(String columnName) {
		return new StringBuilder().append("TO_CHAR(").append(columnName).append(")");

		/*
		 * 
		 * 
		 * decode( sign(DOUBLE_VALUE) , 0, '1' , -1, '0' ||
		 * translate(substr(to_char
		 * (abs(DOUBLE_VALUE),'9.999999999999999EEEE'),3) ,'0.' ,'0') , '2' ||
		 * translate(substr(to_char( DOUBLE_VALUE, '9.999999999999999EEEE'),3)
		 * ,'0.' ,'0') )
		 */
		// return new StringBuilder()
		// .append("DECODE( SIGN(").append(columnName)
		// .append("), 0, '1', -1, '0', || TRANSLATE(SUBSTR(TO_CHAR(ABS(").append(columnName)
		// .append("),'9.999999999999999EEEE'),3) ,'0.' ,'0') , '2' || TRANSLATE(SUBSTR(TO_CHAR(")
		// .append(columnName)
		// .append(", '9.999999999999999EEEE'),3) ,'0.' ,'0')")
		// ;
		//        
	}

	/**
	 * Returns Boolean type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	@Override
	public StringBuilder getBooleanColumnToStringConversion(String columnName) {
		return new StringBuilder().append("TO_CHAR(").append(columnName).append(", '9')");
	}

	/**
	 * @param value value
	 * @return 1 if true 0 otherwise
	 */
	@Override
	public Object convertToDBBoolean(final Boolean value) {
		return value.booleanValue() ? "1" : "0";
	}

	/**
	 * Converts Boolean DB to Java boolean
	 * @param object
	 * @return 
	 */
	@Override
	public Boolean convertFromDBBoolean(final Object object) {
		if (object == null) {
			return Boolean.FALSE;
		}

		if (object instanceof Boolean) {
			return (Boolean) object;
		}

		if (object instanceof String) {
			String val = (String) object;
			return (val.length() == 1 && val.charAt(0) == '1') ? Boolean.TRUE : Boolean.FALSE;
		}

		return Boolean.FALSE;

	}

	@Override
	public void checkIdGeneratorInfrastracture(final DatabaseConnection conn) throws RepositoryException {
		try {
			conn.execute("ALTER SEQUENCE " + DEFAULT_SEQUENCE_NAME + " CACHE 50  INCREMENT BY " + DEFAULT_ID_RANGE);
		} catch (RepositoryException e) {
			throw new UnsupportedOperationException("Sequence altering");
		}
	}

	public String getDatabaseVendor() {
		return VENDOR_ORACLE;
	}

	// TODO to be removed
	@Override
	public List<String> getStoredProcedures() {
		return null;
	}
	
	@Override
	public void beforeInitializeDatabase(final DatabaseConnection conn) throws RepositoryException {
	}
	
	@Override
	public DatabaseConnection afterInitializeDatabase(final DatabaseConnection conn, final Map config) throws RepositoryException {
		// recompile current schema
		// LogUtils.debug(log, "Recompilation of all JCR schema");
		// conn.execute("BEGIN DBMS_UTILITY.COMPILE_SCHEMA(USER); END;");
		return conn;
	}

	@Override
	protected void addFullTextSearchIndex(final IndexDefinition indexDef, final StringBuffer buffer) throws RepositoryException {
		buffer.append(" INDEXTYPE IS ctxsys.context");
		for (final DBObjectDef item : specificDBObjectDefs) {
			if (item instanceof OracleTextPreferenceDef) {
				OracleTextPreferenceDef j = (OracleTextPreferenceDef) item;
				if (j.isApplicableForIndex(indexDef.getName())) {
					buffer.append(" PARAMETERS('DATASTORE " + j.getPrefName() + "')");
					LogUtils.debug(LOG, "Adding OracleText DATASTORE preference '" + j.getPrefName() + "' to OracleText index '" + indexDef.getName() + "'");
				}
			}
			if (item instanceof OracleTextSectionGroupDef) {
				OracleTextSectionGroupDef j = (OracleTextSectionGroupDef) item;
				if (j.isApplicableForIndex(indexDef.getName())) {
					buffer.append(" PARAMETERS('SECTION GROUP " + j.getSgpName() + "')");
					LogUtils.debug(LOG, "Adding OracleText SECTION GROUP setting '" + j.getSgpName() + "' to OracleText index '" + indexDef.getName() + "'");
				}
			}
		}
	}
	@Override
	public boolean isFTSSupported() {
		return true;
	}

	@Override
	public FTSQueryBuilder getFTSBuilder_() {
		// FTSQueryBuilder is thought as stateless .. thus single instance can
		// be used ... probably ...
		return new OracleFTSQueryBuilder();
	}

	@Override
	public DropSQLProvider getDropProvider(final Map config) throws RepositoryException {
		return new DropOracleSQLObjects();
	}

	public String getJDBCDriverName() {
		return "oracle.jdbc.OracleDriver";
	}

	// @Override
	// public BasicSecurityFilter getSecurityFilter() {
	// return new OracleSecurityFilter();
	// }

	@Override
	public BasicSecurityFilter getSecurityFilter() {
		return new OracleSecurityFilter();
	}

	/*
	 * @Override public BLOBInsertStatement createBLOBInsertStatement(String
	 * tableName, long pkValue, InputStream data) throws RepositoryException {
	 * return new OracleBLOBInsertStatement(tableName, pkValue, data); }//
	 */

	@Override
	protected int getMaxTableLength() {
		return 29;
	}

	@Override
	protected int getMaxColumnLength() {
		return 29;
	}
	
	@Override
	public List<TableDefinition> getSpecificTableDefs(final boolean supportFTS) throws RepositoryException {
		final ArrayList<TableDefinition> result = new ArrayList<TableDefinition>();

		if (supportFTS) {
			final TableDefinition stage = new TableDefinition(TABLE_FTS_STAGE, true);
			stage.addColumn(new ColumnDefinition(stage, FIELD_BLOB, Types.BLOB));
			stage.addColumn(new ColumnDefinition(stage, TABLE_FTS_STAGE__FILENAME, Types.VARCHAR));
			result.add(stage);

			final TableDefinition stageConv = new TableDefinition(TABLE_FTS_STAGE_CONV, true);
			final ColumnDefinition stageConvBlob = new ColumnDefinition(stageConv, FIELD_BLOB, Types.CLOB);
			stageConv.addColumn(stageConvBlob);
			result.add(stageConv);

			final IndexDefinition idf = new IndexDefinition(stageConv);
			idf.setName(TABLE_FTS_STAGE_CONV.toUpperCase() + "_IDX_FTS");
			idf.addColumn(stageConvBlob);
			idf.setFullTextSearch(true);
			stageConv.addIndexDefinition(idf);
		}

		return result;
	}

	/**
	 * Get internal ORACLE USER_ID
	 * @param conn connection
	 * @return USER_ID
	 */
	protected Long getOracleUserID(final DatabaseConnection conn) throws RepositoryException {
		PreparedStatement stmt;
		ResultSet resultSet;
		Long id;
		try {
			stmt = conn.prepareStatement("SELECT USER_ID FROM USER_USERS WHERE USERNAME=USER", false);
			try {
				resultSet = stmt.executeQuery();
				try {
					resultSet.next();
					id = resultSet.getLong(1);
				} finally {
					resultSet.close();
				}
			} finally {
				stmt.close();
			}
		} catch (SQLException e) {
			throw new RepositoryException("SQL error detecting Oracle USER_ID", e);
		}
		return id;
	}
	
	protected String getOracleFTSDynamicUncompressProcedure(final DatabaseConnection conn) throws RepositoryException {
		return ORA_DYN_UNCOMPRESS_PROC + "_" + getOracleUserID(conn);
	}

	@Override
	protected List<DBObjectDef> _getSpecificDBObjectDefs(final DatabaseConnection conn, final Map config) throws RepositoryException {
		final ArrayList<DBObjectDef> result = new ArrayList<DBObjectDef>();

		final HashMap<String, String> subst = new HashMap<String, String>();
		subst.put("CLOBGZIP-JAVA", ORA_LOB_COMPRESS_JAVA_CLASS);
		subst.put("SECURITY-PKG", ORA_SECURITY_PKG);
		subst.put("CONVERT-PROC", ORA_FTS_CONVERT_FUNC);
		subst.put("ZIP-PROC", ORA_FTS_ZIP_FUNC);
		subst.put("DEST-TABLE", TABLE_FTS_DATA);
		subst.put("DEST-ID", FIELD_ID);
		// TODO column name should be taken from Constants
		subst.put("DEST-BLOB", Constants.FIELD_FTS_DATA_XYZ);
		// TODO 'Constants.FTS_INDEX_NAME' is due to tmp coexistence of two
		// columns/FTS indexes in one table
		subst.put("DEST-INDEX", Constants.FTS_INDEX_NAME);
		subst.put("CONV-TABLE", TABLE_FTS_STAGE_CONV);
		subst.put("CONV-INDEX", TABLE_FTS_STAGE_CONV.toUpperCase() + "_IDX_FTS");
		subst.put("CONV-ID", FIELD_ID);
		subst.put("CONV-CLOB", FIELD_BLOB);
		subst.put("SRC-TABLE", TABLE_FTS_STAGE);
		subst.put("SRC-ID", FIELD_ID);
		subst.put("SRC-BLOB", FIELD_BLOB);
		subst.put("JCR_FTS_UTL_PKG", ORA_FTSUTIL_PKG);

		subst.put("RC-OK", String.valueOf(RC_FTS_CONV_OK));
		subst.put("RC-NO-ROWS", String.valueOf(RC_FTS_CONV_NO_ROWS));
		subst.put("RC-TOO-MANY-ROWS", String.valueOf(RC_FTS_CONV_TOO_MANY_ROWS));
		subst.put("RC-UPDATE-ERR", String.valueOf(RC_FTS_CONV_UPDATE_ERR));
		subst.put("RC-DELETE-ERR", String.valueOf(RC_FTS_CONV_DELETE_ERR));
		subst.put("RC-COMPRESS-ERR", String.valueOf(RC_FTS_CONV_COMPRESS_ERR));
		subst.put("RC-EXTRACT-ERR", String.valueOf(RC_FTS_CONV_EXTRACT_ERR));

		subst.put("ERR-TABLE", Constants.TABLE_FTS_INDEXING_ERROR);
		subst.put("ERR-ID-COL", Constants.FIELD_ID);
		subst.put("ERR-CODE-COL", Constants.TABLE_FTS_INDEXING__ERROR_CODE);
		subst.put("ERR-TYPE-COL", Constants.TABLE_FTS_INDEXING__ERROR_TYPE);
		subst.put("ERR-MSG-COL", Constants.TABLE_FTS_INDEXING__COMMENT);
		subst.put("ERR-MSG-MAX-LEN", "254");
		subst.put("LOG-FTS-ERR-PROC", ORA_FTS_ERR_LOG_PROC);

		subst.put("PROCESS-STAGE-RECORD-PROC", ORA_FTS_REC_PROCESS_FUNC);
		subst.put("ERRT-TXT-EXTR-FAIL", "." + FTSCommand.ERROR_TYPE_TXT_EXTRACTION);

		final boolean supportFTS = "true".equals(config.get(Constants.PROPERTY_SUPPORT_FTS));

		final OraclePackageDef securityPackage = new OraclePackageDef(ORA_SECURITY_PKG, getResourceAsStream("sql/SecurityPackage.sql"));
		securityPackage.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
		securityPackage.setSubstitutionMap(subst);
		result.add(securityPackage);

		if (supportFTS) {
			// some steps should be done in different schema (OracleText owner)
			// in the same database and
			// we need to know the schema where the JCR is installed too
			String ctxSysURL = conn.getDatabaseURL(); // (String)config.get(PROPERTY_DATASOURCE_URL);
			if (ctxSysURL == null) {
				ctxSysURL = (String) config.get(PROPERTY_DATASOURCE_URL);
			}
			if (ctxSysURL == null) {
				throw new RepositoryException("URL to database cannot be found, please specify it in repository configuration");
			}

			String ctxSysUser = ORATXT_SCHEMA;
			String ctxSysPass = (String) config.get(PROPERTY_ORACLE_CTXSYS_PASSWORD);
			String jcrSchemaUser = conn.getUserName(); // (String)config.get(PROPERTY_DATASOURCE_USER);
			// to be able install more than one JCR schema in one Oracle
			// database
			// we need ability to install multiple dynamic unpack procedures in
			// CTXSYS schema
			// so the name of this procedure should be unique within CTXSYS
			// schema
			String dynUncompressProcName = getOracleFTSDynamicUncompressProcedure(conn);

			subst.put("JCR-SCHEMA", jcrSchemaUser);
			subst.put("DYN-UNPACK-FOR-FTS", dynUncompressProcName);

			String[] ctxSysObjs = { "CTX_DOC", "CTX_DDL" };
			for (int i = 0; i < ctxSysObjs.length; i++) {
				OracleObjectPrivilegeDef priv = new OracleObjectPrivilegeDef(ctxSysObjs[i], jcrSchemaUser, "EXECUTE");
				priv.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_STAGE_CONV);
				priv.setConnectionParameters(ctxSysURL, ctxSysUser, ctxSysPass);
				result.add(priv);
			}

			OracleTextSectionGroupDef stageSgp = new OracleTextSectionGroupDef(ORATXT_STAGE_CONV_SGP, "BASIC_SECTION_GROUP");
			stageSgp.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_STAGE_CONV);
			stageSgp.registerIndexWhereToUse(TABLE_FTS_STAGE_CONV.toUpperCase() + "_IDX_FTS");
			result.add(stageSgp);

			OracleStoredJavaClassDef clobGzip_ = new OracleStoredJavaClassDef(ORA_LOB_COMPRESS_JAVA_CLASS, getResourceAsStream("sql/ClobGzipJava.sql"));
			clobGzip_.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
			clobGzip_.setSubstitutionMap(subst);
			result.add(clobGzip_);

			OraclePackageDef ftsUtilPkg = new OraclePackageDef(ORA_FTSUTIL_PKG, getResourceAsStream("sql/JcrFtsUtil9.sql"));
			ftsUtilPkg.setSubstitutionMap(subst);
			ftsUtilPkg.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
			result.add(ftsUtilPkg);

			OracleStoredProcedureDef unpackForFTS = new OracleStoredProcedureDef(dynUncompressProcName, getResourceAsStream("sql/DynUnpack.sql"));
			unpackForFTS.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
			unpackForFTS.setSubstitutionMap(subst);
			result.add(unpackForFTS);

			OracleObjectPrivilegeDef p1 = new OracleObjectPrivilegeDef(dynUncompressProcName, ctxSysUser, "EXECUTE");
			p1.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
			result.add(p1);

			OracleStoredProcedureDef unpackForFTSWrapper = new OracleStoredProcedureDef(dynUncompressProcName, getResourceAsStream("sql/DynUnpackWrap.sql"));
			unpackForFTSWrapper.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
			unpackForFTSWrapper.setSubstitutionMap(subst);
			unpackForFTSWrapper.setConnectionParameters(ctxSysURL, ctxSysUser, ctxSysPass);
			result.add(unpackForFTSWrapper);

			ctxSysObjs = new String[] { dynUncompressProcName };
			for (int i = 0; i < ctxSysObjs.length; i++) {
				OracleObjectPrivilegeDef priv = new OracleObjectPrivilegeDef(ctxSysObjs[i], jcrSchemaUser, "EXECUTE");
				priv.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
				priv.setConnectionParameters(ctxSysURL, ctxSysUser, ctxSysPass);
				result.add(priv);
			}

			OracleTextPreferenceDef jcrPref = new OracleTextPreferenceDef(ORATXT_DYN_UNCOMPRESS_PREF, "USER_DATASTORE");
			jcrPref.addAttribute("PROCEDURE", dynUncompressProcName);
			jcrPref.addAttribute("OUTPUT_TYPE", "CLOB");
			jcrPref.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_FTS_DATA);
			// TODO Constants.FTS_INDEX_NAME is due to tmp coexistence of two
			// columns/FTS indexes in one table
			jcrPref.registerIndexWhereToUse(Constants.FTS_INDEX_NAME);
			result.add(jcrPref);

			OracleStoredProcedureDef logProc = new OracleStoredProcedureDef(ORA_FTS_ERR_LOG_PROC, getResourceAsStream("sql/LogFTSProcErr.sql"));
			logProc.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
			logProc.setSubstitutionMap(subst);
			result.add(logProc);

			// OracleStoredFunctionDef proc1=new
			// OracleStoredFunctionDef(ORA_FTS_CONVERT_FUNC,
			// getResourceAsStream("sql/ConvertAndMove.sql"));
			// proc1.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
			// proc1.setSubstitutionMap(subst);
			// result.add(proc1);

			// OracleStoredFunctionDef proc2=new
			// OracleStoredFunctionDef(ORA_FTS_ZIP_FUNC,
			// getResourceAsStream("sql/ZipAndMove.sql"));
			// proc2.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
			// proc2.setSubstitutionMap(subst);
			// result.add(proc2);

			OracleStoredFunctionDef func0 = new OracleStoredFunctionDef(ORA_FTS_REC_PROCESS_FUNC, getResourceAsStream("sql/ProcessStageRecord.sql"));
			func0.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
			func0.setSubstitutionMap(subst);
			result.add(func0);

			OracleStoredFunctionDef func1 = new OracleStoredFunctionDef(ORA_FTS_CONVERT_FUNC, "CREATE FUNCTION " + ORA_FTS_CONVERT_FUNC
					+ "(p_id IN NUMBER) RETURN NUMBER AS " + "BEGIN RETURN " + ORA_FTS_REC_PROCESS_FUNC + "(p_id,'."
					+ FTSCommand.ERROR_CODE_TXT_CONVERT_AND_MOVE_FAILED + "',0);END;");
			func1.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
			result.add(func1);

			OracleStoredFunctionDef func2 = new OracleStoredFunctionDef(ORA_FTS_ZIP_FUNC, "CREATE FUNCTION " + ORA_FTS_ZIP_FUNC
					+ "(p_id IN NUMBER) RETURN NUMBER AS " + "BEGIN RETURN " + ORA_FTS_REC_PROCESS_FUNC + "(p_id,'."
					+ FTSCommand.ERROR_CODE_TXT_ZIP_AND_MOVE_FAILED + "',1);END;");
			func2.setPositionInObjectList(DBOBJ_POS_AFTER_ALL);
			result.add(func2);
		}

		addTrigger(result, subst, Constants.TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE, 5);

		return result;
	}

	protected void addTrigger(final ArrayList<DBObjectDef> result, final HashMap<String, String> subst, 
			final String columnName, final int pos) throws RepositoryException {
		HashMap<String, String> subst2 = new HashMap<String, String>();
		subst2.putAll(subst);
		subst2.put("TRIGGER_NAME", DB_TRIGGER_UNSTRUCTURED + pos);
		subst2.put("TABLE-UNSTRUCTURED-PROP", Constants.TABLE_NODE_UNSTRUCTURED);
		subst2.put("COLUMN", columnName);
		OracleTriggerDef trigerLong = new OracleTriggerDef(DB_TRIGGER_UNSTRUCTURED + pos, getResourceAsStream("sql/TrigerUnstructured.sql"));
		trigerLong.setSubstitutionMap(subst2);
		trigerLong.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_NODE_UNSTRUCTURED);
		result.add(trigerLong);

		subst2 = new HashMap<String, String>();
		subst2.putAll(subst);
		subst2.put("TRIGGER_NAME", DB_TRIGGER_UNSTRUCTURED_MULTIPLE + pos);
		subst2.put("TABLE-UNSTRUCTURED-PROP", Constants.TABLE_NODE_UNSTRUCTURED_VALUES);
		subst2.put("COLUMN", columnName);
		trigerLong = new OracleTriggerDef(DB_TRIGGER_UNSTRUCTURED_MULTIPLE + pos, getResourceAsStream("sql/TrigerUnstructured.sql"));
		trigerLong.setSubstitutionMap(subst2);
		trigerLong.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE, TABLE_NODE_UNSTRUCTURED_VALUES);
		result.add(trigerLong);
	}

	@Override
	public boolean isMIMETypeSupported(final String MIMEType) {
		// MIME types supported by Oracle (abridged)
		final List<String> types = Arrays.asList("text/plain", "application/msword", "application/excel", "application/vnd.ms-excel",
				"application/vndms-excel", "application/x-excel", "application/x-msexcel", "application/powerpoint", "application/mspowerpoint",
				"application/vnd.ms-powerpoint", "text/html", "application/pdf", "image/tiff", "text/richtext", "text/rtf");
		return types.contains(MIMEType);
	}

	@Override
	public void populateBlobData(final Blob ph, final InputStream value) throws SQLException, RepositoryException {
		try {
			
			final OutputStream writer = ph.setBinaryStream(0L);

			int bSize = 8*1024;
			byte[] b = new byte[bSize];
			int read;

			while ((read = value.read(b)) > 0) {
				writer.write(b, 0, read);
			}
			writer.close();
		} catch (IOException exc) {
			throw new RepositoryException(exc);
		}
	}

	/**
	 * Oracle needs a special call to update FTS index
	 * 
	 * @param conn
	 * @throws RepositoryException
	 */
	public void updateFTSIndexes(DatabaseConnection conn) throws RepositoryException {

		// get database time before start of sync
		String syncStarted = null;
		try {
			PreparedStatement getDBTime = conn.prepareStatement("SELECT TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS') FROM DUAL", true);
			try {
				ResultSet resultSet = getDBTime.executeQuery();
				try {
					if (resultSet.next()) {
						syncStarted = resultSet.getString(1);
					} else {
						LOG.error("Cannot get SYSDATE from Oracle database");
					}
				} finally {
					resultSet.close();
				}
			} finally {
				getDBTime.close();
			}
		} catch (SQLException e) {
			LOG.error("SQL error getting SYSDATE from Oracle database: " + e.getMessage());
		} 

		// TODO index name Constants.FTS_INDEX_NAME should be replaced with
		// constant
		// TODO check impact of adding the mem parameter to SYNC_INDEX call
		// long start=System.currentTimeMillis();
		conn.execute("BEGIN COMMIT; CTX_DDL.SYNC_INDEX('" + Constants.FTS_INDEX_NAME + "','25M'); END;");
		// log.debug("OracleText index SYNC took "+(System.currentTimeMillis()-start)+" msec");

		// check for new errors in OracleText view CTX_USER_INDEX_ERRORS for our
		// index since start of sync
		// TODO index name Constants.FTS_INDEX_NAME should be replaced with
		// constant
		PreparedStatement stmt = null;
		ResultSet resultSet = null;
		if (syncStarted != null) {
			try {
				stmt = conn.prepareStatement("SELECT TO_CHAR(ERR_TIMESTAMP,'YYYYMMDDHH24MISS'),ERR_TEXTKEY,ERR_TEXT"
						+ " FROM CTX_USER_INDEX_ERRORS WHERE ERR_INDEX_NAME='" + Constants.FTS_INDEX_NAME + "'"
						+ " AND ERR_TIMESTAMP>=TO_DATE(?,'YYYYMMDDHH24MISS')", false);
				stmt.setString(1, syncStarted);
				resultSet = stmt.executeQuery();
				int errorCount = 0;
				int maxDisplay = 3;
				while (resultSet.next()) {
					if (errorCount == 0) {
						LOG.error("OracleText indexing error(s) detected during sync: ");
					}
					if (errorCount < maxDisplay) {
						LOG.error("Time: " + resultSet.getString(1) + " Rowid: " + resultSet.getString(2) + " Error: " + resultSet.getString(3));
					}
					errorCount++;
				}
				if (errorCount > 0 && errorCount > maxDisplay) {
					LOG.error("First " + maxDisplay + " errors of " + errorCount + " logged. To see all errors use 'SELECT * FROM CTX_USER_INDEX_ERRORS'");
				}
			} catch (SQLException e) {
				LOG.error("SQL error reading CTX_USER_INDEX_ERRORS: " + e.getMessage());
			} finally {
				try {
					resultSet.close();
					stmt.close();
				} catch (SQLException e) {/* do nothing */
				}
			}
		}
	}

	@Override
	public IndexingProcessor getIndexingProcessor() {
		return new OracleIndexingProcessor();
	}

	@Override
	public void sessionSetup(final Connection conn) throws SQLException {
		Statement st = conn.createStatement();
		st.execute("alter session set hash_join_enabled=false");
		st.execute("alter session set optimizer_index_caching=25");
		st.close();

	}
	
	/**
	 * @param table Table definition 
	 */
	public String[] buildDropTableStatement(final TableDefinition table) throws RepositoryException {
		final ArrayList<String> sqls = new ArrayList<String>();
		sqls.add("DROP TABLE " + convertTableName(table.getTableName()) + " cascade constraints");
		return sqls.toArray(new String[sqls.size()]);
	}

	/**
	 * @param conn Connection
	 * @return Schema name
	 */
	public String getSchemaName(final DatabaseConnection conn) throws RepositoryException, SQLException {
		return conn.getDatabaseName();
	}

	@Override
	protected void appendIOTClauseWithinColumnList(final StringBuffer sb, final TableDefinition tableDef) {
		sb.append(", CONSTRAINT ").append(tableDef.getTableName()).append("_PK PRIMARY KEY(");
		int count = 0;
		for (final Iterator<ColumnDefinition> iter = tableDef.getPKColumnIterator(); iter.hasNext();) {
			if (count++ > 0) {
				sb.append(',');
			}
			sb.append((iter.next()).getColumnName());
		}
		sb.append(')');
	}
	
	@Override
	protected void appendIOTClauseAfterColumnList(final StringBuffer sb, final TableDefinition tableDef) {
		sb.append(" ORGANIZATION INDEX ");
	}

	@Override
	public void validateUserName(final String username) throws RepositoryException {
		// http://www.idevelopment.info/data/Oracle/DBA_tips/Database_Administration/DBA_26.shtml
		_validateUserName(username, new String[] { "sys", "system", "ctxsys", "dbsnmp", "outln", 
				"mdsys", "ordsys", "ordplugins", "dssys", "perfstat",
				"wkproxy", "wksys", "wmsys", "xdb", "anonymous", "odm", 
				"odm_mtr", "olapsys", "tracesvr", "repadmin" });
	}

	public void addSecurityConditions(final JCRPrincipals principals, final DatabaseSelectAllStatement stmt, 
			final boolean allowBrowse, final String idColumn, final String securityIdColumn)
			throws RepositoryException {
		final StringBuffer groups = new StringBuffer("xxxJCR_CHECKGROUPxxx");
		for (final String group : principals.getGroupIdList()) {
			groups.append(',');
			groups.append(convertStringToSQL(group));
		}

		final StringBuffer contexts = new StringBuffer("xxxJCR_CHECKCONTEXTxxx");
		for (String context : principals.getContextIdList()) {
			contexts.append(',');
			contexts.append(convertStringToSQL(context));
		}

		final StoredProcedureDatabaseCondition condition = Conditions.storedProcedure("jcr.pread");
		condition.addVariable(idColumn);
		condition.addVariable(securityIdColumn);
		condition.addParameter(principals.getUserId());
		condition.addParameter(groups.toString());
		condition.addParameter(contexts.toString());
		condition.addParameter(allowBrowse);
		stmt.addCondition(Conditions.gt(condition, 0));
	}

	public boolean isResultCountSupported() {
		return true;
	}

	public void addResultCountToStatement(final DatabaseSelectAllStatement statement) {
		statement.addResultColumn("count(*) over () as RECORDCOUNT");
	}

	public String getDatabaseVersion() {
		return "9";
	}

}
