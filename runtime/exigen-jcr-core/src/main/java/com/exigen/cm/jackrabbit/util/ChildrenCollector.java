/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.util;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.util.TraversingItemVisitor;

/**
 * <code>ChildrenCollector</code> is a utility class
 * which can be used to 'collect' child elements of a
 * node. It implements the <code>ItemVisitor</code>
 * interface.
 */
public class ChildrenCollector extends TraversingItemVisitor.Default {

    private final Collection children;
    private final boolean collectNodes;
    private final boolean collectProperties;

    /**
     * Constructs a <code>ChildrenCollector</code>
     *
     * @param children          where the matching children should be added
     * @param collectNodes      true, if child nodes should be collected; otherwise false
     * @param collectProperties true, if child properties should be collected; otherwise false
     * @param maxLevel          number of hierarchy levels to traverse
     *                          (e.g. 1 for direct children only, 2 for children and their children, and so on)
     */
    public ChildrenCollector(Collection children, boolean collectNodes, boolean collectProperties, int maxLevel) {
        super(false, maxLevel);
        this.children = children;
        this.collectNodes = collectNodes;
        this.collectProperties = collectProperties;
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Node node, int level)
            throws RepositoryException {
        if (level > 0 && collectNodes) {
            children.add(node);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Property property, int level)
            throws RepositoryException {
        if (level > 0 && collectProperties) {
            children.add(property);
        }
    }
}
