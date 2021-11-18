/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.hsql;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.drop.DropSQLProvider;

/**
 * TODO Put class description here
 * 
 */
public class DropHSQLObjects extends DropSQLProvider {

    /**
     * 
     */
    public DropHSQLObjects() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see com.exigen.cm.database.drop.DropSQLProvider#getMaterializedViews()
     */
    public ArrayList<String> getMaterializedViews() throws RepositoryException {
        return new ArrayList<String>();
    }

    /**
     * @see com.exigen.cm.database.drop.DropSQLProvider#getConstraints()
     */
    public ArrayList<String> getConstraints() throws RepositoryException {
        
        return getSQLObjects("select CONSTRAINT_NAME, TABLE_NAME from INFORMATION_SCHEMA.SYSTEM_TABLE_CONSTRAINTS where CONSTRAINT_TYPE='FOREIGN KEY'", "TABLE_NAME", "CONSTRAINT_NAME");
    }

    /**
     * @see com.exigen.cm.database.drop.DropSQLProvider#getTables()
     */
    public ArrayList<String> getTables() throws RepositoryException {
        return getSQLObjects("select TABLE_NAME from INFORMATION_SCHEMA.SYSTEM_TABLES where TABLE_SCHEM = 'PUBLIC'", "TABLE_NAME", null);
        
    }

    /**
     * @see com.exigen.cm.database.drop.DropSQLProvider#getViews()
     */
    public ArrayList<String> getViews() throws RepositoryException {
        return new ArrayList<String>();
    }

    /**
     * @see com.exigen.cm.database.drop.DropSQLProvider#getSequences()
     */
    public ArrayList<String> getSequences() throws RepositoryException {
        return new ArrayList<String>();
    }

    /**
     * @see com.exigen.cm.database.drop.DropSQLProvider#getProcedures()
     */
    public ArrayList<String> getProcedures() throws RepositoryException {
        return new ArrayList<String>();
    }

    public ArrayList<String> getFunctions() throws RepositoryException {
        return new ArrayList<String>();
    }

	@Override
	protected ArrayList<String> getIndexes() {
        return new ArrayList<String>();
	}
}


/*
 * $Log: DropHSQLObjects.java,v $
 * Revision 1.1  2007/04/26 09:00:20  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/10/05 14:13:24  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.2  2006/06/27 11:51:11  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 * Revision 1.1  2006/04/17 06:47:01  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/10 12:51:15  maksims
 * #0144985 dialect specific classes moved to dialect.vendor package
 *
 * Revision 1.2  2006/03/31 13:41:24  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/09 11:01:47  ivgirts
 * PTR #1801251 added support for Hypersonic SQL DB
 *
 */
