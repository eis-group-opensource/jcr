/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.ComparisonType.UNARY;

/**
 * Compares node mixins with nulls
 */
class MixinNullComparison extends UnaryComparison {

    MixinNullComparison(UNARY comparison){//, Sequence seq) {
        super(null, /*PROPERTY_TYPE.JCR_MIXINS,*/ comparison);//, seq);
    }

    /**
     * Generates SQL for mixin name comparisons with NULL.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        
        switch(getType()){
            case IS_NULL:
                return negated() ? makeIsNotNull(context) : makeIsNull(context);
                
            case IS_NOT_NULL:
                return negated() ? makeIsNull(context) : makeIsNotNull(context);
            default:
                String message = MessageFormat.format("MixIn Unary comparison {0} is not supported!",
                        getType());
            
                throw new RuntimeException(message);
        }
    }

    
    /**
     * Generates SQL for test that mixin name is not null.
     * @param context
     * @return
     * @throws RepositoryException 
     */
    private FilterSQL makeIsNotNull(FilterContext context) throws RepositoryException {
        FilterSQL filterData = new FilterSQL();

        String alias1 = context.nextAlias().toString(); // type
        String alias2 = context.nextAlias().toString(); // node
        
        filterData.setFilterType(FilterSQL.TYPE.INQUERY);

        StringBuilder sql = filterData.getFilterBody();
        sql.append("SELECT DISTINCT ")
        .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.FIELD_ID))
        .append(" FROM ")
        .append(context.getRealTableName(Constants.TABLE_NODE))
        .append(' ').append(alias2)
        .append(" JOIN ")
        .append(context.getRealTableName(Constants.TABLE_TYPE))
        .append(' ').append(alias1)
        .append(" ON ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
        .append('=')
        .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.FIELD_ID))
        .append(" AND ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__FROM_NODE_TYPE))
        .append("<>")
        .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.TABLE_NODE__NODE_TYPE))
        .append(" AND ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__NODE_TYPE))
        .append("=")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__FROM_NODE_TYPE));


        filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_ID));
        filterData.setMainAlias(context.nextAlias().toString());
        
        /*
         * 
         * 
         * IS NOT NULL test ...
         *    
         * Selects mixins which were added to node instance on runtime and
         * not inherited from some parent node type:
         * 
            select node.ID
            from CM_NODE node 
                join CM_TYPE type on 
                    type.NODE_ID=node.ID 
                  AND type.FROM_NODE_TYPE_ID <> node.NODE_TYPE_ID 
                  AND type.NODE_TYPE_ID=type.FROM_NODE_TYPE_ID
         */ 

        return filterData;        

    }

    /**
     * Generates SQL for test that mixin name is null.
     * @param context
     * @return
     * @throws RepositoryException 
     */
    private FilterSQL makeIsNull(FilterContext context) throws RepositoryException {
        FilterSQL filterData = new FilterSQL();
        
        String alias1 = context.nextAlias().toString(); // type
        String alias2 = context.nextAlias().toString(); // node
        
        filterData.setFilterType(FilterSQL.TYPE.INQUERY);

        StringBuilder sql = filterData.getFilterBody();
        sql.append("SELECT DISTINCT ")
        .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.FIELD_ID))
        .append(" FROM ")
        .append(context.getRealTableName(Constants.TABLE_NODE))
        .append(' ').append(alias2)
        .append(" LEFT JOIN ")
        .append(context.getRealTableName(Constants.TABLE_TYPE))
        .append(' ').append(alias1)
        .append(" ON ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
        .append('=')
        .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.FIELD_ID))
        .append(" AND ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__FROM_NODE_TYPE))
        .append("<>")
        .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.TABLE_NODE__NODE_TYPE))
        .append(" AND ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__NODE_TYPE))
        .append("=")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__FROM_NODE_TYPE))
        .append(" WHERE ")
        .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__NODE_TYPE))
        .append(" IS NULL");



        filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_ID));
        filterData.setMainAlias(context.nextAlias().toString());
        
        
        /*      
         * IS NULL test ...
         *    
         * Selects mixins which were added to node instance on runtime and
         * not inherited from some parent node type:
         * 
            select node.ID
            from CM_NODE node LEFT join CM_TYPE type on node.ID=type.NODE_ID AND type.FROM_NODE_TYPE_ID <> node.NODE_TYPE_ID AND type.NODE_TYPE_ID=type.FROM_NODE_TYPE_ID
            WHERE type.NODE_TYPE_ID IS NULL         
         *  
        //*/

        return filterData;        
    }    
}

/*
 * $Log: MixinNullComparison.java,v $
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