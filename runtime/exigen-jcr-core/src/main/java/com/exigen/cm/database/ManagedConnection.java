/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database;

import javax.jcr.RepositoryException;

public class ManagedConnection extends DatabaseConnection {

	public ManagedConnection(ConnectionProvider connectionProvider)	throws RepositoryException {
		super(connectionProvider);
	}

	@Override
	public boolean allowCommitRollback() throws RepositoryException {
		return false;
	}
	
	@Override
	public void commit() throws RepositoryException {
		
	}
	
	@Override
	public void rollback() throws RepositoryException {
	
	}
	
	@Override
	public void close() throws RepositoryException {
		closeStatements();
	}
	
	@Override
	public boolean isLive() {
		return true;
	}
	
	
	
}
