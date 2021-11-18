/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.taskmanager;

import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.transaction.TransactionHelper;

public abstract class TaskManager {

	public static final String jbossClassName = "com.exigen.cm.impl.taskmanager.jboss.JBossTaskManager";

	public static final String weblogicClassName = "com.exigen.cm.impl.taskmanager.weblogic.WebLogicTaskManager";

	/**
	 * Websphere Task Manager's class name
	 */
	public static final String websphereClassName = "com.exigen.cm.impl.taskmanager.commonj.CommonJTaskManager";
	
	/**
	 * Configures task manager
	 * @param configuration repository configuration
	 * @throws RepositoryException
	 */
	public abstract void configure(Map<String, String> configuration) throws RepositoryException ;

	/**
	 * Executes task imeddiatly
	 * @param task
	 * @throws RepositoryException
	 */
	public abstract void execute(Task task) throws RepositoryException ;
	
	/**
	 * Schedules task for execution
	 * @param task task to execute
	 * @param delay delay in seconds
	 * @throws RepositoryException
	 */
	public abstract void schedule(Task task, Long delay) throws RepositoryException ;

	/**
	 * Gets task manager instance
	 * @param configuration
	 * @return
	 * @throws RepositoryException
	 */
	public static TaskManager getInstance(Map<String, String> configuration) throws RepositoryException{
		TaskManager result = null;
		if (TransactionHelper.getInstance().isManagementEnvironment()){
			if (TransactionHelper.getApplicationServerType() == TransactionHelper.APPLICATION_SERVER_JBOSS){
				result = getInstance(jbossClassName);
			} else if (TransactionHelper.getApplicationServerType() == TransactionHelper.APPLICATION_SERVER_WEBLOGIC){
				result = getInstance(weblogicClassName);
			} else if (TransactionHelper.getApplicationServerType() == TransactionHelper.APPLICATION_SERVER_WEBSPHERE){
				result = getInstance(websphereClassName);
			} else if (TransactionHelper.getApplicationServerType() == TransactionHelper.APPLICATION_SERVER_LOCAL_CONTAINER) {
				result = new StandaloneTaskManager();
			} else {
				throw new RepositoryException("Unsupported server");
			}
		} else {
			result = new StandaloneTaskManager();
		}
		result.configure(configuration);
		return result;
		
	}

	private static TaskManager getInstance(String className) throws RepositoryException{
		try {
			return ((TaskManager) Class.forName(className).newInstance());
		} catch (InstantiationException e) {
			throw new RepositoryException(e);
		} catch (IllegalAccessException e) {
			throw new RepositoryException(e);
		} catch (ClassNotFoundException e) {
			throw new RepositoryException(e);
		}
	}

	abstract public void shutdown() ;

}
