/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import javax.jcr.Node;

/**
 * Provides API to filter nodes to be exported.
 * @author mzizkuns
 *
 */
public interface NodeExportAcceptor {
    
    /**
     * Returns <code>true</code> if node is exportable.
     * @param n
     * @return
     */
    public boolean isExportable(Node n);
}
