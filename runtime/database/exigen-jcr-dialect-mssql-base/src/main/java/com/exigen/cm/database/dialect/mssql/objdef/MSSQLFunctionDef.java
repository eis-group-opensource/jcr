/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.objdef.AbstractDBSourceObjectDef;
import com.exigen.vf.commons.logging.LogUtils;

public class MSSQLFunctionDef extends AbstractDBSourceObjectDef {
	
	private String funcName;
	private Log log = LogFactory.getLog(MSSQLFunctionDef.class);
	private String schema;
	
	public MSSQLFunctionDef(String name,InputStream in,String schema) throws RepositoryException{
		super(name);
		this.setFuncName(name);
		this.setCreateStatement(in);
		this.setSchema(schema);
	}
	public MSSQLFunctionDef(String name,String sql,String schema) throws RepositoryException{
		super(name);
		this.setFuncName(name);
		this.setCreateStatement(sql);
		this.setSchema(schema);
	}
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;
	}
	
	public String getCheckExistsSQL(){
		if (this.getSchema()==null){
			return "SELECT name FROM dbo.sysobjects WHERE xtype='FN' AND uid=USER_ID() AND name='"+
				this.getFuncName()+"'";
		}else{
			return "SELECT 1 FROM sys.schemas s,sys.objects f WHERE s.name='"
				+ this.getSchema() + "' AND s.schema_id=f.schema_id AND f.name='"
				+ this.getFuncName()+"' AND f.type='FN'";
		}		
	}
	
	public String getDeleteSQL(){
		return "DROP FUNCTION "
		+ ( this.getSchema()==null ? "" : this.getSchema()+"." )
		+ this.getFuncName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.FUNCTION, this.getFuncName(), false);
	}

	public String getFuncName() {
		return funcName;
	}

	public void setFuncName(String funcName) throws RepositoryException {
		this.funcName = formatName(funcName);
		if (this.funcName==null){
			String msg="Empty or invalid MSSQL function name '"+funcName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}
	
	public String getDescription(){
		return "MS SQL user defined function '"+this.getFuncName()+"'";
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

}
