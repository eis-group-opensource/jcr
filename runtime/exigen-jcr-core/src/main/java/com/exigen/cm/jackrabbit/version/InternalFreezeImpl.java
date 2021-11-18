/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

/**
 * Implements a <code>InternalFreeze</code>
 */
abstract class InternalFreezeImpl extends InternalVersionItemImpl
        implements InternalFreeze {

    /**
     * The parent item
     */
    private final InternalVersionItem parent;

    /**
     * Creates a new <code>InternalFreezeImpl</code>
     *
     * @param vMgr
     * @param parent
     */
    protected InternalFreezeImpl(VersionManagerImpl vMgr, InternalVersionItem parent) {
        super(vMgr);
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getParent() {
        return parent;
    }

}
