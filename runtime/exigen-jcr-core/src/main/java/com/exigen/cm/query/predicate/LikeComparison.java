/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import javax.jcr.RepositoryException;

import com.exigen.cm.impl.NodeTypeImpl;


/**
 * Like comparison for general property.
 */
class LikeComparison extends GeneralPropertyComparison {
    public final static char ESCAPE_CHAR = '\\';
    public final static String ESCAPE_STATEMENT=" ESCAPE '" + ESCAPE_CHAR + "'";
    
    
    LikeComparison(String attributeName, Object value){//, Sequence seq) {
        super(attributeName, ComparisonType.BINARY.LIKE, value);//, seq);
    }
    
    /**
     * Creates LIKE comparison SQL using provided string pattern adjusted
     * by current DB dialect.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        String value = context.getOwner().getBuildingContext().getDialect().adjustLikeParameter((String)value(), ESCAPE_CHAR);
        return super.createFilterData(context, contextType, value, ESCAPE_STATEMENT);
    } 
    
    /**
     * Returns SQL LIKE postfix.
     */
    protected String getPostfix(){
        return ESCAPE_STATEMENT;
    }
}

/*
 * $Log: LikeComparison.java,v $
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
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */