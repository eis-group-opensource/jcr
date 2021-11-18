/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import com.exigen.cm.impl.NodeTypeImpl;


/**
 * Comparison which always evaluates to <code>true</code> or <code>false</code>.
 */
public class ConstantResultComparison extends Comparison {

    private final boolean result;
    ConstantResultComparison(boolean result){//, Sequence seq) {
        super(null);//, null);//, seq);
        this.result=result;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getConstantResult(){
        /*
                 T  T           -1  F
                 F  F           -1  F
                 F  T            1  T
                 T  F            1  T
        */
        return negated() == result ? -1 : 1;
    }
    
    /**
     * Operation is not supported therefore throws an exception.
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) {
        // Cannot be normally called ...
        throw new UnsupportedOperationException("Operation is not supported!");
    }
}

/*
 * $Log: ConstantResultComparison.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */