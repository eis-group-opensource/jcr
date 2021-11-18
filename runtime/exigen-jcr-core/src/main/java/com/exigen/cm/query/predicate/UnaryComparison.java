/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;





/**
 * Defines unary comparison.
 */
abstract class UnaryComparison extends Comparison{

    private final ComparisonType.UNARY comparisonType;
    
    protected UnaryComparison(String attribute, /*PROPERTY_TYPE attrType,*/ ComparisonType.UNARY comparison){//, Sequence seq){
        super(attribute);//, attrType);//, seq);
        comparisonType=comparison;
    }
    
    /**
     * Returns Unary comparison type.
     * @return ComparisonType.UNARY
     * @see
     */
    protected ComparisonType.UNARY getType(){
        return comparisonType;
    }
    
    
//    public void toSQL(FilterContext context, NodeTypeImpl contextType){
//        context.target().append(attributeName()).append(comparisonType);
//   }
}

/*
 * $Log: UnaryComparison.java,v $
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