/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;



import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SessionSecurityManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.core.util.ReferenceChangeTracker;
import com.exigen.cm.jackrabbit.lock.LockManager;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.version.NodeStateEx;
import com.exigen.cm.jackrabbit.version.VersionHistoryImpl;
import com.exigen.cm.jackrabbit.version.VersionManager;

/**
 * <code>BatchedItemOperations</code> is an <i>internal</i> helper class that
 * provides both high- and low-level operations directly on the
 * <code>ItemState</code> level.
 */
public class BatchedItemOperations extends ItemValidator {

    private static Log log = LogFactory.getLog(BatchedItemOperations.class);

    // flags used by the copy(...) methods
    protected static final int COPY = 0;
    protected static final int CLONE = 1;
    protected static final int CLONE_REMOVE_EXISTING = 2;

    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check access rights
     */
    public static final int CHECK_ACCESS = 1;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check lock status
     */
    public static final int CHECK_LOCK = 2;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check checked-out status
     */
    public static final int CHECK_VERSIONING = 4;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check constraints defined in node type
     */
    public static final int CHECK_CONSTRAINTS = 16;
    /**
     * option for <code>{@link #checkRemoveNode}</code> method:<p/>
     * check that target node is not being referenced
     */
    public static final int CHECK_REFERENCES = 8;

    /**
     * wrapped item state manager
     */
    //protected final UpdatableItemStateManager stateMgr;
    /**
     * lock manager used for checking locking status
     */
    protected final LockManager lockMgr;
    /**
     * current session used for checking access rights and locking status
     */
    protected final SessionImpl session;

    private RepositoryImpl repository;

    private DatabaseConnection connection;

    private WorkspaceImpl _workspace;
    
    private ItemStateManager stateMgr;

    /**
     * Creates a new <code>BatchedItemOperations</code> instance.
     *
     * @param stateMgr   item state manager
     * @param ntReg      node type registry
     * @param lockMgr    lock manager
     * @param session    current session
     * @param hierMgr    hierarchy manager
     * @param nsResolver namespace resolver
     * @throws RepositoryException 
     */
    public BatchedItemOperations(RepositoryImpl repository,
                                WorkspaceImpl _workspace,
                                 NodeTypeRegistry ntReg,
                                 LockManager lockMgr,
                                 SessionImpl session,
                                 NamespaceResolver nsResolver) throws RepositoryException {
        //UpdatableItemStateManager stateMgr,
        //HierarchyManager hierMgr,
        super(ntReg, nsResolver);
        this._workspace = _workspace;
        this.repository = repository;
        //this.stateMgr = stateMgr;
        this.lockMgr = lockMgr;
        this.session = session;
        this.stateMgr = new ItemStateManager(_workspace);
    }

    //-----------------------------------------< controlling batch operations >
    /**
     * Starts an edit operation on the wrapped state manager.
     * At the end of this operation, either {@link #update} or {@link #cancel}
     * must be invoked.
     *
     * @throws IllegalStateException if the state mananger is already in edit mode
     * @throws RepositoryException 
     */
    public void edit() throws IllegalStateException, RepositoryException {
        //stateMgr.edit();
        this.connection = repository.getConnectionProvider().createConnection();
        //throw new UnsupportedOperationException();
    }

    /**
     * Store an item state.
     *
     * @param state item state that should be stored
     * @throws IllegalStateException if the manager is not in edit mode.
     */
    /*public void store(ItemState state) throws IllegalStateException {
        //stateMgr.store(state);
        throw new UnsupportedOperationException();

    }*/

    /**
     * Destroy an item state.
     *
     * @param state item state that should be destroyed
     * @throws IllegalStateException if the manager is not in edit mode.
     */
    /*public void destroy(ItemState state) throws IllegalStateException {
        //stateMgr.destroy(state);
        throw new UnsupportedOperationException();

    }*/

    /**
     * End an update operation. This will save all changes made since
     * the last invokation of {@link #edit()}. If this operation fails,
     * no item will have been saved.
     *
     * @throws RepositoryException   if the update operation failed
     * @throws IllegalStateException if the state mananger is not in edit mode
     */
    public void update() throws RepositoryException, IllegalStateException {
        //try {
            //stateMgr.update();
        this.connection.commit();
        connection.close();
        connection = null;
        /*} catch (ItemStateException ise) {
            String msg = "update operation failed";
            log.debug(msg, ise);
            throw new RepositoryException(msg, ise);
        }*/
    }

    /**
     * Cancel an update operation. This will undo all changes made since
     * the last invokation of {@link #edit()}.
     *
     * @throws IllegalStateException if the state mananger is not in edit mode
     * @throws RepositoryException 
     */
    public void cancel() throws IllegalStateException, RepositoryException {
        //stateMgr.cancel();
        connection.rollback();
        connection.close();
        connection = null;

    }

    //-------------------------------------------< high-level item operations >
    /**
     * Copies the tree at <code>srcPath</code> to the new location at
     * <code>destPath</code>.
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param srcPath
     * @param destPath
     * @param flag     one of
     *                 <ul>
     *                 <li><code>COPY</code></li>
     *                 <li><code>CLONE</code></li>
     *                 <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                 </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     */
    /*public void copy(Path srcPath, Path destPath, int flag)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        copy(srcPath, stateMgr, hierMgr, session.getAccessManager(), destPath, flag);
    }*/

    /**
     * Copies the tree at <code>srcPath</code> retrieved using the specified
     * <code>srcStateMgr</code> to the new location at <code>destPath</code>.
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param srcPath
     * @param srcStateMgr
     * @param srcHierMgr
     * @param srcAccessMgr
     * @param destPath
     * @param flag         one of
     *                     <ul>
     *                     <li><code>COPY</code></li>
     *                     <li><code>CLONE</code></li>
     *                     <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                     </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     * @throws IllegalStateException        if the state mananger is not in edit mode
     */
    public void copy(Path srcPath,
                     ItemStateManager srcStateMgr,
                     AccessManager srcAccessMgr,
                     Path destPath,
                     int flag)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException, IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException("not in edit mode");
        }

        // 1. check paths & retrieve state

        NodeStateEx srcState = getNodeState(srcPath, srcStateMgr);

        Path.PathElement destName = destPath.getNameElement();
        Path destParentPath = destPath.getAncestor(1);
        NodeStateEx destParentState = getNodeState(destParentPath, stateMgr);
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = "invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // 2. check access rights, lock status, node type constraints, etc.

        checkAddNode(destParentState, destName.getName(),
                srcState.getNodeState().getPrimaryTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_VERSIONING | CHECK_CONSTRAINTS);
        // check read access right on source node using source access manager
        try {
            if (!srcAccessMgr.isGranted(srcState.getNodeId(), SecurityPermission.READ)) {
                throw new PathNotFoundException(safeGetJCRPath(srcPath));
            }
        } catch (RepositoryException infe) {
            String msg = "internal error: failed to check access rights for "
                    + safeGetJCRPath(srcPath);
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }

        // 3. do copy operation (modify and store affected states)

        ReferenceChangeTracker refTracker = new ReferenceChangeTracker();

        
		//_NodeState.dumpNode(destParentState.getNodeState(), 0, false);
        // create deep copy of source node state
        NodeStateEx newState = copyNodeState(destName.getName(), srcState, srcStateMgr, srcAccessMgr,
                destParentState, flag, refTracker);

        //_NodeState.dumpNode(destParentState.getNodeState(), 0, false);
        // add to new parent
        //destParentState.addChildNodeEntry(newState);

        // change definition (id) of new node
//        NodeDef newNodeDef =
                findApplicableNodeDefinition(destName.getName(),
                        srcState.getNodeState().getPrimaryTypeName(), destParentState);
        //newState.setDefinitionId(newNodeDef.getId());

        // adjust references that refer to uuid's which have been mapped to
        // newly generated uuid's on copy/clone
        Iterator iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            _PropertyState prop = (_PropertyState) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE && prop.getType() != PropertyType283.WEAKREFERENCE ) {
                continue;
            }
            boolean modified = false;
            InternalValue[] values = prop.getValues();
            InternalValue[] newVals = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                //TODO check uuid work
                UUID original = (UUID) val.internalValue();
                String adjusted = refTracker.getMappedUUID(original.toString());
                if (adjusted != null) {
                    newVals[i] = InternalValue.create(UUID.fromString(adjusted), prop.getRequiredType() == PropertyType283.WEAKREFERENCE);
                    modified = true;
                } else {
                    // reference doesn't need adjusting, just copy old value
                    newVals[i] = val;
                }
                //throw new UnsupportedOperationException();
            }
            if (modified) {
                prop.setValues(newVals);
                //stateMgr.store(prop);
            }
            
            //register references innodestate
            _NodeState parent = prop.getParent();
            for(InternalValue v:newVals){
            	UUID uuid = (UUID) v.internalValue();
            	try {
            		_NodeState n = (_NodeState) parent.getStateManager().getItemByUUID(uuid.toString(), false);
                	parent.registerReference(prop, v, n);
            	} catch (ItemNotFoundException exc){
            		boolean weak = prop.getDefinition().getRequiredType() == PropertyType283.WEAKREFERENCE;
            		if (weak){
            			continue;
            		} else {
            			throw exc;
            		}
            	}
            	
            }
        }
        refTracker.clear();

        // store states
        stateMgr.store(newState);
        stateMgr.store(destParentState);
    }

    /**
     * Moves the tree at <code>srcPath</code> to the new location at
     * <code>destPath</code>.
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param srcPath
     * @param destPath
     * @throws ConstraintViolationException
     * @throws VersionException
     * @throws AccessDeniedException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     * @throws IllegalStateException        if the state mananger is not in edit mode
     */
    public void move(Path srcPath, Path destPath, boolean moveSecurity)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException, IllegalStateException {

        // check precondition
        //TODO uncomment me
        /*if (!stateMgr.inEditMode()) {
            throw new IllegalStateException("not in edit mode");
        }*/

        // 1. check paths & retrieve state

        try {
            if (srcPath.isAncestorOf(destPath)) {
                String msg = safeGetJCRPath(destPath)
                        + ": invalid destination path (cannot be descendant of source path)";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + safeGetJCRPath(destPath);
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        Path.PathElement srcName = srcPath.getNameElement();
        Path srcParentPath = srcPath.getAncestor(1);
        NodeStateEx target = getNodeState(srcPath, stateMgr);
        NodeStateEx srcParent = getNodeState(srcParentPath, stateMgr);

        Path.PathElement destName = destPath.getNameElement();
        Path destParentPath = destPath.getAncestor(1);
        NodeStateEx destParent = getNodeState(destParentPath, stateMgr);

        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = safeGetJCRPath(destPath)
                    + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // 2. check if target state can be removed from old/added to new parent

        checkRemoveNode(target, srcParent.getNodeId(),
                CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING | CHECK_CONSTRAINTS);
        
        checkAddNode(destParent, destName.getName(),
                target.getNodeState().getPrimaryTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_VERSIONING | CHECK_CONSTRAINTS);

        
        // 3. do move operation (modify and store affected states)
        boolean renameOnly = srcParent.getNodeId().equals(destParent.getNodeId());

        int srcNameIndex = srcName.getIndex();
        if (srcNameIndex == 0) {
            srcNameIndex = 1;
        }

        if (renameOnly) {
            // change child node entry
            destParent.renameChildNodeEntry(srcName.getName(), srcNameIndex,
                    destName.getName());
        } else {
            
            _NodeState nodeState = srcParent.deassociateChild(srcName.getName(), srcNameIndex, true);
            destParent.associateChild(nodeState,destName.getName(), moveSecurity);
            // remove child node entry from old parent
            //srcParent.removeChildNodeEntry(srcName.getName(), srcNameIndex);
            // re-parent target node
            //target.setParentId(destParent.getNodeId());
            // add child node entry to new parent
            //destParent.addChildNodeEntry(destName.getName(), target.getNodeId());
            //throw new UnsupportedOperationException();
        }

        // change definition (id) of target node
//        NodeDef newTargetDef =
//                findApplicableNodeDefinition(destName.getName(),
//                        target.getNodeTypeName(), destParent);
//        target.setDefinitionId(newTargetDef.getId());

        // store states
        //stateMgr.store(target);
        //if (renameOnly) {
        //    stateMgr.store(srcParent);
        //} else {
        //    stateMgr.store(destParent);
        //    stateMgr.store(srcParent);
        //}
        //throw new UnsupportedOperationException();
        destParent.save();
        srcParent.save();
    }

    /**
     * Removes the specified node, recursively removing its properties and
     * child nodes.
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param nodePath
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     * @throws IllegalStateException
     */
    /*public void removeNode(Path nodePath)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException,
            IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException("not in edit mode");
        }

        // 1. retrieve affected state
        NodeState target = getNodeState(nodePath);
        NodeId parentId = target.getParentId();

        // 2. check if target state can be removed from parent
        checkRemoveNode(target, parentId,
                CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING
                | CHECK_CONSTRAINTS | CHECK_REFERENCES);

        // 3. do remove operation
        removeNodeState(target);
    }*/

    //--------------------------------------< misc. high-level helper methods >
    /**
     * Checks if adding a child node called <code>nodeName</code> of node type
     * <code>nodeTypeName</code> to the given parent node is allowed in the
     * current context.
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param options      bit-wise OR'ed flags specifying the checks that should be
     *                     performed; any combination of the following constants:
     *                     <ul>
     *                     <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                     current session is granted read & write access on
     *                     parent node</li>
     *                     <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                     there's no foreign lock on parent node</li>
     *                     <li><code>{@link #CHECK_VERSIONING}</code>: make sure
     *                     parent node is checked-out</li>
     *                     <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                     make sure no node type constraints would be violated</li>
     *                     <li><code>{@link #CHECK_REFERENCES}</code></li>
     *                     </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    public void checkAddNode(NodeStateEx parentState, QName nodeName,
                             QName nodeTypeName, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ItemExistsException, RepositoryException {

        Path parentPath = parentState.getPrimaryPath();

        // 1. locking status

        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            // make sure there's no foreign lock on parent node
            verifyUnlocked(parentState.getNodeId());
        }
        
        // 2. versioning status

        if ((options & CHECK_VERSIONING) == CHECK_VERSIONING) {
            // make sure parent node is checked-out
            verifyCheckedOut(parentPath);
        }
        
        // 3. access rights

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            //AccessManager accessMgr = session.getAccessManager();
            // make sure current session is granted read access on parent node
            //if (!accessMgr.isGranted(parentState.getNodeId(), AccessManager.READ)) {
            //    throw new ItemNotFoundException(safeGetJCRPath(parentState.getNodeId()));
            //}
            // make sure current session is granted write access on parent node
            //if (!accessMgr.isGranted(parentState.getNodeId(), AccessManager.WRITE)) {
            //    throw new AccessDeniedException(safeGetJCRPath(parentState.getNodeId())
            //            + ": not allowed to add child node");
            //}
            
            SessionSecurityManager sm = ((SessionImpl)_workspace.getSession()).getSecurityManager();
            try {
                sm.checkPermission(parentState.getNodeId(), SecurityPermission.BROWSE.getPermissionName());
                sm.checkPermission(parentState.getNodeId(), SecurityPermission.ADD_NODE.getPermissionName());
            } catch (AccessControlException exc) {
                throw new AccessDeniedException(safeGetJCRPath(parentState)
                        + ": not allowed to add child node");

            }
            
        }

        // 4. node type constraints

        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            NodeDefinition parentDef = parentState.getDefinition();
            // make sure parent node is not protected
            if (parentDef.isProtected()) {
                throw new ConstraintViolationException(safeGetJCRPath(parentState.getNodeId())
                        + ": cannot add child node to protected parent node");
            }
            // make sure there's an applicable definition for new child node
            EffectiveNodeType entParent = parentState.getNodeState().getEffectiveNodeType();
            entParent.checkAddNodeConstraints(nodeName, nodeTypeName, ((NodeTypeManagerImpl)_workspace.getNodeTypeManager()).getNodeTypeRegistry());
            NodeDef newNodeDef =
                    findApplicableNodeDefinition(nodeName, nodeTypeName,
                            parentState);

            // check for name collisions
            if (parentState.hasProperty(nodeName)) {
                // there's already a property with that name
                throw new ItemExistsException("cannot add child node '"
                        + nodeName.getLocalName() + "' to "
                        + safeGetJCRPath(parentState.getNodeId())
                        + ": colliding with same-named existing property");
            }
            if (parentState.hasNode(nodeName)) {
                // there's already a node with that name...

                // get definition of existing conflicting node
                NodeStateEx  entry = (NodeStateEx) parentState.getNode(nodeName, 1, false);
                //NodeStateEx conflictingState;
                //Long conflictingId = entry.getNodeId();
                //try {
                //    conflictingState = (NodeStateEx) stateMgr.getItemState(conflictingId);
                //} catch (ItemStateException ise) {
                //    String msg = "internal error: failed to retrieve state of "
                //            + safeGetJCRPath(conflictingId);
                //    log.debug(msg);
                //    throw new RepositoryException(msg, ise);
                //}
                NodeDefinition conflictingTargetDef = entry.getDefinition();
                // check same-name sibling setting of both target and existing node
                if (!conflictingTargetDef.allowsSameNameSiblings()
                        || !newNodeDef.allowsSameNameSiblings()) {
                    throw new ItemExistsException("cannot add child node '"
                            + nodeName.getLocalName() + "' to "
                            + safeGetJCRPath(parentState.getNodeId())
                            + ": colliding with same-named existing node");
                }
            }
        }
    }

    /**
     * Checks if removing the given target node is allowed in the current context.
     *
     * @param targetState
     * @param options     bit-wise OR'ed flags specifying the checks that should be
     *                    performed; any combination of the following constants:
     *                    <ul>
     *                    <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                    current session is granted read access on parent
     *                    and remove privilege on target node</li>
     *                    <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                    there's no foreign lock on parent node</li>
     *                    <li><code>{@link #CHECK_VERSIONING}</code>: make sure
     *                    parent node is checked-out</li>
     *                    <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                    make sure no node type constraints would be violated</li>
     *                    <li><code>{@link #CHECK_REFERENCES}</code>:
     *                    make sure no references exist on target node</li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeStateEx targetState, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {
        //checkRemoveNode(targetState, targetState.getParentIdLong(), options);
        throw new UnsupportedOperationException();

    }

    /**
     * Checks if removing the given target node from the specifed parent
     * is allowed in the current context.
     *
     * @param targetState
     * @param parentId
     * @param options     bit-wise OR'ed flags specifying the checks that should be
     *                    performed; any combination of the following constants:
     *                    <ul>
     *                    <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                    current session is granted read access on parent
     *                    and remove privilege on target node</li>
     *                    <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                    there's no foreign lock on parent node</li>
     *                    <li><code>{@link #CHECK_VERSIONING}</code>: make sure
     *                    parent node is checked-out</li>
     *                    <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                    make sure no node type constraints would be violated</li>
     *                    <li><code>{@link #CHECK_REFERENCES}</code>:
     *                    make sure no references exist on target node</li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeStateEx targetState, Long parentId,
                                int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {

        if (targetState.getParentId() == null) {
            // root or orphaned node
            throw new ConstraintViolationException("cannot remove root node");
        }
        Long targetId = targetState.getNodeId();
        NodeStateEx parentState = getNodeState(parentId);
        //Path parentPath = hierMgr.getPath(parentId);

        // 1. locking status

        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            // make sure there's no foreign lock on parent node
            verifyUnlocked(parentId);
        }

        // 2. versioning status

        if ((options & CHECK_VERSIONING) == CHECK_VERSIONING) {
            // make sure parent node is checked-out
            verifyCheckedOut(parentState.getPrimaryPath());
        }

        // 3. access rights

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            SessionSecurityManager sm;
            try {
                // make sure current session is granted read access on parent node
                sm = ((SessionImpl)_workspace.getSession()).getSecurityManager();
                sm.checkPermission(targetId, SecurityPermission.READ.getPermissionName());
                sm.checkPermission(targetId, SecurityPermission.REMOVE.getPermissionName());
                //if (!accessMgr.isGranted(targetId, AccessManager.READ)) {
                //    throw new PathNotFoundException(safeGetJCRPath(targetId));
               // }
                // make sure current session is allowed to remove target node
                //if (!accessMgr.isGranted(targetId, AccessManager.REMOVE)) {
                //    throw new AccessDeniedException(safeGetJCRPath(targetId)
                //            + ": not allowed to remove node");
                //}
            } catch (ItemNotFoundException infe) {
                String msg = "internal error: failed to check access rights for "
                        + safeGetJCRPath(targetId);
                log.debug(msg);
                throw new RepositoryException(msg, infe);
            }
        }

        // 4. node type constraints

        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            NodeDefinition parentDef = parentState.getDefinition();
            if (parentDef.isProtected()) {
                throw new ConstraintViolationException(safeGetJCRPath(parentId)
                        + ": cannot remove child node of protected parent node");
            }
            NodeDefinition targetDef = targetState.getDefinition();
            if (targetDef.isMandatory()) {
                throw new ConstraintViolationException(safeGetJCRPath(targetId)
                        + ": cannot remove mandatory node");
            }
            if (targetDef.isProtected()) {
                throw new ConstraintViolationException(safeGetJCRPath(targetId)
                        + ": cannot remove protected node");
            }
        }

        // 5. referential integrity

        //TODO uncomment me
        //if ((options & CHECK_REFERENCES) == CHECK_REFERENCES) {
        //    EffectiveNodeType ent = targetState.getEffectiveNodeType();
        //    if (ent.includesNodeType(QName.MIX_REFERENCEABLE)) {
        //        NodeReferencesId refsId = new NodeReferencesId(targetState.getNodeId());
        //        if (stateMgr.hasNodeReferences(refsId)) {
        //            try {
        //                NodeReferences refs = stateMgr.getNodeReferences(refsId);
        //                if (refs.hasReferences()) {
        //                    throw new ReferentialIntegrityException(safeGetJCRPath(targetId)
        //                            + ": cannot remove node with references");
        //                }
        //            } catch (ItemStateException ise) {
        //                String msg = "internal error: failed to check references on "
        //                        + safeGetJCRPath(targetId);
        //                log.error(msg, ise);
        //                throw new RepositoryException(msg, ise);
        //            }
        //        }
        //    }
        //}
    }

    /**
     * Verifies that the node at <code>nodePath</code> is writable. The
     * following conditions must hold true:
     * <ul>
     * <li>the node must exist</li>
     * <li>the current session must be granted read & write access on it</li>
     * <li>the node must not be locked by another session</li>
     * <li>the node must not be checked-in</li>
     * <li>the node must not be protected</li>
     * </ul>
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException        if no node exists at
     *                                      <code>nodePath</code> of the current
     *                                      session is not granted read access
     *                                      to the specified path
     * @throws AccessDeniedException        if write access to the specified
     *                                      path is not allowed
     * @throws ConstraintViolationException if the node at <code>nodePath</code>
     *                                      is protected
     * @throws VersionException             if the node at <code>nodePath</code>
     *                                      is checked-in
     * @throws LockException                if the node at <code>nodePath</code>
     *                                      is locked by another session
     * @throws RepositoryException          if another error occurs
     */
    /*public void verifyCanWrite(Path nodePath)
            throws PathNotFoundException, AccessDeniedException,
            ConstraintViolationException, VersionException, LockException,
            RepositoryException {

        NodeState node = getNodeState(nodePath);

        // access rights
        AccessManager accessMgr = session.getAccessManager();
        // make sure current session is granted read access on node
        if (!accessMgr.isGranted(node.getNodeId(), AccessManager.READ)) {
            throw new PathNotFoundException(safeGetJCRPath(node.getNodeId()));
        }
        // make sure current session is granted write access on node
        if (!accessMgr.isGranted(node.getNodeId(), AccessManager.WRITE)) {
            throw new AccessDeniedException(safeGetJCRPath(node.getNodeId())
                    + ": not allowed to modify node");
        }

        // locking status
        verifyUnlocked(nodePath);

        // node type constraints
        verifyNotProtected(nodePath);

        // versioning status
        verifyCheckedOut(nodePath);
    }*/

    /**
     * Verifies that the node at <code>nodePath</code> can be read. The
     * following conditions must hold true:
     * <ul>
     * <li>the node must exist</li>
     * <li>the current session must be granted read access on it</li>
     * </ul>
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException if no node exists at
     *                               <code>nodePath</code> of the current
     *                               session is not granted read access
     *                               to the specified path
     * @throws RepositoryException   if another error occurs
     */
    /*public void verifyCanRead(Path nodePath)
            throws PathNotFoundException, RepositoryException {
        NodeState node = getNodeState(nodePath);

        // access rights
        AccessManager accessMgr = session.getAccessManager();
        // make sure current session is granted read access on node
        if (!accessMgr.isGranted(node.getNodeId(), AccessManager.READ)) {
            throw new PathNotFoundException(safeGetJCRPath(node.getNodeId()));
        }
    }*/

    /**
     * Helper method that finds the applicable definition for a child node with
     * the given name and node type in the parent node's node type and
     * mixin types.
     *
     * @param name
     * @param nodeTypeName
     * @param parentState
     * @return a <code>NodeDef</code>
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public NodeDef findApplicableNodeDefinition(QName name,
                                                QName nodeTypeName,
                                                NodeStateEx parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = parentState.getNodeState().getEffectiveNodeType();
        return entParent.getApplicableChildNodeDef(name, nodeTypeName, ((NodeTypeManagerImpl)_workspace.getNodeTypeManager()).getNodeTypeRegistry());
    }

    /**
     * Helper method that finds the applicable definition for a property with
     * the given name, type and multiValued characteristic in the parent node's
     * node type and mixin types. If there more than one applicable definitions
     * then the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * </ul>
     *
     * @param name
     * @param type
     * @param multiValued
     * @param parentState
     * @return a <code>PropDef</code>
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    /*public PropDef findApplicablePropertyDefinition(QName name,
                                                    int type,
                                                    boolean multiValued,
                                                    NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDef(name, type, multiValued);
    }*/

    /**
     * Helper method that finds the applicable definition for a property with
     * the given name, type in the parent node's node type and mixin types.
     * Other than <code>{@link #findApplicablePropertyDefinition(QName, int, boolean, NodeState)}</code>
     * this method does not take the multiValued flag into account in the
     * selection algorithm. If there more than one applicable definitions then
     * the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * <li>single-value definitions are preferred to multiple-value definitions</li>
     * </ul>
     *
     * @param name
     * @param type
     * @param parentState
     * @return a <code>PropDef</code>
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    /*public PropDef findApplicablePropertyDefinition(QName name,
                                                    int type,
                                                    NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDef(name, type);
    }*/

    //--------------------------------------------< low-level item operations >
    /**
     * Creates a new node.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param nodeName
     * @param nodeTypeName
     * @param mixinNames
     * @param id
     * @return
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws IllegalStateException        if the state mananger is not in edit mode
     */
    /*public NodeState createNodeState(NodeState parent,
                                     QName nodeName,
                                     QName nodeTypeName,
                                     QName[] mixinNames,
                                     NodeId id)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException, IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException("not in edit mode");
        }

        NodeDef def = findApplicableNodeDefinition(nodeName, nodeTypeName, parent);
        return createNodeState(parent, nodeName, nodeTypeName, mixinNames, id, def);
    }*/

    /**
     * Creates a new node based on the given definition.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param nodeName
     * @param nodeTypeName
     * @param mixinNames
     * @param id
     * @param def
     * @return
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws IllegalStateException
     */
    /*public NodeState createNodeState(NodeState parent,
                                     QName nodeName,
                                     QName nodeTypeName,
                                     QName[] mixinNames,
                                     NodeId id,
                                     NodeDef def)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException, IllegalStateException {

        // check for name collisions with existing properties
        if (parent.hasPropertyName(nodeName)) {
            String msg = "there's already a property with name " + nodeName;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // check for name collisions with existing nodes
        if (!def.allowsSameNameSiblings() && parent.hasChildNodeEntry(nodeName)) {
            NodeId errorId = parent.getChildNodeEntry(nodeName, 1).getId();
            throw new ItemExistsException(safeGetJCRPath(errorId));
        }
        if (id == null) {
            // create new id
            id = new NodeId(UUID.randomUUID());
        }
        if (nodeTypeName == null) {
            // no primary node type specified,
            // try default primary type from definition
            nodeTypeName = def.getDefaultPrimaryType();
            if (nodeTypeName == null) {
                String msg = "an applicable node type could not be determined for "
                        + nodeName;
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        NodeState node = stateMgr.createNew(id, nodeTypeName, parent.getNodeId());
        if (mixinNames != null && mixinNames.length > 0) {
            node.setMixinTypeNames(new HashSet(Arrays.asList(mixinNames)));
        }
        node.setDefinitionId(def.getId());

        // now add new child node entry to parent
        parent.addChildNodeEntry(nodeName, id);

        EffectiveNodeType ent = getEffectiveNodeType(node);

        if (!node.getMixinTypeNames().isEmpty()) {
            // create jcr:mixinTypes property
            PropDef pd = ent.getApplicablePropertyDef(QName.JCR_MIXINTYPES,
                    PropertyType.NAME, true);
            createPropertyState(node, pd.getName(), pd.getRequiredType(), pd);
        }

        // add 'auto-create' properties defined in node type
        PropDef[] pda = ent.getAutoCreatePropDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            createPropertyState(node, pd.getName(), pd.getRequiredType(), pd);
        }

        // recursively add 'auto-create' child nodes defined in node type
        NodeDef[] nda = ent.getAutoCreateNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            createNodeState(node, nd.getName(), nd.getDefaultPrimaryType(),
                    null, null, nd);
        }

        // store node
        stateMgr.store(node);
        // store parent
        stateMgr.store(parent);

        return node;
    }*/

    /**
     * Creates a new property.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param propName
     * @param type
     * @param numValues
     * @return
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @throws IllegalStateException        if the state mananger is not in edit mode
     */
    /*public PropertyState createPropertyState(NodeState parent,
                                             QName propName,
                                             int type,
                                             int numValues)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException, IllegalStateException {

        // check precondition
        if (!stateMgr.inEditMode()) {
            throw new IllegalStateException("not in edit mode");
        }

        // find applicable definition
        PropDef def;
        // multi- or single-valued property?
        if (numValues == 1) {
            // could be single- or multi-valued (n == 1)
            try {
                // try single-valued
                def = findApplicablePropertyDefinition(propName,
                        type, false, parent);
            } catch (ConstraintViolationException cve) {
                // try multi-valued
                def = findApplicablePropertyDefinition(propName,
                        type, true, parent);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            def = findApplicablePropertyDefinition(propName,
                    type, true, parent);
        }
        return createPropertyState(parent, propName, type, def);
    }*/

    /**
     * Creates a new property based on the given definition.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     * <p/>
     * <b>Precondition:</b> the state manager needs to be in edit mode.
     *
     * @param parent
     * @param propName
     * @param type
     * @param def
     * @return
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    /*public PropertyState createPropertyState(NodeState parent,
                                             QName propName,
                                             int type,
                                             PropDef def)
            throws ItemExistsException, RepositoryException {
        // check for name collisions with existing child nodes
        if (parent.hasChildNodeEntry(propName)) {
            String msg = "there's already a child node with name " + propName;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // check for name collisions with existing properties
        if (parent.hasPropertyName(propName)) {
            PropertyId errorId = new PropertyId(parent.getNodeId(), propName);
            throw new ItemExistsException(safeGetJCRPath(errorId));
        }

        // create property
        PropertyState prop = stateMgr.createNew(propName, parent.getNodeId());

        prop.setDefinitionId(def.getId());
        if (def.getRequiredType() != PropertyType.UNDEFINED) {
            prop.setType(def.getRequiredType());
        } else if (type != PropertyType.UNDEFINED) {
            prop.setType(type);
        } else {
            prop.setType(PropertyType.STRING);
        }
        prop.setMultiValued(def.isMultiple());

        // compute system generated values if necessary
        InternalValue[] genValues =
                computeSystemGeneratedPropertyValues(parent, def);
        if (genValues != null) {
            prop.setValues(genValues);
        } else if (def.getDefaultValues() != null) {
            prop.setValues(def.getDefaultValues());
        }

        // now add new property entry to parent
        parent.addPropertyName(propName);
        // store parent
        stateMgr.store(parent);

        return prop;
    }*/

    /**
     * Unlinks the specified node state from its parent and recursively
     * removes it including its properties and child nodes.
     * <p/>
     * Note that no checks (access rights etc.) are performed on the specified
     * target node state. Those checks have to be performed beforehand by the
     * caller. However, the (recursive) removal of target node's child nodes are
     * subject to the following checks: access rights, locking, versioning.
     *
     * @param target
     * @throws RepositoryException if an error occurs
     */
    public void removeNodeState(NodeStateEx target)
            throws RepositoryException {

        /*Long parentId = target.getParentIdLong();
        if (parentId == null) {
            String msg = "root node cannot be removed";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        NodeStateEx parent = getNodeState(parentId);
        // remove child node entry from parent
        parent.removeChildNodeEntry(target);
        // store parent
        stateMgr.store(parent);

        // remove target
        recursiveRemoveNodeState(target);*/
        throw new UnsupportedOperationException();

    }

    /**
     * Retrieves the state of the node at the given path.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param nodePath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    /*public NodeState getNodeState(Path nodePath)
            throws PathNotFoundException, RepositoryException {
        return getNodeState(stateMgr, hierMgr, nodePath);
    }*/

    /**
     * Retrieves the state of the node with the given id.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeStateEx getNodeState(Long nodeId)
            throws ItemNotFoundException, RepositoryException {

        return stateMgr.getNodeState(nodeId, connection);
    }

    /**
     * Retrieves the state of the property with the given id.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    /*public PropertyState getPropertyState(PropertyId id)
            throws ItemNotFoundException, RepositoryException {
        return (PropertyState) getItemState(stateMgr, id);
    }*/

    /**
     * Retrieves the state of the item with the given id.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    /*public ItemState getItemState(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        return getItemState(stateMgr, id);
    }*/

    //----------------------------------------------------< protected methods >
    /**
     * Verifies that the node at <code>nodePath</code> is checked-out; throws a
     * <code>VersionException</code> if that's not the case.
     * <p/>
     * A node is considered <i>checked-out</i> if it is versionable and
     * checked-out, or is non-versionable but its nearest versionable ancestor
     * is checked-out, or is non-versionable and there are no versionable
     * ancestors.
     *
     * @param nodePath
     * @throws PathNotFoundException
     * @throws VersionException
     * @throws RepositoryException
     */
    protected void verifyCheckedOut(Path nodePath)
            throws PathNotFoundException, VersionException, RepositoryException {
        // search nearest ancestor that is versionable, start with node at nodePath
        /**
         * FIXME should not only rely on existence of jcr:isCheckedOut property
         * but also verify that node.isNodeType("mix:versionable")==true;
         * this would have a negative impact on performance though...
         */
        session.getStateManager().verifyCheckedOut(nodePath);
        
       /* NodeStateEx nodeState = getNodeState(nodePath, stateMgr);
        while (!nodeState.hasProperty(QName.JCR_ISCHECKEDOUT)) {
            if (nodePath.denotesRoot()) {
                return;
            }
            nodePath = nodePath.getAncestor(1);
            nodeState = getNodeState(nodePath, stateMgr);
        }
        
        _PropertyState propState;
        //try {
            propState = nodeState.getPropertyState(QName.JCR_ISCHECKEDOUT, true);
            //propState = nodeState.getPropertyState(QName.JCR_ISCHECKEDOUT);
        //} catch (ItemStateException ise) {
        //    String msg = "internal error: failed to retrieve state of "
        //            + safeGetJCRPath(propId);
        //    log.debug(msg);
        //    throw new RepositoryException(msg, ise);
        //}
        boolean checkedOut = ((Boolean) propState.getValues()[0].internalValue()).booleanValue();
        if (!checkedOut) {
            throw new VersionException(safeGetJCRPath(nodePath) + " is checked-in");
        }*/
    }

    /**
     * Verifies that the node at <code>nodePath</code> is not locked by
     * somebody else than the current session.
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException
     * @throws LockException         if write access to the specified path is not allowed
     * @throws RepositoryException   if another error occurs
     */
    protected void verifyUnlocked(Long nodeId)
            throws LockException, RepositoryException {
        // make sure there's no foreign lock on node at nodePath
        lockMgr.checkLock(nodeId);
    }

    /**
     * Verifies that the node at <code>nodePath</code> is not protected.
     *
     * @param nodePath path of node to check
     * @throws PathNotFoundException        if no node exists at <code>nodePath</code>
     * @throws ConstraintViolationException if write access to the specified
     *                                      path is not allowed
     * @throws RepositoryException          if another error occurs
     */
    protected void verifyNotProtected(Path nodePath)
            throws PathNotFoundException, ConstraintViolationException,
            RepositoryException {
        /*NodeState node = getNodeState(nodePath);
        NodeDef parentDef = ntReg.getNodeDef(node.getDefinitionId());
        if (parentDef.isProtected()) {
            throw new ConstraintViolationException(safeGetJCRPath(nodePath)
                    + ": node is protected");
        }*/
    	throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the state of the node at <code>nodePath</code> using the given
     * item state manager.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param srcStateMgr
     * @param srcHierMgr
     * @param nodePath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected NodeStateEx getNodeState(Path nodePath, ItemStateManager srcStateMgr)
            throws PathNotFoundException, RepositoryException {
    	return srcStateMgr.getNodeState(nodePath, connection);
    }

    /**
     * Retrieves the state of the item with the specified id using the given
     * item state manager.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param srcStateMgr
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    /*protected ItemState getItemState(ItemStateManager srcStateMgr, ItemId id)
            throws ItemNotFoundException, RepositoryException {
        try {
            return srcStateMgr.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(safeGetJCRPath(id));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of "
                    + safeGetJCRPath(id);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }*/

    //------------------------------------------------------< private methods >
    /**
     * Computes the values of well-known system (i.e. protected) properties.
     * todo: duplicate code in NodeImpl: consolidate and delegate to NodeTypeInstanceHandler
     *
     * @param parent
     * @param def
     * @return the computed values
     */
    /*private InternalValue[] computeSystemGeneratedPropertyValues(NodeState parent,
                                                                 PropDef def) {
        InternalValue[] genValues = null;
*/
        /**
         * todo: need to come up with some callback mechanism for applying system generated values
         * (e.g. using a NodeTypeInstanceHandler interface)
         */

        // compute system generated values
        /*QName declaringNT = def.getDeclaringNodeType();
        QName name = def.getName();
        if (QName.MIX_REFERENCEABLE.equals(declaringNT)) {
            // mix:referenceable node type
            if (QName.JCR_UUID.equals(name)) {
                // jcr:uuid property
                genValues = new InternalValue[]{InternalValue.create(
                        parent.getNodeId().getUUID().toString())};
            }
        } else if (QName.NT_BASE.equals(declaringNT)) {
            // nt:base node type
            if (QName.JCR_PRIMARYTYPE.equals(name)) {
                // jcr:primaryType property
                genValues = new InternalValue[]{InternalValue.create(parent.getNodeTypeName())};
            } else if (QName.JCR_MIXINTYPES.equals(name)) {
                // jcr:mixinTypes property
                Set mixins = parent.getMixinTypeNames();
                ArrayList values = new ArrayList(mixins.size());
                Iterator iter = mixins.iterator();
                while (iter.hasNext()) {
                    values.add(InternalValue.create((QName) iter.next()));
                }
                genValues = (InternalValue[]) values.toArray(new InternalValue[values.size()]);
            }
        } else if (QName.NT_HIERARCHYNODE.equals(declaringNT)) {
            // nt:hierarchyNode node type
            if (QName.JCR_CREATED.equals(name)) {
                // jcr:created property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (QName.NT_RESOURCE.equals(declaringNT)) {
            // nt:resource node type
            if (QName.JCR_LASTMODIFIED.equals(name)) {
                // jcr:lastModified property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (QName.NT_VERSION.equals(declaringNT)) {
            // nt:version node type
            if (QName.JCR_CREATED.equals(name)) {
                // jcr:created property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        }

        return genValues;
    }*/

    /**
     * Recursively removes the given node state including its properties and
     * child nodes.
     * <p/>
     * The removal of child nodes is subject to the following checks:
     * access rights, locking & versioning status. Referential integrity
     * (references) is checked on commit.
     * <p/>
     * Note that the child node entry refering to <code>targetState</code> is
     * <b><i>not</i></b> automatically removed from <code>targetState</code>'s
     * parent.
     *
     * @param targetState
     * @throws RepositoryException if an error occurs
     */
    private void recursiveRemoveNodeState(NodeStateEx targetState)
            throws RepositoryException {

        if (targetState.hasNodes()) {
            // remove child nodes
            // use temp array to avoid ConcurrentModificationException
            /*ArrayList tmp = new ArrayList(targetState.getChildNodeEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) tmp.get(i);
                NodeId nodeId = entry.getId();
                try {
                    NodeStateEx nodeState = (NodeStateEx) stateMgr.getItemState(nodeId);
                    // check if child node can be removed
                    // (access rights, locking & versioning status);
                    // referential integrity (references) is checked
                    // on commit
                    checkRemoveNode(nodeState, targetState.getNodeId(),
                            CHECK_ACCESS
                            | CHECK_LOCK
                            | CHECK_VERSIONING);
                    // remove child node
                    recursiveRemoveNodeState(nodeState);
                } catch (ItemStateException ise) {
                    String msg = "internal error: failed to retrieve state of "
                            + nodeId;
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
                // remove child node entry
                targetState.removeChildNodeEntry(entry.getName(), entry.getIndex());
            }*/
            throw new UnsupportedOperationException();
        }

        // remove properties
        // use temp set to avoid ConcurrentModificationException
        /*HashSet tmp = new HashSet(targetState.getPropertyNames());
        for (Iterator iter = tmp.iterator(); iter.hasNext();) {
            QName propName = (QName) iter.next();
            PropertyId propId =
                    new PropertyId(targetState.getNodeId(), propName);
            try {
                PropertyState propState =
                        (PropertyState) stateMgr.getItemState(propId);
                // remove property entry
                targetState.removePropertyName(propId.getName());
                // destroy property state
                stateMgr.destroy(propState);
            } catch (ItemStateException ise) {
                String msg = "internal error: failed to retrieve state of "
                        + propId;
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
        }

        // now actually do unlink target state
        targetState.setParentId(null);
        // destroy target state (pass overlayed state since target state
        // might have been modified during unlinking)
        stateMgr.destroy(targetState.getOverlayedState());
        */
        throw new UnsupportedOperationException();
    }

    /**
     * Recursively copies the specified node state including its properties and
     * child nodes.
     *
     * @param srcState
     * @param srcStateMgr
     * @param srcAccessMgr
     * @param destParentId
     * @param flag           one of
     *                       <ul>
     *                       <li><code>COPY</code></li>
     *                       <li><code>CLONE</code></li>
     *                       <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                       </ul>
     * @param refTracker     tracks uuid mappings and processed reference properties
     * @return a deep copy of the given node state and its children
     * @throws RepositoryException if an error occurs
     */
    private NodeStateEx copyNodeState(QName nodeName, NodeStateEx srcState,
                                    ItemStateManager srcStateMgr,
                                    AccessManager srcAccessMgr,
                                    NodeStateEx destParent,
                                    int flag,
                                    ReferenceChangeTracker refTracker)
            throws RepositoryException {

        NodeStateEx newState;
        try {
            NodeId id;
            EffectiveNodeType ent = srcState.getEffectiveNodeType();
            boolean referenceable = ent.includesNodeType(QName.MIX_REFERENCEABLE);
            boolean _versionable = ent.includesNodeType(QName.MIX_VERSIONABLE);
            switch (flag) {
                case COPY:
                    // always create new uuid
                    id = new NodeId(repository.nextId(), UUID.randomUUID());
                    if (referenceable) {
                        // remember uuid mapping
                        refTracker.mappedUUID(srcState.getUUID(), id.getUUID());
                    }
                    break;
                case CLONE:
                    if (!referenceable) {
                        // non-referenceable node: always create new uuid
                        id = new NodeId(repository.nextId(), UUID.randomUUID());
                        break;
                    }
                    // use same uuid as source node
                    id = (NodeId)srcState.getId();
                    if (stateMgr.hasItemState(id)) {
                        // node with this uuid already exists
                        throw new ItemExistsException(safeGetJCRPath(id));
                    }
                    break;
                case CLONE_REMOVE_EXISTING:
                    if (!referenceable) {
                        // non-referenceable node: always create new uuid
                        id = new NodeId(repository.nextId(),UUID.randomUUID());
                        break;
                    }
                    // use same uuid as source node
                    id = (NodeId)srcState.getId();
                    if (stateMgr.hasItemState(id)) {
                        //NodeStateEx existingState = (NodeStateEx) stateMgr.getItemState(id);
                        // make sure existing node is not the parent
                        // or an ancestor thereof
                        //Path p0 = hierMgr.getPath(destParentId);
                        //Path p1 = hierMgr.getPath(id);
                        //try {
                        //    if (p1.equals(p0) || p1.isAncestorOf(p0)) {
                        //        String msg = "cannot remove ancestor node";
                        //        log.debug(msg);
                        //        throw new RepositoryException(msg);
                        //    }
                        //} catch (MalformedPathException mpe) {
                        //    // should never get here...
                        //    String msg = "internal error: failed to determine degree of relationship";
                        //    log.error(msg, mpe);
                        //    throw new RepositoryException(msg, mpe);
                       // }
//
                        // check if existing can be removed
                        // (access rights, locking & versioning status,
                        // node type constraints)
  //                      checkRemoveNode(existingState,
  //                              CHECK_ACCESS
  //                              | CHECK_LOCK
  //                              | CHECK_VERSIONING
  //                              | CHECK_CONSTRAINTS);
  //                      // do remove existing
  //                      removeNodeState(existingState);
                        throw new UnsupportedOperationException();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown flag");
            }
            newState = stateMgr.createNew(nodeName ,id, srcState.getNodeTypeName(), 
                    destParent, srcState.isReferenceable(), 
                    srcState.getNodeId().equals(srcState.getNodeState().getSecurityId()), false);
            //security
            //1.check if source node have own security
            if (srcState.getNodeId().equals(srcState.getNodeState().getSecurityId())){
            	//copy parent security
            	if (repository.getSecurityCopyType() == SecurityCopyType.Copy){
	            	for(SecurityEntry ace:destParent.getACEList()){
	                    for(SecurityPermission p:SecurityPermission.values()){
	                        if (ace.getPermission(p) != null){
	                            SecurityModificationEntry sme = new SecurityModificationEntry(newState.getNodeId(),SecurityModificationEntry.SET_PERMISSION, 
	                            		ace.getUserId(), ace.getGroupId(), ace.getContextId(), p, ace.getPermission(p), ace.getPermissionParentId(p), ace.getPermissionFromAsString(p),ace.isDirectPermission(p));
	                            newState.addSecurityModificationEntry(sme);
	                        }
	                    }
	                }
	            	List<SecurityEntry> _acl = srcState.getACEList();
	            	for(SecurityEntry ace:_acl){
	            		for(SecurityPermission p : SecurityPermission.values()){
	            			Boolean value = ace.getPermission(p);
	            			if (value != null){
	            				Long aceParentId = ace.getPermissionParentId(p);
	            				if (aceParentId.equals(srcState.getNodeId())){
	            					SecurityModificationEntry entry = new SecurityModificationEntry(newState.getNodeId(), 
	            							SecurityModificationEntry.SET_PERMISSION,
	            							ace.getUserId(),
	            							ace.getGroupId(),
	            							ace.getContextId(),
	            							p, value, newState.getNodeId(),
	            							p.getExportName(),
	            							false);
	            					newState.addSecurityModificationEntry(entry);
	            				}
	            			}
	            		}
	            	}
            	} else if (repository.getSecurityCopyType() == SecurityCopyType.Inherit){
            		newState.setSecurityId(destParent.getSecurityId());
            		//SecurityModificationEntry sme = new SecurityModificationEntry(newState.getNodeId(),SecurityModificationEntry.RESET, SecurityPrincipal.user(null), null, null, null, null, false);
                    //newState.addSecurityModificationEntry(sme);
            		
            	}
            	
            } 
            
            
            // copy node state
            newState.setMixinTypeNames(srcState.getMixinTypeNames());
            //newState.setDefinitionId(srcState.getDefinitionId());
            // copy child nodes
            //Iterator iter = srcState.getChildNodeEntries().iterator();
            for (NodeStateEx entry:srcState.getNodes()) {
                //NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                NodeId nodeId = (NodeId)entry.getId();
                if (!srcAccessMgr.isGranted(nodeId.getId(), SecurityPermission.READ)) {
                    continue;
                }
                NodeStateEx srcChildState = (NodeStateEx) entry;
				
                //
                 // special handling required for child nodes with special semantics
                 // (e.g. those defined by nt:version,  et.al.)
                 //
                 // todo FIXME delegate to 'node type instance handler'
                 //

                // recursive copying of child node
                copyNodeState(srcChildState.getQName()
                		,srcChildState,
                        srcStateMgr, srcAccessMgr, newState, flag, refTracker);
                // store new child node
                //stateMgr.store(newChildState);
                // add new child node entry to new node
                //newState.addChildNodeEntry(entry.getName(), newChildState.getNodeId());
            }
            // copy properties
            
            for (QName propName : srcState.getNodeState().getPropertyNames()) {
                PropertyId propId = new PropertyId(srcState.getNodeId(), propName);
                if (!srcAccessMgr.isGranted(propId, SecurityPermission.READ)) {
                    continue;
                }
                _PropertyState srcChildState = srcState.getPropertyState(propId.getName(), true);

                //
                 // special handling required for properties with special semantics
                 // (e.g. those defined by mix:referenceable, mix:versionable,
                 // mix:lockable, et.al.)
                 //
                 // todo FIXME delegate to 'node type instance handler'
                 //
                PropertyDefinitionImpl def = srcChildState.getDefinition();
                if (((NodeTypeImpl)def.getDeclaringNodeType()).getQName().equals(QName.MIX_LOCKABLE)) {
                    // skip properties defined by mix:lockable
                    continue;
                }
                if (((NodeTypeImpl)def.getDeclaringNodeType()).getQName().equals(QName.MIX_REFERENCEABLE)
                		&& propName.equals(QName.JCR_UUID)
                ) {
                	continue;
                }
                if (propName.equals(QName.JCR_PRIMARYTYPE) || propName.equals(QName.JCR_MIXINTYPES) 
                ) {
                	continue;
                }

                _PropertyState newChildState = copyPropertyState(srcChildState, id, propName, newState.getNodeState());

                if (_versionable && flag == COPY) {
                    //
                    // a versionable node is being copied:
                    // copied properties declared by mix:versionable need to be
                    // adjusted accordingly.
                    //
                    // jcr:versionHistory
                    if (propName.equals(QName.JCR_VERSIONHISTORY)) {
                        VersionHistory vh = getOrCreateVersionHistory(newState);
                        //newState.getParentId();
                        newChildState.setValues(new InternalValue[]{InternalValue.create(new UUID(vh.getUUID()), false)});
                       //NodeReference nr = new NodeReference((NamespaceRegistryImpl)session._getWorkspace().getNamespaceRegistry(), 
                        //		null, newState.getNodeId(), ((NodeImpl)vh).getNodeId(), newChildState.getName(), ((NodeImpl)vh).getInternalUUID());
                        //newState.getNodeState().registerTmpRefeference(nr);// registerNodeReference(nr);
                    }

                    // jcr:baseVersion
                    if (propName.equals(QName.JCR_BASEVERSION)) {
                        VersionHistory vh = getOrCreateVersionHistory(newState);
                        String rootVersionUUID = ((VersionHistoryImpl)vh).getRootVersionState().getInternalUUID();
                        newChildState.setValues(new InternalValue[]{InternalValue.create(new UUID(rootVersionUUID), false)});
                        //state.internalSetProperty(QName.JCR_BASEVERSION, InternalValue.create(new UUID(rootVersionUUID), false), true, true, true);
                       /* String bvuuid = srcState.getProperty(QName.JCR_BASEVERSION).getString();
                        _NodeState bvState = srcState.getStateManager().getItemByUUID(bvuuid, false);
                        Long bvId = bvState.getNodeId();
                        //VersionImpl bv = (VersionImpl) srcState.getBaseVersion();
                        newChildState.setValues(new InternalValue[]{InternalValue.create(new UUID(bvuuid), false)});
                        NodeReference nr = new NodeReference((NamespaceRegistryImpl)session._getWorkspace().getNamespaceRegistry(), 
                        		null, newState.getNodeId(),bvId, newChildState.getName(),
                        		bvuuid) ;
                        newState.getNodeState().registerTmpRefeference(nr);  //registerNodeReference(nr);
                        */
                    }

                    // jcr:predecessors
                    if (propName.equals(QName.JCR_PREDECESSORS)) {
                        VersionHistory vh = getOrCreateVersionHistory(newState);
                        String rootVersionUUID = ((VersionHistoryImpl)vh).getRootVersionState().getInternalUUID();
                        newChildState.setValues(new InternalValue[]{InternalValue.create(new UUID(rootVersionUUID), false)});
/*                    	PropertyImpl predProp = srcState.getProperty(QName.JCR_PREDECESSORS);
                    	Value[] predValues = predProp.getValues();
                    	InternalValue[] vvv = new InternalValue[predValues.length];
                    	int i = 0;
                    	for(Value v:predValues){
                    		String uuid = v.getString();
                            _NodeState state = srcState.getStateManager().getItemByUUID(uuid, false);
                            Long nid = state.getNodeId();
                            vvv[i++]=InternalValue.create(new UUID(uuid), false);
                            NodeReference nr = new NodeReference((NamespaceRegistryImpl)session._getWorkspace().getNamespaceRegistry(), 
                            		null, newState.getNodeId(), nid, newChildState.getName(), uuid);
                            newState.getNodeState().registerTmpRefeference(nr);//.registerNodeReference(nr);
                    	}
                    	//VersionHistory vh = getOrCreateVersionHistory(newState);
                        //newChildState.setValues(new InternalValue[]{InternalValue.create(new UUID(vh.getRootVersion().getUUID()), false)});
                    	newChildState.setValues(vvv);
  */                      

                    }

                    // jcr:isCheckedOut
                    if (propName.equals(QName.JCR_ISCHECKEDOUT)) {
                        newChildState.setValues(new InternalValue[]{InternalValue.create(true)});
                    }
                }

                if (newChildState.getType() == PropertyType.REFERENCE || newChildState.getType() == PropertyType283.WEAKREFERENCE) {
                    refTracker.processedReference(newChildState);
                }
                // store new property
                //stateMgr.store(newChildState);
                // add new property entry to new node
                if (newState.hasProperty(newChildState.getName())){
                	newState.getPropertyState(newChildState.getName(), true).setValues(newChildState.getValues());
                } else {
                	newState.addProperty(newChildState);
                	if (newChildState.getType() == PropertyType.REFERENCE || newChildState.getType() == PropertyType.REFERENCE){
                		for(InternalValue v:newChildState.getValues()){
                			v.internalValue();
                		}
                	}
                }
            }
            return newState;
        } catch (RepositoryException ise) {
            String msg = "internal error: failed to copy state of " + srcState.getNodeId();
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * Copies the specified property state.
     *
     * @param srcState
     * @param parentId
     * @param propName
     * @return
     * @throws RepositoryException
     */
    private _PropertyState copyPropertyState(_PropertyState srcState,
                                            NodeId parentId,
                                            QName propName,_NodeState newParent)
            throws RepositoryException {

    	PropertyDefinitionImpl def = srcState.getDefinition();

//        PropertyState newState = stateMgr.createNew(propName, parentId);
    	//_PropertyState newState = new _PropertyState(propName, srcState.getRequiredType(), def, parentId.getId(), null);
    	_PropertyState newState = new _PropertyState(srcState.getRepository(), newParent, propName, srcState.getType(), 
    			srcState.getRequiredType(), def.isMultiple(), (PropDefImpl)def.unwrap(), null);
    	
    	
        InternalValue[] values = srcState.getValues();
        if (values != null) {
            /**
             * special handling required for properties with special semantics
             * (e.g. those defined by mix:referenceable, mix:versionable,
             * mix:lockable, et.al.)
             *
             * todo FIXME delegate to 'node type instance handler'
             */
            if (def.getDeclaringNodeType().equals(QName.MIX_REFERENCEABLE)
                    && propName.equals(QName.JCR_UUID)) {
                // set correct value of jcr:uuid property
                newState.setValues(new InternalValue[]{InternalValue.create(parentId.getUUID().toString())});
            } else {
                InternalValue[] newValues = new InternalValue[values.length];
                for (int i = 0; i < values.length; i++) {
                	if (values[i].getType() == PropertyType.BINARY){
                		newValues[i] = values[i].createCopyBinary();
                	} else {
                		newValues[i] = values[i].createCopy();
                	}
                }
                newState.setValues(newValues);
            }
        }
        return newState;
        
    }

    
    private SoftHashMap<NodeStateEx, VersionHistory> vhCache = new SoftHashMap<NodeStateEx, VersionHistory>();
    
    /**
     * Returns the version history of the given node state. A new
     * version history will be created if doesn't exist yet.
     *
     * @param node node state
     * @return the version history of the target node state
     * @throws RepositoryException if an error occurs
     */
    private VersionHistory getOrCreateVersionHistory(NodeStateEx node)
            throws RepositoryException {
        VersionManager vMgr = session.getVersionManager();
        VersionHistory vh = vhCache.get(node);
        if (vh != null){
        	return vh;
        }
        vh = vMgr.getVersionHistory((NodeId)node.getId());
        if (vh == null) {
            // create a new version history
            vh = vMgr.createVersionHistory((NodeId)node.getId(), ((NodeTypeImpl)node.getPrimaryNodeType()).getQName(), node.getMixinTypeNames(), new HashMap<String, Long>());
        }
        vhCache.put(node, vh);
        return vh;
        

    }
}
