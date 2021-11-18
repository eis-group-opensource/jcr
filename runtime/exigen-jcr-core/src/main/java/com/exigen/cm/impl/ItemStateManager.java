/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.HashMap;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.version.NodeStateEx;

public class ItemStateManager {

	
    private static final Log log = LogFactory.getLog(ItemStateManager.class);
    private HashMap<Long, NodeStateEx> states = new HashMap<Long, NodeStateEx>();
	//private RepositoryImpl repository;
	private WorkspaceImpl workspace;
	private NamespaceRegistryImpl namespaceRegistry;
	//private NodeTypeManagerImpl nodeTypeManager;
	//private NodeTypeRegistry ntReg;
	private _SessionStateManager stateManager;
	//private DatabaseConnection connection;
    

    public ItemStateManager(WorkspaceImpl workspace) throws RepositoryException{
    	//this.repository = workspace.getRepository();
    	this.workspace = workspace;
    	this.namespaceRegistry = workspace._getNamespaceRegistry();
    	//this.nodeTypeManager = workspace._getNodeTypeManager();
    	//this.connection = connection;
    	//this.ntReg = ((SessionImpl)workspace.getSession()).getNodeTypeRegistry();
    	this.stateManager = ((SessionImpl)workspace.getSession()).getStateManager();
    	
    }
    
    public NodeStateEx getNodeState(Long nodeId, DatabaseConnection connection) 
    	throws ItemNotFoundException, RepositoryException {
        if (states.containsKey(nodeId)){
            return states.get(nodeId);
        }
        
        NodeStateEx result = new NodeStateEx(stateManager.getNodeState(nodeId, null), stateManager);

        states.put(nodeId, result);
        
        return result;

    }
    
    protected NodeStateEx getNodeState(Path nodePath, DatabaseConnection conn)
            throws PathNotFoundException, RepositoryException {
        try {
        	Long nodeId = ((_NodeState)stateManager.getItem(nodePath, true)).getNodeId();
            if (nodeId == null) {
                throw new PathNotFoundException(safeGetJCRPath(nodePath));
            }
            
            return getNodeState(nodeId, conn);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(nodePath));
        }
        
    }    
    
    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        try {
            return path.toJCRPath(namespaceRegistry);
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use
     * in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     */
    public String safeGetJCRPath(Long id) {
        /*        try {
                    return safeGetJCRPath(hierMgr.getPath(id));
                } catch (ItemNotFoundException e) {
                    // return string representation of id as a fallback
                    return id.toString();
                } catch (RepositoryException e) {
                    log.error(id + ": failed to build path");
                    // return string representation of id as a fallback
                    return id.toString();
                }*/
                //throw new UnsupportedOperationException();
        return "[NodeId:"+id+"]";
            }
    
    public String safeGetJCRPath(NodeStateEx state) throws RepositoryException {
        return "[NodeId:"+state.getPath()+"]";
        
            }
    
    public String safeGetJCRPath(ItemId id) {
        /*        try {
                    return safeGetJCRPath(hierMgr.getPath(id));
                } catch (ItemNotFoundException e) {
                    // return string representation of id as a fallback
                    return id.toString();
                } catch (RepositoryException e) {
                    log.error(id + ": failed to build path");
                    // return string representation of id as a fallback
                    return id.toString();
                }*/
                throw new UnsupportedOperationException();
            }

	public boolean inEditMode() {
		return true;
	}

	public void store(_PropertyState prop) {
		throw new UnsupportedOperationException();
	}

	public void store(NodeStateEx newState) throws RepositoryException {
		if (newState.getNodeState().getStatus() == ItemStatus.New){
			newState._getParent().save();
		} else {
			newState.save();
		}
	}

	public boolean hasItemState(NodeId id) {
		throw new UnsupportedOperationException();
	}

	public NodeStateEx createNew(QName nodeName, NodeId id, QName nodeTypeName, 
	        NodeStateEx destParent, boolean referenceable, 
	        boolean ownSecurity, boolean createAutoCreatedChilds) throws RepositoryException{
		return destParent.addNode(nodeName, nodeTypeName, id, referenceable,ownSecurity, createAutoCreatedChilds, true);
	}

	public DatabaseConnection getConnection() throws RepositoryException{
		return workspace.getConnection();
	}

	
}
