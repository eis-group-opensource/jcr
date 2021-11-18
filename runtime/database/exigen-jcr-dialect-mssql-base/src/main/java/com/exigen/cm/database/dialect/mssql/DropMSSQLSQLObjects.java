/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.drop.DropSQLProvider;


/**
 * implementaion of abstract <code>DropSQLObjectsBase</code> class customized for MSSQL
 * 
 */
public class DropMSSQLSQLObjects extends DropSQLProvider{
	
	protected String userName = null;

	/**
	 * @param user - current user
	 */
	public DropMSSQLSQLObjects(String user) {
		super();
		userName = user;
	}
	
	public ArrayList<String> getMaterializedViews() throws RepositoryException {
		data.clear();
		return data;
	}
	
	
	public ArrayList<String> getTables() throws RepositoryException {
		sb = new StringBuffer();
		sb.append("select o.name ");
		sb.append("from sysobjects o, sysusers u ");
		sb.append("where o.uid =  u.uid and u.name='");
		sb.append(getUserName());
		sb.append("' and o.xtype='U'");
		return getSQLObjects(sb.toString(), "name", null);
	}


	public ArrayList<String> getViews() throws RepositoryException {
		sb = new StringBuffer();
		sb.append("select o.name ");
		sb.append("from sysobjects o, sysusers u ");
		sb.append("where o.uid =  u.uid and u.name='");
		sb.append(getUserName());
		sb.append("' and o.xtype='V'");
		return getSQLObjects(sb.toString(), "name", null);
	}

	
	public ArrayList<String> getConstraints() throws RepositoryException {
		sb = new StringBuffer();
		sb.append("select o.name, ");
		sb.append("(select o1.name from sysobjects o1 where o1.id = o.parent_obj) as p_name ");
		sb.append("from sysobjects o, sysusers u ");
		sb.append("where o.uid =  u.uid and u.name='");
		sb.append(getUserName());
		sb.append("' and o.xtype='F'");
		return getSQLObjects(sb.toString(), "p_name", "name");
	}
	
	
	public ArrayList<String> getSequences() throws RepositoryException {
		data.clear();
		return data;
	}
	
    public ArrayList<String> getProcedures() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select o.name ");
        sb.append("from sysobjects o, sysusers u ");
        sb.append("where o.uid =  u.uid and u.name='");
        sb.append(getUserName());
        sb.append("' and (o.xtype='P' or o.xtype='X')");
        return getSQLObjects(sb.toString(), "name", null);
    }

    public ArrayList<String> getFunctions() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select o.name ");
        sb.append("from sysobjects o, sysusers u ");
        sb.append("where o.uid =  u.uid and u.name='");
        sb.append(getUserName());
        sb.append("' and (o.xtype='FN')");
        return getSQLObjects(sb.toString(), "name", null);
    }

	protected String getUserName() throws RepositoryException {
		DatabaseMetaData dmd = connection.getConnectionMetaData();
    	String result;
		try {
			
			result = dmd.getUserName();
			
		} catch (SQLException e) {
			throw new RepositoryException("Error getting user name from connection");
		}
		return result;
	}
	
	@Override
	protected ArrayList<String> getIndexes() {
        return new ArrayList<String>();
	}


}
