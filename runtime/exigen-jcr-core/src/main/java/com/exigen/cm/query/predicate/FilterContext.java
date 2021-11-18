/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.predicate;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.PropertyData;
import com.exigen.cm.query.BuildingContext.DBQName;
import com.exigen.cm.query.PathSQL.QUERY_PART;
import com.exigen.cm.query.QueryUtils.FixedParameter;
import com.exigen.cm.query.step.PathStep;

/**
 * Holds predicate context data.
 * @author mzizkuns
 */
public class FilterContext{
    private Map<String, PropertyData> propertiesCache = new HashMap<String, PropertyData>();
    
    /**
     * Field name by which sub-select containing predicate filter
     * should be joined with main query.
     */
    public static final String JOINING_FIELD = Constants.FIELD_ID;
    
    private DBQName contextNode;
    private NodeTypeImpl contextType;
    private PathSQL sqlHolder;
    private boolean canUseOwnerTable = false;
    private String ownerAlias;
    private StringBuilder contextTypeAlias;
    
//    private Map<String, String> tableAliases;
    
    private StringBuilder target;
    
//  Holds property tables used in filter which can be referred ordering
    private Map<String, String> propertyReferences;
    
    private static final Log log = LogFactory.getLog(FilterContext.class);
    private final PathStep owner;
    private boolean mixinPropagateAllowed = true;
    private boolean nodeTypePropagateAllowed = true;    
    
    
    public FilterContext(DBQName node, PathSQL sqlHolder, PathStep owner) {
        contextNode=node;
        this.sqlHolder=sqlHolder;
        target = new StringBuilder();
        
//      Store sqlHolder alias
        ownerAlias = sqlHolder.currentAlias().toString();
        this.owner = owner;
    }
    
    public FilterContext(DBQName node, NodeTypeImpl type, PathSQL sqlHolder, StringBuilder contextTypeAlias, PathStep owner) {
        this(node, sqlHolder, owner);
        this.contextTypeAlias = contextTypeAlias;
        contextType=type;        
    }    

    /**
     * Returns <code>true</code> if filter based on this context should notify about node types
     * and mix-ins it uses if these are unambigously declared.<br>
     * For example:<br>
     * <li>filter like [jcr:primaryType='nt:file'] declares node type unumbigously but
     * <li>filter's [@jcr:primaryType='nt:file' or (@my:name='joe')] node type 
     * declaration is ambigous because both nodes with primaryType specified 
     * and attribute name equals to 'joe' must be included in result collection.
     * @return
     */
    boolean isCollectTypesOn(){
        return owner.isFirst();
    }
    
    /**
     * Returns context node data or <code>null</code> if not specified.
     * Used to refer step name when reporting error
     * @return
     */
    public DBQName getContextNode(){
        return contextNode;
    }
    
    /**
     * Returns context node type or <code>null</code> if not specified.
     * @return
     */
    public NodeTypeImpl getContextType(){
        return contextType;
    }
    
    public StringBuilder getContextTypeAlias(){
        return contextTypeAlias;
    }

    /**
     * Returns owning context.
     * @return
     */
    public PathSQL getOwner(){
        return sqlHolder;
    }

    /**
     * Appends parameters list to a specified query part (FROM | WHERE).
     * @param parameters
     * @param queryPart
     */
    public void appendParameters(List<Object> parameters, QUERY_PART queryPart){
        for(Object param:parameters){
            sqlHolder.addParameter(param, queryPart);
        }
    }

    /**
     * Returns <code>true</code> if reference to owning alias can be used
     * while generating filter SQL.
     * @return
     */
    public boolean canUseOwnerTable(){
        return canUseOwnerTable;
    }
    
    /**
     * Sets flag that reference to owning alias can be used
     * while generating filter SQL.
     * @param canUse
     */
    public void canUseOwnerTable(boolean canUse){
        canUseOwnerTable = canUse;
    }
    
    /**
     * Returns owning path step alias.
     * @return
     */
    public String getOwnerAlias(){
        return ownerAlias;
    }
    
    /**
     * Returns SQL buffer.
     * @return
     */
    public StringBuilder target() {
        return target;
    }
    
    /**
     * Merges SQL collected in SQL buffer with owning
     * XPath SQL FROM part.
     *
     */
    public void mergeWithOwner(){
        sqlHolder.from().append(target);
    }
    
    /**
     * Returns next available alias.
     * @return
     */
    public StringBuilder nextAlias(){
        return sqlHolder.nextAlias();
    }
    
    /**
     * Returns current alias.
     * @return
     */
    public StringBuilder currentAlias(){
        return sqlHolder.currentAlias();
    }
    
    /**
     * Sets current alias.
     * @param alias
     */
    public void currentAlias(StringBuilder alias){
        sqlHolder.currentAlias(alias);
    }

    /**
     * Helper method to get real column name from general column name constant.
     * @param field_id
     * @return
     */
    public String getRealColumnName(String field_id) {
        return sqlHolder.getBuildingContext().getRealColumnName(field_id);
    }

    /**
     * Helper method to get real table name from general table name constant.
     * @param tableName
     * @return
     * @throws RepositoryException 
     */
    public String getRealTableName(String tableName) throws RepositoryException {
        return sqlHolder.getBuildingContext().getRealTableName(tableName);
    }
    
    /**
     * Gathers property definitions for property with name propertyName in in
     * optional primaryNodeType without taking type into account.
     * @param propertyName
     * @param primaryNodeType
     * @return
     */
    PropertyData lookupPropertyDefinitions(String propertyName, NodeTypeImpl primaryNodeType){
        return lookupPropertyDefinitions(propertyName, -1, primaryNodeType);
    }
    
    /**
     * Gathers property definitions for property with name propertyName in in
     * optional primaryNodeType with taking type into account.
     * @param propertyName
     * @param valueType
     * @param primaryNodeType
     * @return
     */
    PropertyData lookupPropertyDefinitions(String propertyName, int valueType, NodeTypeImpl primaryNodeType){
        if(propertyName == null) // how can it be!!!
            return null;
        
//      Note: Special Case when valueType=UNDEFINED! clear that query is for Unstructured table! 
//      Not at all ... IS NULL/IS NOT NULL type is UNDEFINED but all property declarants should be checked!
        
        String key = new StringBuilder(propertyName).append(valueType).toString();
        PropertyData result = propertiesCache.get(key);
        if(result != null)
            return result;
        
        RepositoryException error = null;
        
        result = new PropertyData();
        
        try{
            if(primaryNodeType != null)
                sqlHolder.getBuildingContext().lookupPropertyDataInPrimaryNodeType(propertyName, valueType, primaryNodeType, result);
            else
                if(contextType != null)
                    sqlHolder.getBuildingContext().lookupPropertyDataInElementNodeType(propertyName, valueType, contextType, result);
                else
                    sqlHolder.getBuildingContext().lookupPropertyDataInAllNodeTypes(propertyName, valueType, result);
    
            if(!result.hasExactMatches())
                sqlHolder.getBuildingContext().lookupPropertyDataInMixins(propertyName, valueType, result);
    

        }catch(RepositoryException rex){
            error = rex;
        }
        
        if(!result.hasMatches() && !result.hasUnstructuredMatches() || error != null){
            String message = MessageFormat.format("Failed to find property definition for {0}({1}) in a context of node {2} with node type {3}"
                    ,propertyName
                    , PropertyType.nameFromValue(valueType)
                    ,getContextNode().isWildcard()?"*":getContextNode().getName()                            
                    ,primaryNodeType != null ? primaryNodeType.getName() : contextType != null ? contextType.getName() : "UNKNOWN");
                
            log.error(message, error);
            throw new RuntimeException(message, error);
        }
        
        if(result.hasUnsupportedMatches()){
            // failed due to unsupported match
            String message = MessageFormat.format("Search by property {0}({1}) in a context of node {2}({3}) is not supported."
                            ,propertyName
                            , PropertyType.nameFromValue(result.getUnsupportedMatch().getRequiredType())
                            ,getContextNode().isWildcard()?"*":getContextNode().getName()                            
                            ,primaryNodeType != null ? primaryNodeType.getName() : contextType != null ? contextType.getName() : "UNKNOWN");
            
                log.error(message);
                throw new UnsupportedOperationException(message);
        }
        
        propertiesCache.put(key, result);
        return result;
    }



    /**
     * Fixes parameter value thus disallowing this value by dialect.
     * @param value
     * @return
     */
    public Object fixParameter(Object value){
        return new FixedParameter(value);
    }
    

    /**
     * Returns table aliase if registered or <code>null</code>
     * @param tableName
     * @return
     */
    public String getTableAlias(String tableName){
        return owner.getTableAlias(tableName);
    }
    
    /**
     * Adds table alias.
     * @param tableName
     * @param alias
     */
    void addTableAliase(String tableName, String alias){
//        if(tableAliases == null) resetTableAliases();
//        tableAliases.put(tableName, alias);
        owner.addTableAlias(tableName, alias);
    }
    
    

    /**
     * Associates JCR property name with SQL reference
     * @param property
     * @param reference
     */
    void addPropertyReference(String property, String reference){
        if(propertyReferences == null)
            propertyReferences = new HashMap<String, String>();
        propertyReferences.put(property, reference);
    }
    
    /**
     * Returns gathered property-SQL reference associations or <code>null</code>
     * if nothing is collected during filter SQL building.
     * @return
     */
    public Map<String, String> getPropertyReferences(){
        return propertyReferences;
    }

    
    /**
     * Sets flag if mixin data propagation to owning path step is allowed.
     * @param allow
     */
    void setMixinPropagateAllowed(boolean allow){
        mixinPropagateAllowed = allow;
    }
    
    /**
     * Sets flag if node type data propagation to owning path step is allowed.
     * @param allow
     */
    void setNodeTypePropagateAllowed(boolean allow){
        nodeTypePropagateAllowed = allow;
    }
    
    /**
     * Returns flag if mixin data propagation to owning path step is allowed.
     * @return
     */
    boolean isMixinPropagateAllowed(){
        return mixinPropagateAllowed;
    }

    /**
     * Returns flag if node type data propagation to owning path step is allowed.
     * @return
     */
    boolean isNodeTypePropagateAllowed(){
        return nodeTypePropagateAllowed;
    }
    
    
    /**
     * Propagates node type (type or mixin) to PathStep.
     * @param nodeType
     */
    void propagateNodeType(NodeTypeImpl nodeType){
        if(!isCollectTypesOn())
            return; // node types propagation disabled ...
        
        if(nodeType.isMixin() && isMixinPropagateAllowed())
            owner.addNodeType(nodeType);
        else
        if(!nodeType.isMixin() && isNodeTypePropagateAllowed())
            owner.addNodeType(nodeType);
    }
    
}

/*
 * $Log: FilterContext.java,v $
 * Revision 1.4  2008/11/06 10:23:53  maksims
 * export stop types support added
 *
 * Revision 1.3  2008/10/09 13:27:29  maksims
 * #0153705 IllegalArgumentException will be reported if query against Name type property is executed
 *
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
 * Revision 1.6  2006/12/13 14:27:13  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.5  2006/12/05 15:52:24  maksims
 * #1803540 Added ability to search by uuid
 *
 * Revision 1.4  2006/11/29 13:10:24  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.3  2006/11/22 16:35:37  maksims
 * #1802721 Log category performance added
 *
 * Revision 1.2  2006/11/17 10:17:30  maksims
 * #0149157 added query siimplification for case when properties used in predicate belong to explicitly declared context node type
 *
 * Revision 1.1  2006/11/02 17:28:07  maksims
 * #1801897 Query2 addition
 *
 */
