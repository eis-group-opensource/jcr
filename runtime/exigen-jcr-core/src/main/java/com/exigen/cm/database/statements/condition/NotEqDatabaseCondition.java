/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;

public class NotEqDatabaseCondition extends DatabaseCondition {

    private Object value;
    private DatabaseCondition condition;

    public NotEqDatabaseCondition(DatabaseCondition condition, Object value) {
        this.value = value;
        this.condition = condition;
    }

    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) throws RepositoryException {
        StringBuffer result = new StringBuffer();
        result.append(condition.createSQLPart(alias,conn));
        result.append(" <> ?");
        return result;
    }

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement st) throws RepositoryException {
        int shift = condition.bindParameters(pos, conn, st);
        DatabaseTools.bindParameter(st, conn.getDialect(), pos+shift, value, isPureMode());
        return 1 + shift;
    }
    
    @Override
    public String getLocalName(){
    	return condition.getLocalName();
    }


}


/*
 * $Log: NotEqDatabaseCondition.java,v $
 * Revision 1.2  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/07/04 09:27:24  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
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
 * Revision 1.1  2006/03/27 07:22:19  dparhomenko
 * PTR#0144983 optimization
 *
 */