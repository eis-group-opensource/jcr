/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.jta;

import javax.transaction.Transaction;

import com.exigen.cm.database.transaction.JCRTransaction;

public class JTAJCRTRansaction implements JCRTransaction {

	private Transaction transaction;
	private JTAJCRTransactionManager manager;

	public JTAJCRTRansaction(Transaction transaction, JTAJCRTransactionManager manager) {
		this.transaction = transaction;
		this.manager = manager;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof JTAJCRTRansaction)){
			return false;
		}
        if (transaction == null && ((JTAJCRTRansaction)obj).transaction == null){
            return true;
        }
        if (transaction == null && ((JTAJCRTRansaction)obj).transaction != null){
            return false;
        }
		return transaction.equals( ((JTAJCRTRansaction)obj).transaction  );
	}

    public boolean allowConnectionClose() {
        return true;
    }



}
