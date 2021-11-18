/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state;

import javax.jcr.RepositoryException;

import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.jackrabbit.name.Path;

public class NodeOverlayedState {
    
    //private NodeImpl node;
    private NodeId parentId;
    private Path path;

    public NodeOverlayedState(NodeImpl node) throws RepositoryException{
        //this.node = node;
        this.parentId = node.getNodeItemId();
        this.path = node.getPrimaryPath();
    }

    public NodeId getParentId() {
        return parentId;
    }

    public Path getPath() {
        return path;
    }

    public String getParentUUID() {
        throw new UnsupportedOperationException();
    }

}


/*
 * $Log: NodeOverlayedState.java,v $
 * Revision 1.1  2007/04/26 08:58:59  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/02 09:32:12  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.2  2006/10/17 10:46:57  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.1  2006/05/22 14:48:07  dparhomenko
 * PTR#1801941 add observationsupport
 *
 */