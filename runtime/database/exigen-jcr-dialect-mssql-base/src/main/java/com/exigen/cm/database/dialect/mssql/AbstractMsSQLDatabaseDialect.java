/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;

import static com.exigen.cm.Constants.DBOBJ_POS_AFTER_TABLE;
import static com.exigen.cm.Constants.DB_TRIGGER_UNSTRUCTURED;
import static com.exigen.cm.Constants.DB_TRIGGER_UNSTRUCTURED_MULTIPLE;
import static com.exigen.cm.Constants.FIELD_BLOB;
import static com.exigen.cm.Constants.TABLE_FTS_DATA;
import static com.exigen.cm.Constants.TABLE_FTS_DATA__EXT;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE__FILENAME;
import static com.exigen.cm.Constants.TABLE_INDEX_STOPWORD_TMP;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.Constants.TABLE_STOPWORD;
import static com.exigen.cm.Constants.TABLE_STOPWORD__DATA;
import static com.exigen.cm.database.dialect.mssql.MSSQLConstants.MSSQL_FTS_STOPWORD_COPY_PROC;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
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
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.IndexDefinition;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.cm.database.dialect.mssql.objdef.MSSQLColumnDef;
import com.exigen.cm.database.dialect.mssql.objdef.MSSQLFunctionDef;
import com.exigen.cm.database.dialect.mssql.objdef.MSSQLStoredProcedureDef;
import com.exigen.cm.database.dialect.mssql.objdef.MSSQLTriggerDef;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.objdef.DBObjectDef;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.StoredProcedureDatabaseCondition;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class AbstractMsSQLDatabaseDialect extends AbstractDatabaseDialect{

    private static final ArrayList<Long> securityFilterInValues;
    
    
    static {
        securityFilterInValues = new ArrayList<Long>();
        securityFilterInValues.add((long)0);
        securityFilterInValues.add((long)2);
        securityFilterInValues.add((long)5);
    }
	
    /** Log for this class */
    private static final Log log = LogFactory.getLog(AbstractMsSQLDatabaseDialect.class);
    
    public boolean isSequenceSupported() {
        return false;
    }

    public String getColumnTypeBoolean() {
        return "BIT";
    }
    public int getColumnTypeBooleanSQLType() {
        return Types.BIT;
    }

    @Override
    public String getColumnTypeString() {
        return "NVARCHAR";
    }
    @Override
    public int getColumnTypeStringSQLType() {
        return Types.VARCHAR;
    }
        
    public String getAlterActionRestrict() {
        return "NO ACTION";
    }
    
    @Override
    public void addForUpdateWherePart(StringBuffer sb){
        sb.append(" with (updlock,rowlock) ");//updlock,  ,rowlock
    }

    @Override
    protected boolean isAlterStatementBracketNeccesary() {
		return false;
	}

    
    public void _lockRow(DatabaseConnection conn, String tableName, String pkPropertyName, Object pkValue) throws RepositoryException {
        try {
            //select id from Entity with (updlock, rowlock) where id =?
             StringBuffer sql = new StringBuffer();
            sql.append("select ");
            sql.append(pkPropertyName);
            sql.append(" from ");
            sql.append(tableName);
            addForUpdateWherePart(sql);
            sql.append("where "); //updlock,
            sql.append(pkPropertyName);
            sql.append("=?");
            LogUtils.debug(log, sql.toString());
            //DatabaseConnection _conn = conn.getNewConnection();
            final boolean useCache = false;
            PreparedStatement stmt = conn.prepareStatement(sql.toString(), useCache);
            //bind id
            DatabaseTools.bindParameter(stmt, conn.getDialect(), 1, pkValue, false);
            try {
            	stmt.execute();
                ResultSet rs = stmt.getResultSet();
                while (rs.next()){
                	// do nothing
                }
            } finally {
            	// If statement caching is used, connection should not be closed.
                if (!useCache) {					
					stmt.close();
				}
            }
        } catch (SQLException exc){
            throw new RepositoryException("Error locking row", exc);
        }
        
    }

    public String getColumnTypeTimeStamp() {
        return "datetime";
    }

    public String getColumnTypeFloat() {
        return "float";
    }
    
    public String getColumnTypeLong() {
        return "numeric(19,0)";
    }


    /**
     * Selects are always started from SELECT DISTINCT thus position
     * for top insertion is 15
     */    
    public Long[] limitResults(StringBuilder querySelect, int statFrom, int limit, boolean hasForUpdate, List<Object> params) {
        if (statFrom > 0){
            throw new UnsupportedOperationException("MSSQL does not supports startFrom for queries");
        }
        boolean hasDistinct = querySelect.toString().toUpperCase().indexOf("SELECT DISTINCT") == 0; // starts with select distinct
        querySelect.insert( hasDistinct?15:6, " top " + limit +" ");
        return null;
    }
    
    /**
     * Returns Data type column to String conversion function statement.
     * @param columnName
     * @return
     */
    public StringBuilder getDateColumnToStringConversion(String columnName){
        return new StringBuilder()
            .append("CONVERT(VARCHAR, ").append(columnName).append(", 20)");
    }
    /**
     * Returns Long type column to String conversion function statement.
     * @param columnName
     * @return
     */
    public StringBuilder getLongColumnToStringConversion(String columnName){
        return new StringBuilder()
            .append("CAST(").append(columnName).append(" AS VARCHAR)");
        
        /*

    (   CASE
        WHEN LONG_VALUE < 0 
            THEN    '0' + CONVERT(VARCHAR, ABS(LONG_VALUE), 2)
        WHEN LONG_VALUE = 0 THEN '1'
        ELSE
                    '2' + CONVERT(VARCHAR, LONG_VALUE, 2)
        END)         
         
         */
        
//        return new StringBuilder()
//                            .append("(  CASE WHEN ")
//                            .append(columnName)
//                            .append("< 0 THEN  '0' + CONVERT(VARCHAR, ABS(")
//                            .append(columnName)
//                            .append("), 2) WHEN ")
//                            .append(columnName).append("=0 THEN '1'")
//                            .append("ELSE '2' + CONVERT(VARCHAR, ")
//                            .append(columnName)
//                            .append(", 2) END)");
    }
    /**
     * Returns Double type column to String conversion function statement.
     * @param columnName
     * @return
     */
    public StringBuilder getDoubleColumnToStringConversion(String columnName){
        return getLongColumnToStringConversion(columnName);
//        return new StringBuilder()
//        .append("STR(").append(columnName).append(", 20, 9)");
        
/*
 (  CASE
        WHEN DOUBLE_VALUE < 0 
            THEN    '0' + REPLACE(CONVERT(VARCHAR, ABS(DOUBLE_VALUE), 2), '.', '')
        WHEN DOUBLE_VALUE = 0 THEN '1'
        ELSE
                    '2' + REPLACE(CONVERT(VARCHAR, DOUBLE_VALUE, 2), '.', '')    
        END) 
 */        
//        return new StringBuilder()
//                            .append("(  CASE WHEN ")
//                            .append(columnName)
//                            .append("< 0 THEN  '0' + REPLACE(CONVERT(VARCHAR, ABS(")
//                            .append(columnName)
//                            .append("), 2), '.','') WHEN ")
//                            .append(columnName).append("=0 THEN '1'")
//                            .append("ELSE '2' + REPLACE(CONVERT(VARCHAR, ")
//                            .append(columnName)
//                            .append(", 2), '.','') END)");
    }
    /**
     * Returns Boolean type column to String conversion function statement.
     * @param columnName
     * @return
     */
    public StringBuilder getBooleanColumnToStringConversion(String columnName){
        return new StringBuilder()
        .append("STR(").append(columnName).append(", 1, 0)");
    }      
    
    /**
     * Returns LIKE parameter adjusted to correspond database specific.
     * @param parameter is a parameter to adjust.
     * @param escapeChar is a character used to signify literal usage.
     * @return
     */
    public String adjustLikeParameter(String parameter, char escapeChar){
        StringBuffer param = new StringBuffer(parameter);
        
        for(int i=0; i<param.length(); i++){
            char c = param.charAt(i);
            if(c == '[' || c == ']')
                param.insert(i++, escapeChar);
        }
        
        return param.toString();
    }

    protected String getColumnTypeClob() {
        return "TEXT";
    }
    
    protected String getColumnTypeBlob() {
        return "IMAGE";
    }
    
    public String getDatabaseVendor() {
        return VENDOR_MSSQL;
    }    

    
    protected String[] buildIndexStatement(IndexDefinition indexDef, int pos) throws RepositoryException{
        if (indexDef.isFullTextSearch()){
            return null;
        } else {
        	if (indexDef.isUnique()){ // check if this index are over columns from PK
        		StringBuilder sb1=new StringBuilder();
        		for (Iterator iterator = indexDef.getColumnIterator(); iterator.hasNext();) {
                    ColumnDefinition column = (ColumnDefinition) iterator.next();
                    sb1.append(column.getColumnName());
                    if (iterator.hasNext()) {
                    	sb1.append(',');
                    }
                }
        		StringBuilder sb2=new StringBuilder();
        		for (Iterator iterator = indexDef.getTableDefinition().getPKColumnIterator(); iterator.hasNext();) {
                    ColumnDefinition column = (ColumnDefinition) iterator.next();
                    sb2.append(column.getColumnName());
                    if (iterator.hasNext()) {
                    	sb2.append(',');
                    }
                }
        		if (sb1.toString().equals(sb2.toString())){
        			// unique index with pk columns found - return null; clustered unique index
        			// will be created as a beside affect of adding a PK
        			return null;
        		}
        		return super.buildIndexStatement(indexDef, pos);
        	}else{	
        		return super.buildIndexStatement(indexDef, pos);
        	}	
        }
    }
    
    protected int getFullTextServiceProperty(DatabaseConnection conn, String prop) throws RepositoryException{
    	// WARNING: FULLTEXTSERVICEPROPERTY() has different list of available options in SQL 2000 and SQL 2005
    	Integer i;
    	try{
    		PreparedStatement st = conn.prepareStatement("SELECT FULLTEXTSERVICEPROPERTY('"
    				+ prop
    				+ "')", false);
    		ResultSet rs=st.executeQuery();
    		if (rs.next()){
    			i=rs.getInt(1);
    		}else{
    			rs.close();
    			st.close();
    			throw new RepositoryException("Unable to read FTS service property '"+prop+"' - no rows returned");
    		}
    		rs.close();
    		st.close();
    	}catch (SQLException e){
    		throw new RepositoryException("SQL error reading FTS service property '"+prop+"'",e);
    	}
    	if (i==null){
    		throw new RepositoryException("Unable to read FTS service property '"+prop+"' - null returned");
    	}
    	return i.intValue();
    }
    
    protected void setFullTextServiceProperty(DatabaseConnection conn,String prop,int val) throws RepositoryException{
    	// WARNING: sp_fulltext_service has different list of available options in SQL 2000 and SQL 2005 
    	try{
    		CallableStatement st=conn.prepareCallableStatement("{? = sp_fulltext_service '"+prop+"',"+val+" }",false);
			st.registerOutParameter(1, Types.INTEGER);
			st.execute();
			int rc = st.getInt(1);
    		st.execute();
    		st.close();
    		if (rc!=0){ // 0=success 1=failure
    			throw new RepositoryException("Error changing FTS service property '"+prop+"' to "+val+" (return code<>0)");
    		}
    	}catch(SQLException e){
    		throw new RepositoryException("Error setting FTS service property '"+prop+"' to "+val,e);
    	}
    }
    
    public void checkFullTextService(DatabaseConnection conn) throws RepositoryException{
        if (getFullTextServiceProperty(conn,"IsFulltextInstalled")!=1){
        	throw new RepositoryException( 
        			"FTS not istalled on this server, please install FTS. "
        			+ "See http://www.digimaker.com/Installing%20Microsoft%20Full%20Text%20Search_6UGPD.pdf.file"
        	);
        }
    }
    
    public String getDBProperty(DatabaseConnection conn,String prop) throws RepositoryException{
    	// SQL 2000 and SQL 2005 strongly recommends to use DATABASEPROPERTYEX() instead of DATABASEPROPERTY()
    	String val=null;
    	try{
    		PreparedStatement st = conn.prepareStatement(
    				"SELECT CAST(DATABASEPROPERTYEX('"
    				+ conn.getDatabaseName()
    				+ "', '"
    				+ prop
    				+ "') AS NVARCHAR(128))", false); // NVARCHAR(128) - to be able handle the 
    		ResultSet rs = st.executeQuery();
    		if (rs.next()){
    			val=rs.getString(1);
    		}else{
    			rs.close();
    			st.close();
    			throw new RepositoryException("Unable to read database property '"+prop+"' - no rows returned");
    		}

    	}catch(SQLException e){
    		throw new RepositoryException("Error reading database property",e);
    	}
    	if(val==null){
    		throw new RepositoryException("Unable to read database property '"+prop+"' - null returned");
    	}
    	return val;
    }
    
    public void enableFTSinDatabase(DatabaseConnection conn)
			throws RepositoryException {
		// MS 2005 documents warns that SP sp_fulltext_database will be removed
		// in next versions but there is no any clear alternative (execpt
		// manually via SQL management studio)
		try {
			CallableStatement cst = conn.prepareCallableStatement(
					"{? = CALL sp_fulltext_database('enable')}", false);
			cst.registerOutParameter(1, Types.INTEGER);
			cst.execute();
			int rc = cst.getInt(1);
			cst.close();
			if (rc != 0) { // 0=success 1=failure
				throw new RepositoryException(
						"Error enabling FTS for database with sp_fulltext_database (return code<>0)");
			}
		} catch (SQLException e) {
			throw new RepositoryException(
					"SQL error enabling FTS for database with sp_fulltext_database",
					e);
		}
	}
    
    abstract public boolean existsFTSCatalog(DatabaseConnection conn,
			String name) throws RepositoryException;

	abstract public void createFTSCatalog(DatabaseConnection conn, String name)
			throws RepositoryException;
	
	public boolean existsFTSIndex(DatabaseConnection conn, String tabName)
			throws RepositoryException {
		boolean exists = true;
		try {
			PreparedStatement st = conn.prepareStatement(
					"SELECT OBJECTPROPERTY(OBJECT_ID('" + tabName
							+ "'),'TableHasActiveFulltextIndex')", false);
			ResultSet rs = st.executeQuery();
			if (!rs.next()) {
				rs.close();
				st.close();
				throw new RepositoryException(
						"SQL error checking if table has FTS index - no rows returned");
			}
			if (rs.getObject(1) == null || rs.wasNull() || rs.getInt(1) == 0) {
				exists = false;
			}
			rs.close();
			st.close();
		} catch (SQLException e) {
			throw new RepositoryException(
					"SQL error checking if table has FTS index", e);
		}
		return exists;
	}

	abstract public void createFTSIndex(DatabaseConnection conn,
			String ftsCatalog, String table, String textColumn,
			String typeColumn, String keyIndexName) throws RepositoryException;
	   

    public DatabaseConnection afterInitializeDatabase(DatabaseConnection conn, Map config) throws RepositoryException {
    	//Transaction tr = null;
    	if ("true".equals(config.get(Constants.PROPERTY_SUPPORT_FTS))){
    		try {
	        	conn.commit();
	            if (TransactionHelper.getInstance().getTransactionManager() == null){
	                conn.setAutoCommit(true);
	            } else {
	            }
	            
	            String catalogName=conn.getUserName()+"_FTS_CAT"; // each user has its own FTS catalog
	            String tabName=( getSchemaName(conn)==null ? "" :
	            		( getSchemaName(conn)+"."))+ Constants.TABLE_FTS_DATA;
	            
	            //1. Check if FullTextService installed 
	            checkFullTextService(conn);
	            //2. Check if FTS is enabled for database
	            boolean ftsEnabled = getDBProperty(conn, "IsFulltextEnabled").equals("1");
	            	            
	            DatabaseConnection conn1 = conn;
	            JCRTransactionManager trManager = TransactionHelper.getInstance().getTransactionManager();
	            if (trManager != null){
	                try {
	                    trManager.commit(trManager.getTransaction());
	                    conn = conn1.getNewConnection();
	   					//tr = TransactionHelper.getInstance().getTransactionManager().suspend();
	                } catch (Exception e) {
	                    throw new RepositoryException(e);
	                } 
	            }
	            
	            // 3. Enable FTS for current database if not already enabled
	            if (!ftsEnabled){
	            	enableFTSinDatabase(conn);
	            }
	            //4.+5. Check Catalog Exists and create if not exists
	            if (!existsFTSCatalog(conn,catalogName )){
	            	createFTSCatalog(conn,catalogName);
	            }
	            //6.+7. Check if index exists and create if not exists
	            if (!existsFTSIndex(conn,tabName)){
	            	createFTSIndex(conn,catalogName, tabName, Constants.FIELD_FTS_DATA_XYZ,
	            			Constants.TABLE_FTS_DATA__EXT, Constants.TABLE_FTS_DATA + "_PK");
	            }
	            
	            if (trManager != null){
	                try {
	                    trManager.begin();
	                } catch (Exception e) {
	                    throw new RepositoryException(e);
	                } 
	                conn = conn1.getNewConnection();
	            }
	            //conn.createStatement().execute("BEGIN TRANSACTION");
	            
	        } catch(RepositoryException e){
	        	throw new RepositoryException("Error initializing FTS",e);
	        }
	
	        if (TransactionHelper.getInstance().getTransactionManager() == null){
	            conn.setAutoCommit(false);
	        } else {
	            /*if (tr != null){
	            	try {
						TransactionHelper.getInstance().getTransactionManager().resume(tr);
					} catch (Exception e) {
						throw new RepositoryException(e);
					} 
	            }*/
	        }
        }
        return conn;
    }
    
    
    abstract public boolean isFTSSupported() ;
    
//    @Override
//    public FTSQueryBuilder getFTSBuilder() {
//        return new MSSQLFTSQueryBuilder();
//    }
    
    @Override
    public com.exigen.cm.query.predicate.FTSQueryBuilder getFTSBuilder_() {
        return new MSSQLFTSQueryBuilder();
    }

    @Override
    public DropSQLProvider getDropProvider(Map config) throws RepositoryException {
        String dbuser = (String)config.get(Constants.PROPERTY_DATASOURCE_USER);
        return new DropMSSQLSQLObjects(dbuser);
    }

    public abstract String getJDBCDriverName() ;
    
//    @Override
//    public BasicSecurityFilter getSecurityFilter() {
//        return new MSSQLSecurityFilter();
//    }
    
    @Override
    public com.exigen.cm.query.BasicSecurityFilter getSecurityFilter() {
        return new MSSQLSecurityFilter();
    }
    
    public void checkInitDBConfiguration(Map config, boolean supportFTS) throws RepositoryException {
    	// check path to stopwords file
    	if (supportFTS){
	    	String stopwordPath = (String)config.get(Constants.CONFIG_MS_FTS_STOPWORD);
	    	if ( JCRHelper.isEmpty(stopwordPath)){
	    		String msg = Constants.PROPERTY_NOT_FOUND + " " + Constants.CONFIG_MS_FTS_STOPWORD + " path to stopwords";
	            msg = LogUtils.error(log, msg, Constants.CONFIG_MS_FTS_STOPWORD);
	    		throw new RepositoryException(msg);
	    	}
    	}
    }
    
    public void initStopWords(DatabaseConnection connection, Map config) throws RepositoryException{
        String value = (String) config.get(Constants.CONFIG_MS_FTS_STOPWORD);
        if (value == null || value.trim().equals("skip")){
            log.info("Key "+Constants.CONFIG_MS_FTS_STOPWORD+" not defined in configuration");
        } else {
            
//            PreparedStatement st = conn.prepareStatement("EXEC jcr.copy_stopwords '"+value+"'", false);
            PreparedStatement st = connection.prepareStatement("EXEC "
            		+ (getSchemaName(connection)==null ? "" : (getSchemaName(connection)+"."))
            		+ MSSQL_FTS_STOPWORD_COPY_PROC+" ?", false);
            try {
                st.setString(1, value);
                st.executeUpdate();
                st.close();
                connection.commit();
            } catch (SQLException e) {
                String message = MessageFormat.format("Error loading stopwords from {0} on MSSQL DB server. Check file exists and ensure that MSSQL DB user used to open Repository JDBC conection to DB has role 'Bulk Insert Administrators' assigned",
                        new Object[]{value});
                throw new RepositoryException(message, e);
            }
        }
        
    }
    
    public List<DBObjectDef> getSpecificDBObjectDefs(DatabaseConnection conn, Map config) throws RepositoryException{
    	boolean supportFTS = "true".equals(config.get(Constants.PROPERTY_SUPPORT_FTS));
    	
    	ArrayList<DBObjectDef> result = new ArrayList<DBObjectDef>();
     	
       	HashMap<String,String> subst = new HashMap<String,String>();
    	subst.put("USER-SCHEMA", conn.getDatabaseName());
    	subst.put("USER-NAME", conn.getUserName());
    	subst.put("STOPWORD-COPY-PROC",MSSQL_FTS_STOPWORD_COPY_PROC);
    	subst.put("STOPWORD-TAB",TABLE_STOPWORD);
    	subst.put("STOPWORD-COL",TABLE_STOPWORD__DATA);
    	subst.put("STOPWORD-TMP",TABLE_INDEX_STOPWORD_TMP);
    	   	    	    	
    	updateSubstitution(subst);
    	
    	String schemaName = null;
    	try {
			schemaName = conn.getDialect().getSchemaName(conn);
		} catch (SQLException e) {
			throw new RepositoryException(e);
		}
    	
    	if (supportFTS){
	   		MSSQLStoredProcedureDef copyStopW = new MSSQLStoredProcedureDef(MSSQL_FTS_STOPWORD_COPY_PROC,
	   				getResourceAsStream("sql/CopyStopWords.sql"),schemaName);
	    	copyStopW.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE,TABLE_STOPWORD);
	    	copyStopW.setSubstitutionMap(subst);
	    	result.add(copyStopW);
	       	
	       	result.add(new MSSQLColumnDef(TABLE_FTS_DATA,TABLE_FTS_DATA__EXT,"VARCHAR(3)",schemaName));
			
    	}
    	MSSQLStoredProcedureDef p1 = new MSSQLStoredProcedureDef("PREMOVE",getResourceAsStream("sql/CheckPermDelBySecId.sql"),schemaName);
    	p1.setSubstitutionMap(subst);
		result.add(p1);
    	MSSQLStoredProcedureDef p2 = new MSSQLStoredProcedureDef("PREMOVETREE",getResourceAsStream("sql/CheckPermDelTreeByNodeId.sql"),schemaName);
    	p2.setSubstitutionMap(subst);
		result.add(p2);
		
		MSSQLFunctionDef readSecFunc = new MSSQLFunctionDef("PREAD",getResourceAsStream("sql/PRead.sql"),schemaName);
		readSecFunc.setSubstitutionMap(subst);
		result.add(readSecFunc);
				
       	addTrigger(result, subst, Constants.TABLE_NODE_UNSTRUCTURED__BOOLEAN_VALUE, 5, schemaName);
       //	addTrigger(result, subst, Constants.TABLE_NODE_UNSTRUCTURED__DATE_VALUE, 3);
       	//addTrigger(result, subst, Constants.TABLE_NODE_UNSTRUCTURED__LONG_VALUE, 1);
       	//addTrigger(result, subst, Constants.TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE, 2);
  	
    	return result;
    }

	protected void updateSubstitution(HashMap<String, String> subst) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	private void addTrigger(ArrayList<DBObjectDef> result, HashMap<String, String> subst, String columnName, int pos, String schemaName) throws RepositoryException {
		HashMap subst2 = new HashMap();
		String prefix = "";
       	subst2.putAll(subst);
    	subst2.put("TRIGGER_NAME",prefix+DB_TRIGGER_UNSTRUCTURED+pos);
    	subst2.put("TABLE-UNSTRUCTURED-PROP",prefix+Constants.TABLE_NODE_UNSTRUCTURED);
    	subst2.put("COLUMN",columnName);
    	MSSQLTriggerDef trigerLong=new MSSQLTriggerDef(DB_TRIGGER_UNSTRUCTURED+pos,
       			getResourceAsStream("sql/TrigerUnstructured.sql"), schemaName);
       	trigerLong.setSubstitutionMap(subst2);
       	trigerLong.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE,TABLE_NODE_UNSTRUCTURED);
       	result.add(trigerLong);
		
       	subst2 = new HashMap();
       	subst2.putAll(subst);
    	subst2.put("TRIGGER_NAME",prefix+DB_TRIGGER_UNSTRUCTURED_MULTIPLE+pos);
    	subst2.put("TABLE-UNSTRUCTURED-PROP",prefix+Constants.TABLE_NODE_UNSTRUCTURED_VALUES);
    	subst2.put("COLUMN",columnName);
       	trigerLong=new MSSQLTriggerDef(DB_TRIGGER_UNSTRUCTURED_MULTIPLE+pos,
       			getResourceAsStream("sql/TrigerUnstructured.sql"), schemaName);
       	trigerLong.setSubstitutionMap(subst2);
       	trigerLong.setPositionInObjectList(DBOBJ_POS_AFTER_TABLE,TABLE_NODE_UNSTRUCTURED_VALUES);
       	result.add(trigerLong);
	}

    protected String getTriggerPrefix() throws RepositoryException {
		return "";
	}

	public List<TableDefinition> getSpecificTableDefs(boolean supportFTS)  throws RepositoryException{
    	ArrayList<TableDefinition> result = new ArrayList<TableDefinition>();
    	
    	if (supportFTS){
	        TableDefinition stage = new TableDefinition(TABLE_FTS_STAGE, true);
	        stage.addColumn(new ColumnDefinition(stage, FIELD_BLOB, Types.BLOB));
	        stage.addColumn(new ColumnDefinition(stage, TABLE_FTS_STAGE__FILENAME, Types.VARCHAR));
	        result.add(stage);
    	}
        
    	return result;
    }

    @Override
    public boolean isMIMETypeSupported(String MIMEType) {
        //MIME types supported by MS SQL
          final List <String> types = Arrays.asList(
            "text/plain",
            "application/msword",
            "application/excel",
            "application/vnd.ms-excel",
            "application/vndms-excel",
            "application/x-excel",
            "application/x-msexcel",
            "application/powerpoint",
            "application/mspowerpoint",
            "application/vnd.ms-powerpoint",
//          Uncomment this to enable native extraction. See TextExtractionCommand.getExtensionByMIME  for additional code change.
//            "text/rtf",
//            "application/pdf",
            "text/html"
        );
        return types.contains(MIMEType);
    }
    
	public String[] buildDropTableStatement(TableDefinition table) throws RepositoryException{
		ArrayList<String> sqls = new ArrayList<String>();
		sqls.add("DROP TABLE "+convertTableName(table.getTableName()));
		return sqls.toArray(new String[sqls.size()]);
	}

	public String getSchemaName(DatabaseConnection conn) throws RepositoryException {
		return null;
	}

    protected String convertIndexName(String tableName,String indexName) {
		return tableName+"."+indexName;
	}

    @Override
    protected int getMaxColumnLength() {
        return 27;
    }

	@Override
	public void beforeInitializeDatabase(DatabaseConnection conn) throws RepositoryException {
		
		String sql = "SELECT CAST(COLLATIONPROPERTY(CAST(DATABASEPROPERTYEX( '"+conn.getDatabaseName()+"' , 'Collation' ) AS VARCHAR(100)), 'ComparisonStyle') AS INTEGER)%2";
		Statement st = conn.createStatement();
		try {
			st.execute(sql);
			ResultSet rs = st.getResultSet();
			if (rs.next()){
				int value = rs.getInt(1);
				if (value != 0){
					throw new RepositoryException("Please use Case Sensitive database");
				}
			} else {
				throw new RepositoryException("Error getting CaseSensitive status for database");
			}
		} catch (SQLException e){
			throw new RepositoryException("Error getting CaseSensitive status for database", e);
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		super.beforeInitializeDatabase(conn);
	}
	
	public void validateUserName(String username) throws RepositoryException {
		_validateUserName(username, new String[] { "sa" });
	}
	
	
	 public void addSecurityConditions(JCRPrincipals principals, DatabaseSelectAllStatement st, boolean allowBrowse, String idColumn, String securityIdColumn) throws RepositoryException{
         
         StringBuffer groups = new StringBuffer("xxxJCR_CHECKGROUPxxx");
         for(String group:principals.getGroupIdList()){
             groups.append(",");
             groups.append(convertStringToSQL(group));
         }
         
         StringBuffer contexts = new StringBuffer("xxxJCR_CHECKCONTEXTxxx");
         for(String context:principals.getContextIdList()){
             contexts.append(",");
             contexts.append(convertStringToSQL(context));
         }

         
         StoredProcedureDatabaseCondition cn1 = Conditions.storedProcedure((getSchemaName() != "" ? getSchemaName()+"." : "")+"PREAD");
         cn1.addVariable(idColumn);
         cn1.addVariable(securityIdColumn);
         cn1.addParameter(principals.getUserId());
         cn1.addParameter(groups.toString());
         cn1.addParameter(contexts.toString());
         cn1.addParameter(allowBrowse);
         st.addCondition(Conditions.gt(cn1, 0));
  }

    public String getSchemaName()  throws RepositoryException{
        return "";
    }

    @Override
    public boolean isMSSQL() {
    	return true;
    }

    //public void addSecurityConditions(JCRPrincipals principals, DatabaseSelectAllStatement st, boolean allowBrowse, String idColumn, String securityIdColumn) throws RepositoryException{
        
 
        
//if (true){
//    throw new UnsupportedOperationException("Security is not supported for MSSQL");
//}
        /*
HAVING MAX(CASE
WHEN n.security_id IS NULL or sec.id IS NULL THEN 0
WHEN sec.user_id='.superuser' THEN COALESCE(4+sec.p_read,1)
WHEN sec.group_id IN ('.group1','.group2') THEN COALESCE(3-sec.p_read,1)
ELSE 1
END
) IN (0,2,5)             
         */
  /*      st.addLeftOuterJoin(Constants.TABLE_ACE, "sec", Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID, Constants.TABLE_NODE__SECURITY_ID );
        
  //      DatabaseCondition cond1 = Conditions.eq("sec."+Constants.TABLE_ACE__USER_ID, userId);
 //       DatabaseCondition cond2 = null;
//        if (groupIds.size() > 0){
//             cond2 = Conditions.in("sec."+Constants.TABLE_ACE__GROUP_ID, groupIds);
//        }
//        DatabaseCondition cond3 = Conditions.isNull("sec."+Constants.FIELD_ID);
//        if (groupIds.size() > 0){
//            st.addCondition(Conditions.or(new DatabaseCondition[]{cond1, cond2, cond3}));
//        } else {
//            st.addCondition(Conditions.or(new DatabaseCondition[]{cond1, cond3}));
//        }
        
        st.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
        List<String> groupIds = principals.getGroupIdList();
        if (groupIds.size() == 0){
            ArrayList<String> _groupIds = new ArrayList<String>(groupIds);
            groupIds = _groupIds;
            groupIds.add("xxxJCR_CHECKGROUPxxx");
        }

        DatabaseCondition caseWhen1_1 = Conditions.isNull(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID);
        DatabaseCondition caseWhen1_2 = Conditions.isNull("sec."+Constants.FIELD_ID);
        DatabaseCondition caseWhen1 = Conditions.or(caseWhen1_1, caseWhen1_2);
        DatabaseCondition caseWhen2 = Conditions.eq("sec."+Constants.TABLE_ACE__USER_ID, principals.getUserId());
        DatabaseCondition caseWhen3 = Conditions.in("sec."+Constants.TABLE_ACE__GROUP_ID, groupIds);

        CaseDatabaseCondition caseCondition = Conditions.caseCondition(caseWhen1,"0", "1");
        caseCondition.addWhen(caseWhen2,"COALESCE(4+sec."+SecurityPermission.READ.getColumnName()+",1)");
        caseCondition.addWhen(caseWhen3,"COALESCE(3-sec."+SecurityPermission.READ.getColumnName()+",1)");
        
        DatabaseCondition maxCondition = Conditions.max(caseCondition);
        
        DatabaseCondition havingCondition = Conditions.in(maxCondition,securityFilterInValues);
        
        st.addHaving(havingCondition);*/
   // }
    
}
