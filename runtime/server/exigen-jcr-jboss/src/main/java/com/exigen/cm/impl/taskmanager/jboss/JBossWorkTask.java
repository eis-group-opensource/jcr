/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager.jboss;

import javax.resource.spi.work.Work;

import com.exigen.cm.impl.taskmanager.Task;
import com.exigen.cm.impl.taskmanager.WorkTask;

public class JBossWorkTask extends WorkTask implements Work {

	public JBossWorkTask(Task task) {
		super(task);
		// TODO Auto-generated constructor stub
	}

}
