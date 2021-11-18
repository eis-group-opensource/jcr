/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.QueryUtils;


/**
 * Implements position comparison.
 */
class PositionComparison extends BinaryComparison {

    private final int position;
    
    protected PositionComparison(int position, ComparisonType.BINARY comparisonType){//, Sequence seq) {
        
        super(/*PROPERTY_TYPE.POSITION,*/ comparisonType);//, seq);
        this.position = position;
    }

    /**
     * Generates SQL for position related comparison.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
//        if(position == 0)// last is a very special case... and procesed separately
//            return createLast(context);
        
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
        .append(context.getRealColumnName(Constants.TABLE_NODE__INDEX))        
        .append(getComparisonType().toSQL(negated()));

        if(position == 0){ // last
            filterData.getWhere()
            .append(QueryUtils.asPrefix(alias))
            .append(context.getRealColumnName(Constants.TABLE_NODE__INDEX_MAX));
        }else{
            filterData.getWhere().append('?');
            filterData.addParameter(position);
        }
        
        return filterData;
    } 
    
    /**
     * Throws exception if comparison isn't (=|!=|<|>|=<|>=)
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
                String message = MessageFormat.format("comparison {0} cannot be used for position()", getComparisonType());
                throw new IllegalArgumentException(message);
        }
    }
    
    
    
    /*
     * Case 1: ns:claim1[last()]
     * select n.id from CM_NODE n
       join(
        select n.id, n.pos
        from cm_node n join cm_node x 
            on  x.parent_id=n.parent_id         
                  and x.name= n.name  
                  and x.namespace=n.namespace
        where n.name='.claim1'
            and n.namespace = :nsId
        group by n.id, n.pos 
        having max(x.pos)=n.pos
        ) a on  n.id=a.id
     * 
     * 
     * Case 2: claim1[last()]
     * select n.id from CM_NODE n
       join(
        select n.id, n.pos
        from cm_node n join cm_node x 
            on  x.parent_id=n.parent_id         
                  and x.name= n.name  
                  and x.namespace IS NULL
        where n.name='.claim1'
            and n.namespace IS NULL
        group by n.id, n.pos 
        having max(x.pos)=n.pos
        ) a on  n.id=a.id
     * 
     * 
     * Case 3: *[position()=last()]
     * select n.id from CM_NODE n
       join(
        select n.id, n.pos
        from cm_node n join cm_node x 
            on  x.parent_id=n.parent_id         
                  and x.name= n.name  
                  and (x.namespace=n1.namespace 
                          or x.namespace IS NULL 
                              and n1.namespace IS NULL
                          )
        group by n.id, n.pos 
        having max(x.pos)=n.pos
        ) a on  n.id=a.id
     * 
     */
}

/*
 * $Log: PositionComparison.java,v $
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