/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * Boolean AND implementation.
 * Supports ANDing of Comparison-Comparison; Comparison-Boolean; Boolean-Boolean
 */
class AndOp extends BooleanOperation {

    AndOp(BooleanOperation parent) {
        super(TYPE.AND,2,parent);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected List<List<Comparison>> open(Comparison c1, Comparison c2, int negationLevel){
        List<Comparison> andedLeafs = new ArrayList<Comparison>();
        andedLeafs.add(c1);
        andedLeafs.add(c2);
        List<List<Comparison>> list   = new ArrayList<List<Comparison>>();
        list.add(andedLeafs);
        return list;
    }

    /**
     * @inheritDoc
     */    
    @Override    
    protected List<List<Comparison>> open(BooleanOperation bop, Comparison comp, int negationLevel){
        List<List<Comparison>> childList = bop.open(negationLevel);
        for(List<Comparison> ands : childList)
                    ands.add(comp);
        return childList;
    }
    
    
    /**
     * @inheritDoc
     */       
    @Override
    protected List<List<Comparison>> open(BooleanOperation bop1, BooleanOperation bop2, int negationLevel){    
        List<List<Comparison>> childList1 = bop1.open(negationLevel);
        List<List<Comparison>> childList2 = bop2.open(negationLevel);
        
        List<List<Comparison>> result = new ArrayList<List<Comparison>>();
        
        
        for(List<Comparison> ands1 : childList1)
            for(List<Comparison> ands2 : childList2){
                List<Comparison> res = new ArrayList<Comparison>();
                res.addAll(ands1);
                res.addAll(ands2);
                result.add(res);
            }

        return result;
    }
}

/*
 * $Log: AndOp.java,v $
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.2  2006/11/20 16:15:46  maksims
 * #0149156 String conversion for columns fixed
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */