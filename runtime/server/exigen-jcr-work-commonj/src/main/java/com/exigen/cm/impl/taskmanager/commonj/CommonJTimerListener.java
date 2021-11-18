/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager.commonj;

import com.exigen.cm.impl.taskmanager.Task;
import commonj.timers.Timer;
import commonj.timers.TimerListener;
import commonj.work.WorkEvent;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkManager;

public class CommonJTimerListener implements TimerListener {

	private Task t;
	private boolean stop = false;
	private WorkManager workManage;
	private WorkItem wi;

	public CommonJTimerListener(Task task, WorkManager w) {
		this.t = task;
		this.workManage  = w;
	}

	public void timerExpired(Timer task) {
		if (!stop){
			//System.out.println("Executing : "+t);
			if (wi != null){
				if (wi.getStatus() == WorkEvent.WORK_COMPLETED || 
						wi.getStatus() == WorkEvent.WORK_REJECTED){
					wi = null;
				}
			}
			if (wi == null){
				try {
					wi = workManage.schedule(new CommonJWorkTask(t), new WorkListenerImpl());
				} catch (IllegalArgumentException e) {
					wi = null;
					e.printStackTrace();
				} catch (WorkException e) {
					wi = null;
					e.printStackTrace();
				}
			}
			//t.run();
		} else {
			//System.out.println("Stop :"+t);
			task.cancel();
		}
	}

	public void shutdown() {
		//System.out.println("Set stop to true for "+t);
		stop  = true;
		t.release();
		
	}
}