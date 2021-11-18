/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction;

import javax.jcr.RepositoryException;

public class TransactionHelperNoTransactionImpl extends TransactionHelper {

	@Override
	public void commitAndResore(JCRTransaction tr) throws RepositoryException {
			}

	@Override
	public JCRTransactionManager getTransactionManager()
			throws RepositoryException {
		return null;
	}


	@Override
	public void rollbackAndResore(JCRTransaction tr) throws RepositoryException {
	}

	@Override
	public JCRTransaction startNewTransaction() throws RepositoryException {
		return null;
	}


	@Override
	public int getType() {
		return TransactionHelper.APPLICATION_SERVER_NONE;
	}

	@Override
	public boolean isTransactionActive() {
		return false;
	}

	@Override
	protected void init(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void init(int type, JCRTransactionManager trManager) {
		throw new UnsupportedOperationException();
	}

    @Override
    public TrabsactionSynchronization createTransactionSynchranization() {
        
        return new TrabsactionSynchronizationNoTransaction();
    }

}
