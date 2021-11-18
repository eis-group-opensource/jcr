/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import com.exigen.cm.jackrabbit.name.QName;


/**
 * the base interface for nodes that were versioned and turned either into
 * InternalFrozenNode or InteralFrozenVersionHistory.
 */
public interface InternalFreeze extends InternalVersionItem {

    /**
     * returns the name of the node.
     *
     * @return the name of the node.
     */
    QName getName();

}
