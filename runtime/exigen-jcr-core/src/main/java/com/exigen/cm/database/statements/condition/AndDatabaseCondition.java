/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public class AndDatabaseCondition extends DatabaseCondition {

    //private DatabaseCondition c1;

    //private DatabaseCondition c2;
    
    ArrayList<DatabaseCondition> conditions = new ArrayList<DatabaseCondition>();
    

    public AndDatabaseCondition(DatabaseCondition c1, DatabaseCondition c2) {
        //this.c1 = c1;
        //this.c2 = c2;
    	conditions.add(c1);
    	conditions.add(c2);
    }

    public AndDatabaseCondition(List<DatabaseCondition> cc) {
        conditions.addAll(cc);
    }
    
    public AndDatabaseCondition(DatabaseCondition[] cc) {
        conditions.addAll(Arrays.asList(cc));
    }
    
    public AndDatabaseCondition() {
    }

    
    public void add(DatabaseCondition c){
    	conditions.add(c);
    }
    
    
    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) throws RepositoryException {
        /*StringBuffer result = new StringBuffer();
        result.append(" ( ");
        result.append(c1.createSQLPart(alias, conn));
        result.append(" and ");
        result.append(c2.createSQLPart(alias, conn));
        result.append(" ) ");
        return result;*/
    	
        StringBuffer result = new StringBuffer();
        if (conditions.size() > 0){
            result.append(" ( ");
            for(Iterator it = conditions.iterator() ; it.hasNext();){
                DatabaseCondition cond = (DatabaseCondition) it.next();
                result.append(cond.createSQLPart(alias, conn));
                if (it.hasNext()){
                    result.append(" and ");
                }
            }
            /*result.append(c1.createSQLPart(conn));
            result.append(" or ");
            result.append(c2.createSQLPart(conn));*/
            result.append(" ) ");
        }
        return result;
    	
    }

    public int bindParameters(int pos, DatabaseConnection conn,
            PreparedStatement _st) throws RepositoryException {
        /*int shift1 = c1.bindParameters(pos, conn, _st);
        int shift2 = c2.bindParameters(pos + shift1, conn, _st);
        return shift1 + shift2;*/
        int shift = 0;
        for(Iterator it = conditions.iterator() ; it.hasNext();){
            DatabaseCondition cond = (DatabaseCondition) it.next();
            shift+= cond.bindParameters(pos+shift, conn, _st);
        }
        return shift;
    	
    }
    
    @Override
    public String getLocalName(){
    	//return c1.getLocalName();
    	throw new UnsupportedOperationException();
    }
    

}

/*
 * $Log: AndDatabaseCondition.java,v $
 * Revision 1.2  2008/04/30 09:28:50  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/03/27 11:20:55  dparhomenko
 * PTR#1803806 implement jsr283
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
 * Revision 1.1  2006/03/21 13:19:25  dparhomenko
 * PTR#0144983 versioning support
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