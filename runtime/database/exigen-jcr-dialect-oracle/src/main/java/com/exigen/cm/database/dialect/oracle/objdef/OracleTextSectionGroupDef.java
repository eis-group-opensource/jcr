/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.vf.commons.logging.LogUtils;

public class OracleTextSectionGroupDef extends AbstractDBObjectDef {
	
	private String sgpType;
	private String sgpName;
	private ArrayList<String> indexesWhereToUse=new ArrayList<String>();
	private Log log = LogFactory.getLog(OracleTextSectionGroupDef.class);
	
	public OracleTextSectionGroupDef(String name,String type) throws RepositoryException{
		super(name);
		this.setSgpName(name);
		this.setSgpType(type);
	}
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;
	}

	
	public String getCheckExistsSQL(){
		return "SELECT sgp_name FROM ctx_user_section_groups WHERE sgp_name='"+
			this.getSgpName()+"'";
	}

	public void registerIndexWhereToUse(String idx) throws RepositoryException{
		String i=formatName(idx);
		if (i==null){
			String msg="Empty or invalid index name '"+idx+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
		else
			this.indexesWhereToUse.add(i);
	}
	
	public boolean isApplicableForIndex(String idx) throws RepositoryException{
		String i=formatName(idx);
		if (i==null){
			String msg="Empty or invalid index name '"+idx+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
		for (String s:this.indexesWhereToUse)
			if (s.compareTo(i)==0)
				return true;
		return false;
	}
	
	public List<String> getCreateSQL() {
		ArrayList<String> result = new ArrayList<String>();
		result.add("BEGIN CTX_DDL.CREATE_SECTION_GROUP('"+
				this.getSgpName()+"','"+this.getSgpType()+"'); END;");
		return result;
	}
	
	public String getDeleteSQL(){
		return "BEGIN CTX_DDL.DROP_SECTION_GROUP('"+this.getSgpName()+"'); END;";
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.ORACLE_TEXTSECTION_GROUP, this.getSgpName(), privileged);
	}


	public String getSgpName() {
		return sgpName;
	}

	public void setSgpName(String sgpName)  throws RepositoryException{
		this.sgpName = formatName(sgpName);
		if (this.sgpName==null){
			String msg="Empty or invalid OracleText section group name '"+sgpName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getSgpType() {
		return sgpType;
	}

	public void setSgpType(String sgpType)  throws RepositoryException{
		this.sgpType = formatName(sgpType);
		if (this.sgpType==null){
			String msg="Empty or invalid OracleText section group type name '"+sgpType+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getDescription(){
		return "OracleText section group '"+this.getSgpName()+"'";
	}

}
