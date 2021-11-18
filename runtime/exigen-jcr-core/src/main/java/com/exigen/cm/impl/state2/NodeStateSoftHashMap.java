/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.SoftHashMap;

/**
 * This hash map is created to support hard cache of node states during one transaction.
 * 
 * @author givans
 *
 */
public class NodeStateSoftHashMap extends SoftHashMap<Long, _NodeState> {
	
	private Log log = LogFactory.getLog(NodeStateSoftHashMap.class);
	
	private final Map<Long, _NodeState> oneTransactionCache = new HashMap<Long, _NodeState>();
	
	
	public NodeStateSoftHashMap(int hardSize) {
		super(hardSize);
	}
	
	
	
	@Override
	public _NodeState put(Long key, _NodeState state) {
		
		JCRTransaction stateTransaction = state.getCreateInTransaction();
		JCRTransaction currentTransaction = getCurrentTransaction();
		
		if (stateTransaction != null && currentTransaction != null) {
			if (stateTransaction.equals(currentTransaction)) {
				//during one transaction lets use hard cache.
				synchronized (oneTransactionCache) {
					return oneTransactionCache.put(key, state);
				}
			}
		}
		return super.put(key, state);
		
	}
	
	@Override
	public _NodeState get(Object key) {
		_NodeState state = oneTransactionCache.get(key);
		
		if (state != null) {
			
			JCRTransaction stateTransaction = state.getCreateInTransaction();
			JCRTransaction currentTransaction = getCurrentTransaction();
			
			//stateTransaction must not be null!
			if (!stateTransaction.equals(currentTransaction)) {
				//transaction changed, clearing hard cache
				synchronized (oneTransactionCache) {
					oneTransactionCache.clear();
				}
			}		
		} else {
			state = super.get(key);
		}		
		return state;
	}
	
	@Override
	public void clear() {
		synchronized (oneTransactionCache) {
			oneTransactionCache.clear();			
		}
		super.clear();
	}
	
	@Override
	public _NodeState remove(Object key) {
		_NodeState removed;
		synchronized (oneTransactionCache) {
			removed = oneTransactionCache.remove(key);
		}
		if (removed == null) {
			removed = super.remove(key); 
		}
		return removed;
	}
	
	@Override
	public Collection<_NodeState> values() {
		Collection<_NodeState> superValues = super.values();
		superValues.addAll(oneTransactionCache.values());
		return superValues;
	}
	
	@Override
	public Set<Entry<Long, _NodeState>> entrySet() {
		Set<Entry<Long, _NodeState>> entrySet = super.entrySet();
		entrySet.addAll(oneTransactionCache.entrySet());
		return entrySet;
	}
	
	@Override
	public int size() {	
		return super.size() + oneTransactionCache.size();
	}
	
	
	private JCRTransaction getCurrentTransaction() {
    	JCRTransaction currentTransaction = null;
		try {
			currentTransaction = TransactionHelper.getCurrentTransaction();
		} catch (RepositoryException e) {
			log.warn("Error obtaining current transaction: "+e.getMessage(), e);
		}
		return currentTransaction;
	}	
}
