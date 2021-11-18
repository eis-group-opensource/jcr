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
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;

public class DatabaseDeleteStatement extends AbstractDatabaseStatement {

    private ArrayList<DatabaseCondition> conditions = new ArrayList<DatabaseCondition>();

    private ArrayList<ArrayList<DatabaseCondition>> batches = new ArrayList<ArrayList<DatabaseCondition>>(); 
    
    
    public DatabaseDeleteStatement(String tableName, String pkColumnName,
            Object pkValue) {
        super(tableName);
        addCondition(Conditions.eq(pkColumnName, pkValue));

    }

    public void addBatch(){
    	batches.add(conditions);
    	conditions = new ArrayList<DatabaseCondition>();
    }
    
    public DatabaseDeleteStatement(String tableName) {
		super(tableName);
	}

	public String assemblSQL(DatabaseConnection conn) throws RepositoryException {
		if (batches.size() > 0){
    		int initSize = batches.get(0).size();
    		for(List<DatabaseCondition> pp:batches){
    			Collections.sort(pp);
    			if (pp.size() != initSize){
    				throw new RepositoryException("Batches has different parameter count");
    			}
    		}
    		conditions = batches.get(0);
    	}		

		
		if (conditions.size() == 0){
			throw new RepositoryException("Can't delete all data in the table");
		}
		
		
        // DELETE FROM TABLE WHERE id = ?
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM ");
        sb.append(getTableName(conn.getDialect()));
        sb.append(" WHERE ");
        /*sb.append(pkColumnName);
        sb.append("=?");*/
        for(Iterator<DatabaseCondition> it = conditions.iterator() ; it.hasNext() ; ){
            DatabaseCondition condition = it.next();
            sb.append(" ( ");
            sb.append(condition.createSQLPart(null, conn));
            sb.append(" ) ");
            if (it.hasNext()){
                sb.append(" AND ");
            }
        }
        return sb.toString();
    }

    public int applyParameters(PreparedStatement st, DatabaseConnection conn, int startPos)
            throws RepositoryException {
        //DatabaseTools.bindParameter(st, 1, pkValue);
        
        /*SQLParameter p = SQLParameter._create(null ,pkColumnName, pkValue, null);
        try {
            return p.apply(startPos, st, conn.getDialect());
        } catch (SQLException exc){
            throw new RepositoryException("Error setting SQl parameter", exc);
        }*/
    	
    	try {
	    	if (batches.size() == 0){
	            int pos = startPos;
	            for(DatabaseCondition condition: conditions){
	                pos += condition.bindParameters(pos, conn, st);
	            }
	            return conditions.size();
	    	} else {
	    		for(List<DatabaseCondition> pp:batches){
			        int pos = startPos;
		            for(DatabaseCondition p:pp){
		                pos += p.bindParameters(pos, conn, st);
		            }
		            st.addBatch();
	    		}
	    		return batches.get(0).size();
	    	}    	
        } catch (SQLException exc){
        	exc.printStackTrace();
            throw new RepositoryException("Error setting SQl parameter", exc);
        }
    	
        
    }
    
    @Override
	protected boolean _executeStatement(PreparedStatement st, DatabaseConnection conn) throws SQLException, RepositoryException {
		if (batches.size() == 0){
			return super._executeStatement(st, conn);
		} else {
			st.executeBatch();
			return true;
		}
    }

    protected boolean isAutoCloseStatement() {
        return true;
    }
    
    public void addCondition(DatabaseCondition condition){
        conditions.add(condition);
    }

    protected HashMap<String, String> buildTableAliasMapping(){
    	return new HashMap<String,String>();
    }

}

/*
 * $Log: DatabaseDeleteStatement.java,v $
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.9  2007/02/26 13:14:48  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.8  2007/01/24 08:46:43  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.7  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.6  2006/07/07 07:55:59  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.5  2006/07/06 09:29:29  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/06 07:54:40  dparhomenko
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
 * Revision 1.5  2006/03/17 10:12:36  dparhomenko
 * PTR#0144983 add support for indexable_data
 *
 * Revision 1.4  2006/03/03 10:33:19  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.3  2006/03/01 11:54:42  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.2  2006/02/20 15:32:24  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/17 13:03:41  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */