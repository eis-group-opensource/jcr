/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager.commonj;

import com.exigen.cm.impl.taskmanager.Task;
import com.exigen.cm.impl.taskmanager.WorkTask;
import commonj.work.Work;

public class CommonJWorkTask extends WorkTask  implements Work {

	public CommonJWorkTask(Task task) {
		super(task);
	}
}