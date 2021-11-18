/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.query;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.query.predicate.DefaultFilterSQLBuilder;
import com.exigen.cm.query.predicate.FilterSQLBuilder;

/**
 * Provides API to query building context data.
 */
public class BuildingContext {

    private enum PROPERTY_DEF_MATCH_TYPE {EXACT
                                    , NAME_UNDEFINED
                                    , WILDCARD_TYPED
                                    , WILDCARD
                                    , UNSUPPORTED
                                    };
    
    private int aliasCount = 1;
    
    private Map<String, DBQName> qNameCache = new HashMap<String, DBQName>();
    
    private final Map<String, NodeTypeImpl> typeCache = new HashMap<String, NodeTypeImpl>();
    
    private final SessionImpl session;
    
    private DatabaseDialect dialect;
    private DatabaseConnection connection; 
    private final NamespaceRegistryImpl nsRegistry;
    private final NodeTypeManagerImpl ntManager;
    
    private final FilterSQLBuilder filterGenerator = new DefaultFilterSQLBuilder();
    private final BasicSecurityFilter securityFilter;
    
    private boolean areHintsApplicable = false;

    private boolean allowBrowse = false;
    
    public BuildingContext(SessionImpl session) throws RepositoryException{
        this.session=session;
        connection = session.getConnection();
        dialect = connection.getDialect();
        nsRegistry = session._getWorkspace()._getNamespaceRegistry();
        ntManager = session._getWorkspace()._getNodeTypeManager();
        securityFilter= session._getWorkspace().isSecuritySwitchedOn() 
                                    ? dialect.getSecurityFilter() 
                                    : new BasicSecurityFilter(){
                                        @Override
                                        public boolean hasWhere() {
                                            return false;
                                        }
                                    };// security is turned off. no security filter should be applied.
    }
    
    /**
     * Returns current JCR sesions in which context query is executed.
     * @return
     */
    public SessionImpl getSession(){
        return session;
    }
    
    
    /**
     * Returns DB dialect specific security filter instance.
     * @return
     */
    public BasicSecurityFilter getSecurityFilter(){
        return securityFilter;
    }
    
    /**
     * Returns DB dialect instance.
     * @return
     */
    public DatabaseDialect getDialect(){
        return dialect;
    }
    
    /**
     * Returns connection to be used for SQL query executions.
     * @return
     */
    public DatabaseConnection getConnection(){
        return connection;
    }
    
    /**
     * Returns JCR DB namespace ID.
     * @param nsName
     * @return
     */
    public Long getNamespaceId(String nsName){
        return nsName == null || nsName.length()==0 ? null : nsRegistry._getByPrefix(nsName).getId();
    }
    
    /**
     * Returns instance of node type manager associated with context JCR session.
     * @return
     */
    public NodeTypeManagerImpl getNodeTypeManager(){
        return ntManager;
    }
    
    
    /**
     * Returns new alias.
     * @return
     */
    public StringBuilder nextAlias(){
        return new StringBuilder(PathSQL.TARGET_ALIAS).append(aliasCount++);
    }
    
    /**
     * Returns filter SQL generator.
     * @return
     */
    public FilterSQLBuilder getFilterSQLGenerator(){
        return filterGenerator;
    }
    
    
    /**
     * Returns QName as a DBQName.
     * @param qNameString
     * @return
     * @see DBQName
     */
    public DBQName toDBQname(String qNameString){
     return toDBQname(qNameString, true);   
    }
    
    public DBQName toDBQname(String qNameString, boolean canCache){
        if(qNameString == null) return new DBQName();
        
        DBQName res = canCache ? qNameCache.get(qNameString) : null;
        if(res != null)
            return res;
        
        int nsSeparator = qNameString.lastIndexOf(':');
        Long nsId = null;
        String localName;
        if(nsSeparator>0){
            nsId = getNamespaceId(qNameString.substring(0, nsSeparator));
            localName = qNameString.substring(nsSeparator+1);
        }else
            localName = qNameString;
        
        res = new DBQName(qNameString, nsId, localName);
        if(canCache)
            qNameCache.put(qNameString, res);
        
        return res;
    }    
    
    /**
     * Returns table name converted by database dialect.
     * @param tableName
     * @return
     * @throws RepositoryException 
     */
    public String getRealTableName(String tableName) throws RepositoryException{
        return getDialect().convertTableName(tableName);
    }
    
    /**
     * Returns column name converted by database dialect.
     * @param column
     * @return
     */
    public String getRealColumnName(String columnName){
        return getDialect().convertColumnName(columnName);
    }    
    
    
    /**
     * Returns node type definition for type name specified. Once returned
     * node type definition is cached in given context.
     * @param typeName
     * @return
     */
    public NodeTypeImpl getNodeTypeDefinition(String typeName){
        NodeTypeImpl typeData = typeCache.get(typeName);
        if(typeData != null)
            return typeData;
        
        try{
            NodeTypeManagerImpl ntManager = getNodeTypeManager();
            typeData = (NodeTypeImpl)ntManager.getNodeType(typeName);
            
            typeCache.put(typeName, typeData);
            return typeData;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Tests given node type and parent node types for existence of property with specified name and type.
     * @param propertyName
     * @param valueType
     * @param primaryNodeType
     * @return
     */
    public void lookupPropertyDataInPrimaryNodeType(String propertyName, int valueType, NodeTypeImpl primaryNodeType, PropertyData result) {
        NodeType[] parents = primaryNodeType.getSupertypes();
        NodeType[] all = new NodeType[parents.length+1];
        all[0]=primaryNodeType;
        System.arraycopy(parents, 0, all, 1, parents.length);
        lookupInNodeTypes(propertyName, valueType, all, result);
    }

    /**
     * FIX ME When Subtypes access will be implemented by Denis should implement correct handling for element(*, nodeType)
     * Till then will perform as for jcr:primaryType
     * @param propertyName
     * @param valueType
     * @param contextType
     * @return
     */
    public void lookupPropertyDataInElementNodeType(String propertyName, int valueType, NodeTypeImpl contextType, PropertyData result) throws RepositoryException{
        NodeType[] parents = contextType.getSupertypes();
        NodeType[] children = contextType.getSubtypes();
        
        NodeType[] all = new NodeType[children.length + parents.length+1];
        
        all[0]=contextType;
        System.arraycopy(parents, 0, all, 1, parents.length);
        System.arraycopy(children, 0, all, parents.length+1, children.length);        
        lookupInNodeTypes(propertyName, valueType, all, result);
        
//        lookupInPrimaryNodeType(propertyName, valueType, contextType, result);
    }

    /**
     * Performs property lookup by name and type in all mixins registered in a given JCR instanc.
     * @param propertyName
     * @param valueType
     * @param result
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    public void lookupPropertyDataInMixins(String propertyName, int valueType, PropertyData result) throws RepositoryException{
        NodeTypeManager ntManager = getSession().getWorkspace().getNodeTypeManager();
        NodeTypeIterator ntIterator = ntManager.getMixinNodeTypes();
        lookupInNodeTypes(propertyName, valueType, ntIterator, result);
    }

    
    /**
     * Performs property lookup by name and type in all node types registered in a given JCR instance.
     * @param propertyName
     * @param valueType
     * @param result
     * @throws RepositoryException
     */
    @SuppressWarnings("unchecked")
    public void lookupPropertyDataInAllNodeTypes(String propertyName, int valueType, PropertyData result) throws RepositoryException{
        NodeTypeManager ntManager = getSession().getWorkspace().getNodeTypeManager();
        NodeTypeIterator ntIterator = ntManager.getPrimaryNodeTypes();
        lookupInNodeTypes(propertyName, valueType, ntIterator, result);
    }
    

    /**
     * Performs property lookup in a node types specified.
     * @param propertyName
     * @param valueType
     * @param nodeTypes
     * @param result
     */
    public void lookupInNodeTypes(String propertyName, int valueType, NodeType[] nodeTypes, PropertyData result) {
        for(NodeType type : nodeTypes){
            PDefPointer pdef = lookupPropertyDefinition(propertyName, valueType, type);
            if(pdef ==null)
                continue;
            
            switch(pdef.type){
                case EXACT:
                    result.addExact(pdef.propDef);
                    break;
                    
                case NAME_UNDEFINED:
                    result.nameUndefinedPropDef=pdef.propDef;
                    break;
                case WILDCARD_TYPED:
                    result.wildcardTypedPropDef=pdef.propDef;
                    break;
                    
                case WILDCARD:
                    result.wildcard=pdef.propDef;
                    break;
                    
                case UNSUPPORTED:
                    result.unsupportedMatch = pdef.propDef;
                    break;                    
            }
        }
    }

    /**
     * Performs property lookup in node types specified and their parents.
     * @param propertyName
     * @param valueType
     * @param ntIterator
     * @param result
     */
    public void lookupInNodeTypesAndParents(String propertyName, int valueType, Iterator<? extends NodeType> ntIterator, PropertyData result) {
        Map<Long, NodeType> hierarchy = new HashMap<Long, NodeType>();
        while(ntIterator.hasNext()){
            NodeType nt = ntIterator.next();
            if(!hierarchy.containsKey(((NodeTypeImpl)nt).getSQLId()))
                hierarchy.put(((NodeTypeImpl)nt).getSQLId(), nt);
            
            
            NodeType[] parents = nt.getSupertypes();
            for(NodeType parent:parents){
                NodeTypeImpl nti = (NodeTypeImpl)parent;
                if(!hierarchy.containsKey(nti.getSQLId()))
                    hierarchy.put(nti.getSQLId(), nti);
            }
        }

        lookupInNodeTypes(propertyName, valueType, hierarchy.values().iterator(), result);
    }
    

    /**
     * Performs property lookup by name and type in node types specified.
     * @param propertyName
     * @param valueType
     * @param ntIterator
     * @param result
     */
    public void lookupInNodeTypes(String propertyName, int valueType, Iterator<? extends NodeType> ntIterator, PropertyData result) {
        while(ntIterator.hasNext()){
//            NodeType nodeType = ntIterator.nextNodeType();
            NodeType nodeType = (NodeType)ntIterator.next();
            PDefPointer pdef = lookupPropertyDefinition(propertyName, valueType, nodeType);
            if(pdef ==null)
                continue;
            
            switch(pdef.type){
                case EXACT:
                    result.addExact(pdef.propDef);
                    break;
                    
                case NAME_UNDEFINED:
                    result.nameUndefinedPropDef=pdef.propDef;
                    break;
                case WILDCARD_TYPED:
                    result.wildcardTypedPropDef=pdef.propDef;
                    break;
                    
                case WILDCARD:
                    result.wildcard=pdef.propDef;
                    break;
                    
                case UNSUPPORTED:
                    result.unsupportedMatch = pdef.propDef;
                    break;
            }
        }
    }

    /**
     * Performs property lookup by name and type in a node type definition specified. Returned
     * pointer to property contains information about type of property match.
     * @param propertyName
     * @param valueType
     * @param nodeType
     * @return
     */
    private PDefPointer lookupPropertyDefinition(String propertyName, int valueType, NodeType nodeType){
        PropertyDefinition[] pds = nodeType.getDeclaredPropertyDefinitions();
        
        PropertyDefinition nameUD = null;
        PropertyDefinition wildcardTyped = null;
        PropertyDefinition wildcardUD = null;
        PropertyDefinition unsupported = null;
        
        for(PropertyDefinition pd:pds){
            int requiredType = pd.getRequiredType();

            if(requiredType == PropertyType.NAME 
                    &&  pd.getName().equals(propertyName)
                    && valueType == PropertyType.STRING){// names will be passed as strings
                unsupported = pd;
            }            
            
            if(requiredType == PropertyType.REFERENCE || requiredType == PropertyType.PATH)// reference properties should be processed as strings
                requiredType = PropertyType.STRING;
            
            if(pd.isMandatory() 
            		&& pd.getName().equals(propertyName) 
            		&& requiredType != PropertyType.UNDEFINED 
            		&& requiredType != valueType){
            	
            	String message = MessageFormat.format(" Query cannot be executed. Property {0} is declared as {1} in node type {2} but property type detected during query parse is {3}",
            			pd.getName(), PropertyType.nameFromValue(valueType), nodeType.getName(), PropertyType.nameFromValue(requiredType));
            	throw new RuntimeException(message);
            }
            
            if(requiredType == valueType || valueType<0 && requiredType != PropertyType.UNDEFINED){
                if(pd.getName().equals(propertyName))
                    return new PDefPointer((PropertyDefinitionImpl)pd
                            , pd.isMultiple() ? // multiple properties are always unstructured although may have explicit name and type PTR #1803572
                                      PROPERTY_DEF_MATCH_TYPE.WILDCARD_TYPED
                                    : PROPERTY_DEF_MATCH_TYPE.EXACT );
                
                if(pd.getName().charAt(0)=='*' && wildcardTyped == null)
                    wildcardTyped = pd;
            }
            else
            if(requiredType == PropertyType.UNDEFINED){
                if(pd.getName().equals(propertyName))
                    nameUD = pd;
                else
                if(pd.getName().charAt(0)=='*' && wildcardUD == null)
                    wildcardUD = pd;
            }
        }
        
        
        if(nameUD != null) // if valueType is insignificant
            return new PDefPointer((PropertyDefinitionImpl)nameUD, PROPERTY_DEF_MATCH_TYPE.NAME_UNDEFINED);
        
        if(wildcardTyped != null)
            return new PDefPointer((PropertyDefinitionImpl)wildcardTyped, PROPERTY_DEF_MATCH_TYPE.WILDCARD_TYPED);

        if(wildcardUD != null)
            return new PDefPointer((PropertyDefinitionImpl)wildcardUD, PROPERTY_DEF_MATCH_TYPE.WILDCARD);

        if(unsupported != null)
            return new PDefPointer((PropertyDefinitionImpl)unsupported, PROPERTY_DEF_MATCH_TYPE.UNSUPPORTED);
        return null;
    }    
    
    /**
     * Switches hints applicability for Oracle SQL generation.
     */
    public void setHintsApplicable(boolean applicable){
        areHintsApplicable = applicable;
    }
    
    /**
     * Returns <code>true</code> if hints are applicable. Information is used
     * by XPathQuery to find should it add or not DB specific hints in a SQL.
     * @return
     */
    public boolean areHintsApplicable(){
        return areHintsApplicable;
    }
    
    /**
     * Describes found property definition. Uses PROPERTY_DEF_MATCH_TYPE enumeration.
     */
    private static class PDefPointer{
        PropertyDefinitionImpl propDef;
        PROPERTY_DEF_MATCH_TYPE type;
        
        PDefPointer(PropertyDefinitionImpl propDef, PROPERTY_DEF_MATCH_TYPE type){
            this.propDef=propDef;
            this.type=type;
        }
    }

    
    /**
     * Holds qname data as stored in DB.
     */
    public static class DBQName{
        private final Long nsId;
        private final String localName;
        private final String name;

        DBQName(){
            this.nsId=null;
            this.localName=null;
            this.name=null;
        }

        DBQName(String name, Long nsId, String localName){
            this.nsId=nsId;
            this.localName=QueryUtils.decodeEntity(localName);
            this.name=name;
        }

        public String getName(){
            return name;
        }
        
        public boolean hasNamespace(){
            return nsId != null;
        }
        
        public String getLocalName(){
            return localName;
        }
        
        public Long getNamespaceId(){
            return nsId;
        }
        
        public boolean isWildcard(){
            return nsId==null && localName==null;
        }
    }


    public void setAllowBrowse(boolean value) {
        this.allowBrowse  = value;
        
    }

    public boolean isAllowBrowse() {
        return allowBrowse;
    }    
}

/*
 * $Log: BuildingContext.java,v $
 * Revision 1.9  2008/10/09 13:27:28  maksims
 * #0153705 IllegalArgumentException will be reported if query against Name type property is executed
 *
 * Revision 1.8  2008/07/03 08:15:37  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/03 14:27:40  maksims
 * session settings based turning off/on security filter added
 *
 * Revision 1.6  2008/06/02 11:40:22  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/04/25 09:32:13  maksims
 * #1805668 Like made applicable to jcr:name local part. Namespace should always be provided
 *
 * Revision 1.4  2007/10/29 13:25:14  dparhomenko
 * decodng attribute name added (Max)
 *
 * Revision 1.3  2007/10/19 13:45:19  dparhomenko
 * migrate to ECR types
 *
 * Revision 1.2  2007/10/09 07:34:53  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/12/20 16:19:18  maksims
 * #1803635 javadocs added
 *
 * Revision 1.1  2006/12/15 13:13:29  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.4  2006/12/13 14:27:12  maksims
 * #1803572 Ordering redesigned
 *
 * Revision 1.3  2006/12/05 15:52:22  maksims
 * #1803540 Added ability to search by uuid
 *
 * Revision 1.2  2006/11/09 12:07:30  maksims
 * #1801897 SQL hints addition method used
 *
 * Revision 1.1  2006/11/02 17:28:06  maksims
 * #1801897 Query2 addition
 *
 */