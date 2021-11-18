/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.drop.DropSQLProvider;
import com.exigen.vf.commons.logging.LogUtils;

/**
 * implementaion of abstract <code>DropSQLObjectsBase</code> class customized
 * for Oracle
 * 
 */
public class DropOracleSQLObjects extends DropSQLProvider {
	
	private Log log = LogFactory.getLog(DropOracleSQLObjects.class);

    /**
     * @param connection -
     *            current conection to database
     */
    public DropOracleSQLObjects() {
        super();
    }

    public ArrayList<String> getMaterializedViews() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select OBJECT_NAME from user_objects");
        sb.append(" where OBJECT_TYPE = 'MATERIALIZED VIEW'");
        return getSQLObjects(sb.toString(), "OBJECT_NAME", null);
    }

    public ArrayList<String> getTables() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select TABLE_NAME from user_tables where TABLE_NAME not like '%$%'");
        return getSQLObjects(sb.toString(), "TABLE_NAME", null);
    }

    public ArrayList<String> getViews() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select VIEW_NAME from user_views");
        return getSQLObjects(sb.toString(), "VIEW_NAME", null);
    }

    public ArrayList<String> getConstraints() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select TABLE_NAME, CONSTRAINT_NAME from user_constraints");
        sb.append(" where CONSTRAINT_TYPE = 'R'");
        return getSQLObjects(sb.toString(), "TABLE_NAME", "CONSTRAINT_NAME");
    }

    public ArrayList<String> getSequences() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select SEQUENCE_NAME from user_sequences");
        return getSQLObjects(sb.toString(), "SEQUENCE_NAME", null);
    }

    public ArrayList<String> getProcedures() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("select OBJECT_NAME from user_objects WHERE object_type='PROCEDURE'");
        return getSQLObjects(sb.toString(), "OBJECT_NAME", null);
    }
    public ArrayList<String> getPackages() throws RepositoryException {
        sb = new StringBuffer();
        sb.append("SELECT object_name FROM user_objects WHERE object_type='PACKAGE'");
        return getSQLObjects(sb.toString(), "OBJECT_NAME", null);
    }
    public ArrayList<String> getFunctions() throws RepositoryException {
    	sb = new StringBuffer();
        sb.append("SELECT object_name FROM user_objects WHERE object_type='FUNCTION'");
        return getSQLObjects(sb.toString(), "OBJECT_NAME", null);
    }
    public ArrayList<String> getStoredJavaSources() throws RepositoryException {
    	sb = new StringBuffer();
        sb.append("SELECT object_name FROM user_objects WHERE object_type='JAVA SOURCE'");
        return getSQLObjects(sb.toString(), "OBJECT_NAME", null);
    }
    public void dropOracleTextSettings() throws RepositoryException {
    	
		ArrayList<String> todo=new ArrayList<String>();
		StringBuffer sb = new StringBuffer();

        sb.append("SELECT pre_name FROM ctx_user_preferences");
    	ArrayList<String> data=getSQLObjects(sb.toString(), "PRE_NAME", null);
		for (String name:data){
			String plsql="BEGIN CTX_DDL.DROP_PREFERENCE('"+ name +"'); END;";
			todo.add(plsql);
		}	
		sb = new StringBuffer();
        sb.append("SELECT sgp_name FROM ctx_user_section_groups");
    	data=getSQLObjects(sb.toString(), "SGP_NAME", null);
		for (String name : data){
			String plsql="BEGIN CTX_DDL.DROP_SECTION_GROUP('"+ name +"'); END;";
			todo.add(plsql);
		}
		try {
		    processQueries(todo);
        } catch (Exception exc){
            //do nothing
        }
		
    }
    
    public void checkSchemaIsEmpty() throws RepositoryException {
		StringBuffer sb = new StringBuffer();
        sb.append("SELECT object_name||' ('||object_type||')' AS item FROM user_objects");
    	ArrayList<String> data=getSQLObjects(sb.toString(), "ITEM", null);
		for (String name : data)
			LogUtils.error(log,"After cleaning remains in DB: "+name);
		if (data.size()>0){
			String msg="Num of objects in DB schema after cleanup: "+data.size();
			LogUtils.error(log, msg);
			throw new RepositoryException(msg);
		}
    }
      
    public void drop() throws RepositoryException {
    	data = getPackages();
    	try {
    		dropObjects("package", data);
    	} catch (Exception e) {
    		throw new RepositoryException(e);
    	}
    	
    	data.clear();
    	data = getStoredJavaSources();
    	try {
    		dropObjects("JAVA SOURCE", data);
    	} catch (Exception e) {
    		throw new RepositoryException(e);
    	}
    	
    	try{
    		dropOracleTextSettings();
    	}catch (Exception e){
    		throw new RepositoryException(e);
    	}
    	
    	super.drop();
    	checkSchemaIsEmpty();
    }
 
	@Override
	protected ArrayList<String> getIndexes() {
        return new ArrayList<String>();
	}

}
