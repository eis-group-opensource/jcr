/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils.dbtest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.dialect.oracle.objdef.OracleTextPolicyDef;
import com.exigen.cm.database.objdef.DBObjectDef;

public class Oracle10DatabaseTest extends DatabaseAbstractTest {


	public Oracle10DatabaseTest(Map<String, String> config) throws RepositoryException {
		super(config);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void testPermissions(ArrayList<String> errors) {
		
	}


	@Override
	protected void testPreConditions(ArrayList<String> errors) throws RepositoryException {
		//test quota
		Statement st = conn.createStatement();
		try {
			st.execute("select COUNT(*) as cc from user_sys_privs where privilege='UNLIMITED TABLESPACE'");
			ResultSet rs = st.getResultSet();
			rs.next();
			int result = rs.getInt("cc");
			if (result != 1){
				//check individual tablespaces
				st.execute("select b.bytes as BYTES,b.max_bytes as MAX_BYTES from  user_users a, user_ts_quotas b where a.default_tablespace=b.tablespace_name");
				rs = st.getResultSet();
				if (rs.next()){
					int total = rs.getInt("MAX_BYTES");
					int used = rs.getInt("BYTES");
					if (total != -1){
						float usage = (float) used / (float) total * (float)100; 
						if (usage > 90){
							errors.add("Please increase user quota");
						}
					}
				} else {
					errors.add("Please specify quota for database user");
				}
				
			}
		} catch (SQLException exc){
			throw new RepositoryException("Error checking quotas", exc);
		}
		
		
		//check permission:create table create sequence
		try {
			st.execute("select privilege from session_privs");
			ResultSet rs = st.getResultSet();
			ArrayList<String> permissions = new ArrayList<String>();
			while(rs.next()){
				String p = rs.getString("privilege");
				permissions.add(p.toUpperCase());
			}
			checkPermission(errors, permissions, "CREATE TRIGGER");
			checkPermission(errors, permissions, "CREATE TABLE");
			checkPermission(errors, permissions, "CREATE SEQUENCE");
			checkPermission(errors, permissions, "CREATE PROCEDURE");
			
		} catch (Exception exc) {
			throw new RepositoryException("Error checking permissions", exc);
		}
	}

	private void checkPermission(ArrayList<String> errors, ArrayList<String> permissions, String p) {
		if (!permissions.contains(p.toUpperCase())){
			errors.add("Please add "+p+" privilege to database user");
		}
	}

	@Override
	protected void testConfiguration(ArrayList<String> errors) {
		if (config.get(Constants.PROPERTY_ORACLE_CTXSYS_PASSWORD) == null){
			errors.add("Key "+Constants.PROPERTY_ORACLE_CTXSYS_PASSWORD+" not defined in configuration");
		}		
	}

	protected boolean skipDBObject(DBObjectDef dbObject) {
		// TODO Auto-generated method stub
		return dbObject instanceof OracleTextPolicyDef;
	}



}
