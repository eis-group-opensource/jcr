/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.ResultSetMetadata;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class AbstractDatabaseStatement implements DatabaseStatement{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(DatabaseStatement.class);
    
    protected String _tableName;

    protected PreparedStatement st = null;

    protected ResultSet resultSet;

    private DatabaseDialect dialect;
    
    private boolean ignoreBLOB = false;

	private DatabaseConnection connection;
    
	protected boolean cacheStatement = true;

	private ResultSetMetadata md;
	
	private boolean resultPureMode = false;


	public AbstractDatabaseStatement(String tableName) {
        this._tableName = tableName;
    }

    public boolean execute(DatabaseConnection conn) throws RepositoryException{
        this.dialect = conn.getDialect();
        this.connection = conn;
        st = createStatement(conn);
        boolean result = executeStatement(st, conn);
        try {
            resultSet = st.getResultSet();
        } catch (SQLException exc){
            throw new RepositoryException("Error getting result set", exc);
        }
        processResulSet(conn);
        if (isAutoCloseStatement()){
            close();
        } 
        return result;
    }

    protected void processResulSet(DatabaseConnection conn) throws RepositoryException {
        // do nothing
        
    }

    protected boolean executeStatement(PreparedStatement st,DatabaseConnection conn) throws RepositoryException {
        applyParameters(st, conn, 1);
        try {
            return _executeStatement(st, conn);
        } catch (SQLException exc){
        	//exc.printStackTrace();
            throw new RepositoryException("Error executing prepared statement", exc);
        }
    }


    protected boolean _executeStatement(PreparedStatement st,DatabaseConnection conn) throws SQLException, RepositoryException {
        return st.execute();
    }

    protected PreparedStatement createStatement(DatabaseConnection conn) throws RepositoryException {
        String sql = assemblSQL(conn);
        LogUtils.debug(log, sql);
        //System.out.println(sql);
        st = conn.prepareStatement(sql, cacheStatement);
        return st;
    }

    abstract public String assemblSQL(DatabaseConnection conn) throws RepositoryException ;
    
    abstract protected boolean isAutoCloseStatement();

    abstract public int applyParameters(PreparedStatement st, DatabaseConnection conn, int startPos) throws RepositoryException;

    
    public void close(){
        try {
            if (resultSet != null){
            	//TODO do we need this always, or only for MSSQL Lock statement ?
            	
            	if (dialect.isMSSQL()){
	            	try {
	            		if (!resultSet.isLast()){
			            	while (resultSet.next()){
			            	}
	            		}
	            	} catch (SQLException exc){
	            	}
            	}
                resultSet.close();
                resultSet = null;
            }
        } catch (Exception exc){
            exc.printStackTrace();
        } catch (InternalError e){
        	e.printStackTrace();
        }
        try {
            if (st != null){
                DatabaseTools.closePreparedStatement(st, connection);
                st = null;
            }
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }
    
    protected RowMap buildRow() throws RepositoryException {
        return DatabaseTools.assembleRow(resultSet, dialect, getMetadata(resultSet));
    }

    private ResultSetMetadata getMetadata(ResultSet resultSet2) throws RepositoryException {
		if (md == null){
			md = new ResultSetMetadata(resultSet2, ignoreBLOB, buildTableAliasMapping(), isResultPureMode());
		}
		return md;
	}

	protected abstract HashMap<String, String> buildTableAliasMapping();

	public String getTableName(DatabaseDialect dialect) throws RepositoryException{
        return dialect.convertTableName(_tableName);
    }
	
	public String getOriginalTableName(){
		return _tableName;
	}

    
    
    protected void setIgnoreBLOB(boolean b) {
        ignoreBLOB=b;
    }
    
    /**
     * Returns <code>true</code> if BLOB columns shouldn't be included in result.
     * @return
     */
    public boolean isIgnoreBLOB(){
        return ignoreBLOB;
    }

    
	public boolean isResultPureMode() {
		return resultPureMode;
	}

	public void setResultPureMode(boolean resultPureMode) {
		this.resultPureMode = resultPureMode;
	}
    
}


/*
 * $Log: AbstractDatabaseStatement.java,v $
 * Revision 1.7  2009/03/12 10:57:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2009/01/26 10:53:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2007/11/30 07:47:48  dparhomenko
 * Fix lock problem
 *
 * Revision 1.4  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.3  2007/09/11 12:38:51  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/06/07 09:20:12  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.15  2007/02/22 09:24:18  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.14  2006/12/15 11:54:33  dparhomenko
 * PTR#1803217 code reorganization
 *
 * Revision 1.13  2006/11/27 09:49:58  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.12  2006/11/14 07:37:21  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.11  2006/11/02 09:49:34  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.10  2006/09/29 13:55:27  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.9  2006/09/27 12:33:00  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.8  2006/08/22 11:50:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.7  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.6  2006/08/04 12:33:35  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.5  2006/07/12 10:10:25  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/04 10:52:05  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/05/25 14:49:16  dparhomenko
 * PTR#1801955 add JBOSS support
 *
 * Revision 1.2  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.1  2006/05/03 12:07:08  dparhomenko
 * PTR#0144983 make DatabaseStatement as interface
 *
 */