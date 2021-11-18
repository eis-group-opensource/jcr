/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeDefImpl;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.nodetype.v283.NodeTypeDefinitionImpl;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.ValueFactoryImpl;
import com.exigen.cm.jackrabbit.value.ValueHelper;

public class NodeTypeImpl implements NodeType {

    /** Log for this class */
    private static final Log log = LogFactory.getLog(NodeTypeImpl.class);
    
    //private QName ntName;

    private NodeTypeDef def;

    private NodeTypeManagerImpl nodeTypeManager;

    private ArrayList<NodeDefinitionImpl> childNodes = new ArrayList<NodeDefinitionImpl>();

    private ArrayList<PropertyDefinitionImpl> childProperties = new ArrayList<PropertyDefinitionImpl>();

    private String name;
    
    private EffectiveNodeType _ent;

    private NamespaceRegistryImpl nsRegistry;

    
    

    public NodeTypeImpl(NamespaceRegistryImpl nsRegistry, NodeTypeManagerImpl nodeTypeManager, NodeTypeDef ntd) throws  RepositoryException {
        this.def = ntd;
        this.nodeTypeManager =nodeTypeManager;
        this.nsRegistry = nsRegistry;
        NodeDef[] childs = def.getChildNodeDefs();
        for(int i = 0 ; i < childs.length ; i++){
            childNodes.add(new NodeDefinitionImpl(nodeTypeManager, this, childs[i], nsRegistry));
        }
        PropDef[] props = def.getPropertyDefs();
        for(int i = 0 ; i < props.length ; i++){
            childProperties.add(new PropertyDefinitionImpl(this, props[i], nsRegistry));
        }
        try {
            this.name = getQName().toJCRName(nsRegistry);
        } catch (NoPrefixDeclaredException e) {
            throw new NoSuchNodeTypeException("Error creating node", e);
        }
        
            
    }
    
    public NodeTypeDef getNodeTypeDef() {
        return (NodeTypeDef)def.clone();
    }

    public String getName() {
        return name;
    }

    public boolean isMixin() {
        return def.isMixin();
    }

    public boolean hasOrderableChildNodes() {
        return def.hasOrderableChildNodes();
    }

    public String getPrimaryItemName() {
        try {
            QName piName = def.getPrimaryItemName();
            if (piName != null) {
                return piName.toJCRName(nsRegistry);
            } else {
                return null;
            }
        } catch (NoPrefixDeclaredException npde) {
            // should never get here
            log.error("encountered unregistered namespace in name of primary item", npde);
            return def.getName().toString();
        }
    }

    NodeType[] subTypes = null;

    public NodeType[] getSubtypes() throws RepositoryException{
    	if (subTypes == null){
    		ArrayList<NodeTypeImpl> result = new ArrayList<NodeTypeImpl>();
    		NodeTypeIterator nti = nodeTypeManager.getAllNodeTypes();
    		while (nti.hasNext()){
    			NodeTypeImpl nt = (NodeTypeImpl) nti.next();
    			if (nt.getEffectiveNodeType().includesNodeType(getQName()) &&
    					!nt.getName().equals(getName())){
    				result.add(nt);
    			}
    		}
    		subTypes = (NodeTypeImpl[]) result.toArray(new NodeTypeImpl[result.size()]);
    	}
    	return subTypes;
    }

    NodeType[] superTypes = null;
    
    public NodeType[] getSupertypes() {
    	if (superTypes == null){
	        QName[] ntNames = getEffectiveNodeType().getInheritedNodeTypes();
	        NodeType[] supertypes = new NodeType[ntNames.length];
	        for (int i = 0; i < ntNames.length; i++) {
	            try {
	                supertypes[i] = nodeTypeManager.getNodeType(ntNames[i]);
	            } catch (NoSuchNodeTypeException e) {
	                // should never get here
	                log.error("undefined supertype", e);
	                return new NodeType[0];
	            }
	        }
	        this.superTypes = supertypes;
    	}
    	return this.superTypes;
    }

    public NodeType[] getDeclaredSupertypes() {
        QName[] ntNames = def.getSupertypes();
        NodeType[] supertypes = new NodeType[ntNames.length];
        for (int i = 0; i < ntNames.length; i++) {
            try {
                supertypes[i] = nodeTypeManager.getNodeType(ntNames[i]);
            } catch (NoSuchNodeTypeException e) {
                // should never get here
                log.error("undefined supertype", e);
                return new NodeType[0];
            }
        }
        return supertypes;
    }

    public boolean isNodeType(String nodeTypeName) {
        QName ntName;
        try {
            ntName = QName.fromJCRName(nodeTypeName, getNamespaceResolver());
        } catch (IllegalNameException ine) {
            log.warn("invalid node type name: " + nodeTypeName, ine);
            return false;
        } catch (UnknownPrefixException upe) {
            log.warn("invalid node type name: " + nodeTypeName, upe);
            return false;
        }
        return (getQName().equals(ntName) || isDerivedFrom(ntName));
    }

    
    private NamespaceResolver getNamespaceResolver() {
        return nsRegistry;
    }

    PropertyDefinition[] allProps = null;

	private PropertyDefinition[] childPropertiesCahce;
    
    public PropertyDefinition[] getPropertyDefinitions() {
    	if (allProps == null){
	        PropDef[] pda = getEffectiveNodeType().getAllPropDefs();
	        PropertyDefinition[] propDefs = new PropertyDefinition[pda.length];
	        for (int i = 0; i < pda.length; i++) {
	            propDefs[i] = nodeTypeManager.getPropertyDefinition(pda[i]);
	        }
	        allProps = propDefs;
    	}
    	return allProps;
    }

    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return (PropertyDefinition[]) childProperties.toArray(new PropertyDefinition[childProperties.size()]);
    }

    public PropertyDefinition[] getDeclaredPropertyDefinitionsCache() {
    	if (childPropertiesCahce == null){
    		childPropertiesCahce = (PropertyDefinition[]) childProperties.toArray(new PropertyDefinition[childProperties.size()]);
    	}
    	return childPropertiesCahce;
    }

    public NodeDefinition[] getChildNodeDefinitions() {
        NodeDef[] cnda = getEffectiveNodeType().getAllNodeDefs();
        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
        for (int i = 0; i < cnda.length; i++) {
            nodeDefs[i] = nodeTypeManager.getNodeDefinition(cnda[i].getSQLId());
        }
        return nodeDefs;
    }

    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return (NodeDefinition[]) childNodes.toArray(new NodeDefinition[childNodes.size()]);
    }
    

    public boolean canSetProperty(String propertyName, Value value) {
        if (value == null) {
            // setting a property to null is equivalent of removing it
            return canRemoveItem(propertyName);
        }
        try {
            QName name = QName.fromJCRName(propertyName, nsRegistry);
            PropDef def;
            try {
                // try to get definition that matches the given value type
                def = getEffectiveNodeType().getApplicablePropertyDef(name, value.getType(), false);
            } catch (ConstraintViolationException cve) {
                // fallback: ignore type
                def = getEffectiveNodeType().getApplicablePropertyDef(name, PropertyType.UNDEFINED, false);
            }
            if (def.isProtected()) {
                return false;
            }
            if (def.isMultiple()) {
                return false;
            }
            int targetType;
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != value.getType()) {
                // type conversion required
                targetType = def.getRequiredType();
            } else {
                // no type conversion required
                targetType = value.getType();
            }
            // create InternalValue from Value and perform
            // type conversion as necessary
            InternalValue internalValue = InternalValue.create(value, targetType,
                    nsRegistry, null);
            EffectiveNodeType.checkSetPropertyValueConstraints(
                    def, new InternalValue[]{internalValue});
            return true;
        } catch (IllegalNameException be) {
            // implementation specific exception, fall through*/
        } catch (UnknownPrefixException upe) {
            // implementation specific exception, fall through*/
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    public boolean canSetProperty(String propertyName, Value[] values) {
    	if (values == null) {
            // setting a property to null is equivalent of removing it
            return canRemoveItem(propertyName);
        }
        try {
            QName name = parseQName(propertyName);
            // determine type of values
            int type = PropertyType.UNDEFINED;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    // skip null values as those would be purged
                    continue;
                }
                if (type == PropertyType.UNDEFINED) {
                    type = values[i].getType();
                } else if (type != values[i].getType()) {
                    // inhomogeneous types
                    return false;
                }
            }
            PropDef def;
            try {
                // try to get definition that matches the given value type
                def = getEffectiveNodeType().getApplicablePropertyDef(name, type, true);
            } catch (ConstraintViolationException cve) {
                // fallback: ignore type
                def = getEffectiveNodeType().getApplicablePropertyDef(name, PropertyType.UNDEFINED, true);
            }

            if (def.isProtected()) {
                return false;
            }
            if (!def.isMultiple()) {
                return false;
            }
            // determine target type
            int targetType;
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                // type conversion required
                targetType = def.getRequiredType();
            } else {
                // no type conversion required
                targetType = type;
            }

            ArrayList list = new ArrayList();
            // convert values and compact array (purge null entries)
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    // perform type conversion as necessary and create InternalValue
                    // from (converted) Value
                    InternalValue internalValue;
                    if (targetType != type) {
                        // type conversion required
                        Value targetVal = ValueHelper.convert(
                                values[i], targetType,
                                new ValueFactoryImpl());
                        internalValue = InternalValue.create(targetVal, this.nsRegistry, null);
                    } else {
                        // no type conversion required
                        internalValue = InternalValue.create(values[i], nsRegistry, null);
                    }
                    list.add(internalValue);
                }
            }
            InternalValue[] internalValues =
                    (InternalValue[]) list.toArray(new InternalValue[list.size()]);
            EffectiveNodeType.checkSetPropertyValueConstraints(def, internalValues);
            return true;
        } catch (RepositoryException re) {
            // fall through
        }
        return false;

    }

    public boolean canAddChildNode(String childNodeName) {
        try {
            getEffectiveNodeType().checkAddNodeConstraints(parseQName(childNodeName));
            return true;
        } catch (RepositoryException re) {
        }
        return false;
    }

	private QName parseQName(String childNodeName) throws RepositoryException {
		try {
			return QName.fromJCRName(childNodeName, nsRegistry);
		} catch (IllegalNameException e) {
			throw new RepositoryException(e.getMessage());
		} catch (UnknownPrefixException e) {
			throw new RepositoryException(e.getMessage());
		}
	}

    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
    	try {
            getEffectiveNodeType().checkAddNodeConstraints(
                    parseQName(childNodeName),
                    parseQName(nodeTypeName),
                    this.nodeTypeManager.getNodeTypeRegistry());
            return true;
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    public boolean canRemoveItem(String itemName) {
    	try {
            getEffectiveNodeType().checkRemoveItemConstraints(parseQName(itemName));
            return true;
        } catch (RepositoryException re) {
            // fall through
        }
        return false;
    }

    public QName getQName() {
        return def.getName();
    }


    public NodeDefinitionImpl getChildNodeDefinition(Long id){
        for(Iterator it = childNodes.iterator(); it.hasNext();){
            NodeDefinitionImpl def = (NodeDefinitionImpl) it.next();
            if (def.getSQLId().equals(id)){
                return def;
            }
        } 
        throw new RuntimeException("ChildNodeDefinition not found");
    }

    public PropertyDefinitionImpl getPropertyDefinition(Long id) throws NoSuchNodeTypeException {
        for(Iterator it = childProperties.iterator(); it.hasNext();){
            PropertyDefinitionImpl def = (PropertyDefinitionImpl) it.next();
            if (def.getSQLId().equals(id)){
                return def;
            }
        } 
        throw new NoSuchNodeTypeException();
    }

    public PropertyDefinitionImpl getPropertyDefinition(QName name) throws NoSuchNodeTypeException {
        for(Iterator it = childProperties.iterator(); it.hasNext();){
            PropertyDefinitionImpl def = (PropertyDefinitionImpl) it.next();
            if (def.getQName().equals(name)){
                return def;
            }
        } 
        throw new NoSuchNodeTypeException();
    }


    public Long getSQLId() {
        return def.getId();
    }

    HashMap<QName, Boolean> derivedCache = new HashMap<QName, Boolean>();
    
    /**
     * Checks if this node type is directly or indirectly derived from the
     * specified node type.
     *
     * @param nodeTypeName
     * @return true if this node type is directly or indirectly derived from the
     *         specified node type, otherwise false.
     * @throws RepositoryException 
     * @throws NoSuchNodeTypeException 
     */
    public boolean isDerivedFrom(QName nodeTypeName) {
    	Boolean result = derivedCache.get(nodeTypeName);
    	if (result == null){
    		result = !nodeTypeName.equals(def.getName()) && getEffectiveNodeType().includesNodeType(nodeTypeName);
    		derivedCache.put(nodeTypeName, result);
    	}
    	return result;
    }
    
    public String getTableName() {
        return def.getTableName();
    }

    private PropertyDefinitionImpl[] autoCreatedPropertyDefinitions;
    
    /**
     * Returns an array containing only those property definitions of this
     * node type (including the property definitions inherited from supertypes
     * of this node type) where <code>{@link PropertyDefinition#isAutoCreated()}</code>
     * returns <code>true</code>.
     *
     * @return an array of property definitions.
     * @throws RepositoryException 
     * @throws ConstraintViolationException 
     * @see PropertyDefinition#isAutoCreated
     */
    public PropertyDefinitionImpl[] getAutoCreatedPropertyDefinitions() throws ConstraintViolationException, RepositoryException {
    	if (autoCreatedPropertyDefinitions == null){
	        PropDef[] pda = getEffectiveNodeType().getAutoCreatePropDefs();
	        PropertyDefinitionImpl[] propDefs = new PropertyDefinitionImpl[pda.length];
	        for (int i = 0; i < pda.length; i++) {
	            propDefs[i] = nodeTypeManager.getPropertyDefinition(pda[i]);
	        }
	        autoCreatedPropertyDefinitions = propDefs;
    	}
    	return autoCreatedPropertyDefinitions;
    }

    public EffectiveNodeType getEffectiveNodeType() {
        if (_ent == null){ 
            try {
            	_ent = nodeTypeManager.getNodeTypeRegistry().getEffectiveNodeType(def.getName());
                //ent = EffectiveNodeType.create(nodeTypeManager.getNodeTypeRegistry(), def);
            } catch (Exception e) {
                //TODO fix me
                throw new RuntimeException(e);
            }
        } 
        return _ent;
    }

    private NodeDefinition[] autoCreatedNodeDefinitions;
    /**
     * Returns an array containing only those child node definitions of this
     * node type (including the child node definitions inherited from supertypes
     * of this node type) where <code>{@link NodeDefinition#isAutoCreated()}</code>
     * returns <code>true</code>.
     *
     * @return an array of child node definitions.
     * @throws NoSuchNodeTypeException 
     * @see NodeDefinition#isAutoCreated
     */
    public NodeDefinition[] getAutoCreatedNodeDefinitions() throws NoSuchNodeTypeException {
    	if (autoCreatedNodeDefinitions == null){
	        NodeDef[] cnda = getEffectiveNodeType().getAutoCreateNodeDefs();
	        NodeDefinition[] nodeDefs = new NodeDefinition[cnda.length];
	        for (int i = 0; i < cnda.length; i++) {
	            nodeDefs[i] = nodeTypeManager.getNodeDefinition(cnda[i].getSQLId());
	        }
	        autoCreatedNodeDefinitions = nodeDefs;
    	}
    	return autoCreatedNodeDefinitions;
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>def</code> are satisfied by the the specified <code>values</code>.
     * <p/>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param def    The definiton of the property
     * @param values An array of <code>InternalValue</code> objects.
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public static void checkSetPropertyValueConstraints(PropertyDefinitionImpl def,
                                                        InternalValue[] values)
            throws ConstraintViolationException, RepositoryException {
        EffectiveNodeType.checkSetPropertyValueConstraints(def.unwrap(), values);
    }

    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", getQName());
        builder.append("table", getTableName());
        return builder.toString();
    }

	public String getPresenceColumn() {
		return def.getPresenceColumn();
	}

	public NodeTypeTemplate createNodeTypeTemplate() throws RepositoryException{
		NodeTypeDefinitionImpl def1 = (NodeTypeDefinitionImpl) nodeTypeManager.createNodeTypeTemplate();
		
		ArrayList<String> tmp = new ArrayList<String>();
		for(QName n:def.getSupertypes()){
				try {
					tmp.add(n.toJCRName(nsRegistry));
				} catch (NoPrefixDeclaredException e) {
					throw new RepositoryException(e);
				}
		}
		def1.setDeclaredSuperTypeNames((String[]) tmp.toArray(new String[tmp.size()]));
		def1.setMixin(def.isMixin());
		def1.setName(name);
		def1.setOrderableChildNodes(def.hasOrderableChildNodes());
		def1.setPrimaryItemName(name);
		//add properties
		for(PropDef prop:def.getPropertyDefs()){
			PropDefImpl propImpl = (PropDefImpl) prop;
			PropertyDefinitionTemplate v = propImpl.createPropertyTemplate(nodeTypeManager.createPropertyDefinitionTemplate(), nsRegistry);
			def1.getPropertyDefintionTemplates().add(v);
		}
		//add nodes
		for(NodeDef node:def.getChildNodeDefs()){
			NodeDefImpl nodeImpl = (NodeDefImpl) node;
			NodeDefinitionTemplate v = nodeImpl.createNodeTemplate(nodeTypeManager.createNodeDefinitionTemplate(), nsRegistry);   
			def1.getNodeDefintionTemplates().add(v);
		}
		
		return def1;
	}
    
}


/*
 * $Log: NodeTypeImpl.java,v $
 * Revision 1.2  2007/07/20 08:41:05  dparhomenko
 * PTR#1804803 fix ptr
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.17  2007/03/02 09:31:58  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.16  2007/02/02 08:48:51  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.15  2007/01/24 08:46:24  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.14  2006/11/21 07:19:25  dparhomenko
 * PTR#1803402 fix errors
 *
 * Revision 1.13  2006/11/16 15:56:50  dparhomenko
 * PTR#1803402 fix errors
 *
 * Revision 1.12  2006/11/14 07:37:19  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.11  2006/11/02 09:53:47  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.10  2006/11/01 12:01:20  dparhomenko
 * PTR#0148728 fix isNodeTypeUsed
 *
 * Revision 1.9  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.8  2006/10/11 13:08:55  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.7  2006/09/28 12:23:30  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.6  2006/05/22 14:48:02  dparhomenko
 * PTR#1801941 add observationsupport
 *
 * Revision 1.5  2006/05/02 08:41:41  dparhomenko
 * PTR#0144983 fix effective node type null pointer exception
 *
 * Revision 1.4  2006/05/02 08:41:07  dparhomenko
 * PTR#0144983 fix effective node type null pointer exception
 *
 * Revision 1.3  2006/04/28 10:59:55  dparhomenko
 * PTR#0144983 add unimplemented methods
 *
 * Revision 1.2  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/13 10:03:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/03/29 12:56:19  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.8  2006/03/20 09:00:35  ivgirts
 * PTR #1801375 added methods for registering and altering node types
 *
 * Revision 1.7  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.6  2006/03/01 12:31:48  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.5  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/24 13:32:35  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/21 13:53:21  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/16 13:53:05  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */