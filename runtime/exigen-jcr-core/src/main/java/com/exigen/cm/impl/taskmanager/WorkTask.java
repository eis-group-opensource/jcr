/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager;

public class WorkTask {

	private Task task;

	public WorkTask(Task task){
		this.task = task;
	}
	
	public void release() {
		task.release();
	}

	public void run() {
		task.run();
	}

	public boolean isDaemon() {
		return false;
	}

	@Override
	public String toString() {
		return task.toString();
	}
	
}
