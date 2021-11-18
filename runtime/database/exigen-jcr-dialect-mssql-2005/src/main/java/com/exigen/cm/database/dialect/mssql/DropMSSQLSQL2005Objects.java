/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.mssql;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

public class DropMSSQLSQL2005Objects extends DropMSSQLSQLObjects {

	public DropMSSQLSQL2005Objects(String user) {
		super(user);
	}

	protected String getUserName() throws RepositoryException {
		
		return this.connection.getUserName();
	}

	
	
	@Override
	protected void dropCustomEnd() throws Exception {
		data.clear();

		data.add(userName + "_jcr");
		try {
			//dropObjects("schema", data);
			
			sqlQueries.clear();
			
			sqlQueries.add("drop schema "+this.connection.getUserName() + "_jcr");
			processQueries(sqlQueries);
			
		} catch (Exception exc) {
			log.debug(exc.getMessage());
		}

	}

	private ArrayList<String> getObjects(String objectName)
			throws RepositoryException {
		// select * from sys.objects o , sys.schemas s where type_desc =
		// 'USER_TABLE'
		// and s.schema_id = o.schema_id and s.name='ipbpoc_jcr'

		sb = new StringBuffer();
		sb.append("select o.name from sys.objects o , sys.schemas s where type = '");
		sb.append(objectName);
		sb.append("' and s.schema_id = o.schema_id and s.name='");
		sb.append(getUserName() + "_jcr");
		sb.append("'");

		ArrayList<String> result = getSQLObjects(sb.toString(), "name", null);
/*		ArrayList<String> result2 = new ArrayList<String>();
		for(String s :result){
			result2.add(userName + "_jcr."+s);
		}
		return result2;*/
		return result;
	}

	@Override
	public ArrayList<String> getTables() throws RepositoryException {
		return getObjects("U");
	}

	@Override
	public ArrayList<String> getConstraints() throws RepositoryException {
		sb = new StringBuffer();
		sb.append("select o1.name as pname,o.* from sys.all_objects o , sys.all_objects o1, sys.schemas s where o.type= 'F' and ");
		sb.append("s.schema_id = o.schema_id and o1.object_id = o.parent_object_id ");
		sb.append(" and s.name='");
		sb.append(getUserName() + "_jcr");
		sb.append("'");

		ArrayList<String> result = getSQLObjects(sb.toString(), "pname", "name");
		/*ArrayList<String> result2 = new ArrayList<String>();
		for(Iterator<String> it = result.iterator(); it.hasNext() ; ){
			result2.add(userName + "_jcr."+it.next());
			result2.add(it.next());
		}
		return result2;*/
		return result;

	}

	@Override
	public ArrayList<String> getFunctions() throws RepositoryException {
		return getObjects("FN");
	}

	@Override
	public ArrayList<String> getProcedures() throws RepositoryException {
		ArrayList<String> result = new ArrayList<String>(getObjects("P"));
		result.addAll(getObjects("X"));
		return result;

	}

	@Override
	public ArrayList<String> getViews() throws RepositoryException {
		return getObjects("V");
	}

}
