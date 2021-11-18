/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.step;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.PathSQL.QUERY_PART;
import com.exigen.cm.query.predicate.Condition;
import com.exigen.cm.query.predicate.FilterContext;


/**
 * Represents Named Path step.
 */
class NamedPathStep extends PathStep {
//  If true defines that parent ID should be added directly to SQL.
//  If false parent ID will be added as parameter
//  Currently added for TESTING purposes only!
    private final boolean inlineDirectParentId = false;
    
    
    public NamedPathStep(String name, PathStep parent, Condition filter, boolean isDescendantOrSelf, int index) {
        super(parent, name, filter, isDescendantOrSelf,index);
    }


    @Override
    protected void fillSQL(PathSQL context) throws RepositoryException {
        FilterContext fc = hasFilter() ? new FilterContext(getStepNameAsDBQname(context), context, this) : null;
        fillContext(context, true, fc);
    }
    

    /**
     * Adds SQL constraints for Named Path step.
     * @param context
     * @param canSwitchCurrent
     * @param fc
     * @return
     * @throws RepositoryException 
     */
    protected StringBuilder fillContext(PathSQL context, boolean canSwitchCurrent, FilterContext fc) throws RepositoryException {
        Long nodeId = getDirectPathNodeId(context);
        if(nodeId!=null){
            context.where().append(" AND ").append(QueryUtils.asPrefix(context.currentAlias()));
            
            if(inlineDirectParentId){
                context.where().append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__PARENT))
                .append("=").append(nodeId);
            }else{
                context.where().append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))
                .append("=?");
                context.addParameter(nodeId, QUERY_PART.WHERE);
            }
            addFilteringSQL(context, fc);            
            return null;
        }
        
        addContextNodeConstraintByName(context);
        addFilteringSQL(context, fc);
        addOrderingReferences(context, fc);
        
        StringBuilder parentAlias;
        if(isDescendantOrSelf())
            parentAlias=fillDescendantOrSelf(context);
        else
            parentAlias=fillDirectChild(context);

        if(parentAlias!=null && canSwitchCurrent){
            context.currentAlias(parentAlias);
            getParent().fillSQL(context);
        }
        
        return parentAlias;
    }
    
    
    /**
     * Adds SQL constraints specific for step pointing to descendants.
     * @param context
     * @return
     * @throws RepositoryException 
     */
    private StringBuilder fillDescendantOrSelf(PathSQL context) throws RepositoryException{
        if(isLast()) return null;

        /*
        join cm_node_parents   n1 on n1.node_id = n.id        
        join cm_node           n2 on n2.id      = n1.parent_id        
        */

        StringBuilder parentsAlias = context.nextAlias();
        StringBuilder parentAlias = context.nextAlias();
        
        context.from()
        .append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE_PARENT)).append(' ').append(parentsAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(parentsAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_TYPE_ID))
        .append('=')
        .append(QueryUtils.asPrefix(context.currentAlias()))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))        
        .append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE)).append(' ').append(parentAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(parentAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))
        .append('=')
        .append(QueryUtils.asPrefix(parentsAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__PARENT));
        
        return parentAlias;
    }
    
    /**
     * Add SQL constraints for step pointing to direct child.
     * @param context
     * @return
     * @throws RepositoryException 
     */
    private StringBuilder fillDirectChild(PathSQL context) throws RepositoryException{

        if(isLast()){
            /*
             * For last()
             *  and n1.depth=1 
             */
            context.where()
            .append(" AND ")
            .append(QueryUtils.asPrefix(context.currentAlias()))
            .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__NODE_DEPTH)).append("=?");
            
            context.addParameter(1, QUERY_PART.WHERE);
            return null;
        }

        
        Long directParentId = getParent().getDirectPathNodeId(context);
        if(directParentId != null && !getParent().hasFilter()){
//          If there is parent with direct path and this parent has no filter refer it through its ID
//          If there is parent with filter it will be processed on given parent step building
//          and it cannot be processed here because filter tables should be joined to parent CM_NODE ref
            context.where().append(" AND ").append(QueryUtils.asPrefix(context.currentAlias()));
            if(inlineDirectParentId){
                context.where().append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__PARENT)).append("=")
                .append(directParentId);
            }else{
                context.where().append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__PARENT)).append("=?");
                context.addParameter(directParentId, QUERY_PART.WHERE);
            }
            return null;
        }
        
        
        /* join CM_NODE n1 ON n.parent_id=n1.id */
        StringBuilder parentAlias = context.nextAlias();
        
        context.from().append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE)).append(' ').append(parentAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(parentAlias)).append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))
        .append('=')
        .append(QueryUtils.asPrefix(context.currentAlias()))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__PARENT));

        return parentAlias;
    }
}

/*
 * $Log: NamedPathStep.java,v $
 * Revision 1.2  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 09:01:09  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/20 16:19:16  maksims
 * #1803635 javadocs added
 *
 * Revision 1.1  2006/12/15 13:13:21  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.6  2006/12/13 14:27:08  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.5  2006/11/29 13:10:20  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.4  2006/11/28 09:33:42  maksims
 * #1802721 tests fixed
 *
 * Revision 1.3  2006/11/23 14:55:29  maksims
 * #1802721 reference  to direct parent simplified
 *
 * Revision 1.2  2006/11/17 10:17:28  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.1  2006/11/02 17:28:14  maksims
 * #1801897 Query2 addition
 *
 */