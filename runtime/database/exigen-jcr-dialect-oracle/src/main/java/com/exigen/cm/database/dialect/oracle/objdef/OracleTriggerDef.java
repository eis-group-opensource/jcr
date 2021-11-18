/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.vf.commons.logging.LogUtils;

public class OracleTriggerDef  extends OracleSourceObjectDef {

	private String triggerName;
	private Log log = LogFactory.getLog(OracleTriggerDef.class);
	
	public OracleTriggerDef(String name,InputStream in) throws RepositoryException{
		super(name);
		this.setTriggerName(name);
		this.setCreateStatement(in);
	}
	
	public OracleTriggerDef(String name,String sql) throws RepositoryException{
		super(name);
		this.setTriggerName(name);
		this.setCreateStatement(sql);
	}

	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;//
	}
	
	public String getCheckExistsSQL(){
		return "SELECT TRIGGER_NAME FROM USER_TRIGGERS WHERE TRIGGER_NAME='"+
			this.getTriggerName()+"'";
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
			String msg="Empty or invalid Oracle trigger name '"+triggerName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}
	
	public String getDescription(){
		return "Oracle trigger '"+this.getTriggerName()+"'";
	}

}
