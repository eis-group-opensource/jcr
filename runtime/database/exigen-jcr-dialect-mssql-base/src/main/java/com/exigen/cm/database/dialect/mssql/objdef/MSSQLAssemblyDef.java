/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_DELETE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseObject;
import com.exigen.cm.database.dialect.mssql.MSSQLConstants;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.vf.commons.logging.LogUtils;

public class MSSQLAssemblyDef extends AbstractDBObjectDef {
	
	private InputStream resource;
	private Log log = LogFactory.getLog(MSSQLAssemblyDef.class);
	
	public MSSQLAssemblyDef(String name,InputStream resource) throws RepositoryException{
		super(name);
		this.setResource(resource);
	}
	
	public int getClassActions(){
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_DELETE+DBOBJ_ACTION_EXISTS;//
	}
	
	public String getCheckExistsSQL(){
		return "SELECT 1 FROM sys.assemblies WHERE name='" + this.getName()+ "'";
	}
	
	public String getDeleteSQL(){
		return "DROP ASSEMBLY [" + this.getName() + "] WITH NO DEPENDENTS";
	}
	
	public List<String> getCreateSQL() throws RepositoryException {
		ArrayList<String> x = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		sb.append("IF (SELECT COUNT(*) FROM sys.assemblies WHERE name='")
				.append(this.getName()).append("')<1 BEGIN CREATE ASSEMBLY [")
				.append(this.getName()).append("] FROM 0x");
		int b;
		try {
			while ((b = resource.read()) != -1) {
				sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1)
						.toUpperCase());
			}
			resource.close();
		} catch (IOException e) {
			String msg = "IO error reading resource stream (CLR assembly DLL): "
					+ e.getMessage();
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
		sb.append(" WITH PERMISSION_SET = UNSAFE ").append(
				"GRANT references,control ON ASSEMBLY::[").append(
				this.getName()).append("] TO [").append(
				MSSQLConstants.MSSQL_DB_ROLE).append("] END");
		// grant new assembly to other potential JCR installations
		// in this database - references (to use), control (to delete)
		//
		x.add(sb.toString());
		return x;
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
		conn.registerSysObject(DatabaseObject.ASSEMBLY, this.getName(), false);
	}

	public void setResource(InputStream resource){
		this.resource=resource;
	}
	
	public String getDescription(){
		return "MS SQL CLR assembly '"+this.getName()+"'";
	}

}
