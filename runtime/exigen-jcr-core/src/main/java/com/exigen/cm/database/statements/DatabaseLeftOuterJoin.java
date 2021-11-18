/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.condition.DatabaseCondition;

public class DatabaseLeftOuterJoin extends DatabaseJoin{

    private String fkTableAlias;
    private List<DatabaseCondition> conditions = new  ArrayList<DatabaseCondition>();

    public DatabaseLeftOuterJoin(String table, String table_alias, String idFiled, String fkField, boolean addToResult, String fkTableAlias) {
        super(table, table_alias, idFiled, fkField, addToResult);
        this.fkTableAlias = fkTableAlias;
    }

    public void generateSQLFromFragment(StringBuffer sb, DatabaseConnection conn) throws RepositoryException {
        sb.append(" LEFT OUTER JOIN  ");
        sb.append(conn.getDialect().convertTableName(_table));
        sb.append(" ");
        if (getTableAlias() != null && getTableAlias().length() > 0){
            sb.append(getTableAlias());
        }        
        sb.append(" ON (");
        if (getTableAlias() != null && getTableAlias().length() > 0){
            sb.append(getTableAlias());
            sb.append(".");
        }        
        sb.append(fkField);
        sb.append(" = ");
        if (fkTableAlias != null && fkTableAlias.length() > 0){
            sb.append(fkTableAlias);
            sb.append(".");
        }        
        sb.append(idFiled);
        for(DatabaseCondition cond:conditions){
            sb.append(" AND ( ");
            sb.append(cond.createSQLPart(getTableAlias(), conn));
            
            sb.append(") ");
        }
        sb.append(" )");
        
    }

    public void addCondition(DatabaseCondition condition) {;
        conditions.add(condition);
        
    }

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement st) throws RepositoryException{
        int total = 0;
        for(DatabaseCondition cond:conditions){
            total+=cond.bindParameters(pos+total, conn, st);
        }
        return total;
    }

}


/*
 * $Log: DatabaseLeftOuterJoin.java,v $
 * Revision 1.4  2008/07/17 06:57:46  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/07/16 08:45:05  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/08/11 09:04:43  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.2  2006/05/11 07:00:21  dparhomenko
 * PTR#0144983 add FTS test
 *
 * Revision 1.1  2006/04/17 06:47:12  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 07:22:21  dparhomenko
 * PTR#0144983 optimization
 *
 */