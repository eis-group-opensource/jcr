/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction;

import javax.jcr.RepositoryException;

public interface JCRTransactionManager {


	public JCRTransaction getTransaction() throws RepositoryException;

	public JCRTransaction begin() throws RepositoryException;

	public void rollback(JCRTransaction tr)  throws RepositoryException;

	public void commit(JCRTransaction tr)  throws RepositoryException;

	public void commitAndResore(JCRTransaction tr) throws RepositoryException;

	public void rollbackAndResore(JCRTransaction tr) throws RepositoryException;

	public JCRTransaction startNewTransaction() throws RepositoryException;

    public TrabsactionSynchronization createTransactionSynchranization() throws RepositoryException;

}
