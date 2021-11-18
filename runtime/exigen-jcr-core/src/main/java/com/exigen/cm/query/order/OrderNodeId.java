/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.order;

import com.exigen.cm.Constants;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.step.PathStep;


/**
 * Defines ordering by node ID.
 */
public class OrderNodeId extends OrderDefinition {
    protected OrderNodeId(ORDER order) {
        super(order);
    }

    /**
     * @inheritDoc
     */
    @Override
    public String[] getReferredColumns() {
        return new String[]{Constants.FIELD_ID};
    }
    
    /**
     * Does nothing because node ID is always declared in a SQL select statement for context node.
     */
    @Override
    public void toSQL(PathStep owner, PathSQL target, FilterContext fc) {
    }
}

/*
 * $Log: OrderNodeId.java,v $
 * Revision 1.1  2007/04/26 09:01:08  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:19  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:25  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.2  2006/12/13 14:27:10  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.1  2006/11/02 17:28:09  maksims
 * #1801897 Query2 addition
 *
 */