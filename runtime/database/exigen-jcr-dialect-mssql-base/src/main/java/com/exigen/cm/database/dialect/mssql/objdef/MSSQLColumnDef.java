/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql.objdef;

import static com.exigen.cm.Constants.DBOBJ_ACTION_CREATE;
import static com.exigen.cm.Constants.DBOBJ_ACTION_EXISTS;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.objdef.AbstractDBObjectDef;
import com.exigen.vf.commons.logging.LogUtils;


public class MSSQLColumnDef extends AbstractDBObjectDef {
	
	private String tableName;
	private String addtnlColName;
	private String addtnlColType;
	private String schema;
	private Log log = LogFactory.getLog(MSSQLColumnDef.class);
	
	public MSSQLColumnDef(String tab, String col, String type) throws RepositoryException{
		super(tab);
		this.setTableName(tab);
		this.setAddtnlColName(col);
		this.setAddtnlColType(type);
		this.setSchema("");
	}
	
	public MSSQLColumnDef(String tab, String col, String type,String schema) throws RepositoryException{
		super(tab);
		this.setTableName(tab);
		this.setAddtnlColName(col);
		this.setAddtnlColType(type);
		this.setSchema(schema);
	}
	
	@Override
	protected void register(DatabaseConnection conn, boolean privileged) throws RepositoryException{
	}

	public int getClassActions(){
		// it is possible to add DBOBJ_ACTION_DELETE and method getDeleteSQL() if required
		return DBOBJ_ACTION_CREATE+DBOBJ_ACTION_EXISTS;
	}
	
	public List<String> getCreateSQL() {
		ArrayList<String> result=new ArrayList<String>();
		result.add("ALTER TABLE  "
				+ ( this.getSchema()==null ? "" : this.getSchema()+"." )
				+ this.getTableName()
				+ " ADD "+this.getAddtnlColName()
				+" "+this.getAddtnlColType()
		);
		result.add("COMMIT");
		return result;
	}

	public String getCheckExistsSQL() {
		// TODO check column type too
		if (this.getSchema() == null) {
			return "SELECT 1 FROM dbo.syscolumns WHERE name='"
					+ this.getAddtnlColName() + "' AND id=("
					+ "SELECT id FROM dbo.sysobjects WHERE name='"
					+ this.getTableName() + "' AND uid=USER_ID() AND xtype='U'"
					+ ")";
		} else {
			return "SELECT 1 FROM sys.schemas s,sys.objects o,sys.columns c WHERE s.name='"
					+ this.getSchema()
					+ "' AND s.schema_id=o.schema_id AND o.name='"
					+ this.getTableName()
					+ "' AND o.object_id=c.object_id AND c.name='"
					+ this.getAddtnlColName() + "'";
		}
	}

	public String getAddtnlColName() {
		return addtnlColName;
	}

	public void setAddtnlColName(String addtnlColName)  throws RepositoryException {
		this.addtnlColName = formatName(addtnlColName);
		if (this.addtnlColName==null){
			String msg="Empty or invalid MSSQL table column name '"+addtnlColName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getAddtnlColType() {
		return addtnlColType;
	}

	public void setAddtnlColType(String addtnlColType) throws RepositoryException{
		this.addtnlColType = formatNameUpper(addtnlColType,"^(i?)[A-Z][A-Z0-9]*(\\([0-9\\,]+\\))?$");
		if (this.addtnlColType==null){
			String msg="Empty or invalid MSSQL table column type '"+addtnlColType+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
	}

	public String getTableName() {
		return tableName;
	}
	
	public String getSchema() {
		return schema;
	}
	
	public void setSchema(String schema) throws RepositoryException{
		this.schema = formatName(schema,30,false); // false=don't force upercase
	}
	

	public void setTableName(String tableName) throws RepositoryException{
		this.tableName = formatName(tableName);
		if (this.tableName==null){
			String msg="Empty or invalid MSSQL table name '"+tableName+"'";
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}	
	}
	
	public String getDescription(){
		return "Additional column '"+this.getAddtnlColName()+
			"' in MS SQL table '"+this.getTableName()+"'";
	}

}
