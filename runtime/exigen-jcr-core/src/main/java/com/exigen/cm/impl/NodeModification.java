/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.List;

import com.exigen.cm.impl.state2._NodeState;

public class NodeModification {

    private ArrayList<_NodeState> nodes = new ArrayList<_NodeState>();

    public void registerNodeModification(_NodeState n) {
        if (!nodes.contains(n)){
            nodes.add(n);
        }
    }

    public boolean allowNodeSave(_NodeState node, List<_NodeState> dirtyNodes) {
        if (nodes.contains(node)) {
            if (!dirtyNodes.containsAll(nodes)) {
                return false;
            }
        }
        return true;
    }

    public boolean isFinished(_NodeState node, List<_NodeState> dirtyNodes) {
        if (nodes.contains(node)) {
            if (!dirtyNodes.containsAll(nodes)) {
                return true;
            }
        }
        return false;
    }

}

/*
 * $Log: NodeModification.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/22 12:10:05  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.2  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.1  2006/06/22 12:00:25  dparhomenko
 * PTR#0146672 move operations
 *
 */