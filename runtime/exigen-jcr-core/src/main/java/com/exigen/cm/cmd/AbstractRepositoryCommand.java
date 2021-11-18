/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd;


import com.exigen.cm.impl.RepositoryImpl;

public abstract class AbstractRepositoryCommand extends AbstractCommand{

    protected RepositoryImpl repository;

    public RepositoryImpl getRepository() {
        return repository;
    }

    public void setRepository(RepositoryImpl repository) {
        this.repository = repository;
    }

}


/*
 * $Log: AbstractRepositoryCommand.java,v $
 * Revision 1.1  2007/04/26 08:59:40  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/11/09 13:44:51  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.12  2006/08/15 08:38:01  dparhomenko
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