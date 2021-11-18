/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.ContentStore;

public abstract class StoreContainer {

	private HashMap<String, ContentStore> stores = new HashMap<String, ContentStore>();

	private ContentStore _defaultStore;

	private DatabaseConnection connection;

	public StoreContainer(DatabaseConnection connection) {
		this.connection = connection;
	}

	public ContentStore getDefaultStore() throws RepositoryException {
		if (_defaultStore == null) {
			_defaultStore = _getDefaultStore();
	        stores.put("", _defaultStore);
		}
		if (!stores.containsKey("")){
	        stores.put("", _defaultStore);
		}
		return assignSession(_defaultStore);
	}

	protected ContentStore assignSession(ContentStore store) throws RepositoryException {
		return store;
	}

	public final ContentStore getContentStore(String storeId) throws RepositoryException {
		if (storeId == null || storeId.equals("")) {
			storeId = "";
			getDefaultStore();
		}
		if (!stores.containsKey(storeId)) {
			stores.put(storeId, createContentStore(storeId));
		}
		ContentStore result = (ContentStore) stores.get(storeId);
		if (!result.isTransactionStarted()){
			result.begin(connection);
		}
		return assignSession(result);
	}

	public void commitStores() {
		for (Iterator it = stores.values().iterator(); it.hasNext();) {
			ContentStore st = (ContentStore) it.next();
			if (st.isTransactionStarted()) {
				st.commit();
			}
		}
		stores.clear();
	}

	public void commitPutStores() {
		for (Iterator it = stores.values().iterator(); it.hasNext();) {
			ContentStore st = (ContentStore) it.next();
			if (st.isTransactionStarted()) {
				st.commitPut();
			}
		}
	}

	public void rollbackStores() {
		for (Iterator it = stores.values().iterator(); it.hasNext();) {
			ContentStore st = (ContentStore) it.next();
			if (st.isTransactionStarted()) {
				st.rollback();
			}
		}
		stores.clear();
	}

	abstract protected ContentStore _getDefaultStore();

	abstract protected ContentStore createContentStore(String storeId);

	abstract public DatabaseConnection getConnection()
			throws RepositoryException;

}
