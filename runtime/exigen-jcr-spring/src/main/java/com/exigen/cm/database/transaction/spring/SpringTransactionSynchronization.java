/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.spring;

import org.springframework.transaction.support.TransactionSynchronization;

import com.exigen.cm.database.transaction.AbstractTransactionSynchronization;

public class SpringTransactionSynchronization extends AbstractTransactionSynchronization
	implements TransactionSynchronization{

	private SpringJCRTransactionManager transactionManager;

	public SpringTransactionSynchronization(
			SpringJCRTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	public void commit() {
    }

	public void afterCommit() {
	}

	public void beforeCommit(boolean readOnly) {
	}


	public void resume() {
	}

	public void suspend() {
	}

   public void afterCompletion(int arg0) {
        clearLocks();
    }

    public void beforeCompletion() {
    }

}
