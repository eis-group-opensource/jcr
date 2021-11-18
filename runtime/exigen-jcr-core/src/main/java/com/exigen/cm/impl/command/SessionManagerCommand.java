/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.command;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.AbstractRepositoryCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.SessionManager;
import com.exigen.cm.jackrabbit.lock.LockManagerImpl;

public class SessionManagerCommand extends AbstractRepositoryCommand {

	//timeout in minutes
	private static final int TIMEOUT = 5;

	private Log log = LogFactory.getLog(SessionManagerCommand.class);
	
	@Override
	public String getDisplayableName() {
		return "SessionManagerCommand";
	}

	public boolean execute(DatabaseConnection connection) throws RepositoryException {
		log.debug("Execute "+getDisplayableName());
		
		JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
		try {
			DatabaseConnection conn = null;
		
			SessionManager sessionManager = repository.getSessionManager();
			//1. update active sessions
			List<SessionImpl> sessions = sessionManager.getActiveSessions();
			if (sessions.size() > 0){
				conn = getConnection(conn);
			}
			ArrayList<Long> ids = new ArrayList<Long>();
			for(SessionImpl session:sessions){
				ids.add(session.getSessionId());
				if (ids.size() > 0 && ids.size() > 100){
					executeUpdateSessions(conn, ids);
					ids.clear();
				}
			}
			
			if (ids.size() > 0){
				executeUpdateSessions(conn, ids);
				ids.clear();
			}
			
			
			
			
			//2. remove logged out sessions
			sessions = sessionManager.getClosedSessions();
			if (sessions.size() > 0){
				conn = getConnection(conn);
			}
			ids = new ArrayList<Long>();
			for(SessionImpl session:sessions){
				ids.add(session.getSessionId());
				if (ids.size() > 0 && ids.size() > 100){
					executeDeleteSessions(conn, ids);
					ids.clear();
				}
			}
			
			if (ids.size() > 0){
				executeDeleteSessions(conn, ids);
				ids.clear();
			}
			
			//3. clean up dead sessions TODO
		
			conn = getConnection(conn);
			Calendar time = Calendar.getInstance();
			time.add(Calendar.MINUTE, - TIMEOUT);
			DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_SESSION_MANAGER, true);
			st.addCondition(Conditions.lt(Constants.TABLE_SESSION_MANAGER__DATE, time));
			st.execute(conn);
			/*LockManagerImpl lm = null;
			if (st.hasNext()){
				
			}*/
			while (st.hasNext()){
				RowMap row = st.nextRow();
				Long sessionId = row.getLong(Constants.FIELD_ID);
				log.warn("Lost session "+sessionId);
				//inlock all nodes that are belong to this session
				//select * from jcr_jcr.CM_NODE_LOCK_INFO where SESSION_ID = XXX and PARENT_LOCK_ID = NODE_ID
				
				DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(Constants._TABLE_NODE_LOCK_INFO, true);
				st1.addCondition(Conditions.eqProperty(Constants.FIELD_TYPE_ID, Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID));
				st1.addCondition(Conditions.eq(Constants.TABLE_NODE_LOCK_INFO__SESSION_ID, sessionId));
				st1.execute(conn);
				
				while (st1.hasNext()){
					RowMap row1 = st1.nextRow();
					Long nodeId = row1.getLong(Constants.FIELD_TYPE_ID);
					log.info("Unlock Node "+nodeId);
					LockManagerImpl lm = new LockManagerImpl(repository.getNodeTypeManager().getNodeTypeRegistry(), repository.getConnectionProvider().createConnection(), repository);
					lm.unlock(nodeId, "sessionmanager", (long) -1, 
							null, new HashMap<String, Object>());
				}
				DatabaseDeleteStatement dst = DatabaseTools.createDeleteStatement(Constants.TABLE_SESSION_MANAGER);
				dst.addCondition(Conditions.eq(Constants.FIELD_ID, sessionId));
				dst.execute(conn);
				
			}
		
			if (conn != null){
				conn.commit();
				conn.close();
			}
			sessionManager.eraseSession(sessions);
		} finally {
			TransactionHelper.getInstance().commitAndResore(tr);
		}
		
		return false;
	}

	private void executeDeleteSessions(DatabaseConnection conn, ArrayList<Long> ids) throws RepositoryException {
		DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(Constants.TABLE_SESSION_MANAGER);		
		st.addCondition(Conditions.in(Constants.FIELD_ID, ids));
		st.execute(conn);
	}

	private void executeUpdateSessions(DatabaseConnection conn, ArrayList<Long> ids) throws RepositoryException{
		DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(Constants.TABLE_SESSION_MANAGER);		
		st.addCondition(Conditions.in(Constants.FIELD_ID, ids));
		st.addValue(SQLParameter.create(Constants.TABLE_SESSION_MANAGER__DATE,Calendar.getInstance() ));
		st.execute(conn);
	}

	public DatabaseConnection getConnection(DatabaseConnection conn) throws RepositoryException{
		if (conn == null){
			conn =  repository.getConnectionProvider().createConnection();
			conn.lockTableRow(Constants.TABLE_SYSTEM_PROPERTIES, Constants.FIELD_ID , Constants.TABLE_SYSTEM_PROPERTIES__VALUE__CREATED_DATETIME);
			return conn;
		} else {
			return conn;
		}
	}
	
}
