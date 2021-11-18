/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import javax.jcr.ItemNotFoundException;
import javax.jcr.version.VersionException;

import com.exigen.cm.jackrabbit.name.QName;

/**
 * Implements a <code>InternalFrozenVersionHistory</code>
 */
public class InternalFrozenVHImpl extends InternalFreezeImpl
        implements InternalFrozenVersionHistory {

    /**
     * the underlying persistence node
     */
    private NodeStateEx node;

    /**
     * Creates a new frozen version history.
     *
     * @param node
     */
    public InternalFrozenVHImpl(VersionManagerImpl vMgr, NodeStateEx node,
                                InternalVersionItem parent) {
        super(vMgr, parent);
        this.node = node;
    }


    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return node.getQName();
    }

    /**
     * {@inheritDoc}
     */
    public Long getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     * @throws ItemNotFoundException 
     */
    public String getVersionHistoryId() throws ItemNotFoundException {
        return node.getPropertyState(QName.JCR_CHILDVERSIONHISTORY, true).getString();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory()
            throws VersionException {
        /*try {
            return getVersionManager().getVersionHistory(getVersionHistoryId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }*/
        
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @throws ItemNotFoundException 
     */
    public String getBaseVersionId() throws ItemNotFoundException {
        return (String) node.getPropertyState(QName.JCR_BASEVERSION, true).getString();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getBaseVesion()
            throws VersionException {
        /*try {
            InternalVersionHistory history = getVersionManager().getVersionHistory(getVersionHistoryId());
            return history.getVersion(getBaseVersionId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }*/
        throw new UnsupportedOperationException();

    }
}
