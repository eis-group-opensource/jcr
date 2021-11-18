/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import com.exigen.cm.impl.NodeTypeImpl;

/**
 * Holds FTS condition data.
 */
public class FTSCondition extends Comparison {

    private ComparisonType.FTS type;
    private String value;
    
    FTSCondition(ComparisonType.FTS type, String value){
        super(null);
        
        this.type=type;
        this.value=value;
    }
    
    /**
     * Returns FTS condition value.
     * @return
     */
    String getValue(){
        return value;
    }
    
    /**
     * Returns FTS condition type.
     * @return
     * @see ComparisonType#FTS
     */
    ComparisonType.FTS getType(){
        return type;
    }


    /**
     * Throws UnsupportedOperationException.
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) {
        throw new UnsupportedOperationException("Operation not supported by FTS Comparison");
    }

}

/*
 * $Log: FTSCondition.java,v $
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