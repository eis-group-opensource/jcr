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

public class OracleStoredJavaClassDef extends OracleSourceObjectDef {
	
	private String className;
	private Log log = LogFactory.getLog(OracleStoredJavaClassDef.class);

	public OracleStoredJavaClassDef(String name,InputStream in) throws RepositoryException{
		super(name);
		this.setClassName(name);
		this.setCreateStatement(in);
	}

	public OracleStoredJavaClassDef(String name,String sql) throws RepositoryException{
		super(name);
		this.setClassName(name);
		this.setCreateStatement(sql);
	}

	public String getCheckExistsSQL(){
		return "SELECT object_name FROM user_objects WHERE object_type='JAVA CLASS' and object_name='"+
			this.getClassName()+"'";
	}
	
	public String getCheckStatusSQL(){
		return "SELECT status FROM user_objects WHERE object_type='JAVA CLASS' "+
		    " and object_name='"+this.getClassName()+"'";
	}

	public String getClassName() {
		return className;
	}
	
	public String getDeleteSQL(){
		return "DROP JAVA SOURCE "+this.getClassName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.JAVASOURCE, this.getClassName(), privileged);
	}
	
	
	public String getCompileSQL(){
		return "ALTER JAVA SOURCE "+this.getClassName()+ " COMPILE";
	}

	public void setClassName(String className) throws RepositoryException{
		this.className = formatName(className);
		if (this.className==null){
			String msg="Empty or invalid Oracle stored Java class name '"+className+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}	
	}
	
	public String getDescription(){
		return "Oracle stored Java class '"+this.getClassName()+"'";
	}

}
