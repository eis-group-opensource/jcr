/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.step;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.Condition;
import com.exigen.cm.query.predicate.FilterContext;

/**
 * Represents jcr:deref(@referrinfAttribute, targetNodeName) path step.
 */
public class DerefPathStep extends PathStep {
    private final String referringAttribute;
    
    public DerefPathStep(String referringAttribute, String targetNodeName, PathStep parent, Condition filter, boolean isDescendantOrSelf, int index) {
        super(parent, targetNodeName, filter, isDescendantOrSelf, index);
        
        this.referringAttribute=referringAttribute;
    }

    @Override
    protected boolean canUseAsDirect() {
        return false;
    }


    /*
 ns:nodeName/jcr:deref(@refAttr,'ns:name')
    
       join cm_node_reference n1 on n1.to_id=n.id 
       join cm_node n2 on n2.id=n1.from_id 
        
       and n1.property_name='refattr'
       and n1.propertyNamespace=namespaceId
       
    --- If not wildcard ---
       and n.node_name='name'
       and n.namespace=:nsId

 ns:nodeName//jcr:deref(@refAttr,*)
    
        join cm_node_reference n1 on n1.to_id = n.id 
        join cm_node_parents n2 on n2.node_id = n1.from_id
        join cm_node n3 on n3.id= n2.parent_id
    
        and n1.property_name='refattr'
        and n1.propertyNamespace=namespaceId
       
    --- If not wildcard ---
        and n.node_name='name'
        and n.namespace=:nsId
    
    */
    
    /**
     * Builds SQL binding current path step with path step to node which refers given via property specified.
     * @throws RepositoryException 
     */
    @Override
    public void fillSQL(PathSQL context) throws RepositoryException {
        StringBuilder referenceAlias = context.nextAlias();
        
        context.where()
        .append(" AND ")
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_REFERENCE__PROPERTY_NAME))
        .append("=?")
        .append(" AND ")
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE));
        
        BuildingContext.DBQName raQName = context.getBuildingContext().toDBQname(referringAttribute);
        context.addParameter(raQName.getLocalName(), PathSQL.QUERY_PART.WHERE);
        
        if(raQName.hasNamespace()){ 
            context.where().append("=?");
            context.addParameter(raQName.getNamespaceId(), PathSQL.QUERY_PART.WHERE);            
        }else{
            context.where().append(" IS NULL");
        }

        
        addContextNodeConstraintByName(context);
        
        
        FilterContext fc = hasFilter() ? new FilterContext(getStepNameAsDBQname(context), context, this) : null;
        addFilteringSQL(context, fc);
        addOrderingReferences(context, fc);
        
        StringBuilder parentAlias;
        if(isDescendantOrSelf())
            parentAlias=fillDescendantOrSelf(context, referenceAlias);
        else
            parentAlias=fillDirectChild(context, referenceAlias);

        if(parentAlias!=null){
                context.currentAlias(parentAlias);
                getParent().fillSQL(context);
        }
    }
    
    /**
     * Builds dereference SQL for case when referee to given path step is specified as
     * one of subtree nodes.
     * @param context
     * @param referenceAlias
     * @return
     * @throws RepositoryException 
     */
    protected StringBuilder fillDescendantOrSelf(PathSQL context, StringBuilder referenceAlias) throws RepositoryException{
        /*
            join cm_node_reference n1 on n1.to_id = n.id 
            join cm_node_parents n2 on n2.node_id = n1.from_id
            join cm_node n3 on n3.id= n2.parent_id
            
    if last:
            join cm_node_reference n1 on n1.to_id = n.id 
         */
        
        context.from().append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE_REFERENCE))
        .append(' ').append(referenceAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_REFERENCE__TO))
        .append('=')
        .append(QueryUtils.asPrefix(context.currentAlias()))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID));

        if(isLast())
            return null;

        StringBuilder parentsAlias = context.nextAlias();        
        StringBuilder parentAlias = context.nextAlias();

        context.from()
        .append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE_PARENT)).append(' ').append(parentsAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(parentsAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_TYPE_ID))
        .append('=')
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_REFERENCE__FROM))
        
        
        .append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE)).append(' ').append(parentAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(parentAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))
        .append('=')
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_PARENT__PARENT_ID));
        
        return parentAlias;
    }
    
    /**
     * Builds dereference SQL for case when referee to given path step speficied to be direct child of parent.
     * @param context
     * @param referenceAlias
     * @return
     * @throws RepositoryException 
     */
    protected StringBuilder fillDirectChild(PathSQL context, StringBuilder referenceAlias) throws RepositoryException{
        /*
             join cm_node_reference n1 on n1.to_id=n.id 
             join cm_node n2 on n2.id=n1.from_id 
             
     if last:
             join cm_node_reference n1 on n1.to_id=n.id 
             join cm_node n2 on n2.id=n1.from_id 
             AND n2.DEPTH=1
         */
        
        StringBuilder parentAlias = context.nextAlias();
        
        context.from().append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE_REFERENCE))
        .append(' ').append(referenceAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_REFERENCE__TO))
        .append('=')
        .append(QueryUtils.asPrefix(context.currentAlias()))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))
        
        .append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(Constants.TABLE_NODE)).append(' ').append(parentAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(parentAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID))
        .append('=')
        .append(QueryUtils.asPrefix(referenceAlias))
        .append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE_REFERENCE__FROM));
        
        if(isLast()){
            context.where()
            .append(" AND ").append(context.getBuildingContext().getRealColumnName(Constants.TABLE_NODE__NODE_DEPTH))
            .append("=?");
            
            context.addParameter(1, PathSQL.QUERY_PART.WHERE);
        }
        
        return parentAlias;
    }

    
}

/*
 * $Log: DerefPathStep.java,v $
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
 * Revision 1.3  2006/12/13 14:27:09  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.2  2006/11/17 10:17:28  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.1  2006/11/02 17:28:14  maksims
 * #1801897 Query2 addition
 *
 */