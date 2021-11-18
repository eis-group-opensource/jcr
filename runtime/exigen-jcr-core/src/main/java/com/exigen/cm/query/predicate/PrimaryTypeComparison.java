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
 * Comparison for JCR Name test.
 */
class PrimaryTypeComparison extends BinaryComparison{
    private static final Log log = LogFactory.getLog(PrimaryTypeComparison.class);
    private final String primaryTypeName;
    
    PrimaryTypeComparison(String primaryTypeName, ComparisonType.BINARY comparisonType){//, Sequence seq) {
        super(/*PROPERTY_TYPE.JCR_NAME,*/ comparisonType);//, seq);
        if(primaryTypeName== null)
            throw new IllegalArgumentException("NULL cannot be used for as a jcr:primaryType");
        
        this.primaryTypeName=QueryUtils.stripQuotes(primaryTypeName);
    }
    
    /**
     * Throws exception if comparison isn't from (=|!=)
     */
    @Override
    public void validate() {
        switch(getComparisonType()){
            case EQUALS:
            case NOT_EQUALS:
                return;
            default:
                String message = MessageFormat.format("comparison {0} cannot be used for jcr:primaryType", getComparisonType());
                throw new IllegalArgumentException(message);
        }
    }

    /**
     * Generates SQL for primary type comparison. Throws exception if node type provided
     * is mixin.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{

        if(contextType.isMixin()){
                String message = MessageFormat.format("{0} is mix-in type and cannot be used for comparison with jcr:primaryType",
                        contextType.getName());
                log.error(message);
                throw new RuntimeException(message);
        }        
        
        FilterSQL filterData = new FilterSQL();
        String alias;

        /* 
         * It is expected that contextType is the same as primaryType ...
         */        
        if(contextType == null || !getPrimaryTypeName().equalsIgnoreCase(contextType.getName())){
            String message = MessageFormat.format("Ambigous primary types expected {0} but recieved {1}",
                    getPrimaryTypeName(), contextType == null ? "NULL":contextType.getName());
            throw new RuntimeException(message);
        }

        Long typeId = contextType.getSQLId();        
        
        if(context.canUseOwnerTable()){
            filterData.setFilterType(FilterSQL.TYPE.DIRECT);
            alias = context.getOwnerAlias();
        }else{
            alias = context.nextAlias().toString();
            filterData.setMainTable(context.getRealTableName(Constants.TABLE_NODE));
            filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_ID));
        }
        filterData.setMainAlias(alias);
        
        filterData.getWhere()
        .append(QueryUtils.asPrefix(alias))
        .append(context.getRealColumnName(Constants.TABLE_NODE__NODE_TYPE))
        .append(getComparisonType().toSQL(negated()))
        .append('?');
        
        filterData.addParameter(typeId);
        
//        .append(" AND ")
//        .append(QueryUtils.asPrefix(alias))        
//        .append(context.getRealColumnName(Constants.FIELD_NAMESPACE));
//        
//        PathSQL.DBQName dbQName = context.getOwner().toDBQname(getPrimaryTypeName());
//        filterData.addParameter(dbQName.getLocalName());
//        
//        
//        if(dbQName.hasNamespace()){
//            filterData.getWhere()
//             .append(getComparisonType().toSQL(negated()))
//             .append('?');
//
//            filterData.addParameter(dbQName.getNamespaceId());
//        }else{
//            filterData.getWhere().append(negated()? "IS NOT NULL":"IS NULL");
//        }
        
        return filterData;

    }    
    
    /**
     * Returns <code>true</code> if given comparison can be used for type constraining.
     */
    @Override
    public boolean isTypeConstraining(){
        return negated() ? getComparisonType() == BINARY.NOT_EQUALS : getComparisonType() == BINARY.EQUALS;
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof PrimaryTypeComparison){
            PrimaryTypeComparison pc = (PrimaryTypeComparison)obj;
            return getComparisonType() == pc.getComparisonType() && primaryTypeName.equalsIgnoreCase(pc.primaryTypeName);
        }
        return false;
    }
    
    /**
     * Returns primary type name.
     * @return
     */
    public String getPrimaryTypeName(){
        return primaryTypeName;
    }
    
    /**
     * Returns primary type name.
     * @return
     */
    @Override
    public String toString() {
        return getPrimaryTypeName();
    }
}

/*
 * $Log: PrimaryTypeComparison.java,v $
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
 * Revision 1.3  2006/11/16 13:55:49  maksims
 * #0149156 Disabled to use mixins in primaryType
 *
 * Revision 1.2  2006/11/09 12:08:13  maksims
 * #1801897 comments added
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */