/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class NodeTreeItem {

	private Long id;
	private Long depth;
	private NodeTreeItem parent;
	//private ArrayList<NodeTreeItem> childs = new ArrayList<NodeTreeItem>();
	private HashMap<Long, NodeTreeItem> childs = new HashMap<Long, NodeTreeItem>();
	private ArrayList<Long> batch = null;

	public NodeTreeItem(Long id, Long depth, NodeTreeItem parent){
		this.id = id;
		this.depth = depth;
		this.parent = parent;
		if (parent != null){
			parent.addChild(this);
		}
	}

	private void addChild(NodeTreeItem item) {
		//childs.add(item);
		childs.put(item.getId(), item);
		
	}

	public Long getId() {
		return id;
	}

	public NodeTreeItem find(Long parentId, Long _depth) {
		if (parentId.equals(id)){
			return this;
		}
		/*for(NodeTreeItem i : childs){
			if (i.getId().equals(parentId)){
				return i;
			}
		}*/

		
		if (depth.equals(_depth)){
			/*for(NodeTreeItem i : childs){
				if (i.getId().equals(parentId)){
					return i;
				}
			}
			return null;*/
			return childs.get(parentId);
		} else {
			NodeTreeItem result = null;
			for(NodeTreeItem i : childs.values()){
				result = i.find(parentId, _depth);
				if (result != null){
					break;
				}
			}
			return result;
			
			
		}
	}

	/*public ArrayList<Long> getChildIds(){
		ArrayList<Long> ids = new ArrayList<Long>();
		for(NodeTreeItem i : childs){
			ids.add(i.getId());
		}
		return ids;
		
	}*/
	
	public List<Long> getBatch(){
		if (batch != null){
			return batch;
		} else {
			return parent.getBatch();
		}
	}
	
	public void addChilds(int batchSize) {
		if (parent == null || parent.allowAdd(batchSize)){
			if (parent == null && batch == null){
				batch = new ArrayList<Long>();
			}
			if (parent == null){
				addToBatch(id);
			} else {
				parent.addToBatch(id);
			}
			
		} else {
			batch = new ArrayList<Long>();
			batch.add(id);
		}
		for(NodeTreeItem i : childs.values()){
			i.addChilds(batchSize);
		}
		
	}

	private void addToBatch(Long id2) {
		if (batch != null){
			batch.add(id2);
		} else {
			parent.addToBatch(id2);
		}
		
	}

	private boolean allowAdd(int batchSize) {
		if (batch != null){
			return batch.size() < batchSize; 
		} else {
			if (parent != null){
				return parent.allowAdd(batchSize);
			} else {
				batch = new ArrayList<Long>();
				return true;
			}
		}
	}

	public Collection<NodeTreeItem> getChilds() {
		return childs.values();
		
	}
	
}
