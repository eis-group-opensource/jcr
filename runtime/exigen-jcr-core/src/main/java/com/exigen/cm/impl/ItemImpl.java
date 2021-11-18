/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.state.ItemState;
import com.exigen.cm.impl.state2.FakeJCRTransaction;
import com.exigen.cm.impl.state2.IdIterator;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2.NodeStateIterator;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._ItemState;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.value.ValueFactoryImpl;

public abstract class ItemImpl  implements Item, ItemState{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(ItemImpl.class);
    protected Path cachedPath = null;
	private _ItemState itemState;
	protected RepositoryImpl repository;
	protected _AbstractsStateManager stateManager;

    public ItemImpl(_ItemState state, _AbstractsStateManager sm) {
    	this.repository = state.getRepository();
        this.itemState = state;
        this.stateManager = sm;
    }
    
    public _AbstractsStateManager getStateManager() {
		return stateManager;
	}


    
    public WorkspaceImpl _getWorkspace(){
        return _getSession()._getWorkspace();
    }
    
    protected abstract SessionImpl _getSession();

	public RepositoryImpl _getRepository(){
        return repository;
    }
     
    public String getPath() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        try {
            return getPrimaryPath().toJCRPath(_getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace";
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
    }

    public String getName() throws RepositoryException {
    	if (getDepth() == 0 && getPrimaryPath().denotesRoot()){
    		return "";
    	}
        try {
            return getQName().toJCRName(_getNamespaceResolver());
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e);
        }
    }

    
    protected NamespaceResolver _getNamespaceResolver() {
        return stateManager.getNamespaceResolver();
    }

    public int getDepth() throws RepositoryException {
    	return itemState.getDepth();
    }

    public Session getSession() throws RepositoryException {
        return _getSession();
    }


	public boolean isTransactionalNew() {
		try {
    		_NodeState state = null;
    		if (this instanceof _NodeImpl){
    			state = ((_NodeImpl)this).getNodeState();
    		} else {
    			state = ((_NodeImpl)((PropertyImpl) this).getParent()).getNodeState();
    		}
    		JCRTransaction createInTr = state.getCreateInTransaction();
    		if (createInTr != null){
    			JCRTransaction current = TransactionHelper.getCurrentTransaction();
    			if (current== null){    			    
    				current = new FakeJCRTransaction(getStateManager().getConnection(), state);
    			}
    			if (createInTr.equals(current)){
    				return true;
    			}
    		}
		} catch (RepositoryException exc){
			exc.printStackTrace();
		}
		return false;
	}

    

    public boolean isNew() {
    	if ( itemState.getStatus().equals(ItemStatus.New)) {
    		return true;
    	} else {
    		return isTransactionalNew();
    	}
    	
    }

    public boolean isModified() {
    	return itemState.getStatus().equals(ItemStatus.Modified);
    }
    

    /**
     * {@inheritDoc}
     */
    public boolean isSame(Item otherItem) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (this == otherItem) {
            return true;
        }
        if (otherItem instanceof ItemImpl) {
            ItemImpl other = (ItemImpl) otherItem;
            return other.getId().equals(getId());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void refresh(boolean keepChanges)
            throws InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();
        internalRefresh(keepChanges);
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void internalRefresh(boolean keepChanges)
            throws InvalidItemStateException, RepositoryException {

        if (keepChanges) {
            // todo FIXME should reset Item#status field to STATUS_NORMAL
            // * of all descendent non-transient instances; maybe also
            // * have to reset stale ItemState instances 
            //return;
            throw new RepositoryException("Unsupported operation");
        }

        //if (isNode()) {
        //    // check if this is the root node
        //    if (getDepth() == 0) {
        //        // optimization
        //        stateMgr.disposeAllTransientItemStates();
        //        return;
        //    }
        //}

        if (isNode()){
            //TODO update references
            NodeImpl node = (NodeImpl) this;
            List<_NodeState> childs = stateManager.getModifiedNodesDirect(node.getNodeState().getNodeId(), true);
            for(_NodeState c:childs){
            	NodeImpl nn = new NodeImpl(c, stateManager);
                nn.internalRefresh(keepChanges);
            }
            
            //refresh new and removed references
//          TODO optimize read Ahead
            if (node.getNodeState().isReferencesToLoaded()){
	            for(NodeReference nr:node.getNodeState().getReferencesTo()){
	                Long toId = nr.getToId();
	                if (nr.getState().equals(ItemStatus.New)){
	                    _NodeState to = stateManager.getNodeState(toId, null);
	                    to.unregisterTmpRefeference(nr);
	                    nr.setDeleted();
	                }
	                if (nr.getState().equals(ItemStatus.Invalidated)){
	                    _NodeState to = stateManager.getNodeState(toId, null);
	                    to.unregisterReferenceRemove(nr);
	                    nr.resetStateToNormal();
	                }
	            }
            }
            
            ItemStatus status = node.getNodeState().getStatus();
            if (status.equals(ItemStatus.Normal)){
            	node.reloadNode();
            } else if (status.equals(ItemStatus.New)){
                stateManager.unRegisterModifedState(node.getNodeState());
                stateManager.evictState(node.getNodeState());
                node.getNodeState().setStatusDestroyed();
            } else if (status.equals(ItemStatus.Modified)){ 
                node.getNodeState().setStatusNormal();
                stateManager.unRegisterModifedState(node.getNodeState());
                node.reloadNode();
            } else if (status.equals(ItemStatus.Invalidated)){
                node.getNodeState().setStatusNormal();
                stateManager.unRegisterModifedState(node.getNodeState());
                node.reloadNode();
            } else {
                    throw new RepositoryException("Unsupported operation for status "+status);
            }
        } else {
            throw new RepositoryException("Unsupported operation");
        }

    }


    /**
     * {@inheritDoc}
     */
    public void remove()
            throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        internalRemove(false, false);
    }
    
    protected void sanityCheck() throws RepositoryException {
        // check session status
    	if (!isNode() && getParentNodeId() != null){
    		_getParent().sanityCheck();
    	}

        // check status of this item for read operation
        if (itemState.getStatus().equals(ItemStatus.Destroyed) || 
        		itemState.getStatus().equals(ItemStatus.Invalidated)) {
            throw new InvalidItemStateException(safeGetJCRPath() + ": the item does not exist anymore");
        }
    }
    
    public Path getPrimaryPath() throws RepositoryException {
    	return itemState.getPrimaryPath();

        
    }

    abstract public _NodeImpl _getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException ;

    abstract public int getIndex() throws RepositoryException ;

    abstract protected QName getQName();

    
    
    
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    	save(true);
    }

    public void save(boolean validate) throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    	save(validate, true, true);
    }
    

    public void save(boolean validate, boolean allowSaveVersionHistory, boolean incVersion) throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        if (isNode() && ((_NodeImpl)this).getNodeState().getStatus() == ItemStatus.New) {
            throw new RepositoryException("Can't call save on new node");
        }

        if (!isNode()) {
            throw new RepositoryException("Can't call save on single property");
        }
    	stateManager.save(((_NodeState)getItemState()).getNodeId(), validate, allowSaveVersionHistory, incVersion);
    }

/*    abstract protected void _afterSave(DatabaseConnection conn, ChangeLog changeLog) throws RepositoryException;

    abstract protected ArrayList<DatabaseStatement> _save(DatabaseConnection conn, ArrayList<CacheKey> evictList, ArrayList<DatabaseStatement> lastDelete, ChangeLog changeLog) throws RepositoryException;
    abstract protected ArrayList<DatabaseStatement> _save2(DatabaseConnection conn, ArrayList<CacheKey> evictList, ArrayList<DatabaseStatement> lastDelete, ChangeLog changeLog) throws RepositoryException;
  */  
    

    protected NodeManager getNodeManager() {
        return _getSession().getNodeManager();
    }
    
    protected DatabaseConnection getConnection() throws RepositoryException {
        return stateManager.getConnection();
    }

    protected NamespaceRegistryImpl _getNamespaceRegistry() {
        return _getSession()._getWorkspace()._getNamespaceRegistry();
    }

    
    /**
     * Failsafe mapping of internal <code>id</code> to JCR path for use in
     * diagnostic output, error messages etc.
     *
     * @return JCR path or some fallback value
     */
    @Deprecated
    public String safeGetJCRPath(Path path) {
        try {
            return path.toJCRPath(_getNamespaceResolver());
        } catch (Exception npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }
    
    @Deprecated
    public String safeGetJCRPath() {
        try {
            return safeGetJCRPath(getPrimaryPath());
        } catch (Exception npde) {
            try {
                log.error("failed to convert " + getPrimaryPath().toString() + " to JCR path.");
                // return string representation of internal path as a fallback
                return getPrimaryPath().toString();
            } catch (Exception exc){
                ///return id.toString();
            	//FIXME
            	throw new UnsupportedOperationException();
            }
        }
    }

    public final ItemId getId(){
        return getItemId();
    }

    abstract protected ItemId getItemId() ;

	protected NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException {
        return stateManager.getNodeTypeManager();
    }
    
   
    public ValueFactoryImpl getValueFactory(){
        return _getSession()._getValueFactory();
    }
    
    
    /**
     * Same as <code>{@link Item#remove()}</code> except for the
     * <code>noChecks</code> parameter.
     *
     * @param noChecks
     * @throws VersionException
     * @throws LockException
     * @throws RepositoryException
     */
    public void internalRemove(boolean noChecks, boolean skipSNSIpdate)
            throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        Path.PathElement thisName = getPrimaryPath().getNameElement();

        // check if protected
        if (isNode()) {
            _NodeImpl node = (_NodeImpl) this;
            // check if this is the root node
            if (node.getDepth() == 0) {
                String msg = safeGetJCRPath() + ": cannot remove root node";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            NodeDefinition def = node.getDefinition();
            // check protected flag
            if (!noChecks && def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected node";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        } else {
            PropertyImpl prop = (PropertyImpl) this;
             PropertyDefinition def = prop.getDefinition();
            // check protected flag
            if (!noChecks && def.isProtected()) {
                String msg = safeGetJCRPath() + ": cannot remove a protected property";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }

        _NodeImpl parentNode = (_NodeImpl) _getParent();

        // verify that parent node is checked-out
        if (!noChecks && !parentNode.internalIsCheckedOut(false)) {
            String msg = parentNode.safeGetJCRPath() + ": cannot remove a child of a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check protected flag of parent node
        if (!noChecks && parentNode.getDefinition().isProtected()) {
            String msg = parentNode.safeGetJCRPath() + ": cannot remove a child of a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        if (!noChecks) {
            parentNode.checkLock();
            if (this instanceof _NodeImpl) {
            	((_NodeImpl)this).checkLock();
            	((_NodeImpl)this).checkDeepLock();
            }
        }

        // delegate the removal of the child item to the parent node
        if (isNode()) {
        	//preload all childs
			//preload all direct children
			IdIterator childs = getStateManager().getChildNodesId((_NodeState)getItemState(),false, null);
			NodeStateIterator si = new NodeStateIterator(childs, getStateManager(), null);
			for(_NodeState child:si){
				child.getSecurityId();
			}

        	
            parentNode.removeChildNode(thisName.getName(), thisName.getIndex(),skipSNSIpdate, !noChecks);
        } else {
            parentNode.removeChildProperty(thisName.getName(), true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public abstract void accept(ItemVisitor visitor)
            throws RepositoryException;
    
    /**
     * {@inheritDoc}
     */
    public Item getAncestor(int degree)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (degree == 0) {
            return _getSession().getRootNode();
        }

        try {
            // Path.getAncestor requires relative degree, i.e. we need
            // to convert absolute to relative ancestor degree
            Path path = getPrimaryPath();
            int relDegree = path.getAncestorCount() - degree;
            if (relDegree < 0) {
                throw new ItemNotFoundException();
            }
            Path ancestorPath = path.getAncestor(relDegree);
            return _getSession().getItem(ancestorPath);
        } catch (PathNotFoundException pnfe) {
            throw new ItemNotFoundException();
        }
    }  
    
    public String getParentUUID() throws AccessDeniedException, RepositoryException {
        /*try {
            NodeImpl p = _getParent();
            if (p != null){
                if (p.internalUUID != null){
                    return p.internalUUID;
                }
                if (p._isNodeType(QName.MIX_REFERENCEABLE)) {
                    p.internalUUID = p._getProperty(QName.JCR_UUID).getString();
                }
                return p.internalUUID;
                
            }
        } catch (ItemNotFoundException e){
            
        }
        return null;*/
    	throw new UnsupportedOperationException();

    }
    
    
    public _ItemState getItemState(){
    	return itemState;
    }	
	

}


/*
 * $Log: ItemImpl.java,v $
 * Revision 1.13  2009/02/25 09:04:21  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.12  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2009/01/09 12:39:16  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/07/22 09:28:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/07/22 09:06:26  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/07/01 11:10:34  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/13 09:35:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/01/30 14:58:08  dparhomenko
 * make lazy loading for referencing information
 *
 * Revision 1.5  2008/01/03 11:56:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2007/11/30 07:47:47  dparhomenko
 * Fix lock problem
 *
 * Revision 1.3  2007/10/12 10:40:52  dparhomenko
 * Fix restore issues
 *
 * Revision 1.2  2007/07/10 10:17:41  dparhomenko
 * PTR#0152003 fix insert statemnt for oracle blobs
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.22  2007/03/02 09:31:59  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.21  2006/11/14 07:37:19  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.20  2006/11/03 13:09:13  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.19  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.18  2006/10/11 13:08:55  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.17  2006/10/09 11:22:50  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.16  2006/10/03 11:20:10  dparhomenko
 * PTR#0148480 fin indexing
 *
 * Revision 1.15  2006/10/02 15:07:09  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.14  2006/09/26 10:11:07  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.13  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.12  2006/08/15 13:33:29  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.11  2006/08/07 14:25:55  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.10  2006/07/10 12:06:01  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.9  2006/07/03 09:25:16  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.8  2006/06/27 11:51:05  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 * Revision 1.7  2006/06/22 12:00:25  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.6  2006/06/09 08:55:40  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.5  2006/05/22 14:48:02  dparhomenko
 * PTR#1801941 add observationsupport
 *
 * Revision 1.4  2006/04/28 14:59:42  dparhomenko
 * PTR#0144983 fix remove nodes with references
 *
 * Revision 1.3  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.2  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.7  2006/04/13 10:03:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/04/06 09:16:21  dparhomenko
 * PTR#0144983 MS FTS
 *
 * Revision 1.5  2006/04/05 12:48:07  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.4  2006/04/04 11:46:08  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.3  2006/04/03 13:08:20  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.2  2006/03/29 12:56:19  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.14  2006/03/27 14:27:22  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.13  2006/03/23 14:26:49  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.12  2006/03/17 10:12:38  dparhomenko
 * PTR#0144983 add support for indexable_data
 *
 * Revision 1.11  2006/03/16 13:13:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.10  2006/03/14 14:54:07  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.9  2006/03/14 11:55:40  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.8  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.7  2006/02/27 15:53:55  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.6  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.5  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/17 13:03:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/16 15:47:59  dparhomenko
 * PTR#0144983 restructurize
 *
 * Revision 1.2  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/13 12:40:40  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */