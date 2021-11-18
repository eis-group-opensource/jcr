/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__FROM;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NAME;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE__TO;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.impl.iterators.NodeIteratorImpl;
import com.exigen.cm.impl.iterators.PropertyIteratorImpl;
import com.exigen.cm.impl.state.NodeState;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.jackrabbit.lock.LockManager;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.util.ChildrenCollectorFilter;
import com.exigen.cm.jackrabbit.util.IteratorHelper;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.ValueHelper;
import com.exigen.cm.jackrabbit.version.GenericVersionSelector;
import com.exigen.cm.jackrabbit.version.InternalFreeze;
import com.exigen.cm.jackrabbit.version.InternalFrozenNode;
import com.exigen.cm.jackrabbit.version.InternalFrozenVersionHistory;
import com.exigen.cm.jackrabbit.version.InternalVersion;
import com.exigen.cm.jackrabbit.version.InternalVersionHistory;
import com.exigen.cm.jackrabbit.version.VersionHistoryImpl;
import com.exigen.cm.jackrabbit.version.VersionManagerImpl;
import com.exigen.cm.jackrabbit.version.VersionSelector;

public class NodeImpl extends _NodeImpl implements Node, NodeState {

    /** Log for this class */
    private static final Log log = LogFactory.getLog(NodeImpl.class);

    
    protected HashMap<String, Long> childNodesId = new HashMap<String, Long>();
    
    //TODO make privat
	_NodeState state;


	private SessionImpl session;

    public NodeImpl(_NodeState state, _AbstractsStateManager sm) {
        super(state, sm);
        this.session = ((_SessionStateManager)sm).getSession();
        //this._nodeId = nodeId;
        
        //reloadNode(row, false);
        //this.state = session.getStateManager().getNodeState(((NodeId)itemId).getId());
        this.state = (_NodeState) getItemState();;
        //initializationStage = false;
    } 
    
    public  SessionImpl _getSession(){
    	return session;
    }
    
    
    public Node addNode(String relPath, String nodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException{
        sanityCheck();
        NodeTypeImpl nt = null;
        if (nodeTypeName != null){
            nt = (NodeTypeImpl) session.getWorkspace().getNodeTypeManager().getNodeType(nodeTypeName);
        }
        return (Node) internalAddNode(relPath, nt, null);
    }

    public Node addNode(String relPath, QName nodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException{
        sanityCheck();
        NodeTypeImpl nt = null;
        if (nodeTypeName != null){
            nt = (NodeTypeImpl) ((NodeTypeManagerImpl)session.getWorkspace().getNodeTypeManager()).getNodeType(nodeTypeName);
        }
        return (Node) internalAddNode(relPath, nt, null);
    }


    
    



    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        return addNode(relPath, (String) null);
    }

    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }


    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        int type;
        if (values == null || values.length == 0
                || values[0] == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }
        return setProperty(name, values, type);
    }

    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status);
        try {
            if (type == PropertyType.UNDEFINED) {
                prop.setValue(values);
            } else {
                prop.setValue(ValueHelper.convert(values, type));
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
                    
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        /**
         * if the target property is not of type STRING then a
         * best-effort conversion is tried
         */
        return setProperty(name, values, PropertyType.UNDEFINED);
    }

    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status);
        try {
            if (type == PropertyType.UNDEFINED) {
                prop.setValue(values);
            } else {
                prop.setValue(ValueHelper.convert(values, type));
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
                    
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        /**
         * if the target property is not of type STRING then a
         * best-effort conversion is tried
         */
        return setProperty(name, value, PropertyType.UNDEFINED);
    }

    public Property setProperty(QName name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        /**
         * if the target property is not of type STRING then a
         * best-effort conversion is tried
         */
        return setProperty(name, value, PropertyType.UNDEFINED);
    }

    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.BINARY, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.BOOLEAN, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.DOUBLE, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.LONG, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.DATE, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop;
        try {
        	prop = getOrCreateProperty(name, PropertyType.REFERENCE, false, status);
        } catch (ConstraintViolationException exc){
        	try {
        		prop = getOrCreateProperty(name, PropertyType283.WEAKREFERENCE, false, status);
        	} catch (ConstraintViolationException exc1) {
        		throw exc;
        	}
        }
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                	e.printStackTrace();
                    //TODO may be exception ??
                    //throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();
        Long id = resolveRelativeNodePath(relPath, true);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        try {
            return session.getNodeManager().buildNode(getStateManager().getNodeState(id, null), true);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }
    }

    public NodeIterator getNodes() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return getNodes(true, null);
    }

    public NodeIterator getNodes(int offset, int limit) throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return getNodes(true, null, offset, limit);
    }

    public NodeIterator getNodes(QName[] nodetypes) throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return getNodes(true, nodetypes);
    }

    protected NodeIterator getNodes(boolean checkSecurity, QName[] nodetypes) throws RepositoryException {
        return getNodes(checkSecurity, nodetypes, 0, -1);
    }

    
    protected NodeIterator getNodes(boolean checkSecurity, QName[] nodetypes, int offset, int limit) throws RepositoryException {
        return new NodeIteratorImpl(session, stateManager.getChildNodesId(state, checkSecurity, null, nodetypes, offset, limit));
    }

    public NodeIterator getNodes(String namePattern) throws RepositoryException {
    	return getNodes(namePattern, true);
    }
    
    public NodeIterator getNodes(String namePattern, boolean checkSecurity) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        //TODO convert regexp to SQL 
        return new NodeIteratorImpl(session, stateManager.getNodesWithPattern(state, namePattern, checkSecurity));
    }


    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        PropertyId id = state.resolveRelativePropertyPath(relPath);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        //try {
            //return (Property) itemMgr.getItem(id);
            //throw new UnsupportedOperationException();
            //TODO optimize this
            return getProperty(id.getName());
        //} catch (AccessDeniedException ade) {
        //    throw new PathNotFoundException(relPath);
        //}
    }


    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        ArrayList<PropertyImpl> properties = new ArrayList<PropertyImpl>();
        // traverse children using a special filtering 'collector'
        accept(new ChildrenCollectorFilter(namePattern, properties, false, true, 1));
        return new IteratorHelper(Collections.unmodifiableList(properties));
    }

    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        String name = getPrimaryNodeType().getPrimaryItemName();
        if (name == null) {
            throw new ItemNotFoundException();
        }
        if (hasProperty(name)) {
            return getProperty(name);
        } else if (hasNode(name)) {
            return getNode(name);
        } else {
            throw new ItemNotFoundException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getUUID()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();
        
        if (!_isNodeType(QName.MIX_REFERENCEABLE)) {
            throw new UnsupportedRepositoryOperationException();
        }
        
        return getInternalUUID();
    }
    


 
    /**
     * {@inheritDoc}
     */

    public boolean hasProperties() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        //TODO
        /**
         * hasProperties respects the access rights
         * of this node's session, i.e. it will
         * return false if properties exist
         * but the session is not granted read-access
         */
        //return itemMgr.hasChildProperties((NodeId) id);
        return true;
    }



    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return state.getMixinNodeTypes();
    }

    /**
     * {@inheritDoc}
     */
    public void addMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        QName ntName;
        try {
            ntName = session.getNamespaceResolver().getQName(mixinName);
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, upe);
        }

        addMixin(ntName);
    }
    
    

	/**
     * {@inheritDoc}
     */
    public void removeMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        QName ntName;
        try {
            ntName = session.getNamespaceResolver().getQName(mixinName);
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, upe);
        }

        removeMixin(ntName);
    }
    

    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check checked-out status
        if (!internalIsCheckedOut(false)) {
            return false;
        }

        // check protected flag
        if (getDefinition().isProtected()) {
            return false;
        }

        // check lock status
        //TODO uncomment me
        try {
            checkLock();
        } catch (LockException le) {
            return false;
        }
        

        QName ntName;
        try {
            ntName = session.getNamespaceResolver().getQName(mixinName);
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid mixin type name: "
                    + mixinName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid mixin type name: "
                    + mixinName, upe);
        }

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl mixin = ntMgr.getNodeType(ntName);
        if (!mixin.isMixin()) {
            return false;
        }
        NodeTypeImpl primaryType = ntMgr.getNodeType(state.getPrimaryTypeName());
        if (primaryType.isDerivedFrom(ntName)) {
            return false;
        }

        // build effective node type of mixins & primary type
        // in order to detect conflicts
        NodeTypeRegistry ntReg = session.getNodeTypeRegistry();
        EffectiveNodeType entExisting;
        try {
            // existing mixin's
            HashSet<QName> set = new HashSet<QName>(getMixinTypeNames());
            // primary type
            set.add(state.getPrimaryTypeName());
            // build effective node type representing primary type including existing mixin's
            entExisting = ntReg.getEffectiveNodeType(set.toArray(new QName[set.size()]));
            if (entExisting.includesNodeType(ntName)) {
                return false;
            }
            // add new mixin
            set.add(ntName);
            // try to build new effective node type (will throw in case of conflicts)
            ntReg.getEffectiveNodeType(set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            return false;
        }
        
        HashSet<QName> set = new HashSet<QName>();
        set.add(ntName);
        //check existing properties
        try {
			entExisting = ntReg.getEffectiveNodeType(set.toArray(new QName[set.size()]));
	        for(_PropertyState p:state.getProperties()){
	        	QName propName = p.getName();
	        	int propType = p.getType();
	        	try {
	        		PropDef propDef = entExisting.getApplicablePropertyDef(propName, PropertyType.UNDEFINED);
	        		boolean fromCommonParent = false;
	        		QName parent1 = propDef.getDeclaringNodeType();
	        		if (!parent1.equals(ntName)){
	        			QName parent2 = ((NodeTypeImpl)getProperty(propName).getDefinition().getDeclaringNodeType()).getQName();
	        			if (parent2.equals(parent1)){
	        				fromCommonParent = true;
	        			}
	        		}
	        		if (!fromCommonParent){
		        		if (propDef.isUnstructured()){
		        			PropDef def2 = ((PropertyDefinitionImpl)p.getDefinition()).unwrap();
		        			if (def2.isUnstructured()){
		        				if (def2.isMultiple() != propDef.isMultiple() ){
		        					return false;
		        				}
		        				if (propDef.getRequiredType() != PropertyType.UNDEFINED && propDef.getRequiredType() != propType){
		        					return false;
		        				}
		        			} else {
		        				return false;
		        			}
		        		} else {
		        			return false;
		        		}
	        		}
	        	} catch (ConstraintViolationException exc){
	        		//do nothing
	        	}
	        }
	        NodeIterator ni = getNodes(false, null);
	        while(ni.hasNext()){
	        	NodeImpl n = (NodeImpl)ni.nextNode();
	        	try {
	        		PropDef propDef = entExisting.getApplicablePropertyDef(n.getQName(), PropertyType.UNDEFINED);
	        		if (!propDef.isUnstructured()){
	        			return false;
	        		}
	        	} catch (ConstraintViolationException exc){
	        	}
	        }
	        
	        
	        NodeDefinition[] childDefs = mixin.getChildNodeDefinitions();
	        for(NodeDefinition childDef:childDefs){
	        	if (childDef.isMandatory()){
		        	String n = childDef.getName();
		        	if (!n.equals("*")){
		        		if (hasProperty(n)){
		        			return false;
		        		}
		        		if (getNodes(n,false).hasNext()){
		        			return false;
		        		}
		        	}
	        	}
	        }
	        PropertyDefinition[] _childDefs = mixin.getPropertyDefinitions();
	        for(PropertyDefinition childDef:_childDefs){
	        	if (childDef.isMandatory()){
		        	String n = childDef.getName();
		        	if (!n.equals("*")){
		        		if (hasProperty(n)){
		        			if (!childDef.getDeclaringNodeType().getName().equals(getProperty(n).getDefinition().getDeclaringNodeType().getName())){
		        				return false;
		        			}
		        		}
		        		if (getNodes(n,false).hasNext()){
		        			if (!childDef.getDeclaringNodeType().getName().equals(getNode(n).getDefinition().getDeclaringNodeType().getName())){
		        				return false;
		        			}
		        		}
		        	}
	        	}
	        }
		} catch (NodeTypeConflictException e) {
			throw new RepositoryException("Error evaluating node type");
		}

        return true;
    }

    

    /**
     * {@inheritDoc}
     */
    public void checkout()
            throws UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versionable
        checkVersionable();

        // check checked-out status
        if (internalIsCheckedOut(true)) {
            String msg = safeGetJCRPath() + ": Node is already checked-out. ignoring.";
            log.debug(msg);
            return;
        }

        // check lock status
        checkLock();

            PropertyImpl prop = internalSetProperty(QName.JCR_ISCHECKEDOUT, InternalValue.create(true), false, true);
            prop.getItemState().setStatusModified();
            prop = internalSetProperty(QName.JCR_PREDECESSORS,
                    new InternalValue[]{
                        InternalValue.create(new UUID(getBaseVersion().getUUID()), false)
                    }, PropertyType.REFERENCE, false);
            prop.getItemState().setStatusModified();
    
            registerModification();
            
            stateManager.save(state.getNodeId(), true, true, true);
            
            //update references information
            _updateVersionReferences(getConnection());
            getConnection().commit();
            state.resetCheckedOut();
    }

    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCheckedOut() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return internalIsCheckedOut(true);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(String versionName, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {

        // checks
        sanityCheck();
        checkSessionHasPending();
        checkLock();

        GenericVersionSelector gvs = new GenericVersionSelector();
        gvs.setName(versionName);
        internalRestore(getVersionHistory().getVersion(versionName), gvs, removeExisting, false);
        // session.save/revert is done in internal restore
    }
    
    /**
     * Checks if this nodes session has pending changes.
     *
     * @throws InvalidItemStateException if this nodes session has pending changes
     * @throws RepositoryException
     */
    private void checkSessionHasPending()
            throws InvalidItemStateException, RepositoryException {
        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to perform operation. Session has pending changes.";
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }


    }    

    /**
     * {@inheritDoc}
     */
    public void restore(Version version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            RepositoryException {

        // do checks
        sanityCheck();
        checkSessionHasPending();
        checkVersionable();
        checkLock();

        // check if 'own' version
        if (!version.getContainingHistory().isSame(getVersionHistory())) {
            throw new VersionException("Unable to restore version. Not same version history.");
        }

        internalRestore(version, new GenericVersionSelector(version.getCreated()), removeExisting, false);
        // session.save/revert is done in internal restore
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version version, String relPath, boolean removeExisting)
            throws PathNotFoundException, ItemExistsException, VersionException,
            ConstraintViolationException, UnsupportedRepositoryOperationException,
            LockException, InvalidItemStateException, RepositoryException {

        // do checks
        sanityCheck();
        checkSessionHasPending();
        checkLock();

        // if node exists, do a 'normal' restore
        if (hasNode(relPath)) {
            getNode(relPath).restore(version, removeExisting);
        } else {
            NodeImpl node;
            try {
                // check if versionable node exists
                InternalFrozenNode fn = ((VersionImpl) version).getFrozenNode();
                node = (NodeImpl) session.getNodeByUUID(fn.getFrozenUUID());
                if (removeExisting) {
                    try {
                        Path dstPath = Path.create(getPrimaryPath(), relPath, session.getNamespaceResolver(), true);
                        // move to respective location
                        session.move(node.getPath(), dstPath.toJCRPath(session.getNamespaceResolver()));
                        // need to refetch ?
                        node = (NodeImpl) session.getNodeByUUID(fn.getFrozenUUID());
                    } catch (MalformedPathException e) {
                        throw new RepositoryException(e);
                    } catch (NoPrefixDeclaredException e) {
                        throw new RepositoryException("InternalError.", e);
                    }
                } else {
                    throw new ItemExistsException("Unable to restore version. Versionable node already exists.");
                }
            } catch (ItemNotFoundException e) {
                // not found, create new one
                node = addNode(relPath, ((VersionImpl) version).getFrozenNode());
            }

            // recreate node from frozen state
            node.internalRestore(version, new GenericVersionSelector(version.getCreated()), removeExisting, false);
            // session.save/revert is done in internal restore
        }
    }
    
    
    /**
     * Creates a new node at <code>relPath</code> of the node type, uuid and
     * eventual mixin types stored in the frozen node. The same as
     * <code>{@link #addNode(String relPath)}</code> except that the primary
     * node type type, the uuid and evt. mixin type of the new node is
     * explictly specified in the nt:frozen node.
     * <p/>
     *
     * @param relPath The path of the new <code>Node</code> that is to be created.
     * @param frozen  The frozen node that contains the creation information
     * @return The node that was added.
     * @throws ItemExistsException          If an item at the
     *                                      specified path already exists(and same-name siblings are not allowed).
     * @throws PathNotFoundException        If specified path implies intermediary
     *                                      <code>Node</code>s that do not exist.
     * @throws NoSuchNodeTypeException      If the specified <code>nodeTypeName</code>
     *                                      is not recognized.
     * @throws ConstraintViolationException If an attempt is made to add a node as the
     *                                      child of a <code>Property</code>
     * @throws RepositoryException          if another error occurs.
     */
    private NodeImpl addNode(String relPath, InternalFrozenNode frozen)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, NoSuchNodeTypeException,
            RepositoryException {

        // get frozen node type
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl nt = ntMgr.getNodeType(frozen.getFrozenPrimaryType());

        // get frozen uuid
        String uuid = frozen.getFrozenUUID();

        NodeImpl node = (NodeImpl) internalAddNode(relPath, nt, uuid, null);

        // get frozen mixin
        // todo: also respect mixing types on creation?
        QName[] mxNames = frozen.getFrozenMixinTypes();
        for (int i = 0; i < mxNames.length; i++) {
            node.addMixin(mxNames[i]);
        }
        return node;
    }    

    /**
     * {@inheritDoc}
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {

        // do checks
        sanityCheck();
        checkSessionHasPending();
        checkLock();

        Version v = getVersionHistory().getVersionByLabel(versionLabel);
        if (v == null) {
            throw new VersionException("No version for label " + versionLabel + " found.");
        }
        internalRestore(v, new GenericVersionSelector(versionLabel), removeExisting, false);
        // session.save/revert is done in internal restore
    }


    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        checkVersionable();
        VersionHistoryImpl h = (VersionHistoryImpl) getProperty(QName.JCR_VERSIONHISTORY).getNode();
        InternalVersionHistory ih = session.getVersionManager().getInternalVersionHistory(session, (NodeId) getId());
        h.setHistory(ih);
        return h;
    }






    
    public Lock lock(boolean isDeep, boolean isSessionScoped) 
		    throws UnsupportedRepositoryOperationException, LockException,
		    AccessDeniedException, InvalidItemStateException,
		    RepositoryException {
    	return lock(isDeep, isSessionScoped, new HashMap<String, Object>());

    }

  /*  public void touchLock(Calendar expires) throws LockException, RepositoryException{
    	checkLockable();
    	if (isLocked()){
    		 LockManager lockMgr = session.getLockManager();
    	        synchronized (lockMgr) {
    	            lockMgr.touchLock(this, expires);
    	        }
    	} else {
    		throw new LockException("Node not locked "+getPath());
    	}
    }
    
*/
    
    /**
     * {@inheritDoc}
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped, Map<String, Object> options)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
    	
    	
        // check state of this instance
        sanityCheck();

    	if (repository.isIgnoreLock()){
    		return null;
    	}

        
        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to lock node. Node has pending changes: " + safeGetJCRPath();
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }

        checkLockable();

        
        LockManager lockMgr = session.getLockManager();
        synchronized (lockMgr) {
            Lock lock = lockMgr.lock(this, isDeep, isSessionScoped, options);

            // add properties to content
            internalSetProperty(QName.JCR_LOCKOWNER,
                    InternalValue.create(getSession().getUserID()), false, true);
            internalSetProperty(QName.JCR_LOCKISDEEP,
                    InternalValue.create(isDeep), false, true);
            //save(true, false, false);
            if (isDeep){
            	processDeepLock(getNodeId(), session.getUserID());
            }
            return lock;
        }
    }
    
    /**
     * Checks if this node is lockable, i.e. has 'mix:lockable'.
     *
     * @throws LockException       if this node is not lockable
     * @throws RepositoryException if another error occurs
     */
    private void checkLockable() throws LockException, RepositoryException {
        if (!isNodeType(QName.MIX_LOCKABLE)) {
            String msg = "Unable to perform locking operation on non-lockable node: "
                    + safeGetJCRPath();
            log.debug(msg);
            throw new LockException(msg);
        }
    }    

    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (isNew()) {
            throw new LockException("Node not locked: " + safeGetJCRPath());
        }
        return session.getLockManager().getLock(this);
    }

    public void unlock()
		    throws UnsupportedRepositoryOperationException, LockException,
		    AccessDeniedException, InvalidItemStateException,
		    RepositoryException {
   		unlock(new HashMap<String, Object>());
    }
    
    /**
     * {@inheritDoc}
     */
    public void unlock(Map<String, Object> options)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
    	if (repository.isIgnoreLock()){
    		return;
    	}
    	if (isTransactionalNew()){
    		return;
    	}
    	
    	//JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
    	//try {
	        // check state of this instance
	        sanityCheck();
	
	        // check for pending changes
	        if (hasPendingChanges()) {
	            String msg = "Unable to unlock node. Node has pending changes: " + safeGetJCRPath();
	            log.debug(msg);
	            throw new InvalidItemStateException(msg);
	        }
	
	        checkLockable();
	
	        LockManager lockMgr = session.getLockManager();
	        synchronized (lockMgr) {
	        	//boolean isDeep = fa
	        	
	            boolean isDeep = lockMgr.getLock(this).isDeep();//getProperty(QName.JCR_LOCKISDEEP).getBoolean();
	            lockMgr.unlock(this, options);
	            // remove properties in content
	            internalSetProperty(QName.JCR_LOCKOWNER, (InternalValue) null, false, true);
	            internalSetProperty(QName.JCR_LOCKISDEEP, (InternalValue) null, false, true);
	            //save(true, false, false);
	            if (isDeep){
	            	processDeepLock(null, null);
	            }
	            
	        }
	    	//TransactionHelper.getInstance().commitAndResore(tr);
    	//} catch (RepositoryException e){
    		//session.getConnection().commit();
	    	//TransactionHelper.getInstance().rollbackAndResore(tr);
	    	//throw e;
    	//}
    }

    private void processDeepLock(Long value, String lockOwner) throws RepositoryException {
		stateManager.processDeepLock(getNodeId(), value, lockOwner);
		
	}

	/**
     * {@inheritDoc}
     */
    public boolean holdsLock() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (!isNodeType(QName.MIX_LOCKABLE) || isNew()) {
            // a node that is new or not lockable never holds a lock
            return false;
        }
        return session.getLockManager().holdsLock(this);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked() throws RepositoryException {
        // check state of this instance
    	if (repository.isIgnoreLock()){
    		return false;
    	}

        sanityCheck();

        if (isNew()) {
            return false;
        }
        return session.getLockManager().isLocked(this);
    }

    
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        sanityCheck();
        return (Node)_getParent();
    }

    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        QName ntName;
        try {
            ntName = session.getNamespaceResolver().getQName(nodeTypeName);
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid node type name: " + nodeTypeName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid node type name: " + nodeTypeName, upe);
        }
        return isNodeType(ntName);
    }


    //private HashMap<QName, PropertyImpl> props = new HashMap<QName, PropertyImpl>();
    
    public int hashCode() {
    	//FIXME to NodeId.hashCode()
        return state.getNodeId().hashCode();
    }

    private boolean hasChildNodes() throws RepositoryException {
        return stateManager.hasChildNodes(state);
    }
    
    /**
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    
    /**
     * Determines if there are pending unsaved changes either on <i>this</i>
     * node or on any node or property in the subtree below it.
     *
     * @return <code>true</code> if there are pending unsaved changes,
     *         <code>false</code> otherwise.
     * @throws RepositoryException if an error occured
     */
    protected boolean hasPendingChanges() throws RepositoryException {
        /*
        if (isTransient()) {
            return true;
        }
        Iterator iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
        return iter.hasNext();*/
        return stateManager.hasPendingChanges(state);
    }    
    

    //------------------------------< versioning support: public Node methods >
    /**
     * {@inheritDoc}
     */
    public Version checkin()throws VersionException, UnsupportedRepositoryOperationException,
    InvalidItemStateException, LockException, RepositoryException {
    	return checkin(false);
    }
    public Version checkin(boolean minorChange)
            throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versionable
        checkVersionable();

        // check if checked out
        if (!internalIsCheckedOut(true)) {
            String msg = safeGetJCRPath() + ": Node is already checked-in. ignoring.";
            log.debug(msg);
            return getBaseVersion();
        }

        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to checkin node. Node has pending changes: " + safeGetJCRPath();
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }

        // check lock status
        checkLock();

        Version v = ((VersionManagerImpl)session.getVersionManager()).checkin(this, minorChange);
        
        DatabaseConnection conn = getConnection();
        try {
            PropertyImpl prop = internalSetProperty(QName.JCR_ISCHECKEDOUT, InternalValue.create(false), false, true);
            prop.getItemState().setStatusModified();

            prop = internalSetProperty(QName.JCR_BASEVERSION, InternalValue.create(new UUID(v.getUUID()), false), false, true);
            prop.getItemState().setStatusModified();

            prop = internalSetProperty(QName.JCR_PREDECESSORS, InternalValue.EMPTY_ARRAY, PropertyType.REFERENCE, false);
            prop.getItemState().setStatusModified();
            
            registerModification();
            
            stateManager.save(state.getNodeId(), true, true, true);
            //update references information
            _updateVersionReferences(conn);
            conn.commit();
        
        } finally {
            conn.close();
            state.resetCheckedOut();
        }

        
        return v;
    }

    private void _updateVersionReferences(DatabaseConnection conn) throws RepositoryException {
            ArrayList<DatabaseStatement> statements = new ArrayList<DatabaseStatement>();
            for(int i = 0 ; i < state.getReferencesTo().size() ; i++){
                NodeReference nr = (NodeReference) state.getReferencesTo().get(i);
                if (nr.getPropertyQName().equals(QName.JCR_BASEVERSION) || nr.getPropertyQName().equals(QName.JCR_PREDECESSORS)) {
                    if (nr.getState().equals(ItemStatus.New)) {
                        DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE_REFERENCE);
                        insert.addValue(SQLParameter.create(FIELD_ID, nr.getId()));
                        insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__FROM, nr.getFromId()));
                        insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__TO, nr.getToId()));
                        insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__PROPERTY_NAME, nr.getPropertyName()));
                        insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE, nr.getPropertyNamespaceId()));
                        statements.add(insert);
                    } else if (nr.getState().equals(ItemStatus.Normal)){
                    } else if (nr.getState().equals(ItemStatus.Invalidated)){
                        if (nr.getId() != null) {
                            DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_REFERENCE, FIELD_ID, nr.getId());
                            statements.add(st);
                        }
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }
            }
            DatabaseTools.executeStatements(statements, conn);
            conn.commit();
//          TODO optimize read Ahead
            ArrayList<NodeReference> destroyedRefs = new ArrayList<NodeReference>();
                for(int i = 0 ; i < state.getReferencesTo().size() ; i++){
                    NodeReference nr = (NodeReference) state.getReferencesTo().get(i);
                    if (nr.getPropertyQName().equals(QName.JCR_BASEVERSION) || nr.getPropertyQName().equals(QName.JCR_PREDECESSORS)) {
                        if (nr.getState().equals(ItemStatus.New)){
                            Long nodeId = nr.getToId();
                            _NodeState n = stateManager.getNodeState(nodeId, null, true,nodeId.toString());
                            n.registerPermanentRefeference(nr);
                            nr.resetStateToNormal();
                        } else if (nr.getState().equals(ItemStatus.Invalidated)){
                            Long nodeId = nr.getToId();
                            try {
                                _NodeState n = stateManager.getNodeState(nodeId, null, true,nodeId.toString());
                                n.registerPermanentRefeferenceRemove(nr);
                            } catch (Exception exc){
                                //do nothing, node already deleted
                            }
                            nr.setDeleted();
                            destroyedRefs.add(nr);
                        }  else if (nr.getState().equals(ItemStatus.Destroyed)) {
                            //do nothing
                            destroyedRefs.add(nr);
                        } else if (nr.getState().equals(ItemStatus.Normal)) {
                            //do nothing
                        }else {
                            throw new RepositoryException("unknown state for NodeReference");
                        }
                    }
                }
            state.getReferencesTo().removeAll(destroyedRefs);
    }
    

  
    /**
     * Internal method to restore a version.
     *
     * @param version
     * @param vsel    the version selector that will select the correct version for
     *                OPV=Version childnodes.
     * @throws UnsupportedRepositoryOperationException
     *
     * @throws RepositoryException
     */
    private void internalRestore(Version version, VersionSelector vsel, boolean removeExisting, boolean allowRoot)
            throws UnsupportedRepositoryOperationException, RepositoryException {

        try {
            internalRestore(((VersionImpl) version).getInternalVersion(), vsel, removeExisting, allowRoot);
        } catch (RepositoryException e) {
            // revert session
            try {
                log.error("reverting changes applied during restore...");
                session.refresh(false);
            } catch (RepositoryException e1) {
                // ignore this
            }
            throw e;
        }
        
        
        ((NodeImpl)session.getRootNode()).save(true, false, true);
    }
    
    /**
     * Internal method to restore a version.
     *
     * @param version
     * @param vsel           the version selector that will select the correct version for
     *                       OPV=Version childnodes.
     * @param removeExisting
     * @throws RepositoryException
     */
    protected InternalVersion[] internalRestore(InternalVersion version, VersionSelector vsel,
                                                boolean removeExisting, boolean allowRoot)
            throws RepositoryException {

        // fail if root version
        if (version.isRootVersion() && ! allowRoot) {
            throw new VersionException("Restore of root version not allowed.");
        }

        // set jcr:isCheckedOut property to true, in order to avoid any conflicts
        internalSetProperty(QName.JCR_ISCHECKEDOUT, InternalValue.create(true), true, true);

        // 1. The child node and properties of N will be changed, removed or
        //    added to, depending on their corresponding copies in V and their
        //    own OnParentVersion attributes (see 7.2.8, below, for details).
        HashSet<InternalVersion> restored = new HashSet<InternalVersion>();
        restoreFrozenState(version.getFrozenNode(), vsel, restored, removeExisting);
        restored.add(version);

        // 2. N's jcr:baseVersion property will be changed to point to V.
        internalSetProperty(QName.JCR_BASEVERSION, InternalValue.create(new UUID(version.getUUID()), false), true, true);

        // 4. N's jcr:predecessor property is set to null
        internalSetProperty(QName.JCR_PREDECESSORS, InternalValue.EMPTY_ARRAY, PropertyType.REFERENCE, true);

        // also clear mergeFailed
        //internalSetProperty(QName.JCR_MERGEFAILED, (InternalValue[]) null, PropertyType.REFERENCE, true);
        if (hasProperty(QName.JCR_MERGEFAILED)){
            getProperty(QName.JCR_MERGEFAILED).remove();
        }

        // 3. N's jcr:isCheckedOut property is set to false.
        internalSetProperty(QName.JCR_ISCHECKEDOUT, InternalValue.create(false), true, true);

        return (InternalVersion[]) restored.toArray(new InternalVersion[restored.size()]);
    }

    public void onRedefine(Long id) {
        //TODO ???
        //throw new UnsupportedOperationException();
        
    }

    public void refreshState(boolean b) throws RepositoryException {
        //reloadNode(null, true);
    	throw new UnsupportedOperationException();
        
    }

    public void setInvalidates() {
        //_status = STATUS_INVALIDATED;
    	throw new UnsupportedOperationException();
        
    }

    /**
     * Restores the properties and child nodes from the frozen state.
     *
     * @param freeze
     * @param vsel
     * @param removeExisting
     * @throws RepositoryException
     */
    void restoreFrozenState(InternalFrozenNode freeze, VersionSelector vsel, Set restored, boolean removeExisting)
            throws RepositoryException {

        // check uuid
        if (isNodeType(QName.MIX_REFERENCEABLE)) {
            String uuid = freeze.getFrozenUUID();
            if (uuid != null && !uuid.equals(getUUID())) {
                throw new ItemExistsException("Unable to restore version of " + state.safeGetJCRPath() + ". UUID changed.");
            }
        }

        // check primary type
        if (!freeze.getFrozenPrimaryType().equals(state.getPrimaryTypeName())) {
            // todo: check with spec what should happen here
            throw new ItemExistsException("Unable to restore version of " + state.safeGetJCRPath() + ". PrimaryType changed.");
        }

        // adjust mixins
        QName[] mixinNames = freeze.getFrozenMixinTypes();
        setMixinTypesProperty(new HashSet<QName>(Arrays.asList(mixinNames)));

        // copy frozen properties
        _PropertyState[] props = freeze.getFrozenProperties();
        HashSet<QName> propNames = new HashSet<QName>();
        for (int i = 0; i < props.length; i++) {
            _PropertyState prop = props[i];
            propNames.add(prop.getName());
            if (prop.getDefinition().isMultiple()) {
                internalSetProperty(props[i].getName(), prop.getValues(), props[i].getType(), true);
            } else {
                internalSetProperty(props[i].getName(), prop.getValues()[0], true, true);
            }
        }
        // remove properties that do not exist in the frozen representation
        PropertyIterator piter = getProperties();
        while (piter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) piter.nextProperty();
            // ignore some props that are not well guarded by the OPV
            if (prop.getQName().equals(QName.JCR_VERSIONHISTORY)) {
                continue;
            } else if (prop.getQName().equals(QName.JCR_PREDECESSORS)) {
                continue;
            }
            if (prop.getDefinition().getOnParentVersion() == OnParentVersionAction.COPY
                    || prop.getDefinition().getOnParentVersion() == OnParentVersionAction.VERSION) {
                if (!propNames.contains(prop.getQName())) {
                    removeChildProperty(prop.getQName(), true);
                }
            }
        }

        // add 'auto-create' properties that do not exist yet
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        for (int j = 0; j < mixinNames.length; j++) {
            NodeTypeImpl mixin = ntMgr.getNodeType(mixinNames[j]);
            PropertyDefinition[] pda = mixin.getAutoCreatedPropertyDefinitions();
            for (int i = 0; i < pda.length; i++) {
                PropertyDefinitionImpl pd = (PropertyDefinitionImpl) pda[i];
                if (!hasProperty(pd.getQName())) {
                    createChildProperty(pd.getQName(), pd.getRequiredType(), pd, null);
                }
            }
        }

        // first delete all non frozen version histories
        NodeIterator iter = getNodes();
        while (iter.hasNext()) {
            NodeImpl n = (NodeImpl) iter.nextNode();
            if (!n._isNodeType(QName.MIX_REFERENCEABLE) || !freeze.hasFrozenHistory(n.getUUID())) {
                n.internalRemove(true, false);
            }
        }

        // restore the frozen nodes
        InternalFreeze[] frozenNodes = freeze.getFrozenChildNodes();
        for (int i = 0; i < frozenNodes.length; i++) {
            InternalFreeze child = frozenNodes[i];
            if (child instanceof InternalFrozenNode) {
                InternalFrozenNode f = (InternalFrozenNode) child;
                // check for existing
                if (f.getFrozenUUID() != null) {
                    try {
                        NodeImpl existing = (NodeImpl) session.getNodeByUUID(f.getFrozenUUID());
                        // check if one of this restoretrees node
                        if (removeExisting) {
                        	ItemStatus status = existing.getNodeState().getStatus();
                        	if (!status.equals(ItemStatus.Invalidated) && !status.equals(ItemStatus.Destroyed)){
                        		existing.remove();
                        	}
                        } else {
                            // since we delete the OPV=Copy children beforehand, all
                            // found nodes must be outside of this tree
                            throw new ItemExistsException("Unable to restore node, item already exists outside of restored tree: "
                                    + existing.safeGetJCRPath());
                        }
                    } catch (ItemNotFoundException e) {
                        // ignore, item with uuid does not exist
                    }
                }
                NodeImpl n = addNode(f.getName(), f);
                n.restoreFrozenState(f, vsel, restored, removeExisting);

            } else if (child instanceof InternalFrozenVersionHistory) {
                InternalFrozenVersionHistory f = (InternalFrozenVersionHistory) child;
                VersionHistoryImpl history = (VersionHistoryImpl) session.getNodeByUUID(f.getVersionHistoryId());
                String nodeUUID = history.getVersionableUUID();

                // check if representing versionable already exists somewhere
                if (stateManager.itemExists(nodeUUID)) {
                    _NodeImpl n = (_NodeImpl) session.getNodeByUUID(nodeUUID);
                    if (n.getParent().isSame(this)) {
                        // so order at end
                        // orderBefore(n.getName(), "");
                    } else if (removeExisting) {
                        session.move(n.getPath(), getPath() + "/" + n.getName());
                    } else {
                        // since we delete the OPV=Copy children beforehand, all
                        // found nodes must be outside of this tree
                        throw new ItemExistsException("Unable to restore node, item already exists outside of restored tree: "
                                + n.safeGetJCRPath());
                    }
                } else {
                    // get desired version from version selector
                    InternalVersion v = ((VersionImpl) vsel.select(history)).getInternalVersion();
                    NodeImpl node = addNode(child.getName(), v.getFrozenNode());
                    node.internalRestore(v, vsel, removeExisting, true);
                    // add this version to set
                    restored.add(v);
                }
            }
        }
    }
    
    /**
     * Creates a new node at <code>relPath</code> of the node type, uuid and
     * eventual mixin types stored in the frozen node. The same as
     * <code>{@link #addNode(String relPath)}</code> except that the primary
     * node type type, the uuid and evt. mixin type of the new node is
     * explictly specified in the nt:frozen node.
     * <p/>
     *
     * @param name   The name of the new <code>Node</code> that is to be created.
     * @param frozen The frozen node that contains the creation information
     * @return The node that was added.
     * @throws ItemExistsException          If an item at the
     *                                      specified path already exists(and same-name siblings are not allowed).
     * @throws PathNotFoundException        If specified path implies intermediary
     *                                      <code>Node</code>s that do not exist.
     * @throws NoSuchNodeTypeException      If the specified <code>nodeTypeName</code>
     *                                      is not recognized.
     * @throws ConstraintViolationException If an attempt is made to add a node as the
     *                                      child of a <code>Property</code>
     * @throws RepositoryException          if another error occurs.
     */
    private NodeImpl addNode(QName name, InternalFrozenNode frozen)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, NoSuchNodeTypeException,
            RepositoryException {

        // get frozen node type
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl nt = ntMgr.getNodeType(frozen.getFrozenPrimaryType());

        // get frozen uuid
        String uuid = frozen.getFrozenUUID();

        NodeImpl node = (NodeImpl) internalAddChildNode(name, nt, uuid);

        // get frozen mixin
        // todo: also respect mixing types on creation?
        QName[] mxNames = frozen.getFrozenMixinTypes();
        for (int i = 0; i < mxNames.length; i++) {
            node.addMixin(mxNames[i]);
        }
        if (uuid != null){
        	//check if node have uuid property
        	boolean allowUUID = true;
        	try{
	        	node.getNodeState().getEffectiveNodeType().getApplicablePropertyDef(
	        			QName.JCR_UUID, PropertyType.STRING, false);
        	} catch (Exception exc){
        		allowUUID = false;
        	}
        	if (allowUUID){
            	InternalValue v = InternalValue.create(uuid);
        		node.internalSetProperty(QName.JCR_UUID, v, true, false);
        	}
        }
        return node;
    }

    public String toString(){
        ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("id", state.getNodeId());
        builder.append("name", state.getName());
        builder.append("path", state.getInternalPath());
        builder.append("version", state.getVersion());
        try {
            builder.append("type", state.getPrimaryTypeName().toString());
        } catch (RepositoryException e) {
        }
        return builder.toString();
    }

    @Deprecated
    public Long getSecurityId() {
    	return state.getSecurityId();
    }

    public void setSecurityId(Long securityId) throws AccessDeniedException {
    	state.setSecurityId(securityId);
        registerModification();
        state.setBasePropertiesChanged(true);
    }

    


    public String getNodeTypeName() throws RepositoryException {
        return getPrimaryNodeType().getName();
    }

    public NodeState getParentState() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return (NodeState) _getParent();
    }



    public void renameChildNode(NodeImpl targetNode, QName destName, NodeModification nm) throws RepositoryException {
        QName srcName = targetNode.getQName();
        int destIndex = 1;
        //1. check is dest exist
        _NodeImpl tn = getNode(destName, 1 , false);
        if (tn != null){
            if (!tn.getDefinition().allowsSameNameSiblings()){
                throw new ItemExistsException(tn.safeGetJCRPath());
            }
            //1.a if exist , check SNS, and set index
            try {
                NodeIterator ni = getNodes(destName.toJCRName(_getNamespaceRegistry()), false);
                while (ni.hasNext()){
                    ni.nextNode();
                    destIndex++;
                }
            } catch (NoPrefixDeclaredException e) {
            } catch (RepositoryException e) {
            }
        }
        //count src nodes count
        long srcCount = 0;
        try {
            NodeIterator ni = getNodes(srcName.toJCRName(_getNamespaceRegistry()), false);
            while (ni.hasNext()){
                ni.next();
                srcCount++;
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        } 
        
        //2.rename node
        targetNode.getNodeState().updateName(destName, new Long(destIndex), this.getNodeState(), nm, true);
        targetNode.getNodeState().setSnsMax((long)destIndex);
        //4. TODO update security
        
        //5. if srx SNS, change SNS and child paths
        if (srcCount > 1){
            rebuildChildIndex(srcName, nm, srcCount -1);
        }
    	//throw new UnsupportedOperationException();
 
    }

    void rebuildChildIndex(QName srcName, NodeModification nm, long snsMax)  throws RepositoryException {
        int pos = 1;
		try {
			for (NodeIterator ni = getNodes(srcName.toJCRName(_getNamespaceRegistry()), false); ni.hasNext();) {
				NodeImpl node = (NodeImpl) ni.next();
				node.getNodeState().setSnsMax(snsMax);
				node.getNodeState().updateName(null, new Long(pos++),this.getNodeState(), nm, true);
			}
		} catch (NoPrefixDeclaredException e) {
		} catch (RepositoryException e) {
		}
    	// throw new UnsupportedOperationException();
        
    }

	protected _NodeImpl instantiate(_NodeState state2) throws RepositoryException {
		return _getSession().getNodeManager().buildNode(state2, false, true);
	}


	@Override
	protected _NodeImpl buildNode(_NodeState nodeState) throws RepositoryException {
		return new NodeImpl(nodeState, stateManager);
	}


    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }
        return setProperty(name, value, type);
    }

    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status);
        try {
            if (type == PropertyType.UNDEFINED) {
                prop.setValue(value);
            } else {
                prop.setValue(ValueHelper.convert(value, type));
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                try {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                } catch (Exception e) {
                    //TODO may be exception ??
                    throw new RepositoryException("Error removing property");
                } 
            }
            // rethrow
            throw re;
        }
        return prop;
    }

	protected boolean checkProtection(){
		return true;
	}

    public PropertyIterator getProperties() throws RepositoryException {
        return new PropertyIteratorImpl(session, state.getProperties(), this);
    }


	public void reloadNode() throws RepositoryException {
		stateManager.reloadState(state);
		
	}

	public Long getParentNodeId() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
		return this.state.getParentId();
	}

	
	protected void sanityCheck() throws RepositoryException {
	    _getSession().sanityCheck();
	    super.sanityCheck();
	}


}


/*
 * $Log: NodeImpl.java,v $
 * Revision 1.21  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.20  2009/01/27 14:07:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.19  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.18  2009/01/19 08:36:56  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.17  2008/12/13 09:26:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.16  2008/09/19 10:14:17  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.15  2008/07/22 09:28:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.14  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.13  2008/06/13 09:35:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.12  2008/06/09 12:36:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2008/05/19 07:18:14  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/04/29 10:55:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/03/28 13:45:57  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.8  2008/01/03 11:56:41  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2007/11/30 07:47:48  dparhomenko
 * Fix lock problem
 *
 * Revision 1.6  2007/11/13 13:40:40  dparhomenko
 * ignoreLock functionality
 *
 * Revision 1.5  2007/10/26 11:00:51  dparhomenko
 * Fix Lock problem in management environment
 *
 * Revision 1.4  2007/08/29 12:55:30  dparhomenko
 * fix drop procedure for version without fts
 *
 * Revision 1.3  2007/08/08 07:45:31  dparhomenko
 * PTR#1805084 fix ptr
 *
 * Revision 1.2  2007/06/15 13:19:23  dparhomenko
 * PTR#0152003 fix insert statemnt for oracle blobs
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.54  2007/03/22 12:10:05  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.53  2007/03/12 08:24:02  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.52  2007/03/02 14:46:11  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.51  2007/02/02 15:38:54  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.50  2007/01/24 08:46:25  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.49  2006/11/30 11:42:39  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.48  2006/11/30 10:59:48  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.47  2006/11/14 07:37:19  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.46  2006/11/03 13:09:13  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.45  2006/11/01 14:11:28  dparhomenko
 * PTR#1803326 new features
 *
 * Revision 1.44  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.43  2006/10/09 12:00:14  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.42  2006/10/09 11:22:50  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.41  2006/10/03 09:16:08  dparhomenko
 * PTR#0148428 Fix NPE
 *
 * Revision 1.40  2006/10/02 15:07:09  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.39  2006/09/26 12:31:49  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.38  2006/09/26 10:11:07  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.37  2006/09/25 08:33:00  dparhomenko
 * PTR#1802460
 *
 * Revision 1.36  2006/09/21 10:44:31  dparhomenko
 * PTR#1802460
 *
 * Revision 1.35  2006/09/20 13:29:51  dparhomenko
 * PTR#1802460
 *
 * Revision 1.34  2006/09/19 08:54:34  dparhomenko
 * PTR#1802460
 *
 * Revision 1.33  2006/09/11 12:59:27  dparhomenko
 * PTR#0148066
 *
 * Revision 1.32  2006/09/11 11:42:12  dparhomenko
 * PTR#0148066 fix restoring subnodes
 *
 * Revision 1.31  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.30  2006/08/17 11:30:12  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.29  2006/08/16 10:08:59  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.28  2006/08/15 08:23:19  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.27  2006/08/14 10:24:07  dparhomenko
 * PTR#1802276 implement autocreated
 *
 * Revision 1.26  2006/08/14 10:16:14  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.25  2006/08/11 11:24:24  dparhomenko
 * PTR#1802633 fix problem with delete node
 *
 * Revision 1.24  2006/08/11 09:27:27  dparhomenko
 * PTR#1802633 fix problem with delete node
 *
 * Revision 1.23  2006/08/10 13:41:07  dparhomenko
 * PTR#0147584 fix removing mandatory child
 *
 * Revision 1.22  2006/08/10 13:10:06  dparhomenko
 * PTR#0147668 fix add mixin
 *
 * Revision 1.21  2006/08/10 10:26:06  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.20  2006/08/07 14:25:55  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.19  2006/07/11 10:25:58  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.18  2006/07/07 07:56:07  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.17  2006/07/06 09:29:15  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.16  2006/06/30 08:26:34  dparhomenko
 * PTR#0147122 fix versioning
 *
 * Revision 1.15  2006/06/27 14:40:53  dparhomenko
 * PTR#0147122 fix versioning
 *
 * Revision 1.14  2006/06/27 11:51:05  dparhomenko
 * PTR#1802276 implement exigen autocreated properties
 *
 * Revision 1.13  2006/06/22 12:00:25  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.12  2006/06/15 13:18:02  dparhomenko
 * PTR#0146580 fix sns remove on root node
 *
 * Revision 1.11  2006/06/09 08:55:40  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.10  2006/06/02 09:32:14  dparhomenko
 * PTR#0146580 fix sns remove on root node
 *
 * Revision 1.9  2006/06/02 08:20:25  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.8  2006/06/02 07:21:28  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.7  2006/05/22 14:48:02  dparhomenko
 * PTR#1801941 add observationsupport
 *
 * Revision 1.6  2006/04/28 14:59:42  dparhomenko
 * PTR#0144983 fix remove nodes with references
 *
 * Revision 1.5  2006/04/24 08:55:17  dparhomenko
 * PTR#0144983 fts
 *
 * Revision 1.4  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.3  2006/04/19 13:13:34  dparhomenko
 * PTR#0144983 tests
 *
 * Revision 1.2  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 */