/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.jcr.RepositoryException;

import com.exigen.cm.cmd.DeamonThreadFactory;
import com.exigen.cm.database.transaction.TransactionHelper;

public class JCRServiceLocator {
	
    public static ThreadFactory getDeamonThreadFactory() throws RepositoryException{
    	if (TransactionHelper.getInstance().isManagementEnvironment()){
    		throw new UnsupportedOperationException("ThreadFactory is not supported in management environment");
    	} else {
    		ThreadFactory tf = new DeamonThreadFactory(Executors.defaultThreadFactory());
    		return tf;
    	}
	}
	
    public static ThreadFactory getNormalThreadFactory()  throws RepositoryException{
    	if (TransactionHelper.getInstance().isManagementEnvironment()){
    		throw new UnsupportedOperationException("ThreadFactory is not supported in management environment");
    	} else {
    		ThreadFactory tf = Executors.defaultThreadFactory();
    		return tf;
    	}
	}
	
	
}
