/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;

public class DatabaseUpdateStatement extends AbstractDatabaseStatement implements ValueChangeDatabaseStatement{

    String pkColumnName;

    Object pkValue;
    
    int count;
    
    private ArrayList<SQLParameter> values = new ArrayList<SQLParameter>(); 
    private ArrayList<ArrayList<SQLParameter>> batches = new ArrayList<ArrayList<SQLParameter>>(); 
    
    private ArrayList<DatabaseCondition> conditions = new ArrayList<DatabaseCondition>();
    private ArrayList<ArrayList<DatabaseCondition>> batchesC = new ArrayList<ArrayList<DatabaseCondition>>(); 
    
    public void addBatch(){
    	batches.add(values);
    	values = new ArrayList<SQLParameter>();
    	
    	batchesC.add(conditions);
    	conditions = new ArrayList<DatabaseCondition>();
    }    
    
    public Object getPkValue(){
    	return pkValue;
    }

    public DatabaseUpdateStatement(String tableName, String pkColumnName,
            Object pkValue) {
        this(tableName);
        this.pkColumnName = pkColumnName;
        this.pkValue = pkValue;
        addCondition(Conditions.eq(pkColumnName, pkValue));
    }
    
    public DatabaseUpdateStatement(String tableName) {
        super(tableName);
    }

    public String assemblSQL(DatabaseConnection conn) throws RepositoryException {
    	if (batches.size() > 0){
    		int initSize = batches.get(0).size();
    		for(List<SQLParameter> pp:batches){
    			Collections.sort(pp);
    			if (pp.size() != initSize){
    				throw new RepositoryException("Batches has different parameter count");
    			}
    		}
    		values = batches.get(0);
    		conditions = batchesC.get(0);
    	}    	
    	
    	
        // UPDATE TABLE WHERE id=1 SET aa=3,dd=b
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE ");
        sb.append(getTableName(conn.getDialect()));
        //sb.append(" "+Constants.DEFAULT_ROOT_ALIAS);
        sb.append(" SET ");
        for(Iterator<SQLParameter> it = values.iterator() ; it.hasNext() ;){
            SQLParameter p = it.next();
            sb.append(p.getName());
            sb.append("=");
            p.registerParameter(conn, sb);
            if (it.hasNext()){
                sb.append(",");
            }
        }
        if (conditions.size() > 0){
            sb.append(" WHERE ");
        }
        /*if (pkColumnName != null){
            sb.append(pkColumnName);
            sb.append("=?");
        } else {*/
            for(Iterator<DatabaseCondition> it = conditions.iterator() ; it.hasNext() ; ){
                DatabaseCondition condition = it.next();
                sb.append(" ( ");
                sb.append(condition.createSQLPart(null, conn));
                sb.append(" ) ");
                if (it.hasNext()){
                    sb.append(" AND ");
                }
            }
        //}
        return sb.toString();
    }

    public int applyParameters(PreparedStatement st, DatabaseConnection conn, int startPos)
            throws RepositoryException {
        // apply new values
    	try {
	    	if (batches.size() == 0){
		        int pos = startPos;
		        for(int i = 0 ; i < values.size() ; i++){
		            SQLParameter p = (SQLParameter) values.get(i);
		            try {
		                pos+=p.apply(pos, st, conn.getDialect());
		            } catch (SQLException exc){
		                throw new RepositoryException("Error setting SQl parameter", exc);
		            }
		        }
		        //bind row id
		        //DatabaseTools.bindParameter(st, values.size() + 1, pkValue);
		        /*SQLParameter p = SQLParameter._create(null ,pkColumnName, pkValue);
		        try {
		            p.apply(values.size(), st, conn.getDialect());
		        } catch (SQLException exc){
		            throw new RepositoryException("Error setting SQl parameter", exc);
		        }*/
		        for(DatabaseCondition condition: conditions){
		            pos += condition.bindParameters(pos, conn, st);
		        }
	    	} else {
	    		for (int  j = 0 ; j < batches.size() ; j++){
	    			values = batches.get(j);
	    			conditions = batchesC.get(j);
			        int pos = startPos;
			        for(int i = 0 ; i < values.size() ; i++){
			            SQLParameter p = (SQLParameter) values.get(i);
			            try {
			                pos+=p.apply(pos, st, conn.getDialect());
			            } catch (SQLException exc){
			                throw new RepositoryException("Error setting SQl parameter", exc);
			            }
			        }
			        for(DatabaseCondition condition: conditions){
			            pos += condition.bindParameters(pos, conn, st);
			        }
	    			st.addBatch();
	    		}
	    	}
        } catch (SQLException exc){
        	exc.printStackTrace();
            throw new RepositoryException("Error setting SQl parameter", exc);
        }        
        
        return values.size() + conditions.size();
    }

    protected boolean isAutoCloseStatement() {
        return true;
    }

    public void addValue(SQLParameter p) {
        values.add(p);
    }
    
    protected boolean _executeStatement(PreparedStatement st,DatabaseConnection conn) throws SQLException, RepositoryException {
    	boolean result = true;
    	if (batches.size() == 0){
    		result =  st.execute();
    	} else {
    		st.executeBatch();
			return true;
    	}
        count = st.getUpdateCount();
        if (pkColumnName != null){
            if (count > 1){
                throw new RepositoryException("Update statement updates more than 1 row "+assemblSQL(conn));
            }
            if (count == 0){
                throw new RepositoryException("Update statement doesn't update row "+assemblSQL(conn));
            }
        }
        return result;
    }
    
    public void addCondition(DatabaseCondition condition){
        conditions.add(condition);
    }
    
    public int getUpdateCount(){
        return count;
    }
    
    protected HashMap<String, String> buildTableAliasMapping(){
    	return new HashMap<String,String>();
    }

    @Override
    public boolean execute(DatabaseConnection conn) throws RepositoryException {
        if (values.size() == 0 && batches.size() == 0){
            return true;
        }
        return super.execute(conn);
    }

}

/*
 * $Log: DatabaseUpdateStatement.java,v $
 * Revision 1.3  2008/06/26 07:20:49  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/11/30 07:47:48  dparhomenko
 * Fix lock problem
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.13  2007/03/27 11:21:03  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.12  2006/09/27 12:33:00  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.11  2006/09/26 12:31:50  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.10  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.9  2006/07/21 12:38:48  zahars
 * PTR#0144986 FreeReserved command introduced
 *
 * Revision 1.8  2006/07/06 09:29:29  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.7  2006/07/06 07:54:40  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.6  2006/07/03 11:45:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.5  2006/06/30 12:40:36  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/06/30 10:34:36  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/05/10 09:00:42  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.2  2006/05/03 12:07:08  dparhomenko
 * PTR#0144983 make DatabaseStatement as interface
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.7  2006/04/05 14:30:42  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.6  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.5  2006/02/27 15:02:50  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/20 15:32:24  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/16 13:53:07  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/13 12:40:55  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:33  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */