/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.query.PropertyData;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.BuildingContext.DBQName;


/**
 * Defines IS NOT NULL comparison.
 * 
 */
class IsNotNullComparison extends UnaryComparison {
    private static Log log = LogFactory.getLog(IsNotNullComparison.class);
    
    
    IsNotNullComparison(String attribute){//, Sequence seq) {
        super(attribute, /*PROPERTY_TYPE.GENERAL,*/ ComparisonType.UNARY.IS_NOT_NULL);//, seq);
    }
    
    /**
     * Generates IS NOT NULL SQL statement for specified property.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        if(negated()) 
            return new IsNullComparison(getPropertyName()).createFilterData(context, contextType);
        
        PropertyData fpData = context.lookupPropertyDefinitions(getPropertyName(), contextType);
        List<PropertyDefinitionImpl> propDefs = fpData.toList();
        
        FilterSQL filterData = new FilterSQL();
        if(propDefs.size()==1)
            createSimple(context, propDefs.get(0), filterData);
        else
         if(propDefs.size()>1)
            createInquery(context, propDefs, filterData);
        else{
            String message = MessageFormat.format("No property definitions found for {0} in a context of node {1} with node type {2}"
                    , context.getContextNode().isWildcard()?"*":context.getContextNode().getName()
                            ,getPropertyName(),
                            contextType != null ? contextType.getName() : contextType != null ? contextType.getName() : "UNDEFINED");

            log.error(message);
            throw new RuntimeException(message);
        }

        return filterData;
    }   

    
    /*
        from CMV_QDATA1 
        where X_TYPE1_VALUE IS NOT NULL

    OR
        from CM_NODE_UNSTRUCTURED 
        where name='type1_value'
     */
    /**
     * Generates  IS NOT NULL SQL statement for simple case when single property definition 
     * for property name used in this comparison is found.
     * @throws RepositoryException 
     */
    private void createSimple(FilterContext context, PropertyDefinitionImpl propDef, FilterSQL filterData) throws RepositoryException {
//        StringBuilder sql = filterData.getFilterBody();
        StringBuilder alias = context.nextAlias();
            

        if(propDef.isUnstructured()){
            filterData.setMainTable(context.getOwner().getBuildingContext().getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED));
            filterData.getWhere()
            .append(QueryUtils.asPrefix(alias))
            .append(context.getRealColumnName(Constants.FIELD_NAME)).append("=? AND ")
            .append(QueryUtils.asPrefix(alias))
            .append(context.getRealColumnName(Constants.FIELD_NAMESPACE));

            DBQName dbQName = context.getOwner().getBuildingContext().toDBQname(getPropertyName());
            filterData.addParameter(dbQName.getLocalName());

            if(!dbQName.hasNamespace()){
                filterData.getWhere().append(" IS NULL");
            }else{
                filterData.getWhere().append("=?");
                filterData.addParameter(dbQName.getNamespaceId());
            }
        }else{
            NodeTypeImpl declaringType = (NodeTypeImpl)propDef.getDeclaringNodeType();
            String tableName = context.getOwner().getBuildingContext().getRealTableName(declaringType.getTableName());

            filterData.setMainTable(context.getOwner().getBuildingContext().getRealTableName(tableName));
            filterData.getWhere()
            .append(QueryUtils.asPrefix(alias))
            .append(context.getRealColumnName(propDef.getColumnName())).append(" IS NOT NULL AND ")
            .append(QueryUtils.asPrefix(alias))
            .append(declaringType.getPresenceColumn()).append("=?");
            
            filterData.addParameter(true);                
        }
        
        filterData.setMainAlias(alias.toString());
        filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_TYPE_ID));                
    }
    

    /*
     * select node_id 
        from CMV_QDATA1 
        where X_TYPE1_VALUE IS NOT NULL
        union
        select node_id 
        from CMV_QDATA2 
        where X_TYPE1_VALUE IS NOT NULL
        union 
        select node_id 
        from CM_NODE_UNSTRUCTURED 
        where name='type1_value'
     */
    /**
     * Generates internal query for case when multiple property definitions are found
     * for property name used in given comparison.
     * @throws RepositoryException 
     */
    protected void createInquery(FilterContext context, List<PropertyDefinitionImpl> propDefs, FilterSQL filterData) throws RepositoryException{
        filterData.setFilterType(FilterSQL.TYPE.INQUERY);
        
        StringBuilder sql = filterData.getFilterBody();
        
        for(int i=0; i<propDefs.size(); i++){
            PropertyDefinitionImpl pd = propDefs.get(i);
            
            if(i>0)
                sql.append(" UNION ");
            
            StringBuilder alias = context.nextAlias();
            
            sql.append("SELECT ")
            .append(QueryUtils.asPrefix(alias)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
            .append(" FROM ");
  
            if(pd.isUnstructured()){
                sql.append(context.getOwner().getBuildingContext().getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED)).append(' ').append(alias)
                
                .append(" WHERE ")
                .append(QueryUtils.asPrefix(alias))
                .append(context.getRealColumnName(Constants.FIELD_NAME)).append("=? AND ")
                .append(QueryUtils.asPrefix(alias))
                .append(context.getRealColumnName(Constants.FIELD_NAMESPACE));

                DBQName dbQName = context.getOwner().getBuildingContext().toDBQname(getPropertyName());
                filterData.addParameter(dbQName.getLocalName());

                if(!dbQName.hasNamespace()){
                    sql.append(" IS NULL");
                }else{
                    sql.append("=?");
                    filterData.addParameter(dbQName.getNamespaceId());
                }
            }else{
                NodeTypeImpl declaringType = (NodeTypeImpl)pd.getDeclaringNodeType();
                
                String tableName = context.getOwner().getBuildingContext().getRealTableName(declaringType.getTableName());
                sql.append(tableName).append(' ').append(alias)
                .append(" WHERE ")
                .append(QueryUtils.asPrefix(alias))
                .append(context.getRealColumnName(pd.getColumnName())).append(" IS NOT NULL AND ")
                .append(QueryUtils.asPrefix(alias))
                .append(declaringType.getPresenceColumn()).append("=?");
                filterData.addParameter(true);                
            }
        }

        filterData.setMainAlias(context.nextAlias().toString());
        filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_TYPE_ID));                
    }
}

/*
 * $Log: IsNotNullComparison.java,v $
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