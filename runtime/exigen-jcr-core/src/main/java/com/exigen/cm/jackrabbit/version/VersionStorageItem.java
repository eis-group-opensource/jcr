/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.NodeTypeContainer;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.ParentNode;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.state2.ChangeState;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.QName;

public class VersionStorageItem {

	private VersionStorageItem parent;
	private String name;
	private Long nodeId;
	private Long securityId;
	private String path;
	private List<VersionStorageItem> childs = new ArrayList<VersionStorageItem>();
	private RepositoryImpl repository;
	private Long storeConfigurationId;
	private Long depth;
	private NodeTypeManagerImpl ntManager;
	private List<ParentNode> parentNodes = new ArrayList<ParentNode>();
	
	public VersionStorageItem(String name, VersionStorageItem parent, RepositoryImpl repository, Long depth, NodeTypeManagerImpl ntManager){
		this.name = name;
		this.parent = parent;
		this.repository = repository;
		this.depth = depth;
		this.ntManager = ntManager;
	}
	
	public VersionStorageItem addChild(String name){
		for(VersionStorageItem child:childs){
			if (child.getName().equals(name)){
				return child;
			}
		}
		VersionStorageItem c = new VersionStorageItem(name, this, repository, depth + 1, ntManager);
		childs.add(c);
		return c;
	}

	public List<VersionStorageItem> getChilds() {
		return childs;
	}

	public String getName() {
		return name;
	}

	public Long getNodeId() {
		return nodeId;
	}

	public VersionStorageItem getParent() {
		return parent;
	}

	public String getPath() {
		return path;
	}

	public Long getSecurityId() {
		if (securityId == null){
			return parent.getSecurityId();
		}
		return securityId;
	}

	public void setPath(String p1) {
		this.path = p1;
		
	}

	public void initializeTree(HashMap<String, RowMap> values) {
		RowMap m = values.get(path);
		if (m != null){
			this.nodeId = m.getLong(Constants.FIELD_ID);
			this.securityId = m.getLong(Constants.TABLE_NODE__SECURITY_ID);
			this.storeConfigurationId = m.getLong(Constants.TABLE_NODE__CONTENT_STORE_CONFIG_NODE);
		}
		if ( m != null || this.path == null){
			for(VersionStorageItem child: childs){
				child.initializeTree(values);
			}
		}
		
	}

	public void populate(ChangeState changeState, DatabaseInsertStatement ntBaseSt ,DatabaseInsertStatement ntVersionStorageSt) throws RepositoryException{
		if (path != null && nodeId == null){
			//node state
			_NodeState st = new _NodeState(repository);
			this.nodeId = st.getNodeId();
			st.setName(new QName("", name));
			st.setParentId(parent.getNodeId());
			st.setSecurityId(parent.getSecurityId());
			st.setIndex((long)1);
			st.setSnsMax((long)1);
			st.setNodeTypeId(repository.getVersionStorageTypeId());
			st.buildInternalPath(parent.getPath(), parent.getDepth());
	        st.setWorkspaceId(null);
	        st.setStoreConfigurationId(parent.getStoreConfigurationId());
			changeState.addNewNode(st);
			
			//node types
			/*EffectiveNodeType ent = st.getEffectiveNodeType();
	        QName[] allTypes = ent.getAllNodeTypes();
	        for(int i = 0 ; i < allTypes.length ; i++){*/
	        //}			
            NodeTypeImpl from = ntManager.getNodeType(QName.REP_VERSIONSTORAGE);	        	
            NodeTypeImpl nt = ntManager.getNodeType(QName.NT_BASE);
        	NodeTypeContainer ntc = new NodeTypeContainer(st.getNodeId(), nt, from);
        	changeState.addNewType(ntc);
        	//ntBaseSt.addValue(SQLParameter.create(Constants.FIELD_ID, repository.nextId()));
        	ntBaseSt.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, nodeId));
        	ntBaseSt.addBatch();
            nt = ntManager.getNodeType(QName.REP_VERSIONSTORAGE);
        	ntc = new NodeTypeContainer(st.getNodeId(), nt, from);
        	changeState.addNewType(ntc);
        	//ntVersionStorageSt.addValue(SQLParameter.create(Constants.FIELD_ID, repository.nextId()));
        	ntVersionStorageSt.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, nodeId));
        	ntVersionStorageSt.addBatch();
			
			//parent nodes
	        
	        for(ParentNode pn:getParents()){
	        	changeState.addNewParentNode(pn);
	        }
			
			//record in type tables
			
		}
		
		for(VersionStorageItem child: childs){
			child.populate(changeState, ntBaseSt, ntVersionStorageSt);
		}
	}

	private Long getStoreConfigurationId() {
		return storeConfigurationId;
	}

	public Long getDepth() {
		return depth;
	}

	public List<ParentNode> getParents() {
        if (parentNodes.size() == 0){
	        for(ParentNode ps:parent.getParents()){
	        	parentNodes.add(new ParentNode(nodeId, ps.getParentId(), ps.getPosition().longValue() + 1));
	        }
	        parentNodes.add(new ParentNode(nodeId, parent.getNodeId(), 1));
        }
		
		return parentNodes;
	}

	public void setParents(List<ParentNode> parentNodes) {
		this.parentNodes = parentNodes;
		
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void populateIds(ArrayList<Long> ids) {
		if (childs.size() == 0){
			ids.add(nodeId);
		} else {
			for(VersionStorageItem child:childs){
				child.populateIds(ids);
			}
		}
		
	}

	public void buildPaths(HashMap<String, Long> paths) {
		if (childs.size() == 0){
			paths.put(path, nodeId);
		} else {
			for(VersionStorageItem child:childs){
				child.buildPaths(paths);
			}
		}
	}
}
