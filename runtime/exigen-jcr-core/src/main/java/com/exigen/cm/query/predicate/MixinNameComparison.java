/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.ComparisonType.BINARY;


/**
 * Comparison for JCR MixinTypes test.
 */
class MixinNameComparison extends BinaryComparison {
    
    private static final Log log = LogFactory.getLog(MixinNameComparison.class);
    
    private String mixinName;
    
    MixinNameComparison(String mixinName, ComparisonType.BINARY comparisonType){//, Sequence seq) {
        super(/*PROPERTY_TYPE.JCR_MIXINS,*/ comparisonType);//, seq);
        if(mixinName == null)
            throw new IllegalArgumentException("NULL cannot be used for as a jcr:mixinTypes");
        
        this.mixinName = QueryUtils.stripQuotes(mixinName);
    }
    
    /**
     * Generates SQL for MixIn name comparison.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        
        NodeTypeImpl mix = context.getOwner().getBuildingContext().getNodeTypeDefinition(mixinName);
        if(!mix.isMixin()){
            String message = MessageFormat.format("{0} node type isn't a mix-in type and cannot be used as jcr:mixinTypes parameter.",
                    mixinName);
            log.error(message);
            throw new IllegalArgumentException(message);
        }

        context.propagateNodeType(mix);
        
        FilterSQL filterData = new FilterSQL();

        String alias1 = context.nextAlias().toString(); // type
        String alias2; // node
        

        
        /*         
         * Selects mixins which were added to node instance on runtime and
         * not inherited from some parent node type:
         * 
         * select type.NODE_TYPE_ID
           from CM_NODE node join CM_TYPE type on type.NODE_ID=node.ID
           WHERE type.FROM_NODE_TYPE_ID <> node.NODE_TYPE_ID AND type.NODE_TYPE_ID=type.FROM_NODE_TYPE_ID
         * 
         */
        
        if(context.canUseOwnerTable()){
            alias2 = context.getOwnerAlias();
            
            filterData.setMainAlias(alias1);
            
            filterData.setMainTable(context.getRealTableName(Constants.TABLE_TYPE));
            filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_TYPE_ID));
            
        }else{
            alias2 = context.nextAlias().toString();            
            filterData.setMainAlias(alias2);
            filterData.setMainTable(context.getRealTableName(Constants.TABLE_NODE));
            filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_ID));
            

            
            filterData.getMainTableJoins()
            .append(" JOIN ")
            .append(context.getRealTableName(Constants.TABLE_TYPE))
            .append(' ').append(alias1).append(" ON ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
            .append('=')
            .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.FIELD_ID));
        }
        
        filterData.getWhere()
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__FROM_NODE_TYPE))
            .append("<>")
            .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.TABLE_NODE__NODE_TYPE))
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__NODE_TYPE))
            .append("=")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__FROM_NODE_TYPE))
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_TYPE__NODE_TYPE))
            .append(getComparisonType().toSQL(negated()))
            .append('?');

            
        filterData.addParameter(mix.getSQLId());

        
//        filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_TYPE_ID));
//        filterData.setMainTable(context.getRealTableName(mix.getTableName()));
        
//        filterData.getWhere()
//        .append(PathSQL.asPrefix(alias))
//        .append(context.getRealColumnName(mix.getPresenceColumn()))
//        .append("=?");
        
//        filterData.addParameter(true);
        
        return filterData;
    }
    
    /**
     * Return <code>true</code> if given comparison can be used for constraining by mixin name.
     */
    @Override
    public boolean isMixinConstraining() {
        return negated() ? getComparisonType() == BINARY.NOT_EQUALS : getComparisonType() == BINARY.EQUALS;
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public void validate() {
        switch(getComparisonType()){
            case EQUALS:
            case NOT_EQUALS:
                return;
            default:
                String message = MessageFormat.format("comparison {0} cannot be used for jcr:mixinTypes", getComparisonType());
                throw new IllegalArgumentException(message);
        }
    }

}

/*
 * $Log: MixinNameComparison.java,v $
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
 * Revision 1.2  2006/12/13 14:27:13  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */