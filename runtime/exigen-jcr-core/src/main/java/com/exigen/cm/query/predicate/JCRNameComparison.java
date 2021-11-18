/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.QueryUtils;


/**
 * Comparison for JCR Name test.
 */
class JCRNameComparison extends BinaryComparison {
    
    private final String nodeName;
    
    JCRNameComparison(String nodeName, ComparisonType.BINARY comparisonType){//, Sequence seq) {
        super(/*PROPERTY_TYPE.JCR_NAME,*/ comparisonType);//, seq);
        if(nodeName== null)
            throw new IllegalArgumentException("NULL cannot be used for as a jcr:name");

        this.nodeName=QueryUtils.stripQuotes(nodeName);
    }
    
    /**
     * Validates node name comparison.
     */
    @Override
    public void validate() {
        switch(getComparisonType()){
            case EQUALS:
            case NOT_EQUALS:
            case LIKE:                
                return;
            default:
                String message = MessageFormat.format("comparison {0} cannot be used for jcr:name", getComparisonType());
                throw new IllegalArgumentException(message);
        }
    }

    /**
     * Creates SQL related to node name comparison.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        FilterSQL filterData = new FilterSQL();

        String alias;
        if(context.canUseOwnerTable()){
            filterData.setFilterType(FilterSQL.TYPE.DIRECT);
            alias = context.getOwnerAlias();
        }else{
            alias = context.nextAlias().toString();
            
            filterData.setMainAlias(alias);
            filterData.setMainTable(context.getRealTableName(Constants.TABLE_NODE));
            filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_ID));
        }
        
        
        BuildingContext bContext = context.getOwner().getBuildingContext();
        boolean comparisonLike = getComparisonType() == ComparisonType.BINARY.LIKE;
        
        BuildingContext.DBQName dbQName = comparisonLike 
                                            ? bContext.toDBQname(nodeName, false) // no caching
                                            : bContext.toDBQname(nodeName);
        
        StringBuilder where = filterData.getWhere(); 
        where
        .append(QueryUtils.asPrefix(alias))
        .append(context.getRealColumnName(Constants.FIELD_NAME))
        .append(getComparisonType().toSQL(negated()))
        .append('?');
        
        if(comparisonLike)
            where.append(LikeComparison.ESCAPE_STATEMENT);
        
        
        where
        .append(" AND ")
        .append(QueryUtils.asPrefix(alias))
        .append(context.getRealColumnName(Constants.FIELD_NAMESPACE));

        String localName = dbQName.getLocalName();
        if(comparisonLike)
            localName = bContext.getDialect().adjustLikeParameter(localName, LikeComparison.ESCAPE_CHAR);
        
        
        filterData.addParameter(localName);
        

        if(!dbQName.hasNamespace()){
            filterData.getWhere().append(" IS NULL");
        }else{
            filterData.getWhere()
            .append(   (comparisonLike 
                        ? ComparisonType.BINARY.EQUALS 
                        : getComparisonType())
                        .toSQL(negated()))
            .append('?');

            filterData.addParameter(dbQName.getNamespaceId());
        }
        
        return filterData;
    }    
}

/*
 * $Log: JCRNameComparison.java,v $
 * Revision 1.3  2008/04/25 09:32:13  maksims
 * #1805668 Like made applicable to jcr:name local part. Namespace should always be provided
 *
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
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