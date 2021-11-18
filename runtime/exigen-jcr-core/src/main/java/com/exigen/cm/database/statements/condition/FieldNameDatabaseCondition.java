/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public class FieldNameDatabaseCondition extends DatabaseCondition{

    
    
    private String fieldName;

    public FieldNameDatabaseCondition(String fieldName) {
        this.fieldName = fieldName;
    }

    public StringBuffer createSQLPart(String alias, DatabaseConnection conn) {
        StringBuffer result = new StringBuffer(createFieldName(alias, fieldName));
        return result;
    }

    public int bindParameters(int pos, DatabaseConnection conn,
            PreparedStatement _st) throws RepositoryException {
        return 0;
    }

    @Override
    public String getLocalName(){
    	return fieldName;
    }

}


/*
 * $Log: FieldNameDatabaseCondition.java,v $
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
 * Revision 1.1  2006/04/12 12:49:00  dparhomenko
 * PTR#0144983 restructurization
 *
 */