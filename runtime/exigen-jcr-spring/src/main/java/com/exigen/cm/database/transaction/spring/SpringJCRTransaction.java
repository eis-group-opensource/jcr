/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.spring;

import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.exigen.cm.database.transaction.JCRTransaction;

public class SpringJCRTransaction  implements JCRTransaction{

	private TransactionStatus status;
	private SpringJCRTransactionManager manager;

	public SpringJCRTransaction(TransactionStatus status, SpringJCRTransactionManager manager) {
		this.status = status;
		this.manager =manager;
	}

	public TransactionStatus getStatus() {
		return status;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SpringJCRTransaction)){
			return false;
		}
		if (status instanceof DefaultTransactionStatus){
			SpringJCRTransaction _other = (SpringJCRTransaction) obj;
			if (!(_other.status instanceof DefaultTransactionStatus)){
				return false;
			}
			DefaultTransactionStatus s1 = (DefaultTransactionStatus) status;
			DefaultTransactionStatus s2 = (DefaultTransactionStatus) _other.status;
			JdbcTransactionObjectSupport tos1 = (JdbcTransactionObjectSupport) s1.getTransaction();
			JdbcTransactionObjectSupport tos2 = (JdbcTransactionObjectSupport) s2.getTransaction();
			//tos1.getConnectionHolder()
			//return s1.getTransaction().equals(s2.getTransaction());
			
			if (tos1 == null){
				return false;
			}
			if (tos2 == null){
				return false;
			}
            if (tos1.getConnectionHolder() == null){
                return false;
            }
            if (tos2.getConnectionHolder() == null){
                return false;
            }			
			
			return tos1.getConnectionHolder().equals(tos2.getConnectionHolder());
			
		}
		return status.equals(((SpringJCRTransaction)obj).status);
	}

    public boolean allowConnectionClose() {
        return true;
    }



}
