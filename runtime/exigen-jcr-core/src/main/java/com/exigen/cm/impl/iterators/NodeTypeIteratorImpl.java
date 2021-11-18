/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.iterators;

import java.util.List;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

public class NodeTypeIteratorImpl extends RangeIteratorImpl implements
        NodeTypeIterator {

    public NodeTypeIteratorImpl(List<NodeType> data) {
        super(null, data);
    }

    protected Object buildObject(Object source) {
        return source;
    }

    public NodeType nextNodeType() {
        return (NodeType) next();
    }

}

/*
 * $Log: NodeTypeIteratorImpl.java,v $
 * Revision 1.1  2007/04/26 09:01:27  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2007/01/24 08:46:23  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.1  2006/04/17 06:47:00  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/03/27 14:57:33  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/02/20 15:32:31  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */