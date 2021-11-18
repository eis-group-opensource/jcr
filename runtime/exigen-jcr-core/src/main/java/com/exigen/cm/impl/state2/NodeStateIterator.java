/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.util.ChildrenCollectorFilter;

public class NodeStateIterator implements Iterator<_NodeState>,Iterable<_NodeState>, SizeIterator{

	private IdIterator ids;
	private _AbstractsStateManager stManager;
	private String namePattern;
	private ArrayList<_NodeState> cache = new ArrayList<_NodeState>();
	int pos = 0;

	public NodeStateIterator(IdIterator ids, _AbstractsStateManager stManager, String namePattern){
		this.ids = ids;
		this.stManager = stManager;
		this.namePattern = namePattern;
	}
	
	private boolean loadNext(){
		try {
			while (ids.hasNext()){
				Long id = ids.next();
				ArrayList<Long> nextIds = ids.getNextIds();
				_NodeState node =  stManager.getNodeState(id, nextIds);
				boolean result = false;
				if (namePattern == null || ChildrenCollectorFilter.matches(node.getName().toJCRName(stManager.getNamespaceRegistry()), namePattern)){
				    cache.add(node);
				    result = true;
				}
				ArrayList<Long> empty = new ArrayList<Long>();
				nextIds.remove(id);
				for(Long i:nextIds){
					id = ids.next();
					if (!id.equals(i)){
						break;
					}
					node =  stManager.getNodeState(i, empty);
					if (namePattern == null || ChildrenCollectorFilter.matches(node.getName().toJCRName(stManager.getNamespaceRegistry()), namePattern)){
					    cache.add(node);
					    result = true;
					}
				}
				if (result){
					return true;
				}
				
			}
			return false;
		} catch (RepositoryException e) {
			throw new IllegalStateException();
		} catch (NoPrefixDeclaredException e) {
			throw new IllegalStateException();
		}
	}
	

	public boolean hasNext() {
		if (pos < cache.size()){
			return true;
		}
		return loadNext();
	}

	public _NodeState next() {
		return cache.get(pos++);
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public void close(){
		
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	public Iterator<_NodeState> iterator() {
		return this;
	}

	public int getSize() {
		while (loadNext()){}
		return cache.size();
	}

    public Long get(int index) {
       while(loadNext()){
           
       }
       return cache.get(index).getNodeId();
    }
	
	

}
