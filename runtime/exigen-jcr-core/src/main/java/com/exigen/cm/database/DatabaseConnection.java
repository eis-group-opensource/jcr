/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import static com.exigen.cm.Constants.TABLE_NODE_LOCK;
import static com.exigen.cm.Constants.TABLE_SYSTEM_OBJECTS;
import static com.exigen.cm.Constants.TABLE_SYSTEM_OBJECTS__NAME;
import static com.exigen.cm.Constants.TABLE_SYSTEM_OBJECTS__PRIVILEGED;
import static com.exigen.cm.Constants.TABLE_SYSTEM_OBJECTS__TYPE;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.collections.map.ReferenceIdentityMap;

import com.exigen.cm.Constants;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TrabsactionSynchronization;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.vf.commons.logging.LogUtils;

public class DatabaseConnection{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(DatabaseConnection.class);

    private Connection connection;

    private ConnectionProvider connectionProvider;

    private boolean allowClose = true;
      
    boolean allowCommitRollback;

    private Throwable createdThread;

    private boolean autoCommit = false;
    
    private Long _connectionId;

    private TrabsactionSynchronization transactionSynchronization;

    public DatabaseConnection(ConnectionProvider connectionProvider) throws RepositoryException {
        createdThread = new Throwable();
        this.connectionProvider = connectionProvider;
        
        try {
            this.connection = createConnection();
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
        
        addSyncronization();
        
        initConnection();
    }
 
    public DatabaseConnection(ConnectionProvider connectionProvider, Connection conn) throws RepositoryException {
        createdThread = new Throwable();
        this.connection = conn;
        this.connectionProvider = connectionProvider;
        
        addSyncronization();
        
        initConnection();
    }

	private void addSyncronization() throws RepositoryException {
        this.transactionSynchronization =  TransactionHelper.getInstance().createTransactionSynchranization();
    }

    private void initConnection() throws RepositoryException {
		//TODO may be cache on repository level on connection level
        JCRTransactionManager manager = TransactionHelper.getInstance().getTransactionManager();
        try {
            if (manager == null || manager.getTransaction() == null){
                allowCommitRollback = true;;
            } else {
                allowCommitRollback = false;
            }
        } catch (Exception exc){
            throw new RepositoryException(exc);
        }
        
        //try {
			//conn.createStatement().execute("BEGIN TRANSACTION");
		//} catch (SQLException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
	}

    public DatabaseDialect getDialect() throws RepositoryException {
        sanityCheck();
        return connectionProvider.getDialect();
    }

    private void sanityCheck() throws RepositoryException {
        if (connection == null) {
            throw new RepositoryException("Connection closed");
        }

    }
    
    public boolean isLive(){
    	try {
	    	if (connection.isClosed()) {
	    		connection = null;
	    	}
    	} catch (Exception exc){
    		connection = null;
    	}
        return connection != null;
    }
    
    public boolean allowCommitRollback() throws RepositoryException{
        return allowCommitRollback;
    }

    public void close() throws RepositoryException {
        if (allowClose){
        	if (connection == null){
        		return;
        	}
            sanityCheck();
            try {
                closeStatements();
                if (allowCommitRollback() && !connection.isClosed()) {
                    connection.rollback();
                }
                if (!connection.isClosed()) {
                    JCRTransaction tr = TransactionHelper.getCurrentTransaction();
                    if (tr == null || tr.allowConnectionClose()){
                        connection.close();
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
            connection = null;
        }
    }
	
	/** called from within 'try' */
    protected void closeStatements() {
        //close statements
        if (statements != null && connection != null){
	        if (statements.containsKey(connection)){
	            Map<String, Statement> sts = statements.get(connection);
	            for(Iterator<String> it = sts.keySet().iterator(); it.hasNext();){
	                String sql = it.next();
	                Statement st = sts.get(sql);
	                try {
	                    st.close();
	                } catch (Exception e){
	                    
	                }
	            }
	            sts.clear();
	            statements.remove(connection);
	        }
        }
    }

    public void commit() throws RepositoryException {
        sanityCheck();
        lockCache.clear();
        try {
            if (allowCommitRollback()){
            	//System.out.println("COMMIT");
                //LogUtils.debug(log, "COMMIT");
                if (!autoCommit){
                   // connection.createStatement().execute("COMMIT TRANSACTION");
                    connection.commit();
                    transactionSynchronization.commit();
                    /*connection.close();
                    connection = connectionProvider.createSQLConnection();
                    initConnection();*/
                    //connection.createStatement().execute("BEGIN TRANSACTION");
                }
            } else {
                //LogUtils.debug(log, "Declarative transactions: commit not allowed");
            }
            //closeStatements();
        } catch (SQLException exc) {
            throw new RepositoryException("Error commiting data to database",
                    exc);
        }
    }

    public void rollback() throws RepositoryException {
        sanityCheck();
        lockCache.clear();
        try {
            closeStatements();
        	if (allowCommitRollback()){
	            LogUtils.debug(log, "ROLLBACK");
	            connection.rollback();
        	}
        } catch (SQLException exc) {
            LogUtils.debug(log, "Error rollback", exc);
        }
    }

    public Long nextId() throws RepositoryException {
        sanityCheck();
        return connectionProvider.nextId(this);
    }

    public DatabaseMetaData getConnectionMetaData() throws RepositoryException {
        sanityCheck();
        try {
            return connection.getMetaData();
        } catch (SQLException exc){
            throw new RepositoryException(exc);
        }
    }

    public void execute(String sql) throws RepositoryException{
        sanityCheck();
        Statement st = null;
         try {
             st = connection.createStatement();
             // Some CREATE .. SQL statements can be extremely long (e.g. CREATE ASSEMBLY under SQL2005)
             if (sql.length()<=10240){
            	 LogUtils.debug(log, sql);
             }else{
            	 LogUtils.info(log, sql.substring(0,10240)+"... rest of sql is truncated");
             }
             st.execute(convertSQL(sql));
         } catch (SQLException e){
             throw new RepositoryException(e);
         } finally {
             closeStatement(st);
         }
         
     }

    public void executeUpdate(String sql) throws RepositoryException{
        sanityCheck();
        Statement st = null;
         try {
             st = connection.createStatement();
             LogUtils.debug(log, sql);
             st.executeUpdate(sql);
         } catch (SQLException e){
             throw new RepositoryException(e);
         } finally {
             closeStatement(st);
         }
         
     }
    
    private void execute(String sql, Statement st) throws RepositoryException{
         try {
             LogUtils.debug(log, convertSQL(sql));
             st.execute(sql);
         } catch (SQLException e){
             throw new RepositoryException(e);
         } 
     }
    



	private String convertSQL(String sql) throws RepositoryException, SQLException {
		//sql = sql.replace("$$SCHEMA_NAME$$", getDialect().getSchemaName(this));
		return sql;
	}

	public void dropTable(TableDefinition table) throws RepositoryException {
        sanityCheck();
        Statement st = null;;
        try {
            st = connection.createStatement();
            String[] sqls = getDialect().buildDropTableStatement(table);
            for(String sql:sqls){
            	execute(sql, st);
            }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
		
	}

    
    // createTables() does not create dialect specific database objects before/after table
    public void createTables(TableDefinition[] tables) throws RepositoryException {
        sanityCheck();
        Statement st = null;;
        try {
            st = connection.createStatement();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
        try {
            for(int i = 0; i < tables.length ; i++){
            	//System.out.println(tables[i].getTableName());
                //create tables
            	if (!tables[i].getTableName().equals(TABLE_SYSTEM_OBJECTS) && !tables[i].isAlter()){
            		registerSysObject(DatabaseObject.TABLE, getDialect().convertTableName(tables[i].getTableName()), false);
            	}
                String sql = getDialect().buildCreateStatement(tables[i]);
                execute(sql, st);
            	if (tables[i].getTableName().equals(TABLE_SYSTEM_OBJECTS) && !tables[i].isAlter()){
            		registerSysObject(DatabaseObject.TABLE, getDialect().convertTableName(tables[i].getTableName()), false);
            	}
            }
            
            for(int i = 0; i < tables.length ; i++){
                String[][] sqls = getDialect().buildCreateIndexStatements(tables[i]);
                for (int j = 0; j < sqls.length; j++) {
                    String[] sqlIndex = sqls[j];
                    if (sqlIndex != null && sqlIndex[1].length() > 0){
                    	registerSysObject(DatabaseObject.INDEX, sqlIndex[0], false);
                        execute(sqlIndex[1], st);
                    }     
                }       
            }        
            
            for(int i = 0; i < tables.length ; i++){
                //add pk 
                if (tables[i].getPKColumnIterator().hasNext() && !tables[i].isIndexOrganized()){
                    String sql = getDialect().buildPKAlterStatement(tables[i]);
                    execute(sql, st);
                }
                
            }
            for(int i = 0; i < tables.length ; i++){
                //add fk
                String sql = getDialect().buildFKAlterStatement(tables[i], this);
                if (sql != null && sql.length() > 0){
                    execute(sql, st);
                }
                
            }
        } finally {
            try {
                st.close();
            } catch (SQLException e) {
            }
        }
        
    }
    
    

    public void registerSysObject(DatabaseObject type, String name, boolean privileged) throws RepositoryException {
		DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_SYSTEM_OBJECTS);
		st.addValue(SQLParameter.create(TABLE_SYSTEM_OBJECTS__TYPE, type.name()));
		st.addValue(SQLParameter.create(TABLE_SYSTEM_OBJECTS__NAME, name));
		st.addValue(SQLParameter.create(TABLE_SYSTEM_OBJECTS__PRIVILEGED, privileged));
		st.execute(this);
		commit();
		
	}

	public RowMap loadRow(String tableName, String pkColumnName, Object pkValue) throws RepositoryException {
        sanityCheck();
        DatabaseSelectOneStatement st =DatabaseTools.createSelectOneStatement(tableName, pkColumnName, pkValue);
        st.execute(this);
        RowMap row = st.getRow();
        return row;
    }

//    private final static Map<Connection, Map<String, Statement>> statements = new WeakHashMap<Connection, Map<String, Statement>>();
    private final static Map<Connection, Map<String, Statement>> statements = new ReferenceIdentityMap(ReferenceIdentityMap.WEAK, ReferenceIdentityMap.HARD, true);
    
    // for calling stored procedures
    public CallableStatement prepareCallableStatement(String sql, boolean cache) throws RepositoryException{
        Map<String, Statement> sts = getStatementCache();
    	try{
            if (cache){
                if (sts.containsKey(sql)){
                	CallableStatement cst=connection.prepareCall(sql);
                	cst.clearParameters();
                    cst.clearWarnings();
                    return cst;
                } 
                CallableStatement cst=connection.prepareCall(sql);
                sts.put(sql, cst);
                return cst;
            } else {
            	CallableStatement cst=connection.prepareCall(sql);
            	return cst;
            }
    	}catch (SQLException e){
    		e.printStackTrace();
    		throw new RepositoryException("Error preparing call: "+e.getMessage());
    	}
    }    

    
    public PreparedStatement prepareStatement(String sql, boolean cache) throws RepositoryException {
        sanityCheck();
        try {
            Map<String, Statement> stmtCache = getStatementCache();
            if (cache){
            	try {
	                if (stmtCache.containsKey(sql)){
	                    PreparedStatement stmt = (PreparedStatement) stmtCache.get(sql);
	                    /*try {
	                        ResultSet rs = stmt.getResultSet();
	                        if (rs != null){
	                            rs.cancelRowUpdates();
	                            rs.close();
	                        }
	                    } catch (Exception e) {
	                        System.err.println("Error closing result set "+e.getMessage());
	                    }*/
	                    stmt.clearParameters();
	                    stmt.clearWarnings();
                    	return stmt;
	                }
            	} catch (SQLException exc){
            		stmtCache.remove(sql);
            		//error getting statement from cache
            	}
                PreparedStatement stmt = connection.prepareStatement(sql);
                stmtCache.put(sql, stmt);
                return stmt;
            } else {
                PreparedStatement stmt = connection.prepareStatement(sql);
                return stmt;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error preparing sql statement :" + sql, e);
        }
    }

	private Map<String, Statement> getStatementCache() {
	    try {
	        if (connection.isClosed()) {
	            recreateConnection();
	    	}
	    } catch (Exception e) {
	        recreateConnection();
        }	    
	    Map<String, Statement> stmtCache;
		if (statements.containsKey(connection)){
			stmtCache = statements.get(connection);
		} else {
			stmtCache = new HashMap<String, Statement>();
		    statements.put(connection, stmtCache);
		}
		return stmtCache;
	}

    private void recreateConnection() {
        statements.remove(connection); // by identity
        try {
            connection = createConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }    
    

    public DatabaseConnection getNewConnection() throws RepositoryException {
        return connectionProvider.createConnection();
    }

    protected void finalize() throws Throwable {
        try {
            if (connection != null){
                log.error("Unclosed database connection", createdThread);
                close();
            }
        } catch (Exception e) {
        }
        super.finalize();
    }

    /*public _RepositoryImpl getRepository() {
        return repository;
    }*/

    public void setAllowClose(boolean value) {
        this.allowClose = value;
        
    }

    private HashSet<String> lockCache = new HashSet<String>();
    
    public void lockTableRow(String tableName, Object id) throws RepositoryException{
    	String key = tableName+"_"+id;
    	/*if (lockCache.contains(key)){
    		return;
    	} else {*/
            DatabaseTools.lockTableRow(this, tableName, id);
    		//getDialect().lockTableRow(this, tableName, id);
    		lockCache.add(key);
    	//}
    }

    public void lockTableRow(String tableName,String pkField ,Object id) throws RepositoryException{
    	String key = tableName+"_"+id;
    	/*if (lockCache.contains(key)){
    		return;
    	} else {*/
    		DatabaseTools.lockTableRow(this, tableName, pkField, id);
    		lockCache.add(key);
    	//}
    }

    public String getDatabaseName() throws RepositoryException{
        sanityCheck();
        String result;
		try {
			result = connection.getCatalog();
	        if (result == null){
	            DatabaseMetaData dmd = connection.getMetaData();
	        	result = dmd.getUserName();
	            /*ResultSet result = dmd.getSchemas();
	            while (result.next()){
	                for(int i = 1 ; i <= result.getMetaData().getColumnCount(); i++){
	                	String name =  result.getMetaData().getColumnName(i);
	                	
	                }
	            }*/
	        }
	        return result;
		} catch (SQLException e) {
			throw new RepositoryException("Error retrieving database name", e);
		} 
    }
    
    private String userName;
    public String getUserName() throws RepositoryException {
    	if (userName == null){
        	userName = connectionProvider.getDialect().getUserName(connection);
    	}
    	return userName;
    }
    
    public String getDatabaseURL() throws RepositoryException{
        sanityCheck();
        try {
	        DatabaseMetaData dmd = connection.getMetaData();
	    	String result = dmd.getURL();
	        return result;
        } catch (SQLException e){
        	throw new RepositoryException(e.getMessage());
        }
    }


    public void setAutoCommit(boolean value) throws RepositoryException {
        sanityCheck();
		setAutoCommit(connection, value);
	}

	protected void setAutoCommit(Connection c, boolean value) throws RepositoryException {
        try {
            c.setAutoCommit(value);
            this.autoCommit = value;
        } catch (SQLException e) {
        	close();
            throw new RepositoryException(e);
        }
    }


    public boolean isAllowClose() {
        return allowClose;
    }

	public String getCatalog() throws RepositoryException {
		try {
			
			String result =   connection.getCatalog();
			if (result != null){
	        	result = result.toLowerCase();
			}
			return result;
		}catch (Exception e) {
			return null;
		}
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}

	public Statement createStatement() throws RepositoryException {
		try {
			return connection.createStatement();
		} catch (SQLException e) {
			throw new RepositoryException(e);
		}
	}

	
    public void closeStatement(Statement st) {
   		//sanityCheck();
/*        try {
            st.close();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }*/
        if (st != null && !getStatementCache().values().contains(st)){
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
          //  System.out.println("skip close");
        }
    }

    public void lockNode(Long nodeId) throws RepositoryException{
    	lockTableRow(TABLE_NODE_LOCK,Constants.FIELD_TYPE_ID , nodeId);
    }

    public TrabsactionSynchronization getTransactionSynchronization() {
        return transactionSynchronization;
    }

    public Long getConnectionId() throws RepositoryException {
        if (_connectionId == null){
            this._connectionId = nextId();
        }
        return _connectionId;
    }
    
	protected Connection createConnection() throws Exception { 

		return connectionProvider.createSQLConnection();
	}
    
    
}

/*
 * $Log: DatabaseConnection.java,v $
 * Revision 1.21  2011/09/09 10:51:09  jkersovs
 * EPB-335 'DatabaseConnection statements cache breaks on WAS 7.0.0.17'
 * Fix provided by V. Beilins
 *
 * Revision 1.20  2011/09/09 09:42:55  jkersovs
 * EPB-335 'DatabaseConnection statements cache breaks on WAS 7.0.0.17'
 * Fix provided by V. Beilins
 *
 * Revision 1.19  2011/02/22 09:40:58  vsverlovs
 * EPB-233 - Document cannot be scanned and saved to JCR.
 * If statement cache is turned on statement should not be closed.
 * New variable useCache is introduced, to identify when to close statements after use.
 *
 * Revision 1.18  2009/02/03 12:02:12  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.17  2008/07/23 09:56:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.16  2008/07/01 11:10:34  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.15  2008/06/19 06:52:10  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.14  2008/05/07 09:14:10  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.13  2008/01/30 09:28:03  dparhomenko
 * PTR#1806303
 *
 * Revision 1.12  2008/01/25 14:52:48  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2008/01/16 14:09:43  vpukis
 * PTR#0154674 - fixed to call db stored procedures using qualified names under SQL 2005
 *
 * Revision 1.10  2008/01/03 11:56:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2007/12/05 09:43:41  vpukis
 * PTR#0153866 truncate SQL with len> 10K in debug output (SQL 2005 CREATE ASSEMBLY statements generate very long SQL-s)
 *
 * Revision 1.8  2007/11/30 07:47:49  dparhomenko
 * Fix lock problem
 *
 * Revision 1.7  2007/10/22 10:58:49  dparhomenko
 * Fix locks in Spring + MSSQL environment
 *
 * Revision 1.3  2006/02/16 13:53:08  dparhomenko
 * PTR#0144983 start jdbc implementation
 * Revision 1.2 2006/02/13 12:40:45
 * dparhomenko PTR#0143252 start jdbc implementation
 * 
 * Revision 1.1 2006/02/10 15:50:26 dparhomenko PTR#0143252 start jdbc
 * implementation
 * 
 */