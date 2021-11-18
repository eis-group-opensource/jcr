/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;

import static com.exigen.cm.database.dialect.mssql.MSSQLConstants.MSSQL_FTS_CONVERT_PROC;
import static com.exigen.cm.database.dialect.mssql.MSSQLConstants.MSSQL_FTS_IFILTER_DETECT_PROC;
import static com.exigen.cm.database.dialect.mssql.MSSQLConstants.MSSQL_FTS_STAGE_RECORD_PROC;
import static com.exigen.cm.database.dialect.mssql.MSSQLConstants.MSSQL_FTS_ZIP_PROC;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.mssql.objdef.MSSQLAssemblyDef;
import com.exigen.cm.database.dialect.mssql.objdef.MSSQLStoredProcedureDef;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.vf.commons.logging.LogUtils;


public class MsSQL2005DatabaseDialect extends AbstractMsSQLDatabaseDialect{

	private String userName;
	private static final Log log = LogFactory.getLog(MsSQL2005DatabaseDialect.class);
	
	public MsSQL2005DatabaseDialect(){
		//System.out.println("init");
	}
	
	@Override
	public boolean isFTSSupported() {
		return true;
	}

    public String getJDBCDriverName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

	public String getSchemaName(DatabaseConnection conn) throws RepositoryException {
		return conn.getUserName()+"_jcr";
	}

	@Override
	public String getSchemaName() throws RepositoryException {
		return getUserName()+"_jcr";
	}
	
   public synchronized String getUserName() throws RepositoryException{
        if (userName == null) {
            DatabaseConnection conn = connectionProvider.createConnection();
            try {
                userName = conn.getUserName();
            } finally {
                conn.close();
            }
        }
        return userName;
    }
	
	private boolean hasDBPrivilege(DatabaseConnection conn, String[] privs)
			throws RepositoryException {
		boolean rc = true;
		PreparedStatement st = conn.prepareStatement(
				"SELECT 1 FROM sys.fn_my_permissions(NULL,'DATABASE')"
						+ "  WHERE permission_name=?", false);
		ResultSet rs = null;
		try {
			for (String priv : privs) {
				LogUtils.debug(log,
						"Check if we have database level permission '" + priv
								+ "'");
				st.setString(1, priv);
				rs = st.executeQuery();
				if (!rs.next()) {
					LogUtils
							.warn(
									log,
									"You have not database level permission: '"
											+ priv
											+ "'. Use GRANT SQL statement in JCR database to fix this.");
					rc = false;
				}
				rs.close();
			}
		} catch (SQLException e) {
			LogUtils.warn(log,
					"SQL error checking database level permissions with sys.fn_my_permissions(): "
							+ e.getMessage());
			rc = false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return rc;
	}

	private boolean hasServerPrivilege(DatabaseConnection conn, String[] privs)
			throws RepositoryException {
		boolean rc = true;
		PreparedStatement st = conn
				.prepareStatement(
						"SELECT * FROM sys.server_permissions pe,sys.server_principals pr "
								+ "WHERE pe.class=100 AND pe.state='G' AND pe.permission_name=? "
								+ "AND pe.grantee_principal_id=pr.principal_id AND pr.name=?",
						false);
		ResultSet rs = null;
		try {
			for (String priv : privs) {
				LogUtils.debug(log,
						"Check if we have SQL server level permission '" + priv
								+ "'");
				st.setString(1, priv);
				st.setString(2, conn.getUserName());
				rs = st.executeQuery();
				if (!rs.next()) {
					LogUtils
							.warn(
									log,
									"You have not SQL server level permission: '"
											+ priv
											+ "'. Use GRANT SQL statement in master database to fix this.");
					rc = false;
				}
				rs.close();
			}
		} catch (SQLException e) {
			LogUtils.warn(log,
					"SQL error checking SQL server level permissions: "
							+ e.getMessage());
			rc = false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return rc;
	}

	private String sqlServerCfgOption(DatabaseConnection conn, String option)
			throws RepositoryException {
		String v = null;
		PreparedStatement st = conn
				.prepareStatement(
						"select CAST(value_in_use AS VARCHAR(255)) from sys.configurations where name=?",
						false);
		ResultSet rs = null;
		try {
			LogUtils.debug(log, "Checking SQL server cfg option '" + option
					+ "'");
			st.setString(1, option);
			rs = st.executeQuery();
			if (!rs.next()) {
				LogUtils.warn(log, "Cannot read SQL server cfg option '"
						+ option + "'");
				v = null;
			} else {
				v = rs.getString(1);
			}
			rs.close();
		} catch (SQLException e) {
			LogUtils.warn(log, "SQL error reading SQL server cfg option '"
					+ option + "': " + e.getMessage());
			v = null;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		LogUtils.debug(log, "SQL Server cfg option " + option + " = " + v);
		return v;
	}

	private boolean clrEnabledOnSQLServer(DatabaseConnection conn)
			throws RepositoryException {
		return sqlServerCfgOption(conn, "clr enabled").equalsIgnoreCase("1");
	}
	
	private boolean trustworthyDatabase(DatabaseConnection conn)
			throws RepositoryException {
		boolean rc=false;
		PreparedStatement st = conn
				.prepareStatement(
						"SELECT is_trustworthy_on FROM sys.databases where name=?",
						false);
		ResultSet rs = null;
		try {
			LogUtils.debug(log, "Checking is_trustworthy_on for JCR database");
			st.setString(1, conn.getDatabaseName());
			rs = st.executeQuery();
			if (!rs.next()) {
				LogUtils.warn(log, "Cannot read from sys.databases information about current database");
				rc = false;
			} else {
				rc = rs.getBoolean(1);
			}
			rs.close();
		} catch (SQLException e) {
			LogUtils.warn(log, "SQL error reading information about current database: "
					+ e.getMessage());
			rc = false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		LogUtils.debug(log, "is_trustworthy_on = " + rc);
		return rc;
	}
	
	public boolean hasDatabaseRole(DatabaseConnection conn, String role) throws RepositoryException{
		boolean rc=false;
		String sql="SELECT 1 FROM sys.database_role_members m, sys.database_principals r,"
			+ " sys.database_principals u WHERE r.type='R' and r.name=?"
			+ " and u.type='S' and u.name=? and m.role_principal_id=r.principal_id"
			+ " and m.member_principal_id=u.principal_id";
		PreparedStatement st = conn.prepareStatement(sql,false);
		ResultSet rs = null;
		try {
			LogUtils.debug(log, "Checking DB role '"+role+"' membership");
			st.setString(1, role);
			st.setString(2, conn.getUserName());
			rs = st.executeQuery();
			if (rs.next()) {
				rc = true;
			}
			rs.close();
		} catch (SQLException e) {
			LogUtils.warn(log, "SQL error reading DB role membership info: "
					+ e.getMessage());
			rc = false;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		LogUtils.debug(log, "is member of database role '"+role+"': " + rc);
		return rc;
	}

	@Override
	public void beforeInitializeDatabase(DatabaseConnection conn)
			throws RepositoryException {

		if (!hasDBPrivilege(conn, new String[] { "CONNECT", "CREATE SCHEMA",
				"CREATE TABLE", "CREATE PROCEDURE", "CREATE FUNCTION" })) {
			String msg = "Cannot initialize database - not enough rights in database."
					+ "  Use GRANT SQL statements to fix it";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
		
		super.beforeInitializeDatabase(conn);
		//create schema
		String sql = "create schema "+getSchemaName(conn)+" AUTHORIZATION "+conn.getUserName();
		Statement st = conn.createStatement();
		try {
			st.execute(sql);
			conn.commit();
		} catch (SQLException e){
			throw new RepositoryException("Error creating JCR SCHEMA", e);
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public DropSQLProvider getDropProvider(Map config) throws RepositoryException {
		// TODO Auto-generated method stub
		String dbuser = (String)config.get(Constants.PROPERTY_DATASOURCE_USER);
        return new DropMSSQLSQL2005Objects(dbuser);
	}

	@Override
    public String convertTableName(String tableName) throws RepositoryException{
		String prefix = getSchemaName()+".";
		if (!tableName.startsWith(prefix)){
	        String result = super.convertTableName(tableName);
	        result = getSchemaName()+"."+result;
	        return result;
		} else {
			return tableName;
		}
    }
	
	@Override
    protected String convertIndexName(String name) throws RepositoryException {
		return super.convertTableName(name).replace('.', '_');
	}	

	

	
	@Override
    protected String getTriggerPrefix() throws RepositoryException {
		return getSchemaName()+".";
	}

	@Override
	protected void updateSubstitution(HashMap<String, String> subst) throws RepositoryException {
		subst.put("USER-SCHEMA", getSchemaName());
		subst.put("USER-NAME", getSchemaName());
		
	}

	
	public String extractTableName(String tableName) {
		int pos = tableName.indexOf(".");
		if (pos >  0){
			return tableName.substring(pos+1);
		}else {
			return tableName;
		}
			
	}


	public String getUserName(Connection connection) throws RepositoryException{
        try {
	        DatabaseMetaData dmd = connection.getMetaData();
	    	String loginName = dmd.getUserName();
	    	Statement st = connection.createStatement();
	    	st.execute("select p.name as dbUserName from sys.database_principals p , sys.sql_logins l where p.sid = l .sid and l.name='"+loginName+"'");
	    	ResultSet rs = st.getResultSet();
	    	rs.next();
	    	String result = rs.getString("dbUserName");
	        return result;
        } catch (SQLException e){
        	throw new RepositoryException("Error getting db user name from connection"+e.getMessage());
        }
	}
	
	public void checkFullTextService(DatabaseConnection conn) throws RepositoryException{
		super.checkFullTextService(conn);
		if(getFullTextServiceProperty(conn,"LoadOSResources")!=1){
			setFullTextServiceProperty(conn, "load_os_resources", 1);
        }
		if(getFullTextServiceProperty(conn,"VerifySignature")!=0){
			setFullTextServiceProperty(conn, "verify_signature", 0);
        }
    }
		
	public boolean existsFTSCatalog(DatabaseConnection conn,String name) throws RepositoryException{
		// MSSQL2005 recommended option is to look in sys.fulltext_catalogs
		boolean rc=false;
		try{
			PreparedStatement st=conn.prepareStatement(
					"SELECT 1 FROM sys.fulltext_catalogs WHERE name=?",
					false);
			st.setString(1, name);
			ResultSet rs=st.executeQuery();
			rc=rs.next();
			rs.close();
			st.close();
		}catch(SQLException e){
			throw new RepositoryException("SQL error checking FTS catalog existence",e);
		}
		return rc;
	}
	
	public void createFTSCatalog(DatabaseConnection conn, String name)
			throws RepositoryException {
		try {
			String sql = "CREATE FULLTEXT CATALOG " + name;
			LogUtils.debug(log, "FTS Catalog creation SQL: " + sql);
			PreparedStatement st = conn.prepareStatement(sql, false);
			st.execute();
			st.close();
		} catch (SQLException e) {
			throw new RepositoryException("SQL error creating FTS catalog", e);
		}
	}

	public void createFTSIndex(DatabaseConnection conn, String ftsCatalog,
			String table, String textColumn, String typeColumn,
			String keyIndexName) throws RepositoryException {
		try {
			String sql = "CREATE FULLTEXT INDEX ON " + table + "(" + textColumn
					+ " TYPE COLUMN " + typeColumn + "  LANGUAGE 0"
					+ ") KEY INDEX " + keyIndexName + " ON " + ftsCatalog
					+ " WITH CHANGE_TRACKING AUTO";
			LogUtils.debug(log, "FTS index creation SQL: " + sql);
			PreparedStatement st = conn.prepareStatement(sql, false);
			st.execute();
			st.close();
		} catch (SQLException e) {
			throw new RepositoryException("SQL error creating FTS catalog", e);
		}
	}
	
	public boolean isFTSIndexPopulated(DatabaseConnection conn,String table) throws RepositoryException{
       	boolean rc=false;
    	try{
    		PreparedStatement st = conn.prepareStatement("SELECT CAST(OBJECTPROPERTYEX(OBJECT_ID('"
              + table + "'),'TableFulltextPopulateStatus') AS INT)", true);
    		ResultSet rs=st.executeQuery();
    		if(rs.next()){
    			LogUtils.debug(log,"FTS index status for table ["+table+"] is "+rs.getInt(1));
    			if (rs.getInt(1)==0){
    				rc=true;
    			}
    		}else{
    			rs.close();
    			st.close();
    			throw new RepositoryException("SQL error checking FTS index status - no rows returned");
    		}
    		rs.close();
    		st.close();
    	}catch(SQLException e){
    		throw new RepositoryException("SQL error checking FTS index status", e);
    	}
    	return rc;
    }
	
	public List<DBObjectDef> getSpecificDBObjectDefs(DatabaseConnection conn, Map config) throws RepositoryException{
		boolean supportFTS = "true".equals(config.get(Constants.PROPERTY_SUPPORT_FTS));
		List<DBObjectDef> result=super.getSpecificDBObjectDefs(conn, config);
		
		if (supportFTS){
			
			if (!hasDBPrivilege(conn, new String[] { "CREATE ASSEMBLY",
					"CREATE FULLTEXT CATALOG", "ALTER ANY FULLTEXT CATALOG" })) {
				String msg = "Cannot initialize database - missing database level permissions."
						+ "  Use GRANT SQL statements to fix it";
				LogUtils.error(log, msg);
				throw new RepositoryException(msg);
			}
			
			if(! hasServerPrivilege(conn, new String[]{"UNSAFE ASSEMBLY"})){
				String msg="Cannot initialize database - missing SQL server level permissions."
					+ "  Use GRANT SQL statements to fix it";
				LogUtils.error(log,msg);
				throw new RepositoryException(msg);
			}
			
			if (!hasDatabaseRole(conn, MSSQLConstants.MSSQL_DB_ROLE)) {
				String msg = "Cannot initialize database - missing membership in database role '"
						+ MSSQLConstants.MSSQL_DB_ROLE
						+ "'.  Use EXEC sp_addrolemember N'jcr', N'username' to fix it";
				LogUtils.error(log, msg);
				throw new RepositoryException(msg);
			}
			
			if (! clrEnabledOnSQLServer(conn)){
				String msg="Cannot initialize database - CLR should be enabled to install JCR FTS specific objects."
					+ " Use sp_configure to fix this";
				LogUtils.error(log,msg);
				throw new RepositoryException(msg);
			}
			
			if ( ! trustworthyDatabase(conn)){
				String msg="Cannot initialize database - Database shoud be set TRUSTWORTHY. "
					+ " Use ALTER DATABASE SQL statement to fix this";
				LogUtils.error(log,msg);
				throw new RepositoryException(msg);
			}
	    	
			// IMPORTANT: it's impossible to store in single database the same assembly more than one time
			// (even with different assembly versions)
			// to be able store two or more JCR schemas in a single database:
			// use /out directive of C# compiler - it stores name of assembly within compiled *.dll file
			// e.g. /out:JCR_FTS_SQL2005_V7.dll,/out:JCR_FTS_SQL2005_V8.dll,... 
			// first JCR installation creates assembly JCR_FTS_SQL_Vn, deinstallation should
			// check if the current schema was the last user of that assembly
			//
			String assemblyNameVersion="JCR_FTS_SQL2005_V3";
			result.add(	new MSSQLAssemblyDef( assemblyNameVersion,
				getResourceAsStream("sql/"+assemblyNameVersion+".dll")));
			
			result.add(	new MSSQLStoredProcedureDef( MSSQL_FTS_STAGE_RECORD_PROC,
				"CREATE PROCEDURE " + getSchemaName(conn)+"." + MSSQL_FTS_STAGE_RECORD_PROC+"("
				+ "@id INT, @extract BIT, @errcode NVARCHAR(1024),@schema NVARCHAR(1024),"
				+ "@env NVARCHAR(1024)) AS EXTERNAL NAME "
				+ assemblyNameVersion + ".StoredProcedures.processStageRecord",
				getSchemaName(conn)));
	       	
	       	String stageProcessingArgs=
	       			Constants.TABLE_FTS_STAGE
	       			+ ","+Constants.FIELD_ID
	       			+ ","+Constants.TABLE_FTS_STAGE__FILENAME
	       			+ ","+Constants.FIELD_BLOB
	       			+ ","+Constants.TABLE_FTS_DATA
	       			+ ","+Constants.FIELD_ID
	       			+ ","+Constants.TABLE_FTS_DATA__EXT
	       			+ ","+Constants.FIELD_FTS_DATA_XYZ
	       			+ ","+MSSQLConstants.MSSQL_FTS_TEMP_DIR
	       			+ ","+Constants.TABLE_FTS_INDEXING_ERROR
	       			+ ","+Constants.FIELD_ID
	       			+ ","+Constants.TABLE_FTS_INDEXING__ERROR_CODE
	       			+ ","+Constants.TABLE_FTS_INDEXING__ERROR_TYPE
	       			+ ","+Constants.TABLE_FTS_INDEXING__COMMENT
	       			+ ","+"254"
	       			+ ","+FTSCommand.ERROR_CODE_TXT_EXTRACTION_FAILED
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_OK)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_ERR)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_MSSQL_MISSING_DOC_FILTER)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_MSSQL_MISSING_ZIP_FILTER)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_COMPRESS_ERR)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_EXTRACT_ERR)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_NO_ROWS)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_TOO_MANY_ROWS)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_MSSQL_BAD_FILE_EXT)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_MSSQL_LOB_TO_FILE_ERR)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_MSSQL_FILE_TO_LOB_ERR)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_DELETE_ERR)
	       			+ ","+String.valueOf(Constants.RC_FTS_CONV_UPDATE_ERR)
	       	;
	       	
	       	result.add(new MSSQLStoredProcedureDef(MSSQL_FTS_CONVERT_PROC,
					"CREATE PROCEDURE " + getSchemaName(conn) + "."
							+ MSSQL_FTS_CONVERT_PROC
							+ "(@id INT) AS BEGIN DECLARE @rc INT "
							
							// to be sure that implicit transaction is started before call to CLR
							// looks like that if the first DML are within SQL CLR stored proc
							// then changes are lost after exit form CLR
		                	+ "IF @@TRANCOUNT<1 UPDATE " +getSchemaName(conn)+
		                		"."+Constants.TABLE_SYSTEM_PROPERTIES+" SET "
		                		+ Constants.TABLE_SYSTEM_PROPERTIES__VALUE+"=" 
		                		+ Constants.TABLE_SYSTEM_PROPERTIES__VALUE+" WHERE 100<0 "
		                	
							+"EXEC @rc="
							+ getSchemaName(conn) + "."
							+ MSSQL_FTS_STAGE_RECORD_PROC + " @id, 1,'"
							+ FTSCommand.ERROR_CODE_TXT_CONVERT_AND_MOVE_FAILED
							+ "','" + getSchemaName(conn) + "','"
							+ stageProcessingArgs + "' RETURN @rc END",
					getSchemaName(conn)));

			result.add(new MSSQLStoredProcedureDef(MSSQL_FTS_ZIP_PROC,
					"CREATE PROCEDURE " + getSchemaName(conn) + "."
							+ MSSQL_FTS_ZIP_PROC
							+ "(@id INT) AS BEGIN DECLARE @rc INT "
							
							// to be sure that implicit transaction is started before call to CLR
							// looks like that if the first DML are within SQL CLR stored proc
							// then changes are lost after exit form CLR
							
		                	+ "IF @@TRANCOUNT<1 UPDATE " +getSchemaName(conn)+
		                		"."+Constants.TABLE_SYSTEM_PROPERTIES+" SET "
		                		+ Constants.TABLE_SYSTEM_PROPERTIES__VALUE+"=" 
		                		+ Constants.TABLE_SYSTEM_PROPERTIES__VALUE+" WHERE 100<0 "
							
							+ "EXEC @rc="
							+ getSchemaName(conn) + "."
							+ MSSQL_FTS_STAGE_RECORD_PROC + " @id, 0,'"
							+ FTSCommand.ERROR_CODE_TXT_ZIP_AND_MOVE_FAILED
							+ "','" + getSchemaName(conn) + "','"
							+ stageProcessingArgs + "' RETURN @rc END",
					getSchemaName(conn)));
			
	       	// first dummy argument was used in SQL2000 dialect only
			result.add( new MSSQLStoredProcedureDef(MSSQL_FTS_IFILTER_DETECT_PROC,
					"CREATE PROCEDURE " + getSchemaName(conn)+"." + MSSQL_FTS_IFILTER_DETECT_PROC
					+ "(@dummy INT, @ext NVARCHAR(255)) AS EXTERNAL NAME "
					+ assemblyNameVersion + ".StoredProcedures.detectIFilter", getSchemaName(conn)));
			
		}
		return result;
	}
	
	// recommended for use in SQL 2005 instead of IMAGE
	protected String getColumnTypeBlob() {
        return "VARBINARY(MAX)";
    }
	
	// recommended for use in SQL 2005 instead of TEXT
	protected String getColumnTypeClob() {
        return "NVARCHAR(MAX)";
    }
    
	
    public boolean isResultCountSupported(){
        return true;
    }
    
    public void addResultCountToStatement(DatabaseSelectAllStatement statement){
        statement.addResultColumn("count(*) over () as RECORDCOUNT");
    }

	public String getDatabaseVersion() {
		return "2005";
	}
    
    
}