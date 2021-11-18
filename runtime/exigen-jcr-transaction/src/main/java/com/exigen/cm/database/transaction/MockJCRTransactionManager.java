/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction;

import javax.jcr.RepositoryException;

public class MockJCRTransactionManager implements JCRTransactionManager{

	public JCRTransaction begin() throws RepositoryException {
		return null;
	}

	public void commit(JCRTransaction tr) throws RepositoryException {
	}

	public void commitAndResore(JCRTransaction tr) throws RepositoryException {
	}

	public JCRTransaction getTransaction() throws RepositoryException {
		return null;
	}

	public void rollback(JCRTransaction tr) throws RepositoryException {
	}

	public void rollbackAndResore(JCRTransaction tr) throws RepositoryException {
	}

	public JCRTransaction startNewTransaction() throws RepositoryException {
		return new MockJCRTransaction();
	}

    public TrabsactionSynchronization createTransactionSynchranization()
            throws RepositoryException {
        throw new UnsupportedOperationException();
    }

}

class MockJCRTransaction implements JCRTransaction {

    public boolean allowConnectionClose() {
        return true;
    }
	
}
