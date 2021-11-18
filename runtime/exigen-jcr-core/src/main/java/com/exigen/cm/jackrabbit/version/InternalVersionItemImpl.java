/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

/**
 * Implements a <code>InternalVersionItem</code>.
 */
abstract class InternalVersionItemImpl implements InternalVersionItem {

    /**
     * the version manager
     */
    protected final VersionManagerImpl vMgr;

    /**
     * Creates a new Internal version item impl
     *
     * @param vMgr
     */
    protected InternalVersionItemImpl(VersionManagerImpl vMgr) {
        this.vMgr = vMgr;
    }

    /**
     * Returns the persistent version manager for this item
     *
     * @return
     */
    protected VersionManagerImpl getVersionManager() {
        return vMgr;
    }

    /**
     * Returns the external id of this item
     *
     * @return
     */
    public abstract Long getId();

    /**
     * returns the parent version item or null
     *
     * @return
     */
    public abstract InternalVersionItem getParent();

}
