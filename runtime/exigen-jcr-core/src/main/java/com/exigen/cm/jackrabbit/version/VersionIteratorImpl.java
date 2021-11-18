/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.VersionImpl;

/**
 * This Class implements a VersionIterator that iterates over a version
 * graph following the successor nodes. When this iterator is created, it gathers
 * the id's of the versions and returns them when iterating. please note, that
 * a version can be deleted while traversing this iterator and the 'nextVesion'
 * would produce a  ConcurrentModificationException.
 */
public class VersionIteratorImpl implements VersionIterator {

    /**
     * the id's of the versions to return
     */
    private LinkedList versions = new LinkedList();

    /**
     * the current position
     */
    private int pos = 0;

    /**
     * the session for wrapping the versions
     */
    private final SessionImpl session;

    private InternalVersion rootVersion;

    /**
     * Creates a new VersionIterator that iterates over the version tree,
     * starting the root node.
     *
     * @param rootVersion
     */
    public VersionIteratorImpl(SessionImpl session, InternalVersion rootVersion) {
        this.session = session;
        this.rootVersion = rootVersion;
        
        addVersion(rootVersion);
    }

    /**
     * {@inheritDoc}
     */
    public Version nextVersion() {
        if (versions.isEmpty()) {
            throw new NoSuchElementException();
        }
        Long id = (Long) versions.removeFirst();
        pos++;

        try {
            VersionImpl v  = (VersionImpl) session.getNodeManager().buildNode(id);
            InternalVersion iv = rootVersion.getVersionHistory().getVersion(v.getQName());
            v.setVersion(iv);
            return v;
        } catch (RepositoryException e) {
            throw new ConcurrentModificationException("Unable to provide element: " + e.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        while (skipNum > 0) {
            skipNum--;
            nextVersion();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return versions.size();
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException since this operation is not supported
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return !versions.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        return nextVersion();
    }

    /**
     * Adds the version 'v' to the list of versions to return and then calls
     * it self recursively with all the versions predecessors.
     *
     * @param v
     */
    private synchronized void addVersion(InternalVersion v) {
        Long id = v.getId();
        if (!versions.contains(id)) {
            versions.add(id);
            InternalVersion[] vs = v.getSuccessors();
            for (int i = 0; i < vs.length; i++) {
                addVersion(vs[i]);
            }
        }
    }
}
