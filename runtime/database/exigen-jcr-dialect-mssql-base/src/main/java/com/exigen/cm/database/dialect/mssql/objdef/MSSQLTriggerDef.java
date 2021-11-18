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

public class MSSQLTriggerDef extends AbstractDBSourceObjectDef {
	
	private String triggerName;
	private Log log = LogFactory.getLog(MSSQLTriggerDef.class);
	private String schema;
	
	public MSSQLTriggerDef(String name,InputStream in, String schemaName) throws RepositoryException{
		super(name);
		this.setTriggerName(name);
		this.setCreateStatement(in);
		this.setSchema(schemaName);
	}
	
	public MSSQLTriggerDef(String name,String sql, String schemaName) throws RepositoryException{
		super(name);
		this.setTriggerName(name);
		this.setCreateStatement(sql);
		this.setSchema(schemaName);
	}

	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;//
	}
	
	public String getCheckExistsSQL() {
		if (this.getSchema() == null) {
			return "select name from dbo.sysobjects where xtype='TR' and uid=USER_ID() and name='"
					+ this.getTriggerName() + "'";
		} else {
			return "SELECT 1 FROM sys.triggers t,sys.objects o,sys.schemas s WHERE t.name='"
					+ this.getTriggerName()
					+ "' AND t.parent_id=o.object_id AND o.schema_id=s.schema_id AND s.name='"
					+ this.getSchema() + "'";
		}
	}
		
		
	public String getDeleteSQL(){
		return "DROP TRIGGER "+this.getTriggerName();
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.TRIGGER, this.getTriggerName(), false);
	}

	public String getTriggerName() {
		return triggerName;
	}

	public void setTriggerName(String triggerName) throws RepositoryException {
		this.triggerName = formatName(triggerName);
		if (this.triggerName==null){
			String msg="Empty or invalid MSSQL trigger name '"+triggerName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}
	
	public String getDescription(){
		return "MS SQL trigger '"+this.getTriggerName()+"'";
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

}
