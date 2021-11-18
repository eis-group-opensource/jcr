/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;




/**
 * Defines binary comparison of type <attribute> <op> <value>
 */
abstract class BinaryComparison extends Comparison{
    
    private final ComparisonType.BINARY comparisonType;
    private Object value;
    
    protected BinaryComparison(/*PROPERTY_TYPE attrType,*/ ComparisonType.BINARY comparisonType){//, Sequence seq){
        this(null, /*PROPERTY_TYPE.JCR_NAME,*/ comparisonType, null);//, seq);
    }
    
    protected BinaryComparison(String attributeName, /*PROPERTY_TYPE filterType,*/ ComparisonType.BINARY comparisonType, Object value){//, Sequence seq){
        super(attributeName);//, filterType);//, seq);
        this.value=value;
        this.comparisonType=comparisonType;
    }
    
    
    /**
     * Returns binary comparison value.
     * @return
     */
    protected Object value(){
        return value;
    }
    
    /**
     * Sets binary comparison value.
     * @param value
     */
    protected void value(Object value){
        this.value = value;
    }
    
    
    /**
     * Returns comparison type.
     * @return
     */
    protected ComparisonType.BINARY getComparisonType(){
        return comparisonType;
    }
}
/*
 * $Log: BinaryComparison.java,v $
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