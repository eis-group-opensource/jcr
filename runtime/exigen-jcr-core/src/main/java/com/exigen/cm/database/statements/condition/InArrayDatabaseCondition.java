/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;


public class InArrayDatabaseCondition extends DatabaseCondition {

    //private String fieldName;
    private Collection values;
    private DatabaseCondition c1;;

    public InArrayDatabaseCondition(String fieldName, String[] values) {
        //this.fieldName = fieldName;
        this.c1 = new FieldNameDatabaseCondition(fieldName);
        this.values = Arrays.asList(values);
    }
    public InArrayDatabaseCondition(String fieldName, Collection values) {
        this.c1 = new FieldNameDatabaseCondition(fieldName);
        //this.fieldName = fieldName; 
        this.values = values;
    }

    public InArrayDatabaseCondition(DatabaseCondition c1, Collection values) {
        this.c1 = c1;
        this.values = values;
    }
    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) throws RepositoryException {
        StringBuffer result = new StringBuffer();
        result.append(c1.createSQLPart(alias, conn));
        result.append(" in (");
        for(Iterator it = values.iterator(); it.hasNext();){
            result.append("?");
            it.next();
            if (it.hasNext()){
                result.append(",");
            }
        }
        result.append(")");
        return result;
    }

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement _st) throws RepositoryException {
        int result = 0;
        result = c1.bindParameters(pos, conn, _st);
        for(Iterator it = values.iterator(); it.hasNext();){
            DatabaseTools.bindParameter(_st, conn.getDialect(), pos + result, it.next(), isPureMode());
            result++;
        }
        return result;
    }
    
    @Override
    public String getLocalName(){
    	return c1.getLocalName();
    }


}


/*
 * $Log: InArrayDatabaseCondition.java,v $
 * Revision 1.2  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.3  2006/06/30 10:34:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/05/10 09:00:39  dparhomenko
 * PTR#0144983 dynamic table names
 *
 * Revision 1.1  2006/04/17 06:46:39  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/12 12:49:00  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/03/27 14:27:25  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/03/21 13:19:25  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/03/03 10:33:16  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.1  2006/03/01 11:54:44  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.1  2006/02/13 12:40:46  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */