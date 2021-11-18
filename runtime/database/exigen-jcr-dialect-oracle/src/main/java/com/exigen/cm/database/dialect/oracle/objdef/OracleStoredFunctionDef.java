/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.vf.commons.logging.LogUtils;

public class OracleStoredFunctionDef extends OracleSourceObjectDef {
	
	private String funcName;
	private Log log = LogFactory.getLog(OracleStoredFunctionDef.class);
	
	public OracleStoredFunctionDef(String name,InputStream in) throws RepositoryException{
		super(name);
		this.setFuncName(name);
		this.setCreateStatement(in);
	}

	public OracleStoredFunctionDef(String name,String sql) throws RepositoryException{
		super(name);
		this.setFuncName(name);
		this.setCreateStatement(sql);
	}

	public String getCheckExistsSQL(){
		return "SELECT object_name FROM user_objects WHERE object_type='FUNCTION' and object_name='"+
			this.getFuncName()+"'";
	}
	
	public String getCheckStatusSQL(){
		return "SELECT status FROM user_objects WHERE object_type='FUNCTION' and object_name='"+
			this.getFuncName()+"'";
	}
	
	public String getDeleteSQL(){
		return "DROP FUNCTION "+this.getFuncName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.FUNCTION, this.getFuncName(), privileged);
	}

	
	public String getCompileSQL(){
		return "ALTER FUNCTION "+this.getFuncName()+ " COMPILE";
	}

	public String getFuncName() {
		return funcName;
	}

	public void setFuncName(String funcName) throws RepositoryException{
		this.funcName = formatName(funcName);
		if (this.funcName==null){
			String msg="Empty or invalid stored Oracle function name '"+funcName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}	
	}
	
	public String getDescription(){
		return "Oracle PL/SQL function '"+this.getFuncName()+"'";
	}

}
