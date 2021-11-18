/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;


/**
 * Comparison for Repository ID.
 */
class NodeIdComparison extends BinaryComparison{
    NodeIdComparison(ComparisonType.BINARY comparisonType, Object value){//, Sequence seq) {
        super(null, /*PROPERTY_TYPE.JCR_ID,*/ comparisonType, value);//, seq);
    }

    /**
     * Generates SQL for Node ID comparison.
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
        
        filterData.getWhere()
        .append(QueryUtils.asPrefix(alias))
        .append(context.getRealColumnName(Constants.FIELD_ID))
        .append(getComparisonType().toSQL(negated()))
        .append('?');
        
        filterData.addParameter(value());
        
        return filterData;
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public void validate() {
        switch(getComparisonType()){
            case EQUALS:
            case NOT_EQUALS:
            case LT:                
            case GT:
            case ELT:
            case EGT:
                break;
            default:
                String message = MessageFormat.format("comparison {0} cannot be used for rep:id", getComparisonType());
                throw new IllegalArgumentException(message);
        }
        
        if(!(value() instanceof Number)){
            String message = MessageFormat.format("value {0} cannot be used to compare with rep:id. Value must be a number!", value());
            throw new IllegalArgumentException(message);
        }
    }
}

/*
 * $Log: NodeIdComparison.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
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