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

public class OraclePackageDef extends OracleSourceObjectDef {
	
	private String pkgName;
	private Log log = LogFactory.getLog(OraclePackageDef.class);
	
	public OraclePackageDef(String name,InputStream in) throws RepositoryException{
		super(name);
		this.setPkgName(name);
		this.setCreateStatement(in);
	}
	
	public OraclePackageDef(String name,String sql) throws RepositoryException{
		super(name);
		this.setPkgName(name);
		this.setCreateStatement(sql);
	}


	public String getCheckExistsSQL(){
		return "SELECT object_name FROM user_objects WHERE object_type='PACKAGE BODY' and object_name='"+
			this.getPkgName()+"'";
	}
	
	public String getCheckStatusSQL(){
		return "SELECT status FROM user_objects WHERE (object_type='PACKAGE' OR object_type='PACKAGE BODY') "+
		    " and object_name='"+this.getPkgName()+"' ORDER BY DECODE(status,'VALID',1,0) ASC";
	}
	
	public String getDeleteSQL(){
		return "DROP PACKAGE "+this.getPkgName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.PACKAGE, this.getPkgName(), privileged);
	}

	
	public String getCompileSQL(){
		return "ALTER PACKAGE "+this.getPkgName()+ " COMPILE";
	}

	public String getPkgName() {
		return pkgName;
	}

	public void setPkgName(String pkgName)  throws RepositoryException {
		this.pkgName = formatName(pkgName);
		if (this.pkgName==null){
			String msg="Empty or invalid Oracle PLSQL package name '"+pkgName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}
	
	public String getDescription(){
		return "Oracle PL/SQL package '"+this.getPkgName()+ "'";
	}

}
