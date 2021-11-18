/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.hsql;

import static com.exigen.cm.Constants.FIELD_BLOB;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE;
import static com.exigen.cm.Constants.TABLE_FTS_STAGE__FILENAME;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.dialect.AbstractDatabaseDialect;
import com.exigen.cm.database.dialect.DeleteProcessor;
import com.exigen.cm.database.dialect.IndexingProcessor;
import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.StoredProcedureDatabaseCondition;
import com.exigen.cm.security.JCRPrincipals;

/**
 * TODO Put class description here
 * 
 */
public class HyperSonicSQLDatabaseDialect extends AbstractDatabaseDialect {
    
    /** Log for this class */
    private Log log = LogFactory.getLog(HyperSonicSQLDatabaseDialect.class);

    
    private static final String stopWords = "about 1 after 2 all also 3 an 4 and 5 another 6 any 7 are 8 as 9 at 0 be $ because been before being between both but by came can come could did do each for from get got has had he have her here him himself his how if in into is it like make many me might more most much must my never now of on only or other our out over said same see should since some still such take than that the their them then there these they this those through to too under up very was way we well were what where which while who with would you your";
    private static final String STOPWORDS_SQL = new StringBuffer("INSERT INTO ")
                                                    .append(Constants.TABLE_STOPWORD)
                                                    .append(" (")
                                                    .append(Constants.TABLE_STOPWORD__DATA)
                                                    .append(") VALUES (?)")
                                                    .toString();    
    
    /**
     * @see com.exigen.cm.database.dialect.AbstractDatabaseDialect#_lockRow(com.exigen.cm.database.DatabaseConnection, java.lang.String, java.lang.String, java.lang.Object)
     */
    protected void _lockRow(DatabaseConnection conn, String tableName, String pkPropertyName, Object pkValue) throws RepositoryException {

    }
    
    public String buildAlterTableStatement(String tableName, ColumnDefinition[] columnDefs) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < columnDefs.length; i++) {
            sb.append("ALTER TABLE ");
            sb.append(tableName);
            sb.append(" ADD ");
            ColumnDefinition columnDef = columnDefs[i];
            //id varchar(32) NOT NULL
            sb.append(columnDef.getColumnName());
            sb.append(" ");
            sb.append(getSQLType(columnDef.getColumnType(), columnDef.getLength()));
            if (columnDef.isNotNull()){
                sb.append(" NOT NULL ");
            }
            sb.append(";");                   
        }
        return sb.toString();
    }      

    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#isSequenceSupported()
     */
    public boolean isSequenceSupported() {
        return false;
    }

    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#getColumnTypeBoolean()
     */
    public String getColumnTypeBoolean() {
        return "BOOLEAN";
    }

    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#getColumnTypeBooleanSQLType()
     */
    public int getColumnTypeBooleanSQLType() {
        return Types.BOOLEAN;
    }

    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#getColumnTypeTimeStamp()
     */
    public String getColumnTypeTimeStamp() {
        return "TIMESTAMP";
    }

    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#getColumnTypeFloat()
     */
    public String getColumnTypeFloat() {
        return "FLOAT";
    }
    
    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#getColumnTypeFloat()
     */
    public String getColumnTypeLong() {
        return "NUMERIC";
    }    
    
    

    /**
     * @see com.exigen.cm.database.dialect.DatabaseDialect#getalterActionRestrict()
     */
    public String getAlterActionRestrict() {
        return "NO ACTION";
    }

    /*
     * Selects are always started from SELECT DISTINCT thus position
     * for top insertion is 15
     */
//    public boolean limitResults(StringBuffer querySelect, int limit) {
//        querySelect.insert( 15, " top " + limit );
//        return false;
//    }    
    
    /**
     * Selects are always started from SELECT DISTINCT thus position
     * correct syntax: SELECT TOP n DISTINCT nn.ID ...
     * for top insertion is 15
     */    
    public Long[] limitResults(StringBuilder querySelect, int startFrom, int limit, boolean hasForUpdate, List<Object> params) {
//        boolean hasDistinct = querySelect.indexOf("SELECT DISTINCT") == 0; // starts with select distinct
//        querySelect.insert( hasDistinct?15:6, " top " + limit );
        querySelect.insert(6, " limit " + startFrom + " " + limit );
        return null;
    }

    public String getDatabaseVendor() {
        return VENDOR_HYPERSONIC;
    }  
    
    @Override
    public DropSQLProvider getDropProvider(Map config) throws RepositoryException {
        return new DropHSQLObjects();
    }

    public String getJDBCDriverName() {
        return "org.hsqldb.jdbcDriver";
    }

    @Override
    protected String getColumnTypeClob() {
        return "longvarchar";
    }
    
//    @Override
//    public BasicSecurityFilter getSecurityFilter() {
//        return new HSQLSecurityFilter();
//    }
    
    @Override
    public com.exigen.cm.query.BasicSecurityFilter getSecurityFilter() {
        return new HSQLSecurityFilter();
    }
    
    @Override
    public DatabaseConnection afterInitializeDatabase(DatabaseConnection conn, Map config) throws RepositoryException {
        super.afterInitializeDatabase(conn, config);
    	boolean supportFTS = "true".equals(config.get(Constants.PROPERTY_SUPPORT_FTS));

    	if (supportFTS){
	//        Initialize stopwords
	        try{
	            log.debug("Adding default stopwords");
	            PreparedStatement swStatement = conn.prepareStatement(STOPWORDS_SQL, false);
	            StringTokenizer st = new StringTokenizer(stopWords);
	            while(st.hasMoreTokens()){
	                swStatement.setString(1, st.nextToken());
	                swStatement.addBatch();
	            }
	                
	            swStatement.executeBatch();
	            conn.commit();
	        }catch(Exception ex){
	            log.error("Stopwords initialization failed!", ex);
	            throw new RepositoryException("Stopwords initialization failed!", ex);
	        }
	        
	        
    	}
        
        return conn;
        
        
    }

    @Override
    protected String getColumnTypeBlob() {
        return "LONGVARBINARY";
    }
    
    protected void appendHSCreateTablePrefix(StringBuffer sb, TableDefinition tableDef) {
    	boolean hasBinary = false;
        for(Iterator it = tableDef.getColumnIterator() ; it.hasNext() ;){
            ColumnDefinition columnDef = (ColumnDefinition) it.next();
            if (columnDef.getColumnType() == Types.BLOB){
            	hasBinary = true;
            }
        }
        
        if (hasBinary){
        	sb.append(" CACHED ");
        }
        else {
        	sb.append(" MEMORY ");
        }

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
    
    public IndexingProcessor getIndexingProcessor(){
        return new HSQLIndexingProcessor();
    }
    
    public DeleteProcessor getDeleteProcessor() {
        return new HSQLDeleteProcessor();
    }
    
	public String[] buildDropTableStatement(TableDefinition table) throws RepositoryException{
		ArrayList<String> sqls = new ArrayList<String>();
		sqls.add("DROP TABLE "+convertTableName(table.getTableName()));
		return sqls.toArray(new String[sqls.size()]);
	}

	public String getSchemaName(DatabaseConnection conn) {
		return null;
	}

    protected String buildAlterStatement(TableDefinition tableDef) throws RepositoryException {
    	StringBuffer sb = new StringBuffer();
    	for(Iterator<ColumnDefinition> it=tableDef.getColumnIterator(); it.hasNext();){
    		sb.append(_buildCreateStatement(tableDef,Arrays.asList(new ColumnDefinition[]{it.next()}).iterator()));
    		sb.append(";");
    	}
    	return sb.toString();
	}
	
    protected boolean isAlterStatementBracketNeccesary() {
		return false;
	}
    
    @Override
    public StringBuilder getDateColumnToStringConversion(String columnName) {
        return getDoubleColumnToStringConversion(columnName);
    }
    
    @Override
    public StringBuilder getDoubleColumnToStringConversion(String columnName) {
        return new StringBuilder().append("CONVERT(").append(columnName).append(", VARCHAR)");
    }
    
    @Override
    public StringBuilder getLongColumnToStringConversion(String columnName) {
        return getDoubleColumnToStringConversion(columnName);
    }
    
    @Override
    public StringBuilder getBooleanColumnToStringConversion(String columnName) {
        return getDoubleColumnToStringConversion(columnName);
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
        
    	
    	StoredProcedureDatabaseCondition cn1 = Conditions.storedProcedure("\"com.exigen.cm.database.dialect.hsql.StoredProcedures.checkPermissionRead\"");
        cn1.addVariable(idColumn);
        cn1.addVariable(securityIdColumn);
        cn1.addParameter(principals.getUserId());
        cn1.addParameter(groups.toString());
        cn1.addParameter(contexts.toString());
        cn1.addParameter(allowBrowse);
        st.addCondition(Conditions.eq(cn1, Boolean.TRUE));    
    }

	public String getDatabaseVersion() {
		return "1.8.0.7";
	}

}


/*
 * $Log: HyperSonicSQLDatabaseDialect.java,v $
 * Revision 1.14  2010/09/07 14:14:48  vsverlovs
 * EPB-198: code_review_EPB-105_2010-09-02
 *
 * Revision 1.13  2009/03/18 09:11:14  vpukis
 * EPBJCR-22: Oracle 11g (11.1.0.7) dialect
 *
 * Revision 1.12  2008/07/22 09:06:27  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2008/07/16 11:42:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/07/09 10:13:07  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/07/09 07:50:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/07/08 08:17:50  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/09 12:36:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/06/02 11:40:22  dparhomenko
 * *** empty log message ***
 *
 */
