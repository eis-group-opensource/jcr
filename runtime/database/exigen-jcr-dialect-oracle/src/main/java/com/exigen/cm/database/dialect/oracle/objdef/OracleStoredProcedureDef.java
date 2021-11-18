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

public class OracleStoredProcedureDef extends OracleSourceObjectDef {
	
	private String procName;
	private Log log = LogFactory.getLog(OracleStoredProcedureDef.class);
	
	public OracleStoredProcedureDef(String name,InputStream in) throws RepositoryException{
		super(name);
		this.setProcName(name);
		this.setCreateStatement(in);
	}

	public OracleStoredProcedureDef(String name,String sql) throws RepositoryException{
		super(name);
		this.setProcName(name);
		this.setCreateStatement(sql);
	}

	public String getCheckExistsSQL(){
		return "SELECT object_name FROM user_objects WHERE object_type='PROCEDURE' and object_name='"+
			this.getProcName()+"'";
	}
	
	public String getCheckStatusSQL(){
		return "SELECT status FROM user_objects WHERE object_type='PROCEDURE' and object_name='"+
			this.getProcName()+"'";
	}
	
	public String getDeleteSQL(){
		return "DROP PROCEDURE "+this.getProcName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.PROCEDURE, this.getProcName(), privileged);
	}

	public String getCompileSQL(){
		return "ALTER PROCEDURE "+this.getProcName()+ " COMPILE";
	}

	public String getProcName() {
		return procName;
	}

	public void setProcName(String procName) throws RepositoryException{
		this.procName = formatName(procName);
		if (this.procName==null){
			String msg="Empty or invalid stored Oracle procedure name '"+procName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}	
	}
	
	public String getDescription(){
		return "Oracle PL/SQL procedure '"+this.getProcName()+"'";
	}

}
