/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;

public class StoredProcedureDatabaseCondition extends DatabaseCondition {

    private String procedureName;
    private ArrayList<StoredProcedureArgument> params = new ArrayList<StoredProcedureArgument>();

    public StoredProcedureDatabaseCondition(String procedureName) {
        this.procedureName = procedureName;
    }

    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) {
        StringBuffer result = new StringBuffer(procedureName);
        result.append("(");
        boolean first = true;
        for(StoredProcedureArgument arg:params){
            if (!first){
                result.append(",");
            }
            first = false;
            if (arg instanceof StoredProcedureParameter){
                result.append("?");
            } else {
                result.append(arg.getValue());
            }
        }
        result.append(")");
        
        return result;
    }

    public int bindParameters(int pos, DatabaseConnection conn,
            PreparedStatement _st) throws RepositoryException {
        int shift = 0;
        for(StoredProcedureArgument arg:params){
            if (arg instanceof StoredProcedureParameter){
                DatabaseTools.bindParameter(_st, conn.getDialect(), pos+shift, arg.getValue(), isPureMode());
                shift++;
            } 
        }
        return shift;
    }
    
    public void addParameter(Object value){
        params.add(new StoredProcedureParameter(value));
    }

    public void addVariable(Object value){
        params.add(new StoredProcedureArgument(value));
    }
    
    static class StoredProcedureArgument{
        public Object value;
        
        public StoredProcedureArgument(Object value){
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
        
    }
    
    static class StoredProcedureParameter extends StoredProcedureArgument{

        public StoredProcedureParameter(Object value) {
            super(value);
        }
        
    }
    
    @Override
    public String getLocalName(){
    	throw new UnsupportedOperationException();
    }


}

/*
 * $Log: StoredProcedureDatabaseCondition.java,v $
 * Revision 1.2  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.2  2006/06/30 10:34:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/04/17 06:46:39  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/31 13:41:20  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.2  2006/03/22 11:18:53  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/03/16 13:13:06  dparhomenko
 * PTR#0144983 versioning support
 * Revision 1.2 2006/03/03 10:33:16
 * dparhomenko PTR#0144983 versioning support
 * 
 * Revision 1.1 2006/03/01 11:54:44 dparhomenko PTR#0144983 support locking
 * 
 * Revision 1.1 2006/02/13 12:40:46 dparhomenko PTR#0143252 start jdbc
 * implementation
 * 
 */