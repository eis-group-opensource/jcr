/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;


public class InDatabaseCondition extends DatabaseCondition {

    private String fieldName;
    private DatabaseSelectAllStatement st;
    private boolean notMode;

    public InDatabaseCondition(String fieldName, DatabaseSelectAllStatement st, boolean notMode) {
        this.fieldName = fieldName;
        this.st = st;
        this.notMode = notMode;
    }

    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) throws RepositoryException {
        StringBuffer result = new StringBuffer();
        result.append(createFieldName(alias, fieldName));
        if (notMode){
            result.append(" not ");
        }
        result.append(" in (");
        result.append(st.assemblSQL(conn));
        result.append(")");
        return result;
    }

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement _st) throws RepositoryException {
        return st.applyParameters(_st, conn, pos);
    }

    @Override
    public String getLocalName(){
    	return fieldName;
    }

}


/*
 * $Log: InDatabaseCondition.java,v $
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