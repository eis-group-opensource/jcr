/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.lang.ref.WeakReference;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;

public class ChildMaxPosition {
	
	private boolean changed = false;
	private long max;
	private QName childName;
	private int childPosition=0;
	private Path parent;
	private int hash = 0;
	private WeakReference<_NodeState> item;
	private _AbstractsStateManager stateManager;
	boolean isNew = false;
	private Long nodeId;

	
	public ChildMaxPosition(Path parent, QName childName,_NodeState item,  long max, _AbstractsStateManager stateManager){
		this.parent = parent;
		this.childName = childName;
		this.max = max;
		this.stateManager = stateManager;
		if (item != null){
			this.item = new WeakReference<_NodeState>(item);
			this.nodeId = item.getNodeId();
		}
		if (max == 0){
			isNew = true;
		}
		//this.childPosition = item.getIndex();
	}
	
	public boolean isNew(){
		return isNew;
	}

	public boolean isChanged() {
		return changed;
	}

	public long getMax() {
		return max;
	}

	public void setMax(long max) {
		this.max = max;
	}

	public QName getChildName() {
		return childName;
	}

	public Path getParent() {
		return parent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
            return true;
        }
        if (obj instanceof ChildMaxPosition) {
        	ChildMaxPosition other = (ChildMaxPosition)obj;
        	if (other.getParent().equals(parent) && other.getChildName().equals(childName) && childPosition == other.childPosition){
        		return true;
        	}
        }
        return false;
	}

	
	@Override
	public int hashCode() {
		int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + parent.hashCode();
            h = 37 * h + childName.hashCode();
            hash = h;
        }
        return h;
		
	}

	public _NodeState getItem() throws RepositoryException {
		if (nodeId != null){
			_NodeState result = item.get();
			if (result == null){
				result = stateManager.getNodeState(nodeId, null);
				item = new WeakReference<_NodeState>(result);
			}
			return result;
		} else {
			return null;
		}
	}

	public void inc(_NodeState state) throws RepositoryException {
		if (max  < 0){
			max = 0;
		}
		if (item == null){
			item =new WeakReference<_NodeState>(state);
			nodeId = state.getNodeId();
		}
		max++;
		if (!isNew){
			changed = true;
			isNew = false;
		}
		stateManager.updateSNSMax(this);
		
	}
	public void dec() throws RepositoryException {
		max--;
		changed = true;
		stateManager.updateSNSMax(this);
	}

	public void resetNew() {
		isNew = false;
	}
	
}
