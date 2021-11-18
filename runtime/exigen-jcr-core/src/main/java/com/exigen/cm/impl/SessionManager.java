/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;

public class SessionManager {

	private RepositoryImpl repository;
	

	Set<WeakReference<SessionImpl>> _sessions = Collections.synchronizedSet((new HashSet<WeakReference<SessionImpl>>()));
	Set<SessionImpl> closedSessions = Collections.synchronizedSet(new HashSet<SessionImpl>());
	
	public SessionManager(RepositoryImpl repository) {
		this.repository = repository;
	}

	public void registerSession(SessionImpl session) throws RepositoryException {
		synchronized (_sessions) {
			/*if (sessions.contains(session)){
				return;
			}*/
			_sessions.add(new WeakReference(session));
		}
		createSessionRecord(session.getSessionId());

	}


	public void unregisterSession(SessionImpl session) {
		synchronized (_sessions) {
			List<WeakReference<SessionImpl>>  remove = new ArrayList<WeakReference<SessionImpl>>();
			for(WeakReference<SessionImpl> rf:_sessions){
				SessionImpl s = rf.get();
				if (s != null && s.getSessionId().equals(session.getSessionId())){
					remove.add(rf);
					break;
				}
				if (s == null){
					remove.add(rf);
				}
			}
			_sessions.removeAll(remove);
			
		}
		synchronized (closedSessions) {
			closedSessions.add(session);
		}
	}
	
	public List<SessionImpl> getActiveSessions(){
		ArrayList<SessionImpl> result = new ArrayList<SessionImpl>();
		synchronized (_sessions) {
			List<WeakReference<SessionImpl>>  remove = new ArrayList<WeakReference<SessionImpl>>();
			for(WeakReference<SessionImpl> rf:_sessions){
				SessionImpl s = rf.get();
				if (s != null){
					result.add(s);
				} else {
					remove.add(rf);
				}
				_sessions.removeAll(remove);
			}
		}
		return result;
	}

	public List<SessionImpl> getClosedSessions(){
		ArrayList<SessionImpl> result = new ArrayList<SessionImpl>();
		synchronized (closedSessions) {
			result.addAll(closedSessions);
		}
		return result;
	}

	public void eraseSession(List<SessionImpl> removed){
		synchronized (closedSessions) {
			closedSessions.removeAll(removed);
		}
	}
	
	private void createSessionRecord(Long sessionId) throws RepositoryException {
		JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
		try {
			DatabaseConnection conn = repository.getConnectionProvider().createConnection();
			DatabaseInsertStatement st = DatabaseTools.createInsertStatement(Constants.TABLE_SESSION_MANAGER);
			st.addValue(SQLParameter.create(Constants.FIELD_ID, sessionId));
			st.addValue(SQLParameter.create(Constants.TABLE_SESSION_MANAGER__DATE,Calendar.getInstance() ));
			st.execute(conn);
			conn.commit();
			conn.close();
		} finally {
			TransactionHelper.getInstance().commitAndResore(tr);
		}
	}
	

}
