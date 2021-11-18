/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager;



public class ScheduledWorkTask {

	private Task task;
	private Long delay;

	public ScheduledWorkTask(Task task, Long delay) {
		this.task = task;
		this.delay = delay;
	}

	public void release() {
		delay = (long)-1;
	}

	public void run() {
		//System.out.println("@@@@@@@@@@@@@@@@@@@@@ "+delay);
		while(delay >= 0){
			//System.out.println("Execute "+task);
			try {
				task.run();
			} catch (Throwable th){
				th.printStackTrace();
			}
			try {
				Thread.sleep(delay*1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public boolean isDaemon() {
		return true;
	}	

}
