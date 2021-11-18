/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager.jboss;

import java.util.ArrayList;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.resource.work.JBossWorkManagerMBean;

import com.exigen.cm.impl.taskmanager.Task;
import com.exigen.cm.impl.taskmanager.TaskManager;

public class JBossTaskManager extends TaskManager{

	private ArrayList<Task> tasks = new ArrayList<Task>();
	private ArrayList<JBossScheduledWorkTask> timers = new ArrayList<JBossScheduledWorkTask>();
	
	@Override
	public void configure(Map<String, String> configuration) throws RepositoryException {
	}

	@Override
	public void execute(Task task) throws RepositoryException {
		WorkManager wm = getWorkManager();
		try {
			wm.startWork(new JBossWorkTask(task));
			tasks.add(task);
		} catch (WorkException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void schedule(Task task, Long delay) throws RepositoryException {
		WorkManager wm = getWorkManager();
		try {
			JBossScheduledWorkTask t = new JBossScheduledWorkTask(task, delay);
			wm.startWork(t);
			timers.add(t);
			tasks.add(task);
			
		} catch (WorkException e) {
			throw new RepositoryException(e);
		}
	}
	
	public WorkManager getWorkManager() throws RepositoryException{
		MBeanServer server = MBeanServerLocator.locateJBoss();
		ObjectName objectName;
		try {
			objectName = new ObjectName("jboss.jca:service=WorkManager");
		} catch (MalformedObjectNameException e) {
			throw new RepositoryException(e);
		} catch (NullPointerException e) {
			throw new RepositoryException(e);
		}
		JBossWorkManagerMBean mbean =
		      (JBossWorkManagerMBean)MBeanServerInvocationHandler.newProxyInstance(
		                                                   server,
		                                                   objectName,
		                                                   JBossWorkManagerMBean.class,
		                                                   false);
		WorkManager mngr = mbean.getInstance();
		return mngr;
	}	
	
	
	@Override
	public void shutdown() {
		for(JBossScheduledWorkTask t:timers){
			t.release();
		}
		for(Task t:tasks){
			t.release();
		}
		
	}

}
