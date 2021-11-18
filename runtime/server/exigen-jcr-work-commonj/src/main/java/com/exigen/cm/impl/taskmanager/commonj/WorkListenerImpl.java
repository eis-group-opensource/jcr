/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager.commonj;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import commonj.work.WorkEvent;
import commonj.work.WorkListener;

public class WorkListenerImpl implements WorkListener {

    /**
     * Logger instance for this class
     */
    private static final Log log = LogFactory.getLog(WorkListenerImpl.class);
	
	
	public void workAccepted(WorkEvent arg0) {
	}

	public void workCompleted(WorkEvent arg0) {
		log.debug("workCompleted "+arg0.getWorkItem());
	}

	public void workRejected(WorkEvent arg0) {
		log.error("workRejected"+arg0.getWorkItem(), arg0.getException());
	}

	public void workStarted(WorkEvent arg0) {
		log.debug("workStarted "+arg0.getWorkItem());
		//System.out.println("workStarted "+arg0.getWorkItem());
	}

}

