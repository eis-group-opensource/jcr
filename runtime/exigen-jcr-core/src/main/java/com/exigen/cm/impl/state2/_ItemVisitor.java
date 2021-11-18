/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import javax.jcr.RepositoryException;

public interface _ItemVisitor {

    /**
     * This method is called when the <code>ItemVisitor</code> is
     * passed to the <code>accept</code> method of a <code>Property</code>.
     * If this method throws an exception the visiting process is aborted.
     *
     * @param property The <code>Property</code> that is accepting this visitor.
     *
     * @throws RepositoryException if an error occurrs
     */
    public void visit(_PropertyState property) throws RepositoryException;

    /**
     * This method is called when the <code>ItemVisitor</code> is
     * passed to the <code>accept</code> method of a <code>Node</code>.
     * If this method throws an exception the visiting process is aborted.
     *
     * @param node The <code>Node</code that is accepting this visitor.
     *
     * @throws RepositoryException if an error occurrs
     */
    public void visit(_NodeState node) throws RepositoryException;	
	
}
