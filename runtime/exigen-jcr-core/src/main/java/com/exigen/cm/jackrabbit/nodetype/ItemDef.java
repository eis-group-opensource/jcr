/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import com.exigen.cm.jackrabbit.name.QName;


/**
 * <code>ItemDef</code> is the internal representation of
 * an item definition. It refers to <code>QName</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.ItemDefinition
 */
public interface ItemDef {

    public static final ItemDef[] EMPTY_ARRAY = new ItemDef[0];

    /**
     * The special wildcard name used as the name of residual item definitions.
     */
    public static final QName ANY_NAME = new QName("", "*");

    /**
     * Gets the name of the child item.
     *
     * @return the name of the child item.
     */
    QName getName();

    /**
     * Gets the name of the declaring node type.
     *
     * @return the name of the declaring node type.
     */
    QName getDeclaringNodeType();

    /**
     * Determines whether the item is 'autoCreated'.
     *
     * @return the 'autoCreated' flag.
     */
    boolean isAutoCreated();

    /**
     * Gets the 'onParentVersion' attribute of the item.
     *
     * @return the 'onParentVersion' attribute.
     */
    int getOnParentVersion();

    /**
     * Determines whether the item is 'protected'.
     *
     * @return the 'protected' flag.
     */
    boolean isProtected();

    /**
     * Determines whether the item is 'mandatory'.
     *
     * @return the 'mandatory' flag.
     */
    boolean isMandatory();

    /**
     * Determines whether this item definition defines a residual set of
     * child items. This is equivalent to calling
     * <code>getName().equals(ANY_NAME)</code>.
     *
     * @return <code>true</code> if this definition defines a residual set;
     *         <code>false</code> otherwise.
     */
    boolean definesResidual();

    /**
     * Determines whether this item definition defines a node.
     *
     * @return <code>true</code> if this is a node definition;
     *         <code>false</code> otherwise (i.e. it is a property definition).
     */
    boolean definesNode();

    int generateHashCode();

}
