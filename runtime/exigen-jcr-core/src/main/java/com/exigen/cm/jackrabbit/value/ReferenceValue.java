/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.value;


import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * A <code>ReferenceValue</code> provides an implementation
 * of the <code>Value</code> interface representing a <code>REFERENCE</code> value
 * (a UUID of an existing node).
 */
public class ReferenceValue extends BaseReferenceValue {

    public static final int TYPE = PropertyType.REFERENCE;

    /**
     * Constructs a <code>ReferenceValue</code> object representing the UUID of
     * an existing node.
     *
     * @param target the node to be referenced
     * @throws IllegalArgumentException If <code>target</code> is nonreferenceable.
     * @throws javax.jcr.RepositoryException      If another error occurs.
     */
    public ReferenceValue(Node target) throws RepositoryException {
        super(target,TYPE);
    }

    public ReferenceValue(String uuid) {
        super(uuid, TYPE);
    }

}
