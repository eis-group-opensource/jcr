/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import java.sql.Connection;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import com.exigen.cm.database.dialect.DatabaseDialect;

public abstract class ConnectionProvider {

	/*ConnectionProvider instance
	
	public static void getInstance(){
		
	}*/
	
    abstract public DatabaseConnection createConnection() throws RepositoryException ;

    abstract public DatabaseDialect getDialect();

    abstract public Long nextId(DatabaseConnection connection) throws RepositoryException;

    abstract public void configure(Map config, DataSource ds) throws RepositoryException;

    abstract public void setAllowCommitRollback(DatabaseConnection conn, boolean b);

    abstract public DatabaseConnection createConnection(String userName, String password) throws RepositoryException ;

    abstract public Connection createSQLConnection() throws RepositoryException;

}


/*
 * $Log: ConnectionProvider.java,v $
 * Revision 1.3  2008/05/15 06:46:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/05/31 08:54:22  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:00:52  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/09/08 11:43:35  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.1  2006/06/22 12:00:28  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/12 08:30:49  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/11 15:47:11  dparhomenko
 * PTR#0144983 optimization
 *
 */