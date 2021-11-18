/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd;

import java.util.ArrayList;
import java.util.HashMap;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.taskmanager.TaskManager;
import com.exigen.cm.store.ContentStoreProvider;

public class CommandManager {

    private ArrayList<Command> commands = new ArrayList<Command>();

    private HashMap<Command, Long> commandDelay = new HashMap<Command, Long>();

    private boolean started = false;

    //public ScheduledExecutorService scheduler;
    private TaskManager taskManager;

    private ConnectionProvider connectionProvider;

    private ContentStoreProvider storeProvider;

    private RepositoryImpl repository;
    
    private int FTSBatchSize = 10;
    
    public static int corePoolSize = 5;

    private static final Log log = LogFactory.getLog(CommandManager.class);

    public void registerCommand(String className, Long delay)
            throws RepositoryException {
        try {
            registerCommand(Class.forName(className), delay);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException("Command class " + className
                    + " not found");
        }
    }

    public void registerCommand(String className) throws RepositoryException {
        registerCommand(className, null);
    }

    public void registerCommand(Class clazz) throws RepositoryException {
        registerCommand(clazz, null);
    }

    public void registerCommand(Command command) throws RepositoryException {
        registerCommand(command, null);
    }

    public void registerCommand(Class clazz, Long delay)
            throws RepositoryException {
        try {
            registerCommand((Command) clazz.newInstance(), delay);
        } catch (InstantiationException e) {
            throw new RepositoryException(
                    "Error instantiating command from class " + clazz.getName(),
                    e);
        } catch (IllegalAccessException e) {
            throw new RepositoryException(
                    "Error instantiating command from class " + clazz.getName(),
                    e);
        }
    }

    public void registerCommand(Command command, Long delay)
            throws RepositoryException {
        commands.add(command);
        if (delay != null) {
            commandDelay.put(command, delay);
        }
        if (started) {
            startCommand(command);
        }
    }

    private void startCommand(Command command) throws RepositoryException {
        if (command instanceof AbstractCommand) {
            AbstractCommand aCommand = (AbstractCommand) command;
            aCommand.setConnectionProvider(connectionProvider);
            aCommand.setStoreProvider(storeProvider);
            aCommand.setCommandManager(this);
        }
        if (command instanceof AbstractRepositoryCommand) {
            AbstractRepositoryCommand aCommand = (AbstractRepositoryCommand) command;
            aCommand.setRepository(repository);
        }
        if (command instanceof FTSCommand){
            ((FTSCommand)command).setFTSBatchSize(FTSBatchSize);
        }
        
        if (command.init()){
	        Long delay = null;
	        delay = commandDelay.get(command);
	        if (delay == null) {
	            delay = Constants.DEFAULT_COMMAND_EXECUTION_DELAY;
	        }
	        /*scheduler.scheduleWithFixedDelay(new CommandExecutor(command), 0,
	                delay, TimeUnit.SECONDS);*/
	        taskManager.schedule(new CommandExecutor(command), delay);
        }
    }

    public void start() throws RepositoryException {
        if (started) {
            throw new RepositoryException("Manager already started");
        }
        //scheduler = Executors.newScheduledThreadPool(corePoolSize, JCRServiceLocator.getThreadFactory());
        if (taskManager == null){
        	taskManager = TaskManager.getInstance(new HashMap<String, String>());
        }
        //scheduler = Executors.newSingleThreadScheduledExecutor(getThreadFactory());
        for (Command c : commands) {
            startCommand(c);
            log.info("Command started: " + c.getDisplayableName());
        }
        started = true;
    }



	public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }
	
	public void setTaskManager(TaskManager taskManager){
		this.taskManager = taskManager;
	}

    public void setRepository(RepositoryImpl repository) {
        this.repository = repository;
    }

    public void setStoreProvider(ContentStoreProvider storeProvider) {
        this.storeProvider = storeProvider;
    }

	public void setThreadCount(int threadCount) {
		this.corePoolSize = threadCount;
		
	}
    
    public void setFTSBatchSize(int size){
        this.FTSBatchSize = size;
    }

	public void forceCommandExecutio(String className) {
		for (Command c : commands) {
			if (c.getClass().getName().equals(className)){
				try {
					taskManager.execute(new CommandExecutor(c));
				} catch (RepositoryException e) {
					e.printStackTrace();
				}
			}
		}
		//taskManager.schedule(new CommandExecutor(command), delay);
		
	}

}

/*
 * $Log: CommandManager.java,v $
 * Revision 1.3  2009/03/24 07:54:44  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:40  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/02/22 09:24:14  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2006/11/09 13:44:51  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.3  2006/09/28 12:23:41  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.2  2006/08/18 08:18:16  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.1  2006/08/15 08:38:01  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.5  2006/08/10 13:10:00  dparhomenko
 * PTR#0147668 fix add mixin
 *
 * Revision 1.4  2006/07/25 12:56:33  maksims
 * #1802425 unneeded import removed
 *
 * Revision 1.3  2006/07/18 12:51:13  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.2  2006/07/17 14:47:45  zahars
 * PTR#0144986 Cleanup
 *
 * Revision 1.1  2006/07/14 08:21:33  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.1  2006/07/04 09:27:19  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 */