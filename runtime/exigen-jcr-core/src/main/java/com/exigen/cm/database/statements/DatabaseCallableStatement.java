/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.vf.commons.logging.LogUtils;

public class DatabaseCallableStatement extends AbstractDatabaseStatement{
    
    private static final Log log = LogFactory.getLog(DatabaseCallableStatement.class);
    
    private String procedureName;
    
    private List<Object> parameters = new ArrayList<Object>();
    
    private Integer returnParameterType = null;

    private int returmParameterIndex;

    private Object result;

    public DatabaseCallableStatement(String procedureName){
        super(procedureName);
        this.procedureName = procedureName;
    }
    
    public void registerReturnParameterType(int value){
        returnParameterType = value;
    }

    @Override
    public String assemblSQL(DatabaseConnection conn) throws RepositoryException {
        StringBuffer sb = new StringBuffer();
        if (!conn.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_HYPERSONIC)){
	        sb.append("{");
        
        if (returnParameterType != null){
	            sb.append(" ? = ");
	        }
        }
        sb.append("call ");
        if (conn.getDialect().getDatabaseVendor() == DatabaseDialect.VENDOR_HYPERSONIC){
        	sb.append("\"");
        	sb.append(conn.getDialect().getClass().getPackage().getName().replace('/', '.'));
        	sb.append(".StoredProcedures.");
        }
        sb.append(procedureName);
        if (conn.getDialect().getDatabaseVendor() == DatabaseDialect.VENDOR_HYPERSONIC){
        	sb.append("\"");
        }
        sb.append("(");
        boolean first = true;
        for(Object obj:parameters){
            if (!first){
                sb.append(", ");
            }
            sb.append("?");
            first = false;
        }
        sb.append(")");
        if (!conn.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_HYPERSONIC)){
            sb.append("}");
        }

        return sb.toString();
    }

    @Override
    protected boolean isAutoCloseStatement() {
        return true;
    }

    @Override
    public int applyParameters(PreparedStatement st, DatabaseConnection conn, int startPos) throws RepositoryException {
        if (!conn.getDialect().getDatabaseVendor().equals(DatabaseDialect.VENDOR_HYPERSONIC)){
	        if (returnParameterType != null){
	            try {
	                this.returmParameterIndex = startPos;
	                ((CallableStatement)st).registerOutParameter(startPos++,returnParameterType);
	            } catch (SQLException e) {
	                throw new RepositoryException("Error setting return type for prepared statement", e);
	            }
	        }
        } else {
        	//startPos++;
        }
        for(Object value:parameters){
            DatabaseTools.bindParameter(st, conn.getDialect(), startPos++, value, false);
        }
        return startPos;
    }
    
    public void addParameter(Object value){
        parameters.add(value);
    }
    
    protected PreparedStatement createStatement(DatabaseConnection conn) throws RepositoryException {
        String sql = assemblSQL(conn);
        LogUtils.debug(log, sql);
        st = conn.prepareCallableStatement(sql, true);
        return st;
    } 
    
    public Object getReturnValue(){
        return result;
    }

    @Override
    protected void processResulSet(DatabaseConnection conn)
			throws RepositoryException {
		if (returnParameterType != null) {
			if (!conn.getDialect().getDatabaseVendor().equals(
					DatabaseDialect.VENDOR_HYPERSONIC)) {
				try {
					result = ((CallableStatement) st)
							.getObject(returmParameterIndex);
				} catch (SQLException e) {
					throw new RepositoryException(
							"Error getting result form callable statement");
				}
			} else {
				ResultSet rs;
				try {
					rs = st.getResultSet();
					rs.next();
					result = rs.getObject(1);
				} catch (SQLException e) {
					throw new RepositoryException(
							"Error getting result form callable statement");
				}
			}
		}
	}
    
    protected HashMap<String, String> buildTableAliasMapping(){
    	return new HashMap<String,String>();
    }

}


/*
 * $Log: DatabaseCallableStatement.java,v $
 * Revision 1.2  2009/01/26 10:53:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2006/09/29 13:55:27  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.9  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.8  2006/07/11 13:09:13  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 * Revision 1.7 2006/07/11 13:02:32
 * dparhomenko PTR#1802310 Add new features to DatabaseConnection
 * 
 * Revision 1.6 2006/07/11 09:29:03 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 * Revision 1.5 2006/07/11 09:26:12 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 * Revision 1.4 2006/07/10 12:24:02 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 * Revision 1.3 2006/07/05 07:24:45 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 * Revision 1.2 2006/07/04 12:58:56 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 * Revision 1.1 2006/07/04 10:52:05 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 */