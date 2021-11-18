/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.util.ArrayList;
import java.util.List;



/**
 * Boolean NOT. Supports nested NOTs by maintaining negationLevel.
 * Negation won't be performed if negationLevel is even.
 */
class NotOp extends BooleanOperation {
    NotOp(BooleanOperation parent) {
        super(TYPE.NOT,1,parent);
    }
    
    /**
     * Negates descendants if negationLevel is even. Increases negation level value.
     */
    @Override
    protected List<List<Comparison>> open(Comparison c, int negationLevel) {
        negationLevel++;
        
        List<Comparison> not = new ArrayList<Comparison>();
        c.negationLevel(negationLevel);
        c.negated(shouldNegate(negationLevel));
        
        not.add(c);

        List<List<Comparison>> notList   = new ArrayList<List<Comparison>>();
        notList.add(not);
        return notList;

    }

    /**
     * Negates descendants if negationLevel is even. Increases negation level value.
     */
    @Override
    protected List<List<Comparison>> open(BooleanOperation bop, int negationLevel) {
        negationLevel++;
        
        List<List<Comparison>> bopResult = bop.open(negationLevel);
        for(List<Comparison> s: bopResult)
            for(Comparison c: s)
                if(c.negationLevel() == 0)
                    c.negationLevel(negationLevel);
                
        if(!shouldNegate(negationLevel))
            return bopResult;

        List<List<Comparison>> result = new ArrayList<List<Comparison>>();
        for(List<Comparison> s: bopResult){
            List<Comparison> untouched = null;
            
            for(Comparison c: s){
                
                if(c.negationLevel() == negationLevel){
                    List<Comparison> negated = new ArrayList<Comparison>();
                    c.negated(true);
                    negated.add(c);
                    result.add(negated);
                    continue;
                }
                
                if(untouched == null) untouched = new ArrayList<Comparison>();
                untouched.add(c);
            }
            
            if(untouched != null)
                result.add(untouched);
        }
        
        
        return result;
    }
    
    
    /**
     * returns <code>true</code> if given instance should negate expression.
     * This is a case if negationLevel is odd.
     * @param negationLevel
     * @return
     */
    private boolean shouldNegate(int negationLevel){
        return 2*(negationLevel/2) != negationLevel;
    }
    
}

/*
 * $Log: NotOp.java,v $
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