/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.transaction.jta;

import javax.transaction.Synchronization;

import com.exigen.cm.database.transaction.AbstractTransactionSynchronization;

public class JTATransactionSynchronization extends AbstractTransactionSynchronization implements Synchronization{

    public void commit() {
        //do nothing
        
    }

    public void afterCompletion(int arg0) {
        clearLocks();
        
    }

    public void beforeCompletion() {
        // TODO Auto-generated method stub
        
    }

}
