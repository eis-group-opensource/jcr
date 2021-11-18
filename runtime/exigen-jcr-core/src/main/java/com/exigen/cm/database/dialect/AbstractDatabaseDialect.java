/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.IndexDefinition;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.StreamUtils;
import com.exigen.cm.query.predicate.DefaultFTSQueryBuilder;
import com.exigen.cm.query.predicate.FTSQueryBuilder;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.vf.commons.logging.LogUtils;

/**
 * AbstractDatabaseDialect class
 */
public abstract class AbstractDatabaseDialect implements DatabaseDialect {

	private final Log log = LogFactory.getLog(AbstractDatabaseDialect.class);
	protected ConnectionProvider connectionProvider;
	protected List<DBObjectDef> specificDBObjectDefs;

	private Map<String, String> tableNames = new HashMap<String, String>();
	private Map<String, String> columnNames = new HashMap<String, String>();

	public void dropSequence(DatabaseConnection conn) {
	}

	public String buildCreateStatement(final TableDefinition tableDef) throws RepositoryException {
		if (tableDef.isAlter()) {
			return buildAlterStatement(tableDef);
		} else {
			return _buildCreateStatement(tableDef, tableDef.getColumnIterator());
		}
	}

	protected String buildAlterStatement(final TableDefinition tableDef) throws RepositoryException {
		return _buildCreateStatement(tableDef, tableDef.getColumnIterator());
	}

	public final String _buildCreateStatement(TableDefinition tableDef, 
			final Iterator<ColumnDefinition> columns) throws RepositoryException {
		final StringBuffer sb = new StringBuffer();
		if (tableDef.isAlter()) {
			sb.append("ALTER ");
		} else {
			sb.append("CREATE ");
			appendHSCreateTablePrefix(sb, tableDef);
		}
		sb.append("TABLE ");
		sb.append(convertTableName(tableDef.getTableName()));
		if (tableDef.isAlter()) {
			sb.append(" ADD ");
		}
		if (!tableDef.isAlter() || isAlterStatementBracketNeccesary()) {
			sb.append(" ( ");
		}
		boolean hasLobs = false;
		for (final Iterator<ColumnDefinition> it = columns; it.hasNext();) {
			ColumnDefinition columnDef = it.next();
			sb.append(columnDef.getColumnName());
			int colType = columnDef.getColumnType();
			if (colType == Types.BLOB || colType == Types.CLOB) {
				hasLobs = true;
			}
			sb.append(' ');
			sb.append(getSQLType(columnDef.getColumnType(), columnDef.getLength()));
			if (columnDef.isNotNull()) {
				sb.append(" NOT NULL ");
			}
			if (it.hasNext()) {
				sb.append(',');
			}
		}
		if (!tableDef.isAlter() && tableDef.isIndexOrganized()) {
			appendIOTClauseWithinColumnList(sb, tableDef);
		}
		if (!tableDef.isAlter() || isAlterStatementBracketNeccesary()) {
			sb.append(" )");
		}
		if (!tableDef.isAlter() && tableDef.isIndexOrganized()) {
			appendIOTClauseAfterColumnList(sb, tableDef);
		}
		if (hasLobs) {
			appendLobClauseAfterColumnList(sb, tableDef);
		}
		if (isSemicolumnAtEndNeccesary()) {
			sb.append(';');
		}

		return sb.toString();
	}

	protected boolean isAlterStatementBracketNeccesary() {
		return true;
	}

	protected void appendIOTClauseWithinColumnList(StringBuffer sb, TableDefinition tableDef) {
	}

	protected void appendIOTClauseAfterColumnList(StringBuffer sb, TableDefinition tableDef) {
	}

	protected void appendLobClauseAfterColumnList(StringBuffer sb, TableDefinition tableDef) {
	}

	protected void appendHSCreateTablePrefix(StringBuffer sb, TableDefinition tableDef) {
	}

	public String buildAlterTableStatement(String tableName, ColumnDefinition[] columnDefs) throws RepositoryException {
		// TODO use one mechanism together with buildCreateStatemenent(alter
		// mode)
		final StringBuffer sb = new StringBuffer();
		sb.append("ALTER TABLE ");
		sb.append(convertTableName(tableName));
		sb.append(" ADD ");
		for (int i = 0; i < columnDefs.length; i++) {
			final ColumnDefinition columnDef = columnDefs[i];
			// id varchar(32) NOT NULL
			sb.append(columnDef.getColumnName());
			sb.append(" ");
			sb.append(getSQLType(columnDef.getColumnType(), columnDef.getLength()));
			if (columnDef.isNotNull()) {
				sb.append(" NOT NULL ");
			}
			if (i != (columnDefs.length - 1)) {
				sb.append(",");
			}
		}
		if (isSemicolumnAtEndNeccesary()) {
			sb.append(";");
		}
		return sb.toString();
	}

	public String buildAlterTableModifyColumnStatement(String tableName, ColumnDefinition[] columnDefs) throws RepositoryException {
		final StringBuffer sb = new StringBuffer();
		sb.append("ALTER TABLE ");
		sb.append(convertTableName(tableName));
		sb.append(" ALTER COLUMN ");
		for (int i = 0; i < columnDefs.length; i++) {
			final ColumnDefinition columnDef = columnDefs[i];
			// id varchar(32) NOT NULL
			sb.append(columnDef.getColumnName());
			sb.append(' ');
			sb.append(getSQLType(columnDef.getColumnType(), columnDef.getLength()));
			if (columnDef.isNotNull()) {
				sb.append(" NOT NULL ");
			}
			if (i != (columnDefs.length - 1)) {
				sb.append(',');
			}
		}
		if (isSemicolumnAtEndNeccesary()) {
			sb.append(';');
		}
		return sb.toString();
	}

	public String buildAlterTableDropColumn(String tableName, String columnName) throws RepositoryException {
		final StringBuffer sb = new StringBuffer();
		sb.append("ALTER TABLE ");
		sb.append(convertTableName(tableName));
		sb.append(" DROP COLUMN ");
		sb.append(columnName);
		if (isSemicolumnAtEndNeccesary()) {
			sb.append(';');
		}
		return sb.toString();
	}

	public final String[][] buildCreateIndexStatements(TableDefinition tableDef) throws RepositoryException {
		ArrayList<String[]> queries = new ArrayList<String[]>();
		int index = 0;
		for (Iterator<IndexDefinition> iter = tableDef.getIndexeIterator(); iter.hasNext(); index++) {
			IndexDefinition indexDef = iter.next();
			String[] st = buildIndexStatement(indexDef, index++);
			if (st != null) {
				queries.add(st);
			}
		}
		return (String[][]) queries.toArray(new String[queries.size()][]);
	}

	protected String[] buildIndexStatement(IndexDefinition indexDef, int pos) throws RepositoryException {
		final StringBuffer sb = new StringBuffer();
		sb.append("CREATE ");
		if (indexDef.isUnique()) {
			sb.append("UNIQUE ");
		}
		sb.append("INDEX ");
		String indexName = null;
		if (indexDef.getName() == null) {
			indexName = convertIndexName(indexDef.getTableDefinition().getTableName() + "_i" + pos);
		} else {
			indexName = convertIndexName(indexDef.getName());
		}
		sb.append(indexName);
		sb.append(" ON ");
		sb.append(convertTableName(indexDef.getTableDefinition().getTableName()));
		sb.append(" (");
		for (final Iterator<ColumnDefinition> iterator = indexDef.getColumnIterator(); iterator.hasNext();) {
			final ColumnDefinition column = iterator.next();
			sb.append(column.getColumnName());
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(')');
		if (indexDef.isFullTextSearch()) {
			addFullTextSearchIndex(indexDef, sb);
		}
		return new String[] { convertIndexName(convertTableName(indexDef.getTableDefinition().getTableName()), indexName), sb.toString() };

	}

	protected String convertIndexName(String name) throws RepositoryException {
		return this.convertTableName(name);
	}

	public String convertProcedureName(String name) throws RepositoryException {
		return this.convertTableName(name);
	}

	public String convertConstraintName(String name) throws RepositoryException {
		return this.convertTableName(name);
	}

	protected String convertIndexName(String tableName, String indexName) {
		return indexName;
	}

	protected void addFullTextSearchIndex(IndexDefinition indexDef, StringBuffer sb) throws RepositoryException {
	}

	public String buildPKAlterStatement(TableDefinition tableDef) throws RepositoryException {
		// ALTER TABLE "Test11" ADD CONSTRAINT eee PRIMARY KEY (t1);
		StringBuffer sb = new StringBuffer();
		sb.append("ALTER TABLE ");
		sb.append(convertTableName(tableDef.getTableName()));
		sb.append(" ADD CONSTRAINT ");
		sb.append(convertIndexName(tableDef.getTableName() + "_PK"));
		sb.append(" PRIMARY KEY (");
		sb.append(getIdColumnNames(tableDef));
		sb.append(')');
		if (isSemicolumnAtEndNeccesary()) {
			sb.append(';');
		}

		return sb.toString();
	}

	public String buildFKAlterStatement(TableDefinition tableDef, DatabaseConnection conn) throws RepositoryException {
		StringBuffer sb = new StringBuffer();
		// ALTER TABLE "Test11" ADD CONSTRAINT tttt FOREIGN KEY (t1)
		// REFERENCES city (id) ON UPDATE RESTRICT ON DELETE RESTRICT;
		for (Iterator it = tableDef.getColumnIterator(); it.hasNext();) {
			ColumnDefinition columnDef = (ColumnDefinition) it.next();
			TableDefinition fkTable = columnDef.getForeignKey();
			if (fkTable != null) {
				sb.append("ALTER TABLE ");
				sb.append(convertTableName(tableDef.getTableName()));
				sb.append(" ADD CONSTRAINT ");
				String fkName = convertIndexName(tableDef.getTableName()) + "_" + columnDef.getColumnName() + "_" + convertIndexName(fkTable.getTableName())
						+ "_FK";
				// sb.append(convertTableName(tableDef.getTableName()));
				// sb.append("_");
				// sb.append(columnDef.getColumnName());
				// sb.append("_");
				// sb.append(convertTableName(fkTable.getTableName()));
				// sb.append("_FK FOREIGN KEY (");
				sb.append(fkName);
				sb.append(" FOREIGN KEY (");
				sb.append(columnDef.getColumnName());
				sb.append(") REFERENCES ");
				sb.append(convertTableName(fkTable.getTableName()));
				sb.append(" (");
				sb.append(getIdColumnNames(fkTable));
				sb.append(")  ON UPDATE ");
				sb.append(getAlterActionRestrict());
				sb.append(" ON DELETE ");
				sb.append(getAlterActionRestrict());
				sb.append(' ');
				if (isSemicolumnAtEndNeccesary()) {
					sb.append(';');
				}

				conn.registerSysObject(DatabaseObject.CONSTRAINT, convertTableName(tableDef.getTableName()) + "." + fkName, false);

			}
		}
		return sb.toString();
	}

	private String getIdColumnNames(TableDefinition tableDef) {
		StringBuffer sb = new StringBuffer();
		for (final Iterator<ColumnDefinition> it = tableDef.getPKColumnIterator(); it.hasNext();) {
			ColumnDefinition columnDef = it.next();
			sb.append(columnDef.getColumnName());
			if (it.hasNext()) {
				sb.append(',');
			}
		}
		return sb.toString();
	}

	protected String getSQLType(int columnType, int length) throws RepositoryException {
		if (columnType == Types.INTEGER) {
			return getColumnTypeLong();
		} else if (columnType == Types.VARCHAR) {
			StringBuffer sb = new StringBuffer(getColumnTypeString());
			sb.append('(');
			if (length == 0) {
				sb.append(getStringMaxLength());
			} else {
				sb.append(length);
			}
			sb.append(')');
			return sb.toString();
		} else if (columnType == Types.BOOLEAN) {
			return getColumnTypeBoolean();
		} else if (columnType == Types.TIMESTAMP) {
			return getColumnTypeTimeStamp();
		} else if (columnType == Types.FLOAT) {
			return getColumnTypeFloat();
		} else if (columnType == Types.CLOB) {
			return getColumnTypeClob();
		} else if (columnType == Types.BLOB) {
			return getColumnTypeBlob();
		} else if (columnType == Types.VARBINARY) {
			return getColumnTypeBlob();
		}
		throw new RepositoryException("Unsupported column type " + columnType);
	}

	protected int getStringMaxLength() {
		return 3999;
	}

	protected String getColumnTypeBlob() {
		return "BLOB";
	}

	protected String getColumnTypeClob() {
		return "CLOB";
	}

	public String getColumnTypeLong() {
		return "LONG";
	}

	public int getColumnTypeLongSQLType() {
		return Types.NUMERIC;
	}

	public String getColumnTypeString() {
		return "VARCHAR";
	}

	public int getColumnTypeStringSQLType() {
		return Types.VARCHAR;
	}

	public Long reserveIdRange(DatabaseConnection conn, Long range) throws RepositoryException {
		Long _result = null;
		if (isSequenceSupported()) {
			DatabaseConnection _conn = conn;
			if (conn == null) {
				_conn = connectionProvider.createConnection();
			}
			try {
				_result = reserveIdRangeSequence(_conn);
			} finally {
				if (conn == null) {
					_conn.close();
				}
			}
		} else {
			_result = reserveIdRangeTable(range);
		}
		return _result;
	}

	protected Long reserveIdRangeSequence(DatabaseConnection conn) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	protected Long reserveIdRangeTable(Long range) throws RepositoryException {

		// pause current transaction
		final JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
		boolean error = false;
		/*
		 * JCRTransactionManager trManager =
		 * TransactionHelper.getInstance().getTransactionManager(); if
		 * (trManager != null){ try { trManager.begin(); tr =
		 * TransactionHelper.getInstance
		 * ().getTransactionManager().getTransaction(); } catch (Exception e) {
		 * throw new RepositoryException(e); } }
		 */
		DatabaseConnection conn = connectionProvider.createConnection();
		Long current = null;
		try {
			lockTableRow(conn, Constants.TABLE_ID_GENERATOR, Constants.TABLE_ID_GENERATOR__NAME, Constants.DEFAULT_GENERATOR);
			final Map row = conn.loadRow(Constants.TABLE_ID_GENERATOR, Constants.TABLE_ID_GENERATOR__NAME, Constants.DEFAULT_GENERATOR);
			current = (Long) row.get(Constants.TABLE_ID_GENERATOR__NEXT_ID);
			if (current == null) {
				current = 0L;
			}

			// write to database next range
			final Long newValue = Long.valueOf(current.longValue() + range.longValue());
			final DatabaseUpdateStatement stmt = DatabaseTools.createUpdateStatement(Constants.TABLE_ID_GENERATOR, Constants.TABLE_ID_GENERATOR__NAME,
					Constants.DEFAULT_GENERATOR);
			stmt.addValue(SQLParameter.create(Constants.TABLE_ID_GENERATOR__NEXT_ID, newValue));
			stmt.execute(conn);
			conn.commit();
		} catch (ItemNotFoundException exc) {
			final DatabaseInsertStatement st = DatabaseTools.createInsertStatement(Constants.TABLE_ID_GENERATOR);

			st.addValue(SQLParameter.create(Constants.TABLE_ID_GENERATOR__NAME, Constants.DEFAULT_GENERATOR));
			st.addValue(SQLParameter.create(Constants.TABLE_ID_GENERATOR__NEXT_ID, Integer.valueOf(range.intValue() + 1)));

			st.execute(conn);
			conn.commit();
			return 0L;
		} finally {
			conn.close();
			if (tr != null) {
				if (error) {
					TransactionHelper.getInstance().rollbackAndResore(tr);
				} else {
					TransactionHelper.getInstance().commitAndResore(tr);
				}
				// TransactionHelper.getInstance().resumeTransaction(tr);
			}

		}

		/*
		 * if (trManager != null){ try { trManager.commit(); } catch (Exception
		 * e) { throw new RepositoryException(e); } }
		 */
		// TransactionHelper.getInstance().resumeTransaction(tr);
		return current;
	}

	public TableDefinition createIdGeneratorInfrastracture(DatabaseConnection conn) throws RepositoryException {
		if (isSequenceSupported()) {
			addSequence(conn);
			return null;
		} else {
			return addIdGeneratorTable(conn);
		}
	}

	protected void addSequence(DatabaseConnection conn) throws RepositoryException {
		throw new UnsupportedOperationException();
	}

	private TableDefinition addIdGeneratorTable(DatabaseConnection conn) throws RepositoryException {
		final TableDefinition idGeneratorTable = new TableDefinition(Constants.TABLE_ID_GENERATOR);
		idGeneratorTable.addColumn(new ColumnDefinition(idGeneratorTable, Constants.TABLE_ID_GENERATOR__NAME, Types.VARCHAR, true)).setLength(256);
		idGeneratorTable.addColumn(new ColumnDefinition(idGeneratorTable, Constants.TABLE_ID_GENERATOR__NEXT_ID, Types.INTEGER));

		// conn.createTables(new TableDefinition[]{idGeneratorTable});
		return idGeneratorTable;

		/*
		 * DatabaseInsertStatement st =
		 * DatabaseTools.createInsertStatement(Constants.TABLE_ID_GENERATOR);
		 * 
		 * st.addValue(SQLParameter.create(Constants.TABLE_ID_GENERATOR__NAME,
		 * Constants.DEFAULT_GENERATOR));
		 * st.addValue(SQLParameter.create(Constants
		 * .TABLE_ID_GENERATOR__NEXT_ID, new Integer(0)));
		 * 
		 * st.execute(conn);
		 */

	}

	public final void lockTableRow(DatabaseConnection conn, String tableName, String pkPropertyName, Object pkValue) throws RepositoryException {
		_lockRow(conn, convertTableName(tableName), pkPropertyName, pkValue);
	}

	abstract protected void _lockRow(DatabaseConnection conn, String tableName, String pkPropertyName, Object pkValue) throws RepositoryException;

	public final void lockTableRow(DatabaseConnection conn, String tableName, Object id) throws RepositoryException {
		DatabaseTools.lockTableRow(conn, convertTableName(tableName), "ID", id);
	}

	public boolean isSemicolumnAtEndNeccesary() {
		return false;
	}

	public String convertStringToSQL(String value) {
		return value;
	}

	public String convertStringFromSQL(String value, String columnName) throws RepositoryException {
		return value;
	}

	public Object convertToDBInteger(Integer value) {
		return value;
	}

	public Boolean convertFromDBBoolean(Object object) {
		return (Boolean) object;
	}

	/**
	 * Converts java Boolean to instance used to represent boolean in Database.
	 * 
	 * @param value
	 * @return
	 */
	public Object convertToDBBoolean(Boolean value) {
		return value;
	}

	public Object convertToDBLong(Long value) {
		return value;
	}

	/**
	 * Converts Number recieved from DB to java Long.
	 * 
	 * @param value
	 * @return
	 */
	public Long convertFromDBLong(Number value) {
		return Long.valueOf(value.longValue());
	}

	public Object convertToDBDate(Date value) {
		return new Timestamp(value.getTime());
		// return value;
	}

	public Object convertToDBDouble(Double value) {
		return value;
	}

	/**
	 * Returns count all statement
	 * 
	 * @return
	 */
	public String getCountAllStatement() {
		return "count(*)";
	}

	/**
	 * Returns LIKE parameter adjusted to correspond database specific.
	 * 
	 * @param parameter
	 *            is a parameter to adjust.
	 * @param escapeChar
	 *            is a character used to signify literal usage.
	 * @return
	 */
	public String adjustLikeParameter(String parameter, char escapeChar) {
		return parameter;
	}

	public void checkIdGeneratorInfrastracture(DatabaseConnection conn) throws RepositoryException {

	}

	/**
	 * Returns max number of parameters allowed for IN by specific database
	 * dialect.
	 * 
	 * @return max number of parameters for IN
	 */
	public int getInMaxParamsCount() {
		return 299;
	}

	protected InputStream getResourceAsStream(String resource) throws RepositoryException {
		final InputStream result = getClass().getResourceAsStream(resource);
		if (result == null) {
			throw new RepositoryException("Resource " + resource + " not found in " + getClass().getName());
		}
		return result;
	}

	public List<String> getStoredProcedures() {
		final ArrayList<String> result = new ArrayList<String>();
		try {
			final InputStream in = getResourceAsStream(getClass().getSimpleName() + ".sql");
			if (in != null) {
				byte bb[] = StreamUtils.getBytes(in);
				result.add(new String(bb));
			}
		} catch (Exception exc) {
			log.error(exc.getMessage(), exc);
		}
		return result;
	}

	public boolean isFTSSupported() {
		return false;
	}

	// public FTSQueryBuilder getFTSBuilder(){
	// return new DefaultFTSQueryBuilder();
	// }

	public FTSQueryBuilder getFTSBuilder_() {
		return new DefaultFTSQueryBuilder();
	}

	public void beforeInitializeDatabase(DatabaseConnection conn) throws RepositoryException {
	}

	public DropSQLProvider getDropProvider(Map config) throws RepositoryException {
		String msg = "Database drop provider missing for \"{0}\" dialect.";
		msg = MessageFormat.format(msg, new Object[] { getClass().getName() });
		LogUtils.error(log, msg);
		throw new RepositoryException(msg);
	}

	public DropSQLProvider getDropProvider2(Map<String, String> config) {
		return new DefaultDropProvider(config);
	}

	public DatabaseConnection afterInitializeDatabase(DatabaseConnection conn, Map config) throws RepositoryException {
		return conn;
	}

	public void checkConfiguration(Map configuration) throws RepositoryException {
		this.validateUserName((String) configuration.get(Constants.PROPERTY_DATASOURCE_USER));
	}

	public void checkInitDBConfiguration(Map config, boolean supportFTS) throws RepositoryException {

	}

	/**
	 * Returns default security filter implementation.
	 * 
	 * @return no-security filter.
	 */
	// public BasicSecurityFilter getSecurityFilter(){
	// return new BasicSecurityFilter();
	// }

	/**
	 * Returns implementation of dialect specific security filter for new query
	 * implementation.
	 * 
	 * @return
	 */
	public com.exigen.cm.query.BasicSecurityFilter getSecurityFilter() {
		return new com.exigen.cm.query.BasicSecurityFilter();
	}

	public void setConnectionProvider(ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;

	}

	/*
	 * public BLOBInsertStatement createBLOBInsertStatement(String tableName,
	 * long pkValue, InputStream data) throws RepositoryException { return new
	 * DefaultBLOBInsertStatement(tableName, pkValue, data); }//
	 */

	public String convertTableName(String tableName) throws RepositoryException {
		if (tableNames.containsKey(tableName)) {
			return tableNames.get(tableName);
		} else {
			String realTableName = tableName;
			String prefix = null;
			int pos = tableName.indexOf('.');
			if (pos >= 0) {
				prefix = tableName.substring(0, pos + 1);
				realTableName = tableName.substring(pos + 1);
			}

			String result = convertName(realTableName, getMaxTableLength());
			if (pos >= 0) {
				result = prefix + realTableName;
			}
			tableNames.put(tableName, result);
			return result;
		}
	}

	public String convertColumnName(String columnName) {
		if (columnNames.containsKey(columnName)) {
			return columnNames.get(columnName);
		} else {
			columnName = JCRHelper.excludeSpecialCharacters(columnName);
			String result = convertName(columnName, getMaxColumnLength());

			columnNames.put(columnName, result);
			return result;
		}
	}

	public String convertName(String name, int length) {
		if (name.length() > length) {
			int hc = name.hashCode();
			if (hc < 0) {
				hc = -hc;
			}
			String hashCode = Integer.toString(hc);
			final StringBuffer _name = new StringBuffer();
			_name.append(name.substring(0, length - hashCode.length() - 2));
			_name.append("_");
			_name.append(hashCode);
			return _name.toString().toUpperCase();
		} else {
			return name.toUpperCase();
		}
	}

	protected int getMaxTableLength() {
		return 100;
	}

	protected int getMaxColumnLength() {
		return 100;
	}

	public void initStopWords(DatabaseConnection connection, Map config) throws RepositoryException {

	}

	public List<TableDefinition> getSpecificTableDefs(boolean supportFTS) throws RepositoryException {
		return new ArrayList<TableDefinition>();
	}

	protected List<DBObjectDef> _getSpecificDBObjectDefs(DatabaseConnection conn, Map config) throws RepositoryException {
		return new ArrayList<DBObjectDef>();
	}

	public List<DBObjectDef> getSpecificDBObjectDefs(DatabaseConnection conn, Map config) throws RepositoryException {
		if (specificDBObjectDefs == null) {
			specificDBObjectDefs = _getSpecificDBObjectDefs(conn, config);
		}
		return new ArrayList<DBObjectDef>(specificDBObjectDefs);
	}

	public void addForUpdateWherePart(StringBuffer sb) {
	}

	public void addForUpdateAfterStatement(StringBuffer sb) {
	}

	public int getColumnTypeTimeStampSQLType() {
		return Types.TIMESTAMP;
	}

	/**
	 * Checks if database supports MIME type for text extracting
	 * 
	 * @param MIMEType
	 * @return true, if supported
	 */
	public boolean isMIMETypeSupported(String MIMEType) {
		return false;
	}

	public void populateBlobData(Blob b, InputStream value) throws RepositoryException, SQLException {
		throw new UnsupportedOperationException();
	}

	public IndexingProcessor getIndexingProcessor() {
		return new IndexingProcessor();
	}

	public DeleteProcessor getDeleteProcessor() {
		return new DeleteProcessor();
	}

	public void sessionSetup(Connection conn) throws SQLException {

	}

	public Long[] limitResults(StringBuffer querySelect, int startFrom, int limit, boolean hasForUpdate) {
		final StringBuilder tmp = new StringBuilder(querySelect);

		final Long[] values = limitResults(tmp, startFrom, limit, hasForUpdate, null);
		querySelect.delete(0, querySelect.length());
		querySelect.append(tmp);

		return values;
	}

	/**
	 * @inheritDoc
	 */
	public void applyHints(StringBuilder sql) {
	}

	/**
	 * Returns Data type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	public StringBuilder getDateColumnToStringConversion(String columnName) {
		final String message = MessageFormat.format("Conversion of Date column {0} value to String is not supported for {1}", columnName, getDatabaseVendor());

		throw new UnsupportedOperationException(message);
	}

	/**
	 * Returns Long type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	public StringBuilder getLongColumnToStringConversion(String columnName) {
		final String message = MessageFormat.format("Conversion of Long column {0} value to String is not supported for {1}", columnName, getDatabaseVendor());

		throw new UnsupportedOperationException(message);
	}

	/**
	 * Returns Double type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	public StringBuilder getDoubleColumnToStringConversion(String columnName) {
		final String message = MessageFormat.format("Conversion of Double column {0} value to String is not supported for {1}", columnName, getDatabaseVendor());

		throw new UnsupportedOperationException(message);
	}

	/**
	 * Returns Boolean type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	public StringBuilder getBooleanColumnToStringConversion(String columnName) {
		final String message = MessageFormat.format("Conversion of Boolean column {0} value to String is not supported for {1}", columnName, getDatabaseVendor());

		throw new UnsupportedOperationException(message);
	}

	public String extractTableName(String tableName) {
		return tableName;
	}

	public String getUserName(Connection connection) throws RepositoryException {
		try {
			final DatabaseMetaData dmd = connection.getMetaData();
			return dmd.getUserName();
		} catch (SQLException e) {
			throw new RepositoryException(e.getMessage());
		}

	}

	public boolean isFTSIndexPopulated(DatabaseConnection conn, String table) throws RepositoryException {
		return true;
	}

	public void validateUserName(String username) throws RepositoryException {
	}

	protected void _validateUserName(String username, String[] forbiddenNames) throws RepositoryException {
		if (username == null) {
			/*
			 * throw new RepositoryException(
			 * "Null username specified for database user");
			 */
			return;
		}
		final String u = username.trim();
		if (u.length() == 0) {
			throw new RepositoryException("Empty username specified for database user");
		}
		for (final String forbiddenName : forbiddenNames) {
			if (u.equalsIgnoreCase(forbiddenName)) {
				throw new RepositoryException(MessageFormat.format("Do not use system user \"{0}\" user for JCR schema creation", 
						forbiddenName));
			}
		}
	}

	/*
	 * public void addSecurityConditions(JCRPrincipals principals,
	 * DatabaseSelectAllStatement st) throws RepositoryException{
	 * addSecurityConditions }
	 */

	public void addSecurityConditions(JCRPrincipals principals, DatabaseSelectAllStatement st, boolean allowBrowse) throws RepositoryException {
		addSecurityConditions(principals, st, allowBrowse, Constants.DEFAULT_ROOT_ALIAS + "." + Constants.FIELD_ID, 
				Constants.DEFAULT_ROOT_ALIAS + "." + Constants.TABLE_NODE__SECURITY_ID);
	}

	public boolean isResultCountSupported() {
		return false;
	}

	public void addResultCountToStatement(DatabaseSelectAllStatement statement) {
		// do nothing
	}

	public boolean isMSSQL() {
		return false;
	}
}

/*
 * $Log: AbstractDatabaseDialect.java,v $
 * Revision 1.30  2010/09/07 14:14:47  vsverlovs
 * EPB-198: code_review_EPB-105_2010-09-02
 * Revision 1.29 2010/08/27 10:19:41
 * abarancikovs JIRA: EPB-105 - Can't upgrade JCR repository version 10 to 14
 * (EPB 7.0 to 7.1) Added schema changes, to reflect requested DB version.
 * 
 * Revision 1.28 2009/03/18 09:11:14 vpukis EPBJCR-22: Oracle 11g (11.1.0.7)
 * dialect
 * 
 * Revision 1.27 2009/03/12 10:57:00 dparhomenko *** empty log message ***
 * 
 * Revision 1.26 2008/08/27 07:23:15 dparhomenko *** empty log message ***
 * 
 * Revision 1.25 2008/07/16 11:42:52 dparhomenko *** empty log message ***
 * 
 * Revision 1.24 2008/07/09 10:13:07 dparhomenko *** empty log message ***
 * 
 * Revision 1.23 2008/07/09 07:50:28 dparhomenko *** empty log message ***
 * 
 * Revision 1.22 2008/07/08 08:17:50 dparhomenko *** empty log message ***
 * 
 * Revision 1.21 2008/06/19 11:57:13 dparhomenko *** empty log message ***
 * 
 * Revision 1.20 2008/06/09 12:36:14 dparhomenko *** empty log message ***
 * 
 * Revision 1.19 2008/06/02 11:40:22 dparhomenko *** empty log message ***
 * 
 * Revision 1.18 2008/05/19 11:09:02 dparhomenko *** empty log message ***
 * 
 * Revision 1.17 2008/05/07 09:14:11 dparhomenko *** empty log message ***
 * 
 * Revision 1.16 2008/04/29 10:55:59 dparhomenko *** empty log message ***
 * 
 * Revision 1.15 2008/02/19 12:26:17 dparhomenko *** empty log message ***
 */