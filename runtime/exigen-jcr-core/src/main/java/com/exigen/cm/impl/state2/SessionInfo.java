/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import com.exigen.cm.impl.RepositoryImpl;

public class SessionInfo {

	private String userId;
	private Long sessionId;
	private String workspaceName;
	private RepositoryImpl repository;
	
	public SessionInfo(RepositoryImpl repository2, Long sessionId2, 
			String userId2, String workspaceName2) {
		this.repository = repository2;
		this.sessionId = sessionId2;
		userId = userId2;
		workspaceName = workspaceName2;
	}
	public Long getSessionId() {
		return sessionId;
	}
	public String getUserId() {
		return userId;
	}
	public String getWorkspaceName() {
		return workspaceName;
	}
	public RepositoryImpl getRepository() {
		return repository;
	}
	
	
	
}
