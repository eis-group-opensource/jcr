/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.query.BasicSecurityFilter;
import com.exigen.cm.query.predicate.FTSQueryBuilder;
import com.exigen.cm.security.JCRPrincipals;

/**
 * DatabaseDialect class.
 */
public interface DatabaseDialect {

	public static final String VENDOR_ORACLE = "ORACLE";
	public static final String VENDOR_MSSQL = "MSSQL";
	public static final String VENDOR_HYPERSONIC = "HYPERSONIC";

	/**
	 * Validates user name
	 * 
	 * @param username
	 *            User name
	 * @throws RepositoryException
	 *             Exception
	 */
	public void validateUserName(String username) throws RepositoryException;

	/**
	 * Checks if sequence supported
	 * 
	 * @return true if sequence is
	 */
	public boolean isSequenceSupported();

	/**
	 * Builds create statement
	 * 
	 * @param definition
	 *            Table definition
	 * @return Created statement
	 * @throws RepositoryException
	 *             Exception
	 */
	public String buildCreateStatement(TableDefinition definition) throws RepositoryException;

	/**
	 * Builds create index statements
	 * 
	 * @param definition
	 *            Table definition
	 * @return Created index statements
	 * @throws RepositoryException
	 *             Exception
	 */
	public String[][] buildCreateIndexStatements(TableDefinition definition) throws RepositoryException;

	/**
	 * 
	 * @param tableName
	 *            Table name
	 * @param columnDefs
	 *            Column definitions
	 * @return Alter table statements
	 * @throws RepositoryException
	 *             Exception
	 */
	public String buildAlterTableStatement(String tableName, ColumnDefinition[] columnDefs) throws RepositoryException;

	/**
	 * 
	 * @param tableName
	 *            Table name
	 * @param columnDefs
	 *            Column definitions
	 * @return Statements
	 * @throws RepositoryException
	 *             Exception
	 */
	public String buildAlterTableModifyColumnStatement(String tableName, ColumnDefinition[] columnDefs) throws RepositoryException;

	/**
	 * Long
	 * 
	 * @return Long
	 */
	public String getColumnTypeLong();

	/**
	 * 
	 * @return Long SQL type
	 */
	public int getColumnTypeLongSQLType();

	/**
	 * 
	 * @return String
	 */
	public String getColumnTypeString();

	/**
	 * 
	 * @return String SQL Type
	 */
	public int getColumnTypeStringSQLType();

	/**
	 * 
	 * @return Boolean
	 */
	public String getColumnTypeBoolean();

	/**
	 * 
	 * @return Boolean SQL Type
	 */
	public int getColumnTypeBooleanSQLType();

	/**
	 * 
	 * @return Time stamp
	 */
	public String getColumnTypeTimeStamp();

	/**
	 * 
	 * @return Time stamp SQL type
	 */
	public int getColumnTypeTimeStampSQLType();

	/**
	 * 
	 * @return Float
	 */
	public String getColumnTypeFloat();

	/**
	 * 
	 * @param definition
	 *            Table definition
	 * @return PK alter statement
	 * @throws RepositoryException
	 *             Exception
	 */
	public String buildPKAlterStatement(TableDefinition definition) throws RepositoryException;

	/**
	 * 
	 * @param definition
	 *            Table definition
	 * @param conn
	 *            Connection
	 * @return FK alter table
	 * @throws RepositoryException
	 *             Exception
	 */
	public String buildFKAlterStatement(TableDefinition definition, DatabaseConnection conn) throws RepositoryException;

	/**
	 * 
	 * @return Alter Action Restrict
	 */
	public String getAlterActionRestrict();

	/**
	 * 
	 * @param conn
	 *            Connection
	 * @return Table definition
	 * @throws RepositoryException
	 *             Exception
	 */
	public TableDefinition createIdGeneratorInfrastracture(DatabaseConnection conn) throws RepositoryException;

	/**
	 * Checks Id generation
	 * 
	 * @param conn
	 *            Connection
	 * @throws RepositoryException
	 *             Exception
	 */
	public void checkIdGeneratorInfrastracture(DatabaseConnection conn) throws RepositoryException;

	/**
	 * Locks table row
	 * 
	 * @param conn
	 *            Connection
	 * @param tableName
	 *            Table name
	 * @param id
	 *            Id
	 * @throws RepositoryException
	 *             Exception
	 */
	public void lockTableRow(DatabaseConnection conn, String tableName, Object id) throws RepositoryException;

	/**
	 * 
	 * @param conn
	 *            Connection
	 * @param tableName
	 *            Table name
	 * @param pkPropertyName
	 *            PK property name
	 * @param pkValue
	 *            PK value
	 * @throws RepositoryException
	 *             Exception
	 */
	public void lockTableRow(DatabaseConnection conn, String tableName, String pkPropertyName, 
			Object pkValue) throws RepositoryException;

	// public void setRepository(_RepositoryImpl repository);

	/**
	 * @param value
	 *            String to convert
	 */
	public String convertStringToSQL(String value);

	/**
	 * 
	 * @param value
	 *            Value to convert
	 * @param columnName
	 *            Column name
	 * @return Converted string
	 * @throws RepositoryException
	 *             Exception
	 */
	public String convertStringFromSQL(String value, String columnName) throws RepositoryException;

	/**
	 * Reserves id range in database, and returns current id value.
	 * 
	 * @param conn
	 *            database connection
	 * @param range
	 *            range to reserve
	 * @return current id value.
	 * @throws RepositoryException
	 */
	public Long reserveIdRange(DatabaseConnection conn, Long range) throws RepositoryException;

	/**
	 * Modifies query string so to constrain number of results to be returned.
	 * 
	 * @param querySelect
	 *            - is a selection query to be modified.
	 * @param startFrom
	 *            - is a start row that will be returned.
	 * @param limit
	 *            - is a max number of rows which can be returned.
	 * @return <code>true</code> if limit value is added as parameter
	 *         placeholder. Returns <code>false</code> if limit specified as
	 *         numeric value embedded in query string
	 * @deprecated
	 */
	public Long[] limitResults(StringBuffer querySelect, int startFrom, int limit, boolean hasForUpdate);

	/**
	 * Modifies query string so to constrain number of results to be returned.
	 * 
	 * @param querySelect
	 *            - is a selection query to be modified.
	 * @param limit
	 *            - is a max number of rows which can be returned.
	 * @return <code>true</code> if limit value is added as parameter
	 *         placeholder. Returns <code>false</code> if limit specified as
	 *         numeric value embedded in query string
	 */
	public Long[] limitResults(StringBuilder querySelect, int startFrom, int limit, boolean hasForUpdate, List<Object> params);

	/**
	 * Converts java Boolean to instance used to represent boolean in Database.
	 * 
	 * @param value
	 *            Boolean value
	 * @return DB boolean
	 */
	public Object convertToDBBoolean(Boolean value);

	/**
	 * Converts DB boolean to Java boolean
	 * 
	 * @param object
	 *            DB object
	 * @return Boolean
	 */
	public Boolean convertFromDBBoolean(Object object);

	/**
	 * Converts java Long to instance used to represent Long in Database.
	 * 
	 * @param value
	 *            Long value
	 * @return DB Long
	 */
	public Object convertToDBLong(Long value);

	/**
	 * Converts Number received from DB to java Long.
	 * 
	 * @param value
	 *            DB Long
	 * @return Long value
	 */
	public Long convertFromDBLong(Number value);

	/**
	 * Converts java Long to instance used to represent Long in Database.
	 * 
	 * @param value
	 *            Integer
	 * @return DB object
	 */
	public Object convertToDBInteger(Integer value);

	/**
	 * Converts java Date to instance used to represent Date in Database.
	 * 
	 * @param value
	 *            Date
	 * @return DB object
	 */
	public Object convertToDBDate(Date value);

	/**
	 * Converts java Double to instance used to represent Double in Database.
	 * 
	 * @param value
	 *            Double
	 * @return DB object
	 */
	public Object convertToDBDouble(Double value);

	/**
	 * Returns count all statement
	 * 
	 * @return quantity
	 */
	public String getCountAllStatement();

	/**
	 * Returns LIKE parameter adjusted to correspond database specific.
	 * 
	 * @param parameter
	 *            is a parameter to adjust.
	 * @param escapeChar
	 *            is a character used to signify literal usage.
	 * @return Adjusted like parameter
	 */
	public String adjustLikeParameter(String parameter, char escapeChar);

	/**
	 * Returns max number of parameters allowed for IN by specific database
	 * dialect.
	 * 
	 * @return max number of parameters for IN
	 */
	public int getInMaxParamsCount();

	/**
	 * Returns true if given DB supports native FTS searching.
	 * 
	 * @return true is FTS is supported
	 */
	public boolean isFTSSupported();

	/**
	 * Returns true if FTS index for given table is up to date
	 * 
	 * @return true if FTS index populated
	 */
	public boolean isFTSIndexPopulated(DatabaseConnection conn, String table) throws RepositoryException;

	/**
	 * Returns instance of Dialect specific FTS query builder.
	 * 
	 * @return
	 */
	// public FTSQueryBuilder getFTSBuilder();

	/**
	 * Returns second implementation of Dialect specific FTS query builder.
	 * 
	 * @return
	 */
	public FTSQueryBuilder getFTSBuilder_();

	/**
	 * 
	 * @return Stored procedures
	 */
	public List<String> getStoredProcedures();

	/**
	 * 
	 * @return Database vendor
	 */
	public String getDatabaseVendor();

	/**
	 * 
	 * @return Database version
	 */
	public String getDatabaseVersion();

	/**
	 * Actions should be performed before database initialization
	 * 
	 * @param conn
	 *            Connection
	 * @throws RepositoryException
	 *             Exception
	 */
	public void beforeInitializeDatabase(DatabaseConnection conn) throws RepositoryException;

	/**
	 * 
	 * @param conn
	 *            Connection
	 * @param config
	 *            Configuration
	 * @return Connection
	 * @throws RepositoryException
	 *             Exception
	 */
	public DatabaseConnection afterInitializeDatabase(DatabaseConnection conn, Map config) throws RepositoryException;

	/**
	 * Returns Database dialect specific drop provider.
	 * 
	 * @param config
	 *            Configuration
	 * @return
	 */
	public DropSQLProvider getDropProvider(Map config) throws RepositoryException;

	/**
	 * 
	 * @param config
	 *            Configuration
	 * @return
	 * @throws RepositoryException
	 *             Exception
	 */
	public DropSQLProvider getDropProvider2(Map<String, String> config) throws RepositoryException;

	/**
	 * 
	 * @return JDBC driver name
	 */
	public String getJDBCDriverName();

	/**
	 * Returns implementation of dialect specific security filter for new query
	 * implementation.
	 * 
	 * @return security filter
	 */
	public BasicSecurityFilter getSecurityFilter();

	/**
	 * 
	 * @param connectionProvider
	 *            Connection provider
	 */
	public void setConnectionProvider(ConnectionProvider connectionProvider);

	/**
	 * Checks configuration specific for the dialect
	 * 
	 * @param configuration
	 * @throws RepositoryException
	 *             if mandatory properties are missed
	 */
	public void checkConfiguration(Map configuration) throws RepositoryException;

	/*
	 * Returns Dialect specific BLOB insert statement.
	 * 
	 * @return
	 */
	// public BLOBInsertStatement createBLOBInsertStatement(String tableName,
	// long pkValue, InputStream data) throws RepositoryException;

	/**
	 * @param tableName
	 *            table name
	 * @return Converted table name
	 */
	public String convertTableName(String tableName) throws RepositoryException;

	/**
	 * Converts column name
	 * 
	 * @param columnName
	 *            Column name
	 * @return Converted column name
	 */
	public String convertColumnName(String columnName);

	/**
	 * 
	 * @param name
	 *            Procedure name
	 * @return Procedure name
	 * @throws RepositoryException
	 *             Exception
	 */
	public String convertProcedureName(String name) throws RepositoryException;

	/**
	 * 
	 * @param name
	 *            Name
	 * @return
	 * @throws RepositoryException
	 *             exception
	 */
	public String convertConstraintName(String name) throws RepositoryException;

	/**
	 * Initialize stop words
	 * 
	 * @param connection
	 *            Connection
	 * @param config
	 *            configuration
	 * @throws RepositoryException
	 *             Exception
	 */
	public void initStopWords(DatabaseConnection connection, Map config) throws RepositoryException;

	/**
	 * 
	 * @param supportFTS
	 *            Support FTS
	 * @return Table definitions
	 * @throws RepositoryException
	 *             Exception
	 */
	public List<TableDefinition> getSpecificTableDefs(boolean supportFTS) throws RepositoryException;

	/**
	 * 
	 * @param conn
	 *            Connection
	 * @param config
	 *            Configuration
	 * @return Object definitions
	 * @throws RepositoryException
	 *             Exceptions
	 */
	public List<DBObjectDef> getSpecificDBObjectDefs(DatabaseConnection conn, Map config) throws RepositoryException;

	/**
	 * Checks the Mime type
	 * 
	 * @param MIMEType
	 *            Mime type
	 * @return true if supported
	 */
	public boolean isMIMETypeSupported(String MIMEType);

	/**
	 * Populates Blob data
	 * 
	 * @param b
	 *            Blob
	 * @param value
	 *            Input stream
	 * @throws RepositoryException
	 *             Exception
	 * @throws SQLException
	 *             SQL Exception
	 */
	public void populateBlobData(Blob b, InputStream value) throws RepositoryException, SQLException;

	/**
	 * Executes actions after text extraction
	 * 
	 * @return Indexing processor
	 */
	public IndexingProcessor getIndexingProcessor();

	/**
	 * Executes actions needed when content is deleted (delete requested in
	 * INDEXABLE_DATA
	 * 
	 * @return Delete processor
	 */
	public DeleteProcessor getDeleteProcessor();

	/**
	 * Session setup
	 * 
	 * @param conn
	 *            Connection
	 * @throws SQLException
	 *             SQL Exception
	 */
	public void sessionSetup(Connection conn) throws SQLException;

	/**
	 * 
	 * @param table
	 *            Table definition
	 * @return Drop table statement
	 * @throws RepositoryException
	 */
	public String[] buildDropTableStatement(TableDefinition table) throws RepositoryException;

	/**
	 * 
	 * @param conn
	 *            Connection
	 * @return Schema name
	 * @throws RepositoryException
	 *             Exception
	 * @throws SQLException
	 *             SQL exception
	 */
	public String getSchemaName(DatabaseConnection conn) throws RepositoryException, SQLException;

	/**
	 * 
	 * @param tableName
	 *            Table name
	 * @param columnName
	 *            Column name
	 * @return Drop column statement
	 * @throws RepositoryException
	 */
	public String buildAlterTableDropColumn(String tableName, String columnName) throws RepositoryException;

	/**
	 * Drop sequence
	 * 
	 * @param conn
	 *            Connection
	 */
	public void dropSequence(DatabaseConnection conn);

	/**
	 * Applies DB hints to main SELECT.
	 * 
	 * @param sql
	 */
	public void applyHints(StringBuilder sql);

	/**
	 * Returns Data type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	public StringBuilder getDateColumnToStringConversion(String columnName);

	/**
	 * Returns Long type column to String conversion function statement.
	 * 
	 * @param columnName
	 * @return
	 */
	public StringBuilder getLongColumnToStringConversion(String columnName);

	/**
	 * Returns Double type column to String conversion function statement.
	 * 
	 * @param columnName
	 *            Column name
	 * @return Conversion pattern
	 */
	public StringBuilder getDoubleColumnToStringConversion(String columnName);

	/**
	 * Returns Boolean type column to String conversion function statement.
	 * 
	 * @param columnName
	 *            Column name
	 * @return Conversion pattern
	 */
	public StringBuilder getBooleanColumnToStringConversion(String columnName);

	/**
	 * Check init DB configuration
	 * 
	 * @param config
	 *            Configuration
	 * @param supportFTS
	 *            Support TFS
	 * @throws RepositoryException
	 *             Exception
	 */
	public void checkInitDBConfiguration(Map config, boolean supportFTS) throws RepositoryException;

	/**
	 * Extracts table name
	 * 
	 * @param tableName
	 *            Table name
	 * @return Table name
	 */
	public String extractTableName(String tableName);

	/**
	 * User name
	 * 
	 * @param connection
	 *            Connection
	 * @return User name
	 * @throws RepositoryException
	 *             Exception
	 */
	public String getUserName(Connection connection) throws RepositoryException;

	/**
	 * Adds security conditions
	 * 
	 * @param principals
	 *            Principals
	 * @param stmt
	 *            Statement
	 * @param allowBrowse
	 *            Allow Browse
	 * @throws RepositoryException
	 *             Exception
	 */
	public void addSecurityConditions(JCRPrincipals principals, DatabaseSelectAllStatement stmt, 
			boolean allowBrowse) throws RepositoryException;

	/**
	 * Adds security conditions
	 * 
	 * @param principals
	 *            Principals
	 * @param stmt
	 *            Database statement
	 * @param allowBrowse
	 *            Allow browse
	 * @param idColumn
	 *            Id column
	 * @param securityIdColumn
	 *            Security Id Column
	 * @throws RepositoryException
	 *             Exception
	 */
	public void addSecurityConditions(JCRPrincipals principals, DatabaseSelectAllStatement stmt, boolean allowBrowse, 
			String idColumn, String securityIdColumn) throws RepositoryException;

	/**
	 * 
	 * @return true if result count is supported
	 */
	public boolean isResultCountSupported();

	/**
	 * 
	 * @param statement
	 *            Statement
	 */
	public void addResultCountToStatement(DatabaseSelectAllStatement statement);

	/**
	 * 
	 * @return true if is MS SQL
	 */
	public boolean isMSSQL();
}
