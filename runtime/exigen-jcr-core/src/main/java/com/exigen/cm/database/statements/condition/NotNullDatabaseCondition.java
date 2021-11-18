/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;


public class NotNullDatabaseCondition extends DatabaseCondition {

    private String fieldName;

    public NotNullDatabaseCondition(String fieldName) {
        this.fieldName = fieldName;
    }

    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) {
        StringBuffer result = new StringBuffer();
        result.append(createFieldName(alias, fieldName));
        result.append(" is not null");
        return result;
    }

    public int bindParameters(int pos, DatabaseConnection conn, PreparedStatement st) throws RepositoryException {
        return 0;
    }
    
    @Override
    public String getLocalName(){
    	return fieldName;
    }
    

}


/*
 * $Log: NotNullDatabaseCondition.java,v $
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
 * Revision 1.1  2006/03/01 11:54:44  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.1  2006/02/13 12:40:46  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */