/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.vf.commons.logging.LogUtils;

public class OracleTextPolicyDef extends AbstractDBObjectDef {
	
	private String policyName;
	private String filter;
	private Log log = LogFactory.getLog(OracleTextPolicyDef.class);
	
	public OracleTextPolicyDef(String name,String filter) throws RepositoryException{
		super(name);
		this.setPolicyName(name);
		this.setFilter(filter);
	}
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE;
	}
	
	public List<String> getCreateSQL() {
		ArrayList<String> result = new ArrayList<String>();
		result.add("BEGIN CTX_DDL.CREATE_POLICY('"+this.getPolicyName()+
					"','"+this.getFilter()+"'); END;");
		return result;
	}
	
	public String getDeleteSQL(){
		return "BEGIN CTX_DDL.DROP_POLICY('"+this.getPolicyName()+"'); END;";
	}

	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.ORACLE_TEXT_POLICY, this.getPolicyName(), privileged);
	}

	
	public String getPolicyName() {
		return policyName;
	}

	public void setPolicyName(String policyName) throws RepositoryException{
		this.policyName = formatName(policyName);
		if (this.policyName==null){
			String msg="Empty or invalid OracleText policy name '"+policyName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getFilter() {
		return filter;
	}
	
	public void setFilter(String filter) throws RepositoryException{
		this.filter=formatName(filter);
		if (this.filter==null){
			String msg="Empty or invalid OracleText filter name '"+filter+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getDescription(){
		return "OracleText policy '"+this.getPolicyName()+"'";
	}
	
}
