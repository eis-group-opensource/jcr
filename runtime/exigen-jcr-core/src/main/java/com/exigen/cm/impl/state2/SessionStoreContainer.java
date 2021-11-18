/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConstants;

public class SessionStoreContainer extends StoreContainer{

	private SessionImpl session;
	
	public SessionStoreContainer(SessionImpl s) throws RepositoryException{
		super(s.getConnection());
		this.session = s;
	}
	
	@Override
	public DatabaseConnection getConnection() throws RepositoryException {
		return session.getConnection();
	}

	@Override
	protected ContentStore createContentStore(String storeId) {
		return session._getWorkspace()._getRepository().createContentStore(storeId);
	}

	@Override
	protected ContentStore _getDefaultStore() {
		return session._getWorkspace().getRepository().getContentStoreProvider().getStore(ContentStoreConstants.DEFAULT_STORE_NAME);
	}
	
	@Override
	protected ContentStore assignSession(ContentStore store) throws RepositoryException {
		if (!store.isTransactionStarted()){
			store.begin(getConnection());
		}
		return store;
	}
	

	
	
	

}
