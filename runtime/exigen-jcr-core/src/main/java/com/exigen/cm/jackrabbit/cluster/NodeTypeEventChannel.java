/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.cluster;

import java.util.Collection;

/**
 * Event channel used to transmit nodetype registry operations.
 */
public interface NodeTypeEventChannel {

    /**
     * Called when one or more node types have been registered.
     *
     * @param ntDefs collection of node type definitions
     */
    public void registered(Collection ntDefs);

    /**
     * Set listener that will receive information about incoming, external node type events.
     *
     * @param listener node type event listener
     */
    public void setListener(NodeTypeEventListener listener);
}
