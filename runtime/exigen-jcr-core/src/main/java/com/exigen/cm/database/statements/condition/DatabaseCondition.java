/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.statements.condition;

import java.sql.PreparedStatement;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public abstract class DatabaseCondition implements Comparable<DatabaseCondition>{

	private boolean pureMode = false;
	
    public boolean isPureMode() {
		return pureMode;
	}

	public void setPureMode(boolean pureMode) {
		this.pureMode = pureMode;
	}

	abstract public StringBuffer createSQLPart(String alias,
            DatabaseConnection conn) throws RepositoryException;

    abstract public int bindParameters(int pos, DatabaseConnection conn,
            PreparedStatement st) throws RepositoryException;

    protected String createFieldName(String alias, String fieldName) {
        if (fieldName.indexOf(".") < 0 && alias != null) {
            return alias + "." + fieldName;
        }
        return fieldName;
    }
    
	public int compareTo(DatabaseCondition o) {
		return getLocalName().compareTo(getLocalName());
	}
	
	protected abstract String getLocalName();
}

/*
 * $Log: DatabaseCondition.java,v $
 * Revision 1.2  2009/01/26 10:53:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:51  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/01/24 08:46:26  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/06/30 12:40:39  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/06/30 10:34:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 * Revision 1.2 2006/05/10 09:00:39 dparhomenko
 * PTR#0144983 dynamic table names
 * 
 * Revision 1.1 2006/04/17 06:46:39 dparhomenko PTR#0144983 restructurization
 * 
 * Revision 1.1 2006/02/13 12:40:46 dparhomenko PTR#0143252 start jdbc
 * implementation
 * 
 */