/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;

/**
 * Base class for comparisons.
 */
public abstract class Comparison extends Condition{// implements Comparable<Comparison>{
    
//  after simplification comparison may become negated
    private int negationLevel = 0;
    private boolean negated=false;
    private final String attributeName;
    
    protected Comparison(String attributeName){
        this.attributeName=QueryUtils.decodeEntity(attributeName);
    }

    /**
     * Creates comparison filter data which contains information
     * needed to build SQL for that comparison.
     * @param context
     * @param contextType
     * @return
     * @throws RepositoryException 
     */
    protected abstract FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException;

    /**
     * Returns property name given comparison used to compare.
     * @return
     */
    protected String getPropertyName(){
        return attributeName;
    }



    /**
     * Combines comparison with another for simultaneous processing.
     * @param c
     */
    void combine(Comparison c){
    }

    /**
     * Clears combinings if exist.
     * Usefull for cases when same comparisons after opening assembled in a difference AND sets ...
     */
    void clearCombining(){
    }
     
    /**
     * Returns comparison negation level.
     * @return
     */
    int negationLevel(){
        return negationLevel;
    }

    /**
     * Sets negation level to trace cases like not(not(c)) and not(not(not(c))) etc ...
     * @param negationLevel
     */
    void negationLevel(int negationLevel){
        this.negationLevel = negationLevel;
    }
    
    /**
     * Returns <code>true</code> if given comparison is negated.
     * @return
     */
    boolean negated(){
        return negated;
    }

    /**
     * Makes comparison negated e.g. if comparison is C then negated is NOT(C)
     * @param neg
     */
    void negated(boolean n){
        negated=n;
    }

    
    /**
     * Used in case when step filter contains single condition for the sake of uniformity ...
     */
    @Override
    public List<List<Comparison>> open() {
        List<List<Comparison>> res = new ArrayList<List<Comparison>>();
        List<Comparison> comp = new ArrayList<Comparison>();
        res.add(comp);
        comp.add(this);
        
        return res;
    }
    
    /**
     * returns value greater than 0 if compaarison result is always <code>true</code><br/>
     * returns value less than 0 if compaarison result is always <code>false</code><br/>
     * returns value 0 if compaarison result is not a constant.
     * @return
     */
    public int getConstantResult(){
        return 0;
    }
    
    /**
     * Returns <code>true</code> if given comparison constrains node type used for attribute search.
     * @return
     */
    public boolean isTypeConstraining(){
        return false;
    }

    /**
     * Returns <code>true</code> if given comparison constrains mixin type used for attribute search.
     * @return
     */
    public boolean isMixinConstraining(){
        return false;
    }    
}

/*
 * $Log: Comparison.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.2  2006/12/20 16:18:02  maksims
 * #1803572 fix for between condition for general property
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.4  2006/12/13 14:27:13  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.3  2006/11/20 16:15:46  maksims
 * #0149156 String conversion for columns fixed
 *
 * Revision 1.2  2006/11/17 10:17:30  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */