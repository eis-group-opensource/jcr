/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.cluster;


import java.util.Collection;

import javax.jcr.RepositoryException;

import com.exigen.cm.jackrabbit.nodetype.InvalidNodeTypeDefException;

/**
 * Interface used to receive information about incoming, external node type registry events.
 */
public interface NodeTypeEventListener {

    /**
     * Called when one or more node types have been externally registered.
     *
     * @param ntDefs node type definitions
     * @throws RepositoryException if an error occurs
     * @throws InvalidNodeTypeDefException if the node type definition is invalid
     */
    public void externalRegistered(Collection ntDefs)
            throws RepositoryException, InvalidNodeTypeDefException;
}
