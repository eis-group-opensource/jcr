/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;

import com.exigen.cm.jackrabbit.name.QName;



/**
 * The <code>NodeTypeRegistryListener</code> interface allows an implementing
 * object to be informed about node type (un)registration.
 *
 * @see NodeTypeRegistry#addListener(NodeTypeRegistryListener)
 * @see NodeTypeRegistry#removeListener(NodeTypeRegistryListener)
 */
public interface NodeTypeRegistryListener {

    /**
     * Called when a node type has been registered.
     *
     * @param ntName name of the node type that has been registered
     */
    void nodeTypeRegistered(QName ntName);

    /**
     * Called when a node type has been re-registered.
     *
     * @param ntName name of the node type that has been registered
     */
    void nodeTypeReRegistered(QName ntName);

    /**
     * Called when a node type has been deregistered.
     *
     * @param ntName name of the node type that has been unregistered
     */
    void nodeTypeUnregistered(QName ntName);
}
