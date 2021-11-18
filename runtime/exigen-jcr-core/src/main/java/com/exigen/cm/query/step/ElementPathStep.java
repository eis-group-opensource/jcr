/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.step;

import java.text.MessageFormat;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.Condition;
import com.exigen.cm.query.predicate.FilterContext;

/**
 * Represents element(name, nodeType) path step.
 */
class ElementPathStep extends NamedPathStep {
    private final String nodeType;
    private final Log log = LogFactory.getLog(ElementPathStep.class);
    
    
    public ElementPathStep(String name, String nodeType,PathStep parent, Condition filter, boolean isDescendantOrSelf, int index) {
        super(name, parent, filter, isDescendantOrSelf, index);
        this.nodeType=QueryUtils.decodeEntity(nodeType); // Should test that nodeType is primaryNodeType and not mix-in!
    }

    /**
     * Returns node type name or <code>null</code> if node type name isn't specified.
     */
    @Override
    public String getNodeTypeName(){
        return nodeType;
    }

    
    /**
     * Generates SQL binding current node step with it parent and add constraint by node type.
     * @throws RepositoryException 
     */
    @Override
    public void fillSQL(PathSQL context) throws RepositoryException {
        NodeTypeImpl type = context.getBuildingContext().getNodeTypeDefinition(getNodeTypeName());
        if(type.isMixin()){
            String message = MessageFormat.format("{0} is mix-in type and cannot be used in element()",
                    getNodeTypeName());
            log.error(message);
            throw new RuntimeException(message);
        }
        
//      Register node type.
        addNodeType(type);
        
        StringBuilder typeTableAlias = context.nextAlias();        
        FilterContext fc = hasFilter() ? new FilterContext(getStepNameAsDBQname(context), type, context, typeTableAlias, this) : null;
        StringBuilder parentAlias = fillContext(context, false, fc);

        /*  join CMV_NODE_TYPE n1 on n1.node_id=n.id  */
        /*  and n1.NODE_TYPE_ON=true                */
        
//        StringBuilder typeTableAlias = context.nextAlias();
        
        
        String tableName = type.getTableName();
        String columnName = type.getPresenceColumn();

        context.from().append(" JOIN ")
        .append(context.getBuildingContext().getRealTableName(tableName)).append(' ').append(typeTableAlias)
        .append(" ON ")
        .append(QueryUtils.asPrefix(typeTableAlias)).append(context.getBuildingContext().getRealColumnName(Constants.FIELD_TYPE_ID))
        .append('=')
        .append(QueryUtils.asPrefix(context.currentAlias())).append(context.getBuildingContext().getRealColumnName(Constants.FIELD_ID));
        
        context.where()
        .append(" AND ").append(QueryUtils.asPrefix(typeTableAlias)).append(context.getBuildingContext().getRealColumnName(columnName))
        .append("=?");

        context.addParameter(true, PathSQL.QUERY_PART.WHERE);

        if(parentAlias == null) // if parent is already added as direct path
            return;

        context.currentAlias(parentAlias);
        
        if(!isLast())
            getParent().fillSQL(context);
    }
    
}

/*
 * $Log: ElementPathStep.java,v $
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
 * Revision 1.7  2006/12/13 14:27:09  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.6  2006/11/29 13:10:20  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.5  2006/11/17 10:17:28  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.4  2006/11/16 13:56:16  maksims
 * #0149157 decoding implemented
 *
 * Revision 1.3  2006/11/16 12:35:45  maksims
 * #1802721 decoding of type names implemented
 *
 * Revision 1.2  2006/11/06 13:11:01  maksims
 * #1801897 direct path processing fixed
 *
 * Revision 1.1  2006/11/02 17:28:14  maksims
 * #1801897 Query2 addition
 *
 */