/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

/**
 * Condition that is always true (1=1)
 * 
 */
public class AlwaysTrueDatabaseCondition extends DatabaseCondition {

    @Override
    public StringBuffer createSQLPart(String alias, DatabaseConnection conn)
                    throws RepositoryException {
        
        return new StringBuffer("1=1");
    }

    @Override
    public int bindParameters(int pos, DatabaseConnection conn,
                    PreparedStatement st) throws RepositoryException {
        // nothing to bind 
        return 0;
    }

    @Override
    public String getLocalName(){
    	return "fake";
    }

    
}


/*
 * $Log: AlwaysTrueDatabaseCondition.java,v $
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.2  2006/10/09 11:22:56  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.1  2006/09/11 09:17:41  zahars
 * PTR#0144986 INDEXABLE_DATA table is cleared before tests
 *
 */
