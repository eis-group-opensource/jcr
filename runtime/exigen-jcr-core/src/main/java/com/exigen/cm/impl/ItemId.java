/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.Serializable;

/**
 * <code>ItemId</code> serves as the base class for the concrete classes
 * <code>PropertyId</code> and <code>NodeId</code> who uniquely identify
 * nodes and properties in a workspace.
 */
public abstract class ItemId implements Serializable {

    /** Serialization UID of this class. */
    static final long serialVersionUID = -9147603369595196078L;

    /** Memorized hash code. */
    protected int hash;

    /**
     * Creates an empty item ID instance.
     */
    protected ItemId() {
        hash = 0;
    }

    /**
     * Returns <code>true</code> if this id denotes a <code>Node</code>.
     *
     * @return <code>true</code> if this id denotes a <code>Node</code>,
     *         <code>false</code> if it denotes a <code>Property</code>
     * @see PropertyId
     * @see NodeId
     */
    public abstract boolean denotesNode();
}

