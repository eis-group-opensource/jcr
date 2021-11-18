/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.spring;

import javax.jcr.RepositoryException;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.JCRTransactionManager;
import com.exigen.cm.database.transaction.TrabsactionSynchronization;

public class SpringJCRTransactionManager implements JCRTransactionManager{

	private PlatformTransactionManager manager;

	public SpringJCRTransactionManager(PlatformTransactionManager manager){
		this.manager = manager;
	}
	
	public JCRTransaction begin() throws RepositoryException {
		return null;
	}

	public void commit(JCRTransaction tr) throws RepositoryException {
		//TransactionStatus status = manager.getTransaction(new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS));
		//manager.commit(((SpringJCRTransaction)tr).getStatus());
	}

	public JCRTransaction getTransaction() throws RepositoryException {
		 return null;
	}

	public void rollback(JCRTransaction tr) throws RepositoryException {
	}

	public void commitAndResore(JCRTransaction tr) throws RepositoryException {
	}

	public void rollbackAndResore(JCRTransaction tr) throws RepositoryException {
	}

	public JCRTransaction startNewTransaction() throws RepositoryException {
		return null;
	}

	public TrabsactionSynchronization createTransactionSynchranization()
	    throws RepositoryException {
		return null;
	}
	
	private void registerSyncronization(SpringTransactionSynchronization result) {
		
	}
}
