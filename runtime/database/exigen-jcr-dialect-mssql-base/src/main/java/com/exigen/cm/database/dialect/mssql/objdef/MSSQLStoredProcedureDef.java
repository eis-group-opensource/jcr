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

public class MSSQLStoredProcedureDef extends AbstractDBSourceObjectDef {
	
	private String procName;
	private Log log = LogFactory.getLog(MSSQLStoredProcedureDef.class);
	private String schema;
	
	/*public MSSQLStoredProcedureDef(String name,InputStream in) throws RepositoryException{
		super(name);
		this.setProcName(name);
		this.setCreateStatement(in);
	}
	
	public MSSQLStoredProcedureDef(String name,String sql) throws RepositoryException{
		super(name);
		this.setProcName(name);
		this.setCreateStatement(sql);
	}*/

	public MSSQLStoredProcedureDef(String name,InputStream in,String schema) throws RepositoryException{
		super(name);
		this.setProcName(name);
		this.setCreateStatement(in);
		this.setSchema(schema);
	}
	public MSSQLStoredProcedureDef(String name,String sql,String schema) throws RepositoryException{
		super(name);
		this.setProcName(name);
		this.setCreateStatement(sql);
		this.setSchema(schema);
	}
	

	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;
	}
	
	public String getCheckExistsSQL(){
		/*return "select name from dbo.sysobjects where xtype='P' and uid=USER_ID() and name='"+
			this.getProcName()+"'";*/
		
		if (this.getSchema()==null){
			return "SELECT name FROM dbo.sysobjects WHERE xtype='P' AND uid=USER_ID() AND name='"+
				this.getProcName()+"'";
		}else{
			return "SELECT 1 FROM sys.schemas s,sys.procedures p WHERE s.name='"
				+ this.getSchema() + "' AND s.schema_id=p.schema_id AND p.name='"
				+ this.getProcName()+"'";
		}		
	}
	
	public String getDeleteSQL(){
		return "DROP PROCEDURE "
		+ ( this.getSchema()==null ? "" : this.getSchema()+"." )
		+ this.getProcName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.PROCEDURE, this.getProcName(), false);
	}

	public String getProcName() {
		return procName;
	}

	public void setProcName(String procName) throws RepositoryException {
		this.procName = formatName(procName);
		if (this.procName==null){
			String msg="Empty or invalid MSSQL stored procedure name '"+procName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}
	
	public String getDescription(){
		return "MS SQL stored procedure '"+this.getProcName()+"'";
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

}
