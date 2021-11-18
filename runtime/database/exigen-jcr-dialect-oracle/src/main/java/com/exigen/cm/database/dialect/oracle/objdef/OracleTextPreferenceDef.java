/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.vf.commons.logging.LogUtils;

public class OracleTextPreferenceDef extends AbstractDBObjectDef {
	private String prefName;
	private String prefObject;
	private HashMap<String,String> attrs=new HashMap<String,String>();
	private ArrayList<String> indexesWhereToUse=new ArrayList<String>();
	private Log log = LogFactory.getLog(OracleTextPreferenceDef.class);
	
	public OracleTextPreferenceDef(String name,String o) throws RepositoryException{
		super(name);
		this.setPrefName(name);
		this.setPrefObject(o);
	}
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;
	}
	
	public void addAttribute(String name,String val) throws RepositoryException{
		String n=formatName(name);
		String v=val.trim();
		if (n==null){
			String msg="Empty or inavlid Oracle text preference attribute name '"+name+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
		else
			this.attrs.put(n,v);
	}

	public String getCheckExistsSQL(){
		return "SELECT pre_name FROM ctx_user_preferences WHERE pre_name='"+
			this.getPrefName()+"'";
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
		StringBuilder sb=new StringBuilder();
		sb.append("BEGIN ");
		sb.append(" CTX_DDL.CREATE_PREFERENCE('"+this.getPrefName()+"','"+this.getPrefObject()+"');");
		Object[] keys=this.attrs.keySet().toArray();
		for (int i=0;i<keys.length;i++){
			sb.append(" CTX_DDL.SET_ATTRIBUTE('"+this.getPrefName()+"','"+(String)keys[i]+"','"+
					(String)this.attrs.get(keys[i])+"');");
		}
		sb.append(" END;");
		result.add(sb.toString());
		return result;
	}
	
	public String getDeleteSQL(){
		return "BEGIN CTX_DDL.DROP_PREFERENCE('"+this.getPrefName()+"'); END;";
	}

	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.ORACLE_TEXT_PREFERENCE, this.getPrefName(), privileged);
	}

	
	public String getPrefName() {
		return prefName;
	}

	public void setPrefName(String prefName)  throws RepositoryException{
		this.prefName = formatName(prefName);
		if (this.prefName==null){
			String msg="Empty or invalid OracleText preference name '"+prefName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getPrefObject() {
		return prefObject;
	}

	public void setPrefObject(String prefObject)  throws RepositoryException{
		this.prefObject = formatName(prefObject);
		if (this.prefObject==null){
			String msg="Empty or invalid OracleText preference object name '"+prefObject+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}	
	}

	public String getDescription(){
		return "OracleText preference '"+this.getPrefName()+"'";
	}
	
}
