/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Boolean OR implementation.
 * Supports ORing of Comparison-Comparison; Comparison-Boolean; Boolean-Boolean
 */
class OrOp extends BooleanOperation {

    OrOp(BooleanOperation parent) {
        super(TYPE.OR,2,parent);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected List<List<Comparison>> open(Comparison c1, Comparison c2, int negationLevel){
        List<List<Comparison>> list   = new LinkedList<List<Comparison>>();
        
        List<Comparison> comp1 = new ArrayList<Comparison>();
        comp1.add(c1);
        list.add(comp1);
        
        List<Comparison> comp2 = new ArrayList<Comparison>();
        comp2.add(c2);
        list.add(comp2);
        return list;
    }
    
    /**
     * @inheritDoc
     */
    @Override    
    protected List<List<Comparison>> open(BooleanOperation bop, Comparison comp, int negationLevel){
        List<List<Comparison>> childList = bop.open(negationLevel);
        
        List<Comparison> compChild = new ArrayList<Comparison>();
        compChild.add(comp);
        childList.add(compChild);
        return childList;
    }
    
    /**
     * @inheritDoc
     */
    @Override
    protected List<List<Comparison>> open(BooleanOperation bop1, BooleanOperation bop2, int negationLevel){ 
        List<List<Comparison>> childList1 = bop1.open(negationLevel);
        List<List<Comparison>> childList2 = bop2.open(negationLevel);
        childList1.addAll(childList2);
        return childList1;
    }
}

/*
 * $Log: OrOp.java,v $
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