/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query.order;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.query.BuildingContext;
import com.exigen.cm.query.PathSQL;
import com.exigen.cm.query.PropertyData;
import com.exigen.cm.query.QueryUtils;
import com.exigen.cm.query.predicate.FilterContext;
import com.exigen.cm.query.step.PathStep;


/**
 * Defines ordering by general node property.
 */
public class OrderGeneralProperty extends OrderDefinition {
    
    /*
     * Cast to String
     * Oracle:
     *  select TO_CHAR(BOOLEAN_VALUE, '0999999999999999999D999999999999999999') valCol from CM_NODE_UNSTRUCTURED WHERE MULTIPLE=0 AND NAME='.propBoolean' AND TYPE <> 9
        UNION
        SELECT TO_CHAR(NODE_TYPE_ID, '0999999999999999999D999999999999999999') valCol FROM CM_TYPE
        UNION
        SELECT TO_CHAR(X_JCR_CREATED_13, 'YYYY-MM-DD HH24:MI:SS') valCol FROM CM_TYPE_NT_HIERARCHYNODE
        ORDER BY valCol ASC
     * 
     * 
     * MSSQL:
     * select STRING_VALUE valCol from CM_NODE_UNSTRUCTURED WHERE MULTIPLE=0 AND NAME='propString' AND TYPE <> 9
        UNION
        select STR(LONG_VALUE, 20, 9) valCol from CM_NODE_UNSTRUCTURED WHERE MULTIPLE=0 AND NAME='propLong' AND TYPE <> 9
        UNION
        select STR(DOUBLE_VALUE, 20,9) valCol from CM_NODE_UNSTRUCTURED WHERE MULTIPLE=0 AND NAME='propDouble' AND TYPE <> 9
        UNION
        select STR(BOOLEAN_VALUE, 20, 9) valCol from CM_NODE_UNSTRUCTURED WHERE MULTIPLE=0 AND NAME='propBoolean' AND TYPE <> 9
        UNION
        SELECT STR(NODE_TYPE_ID, 20, 9) valCol FROM CM_TYPE
        UNION
        SELECT CONVERT(VARCHAR, X_JCR_CREATED_12, 20) valCol FROM CM_TYPE_NT_HIERARCHYNODE
        ORDER BY valCol ASC
     * 
     */
    
    
    
    private String propertyColumnAlias;
    private static final Log log = LogFactory.getLog(OrderGeneralProperty.class);
//    private static final String VALUE_COLUMN_ALIAS = "ORDER_VALUE";
    
    private final String propertyName;
    
    protected OrderGeneralProperty(String propertyName, ORDER order) {
        super(order);
        this.propertyName=propertyName;
    }
    
    /**
     * @inheritDoc
     */
    @Override
    public String[] getReferredColumns() {
        return new String[]{propertyColumnAlias};
    }
    
    
    /**
     * Generates SQL pieces needed to order by general type property. Adds references
     * to ordered column into FROM statement part of common SQL.
     */
    @Override
    public void toSQL(PathStep owner, PathSQL target, FilterContext fc) {
        propertyColumnAlias = propertyColumnAlias == null ? getNextOrderValueAlias(target):propertyColumnAlias;
        
        List<PropertyDefinitionImpl> propertyDefs = new LinkedList<PropertyDefinitionImpl>();
        
        try{
            int requiredType = findTargets(propertyName, owner.getNodeTypes(), propertyDefs, target.getBuildingContext());
            if(log.isDebugEnabled()){
                log.debug(MessageFormat.format("Ordering property {0} as type: {1}",
                        propertyName,
                        PropertyType.nameFromValue(requiredType)));
            }

            
            
            if(propertyDefs.size() == 1 && !propertyDefs.get(0).isUnstructured()){
                createSimple(owner, target, fc, propertyColumnAlias, propertyDefs.get(0));
                return;
            }
            
            
            
            StringBuilder mainAlias = target.nextAlias();
            
            target.from().append(" JOIN (");
            
//          to prevent multiple unstructured table adds .. although this is runtime error condition if multiple unstructureds appear here!
            int unstructuredAdds = 0;
            int count = 0;
            
            for(PropertyDefinitionImpl pd : propertyDefs){
                if(count++ > 0)
                    target.from().append(" UNION ");
                
                if(pd.isUnstructured()){
                    if(unstructuredAdds++ == 0)
                        addUnstructured(owner, target, propertyColumnAlias, pd, requiredType);
                    else
                        throw new RuntimeException("Something broken ... multiple unstructured refs found while building ordering statement");
                }
                else
                    addGeneral(owner, target, propertyColumnAlias, pd, requiredType);
            }
            
            
            
            
            target.from()
                .append(") ").append(mainAlias)
                .append(" ON ").append(QueryUtils.asPrefix(mainAlias).append(Constants.FIELD_ID))
                .append('=')
                .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS).append(Constants.FIELD_ID));            

            
            target.addSelection(mainAlias.toString(), propertyColumnAlias, propertyColumnAlias);
            
        }catch(RuntimeException ex){
            throw ex; // already logged
            
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to build ordering condition: {0} {1} ",
                    propertyName, getOrder());
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }

    /**
     * Generates SQL allowing to order nodes by unstructured property.
     * @param owner
     * @param target
     * @param propertyColumnAlias
     * @param pd
     * @param requiredType
     * @throws Exception
     */
    private void addUnstructured(PathStep owner, PathSQL target, String propertyColumnAlias, PropertyDefinitionImpl pd, int requiredType) throws Exception{
        BuildingContext bc = target.getBuildingContext();
        
        BuildingContext.DBQName propDBQN = bc.toDBQname(propertyName);
        
        StringBuilder sql = target.from();
        
        StringBuilder alias = bc.nextAlias();
        
        String orderingColumn = bc.getRealColumnName(JCRHelper.getUnstructuredPropertyColumnNameByType(requiredType));

        String multipleColumn = bc.getRealColumnName(Constants.TABLE_NODE_UNSTRUCTURED__MULTIPLE);
        String nameColumn = bc.getRealColumnName(Constants.FIELD_NAME);
        String nsColumn = bc.getRealColumnName(Constants.FIELD_NAMESPACE);
        
        String tableName = bc.getRealTableName(Constants.TABLE_NODE_UNSTRUCTURED);
        
        sql
            .append("SELECT ")
            .append(QueryUtils.asPrefix(alias)).append(Constants.FIELD_TYPE_ID).append(' ').append(Constants.FIELD_ID)
            .append(',')
            .append(QueryUtils.asPrefix(alias)).append(orderingColumn).append(' ').append(propertyColumnAlias)
            .append(" FROM ")
            .append(tableName).append(' ').append(alias)
            .append(" WHERE ")
            .append(QueryUtils.asPrefix(alias)).append(nameColumn).append("=?")
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias)).append(nsColumn);
        
        target.addParameter(propDBQN.getLocalName(), PathSQL.QUERY_PART.FROM);
        
        if(propDBQN.hasNamespace()){
            sql.append("=?");
            target.addParameter(propDBQN.getNamespaceId(), PathSQL.QUERY_PART.FROM);
        }else
            sql.append(" IS NULL");
        
        sql
            .append(" AND ")
            .append(QueryUtils.asPrefix(alias)).append(multipleColumn).append("=?");

        target.addParameter(false, PathSQL.QUERY_PART.FROM);
        
    }
    
    
    /**
     * Generates SQL allowing to order nodes by general type property in case multiple 
     * definitions of ordered property is found.
     * @param owner
     * @param target
     * @param propertyColumnAlias
     * @param pd
     * @param requiredType
     * @throws RepositoryException 
     */
    private void addGeneral(PathStep owner, PathSQL target, String propertyColumnAlias, PropertyDefinitionImpl pd, int requiredType) throws RepositoryException {
        StringBuilder sql = target.from();
        
        BuildingContext bc = target.getBuildingContext();
        StringBuilder alias = bc.nextAlias();
        
        StringBuilder orderingColumn = adjustOrderingColumnName(
                                                QueryUtils.asPrefix(alias).append(bc.getRealColumnName(pd.getColumnName())),
                                                pd.getRequiredType(), 
                                                requiredType, 
                                                bc.getDialect());
        
        NodeTypeImpl dnt = (NodeTypeImpl)pd.getDeclaringNodeType();
        String tableName = bc.getRealTableName(dnt.getTableName());
        String presenceColumn = bc.getRealColumnName(dnt.getPresenceColumn());        
        
        sql
            .append("SELECT ")
            .append(QueryUtils.asPrefix(alias)).append(Constants.FIELD_TYPE_ID).append(' ').append(Constants.FIELD_ID)
            .append(',')
            .append(orderingColumn).append(' ').append(propertyColumnAlias)
            .append(" FROM ")
            .append(tableName).append(' ').append(alias)
            .append(" WHERE ")
            .append(QueryUtils.asPrefix(alias)).append(presenceColumn)
            .append("=?");
        
        target.addParameter(true, PathSQL.QUERY_PART.FROM);
    }

    /**
     * Creates SQL to order by general property in case single property definition for given property is found.
     * @param owner
     * @param target
     * @param fc
     * @param propertyColumnAlias
     * @param pd
     * @throws RepositoryException 
     */
    private void createSimple(PathStep owner, PathSQL target, FilterContext fc, String propertyColumnAlias, PropertyDefinitionImpl pd) throws RepositoryException {
        NodeTypeImpl type = (NodeTypeImpl)pd.getDeclaringNodeType();
        String tableName = type.getTableName();
        String tableAlias = owner.getTableAlias(tableName);
        BuildingContext bc = target.getBuildingContext();
        
        if(tableAlias == null){ // no one refers to target table ... so add it ...
            tableAlias = target.nextAlias().toString();
            owner.addTableAlias(tableName, tableAlias);
            
            target.from()
            .append(" JOIN ")
            .append(target.getBuildingContext().getRealTableName(tableName))
            .append(' ').append(tableAlias)
            .append(" ON ").append(QueryUtils.asPrefix(tableAlias)).append(bc.getRealColumnName(Constants.FIELD_TYPE_ID))
            .append('=')
            .append(QueryUtils.asPrefix(PathSQL.TARGET_ALIAS)).append(bc.getRealColumnName(Constants.FIELD_ID))
            ;
            
            target.where()
            .append(" AND ")
            .append(QueryUtils.asPrefix(tableAlias))
            .append(bc.getRealColumnName(type.getPresenceColumn()))
            .append("=?");
            
            target.addParameter(true, PathSQL.QUERY_PART.WHERE);
        }
        
        target.addSelection(tableAlias, bc.getRealColumnName(pd.getColumnName()), propertyColumnAlias);
        
    }

    /**
     * Searhes property definitions for property in a node types specified for a context node.
     * @param propertyName
     * @param contextTypes
     * @param propertyDefs
     * @param bc
     * @return
     * @throws Exception
     */
    protected int findTargets(String propertyName, Collection<NodeTypeImpl> contextTypes,List<PropertyDefinitionImpl> propertyDefs, BuildingContext bc) throws Exception{
        List<PropertyData> data = new ArrayList<PropertyData>();
        PropertyData result = new PropertyData();
        
        if(contextTypes != null){ 
            bc.lookupInNodeTypesAndParents(propertyName, -1, contextTypes.iterator(), result);
//            bc.lookupInNodeTypes(propertyName, -1, contextTypes.iterator(), result);
            if(result.hasExactMatches()){ 
//              assuming that exact matches based on types provided are derived from
//              the same parent type these all should point to same PropertyDefinition
                PropertyDefinitionImpl reference = null;
                boolean singleReference = true;
                for(PropertyDefinitionImpl pdi : result.getExact()){
                    if(reference == null)
                        reference = pdi;
                    else{
                        singleReference = reference.getSQLId().equals(pdi.getSQLId());
                        if(!singleReference)
                            break; // not the same
                    }
                }
                
                if(singleReference){
                    propertyDefs.add(reference);
                    return reference.getRequiredType();
                }
            }
            
            if(result.hasMatches())
                data.add(result);
        }
        

        if(!result.hasMatches()){ // if nothing is found in declared types try all!
    //      Lookup in:
    //      - All Node types
    //      - All Mixins
            bc.lookupPropertyDataInAllNodeTypes(propertyName, -1, result);
            bc.lookupPropertyDataInMixins(propertyName, -1, result);
            if(result.hasMatches())
                data.add(result);
        }

        if(data.size() == 0){
            String message = MessageFormat.format("No orderable properties with name {0} found in all node types and mix-ins",
                    propertyName);
            log.error(message);
            throw new RuntimeException(message);
        }        
        
        
        
        int firstPropertyType = -1;
        int targetPropertyType = -1;
        
        boolean unstructuredAdded = false;
        for(int i=0; i<data.size(); i++){
            PropertyData pd = data.get(i);
            if(pd.hasExactMatches()){
                for(PropertyDefinitionImpl pi : pd.getExact()){
                    
                    if(firstPropertyType < 0){
                        firstPropertyType = pi.getRequiredType();
                        targetPropertyType = firstPropertyType;
                    }
                    else
                        if(firstPropertyType != pi.getRequiredType())
                            targetPropertyType = PropertyType.STRING;
                    propertyDefs.add(pi);
                }
            }

//          Do not check wildcards twice ...
            if(pd.hasUnstructuredMatches() && !unstructuredAdded ){
                PropertyDefinitionImpl pdi = pd.getUnstructuredMatch();
                
                propertyDefs.add(pdi);
                unstructuredAdded = true;
                
                if(firstPropertyType < 0){
                    firstPropertyType = pdi.getRequiredType();
                    targetPropertyType = firstPropertyType;
                }
                else
                    if(firstPropertyType != pdi.getRequiredType())
                        targetPropertyType = PropertyType.STRING;
            }
        }
        
        return targetPropertyType == PropertyType.UNDEFINED ? PropertyType.STRING : targetPropertyType;
    }
    
    /**
     * Adds wraps ordered column by conversion to String function in case ordering as string is required.
     * @param columnName
     * @param actualType
     * @param requiredType
     * @param dialect
     * @return
     */
    protected StringBuilder adjustOrderingColumnName(StringBuilder columnName, int actualType, int requiredType, DatabaseDialect dialect){
        if(actualType == requiredType)
            return columnName;
        
        switch(actualType){
            case PropertyType.DATE:
                return dialect.getDateColumnToStringConversion(columnName.toString());

                
            case PropertyType.LONG:
                return dialect.getLongColumnToStringConversion(columnName.toString());
                
            case PropertyType.DOUBLE:
                return dialect.getDoubleColumnToStringConversion(columnName.toString());

            case PropertyType.BOOLEAN:
                return dialect.getBooleanColumnToStringConversion(columnName.toString());
                
            case PropertyType.STRING:
                return columnName;
            
            default:
                String message = MessageFormat.format("Cannot cast column of type {0} to String to perform unified ordering by mixed data types",
                        PropertyType.nameFromValue(actualType));
                log.error(message);
                throw new RuntimeException(message);
        }
    }    
}

/*
 * $Log: OrderGeneralProperty.java,v $
 * Revision 1.2  2007/10/09 07:34:51  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 09:01:08  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/21 12:33:19  maksims
 * #1803635 JavaDocs added
 *
 * Revision 1.1  2006/12/15 13:13:25  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.6  2006/12/13 14:27:10  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.5  2006/12/05 15:52:25  maksims
 * #1803540 Added ability to search by uuid
 *
 * Revision 1.4  2006/11/29 13:10:22  maksims
 * #1802721 added ability to use in ordering table containing property column joined by last step filter.
 *
 * Revision 1.3  2006/11/22 17:08:52  maksims
 * #1802721 removed unneeded node ID added by ordering into main select
 *
 * Revision 1.2  2006/11/15 13:16:50  maksims
 * #1802721 Ordering of props as strings added
 *
 * Revision 1.1  2006/11/02 17:28:09  maksims
 * #1801897 Query2 addition
 *
 */