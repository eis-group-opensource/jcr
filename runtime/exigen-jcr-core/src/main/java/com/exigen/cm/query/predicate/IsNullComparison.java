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
 * Defines IS NOT comparison.
 */
class IsNullComparison extends UnaryComparison {
    private static Log log = LogFactory.getLog(IsNullComparison.class);
    
    
    IsNullComparison(String attribute){//, Sequence seq) {
        super(attribute, /*PROPERTY_TYPE.GENERAL,*/ ComparisonType.UNARY.IS_NULL);//, seq);
    }
    
    /**
     * Generates SQL for IS NULL statement.
     * @throws RepositoryException 
     */
    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        if(negated()) 
            return new IsNotNullComparison(getPropertyName()).createFilterData(context, contextType);
        
        FilterSQL filterData = new FilterSQL();
        PropertyData fpData = context.lookupPropertyDefinitions(getPropertyName(), contextType);
        List<PropertyDefinitionImpl> propDefs = fpData.toList();
//        List<PropertyDefinitionImpl> propDefs = context.lookupPropertyDefinitions(getPropertyName(), contextType);

        
        /*
         * It looks there is no INQUERY mode ... but a complex query!!!
         * See cases below ...
         */
         if(propDefs.size()==0){
            String message = MessageFormat.format("No property definitions found for {0} in a context of node {1} with node type {2}"
                    , context.getContextNode().isWildcard()?"*":context.getContextNode().getName()
                            ,getPropertyName(),
                            contextType != null ? contextType.getName() : contextType != null ? contextType.getName() : "UNDEFINED");

            log.error(message);
            throw new RuntimeException(message);
        }


        StringBuilder currentJoiningColumn = null;
         
        StringBuilder where = filterData.getWhere();
        StringBuilder joins = filterData.getMainTableJoins();
         
        for(int i=0; i<propDefs.size(); i++){
            PropertyDefinitionImpl pd = propDefs.get(i);
             
            String tableName = context.getRealTableName(pd.isUnstructured() ? Constants.TABLE_NODE : ((NodeTypeImpl)pd.getDeclaringNodeType()).getTableName());
            StringBuilder alias = context.nextAlias();
            if(i == 0){
                filterData.setMainTable(tableName);
                filterData.setMainAlias(alias.toString());
                String joinToParentCol = context.getRealColumnName(pd.isUnstructured() ? Constants.FIELD_ID : Constants.FIELD_TYPE_ID);
                filterData.setJoiningColumn(joinToParentCol);
                 
                currentJoiningColumn = QueryUtils.asPrefix(alias).append(joinToParentCol);
                 
                if(!pd.isUnstructured())
                    filterData.setParentJoinType(FilterSQL.JOIN.LEFT);
            }else{
                where.append(" AND ");
                 
                 
                if(!pd.isUnstructured()){
                    StringBuilder newJoiningColumn = QueryUtils.asPrefix(alias).append(context.getRealColumnName(Constants.FIELD_TYPE_ID));
                    
                    joins.append(" LEFT OUTER JOIN ")
                    .append(tableName).append(' ').append(alias)
                    .append(" ON ")
                    .append(newJoiningColumn)
                    .append('=')
                    .append(currentJoiningColumn);
                     
                    currentJoiningColumn = newJoiningColumn;
                }
            }

             
             /*
              * Case 1: Regular Property
              *  left join cmv_qdata3 d2 on n.ID = d2.NODE_ID 
                 where d1.x_type1_value IS NULL 
              *
              * Case 2: Unstructured
              *  join cm_node n1 on n1.id=n.id 
                 where not exists (select node_id 
                     from cm_node_unstructured 
                     where name='type1_value' 
                     and node_id=n1.id)
                     
              * Case 3: combined
                 CM_NODE n
                 JOIN cm_node n1 ON n1.ID=n.ID 
                 left join cmv_qdata_all d1 on n.ID = d.NODE_ID 
                 where d1.x_type1_value IS NULL 
                     and d1.x_type3_value IS NULL 
                     and not exists (select node_id 
                         from cm_node_unstructured 
                         where name='type1_value' 
                         and node_id=n1.id)
                         
                         
                 CM_NODE n
                 left join cmv_qdata_all d1 on n.ID = d.NODE_ID
                 JOIN cm_node n1 ON n1.ID=d1.NODE_ID
                 where d1.x_type1_value IS NULL 
                     and d1.x_type3_value IS NULL 
                     and not exists (select node_id 
                         from cm_node_unstructured 
                         where name='type1_value' 
                         and node_id=n1.id)
                         
              */               
             
             if(pd.isUnstructured()){
                 where.append("NOT EXISTS ( SELECT ")
                 .append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
                 .append(" FROM ")
                 .append(context.getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED))
                 .append(" WHERE ")
                 .append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
                 .append('=')
                 .append(currentJoiningColumn)
                 .append(" AND ")
                 .append(context.getRealColumnName(Constants.FIELD_NAME))
                 .append("=? AND ")
                 .append(context.getRealColumnName(Constants.FIELD_NAMESPACE));

                 DBQName dbQName = context.getOwner().getBuildingContext().toDBQname(getPropertyName());
                 filterData.addParameter(dbQName.getLocalName());

                 if(!dbQName.hasNamespace()){
                     where.append(" IS NULL");
                 }else{
                     where.append("=?");
                     filterData.addParameter(dbQName.getNamespaceId());
                 }
                 where.append(')');
             }else{
                 where.append(QueryUtils.asPrefix(alias))
                 .append(context.getRealColumnName(pd.getColumnName()))
                 .append(" IS NULL");
             }
         }
         
        return filterData;    
    }
    
    
    

          
}

/*
 * $Log: IsNullComparison.java,v $
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