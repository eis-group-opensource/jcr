/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConstants;

public class StandaloneStoreContainer extends StoreContainer {

	private _StandaloneStatemanager statemanager;

	public StandaloneStoreContainer(_StandaloneStatemanager statemanager) throws RepositoryException {
		super(statemanager.getConnection());
		this.statemanager = statemanager;
	}

	@Override
	protected ContentStore _getDefaultStore() {
		return createContentStore(ContentStoreConstants.DEFAULT_STORE_NAME);
	}

	@Override
	protected ContentStore createContentStore(String storeId) {
		return statemanager.getRepository().getContentStoreProvider().getStore(storeId);
	}

	@Override
	public DatabaseConnection getConnection() throws RepositoryException {
		return statemanager.getConnection();
	}

}
