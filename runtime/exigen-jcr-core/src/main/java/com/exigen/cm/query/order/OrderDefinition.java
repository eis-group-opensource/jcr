/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.order;

import javax.jcr.RepositoryException;

import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.step.PathStep;



/**
 * Holds ordering data.
 */
public abstract class OrderDefinition {
    
    public enum ORDER {ASC, DESC};
    public static ORDER ORDER_DEFAULT = ORDER.ASC;
    
    private final ORDER order;
    
    /**
     * Postfix for order values generated on the base of common aliases ...
     */
    private static final String ORDER_VALUE = "ORDER_VAL";
    
    protected OrderDefinition(ORDER order){
        this.order = order;
    }
    

    /**
     * Returns Order.ASC or Order.DSC constant.
     * @return
     */
    public ORDER getOrder(){
        return order;
    }
    

    /**
     * Returns list of columns referred by Order SQL
     * Name of column corresponding to given ordering.
     * Name is assigned by PathStep which first performs conversion of given ordering to SQL.
     * Name prefixed by Path SQL subquery alias used for referring in SELECT, ORDER BY and GROUP BY
     * @return
     */
    public abstract String[] getReferredColumns();

    
    /**
     * Generates SQL for given Ordering
     * uses information about available NodeTypes from <code>fc</code>
     * if available and placing generation result into <code>target</code>
     * @param owner path step owning given property definition.
     * @param target
     * @param fc
     * @throws RepositoryException 
     */
    public abstract void toSQL(PathStep owner, PathSQL target, FilterContext fc) throws RepositoryException;
    
    /**
     * Generates next Order Value Alias on the base of commonAlias
     * @param target
     * @return
     */
    protected String getNextOrderValueAlias(PathSQL target){
        return target.getBuildingContext().nextAlias().append(ORDER_VALUE).toString();
    }
}

/*
 * $Log: OrderDefinition.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
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