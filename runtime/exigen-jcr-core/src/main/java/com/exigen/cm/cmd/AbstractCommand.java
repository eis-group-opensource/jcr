/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.store.ContentStoreProvider;

/**
 * Commands that does not need repository
 * but only connection and store should inherit from this class
 * 
 */
public abstract class AbstractCommand implements Command {

    protected ConnectionProvider connectionProvider;
    protected ContentStoreProvider storeProvider;
    protected CommandManager commandManager;

    public CommandManager getCommandManager() {
		return commandManager;
	}

	public void setCommandManager(CommandManager commandManager) {
		this.commandManager = commandManager;
	}

	public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public ContentStoreProvider getStoreProvider() {
        return storeProvider;
    }

    public void setStoreProvider(ContentStoreProvider storeProvider) {
        this.storeProvider = storeProvider;
    }

    public boolean init() throws RepositoryException {
    	return true;
    }

    public abstract String getDisplayableName();

    
    public void forceCommandExecutio(String className){
    	if (commandManager != null){
    		commandManager.forceCommandExecutio(className);
    	}
    }
}


/*
 * $Log: AbstractCommand.java,v $
 * Revision 1.3  2009/03/24 07:54:44  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:40  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.14  2006/11/14 07:37:28  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.13  2006/11/09 13:44:51  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 */
