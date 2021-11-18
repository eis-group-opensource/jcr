/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.taskmanager.Task;

public class CommandExecutor implements Task {

	private Log log = LogFactory.getLog(CommandExecutor.class);

	private Command command;
	
	private boolean execute = true;

	CommandExecutor(Command command) {
		this.command = command;
	}

	public void run() {
		boolean rerun = true;
		DatabaseConnection conn = null;
		try {
			conn = ((AbstractRepositoryCommand) command).getConnectionProvider().createConnection();
		} catch (RepositoryException e1) {
			log.error(e1);
			return;
		}
		try {
			while (rerun && execute) {
				try {
					log.debug("Execute " + command.getClass().getName());
					rerun = command.execute(conn);
					conn.commit();
				} catch (Exception e) {
					try {
						conn.rollback();
					} catch (RepositoryException e1) {
					}
					log.error(e.getMessage(), e);
					rerun = false;
				}
	
			}
		} finally {
			try {
				conn.close();
			} catch (RepositoryException e) {
			}
		}
	}

	public void release() {
		execute = false;
	}

	@Override
	public String toString() {
		return "Command "+command.getDisplayableName();
	}
}

/*
 * $Log: CommandExecutor.java,v $
 * Revision 1.2  2008/07/23 09:56:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:40  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2007/02/22 09:24:14  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.5  2006/11/09 13:44:51  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.4  2006/10/02 15:07:06  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.3  2006/09/28 12:39:48  dparhomenko
 * PTR#1802402 add oracle tests
 * Revision 1.2 2006/09/28 07:45:28 dparhomenko
 * PTR#1803066
 * 
 * Revision 1.1 2006/08/15 08:38:01 dparhomenko PTR#1802426 add new features
 * 
 * Revision 1.1 2006/07/14 08:21:33 zahars PTR#0144986 Old FTS Deleted
 * 
 * Revision 1.1 2006/07/04 09:27:19 dparhomenko PTR#1802310 Add new features to
 * DatabaseConnection
 * 
 */