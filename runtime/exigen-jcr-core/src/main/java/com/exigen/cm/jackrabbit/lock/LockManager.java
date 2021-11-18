/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;


import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl._NodeImpl;
import com.exigen.cm.jackrabbit.name.Path;

/**
 * Defines the functionality needed for locking and unlocking nodes.
 */
public interface LockManager {

    /**
     * Lock a node. Checks whether the node is not locked and then
     * returns a lock object for this node.
     * @param node node
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @return lock object
     * @throws LockException if this node already is locked, or some descendant
     *         node is locked and <code>isDeep</code> is <code>true</code>
     * @see javax.jcr.Node#lock
     */
    Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, Map<String, Object> options)
            throws LockException, RepositoryException;

    /**
     * Returns the Lock object that applies to a node. This may be either a lock
     * on this node itself or a deep lock on a node above this node.
     * @param node node
     * @return lock object
     * @throws LockException if this node is not locked
     * @see javax.jcr.Node#getLock
     */
    Lock getLock(NodeImpl node) throws LockException, RepositoryException;

    /**
     * Removes the lock on a node given by its path.
     * @param node node
     * @throws LockException if this node is not locked or the session
     *         does not have the correct lock token
     * @see javax.jcr.Node#unlock
     */
    void unlock(NodeImpl node, Map<String, Object> options) throws LockException, RepositoryException;

    /**
     * Returns <code>true</code> if the node given holds a lock;
     * otherwise returns <code>false</code>
     * @param node node
     * @return <code>true</code> if the node given holds a lock;
     *         otherwise returns <code>false</code>
     * @see javax.jcr.Node#holdsLock
     */
    boolean holdsLock(NodeImpl node) throws RepositoryException;

    /**
     * Returns <code>true</code> if this node is locked either as a result
     * of a lock held by this node or by a deep lock on a node above this
     * node; otherwise returns <code>false</code>
     * @param node node
     * @return <code>true</code> if this node is locked either as a result
     * of a lock held by this node or by a deep lock on a node above this
     * node; otherwise returns <code>false</code>
     * @see javax.jcr.Node#isLocked
     */
    boolean isLocked(NodeImpl node) throws RepositoryException;

    /**
     * Check whether the node given is locked by somebody else than the
     * current session. Access is allowed if the node is not locked or
     * if the session itself holds the lock to this node, i.e. the session
     * contains the lock token for the lock.
     * @param node node to check
     * @throws LockException if write access to the specified node is not allowed
     * @throws RepositoryException if some other error occurs
     */
    void checkLock(_NodeImpl node)
            throws LockException, RepositoryException;

    
    public void checkLock(Long nodeId) throws LockException, RepositoryException;
    
    /**
     * Check whether the path given is locked by somebody else than the
     * session described. Access is allowed if the node is not locked or
     * if the session itself holds the lock to this node, i.e. the session
     * contains the lock token for the lock.
     * @param path path to check
     * @param session session
     * @throws LockException if write access to the specified path is not allowed
     * @throws RepositoryException if some other error occurs
     */
    void checkLock(Path path, Session session)
            throws LockException, RepositoryException;

    /**
     * Invoked by a session to inform that a lock token has been added.
     * @param session session that has a added lock token
     * @param lt added lock token
     * @throws RepositoryException 
     */
    void lockTokenAdded(SessionImpl session, String lt) throws RepositoryException;

    /**
     * Invoked by a session to inform that a lock token has been removed.
     * @param session session that has a removed lock token
     * @param lt removed lock token
     */
    void lockTokenRemoved(SessionImpl session, String lt);

	void registerListener(LockManagerListener listener);

	void checkDeepLock(_NodeImpl nodeImpl) throws LockException, RepositoryException;

//	void touchLock(NodeImpl nodeImpl, Calendar expires) throws LockException, RepositoryException;
}
