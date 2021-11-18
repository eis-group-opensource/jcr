/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;


import java.util.HashMap;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state.NodeState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.virtual.VirtualItemStateProvider;


/**
 * This interface defines the version manager. It gives access to the underlying
 * persistence layer of the versioning.
 */
public interface VersionManager {

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @return
     */
    VirtualItemStateProvider getVirtualItemStateProvider();

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionable' mixin
     * to a node.
     * @param paths 
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see #getVersionHistory(Session, NodeState) 
     */
    VersionHistory createVersionHistory( NodeId nodeId, QName primaryType, Set<QName> mixinTypes, HashMap<String, Long> paths)
            throws RepositoryException;

    /**
     * Returns the version history of the specified <code>node</code> or
     * <code>null</code> if the given node doesn't (yet) have an associated
     * version history.
     *
     * @param session
     * @param node node whose version history should be returned
     * @return the version history of the specified <code>node</code> or
     *         <code>null</code> if the given node doesn't (yet) have an
     *        associated version history.
     * @throws RepositoryException if an error occurs
     * @see #createVersionHistory(Session, NodeState)
     */
    VersionHistory getVersionHistory(NodeId nodeId)
    throws RepositoryException;

    InternalVersionHistory getInternalVersionHistory(SessionImpl session, NodeId nodeId)
    throws RepositoryException;

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    Version checkin(NodeImpl node) throws RepositoryException;

    /**
     * Removes the specified version from the given version history.
     * @param history
     * @param versionName
     * @throws RepositoryException
     */
    void removeVersion(VersionHistory history, QName versionName)
            throws RepositoryException;

    /**
     * Sets the version <code>label</code> to the given <code>version</code>.
     * If the label is already assigned to another version, a VersionException is
     * thrown unless <code>move</code> is <code>true</code>. If <code>version</code>
     * is <code>null</code>, the label is removed from the respective version.
     * In either case, the version the label was previously assigned is returned,
     * or <code>null</code> of the label was not moved.
     *
     * @param history
     * @param version
     * @param label
     * @param move
     * @return
     * @throws RepositoryException
     */
    Version setVersionLabel(VersionHistory history, QName version, QName label,
                            boolean move)
            throws RepositoryException;

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return
     */
    boolean hasVersionHistory(String id);


    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return
     */
    boolean hasVersion(String id);

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    InternalVersion getVersion(String id) throws RepositoryException;

    /**
     * Close this version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    void close() throws Exception;

    //VersionHistory getOrCreateVersionHistory(_NodeImpl impl) throws RepositoryException;


}
