/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_NAME;
import static com.exigen.cm.Constants.FIELD_NAMESPACE;
import static com.exigen.cm.Constants.TABLE_NODETYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__AUTO_CREATE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__DEFAULT_NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__MANDATORY;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__ON_PARENT_VERSION;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__PROTECTED;
import static com.exigen.cm.Constants.TABLE_NODETYPE_CHILDS__SAMENAMESIBLING;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__AUTO_CREATE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__COLUMN_NAME;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__FTS;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__INDEXABLE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__MANDATORY;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__MILTIPLE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__ON_PARENT_VERSION;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__PROTECTED;
import static com.exigen.cm.Constants.TABLE_NODETYPE_PROPERTY__REQUIRED_TYPE;
import static com.exigen.cm.Constants.TABLE_NODETYPE_SUPERTYPES;
import static com.exigen.cm.Constants.TABLE_NODETYPE_SUPERTYPES__CHILD;
import static com.exigen.cm.Constants.TABLE_NODETYPE_SUPERTYPES__PARENT;
import static com.exigen.cm.Constants.TABLE_NODETYPE__MIXIN;
import static com.exigen.cm.Constants.TABLE_NODETYPE__ORDERABLE_CHILDS;
import static com.exigen.cm.Constants.TABLE_NODETYPE__PRESENCECOLUMN;
import static com.exigen.cm.Constants.TABLE_NODETYPE__PRIMARY_ITEM_NAME;
import static com.exigen.cm.Constants.TABLE_NODETYPE__PRIMARY_ITEM_NAMESPACE;
import static com.exigen.cm.Constants.TABLE_NODETYPE__TABLENAME;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NAME;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NODE_TYPE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__PROP_DEF;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeManager283;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.CreateColumn;
import com.exigen.cm.database.statements.DatabaseCountStatement;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.DropColumn;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.ValueChangeDatabaseStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.iterators.NodeTypeIteratorImpl;
import com.exigen.cm.impl.nodetype.DBNodeTypeReader;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDefDiff;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.nodetype.ValueConstraint;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDefDiff.ChildNodeDefDiff;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDefDiff.PropDefDiff;
import com.exigen.cm.jackrabbit.nodetype.v283.NodeDefinitionTemplateImpl;
import com.exigen.cm.jackrabbit.nodetype.v283.NodeTypeDefinitionImpl;
import com.exigen.cm.jackrabbit.nodetype.v283.PropertyDefinitionTemplateImpl;
import com.exigen.cm.jackrabbit.util.IteratorHelper;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class NodeTypeManagerImpl implements NodeTypeManager, NodeTypeManager283 {

    private List<NodeTypeDef> defs = new ArrayList<NodeTypeDef>();
    private NamespaceRegistryImpl namespaceRegistry;
    private DBNodeTypeReader reader;
    private NodeTypeRegistry ntRegistry;
    private HashMap<Long, NodeType> defTypes = new HashMap<Long, NodeType>();
	private RepositoryImpl repository;

	private HashMap<Long, NodeTypeImpl> nodeTypeCacheById = new HashMap<Long, NodeTypeImpl>();
    private HashMap<Long, NodeDefinitionImpl> idCache = new HashMap<Long, NodeDefinitionImpl>();
    private HashMap<String,String> columnNameCache = new HashMap<String, String>(); 
    
    


    public NodeTypeManagerImpl(NamespaceRegistryImpl nmRegistry, DBNodeTypeReader reader, RepositoryImpl repository) throws RepositoryException {
        this.namespaceRegistry = nmRegistry;
        this.repository = repository;

        //load nodetypes
        this.reader = reader;
        reloadTypes();
        
        
    }

	private void reloadTypes() {
		this.defs = reader.all();
		this.ntRegistry = null;
	}

    HashMap<String, QName> qNameCache = new HashMap<String, QName>();
    
    /**
     * {@inheritDoc}
     * @throws  
     */
    public NodeType getNodeType(String nodeTypeName)
            throws NoSuchNodeTypeException {
        try {
        	QName result = qNameCache.get(nodeTypeName);
        	if (result != null){
        		return getNodeType(result);
        	}
            return getNodeType(QName.fromJCRName(nodeTypeName, namespaceRegistry));
        } catch (UnknownPrefixException upe) {
            throw new NoSuchNodeTypeException(nodeTypeName, upe);
        } catch (IllegalNameException ine) {
            throw new NoSuchNodeTypeException(nodeTypeName, ine);
        } catch (RepositoryException upe) {
            throw new NoSuchNodeTypeException(nodeTypeName, upe);
        } 
    }

    HashMap<QName, NodeTypeImpl> nodeTypeCache = new HashMap<QName, NodeTypeImpl>();
    
    public NodeTypeImpl getNodeType(QName name) throws NoSuchNodeTypeException{
    	NodeTypeImpl result = nodeTypeCache.get(name);
    	if (result != null){
    		return result;
    	}
        for(Iterator it = defs.iterator() ; it.hasNext() ;){
            NodeTypeDef def = (NodeTypeDef) it.next();
            if (def.getName().equals(name)){
                try {
                    result =  getNodeType(def);
                    nodeTypeCache.put(name, result);
                    return result;
                } catch (RepositoryException upe) {
                    throw new NoSuchNodeTypeException(name.toString(), upe);
                }
            }
        }
        throw new NoSuchNodeTypeException(name.toString());
    }

    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        QName[] ntNames = getNodeTypeRegistry().getRegisteredNodeTypes();
        ArrayList<NodeTypeImpl> list = new ArrayList<NodeTypeImpl>(ntNames.length);
        for (int i = 0; i < ntNames.length; i++) {
            list.add(getNodeType(ntNames[i]));
        }
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        ArrayList<NodeType> list = new ArrayList<NodeType>(defs.size());
        for (NodeTypeIterator nti = getAllNodeTypes(); nti.hasNext() ; ) {
            NodeType nt = nti.nextNodeType();
            if (!nt.isMixin()) {
                list.add(nt);
            }
        }
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        ArrayList<NodeType> list = new ArrayList<NodeType>(defs.size());
        for (NodeTypeIterator nti = getAllNodeTypes(); nti.hasNext() ; ) {
            NodeType nt = nti.nextNodeType();
            if (nt.isMixin()) {
                list.add(nt);
            }
        }
        return new IteratorHelper(Collections.unmodifiableCollection(list));
    }

    
    public NodeTypeDef findNodeTypeDef(QName name, Collection<NodeTypeDef> definitions) throws NoSuchNodeTypeException{
        NodeTypeDef existingType = null;
        for(Iterator it = defs.iterator() ; it.hasNext() ; ){
            NodeTypeDef def = (NodeTypeDef) it.next();
            if (name.equals(def.getName())){
                existingType = def;
                break;
            }
        }
        
        if (definitions != null){
	    	for(NodeTypeDef sd:definitions){
	        	if (sd.getName().equals(name)){
	        	    if( existingType != null ) {
                        // EPB-333 - return new definition, but provide the db id from the existing one
	        	        sd.setId( existingType.getId() );
	        	    }
	        		return sd;
	        	}
	        }
        }
        
        if( existingType != null ) {
            return existingType;
        }

        String msg = "Node Type with name \"{0}\" not found";
        msg = MessageFormat.format(msg, new Object[]{name});
        throw new NoSuchNodeTypeException(msg);        
    }
    
    public NodeTypeDef findNodeTypeDefById(Long id) throws NoSuchNodeTypeException{
        for(Iterator it = defs.iterator() ; it.hasNext() ; ){
            NodeTypeDef def = (NodeTypeDef) it.next();
            if (id.equals(def.getId())){
                return def;
            }
        }
        String msg = "Node Type with id \"{0}\" not found";
        msg = MessageFormat.format(msg, new Object[]{id});
        throw new NoSuchNodeTypeException(msg);        
    }
    
    
    public NodeTypeImpl getNodeTypeBySQLId(Long nodeTypeId){
    	NodeTypeImpl result = nodeTypeCacheById.get(nodeTypeId);
    	if (result != null){
    		return result;
    	}
        for(Iterator it = defs.iterator() ; it.hasNext() ;){
            NodeTypeDef def = (NodeTypeDef) it.next();
            if (def.getId().equals(nodeTypeId)){
                try {
                    result = getNodeType(def);
                    nodeTypeCacheById.put(nodeTypeId, result);
                    return result;
                } catch (NoSuchNodeTypeException e) {
                    throw new RuntimeException("Error creating node type");
                }  catch (RepositoryException upe) {
                    throw new RuntimeException(nodeTypeId.toString(), upe);
                }
            }
        }
        throw new RuntimeException("node type by id not found "+ nodeTypeId.toString());
    }

    private NodeTypeImpl getNodeType(NodeTypeDef def) throws RepositoryException {
        if (defTypes.containsKey(def.getId())){
            return (NodeTypeImpl) defTypes.get(def.getId());
        } else {
            NodeTypeImpl nt = new NodeTypeImpl(namespaceRegistry, this,def);
            defTypes.put(def.getId(), nt);
            return nt;
        }
    }
    
    public NodeDefinitionImpl getNodeDefinition(Long id) {
    	NodeDefinitionImpl result = idCache.get(id);
    	if (result !=null){
    		return result;
    	}
        for(Iterator it = defs.iterator() ; it.hasNext() ;){
            NodeTypeDef def = (NodeTypeDef) it.next();
            NodeDef[] childs = def.getChildNodeDefs();
            for(int i = 0 ; i < childs.length ; i++){
                if (childs[i].getSQLId().equals(id)){
                    result = ((NodeTypeImpl)getNodeTypeBySQLId(def.getId())).getChildNodeDefinition(id);
                    idCache.put(id, result);
                    return result;
                }
            }
        }
        throw new RuntimeException("Node definition not found");
    }

    public PropertyDefinitionImpl getPropertyDefinition(PropDef pd) {
        QName nodeTypeName = pd.getDeclaringNodeType();
        NodeTypeImpl nt;
        try {
            nt = getNodeType(nodeTypeName);
            return nt.getPropertyDefinition(pd.getSQLId());
        } catch (NoSuchNodeTypeException e) {
            throw new RuntimeException("PropertyDefinition not found");
        }
    }

    public DBNodeTypeReader getReader() {
        return reader;
    }

    public NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException {
        if (ntRegistry == null){
            ntRegistry = NodeTypeRegistry.create(this.namespaceRegistry,reader);
        }
        return ntRegistry;
    }
    
	public String findColumnName(QName typeName, QName propName) throws NoSuchNodeTypeException {
		String key = typeName.toString()+propName.toString();
		if (columnNameCache.containsKey(key)){
			return columnNameCache.get(key);
		}
		NodeTypeImpl type = getNodeType(typeName);
		String columnName = null;
		PropertyDefinitionImpl prop = type.getPropertyDefinition(propName);
		columnName = prop.getColumnName();
		columnNameCache.put(key, columnName);
		return columnName;
	}
	
    
    
    //------------------------ database operation specific

	private String updateColumnNameInPropDef(DatabaseConnection conn, NodeTypeDef nodeTypeDef, PropDef prop) throws RepositoryException {
		String columnName = prop.getColumnName();
		if (columnName == null){
			if (!prop.isMultiple() && prop.getName().getLocalName().indexOf("*")<0 && prop.getRequiredType() != PropertyType.UNDEFINED){
				
		    	String uri = prop.getName().getNamespaceURI();
		    	String prefix = "";
		    	if (uri != null && uri.length()> 0){
		    		prefix = this.namespaceRegistry.getPrefix(uri).toUpperCase()+"_";
		    	}
		    	columnName = JCRHelper.excludeSpecialCharacters("X_"+prefix+prop.getName().getLocalName().toUpperCase()+"_"+nodeTypeDef.getId());
		    	columnName = conn.getDialect().convertColumnName(columnName);
				((PropDefImpl)prop).setColumnName(columnName);
				
			}
		}
		return columnName;
	}    

    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("types",defs);
        return builder.toString();
    }

    
    public boolean isNodeTypeUsed(String nodeType) throws RepositoryException{
    	DatabaseConnection conn = repository.getConnectionProvider().createConnection();
    	try {
			return isNodeTypeUsed(nodeType, conn, new String[]{nodeType});
		} finally {
			conn.close();
		}
    }
    
    private boolean isNodeTypeUsed(String nodeType, DatabaseConnection conn, String[] skipNodeTypes) throws RepositoryException{
    	ArrayList<String> skipNodeTypesList = new ArrayList<String>(Arrays.asList(skipNodeTypes));
    	NodeTypeImpl nt = (NodeTypeImpl) getNodeType(nodeType);
    	NodeTypeIterator ni = getAllNodeTypes();
    	while(ni.hasNext()){
    		NodeTypeImpl nt2 = (NodeTypeImpl) ni.next();
    		if (nt2.getName().equals(nodeType) || skipNodeTypesList.contains(nt2.getName())){
    			continue;
    		}
    		if (!nt2.getName().equals(nt.getName())){
    			for(NodeType st:nt2.getSupertypes()){
    				if (st.getName().equals(nt.getName()) && !skipNodeTypesList.contains(nt.getName())){
    					return true;
    				}
    			}
    		}
    	}
    	
    	//check that other nodetypes does not have child Node definitions with this nodetype
    	ni = getAllNodeTypes();
    	while(ni.hasNext()){
    		NodeTypeImpl nt2 = (NodeTypeImpl) ni.next();
    		if (nt2.getName().equals(nodeType) || skipNodeTypesList.contains(nt2.getName())){
    			continue;
    		}
    		NodeDefinition[] childs = nt2.getChildNodeDefinitions();
    		for(NodeDefinition child:childs){
    			if (child.getDefaultPrimaryType() != null && child.getDefaultPrimaryType().getName().equals(nodeType)){
    				return true;
    			}
    			for(NodeType nt3:child.getRequiredPrimaryTypes()){
    				if (nt3.getName().equals(nodeType)){
    					return true;
    				}
    			}
    		}
    	}
    	
    	NodeTypeDef def = nt.getNodeTypeDef();
    	/*String tableName = def.getTableName();
		DatabaseCountStatement st = DatabaseTools.createCountStatement(tableName);
		st.addCondition(Conditions.eq(def.getPresenceColumn(), Boolean.TRUE));
		st.execute(conn);
		return st.getCount() > 0;*/
    	return isNodeTypeUsedInNodes(def, conn);
    	
    }

    public boolean isNodeTypeUsedInNodes(NodeTypeDef def, DatabaseConnection conn) throws RepositoryException{
    	//TODO use types table
    	String tableName = def.getTableName();
		DatabaseCountStatement st = DatabaseTools.createCountStatement(tableName);
		st.addCondition(Conditions.eq(def.getPresenceColumn(), Boolean.TRUE));
		st.execute(conn);
		boolean result =  st.getCount() > 0;
		st.close();
		if (!result){
			NodeTypeImpl nt = getNodeType(def);
			for(NodeType subType:nt.getSubtypes()){
				result = isNodeTypeUsedInNodes(((NodeTypeImpl)subType).getNodeTypeDef(), conn);
				if (result){
					break;
				}
			}
		}
		return result;
    	
    }
	
	//-----------------------------------------------------------------------
	// JCR 283 Features
	//-----------------------------------------------------------------------
	
	/**
     * Returns an empty <code>NodeTypeTemplate</code> which can then be used to
     * define a node type and passed to
     * <code>NodeTypeManager.registerNodeType</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     * @return A <code>NodeTypeTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeTypeTemplate createNodeTypeTemplate() throws UnsupportedRepositoryOperationException, RepositoryException{
    	return new NodeTypeDefinitionImpl();
    }
    
    /**
     * Returns a <code>NodeTypeTemplate</code> holding the definition of the
     * specified node type. This template can then be altered and passed to
     * <code>NodeTypeManager.registerNodeType</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param nodeType a <code>NodeType</code>.
     * @return A <code>NodeTypeTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeTypeTemplate createNodeTypeTemplate(NodeType nodeType) throws UnsupportedRepositoryOperationException, RepositoryException{
    	return ((NodeTypeImpl)nodeType).createNodeTypeTemplate();
    }
    

    /**
     * Returns an empty <code>NodeDefinitionTemplate</code> which can then be
     * used to create a child node definition and attached to a
     * <code>NodeTypeTemplate</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @return A <code>NodeDefinitionTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeDefinitionTemplate createNodeDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException{
    	return new NodeDefinitionTemplateImpl();
    }
    

    /**
     * Returns an empty <code>PropertyDefinitionTemplate</code> which can then
     * be used to create a property definition and attached to a
     * <code>NodeTypeTemplate</code>.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @return A <code>PropertyDefinitionTemplate</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException{
    	return new PropertyDefinitionTemplateImpl();
    }
    

    /**
     * Registers a new node type or updates an existing node type using the
     * specified definition and returns the resulting <code>NodeType</code>
     * object.
     * <p/>
     * Typically, the object passed to this method will be a
     * <code>NodeTypeTemplate</code> (a subclass of
     * <code>NodeTypeDefinition</code>) acquired from
     * <code>NodeTypeManager.createNodeTypeTemplate</code> and then filled-in
     * with definition information.
     * <p/>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if the
     * <code>NodeTypeDefinition</code> is invalid.
     * <p/>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and the <code>NodeTypeDefinition</code> specifies a
     * node type name that is already registered.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param ntd an <code>NodeTypeDefinition</code>.
     * @param allowUpdate a boolean
     * @return the registered node type
     * @throws InvalidNodeTypeDefinitionException if the
     *  <code>NodeTypeDefinition</code> is invalid.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *  <code>false</code> and the <code>NodeTypeDefinition</code> specifies a
     *  node type name that is already registered.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) 
    	throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException{
    	ArrayList<NodeTypeDefinition> defs = new ArrayList<NodeTypeDefinition>();
    	defs.add(ntd);
    	return registerNodeTypes(defs, allowUpdate).nextNodeType();
    }
    

    /**
     * Registers or updates the specified <code>Collection</code> of
     * <code>NodeTypeDefinition</code> objects. This method is used to register
     * or update a set of node types with mutual dependencies. Returns an
     * iterator over the resulting <code>NodeType</code> objects.
     * <p/>
     * The effect of the method is 'all or nothing' if an error occurs, no node
     * types are registered or updated.
     * <p/>
     * Throws an <code>InvalidNodeTypeDefinitionException</code> if a
     * <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     * invalid or if the <code>Collection</code> contains an object of a type
     * other than <code>NodeTypeDefinition</code>.
     * <p/>
     * Throws a <code>NodeTypeExistsException</code> if <code>allowUpdate</code>
     * is <code>false</code> and a <code>NodeTypeDefinition</code> within the
     * <code>Collection</code> specifies a node type name that is already
     * registered.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this
     * implementation does not support node type registration.
     *
     * @param definitions
     * @param allowUpdate
     * @return the registered node types.
     * @throws InvalidNodeTypeDefinitionException if a
     *  <code>NodeTypeDefinition</code> within the <code>Collection</code> is
     *  invalid or if the <code>Collection</code> contains an object of a type
     *  other than <code>NodeTypeDefinition</code>.
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is
     *  <code>false</code> and a <code>NodeTypeDefinition</code> within the
     *  <code>Collection</code> specifies a node type name that is already
     *  registered.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws RepositoryException if another error occurs.
     */
    public NodeTypeIterator registerNodeTypes(Collection definitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException{
    	DatabaseConnection conn = repository.getConnectionProvider().createConnection();
    	repository.switchToSingleMode();
    	try {
	    	ArrayList<NodeTypeDef> newDefs = new ArrayList<NodeTypeDef>();
	    	for(NodeTypeDefinitionImpl def: (Collection<NodeTypeDefinitionImpl>) definitions){
	    		NodeTypeDef d = def.toNodeTypeDef(namespaceRegistry);
	    		newDefs.add(d);
	    	}
    	
	    	registerNodeDefs(conn, newDefs, allowUpdate);

	    	ArrayList<NodeType> types = new ArrayList<NodeType>();
	    	for(NodeTypeDef d: newDefs){
	    		types.add(getNodeType(d.getName()));
	    	}
	    	return new NodeTypeIteratorImpl(types);
	    	
    	} finally {
    		repository.switchToNormalOperation();
    		conn.close();
    	}
    	
    }
    
    
    public void registerNodeDefs(DatabaseConnection conn , Collection<NodeTypeDef> definitions, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException{
    	ArrayList<NodeTypeDef> newDefs = new ArrayList<NodeTypeDef>();
    	ArrayList<NodeTypeDefDiff> modifiedDefs = new ArrayList<NodeTypeDefDiff>();
    	SchemaChanges changes = new SchemaChanges(conn);
    	for(NodeTypeDef d: definitions){
    		NodeTypeDef old = getNodeTypeDef(d.getName());
    		if (old == null){
    			newDefs.add(d);
    		} else {
    			if (!allowUpdate){
    				throw new NodeTypeExistsException(d.getName()._toJCRName(namespaceRegistry));
    			}
    			
    			NodeTypeDefDiff diff = NodeTypeDefDiff.create(old, d);
    			if (diff.isTrivial()){
    				
    			} if (!diff.isModified()){
    				continue;
    			} else {
    				//1. is this nodeTypes used in real nodes ?
    				boolean used = getNodeType(old.getName()).getSubtypes().length > 0;
    				if (!used){
    					used  = isNodeTypeUsedInNodes(old, conn);
    				}
    				
    				diff.setNodetypeUsed(used);
    				if (used){
    					//only if node type used, otherwise we can recreate this nodeType
	    				if (diff.supertypesChanged()){
	    					throw new RepositoryException("Can not change supertypes for "+ d.getName()._toJCRName(namespaceRegistry));
	    				}
	    				if (diff.childNodeDefsChanged()){
	    					for(ChildNodeDefDiff chDiff: (List<ChildNodeDefDiff>)diff.getChildNodeDefDiffs()){
	    						if (chDiff.getNewDef() == null){
	    							throw new RepositoryException("Can not remove child node definition for nodetype with existing nodes");
	    						}
	    						if (chDiff.getOldDef() == null && chDiff.getNewDef().isMandatory()){
	    							throw new RepositoryException("Can not add mandatory child node definition for nodetype with existing nodes");
	    						}
	    					}
	    				}
	    				if (diff.propertyDefsChanged()){
	    					for(PropDefDiff chDiff: (List<PropDefDiff>)diff.getPropDefDiffs()){
	    						if (chDiff.getNewDef() == null){
	    							//throw new RepositoryException("Can not remove property definition for nodetype with existing nodes");
	    						}
	    						if (chDiff.getOldDef() == null && chDiff.getNewDef().isMandatory()){
	    							throw new RepositoryException("Can not add mandatory property definition for nodetype with existing nodes");
	    						}
	    					}
	    				}
    				}
    			}
    			modifiedDefs.add(diff);
    		}
    		
    	}
	
    	//validate:
    	DBNodeTypeReader _nodeTypeReader = new DBNodeTypeReader(namespaceRegistry);
		_nodeTypeReader.loadNodeTypes(conn);
		for(NodeTypeDef def:newDefs){
			_nodeTypeReader.addNodeDefinition(def);
		}
		for(NodeTypeDefDiff diff:modifiedDefs){
			_nodeTypeReader.replaceNodeDef(diff.getNewDef());
		}
		
		NodeTypeManagerImpl ntm = new NodeTypeManagerImpl(namespaceRegistry,_nodeTypeReader, repository);
		ntm.validate();
		
		//everything is ok, probably we can persist changes, but first we should check modified node types
		
    	ArrayList<String> nameList = new ArrayList<String>();
		for(NodeTypeDefDiff diff: modifiedDefs){
			if (diff.isTrivial() || diff.isNodetypeUsed()){
				nameList.add(diff.getOldDef().getTableName());
			}
		}    	
    	ArrayList<TableDefinition> existings = new ArrayList<TableDefinition>();
    	NodeTypeIterator ni = getAllNodeTypes();
    	while(ni.hasNext()){
    		NodeTypeImpl nt = (NodeTypeImpl) ni.nextNodeType();
    		if (!nameList.contains(nt.getName())){
	    		TableDefinition td = nt.getNodeTypeDef().getTableDefinition(existings, conn);
	    		existings.add(td);
    		}
    	}
    	
    	ArrayList<String> alreadyRemovedTables = new ArrayList<String>();
		
		//1.clean trivial and not used type definition
    	ArrayList<NodeTypeDefDiff> forRemove = new ArrayList<NodeTypeDefDiff>();
		for(NodeTypeDefDiff diff: modifiedDefs){
			if (!diff.isNodetypeUsed()){//diff.isTrivial() && 
				cleanNodeType(conn, diff.getOldDef(), false, existings, alreadyRemovedTables);
				NodeTypeDef def = diff.getNewDef();
				def.setId(diff.getOldDef().getId());
				newDefs.add(def);
				forRemove.add(diff);
			}
		}
		modifiedDefs.removeAll(forRemove);
		
		//2.register new nodeTypes
		storeNodeTypes(conn, existings, newDefs, changes);
		
		//3.modify existing type definitions 
		alterNodeTypes(conn, modifiedDefs, changes, definitions);
		
		//conn.commit();
        //flush statements to database
        changes.execute(conn);
        //conn.createTables((TableDefinition[]) tables.toArray(new TableDefinition[tables.size()]));
        
		conn.commit();
        reader.loadNodeTypes(conn);
		repository.increaseNodeTypeCounter();

		reloadTypes();
		//TODO clean caches
		nodeTypeCache.clear();
		nodeTypeCacheById.clear();
    }    
    

    private void alterNodeTypes(DatabaseConnection conn, ArrayList<NodeTypeDefDiff> modifiedDefs, SchemaChanges changes, Collection<NodeTypeDef> allDefs) throws RepositoryException{
		for(NodeTypeDefDiff diff:modifiedDefs){
			if (diff.isTrivial() || !diff.isNodetypeUsed()) {
				diff.getNewDef().setId(diff.getOldDef().getId());
				diff.getNewDef().setTableName(diff.getOldDef().getTableName());
				diff.getNewDef().setPresenceColumn(diff.getOldDef().getPresenceColumn());

				//check child nodes
				for(ChildNodeDefDiff ch:(List<ChildNodeDefDiff>) diff.getChildNodeDefDiffs()){
					if (ch.getOldDef() == null){
						//create child definition
						createChildNodeDefinition(conn, diff.getOldDef(), ch.getNewDef(), changes, allDefs);
						//create column
					} else if (ch.getNewDef() == null){
						//drop child definition
						dropChildNodeDefinition(diff.getOldDef(), ch.getOldDef(), changes);
					} else {
						//modify child definition (recreate it)
						dropChildNodeDefinition(diff.getOldDef(), ch.getOldDef(), changes);
						createChildNodeDefinition(conn, diff.getOldDef(), ch.getNewDef(), changes, allDefs);
					}
				}
				
				//check properties
				for(PropDefDiff ch:(List<PropDefDiff>)diff.getPropDefDiffs()){
					if (ch.getOldDef() == null){
						//create child definition
						createPropertyDefinition(conn, diff.getNewDef(), ch.getNewDef(), changes, true);
						//create column
					} else if (ch.getNewDef() == null){
						//drop child definition
						dropPropertyDefinition(diff.getOldDef(), ch.getOldDef(), changes);
					} else {
						//modify child definition (recreate it)
						/*dropPropertyDefinition(diff.getOldDef(), ch.getOldDef(), statements);
						createPropertyDefinition(conn, diff.getOldDef(), ch.getNewDef(), statements);*/
						//TODO alter propdef
					}
				}
			} else {
				if (diff.isNodetypeUsed()){
					throw new RepositoryException("Nodetype cannot be altered (has instances)");
				} else {
					throw new RepositoryException("Nodetype cannot be altered ");
				}
			}
		}
		
	}

	private void dropPropertyDefinition(NodeTypeDef def, PropDef child, SchemaChanges changes) {
		// drop definition
    	//2. remove definition
    	DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_PROPERTY, FIELD_ID, child.getSQLId());
    	changes.add(st);
		
		//drop column if neccessary
    	if (child.isUnstructured()){
    		//1.a unstructured
    		if (child.isMultiple()){
    			//remove multiple values
    			DatabaseSelectAllStatement vs = DatabaseTools.createSelectAllStatement(TABLE_NODE_UNSTRUCTURED, false);
    			vs.addCondition(Conditions.eq(TABLE_NODE_UNSTRUCTURED__PROP_DEF, child.getSQLId()));
    			vs.addResultColumn(FIELD_ID);
    			
    			st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
    			st.addCondition(Conditions.in(TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY, vs));
    			changes.add(st);
    		}
			st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED);
			st.addCondition(Conditions.eq(TABLE_NODE_UNSTRUCTURED__PROP_DEF, child.getSQLId()));
			changes.add(st);
    	} else {
    		//1.b normal value		        		
           changes.add(new DropColumn(def.getTableName(), child.getColumnName()));
    	}
    	
		st = DatabaseTools.createDeleteStatement(TABLE_NODE_REFERENCE, TABLE_NODE_REFERENCE__PROPERTY_NODE_TYPE, def.getId());
		st.addCondition(Conditions.eq(TABLE_NODE_REFERENCE__PROPERTY_NAME, child.getName().getLocalName()));
		st.addCondition(Conditions.eq(TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE, this.namespaceRegistry._getByURI(child.getName().getNamespaceURI()).getId()));
		changes.add(st);
    	
		
		
	}

	private void createPropertyDefinition(DatabaseConnection conn, NodeTypeDef nodeTypeDef, PropDef prop, SchemaChanges changes, boolean createTableColumn) 
		throws RepositoryException{
		if (!createTableColumn){
			if (prop.getColumnName() == null){
				createTableColumn = true;
			}
		}
		//1. create property definition
        DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_NODETYPE_PROPERTY);
        Long id = conn.nextId();
        ((PropDefImpl)prop).setSQLId(id);
        st.addValue(SQLParameter.create(FIELD_ID, id));
        //nodetypeid
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__NODE_TYPE, nodeTypeDef.getId()));
        //name
        st.addValue(SQLParameter.create(FIELD_NAME, prop.getName().getLocalName()));
        //namespace
        String namespace = prop.getName().getNamespaceURI();
        if (namespace != null && namespace.length() > 0 ){
            Long namespaceId = namespaceRegistry._getNamespaceId(namespace);
            st.addValue(SQLParameter.create(FIELD_NAMESPACE, namespaceId));
        }
        //Required type
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__REQUIRED_TYPE, prop.getRequiredType()));
        //OPV
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__ON_PARENT_VERSION, prop.getOnParentVersion()));
        //autocreate
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__AUTO_CREATE, prop.isAutoCreated()));
        //mandatory
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__MANDATORY, prop.isMandatory()));
        //protected
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__PROTECTED, prop.isProtected()));
        //multiple
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__MILTIPLE, prop.isMultiple()));
        //indexable
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__INDEXABLE, prop.isIndexable()));
        //fts
        st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__FTS, prop.isFullTextSearch()));
        //columnName
        String columnName = updateColumnNameInPropDef(conn, nodeTypeDef, prop);
        if (columnName != null){
            st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY__COLUMN_NAME, prop.getColumnName()));  
        }
        
        changes.add(st);

        //constraints
        if (prop.getValueConstraints().length > 0){
            ValueConstraint[] vc = prop.getValueConstraints();
            
            for(int j = 0 ; j < vc.length ; j++){
                DatabaseInsertStatement _st = DatabaseTools.createInsertStatement(TABLE_NODETYPE_PROPERTY_CONSTRAINT);
                Long _id = conn.nextId();
                _st.addValue(SQLParameter.create(FIELD_ID, _id));
                _st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID, id));
                _st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY_CONSTRAINT__VALUE, vc[j].getDefinition()));
                changes.add(_st);
            }
            
        }
        //default values
        if (prop.getDefaultValues().length > 0){
        	InternalValue[] vc = prop.getDefaultValues();
            
            for(InternalValue value:vc){
                DatabaseInsertStatement _st = DatabaseTools.createInsertStatement(TABLE_NODETYPE_PROPERTY_DEFAULTVALUE);
                Long _id = conn.nextId();
                _st.addValue(SQLParameter.create(FIELD_ID, _id));
                _st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID, id));
                _st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__TYPE, value.getType()));
                _st.addValue(SQLParameter.create(TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__VALUE, value.toJCRValue(this.namespaceRegistry).getString()));
                changes.add(_st);
            }
            
        }
		
		
		//2. create column in table if neccessary
        if (!prop.isUnstructured() ) {
        	if (createTableColumn){
	        	TableDefinition table = nodeTypeDef.getTableDefinition(new ArrayList<TableDefinition>(), conn);
	            ColumnDefinition column = table.getColumn(prop.getColumnName());
	            changes.add(new CreateColumn(nodeTypeDef.getTableName(), column));
        	} 
        }
	}

	private void dropChildNodeDefinition(NodeTypeDef def, NodeDef child, SchemaChanges changes) {
		//remove required types
    	DatabaseDeleteStatement	st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES, 
    				TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID, child.getSQLId());
    	changes.add(st);
		
    	//remove child node definition
		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_CHILDS, FIELD_ID, child.getSQLId());
		changes.add(st);
	}

	private void createChildNodeDefinition(DatabaseConnection conn, NodeTypeDef def, NodeDef child, SchemaChanges changes, Collection<NodeTypeDef> allDefs) 
		throws RepositoryException{
        DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_NODETYPE_CHILDS);
        Long id = conn.nextId();
        child.setSQLId(id);
        st.addValue(SQLParameter.create(FIELD_ID, id));
        //nodetypeid
        st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__NODE_TYPE, def.getId()));
        //defaultNodeTypeId
        if (child.getDefaultPrimaryType() != null){
            NodeTypeDef defNodeType = findNodeTypeDef(child.getDefaultPrimaryType(), allDefs);
            st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__DEFAULT_NODE_TYPE, defNodeType.getId()));
        }
        //name
        st.addValue(SQLParameter.create(FIELD_NAME, child.getName().getLocalName()));
        //namespace
        String namespace = child.getName().getNamespaceURI();
        if (namespace != null && namespace.length() > 0 ){
            Long namespaceId = namespaceRegistry._getNamespaceId(namespace);
            st.addValue(SQLParameter.create(FIELD_NAMESPACE, namespaceId));
        }
        //OPV
        st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__ON_PARENT_VERSION, child.getOnParentVersion()));
        //autocreate
        st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__AUTO_CREATE, child.isAutoCreated()));
        //mandatory
        st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__MANDATORY, child.isMandatory()));
        //protected
        st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__PROTECTED, child.isProtected()));
        //SNS
        st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS__SAMENAMESIBLING, child.allowsSameNameSiblings()));
        changes.add(st);
        //add default node types
        QName[] requiredTypes = child.getRequiredPrimaryTypes();
        for(int j = 0 ; j < requiredTypes.length ; j++){
            QName name = requiredTypes[j];
            NodeTypeDef type = findNodeTypeDef(name, allDefs);
            st = DatabaseTools.createInsertStatement(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES);
            Long _id = conn.nextId();
            st.addValue(SQLParameter.create(FIELD_ID, _id));
            st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID, id));
            st.addValue(SQLParameter.create(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE, type.getId()));
            changes.add(st);
        }
		
	}

	private void storeNodeTypes(DatabaseConnection conn, ArrayList<TableDefinition> existings, ArrayList<NodeTypeDef> definitions, SchemaChanges changes) 
    	throws RepositoryException{
        
        //1. register in nodeType tables
        for(NodeTypeDef def: definitions){
            //1a. add to NodeTypes
            Long nodeTypeId = def.getId();
            ValueChangeDatabaseStatement vcSt;
            if (nodeTypeId == null){
	            nodeTypeId = conn.nextId();
	            vcSt = DatabaseTools.createInsertStatement(TABLE_NODETYPE);
	            vcSt.addValue(SQLParameter.create(FIELD_ID, nodeTypeId));
            } else {
            	vcSt = DatabaseTools.createUpdateStatement(TABLE_NODETYPE);
	            ((DatabaseUpdateStatement)vcSt).addCondition(Conditions.eq(FIELD_ID, nodeTypeId));
            }
            vcSt.addValue(SQLParameter.create(FIELD_NAME, def.getName().getLocalName()));
            String namespace = def.getName().getNamespaceURI();
            Long namespaceId = namespaceRegistry._getNamespaceId(namespace);
            vcSt.addValue(SQLParameter.create(FIELD_NAMESPACE, namespaceId));

            vcSt.addValue(SQLParameter.create(TABLE_NODETYPE__MIXIN, def.isMixin()));
            vcSt.addValue(SQLParameter.create(TABLE_NODETYPE__ORDERABLE_CHILDS, def.hasOrderableChildNodes()));
            if (def.getPrimaryItemName() != null){
            	vcSt.addValue(SQLParameter.create(TABLE_NODETYPE__PRIMARY_ITEM_NAME, def.getPrimaryItemName().getLocalName()));
                namespace = def.getPrimaryItemName().getNamespaceURI();
                namespaceId = namespaceRegistry._getNamespaceId(namespace);
                vcSt.addValue(SQLParameter.create(TABLE_NODETYPE__PRIMARY_ITEM_NAMESPACE, namespaceId));
            }
        	updateNodeDerf(def, conn, namespaceRegistry);
            vcSt.addValue(SQLParameter.create(TABLE_NODETYPE__TABLENAME, def.getTableName()));
            vcSt.addValue(SQLParameter.create(TABLE_NODETYPE__PRESENCECOLUMN, def.getPresenceColumn()));
            //st.addValue(SQLParameter.create(TABLE_NODETYPE__EMBEDED, def.isEmbeded()));
            def.setId(nodeTypeId);
            changes.add(vcSt);
            
        }         
        
        //2. create detail table
        ArrayList<TableDefinition> tables = new ArrayList<TableDefinition>();
        for(NodeTypeDef def: definitions){
        	ArrayList<TableDefinition> _tt = new ArrayList<TableDefinition>(tables);
        	_tt.addAll(existings);
            TableDefinition tableDef = def.getTableDefinition(_tt, conn);
            tables.add(tableDef);
            changes.add(tableDef);
        }
        
        
        //1b supertypes
        for(NodeTypeDef def: definitions){
            QName[] superTypes = def.getSupertypes();
            for(int i = 0 ; i < superTypes.length ; i++){
                QName superType = superTypes[i];
                NodeTypeDef superDef = findNodeTypeDef(superType, definitions);
                
                DatabaseInsertStatement st = DatabaseTools.createInsertStatement(TABLE_NODETYPE_SUPERTYPES);
                Long id = conn.nextId();
                st.addValue(SQLParameter.create(FIELD_ID, id));
                st.addValue(SQLParameter.create(TABLE_NODETYPE_SUPERTYPES__PARENT, superDef.getId()));
                st.addValue(SQLParameter.create(TABLE_NODETYPE_SUPERTYPES__CHILD, def.getId()));
                changes.add(st);
            }
        }
        //1c child nodes
        for(NodeTypeDef def: definitions){
            for (NodeDef child: def.getChildNodeDefs()){
            	createChildNodeDefinition(conn, def, child, changes, definitions);
            }        	
        }
        
        //1d properties
        for(NodeTypeDef def: definitions){
            for(PropDef prop:def.getPropertyDefs()) {
            	createPropertyDefinition(conn, def, prop, changes, false);
            } 

        }
        

	}

	public static void updateNodeDerf(NodeTypeDef def, DatabaseConnection conn, NamespaceRegistryImpl namespaceRegistry) throws RepositoryException{
        if (def.getTableName() == null){
            //throw new RepositoryException("Table name not defined for "+def.getName());
        	String uri = def.getName().getNamespaceURI();
        	String prefix = "";
        	if (uri != null && uri.length()> 0){
        		prefix = namespaceRegistry.getPrefix(uri).toUpperCase()+"_";
        	}
        	def.setTableName(JCRHelper.excludeSpecialCharacters("CM_TYPE_"+prefix+def.getName().getLocalName().toUpperCase()));
        }
        if (def.getPresenceColumn() == null){
            //throw new RepositoryException("Table name not defined for "+def.getName());
        	String uri = def.getName().getNamespaceURI();
        	String prefix = "";
        	if (uri != null && uri.length()> 0){
        		prefix = namespaceRegistry.getPrefix(uri).toUpperCase()+"_";
        	}
        	def.setPresenceColumn(conn.getDialect().convertColumnName("T_"+prefix+def.getName().getLocalName().toUpperCase()));
        }

        def.setTableName(conn.getDialect().convertTableName(def.getTableName()));
	}

	private void cleanNodeType(DatabaseConnection conn, NodeTypeDef def, boolean dropTypeRow, ArrayList<TableDefinition> existings, ArrayList<String> alreadyRemovedTables) 
    	throws RepositoryException{
    	//1.remove table
		TableDefinition table = def.getTableDefinition(existings, conn);
		if (!table.isAlter()){
			if (!alreadyRemovedTables.contains(table.getTableName())){
				conn.dropTable(table);
				alreadyRemovedTables.add(table.getTableName());
			}
		} else {
			for(Iterator<ColumnDefinition> ci = table.getColumnIterator(); ci.hasNext();){
				ColumnDefinition column = ci.next();
				if (!column.getColumnName().equals(Constants.FIELD_TYPE_ID)){
					String sql = conn.getDialect().buildAlterTableDropColumn(def.getTableName(), column.getColumnName());
					conn.execute(sql);
				}
			}
		}
    	//2.remove supertypes
		Long id = def.getId();
		DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_SUPERTYPES, 
				TABLE_NODETYPE_SUPERTYPES__PARENT, id);
		st.execute(conn);
		
		//st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES, TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__NODE_TYPE, id);
		//st.execute(conn);
		
		//3.remove required type links
		for(NodeDef child:def.getChildNodeDefs()){
    		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_CHILDS_REQUIRED_TYPES, 
    				TABLE_NODETYPE_CHILDS_REQUIRED_TYPES__CHILD_ID, child.getSQLId());
    		st.execute(conn);
		}
		
		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_CHILDS, TABLE_NODETYPE_CHILDS__NODE_TYPE, id);
		st.execute(conn);
		//remove property definitions
		DatabaseSelectAllStatement st2 = DatabaseTools.createSelectAllStatement(TABLE_NODETYPE_PROPERTY, true);
		st2.addCondition(Conditions.eq(TABLE_NODETYPE_PROPERTY__NODE_TYPE, id));
		st2.execute(conn);
		while(st2.hasNext()){
			RowMap row = st2.nextRow();
			Long propId = row.getLong(FIELD_ID);
    		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_PROPERTY_CONSTRAINT, TABLE_NODETYPE_PROPERTY_CONSTRAINT__PROPERTY_ID, propId);
    		st.execute(conn);
    		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_PROPERTY_DEFAULTVALUE, TABLE_NODETYPE_PROPERTY_DEFAULTVALUE__PROPERTY_ID, propId);
    		st.execute(conn);

    		//clean unstructured props (multiple)
    		DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(TABLE_NODE_UNSTRUCTURED, false);
    		st1.addCondition(Conditions.eq(TABLE_NODE_UNSTRUCTURED__PROP_DEF, propId));
    		st1.addResultColumn(Constants.FIELD_ID);
    		st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED_VALUES);
    		st.addCondition(Conditions.in(TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY, st1));
    		st.execute(conn);
    		
    		//clean unstructured props    		
    		st = DatabaseTools.createDeleteStatement(TABLE_NODE_UNSTRUCTURED, TABLE_NODE_UNSTRUCTURED__PROP_DEF, propId);
    		st.execute(conn);
		}
		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_PROPERTY, TABLE_NODETYPE_PROPERTY__NODE_TYPE, id);
		st.execute(conn);
		st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE_SUPERTYPES, TABLE_NODETYPE_SUPERTYPES__CHILD, id);
		st.execute(conn);
		
		//remove type row
		if (dropTypeRow){
			st = DatabaseTools.createDeleteStatement(TABLE_NODETYPE, FIELD_ID, id);
			st.execute(conn);
		}
		//remove records in node refs table
		st = DatabaseTools.createDeleteStatement(TABLE_NODE_REFERENCE, TABLE_NODE_REFERENCE__PROPERTY_NODE_TYPE, id);
		st.execute(conn);
	}

	private void validate() throws RepositoryException {
		getNodeTypeRegistry();
		NodeTypeIterator allTypes = getAllNodeTypes();
        // gets all types and finds a type there having specified as super. 
        while (allTypes.hasNext()) {
            allTypes.nextNodeType();
        }

		
	}

	public NodeTypeDef getNodeTypeDef(QName name) {
		for(NodeTypeDef d:defs){
			if (d.getName().equals(name)){
				return (NodeTypeDef)d.clone();
			}
		}
		return null;
	}

	/**
     * Unregisters the specified node type.
     * <p/>
     * Throws a <code>NoSuchNodeTypeException</code> if no registered node type
     * exists with the specified name.
     *
     * @param name a <code>String</code>.
     * @throws UnsupportedRepositoryOperationException if this implementation
     *  does not support node type registration.
     * @throws NoSuchNodeTypeException if no registered node type exists with
     *  the specified name.
     * @throws RepositoryException if another error occurs.
     */
    public void unregisterNodeType(String name) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException{
    	unregisterNodeTypes(new String[]{name});
    }
    

    /**
     * Unregisters the specified set of node types. Used to unregister a set of node types with mutual dependencies.
     * <p/>
     * Throws a <code>NoSuchNodeTypeException</code> if one of the names listed is not a registered node type.
     * <p/>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if this implementation does not support node type registration.
     *
     * @param names a <code>String</code> array
     * @throws UnsupportedRepositoryOperationException if this implementation does not support node type registration.
     * @throws NoSuchNodeTypeException if one of the names listed is not a registered node type.
     * @throws RepositoryException if another error occurs.
     */
    public void unregisterNodeTypes(String[] names) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException{
    	DatabaseConnection conn = repository.getConnectionProvider().createConnection();
    	try {
	    	for(String nodeType:names){
	        	if (isNodeTypeUsed(nodeType, conn, names)){
	        		throw new RepositoryException("Node type " +nodeType+" is used either in other node types or has instances");
	        	}
	    	}
	    
	    	ArrayList<String> nameList = new ArrayList<String>(Arrays.asList(names));
	    	
	    	ArrayList<TableDefinition> existings = new ArrayList<TableDefinition>();
	    	NodeTypeIterator ni = getAllNodeTypes();
	    	while(ni.hasNext()){
	    		NodeTypeImpl nt = (NodeTypeImpl) ni.nextNodeType();
	    		if (!nameList.contains(nt.getName())){
		    		TableDefinition td = nt.getNodeTypeDef().getTableDefinition(existings, conn);
		    		existings.add(td);
	    		}
	    	}
	    	
	    	ArrayList<String> alreadyRemovedTables = new ArrayList<String>();
	    	
	    	for(String nodeType:names){
	    		NodeTypeImpl nt = (NodeTypeImpl) getNodeType(nodeType);
	    		NodeTypeDef def = nt.getNodeTypeDef();
	    		cleanNodeType(conn, def, true, existings, alreadyRemovedTables);
	    	}
	    	
	    	
	    	conn.commit();
    	} finally {
    		conn.close();
    	}
    }

	public boolean hasNodeType(String nodeTypeName) {
		try {
			getNodeType(QName.fromJCRName(nodeTypeName, namespaceRegistry));
			return true;
		} catch (Exception exc){
			return false;
		}
	}
        

}


/*
 * $Log: NodeTypeManagerImpl.java,v $
 * Revision 1.10  2012/03/02 12:20:50  jkersovs
 * EPB-333 'EFolderImportNodetypes update is corrupting JCR repository'
 * Applying update created by V.Vingolds
 *
 * Revision 1.9  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2007/08/29 12:55:30  dparhomenko
 * fix drop procedure for version without fts
 *
 * Revision 1.7  2007/08/07 11:44:54  dparhomenko
 * PTR#1805004 fix ptr
 *
 * Revision 1.6  2007/07/31 07:41:50  dparhomenko
 * PTR#1804803 fix ptr
 *
 * Revision 1.5  2007/07/10 10:17:41  dparhomenko
 * PTR#0152003 fix insert statemnt for oracle blobs
 *
 * Revision 1.4  2007/05/09 12:33:44  dparhomenko
 * PTR#1804311 add test-environment directory
 *
 * Revision 1.3  2007/05/09 11:14:59  dparhomenko
 * PTR#1804311 add test-environment directory
 *
 * Revision 1.2  2007/05/08 09:39:30  dparhomenko
 * PTR#1804279 migrate VFCommons to maven from B302 directory
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.35  2007/03/02 09:31:59  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.34  2007/02/26 14:39:09  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.33  2007/02/26 09:46:00  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.32  2007/02/05 13:14:03  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.31  2007/01/24 08:46:24  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.30  2006/12/12 11:14:21  dparhomenko
 * PTR#1803217 improve performance of NodeState.getEffectiveNodeType()
 *
 * Revision 1.29  2006/12/11 12:49:33  dparhomenko
 * PTR#1803596 allow create nodetype with spaces in name
 *
 * Revision 1.28  2006/12/11 12:17:56  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.27  2006/12/08 11:58:22  dparhomenko
 * PTR#0149324 fix security merge on node copy
 *
 * Revision 1.26  2006/12/07 08:38:43  dparhomenko
 * PTR#0149290 fix alter nodetype : add property with # sign
 *
 * Revision 1.25  2006/12/07 08:02:25  dparhomenko
 * PTR#0149114 fix nodetype alter error message
 *
 * Revision 1.24  2006/11/22 09:21:46  dparhomenko
 * PTR#1803351 allow add mandatory property to unused nodetype
 *
 * Revision 1.23  2006/11/22 09:15:23  dparhomenko
 * PTR#1803351 allow add mandatory property to unused nodetype
 *
 * Revision 1.22  2006/11/21 07:53:28  dparhomenko
 * PTR#1803402 fix errors
 *
 * Revision 1.21  2006/11/15 13:00:09  dparhomenko
 * PTR#0149118 fix remove node type
 *
 * Revision 1.20  2006/11/15 12:48:54  dparhomenko
 * PTR#0149114 fix alter node type
 *
 * Revision 1.19  2006/10/31 13:41:11  dparhomenko
 * PTR#0148728 fix isNodeTypeUsed
 *
 * Revision 1.18  2006/10/24 12:58:13  dparhomenko
 * PTR#0148701 fix node copy
 *
 * Revision 1.17  2006/10/23 14:38:15  dparhomenko
 * PTR#0148641 fix data import
 *
 * Revision 1.16  2006/10/20 13:36:20  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.15  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.14  2006/10/11 13:08:55  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.13  2006/10/09 11:22:50  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.12  2006/10/05 14:13:14  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.11  2006/10/03 09:29:26  dparhomenko
 * PTR#1803057 fix getPrimaryItemName
 *
 * Revision 1.10  2006/10/02 15:07:09  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.9  2006/09/26 10:11:07  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.8  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.7  2006/08/15 12:19:00  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.6  2006/08/11 11:30:13  dparhomenko
 * PTR#1802633 fix problem with delete node
 *
 * Revision 1.5  2006/08/07 14:25:55  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.4  2006/07/06 09:29:15  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/04/24 08:55:17  dparhomenko
 * PTR#0144983 fts
 *
 * Revision 1.2  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/13 10:03:44  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/05 09:04:06  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.13  2006/03/21 13:19:27  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.12  2006/03/20 09:00:35  ivgirts
 * PTR #1801375 added methods for registering and altering node types
 *
 * Revision 1.11  2006/03/14 11:55:40  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.10  2006/03/13 09:24:33  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.9  2006/03/03 11:07:49  ivgirts
 * PTR #1801059 thorws SQLException replaced with throws RepositoryException
 *
 * Revision 1.8  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.7  2006/02/27 16:22:19  ivgirts
 * PTR#1801059 Configuration file added.
 *
 * Revision 1.6  2006/02/27 15:53:55  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.5  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/13 12:40:40  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 * Revision 1.1  2006/02/10 15:50:23  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */
