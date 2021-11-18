/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.Collection;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.EventState;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;

public abstract class _ItemState {
	
    /** Log for this class */
    private static final Log log = LogFactory.getLog(_ItemState.class);

	private ItemStatus status = ItemStatus.New;

	//private _RepositoryStateManager stateManager;

	private RepositoryImpl repository;

	protected NodeTypeManagerImpl nodeTypeManager;
	
    protected Path cachedPath = null;

	private NamespaceRegistryImpl namespaceRegistry;

	private _ItemId id = new _ItemId();

	public _ItemState(RepositoryImpl repository) {
		this.repository = repository;
	}

	public RepositoryImpl getRepository(){
		return repository;
	}
	
	public void setStatusNormal() {
		this.status = ItemStatus.Normal;
	}
	

	
	public void setStatusDestroyed(){
		this.status = ItemStatus.Destroyed;
	}
	
	public void setStatusInvalidated(){
		if (ItemStatus.New.equals(status)){
			this.status = ItemStatus.Destroyed;
		} else {
			this.status = ItemStatus.Invalidated;
		}
	}
	
    public void setStatusModified(){
    	this.status = ItemStatus.Modified;
    }

	
    protected NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException {
    	if (nodeTypeManager == null){
    		return repository.getNodeTypeManager();
    	}
		return nodeTypeManager;
	}
    
    protected NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException{
    	return getNodeTypeManager().getNodeTypeRegistry();
    }
    
    public Path getPrimaryPath() throws RepositoryException {
        if (cachedPath == null){
            if (isNode()){
            	_NodeState ns = (_NodeState) this;
            	if (ns.getWeakParentState() == null){
            		Long pId = ns.getParentId();
            		_NodeState st = ns.getStateManager().getNodeFromCache(pId);
            		if (st != null){
            			ns.registerParent(st);
            		}
            	}
            	if (ns.getWeakParentState() != null && getParent().cachedPath != null){
            		Path pPatn = getParent().getPrimaryPath();
            		try {
						cachedPath = Path.create(pPatn, getName(), ((_NodeState)this).getIndex(), false);
					} catch (MalformedPathException e) {
                        throw new RepositoryException(e);
					}
            	} else {
	                String _path = JCRHelper.convertPath(((_NodeState) this).getInternalPath());
	                if (_path.equals("")){
	                    cachedPath = Path.ROOT;
	                } else {
	                    try {
	                        cachedPath = Path.create(_path, getNamespaceResolver(), true);
	                    } catch (MalformedPathException e) {
	                        throw new RepositoryException(e);
	                    }
	                }
            	}
            } else {
                Path parentPath = getParent().getPrimaryPath();
                try {
                    cachedPath = Path.create(parentPath, getName(), true);
                } catch (MalformedPathException e) {
                    throw new RepositoryException(e);
                }
            }
        }
        return cachedPath;
        
    }
	
    public abstract _NodeState getParent() throws UnsupportedRepositoryOperationException, RepositoryException;

    public abstract QName getName();

	public abstract boolean isNode();

	public String safeGetJCRPath() {
        try {
            return safeGetJCRPath(getPrimaryPath());
        } catch (Exception npde) {
            try {
                log.error("failed to convert " + getPrimaryPath().toString() + " to JCR path.");
                // return string representation of internal path as a fallback
                return getPrimaryPath().toString();
            } catch (Exception exc){
                return id.toString();
            }
        }
    }
    
    public String safeGetJCRPath(Path path) {
        try {
            return path.toJCRPath(getNamespaceResolver());
        } catch (Exception npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

	protected NamespaceResolver getNamespaceResolver() throws RepositoryException {
		if (namespaceRegistry == null){
			namespaceRegistry = new NamespaceRegistryImpl(repository, repository.getNamespaceRegistry());
		}
		return namespaceRegistry;
	}
	
	public NamespaceRegistryImpl getNamespaceRegistry() throws RepositoryException{
		return repository.getNamespaceRegistry();
	}

	int depthCache = -1;
	
	public int getDepth() throws RepositoryException {
		if (depthCache < 0){
	        //TODO what result will be after node move ??
	        if (isNode()){
	        	depthCache = ((_NodeState)this).getInternalDepth();
	        } else {
	        	depthCache = getParent().getInternalDepth() + 1;
	        }
		}
		return depthCache;
	}
	
	public ItemStatus getStatus(){
		return status;
	}

	public boolean isModified() {
		return status == ItemStatus.Destroyed || status == ItemStatus.Invalidated || status == ItemStatus.Modified
			|| status == ItemStatus.New;
	}

	public boolean isNew() {
		return status == ItemStatus.New;
	}

	public boolean isRemoved() {
		return status == ItemStatus.Destroyed || status == ItemStatus.Invalidated;
	}
	
    public String getStateString(){
    	return getStatus().name();
    }

	public _ItemId getId() {
		return id;
	}

	abstract public Collection<EventState> getEvents();

	public boolean isDestroyed() {
		
		return ItemStatus.Destroyed == status;
	}



}
