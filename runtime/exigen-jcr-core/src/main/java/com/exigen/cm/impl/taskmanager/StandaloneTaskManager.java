/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;

import com.exigen.cm.cmd.CommandManager;
import com.exigen.cm.impl.JCRServiceLocator;

public class StandaloneTaskManager extends TaskManager{

	private ScheduledExecutorService scheduler;
	
	private ArrayList<Task> tasks = new ArrayList<Task>();
	

	@Override
	public void execute(Task task)  throws RepositoryException{
		tasks.add(task);
		Thread t = JCRServiceLocator.getDeamonThreadFactory().newThread(task);
		t.start();
	}

	@Override
	public void schedule(Task task, Long delay) throws RepositoryException {
		tasks.add(task);
		ScheduledExecutorService _scheduler = getScheduler();
		_scheduler.scheduleWithFixedDelay(task, 0, delay, TimeUnit.SECONDS);
		
	}

	private ScheduledExecutorService getScheduler()  throws RepositoryException{
		if (scheduler == null){
			scheduler = Executors.newScheduledThreadPool(CommandManager.corePoolSize, JCRServiceLocator.getDeamonThreadFactory());
		}
		return scheduler;
	}

	@Override
	public void configure(Map<String, String> configuration) throws RepositoryException {
	}

	@Override
	public void shutdown() {
		for(Task t:tasks){
			t.release();
		}
	}

	
	
}
