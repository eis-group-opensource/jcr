/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager.commonj;

import java.util.ArrayList;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.taskmanager.Task;
import com.exigen.cm.impl.taskmanager.TaskManager;
import commonj.timers.TimerManager;
import commonj.work.WorkException;
import commonj.work.WorkItem;
import commonj.work.WorkManager;

public class CommonJTaskManager extends TaskManager{

	ArrayList<CommonJTimerListener> listeners = new ArrayList<CommonJTimerListener>();
	ArrayList<Task> tasks = new ArrayList<Task>();
	
    /**
     * Logger instance for this class
     */
    private static final Log log = LogFactory.getLog(CommonJTaskManager.class);
	
	
	TimerManager timeManager = null;
	
	@Override
	public void configure(Map<String, String> configuration) throws RepositoryException {
	}

	@Override
	public void execute(Task task) throws RepositoryException {
		WorkManager wm = getWorkManager();
		try {
			WorkItem wi = wm.schedule(new CommonJWorkTask(task), new WorkListenerImpl());
			tasks.add(task);
		} catch (WorkException e) {
			throw new RepositoryException(e);
		}
	}

	@Override
	public void schedule(Task task, Long delay) throws RepositoryException {
		//WorkManager wm = getWorkManager();
		TimerManager tm = getTimerManager();
		try {
			log.debug("Schedule "+task+" with delay "+delay);
			//wm.schedule(new WebLogicScheduledWorkTask(task, delay), new WorkListenerImpl());
			CommonJTimerListener l = new CommonJTimerListener(task, getWorkManager());
			listeners.add(l);
			tasks.add(task);
			tm.schedule(l, 0, delay*1000);
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
	
	private synchronized TimerManager getTimerManager() throws RepositoryException{
		if (timeManager == null){
			// first, try absolute path
			try {
			      InitialContext ic = new InitialContext();
			      timeManager = (TimerManager)ic.lookup
			        ("java:comp/env/tm/TimerManager");
			      return timeManager;
		    } catch (NamingException ne) {
		      log.warn( "Failed to get TimerManager component from JNDI using absolute name ["+ne.getMessage()+"], will try relative name." );
		      // ignore exception, keep trying
		    }	
			
			try {
			      InitialContext ic = new InitialContext();
			      timeManager = (TimerManager)ic.lookup
			        ("tm/TimerManager");
			      return timeManager;
		    } catch (NamingException ne) {
		      log.error( "Failed to get TimerManager component from JNDI using absolute and relative name, please check your server JNDI resource configuration.", ne );
		      throw new RepositoryException("Failed to get TimerManager component from JNDI using absolute and relative name, please check your server JNDI resource configuration.", ne);
		    }	
		}
		return timeManager;
	}
	
	private WorkManager getWorkManager() throws RepositoryException{
		//WorkManager wm1 = J2EEWorkManager.getDefault();
		//return wm1;
		String jndiName1 = "java:comp/env/wm/WorkManager";
		try {
			InitialContext ctx = new InitialContext();
			WorkManager mgr =  (WorkManager)ctx.lookup(jndiName1);
			return mgr;
		} catch (NamingException e) {
			log.warn("Failed to get WorkManager from JNDI under name "+jndiName1+": ["+e.getMessage()+"], will try relative name.");
			// suppress exception, keep trying
		}
		
		String jndiName = "wm/WorkManager";
		try {
			InitialContext ctx = new InitialContext();
			WorkManager mgr =  (WorkManager)ctx.lookup(jndiName);
			return mgr;
		} catch (NamingException e) {
			log.error("Error getting WorkManager from JNDI under name "+jndiName,e);
			throw new RepositoryException("Error getting WorkManager from JNDI under absolute["+jndiName1+"] and relative["+jndiName+"] name, please check your server JNDI resource configuration.",e);
		}
		
	}

	@Override
	public void shutdown() {
		for(CommonJTimerListener l:listeners){
			l.shutdown();
		}
		for(Task t:tasks){
			t.release();
		}
		
	}	

	
}
