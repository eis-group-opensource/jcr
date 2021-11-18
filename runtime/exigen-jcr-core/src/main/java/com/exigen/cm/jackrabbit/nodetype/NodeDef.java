/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import java.io.Serializable;

import com.exigen.cm.jackrabbit.name.QName;


/**
 * <code>NodeDef</code> is the internal representation of
 * a node definition. It refers to <code>QName</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.NodeDefinition
 */
public interface NodeDef extends ItemDef {

    public static final NodeDef[] EMPTY_ARRAY = new NodeDef[0];

    /**
     * Returns an identifier for this node definition.
     *
     * @return an identifier for this node definition.
     */
    //Long getId();

    /**
     * Returns the name of the default primary type.
     *
     * @return the name of the default primary type.
     */
    QName getDefaultPrimaryType();

    /**
     * Returns the array of names of the required primary types.
     *
     * @return the array of names of the required primary types.
     */
    QName[] getRequiredPrimaryTypes();

    /**
     * Reports whether this node can have same-name siblings.
     *
     * @return the 'allowsSameNameSiblings' flag.
     */
    boolean allowsSameNameSiblings();

    void setSQLId(Long id);
    public Long getSQLId();

    void configure(QName rep_root, boolean b, boolean c, boolean d, int abort, boolean sns);

	Serializable getTempId();

	NodeDefId getNodeDefId();
    
    
}
