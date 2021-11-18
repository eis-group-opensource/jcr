/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * <code>IteratorHelper</code> is a utility class which
 * wraps an iterator and implements the various typed iterator
 * interfaces.
 */
public class IteratorHelper
        implements NodeIterator, PropertyIterator, NodeTypeIterator {

    static final long UNDETERMINED_SIZE = -1;

    public static final IteratorHelper EMPTY =
            new IteratorHelper(Collections.EMPTY_LIST);

    private final Iterator iter;
    private long size;
    private long pos;

    /**
     * Constructs an <code>IteratorHelper</code> which is backed
     * by a <code>java.util.Collection</code>.
     *
     * @param c collection which should be iterated over.
     */
    public IteratorHelper(Collection c) {
        this(c.iterator());
        size = c.size();
    }

    /**
     * Constructs an <code>IteratorHelper</code> which is wrapping
     * a <code>java.util.Iterator</code>.
     *
     * @param iter iterator which should be wrapped.
     */
    public IteratorHelper(Iterator iter) {
        this.iter = iter;
        pos = 0;
        size = UNDETERMINED_SIZE;
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return iter.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        // all typed nextXXX methods should
        // delegate to this method
        Object obj = iter.next();
        // increment position
        pos++;
        return obj;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        iter.remove();
    }

    /**
     * {@inheritDoc}
     */
    public Node nextNode() {
        return (Node) next();
    }

    /**
     * {@inheritDoc}
     */
    public Property nextProperty() {
        return (Property) next();
    }

    /**
     * {@inheritDoc}
     */
    public NodeType nextNodeType() {
        return (NodeType) next();
    }
}
