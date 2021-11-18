/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;

public interface Command {

    public boolean init() throws RepositoryException;
    
    public boolean execute(DatabaseConnection connection) throws RepositoryException;
    
    public String getDisplayableName();
    
    
}


/*
 * $Log: Command.java,v $
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:40  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2006/09/28 12:39:48  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.9  2006/08/15 08:38:01  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.2  2006/07/18 12:51:13  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.1  2006/07/14 08:21:33  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.1  2006/07/04 09:27:19  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 */