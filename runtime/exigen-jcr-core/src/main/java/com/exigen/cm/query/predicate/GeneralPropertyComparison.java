/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.PropertyData;
import com.exigen.cm.query.QueryUtils;


/**
 * Implements comparison of general type attributes.
 */
class GeneralPropertyComparison extends BinaryComparison {
    private static Log log = LogFactory.getLog(GeneralPropertyComparison.class);

    private List<GeneralPropertyComparison> combinedComparisons;
    private boolean isCombined = false;
    
    public GeneralPropertyComparison(String attributeName, ComparisonType.BINARY comparisonType, Object value){//, Sequence seq) {
        super(attributeName, comparisonType, value);
        adjustString();
    }

    /**
     * @inheritDoc
     */
    @Override
    void clearCombining() {
        if(isCombined())
            setCombined(false);
        else
            if(hasCombined())
                combinedComparisons = null;
    }
    
    /**
     * Combines comparison with another for simultaneous processing.
     * @param c
     */
    @Override
    protected void combine(Comparison c){
        if(isCombined() // if itself already added
           || c == null  // or null is provided
           || c == this
           || !(c instanceof GeneralPropertyComparison)  // or not instance of
           || !c.getPropertyName().equalsIgnoreCase(getPropertyName())) // or refer another property
            return;
        
        GeneralPropertyComparison gpc = (GeneralPropertyComparison)c;
        
       if(gpc.isCombined())
           return;
           
          
       if(combinedComparisons == null)
            combinedComparisons = new LinkedList<GeneralPropertyComparison>();
        
        
       combinedComparisons.add(gpc);
       gpc.setCombined(true);
    }
    
    /**
     * Sets flag if given instance processing is combined with other.
     * @param isCombined
     */
    private void setCombined(boolean isCombined){
        this.isCombined = isCombined;
    }
    
    /**
     * Returns <code>true</code> if given instance processing if combined with other.
     * @return
     */
    private boolean isCombined(){
        return this.isCombined;
    }
    
    /**
     * Returns <code>true</code> if while given instance processing other combined
     * comparisons should be processed too.
     * @return
     */
    private boolean hasCombined(){
        return combinedComparisons != null;
    }
    
    /**
     * Returns list of combined comparisons.
     * @return
     */
    private List<GeneralPropertyComparison> getCombinedComparisons(){
        return combinedComparisons;
    }
    
    /**
    * Updates value to Date if it is String representation of Date.
    */
    protected void adjustString(){
        if(!(value() instanceof String)) return;
        
        String value = (String) value();
        value = QueryUtils.stripQuotes(value);
        
        Date d = QueryUtils.stringToDate((String)value, false);
        if(d != null) value(d);
        else value(value);
    }

    @Override
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType) throws RepositoryException{
        return createFilterData(context, contextType, value(), "");
    }

    /**
     * Creates Comparison SQL.
     * @param context
     * @param contextType
     * @param value
     * @param postfix
     * @return
     * @throws RepositoryException 
     */
    protected FilterSQL createFilterData(FilterContext context, NodeTypeImpl contextType, Object value, String postfix) throws RepositoryException{
        if(isCombined()) // processed somewhere also
            return null;
        
        
        FilterSQL filterData = new FilterSQL();

//      Turns On hints applicability
        context.getOwner().getBuildingContext().setHintsApplicable(true);
        
        int valueType = QueryUtils.getValueType(value);
        
        PropertyData fpData = context.lookupPropertyDefinitions(getPropertyName(), valueType, contextType);
        List<PropertyDefinitionImpl> propDefs = fpData.toList();

        if(!fpData.hasUnstructuredMatches() && fpData.getExactMatchCount() == 1)
            createSimple(context, propDefs.get(0), filterData, value, postfix);
        else
            createInquery(context, propDefs, filterData, valueType, value, postfix);
        
        return filterData;
    }
    
    /**
     * Returns postfix to be appended after condition SQL statement is generated.
     * For example LIKE requires ESCAPE after it.
     * @return
     */
    protected String getPostfix(){
        return "";
    }

    /**
     * Creates SQL for simple case when just one property definition is found for name
     * used in given comparison.
     * @param context
     * @param propDef
     * @param filterData
     * @param value
     * @param postfix
     * @throws RepositoryException 
     */
    private void createSimple(FilterContext context, PropertyDefinitionImpl propDef, FilterSQL filterData, Object value, String postfix) throws RepositoryException {
        NodeTypeImpl declaringType = (NodeTypeImpl)propDef.getDeclaringNodeType();
        
        String alias;
        boolean direct = context.canUseOwnerTable() && declaringType == context.getContextType();
        String tableName = null;

        
        if(direct) {        
            filterData.setFilterType(FilterSQL.TYPE.DIRECT);
            alias = context.getContextTypeAlias().toString();
            
        }else{
            tableName = context.getOwner().getBuildingContext().getRealTableName(declaringType.getTableName());
            alias = context.getTableAlias(tableName);
            filterData.setAliasReused(alias != null);
            if(!filterData.isAliasReused()){
                alias = context.nextAlias().toString();
                context.addTableAliase(tableName, alias);
            }
        }

        StringBuilder reference = QueryUtils.asPrefix(alias)
            .append(context.getRealColumnName(propDef.getColumnName()));
        
        filterData.getWhere()
        .append(reference)
        .append(getComparisonType().toSQL(negated()))
        .append('?').append(postfix);
        filterData.addParameter(value);
        
        appendCombined(filterData.getWhere(), filterData, reference);


//      Means that conditions bound directly to owner table.  
        if(context.canUseOwnerTable())
            context.addPropertyReference(propDef.getName(), reference.toString());

        if(!direct && !filterData.isAliasReused()){
            filterData.getWhere().append(" AND ")
                .append(QueryUtils.asPrefix(alias))
                .append(context.getRealColumnName(declaringType.getPresenceColumn())).append("=?");
            
            filterData.addParameter(true);
            
            filterData.setMainTable(tableName);
            filterData.setMainAlias(alias.toString());
            filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_TYPE_ID)); 
        }
    }

    /**
     * Appends SQLs for combined comparisons.
     * @param target
     * @param filterData
     * @param reference
     */
    private void appendCombined(StringBuilder target, FilterSQL filterData, StringBuilder reference){
        if(!hasCombined())
            return;
        
        for(GeneralPropertyComparison gpc:getCombinedComparisons()){
            target
            .append(" AND ")
            .append(reference)
            .append(gpc.getComparisonType().toSQL(gpc.negated()))
            .append('?').append(gpc.getPostfix());
            
            filterData.addParameter(gpc.value());
        }        
    }
    
    /**
     * Creates internal query SQL for case when multiple property definitions are
     * found for property name used in given comparison.
     * @param context
     * @param propDefs
     * @param filterData
     * @param valueType
     * @param value
     * @param postfix
     * @throws RepositoryException 
     */
    private void createInquery(FilterContext context, List<PropertyDefinitionImpl> propDefs, FilterSQL filterData, int valueType, Object value, String postfix) throws RepositoryException {
    
        StringBuilder alias = context.nextAlias();
        filterData.setFilterType(FilterSQL.TYPE.INQUERY);
        filterData.setMainAlias(alias.toString());
        filterData.setJoiningColumn(context.getRealColumnName(Constants.FIELD_TYPE_ID));                
        
        
        StringBuilder sql = filterData.getFilterBody();
        BuildingContext.DBQName propDBQName = null;
        
        
        for(int i=0; i<propDefs.size(); i++){
            
            if(i>0)
                sql.append(" UNION ");
            
            PropertyDefinitionImpl propDef = propDefs.get(i);
            
            if(propDef.isUnstructured()){
                if(propDBQName == null)
                    propDBQName = context.getOwner().getBuildingContext().toDBQname(getPropertyName());
                    
                createUnstructured(sql, context, propDef, filterData, valueType, propDBQName, value, postfix);
            }else
                createTypeProperty(sql, context, propDef, filterData, value, postfix);
        }
    }



/*

select node_id 
    from cm_node_unstructured 
    where name='type1_value'
        and namespace_id=:testNdId
        and string_value='valueT1_10' 
        and multiple='0' 
                 union
    select u.node_id 
    from cm_node_unstructured u 
    join cm_node_unstruct_values uv 
        on uv.property_id=u.id 
    where u.name='type1_value' 
        and u.namespace_id=:testNdId
        and uv.string_value='valueT1_10'
*/
    /**
     * Creates SQL for unstructured property.
     * @throws RepositoryException 
     */
    private void createUnstructured(StringBuilder sql, FilterContext context, PropertyDefinitionImpl propDef, FilterSQL filterData, int valueType, BuildingContext.DBQName propDBQName, Object value, String postfix) throws RepositoryException {
    
        StringBuilder alias1 = context.nextAlias();
        String columnName;
        try{        
            columnName = JCRHelper.getUnstructuredPropertyColumnNameByType(valueType);
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to find column in a table for Unstructured data for value {0} of JCR Type: {1}",
                                                    value(), valueType);
                                                    
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }

//      No need always query both multiple and single valued ... 
//      because property definition always declares what type property is
//        if(!propDef.isMultiple()){
//      Build Unstructured single value SQL ...    
            sql.append("SELECT ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
            .append(" FROM ")
            .append(context.getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED)).append(' ').append(alias1)
            .append(" WHERE ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_NAME)).append("=?")
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_NAMESPACE));
    
            filterData.addParameter(propDBQName.getLocalName());
            
            if(propDBQName.hasNamespace()){
                    sql.append("=?");
                    filterData.addParameter(propDBQName.getNamespaceId());
            }else
               sql.append(" IS NULL");
            
            sql
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.TABLE_NODE_UNSTRUCTURED__MULTIPLE)).append("=?")
            .append(" AND ");
//            .append(QueryUtils.asPrefix(alias1));
               
            filterData.addParameter(false);
            
            StringBuilder reference = QueryUtils.asPrefix(alias1).append(context.getRealColumnName(columnName));
            
//            sql.append(context.getRealColumnName(columnName))
            sql.append(reference)            
            .append(getComparisonType().toSQL(negated())).append('?').append(postfix);
            
            filterData.addParameter(value);
            
            appendCombined(sql, filterData, reference);            
//        }else{

        sql.append(" UNION ");
        
        
//      Build Unstructured multy value SQL ...
/*
    join cm_node_unstruct_values uv 
        on uv.property_id=u.id 

    where u.name='type1_value' 
        and u.namespace_id=:testNdId
        and uv.string_value='valueT1_10'
*/
            StringBuilder alias2 = context.nextAlias();
    
            sql.append("SELECT ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
            .append(" FROM ")
            .append(context.getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED)).append(' ').append(alias1)
            .append(" JOIN ")
            .append(context.getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED_VALUES)).append(' ').append(alias2)
            .append(" ON ")
            .append(QueryUtils.asPrefix(alias2)).append(context.getRealColumnName(Constants.TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY))
            .append('=')        
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_ID))        
            
            .append(" WHERE ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_NAME)).append("=?")
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias1)).append(context.getRealColumnName(Constants.FIELD_NAMESPACE));
    
            filterData.addParameter(propDBQName.getLocalName());
            
            if(propDBQName.hasNamespace()){
                    sql.append("=?");
                    filterData.addParameter(propDBQName.getNamespaceId());
            }else
               sql.append(" IS NULL");

            
            StringBuilder reference2 = QueryUtils.asPrefix(alias2).append(context.getRealColumnName(columnName));
            
            sql
            .append(" AND ")
            .append(reference2)
//            .append(QueryUtils.asPrefix(alias2));
//            sql.append(context.getRealColumnName(columnName))
            .append(getComparisonType().toSQL(negated())).append('?').append(postfix);
            
            filterData.addParameter(value);
//            one more ...
            
            appendCombined(sql, filterData, reference2);
//        }
    }


/*
    select node_id 
    from cmv_qdata1 
    where x_type1_value='valueT1_10'
*/
    /**
     * Creates SQL for property definition declared explicitely in some node type.
     * @throws RepositoryException 
     */
    private void createTypeProperty(StringBuilder sql, FilterContext context, PropertyDefinitionImpl propDef, FilterSQL filterData, Object value, String postfix) throws RepositoryException {
        StringBuilder alias = context.nextAlias();
        
        NodeTypeImpl declaringType = (NodeTypeImpl)propDef.getDeclaringNodeType();
        String tableName = context.getOwner().getBuildingContext().getRealTableName(declaringType.getTableName());

        StringBuilder reference = QueryUtils.asPrefix(alias)
                .append(context.getRealColumnName(propDef.getColumnName()));
        
        sql
        .append("SELECT ")
        .append(QueryUtils.asPrefix(alias))
        .append(context.getRealColumnName(Constants.FIELD_TYPE_ID))
        .append(" FROM ")
        .append(tableName).append(' ').append(alias)
        .append(" WHERE ")
        .append(reference)
        .append(getComparisonType().toSQL(negated()))
        .append('?').append(postfix);
        
        filterData.addParameter(value);
        
        appendCombined(sql, filterData, reference);
        
        sql.append(" AND ")
        .append(QueryUtils.asPrefix(alias))
        .append(context.getRealColumnName(declaringType.getPresenceColumn())).append("=?");


        filterData.addParameter(true);
    }
}

/*
 * $Log: GeneralPropertyComparison.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:58:48  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/12/21 12:33:17  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.2  2006/12/20 16:18:02  maksims
 * #1803572 fix for between condition for general property
 *
 * Revision 1.1  2006/12/15 13:13:22  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.6  2006/12/13 14:27:13  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.5  2006/11/29 13:10:24  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.4  2006/11/22 16:35:37  maksims
 * #1802721 Log category performance added
 *
 * Revision 1.3  2006/11/17 10:17:30  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.2  2006/11/09 12:08:27  maksims
 * #1801897 SQL hints addition method used
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */