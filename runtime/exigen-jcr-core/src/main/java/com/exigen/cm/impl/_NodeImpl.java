/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.Order;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.iterators.PropertyIteratorImpl;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.state2.ChildMaxPosition;
import com.exigen.cm.impl.state2.IdIterator;
import com.exigen.cm.impl.state2.IndexedQname;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2.NodeStateIterator;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._ItemState;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.ValueHelper;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class _NodeImpl extends ItemImpl {

	@Override
	protected SessionImpl _getSession() {
		// TODO Auto-generated method stub
		return null;
	} 

	@Override
	public void accept(ItemVisitor visitor) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof _NodeImpl) {
        	_NodeImpl other = (_NodeImpl) obj;
            return getNodeId().equals(other.getNodeId()) && stateManager.equals(other.getStateManager());
        }
        return false;	
    }

	public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

	protected _NodeState state;


    protected static final short CREATED = 0;
	
    protected Boolean allowSetProperty = null;
    protected Boolean allowAddNode = null;
    Boolean allowRemoveChild;
    Boolean allowRemoveAllChild;

	
	/** Log for this class */
	private static final Log log = LogFactory.getLog(_NodeImpl.class);

	public _NodeImpl(_NodeState state, _AbstractsStateManager sm) {
		super(state, sm);
		this.state = state;
		// TODO Auto-generated constructor stub
	}

	protected _NodeImpl createChildNode(QName name, NodeDefinitionImpl def,
			NodeTypeImpl nodeType, String uuid, boolean createAutoCreatedChilds, Long newNodeId) throws RepositoryException {
		return createChildNode(name, def, nodeType, uuid, true, true, createAutoCreatedChilds, newNodeId);
	}

	
	protected _NodeImpl createChildNode(QName name, NodeDefinitionImpl def,
			NodeTypeImpl nodeType, String uuid, boolean checkCollision, boolean checkLocks, boolean createAutoCreatedChilds, Long _newNodeId) throws RepositoryException {
		//Long _index = new Long(1);
		Long nextId;
		if (_newNodeId != null){
			nextId = _newNodeId;
		} else {
			nextId = stateManager.nextId();
		}

		// try to evaluate node index ... (only for SNS)

		ChildMaxPosition max = stateManager.getMaxChildPos(state, name);
		//if (def.allowsSameNameSiblings()) {
			//Long total = stateManager.countTotal(state.getNodeId(), name);
			//if (total.longValue() > 0) {
			//	_index = new Long(total.longValue() + 1);
			//}
			
		//}
		//_index = max.getMax()+1;

		if (name.getLocalName().length() > 254) {
			throw new RepositoryException("Node name is too long");
		}
		NodeId newNodeId = new NodeId(nextId, uuid);
		_getRepository().registerNodeId(newNodeId);
		long __index = max.getMax()+1;
		if (__index < 1){
			__index = 1;
		}
		_NodeImpl node = createInstance(newNodeId, name, state.getNodeId(),
				state.getSecurityId(), __index, def, nodeType, this, __index);
		
		// TODO update parent tree for child

		// TODO register node in session node manager and in local nodes, set
		// this state to changed

		// TODO add 'auto-create' properties defined in node type
		if (createAutoCreatedChilds){
    		PropertyDefinitionImpl[] pda = nodeType
    				.getAutoCreatedPropertyDefinitions();
    		for (int i = 0; i < pda.length; i++) {
    			PropertyDefinitionImpl pd = (PropertyDefinitionImpl) pda[i];
    			if ((!pd.getQName().equals(QName.JCR_UUID) || uuid == null) && 
    					!pd.getQName().equals(QName.JCR_PRIMARYTYPE)) {
    				node.createChildProperty(pd.getQName(), pd.getRequiredType(), pd, null, false, false);//, checkCollision, checkLocks
    			} else {
    				if (pd.getQName().equals(QName.JCR_UUID)){
    					
    					_PropertyState result = new _PropertyState(_getRepository(), node.state,
    							pd.getQName(), pd.getRequiredType(), pd.getRequiredType(), pd.isMultiple(),
    							(PropDefImpl) pd.unwrap(), null);
    					node.state.addProperty(result);
    					if (uuid == null){
    						uuid = _getRepository().generateUUID().toString();
    					} 
    					InternalValue uuidValue = InternalValue.create(uuid);
    					result.setValues(new InternalValue[]{uuidValue});
    					
    				/*
    				 * PropertyState propState = new PropertyState(pd.getQName(),
    				 * pd.getRequiredType(), pd.unwrap(), node.getNodeId(), null);
    				 * propState.setType(pd.getRequiredType());
    				 * propState.setMultiValued(pd.isMultiple()); InternalValue[]
    				 * genValues = new InternalValue[] { InternalValue.create(uuid) };
    				 * node.getInternalUUID() = genValues[0].toString();
    				 * propState.setValues(genValues);
    				 * node.createChildProperty(pd.getQName(), pd.getRequiredType(),
    				 * pd, null, propState);
    				 */
    				//throw new UnsupportedOperationException();
    				} else {
    				}
    
    			}
    		}
    
    		// TODO recursively add 'auto-create' child nodes defined in node type
    		NodeDefinition[] nda = nodeType.getAutoCreatedNodeDefinitions();
    		for (int i = 0; i < nda.length; i++) {
    			NodeDefinitionImpl nd = (NodeDefinitionImpl) nda[i];
    			node.createChildNode(nd.getQName(), nd, (NodeTypeImpl) nd
    					.getDefaultPrimaryType(), null, false, false, createAutoCreatedChilds, null);
    		}
		}

		stateManager.registerNewState(node.state);
		stateManager.registerModifiedState(state);
		/*while (max.getMax() < 0){
			max.inc(node.state);
		}*/
		max.inc(node.state);

		return node;
	}

	protected synchronized _PropertyState createChildProperty(QName name,
			int type, PropertyDefinitionImpl def, Long unstructuredPropertyId)
			throws RepositoryException {
		return createChildProperty(name, type, def, unstructuredPropertyId, true, true);
	}
	protected synchronized _PropertyState createChildProperty(QName name,
			int type, PropertyDefinitionImpl def, Long unstructuredPropertyId, boolean checkCollision, boolean checkLocks)
			throws RepositoryException {
		// String parentUUID = ((NodeState) state).getUUID();

		/*
		 * } catch (ItemStateException ise) { String msg = "failed to add
		 * property " + name + " to " + safeGetJCRPath(); log.debug(msg); throw
		 * new RepositoryException(msg, ise); }
		 */
		
		if (checkCollision && stateManager.hasChildNode(this.state, name, false)) {
			String msg = "there's already a child node with name " + name;
			log.debug(msg);
			throw new RepositoryException(msg);
		}

		
		_PropertyState result = new _PropertyState(_getRepository(), state,
				name, type, def.getRequiredType(), def.isMultiple(),
				(PropDefImpl) def.unwrap(), unstructuredPropertyId);
		state.addProperty(result);
		
		InternalValue[] genValues = state.computeSystemGeneratedPropertyValues(name,
				def);
		InternalValue[] defValues = def.unwrap().getDefaultValues();
		if (genValues != null && genValues.length > 0) {
			result.setValues(genValues);
		} else if (defValues != null && defValues.length > 0) {
			result.setValues(defValues);
		} else if (def.isAutoCreated()) {
			if (!def.isMultiple() && def.getRequiredType() == PropertyType.STRING){
				result.setValues(new InternalValue[]{InternalValue.create("")});
			} else if (!def.isMultiple() && def.getRequiredType() == PropertyType.LONG){
				result.setValues(new InternalValue[]{InternalValue.create(new Long(0))});
			} else if (!def.isMultiple() && def.getRequiredType() == PropertyType.DATE){
				result.setValues(new InternalValue[]{InternalValue.create(Calendar.getInstance())});
			} else if (!def.isMultiple() && def.getRequiredType() == PropertyType.DOUBLE){
				result.setValues(new InternalValue[]{InternalValue.create(new Double(0))});
			} else if (checkProtection()){
				throw new RepositoryException(
						"Default values for autocreated property " + name
								+ " not defined");
			}
			
		}

		
		return result;
	}

	final protected _NodeImpl createInstance(NodeId newNodeId, QName name,
			Long nodeId, Long securityId, Long index, NodeDefinitionImpl def,
			NodeTypeImpl nodeType, _NodeImpl parent, Long snsMax) throws RepositoryException{
		//

		_NodeState parentState = parent.getNodeState();

		
		_NodeState state = new _NodeState(newNodeId.getId(), this.repository );
		
		if (isNew()){
			this.getNodeState().registerChild(new IndexedQname(name, index.intValue()), state.getNodeId());
		}
		
        _NodeImpl result = instantiate(state);
        
        state.setName(name);
        state.setParentId(parent.getNodeId()); 
        state.setWorkspaceId(parent.getNodeState().getWorkspaceId());
        state.setStoreConfigurationId(parent.getNodeState().getStoreConfigurationId());
        state.setSecurityId(securityId);
        state.setParentLockId(null);
        state.setLockOwner(null);
        state.setIndex(index);
        state.setSnsMax(snsMax);
        state.addAllAces(parent.getNodeState().getACEs());

        state.setDefinition(def);
        state.setNodeTypeId(nodeType.getSQLId());
        
        state.setInternalDepth(new Long(parent.getNodeState().getInternalDepth() + 1));

        //create type objects
        EffectiveNodeType ent = state.getEffectiveNodeType();
        QName[] allTypes = ent.getAllNodeTypes();
        for(int i = 0 ; i < allTypes.length ; i++){
            result.registerNewNodeType(allTypes[i], nodeType.getQName());
        }
        
        //update parent information
        for(ParentNode ps:parent.getNodeState().getParentNodes()){
            state.addParentNode(new ParentNode(state.getNodeId(), ps.getParentId(), ps.getPosition().longValue() + 1));
        }
        state.addParentNode(new ParentNode(state.getNodeId(), parent.getNodeId(), 1));
        
		state.assignSession(stateManager);
        state.buildInternalPath(parentState.getInternalPath(), parentState.getInternalDepthLong());
        
        
        state.createDefaultProperties(true);

        
	    result.allowSetProperty = Boolean.TRUE;
	    result.allowAddNode = Boolean.TRUE;
	    stateManager.registerNewState(state);
	    
	    
	    
	    return result;
		
		
	}
	
	
    public void registerNewNodeType(QName nodeTypeName, QName fromNodeType) throws RepositoryException {
        //Adding new type to the node
        NodeTypeImpl nt = getNodeTypeManager().getNodeType(nodeTypeName);
        NodeTypeImpl from = getNodeTypeManager().getNodeType(fromNodeType);
        state.registerNodeType(new NodeTypeContainer(state.getNodeId(), nt, from));
    }
	

	protected abstract _NodeImpl instantiate(_NodeState state2) throws RepositoryException ;	
	
	
	public Long getNodeId() {
		return state.getNodeId();
	}

	public _NodeImpl _getParent() throws ItemNotFoundException,
			AccessDeniedException, RepositoryException {
		if (state.getParentId() != null) {
			if (state.getStatus() == ItemStatus.Destroyed) {
				return _getSession().getNodeManager().buildNode(
						state.getParent(), true, true);
			} else {
				return instantiate(state.getParent());
			}
		} else {
			throw new ItemNotFoundException();
		}
	}

	public int getIndex() throws RepositoryException {
		return state.getIndex();
	}

	public QName getQName() {
		return state.getName();
	}

	public boolean isNode() {
		return true;
	}

	public NodeId getParentId() throws ItemNotFoundException,
			AccessDeniedException, RepositoryException {
		return this.repository.buildNodeId(state.getParentId(), getConnection());
	}

	public boolean hasNodes() throws RepositoryException {
		sanityCheck();
		
		return stateManager.hasChildNodes(state);
	}

	protected _NodeImpl internalAddNode(String relPath, NodeTypeImpl nodeType, Long newNodeId)
			throws ItemExistsException, PathNotFoundException,
			VersionException, ConstraintViolationException, LockException,
			RepositoryException {
		return internalAddNode(relPath, nodeType, null, newNodeId);
	}
	
    protected _NodeImpl internalAddNode(String relPath, NodeTypeImpl nodeType,
            String uuid, Long newNodeId) throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException{
        //check permissions
        canAddNode();
        
        Path nodePath;
        QName nodeName;
        Path parentPath;
        try {
            nodePath = Path.create(getPrimaryPath(), relPath, _getNamespaceResolver(), true);
            if (nodePath.getNameElement().getIndex() != 0) {
                String msg = "illegal subscript specified: " + nodePath;
                LogUtils.debug(log, msg);
                throw new RepositoryException(msg);
            }
            nodeName = nodePath.getNameElement().getName();
            parentPath = nodePath.getAncestor(1);
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to "
                    + safeGetJCRPath();
            LogUtils.debug(log, msg);
            throw new RepositoryException(msg, e);
        }

        _NodeImpl parentNode;
        //try {
            _ItemState parent = null;
            if (parentPath.equals(getPrimaryPath())){
            	parent = state;
            	parentNode = this;
            } else {
            	parent = stateManager.getItem(parentPath, true);
            	if (!parent.isNode()) {
                    String msg = "cannot add a node to property " + parentPath;
                    log.debug(msg);
                    throw new ConstraintViolationException(msg);
                }
                parentNode = instantiate((_NodeState)parent);
            }
            
        //} catch (AccessDeniedException ade) {
        //    ade.printStackTrace();
        //    throw new PathNotFoundException(relPath);
        //}

        // make sure that parent node is checked-out
        if (!parentNode.internalIsCheckedOut(false)) {
            String msg = safeGetJCRPath()
                    + ": cannot add a child to a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check lock status
        parentNode.checkLock();

        // delegate the creation of the child node to the parent node
        return parentNode.internalAddChildNode(nodeName, nodeType, uuid, newNodeId);
    }
    protected _NodeImpl internalAddChildNode(QName nodeName, NodeTypeImpl nodeType, String uuid, Long newNodeId) throws RepositoryException{
    	return internalAddChildNode(nodeName, nodeType, uuid, true, true, true, newNodeId);
    }
	
    protected _NodeImpl internalAddChildNode(QName nodeName, NodeTypeImpl nodeType, String uuid) throws RepositoryException{
    	return internalAddChildNode(nodeName, nodeType, uuid, true, true, true, null);
    }
	
    protected _NodeImpl internalAddChildNode(QName nodeName, NodeTypeImpl nodeType, String uuid, 
            boolean checkCollision, boolean checkLocks, 
            boolean createAutoCreatedChilds, Long newNodeId) throws RepositoryException{
        Path nodePath;
        try {
            nodePath = Path.create(getPrimaryPath(), nodeName, true);
        } catch (MalformedPathException e) {
            // should never happen
            String msg = "internal error: invalid path " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        NodeDefinitionImpl def;
        try {
            QName nodeTypeName = null;
            if (nodeType != null) {
                nodeTypeName = nodeType.getQName();
            }
            
            def = state.getApplicableChildNodeDefinition(nodeName, nodeTypeName);
        } catch (RepositoryException re) {
            String msg = "no definition found in parent node's node type for new node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        if (nodeType == null) {
            // use default node type
            nodeType = (NodeTypeImpl) def.getDefaultPrimaryType();
        }

        // check for name collisions
        if (checkCollision && state.hasProperty(nodeName)) {
            // there's already a property with that name
            throw new ItemExistsException(state.safeGetJCRPath(nodePath));
        }
        ChildMaxPosition childMax = stateManager.getMaxChildPos(state, nodeName);
        if (checkCollision){
	        //_NodeState cne = stateManager.getChildNode(state, nodeName, 1, false);
	        //if (cne != null) {
        	if (childMax.getMax() > 0) {
	            // there's already a child node entry with that name;
	            // check same-name sibling setting of new node
	            if (!def.allowsSameNameSiblings()) {
	            	stateManager.getItem(nodePath, false);
	                throw new ItemExistsException(state.safeGetJCRPath(nodePath));
	            }
	            // check same-name sibling setting of existing node
	            //_NodeState cne = stateManager.getChildNode(state, nodeName, 1, false);
	            //if (!cne.getDefinition().allowsSameNameSiblings()) {
	            if (!childMax.getItem().getDefinition().allowsSameNameSiblings()) {
	                throw new ItemExistsException(state.safeGetJCRPath(nodePath));
	            }
	        }
        }
        
        // check protected flag of parent (i.e. this) node
        if (checkProtection() && getDefinition().isProtected()) {
            String msg = safeGetJCRPath() + ": cannot add a child to a protected node";
            LogUtils.debug(log, msg);
            throw new ConstraintViolationException(msg);
        }

        // now do create the child node
        _NodeImpl result = (_NodeImpl) createChildNode(nodeName, def, nodeType, uuid, checkCollision, checkLocks, createAutoCreatedChilds, newNodeId);
        state.registerModification();
        return result;
    }
    
    abstract protected boolean checkProtection() ;

	public NodeDefinition getDefinition() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        return state.getDefinition();
    }


    
    
    protected void canAddNode() throws AccessDeniedException {
        if (allowAddNode == null){
            try {
            	_getSession().getSecurityManager().checkPermission(state.getNodeId(), SecurityPermission.ADD_NODE.getPermissionName());
                allowAddNode = Boolean.TRUE;
            } catch (Exception e) {
                allowAddNode = Boolean.FALSE;
            }
        } 
        if (!allowAddNode.booleanValue()){
            throw new AccessDeniedException("No access rights to add node for node "+safeGetJCRPath());
        }
        
    }
    
    public void canSetProperty() throws AccessDeniedException {
    	if (state.getStatus().equals(ItemStatus.New)){
    		return;
    	}
    	if (!checkProtection()){
    		return;
    	}
        //if (!initializationStage){
            if (allowSetProperty == null){
                try {
                	_getSession().getSecurityManager().checkPermission(state.getNodeId(), SecurityPermission.SET_PROPERTY.getPermissionName());
                    allowSetProperty = Boolean.TRUE;
                } catch (Exception e) {
                    allowSetProperty = Boolean.FALSE;
                }
            } 
            if (!allowSetProperty.booleanValue()){
                throw new AccessDeniedException("No access rights to set property for node "+state.safeGetJCRPath());
            }
        //}
        
    }

    
    protected void canRemoveChild(_NodeState childState) throws AccessDeniedException {
        if (allowRemoveChild == null){
            try {
                _getSession().getSecurityManager().checkPermission(childState.getNodeId(), SecurityPermission.REMOVE.getPermissionName());
                allowRemoveChild = Boolean.TRUE;
            } catch (Exception e) {
            	//e.printStackTrace();
                allowRemoveChild = Boolean.FALSE;
            }
        } 
        if (!allowRemoveChild.booleanValue()){
            throw new AccessDeniedException("No access rights to remove child for node "+safeGetJCRPath());
        }
        
    }
    
    protected void canRemoveAllChilds() throws AccessDeniedException , RepositoryException{
        /*if (allowRemoveAllChild == null){
            try {
                _getSession().getSecurityManager().checkPermission(state.getNodeId(), SecurityPermission.REMOVE.getPermissionName(), true);
                allowRemoveAllChild = Boolean.TRUE;
            } catch (Exception e) {
            	//e.printStackTrace();
            	allowRemoveAllChild = Boolean.FALSE;
            }
        } 
        if (!allowRemoveAllChild.booleanValue()){
            throw new AccessDeniedException("No access rights to remove childs for node "+safeGetJCRPath());
        }*/
    	checkChildRemove(state);
        
    }
    
    private void checkChildRemove(_NodeState _state) throws RepositoryException{
		IdIterator idIterator = stateManager.getChildNodesId(_state, false, null);
		for(Long id:idIterator){
			_NodeState st = stateManager.getNodeState(id, null, true, id.toString());
			//check remove permission
			try {
				_getSession().getSecurityManager().checkPermission(st.getNodeId(), SecurityPermission.REMOVE.getPermissionName());
			} catch (Exception exc){
	            throw new AccessDeniedException("No access rights to remove child for node "+st.safeGetJCRPath());

			}
			checkChildRemove(st);
		}
		
	}

	/**
     * Same as <code>{@link Node#addNode(String, String)}</code> except that
     * this method takes <code>QName</code> arguments instead of
     * <code>String</code>s and has an additional <code>uuid</code> argument.
     * <p/>
     * <b>Important Notice:</b> This method is for internal use only! Passing
     * already assigned uuid's might lead to unexpected results and
     * data corruption in the worst case.
     *
     * @param nodeName     name of the new node
     * @param nodeTypeName name of the new node's node type or <code>null</code>
     *                     if it should be determined automatically
     * @param uuid         uuid of the new node or <code>null</code> if a new
     *                     uuid should be assigned
     * @return the newly added node
     * @throws ItemExistsException
     * @throws NoSuchNodeTypeException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws LockException
     * @throws RepositoryException
     */
    public synchronized _NodeImpl addNode(QName nodeName, QName nodeTypeName,
			String uuid) throws ItemExistsException, NoSuchNodeTypeException,
			VersionException, ConstraintViolationException, LockException,
			RepositoryException {
		return addNode(nodeName, nodeTypeName, uuid, true, true, true, null);
	}    
    
    public synchronized _NodeImpl addNode(QName nodeName, QName nodeTypeName,
			String uuid, Long newNodeId) throws ItemExistsException, NoSuchNodeTypeException,
			VersionException, ConstraintViolationException, LockException,
			RepositoryException {
		return addNode(nodeName, nodeTypeName, uuid, true, true, true, newNodeId);
	}    
    
    
    /**
     * Same as <code>{@link Node#addNode(String, String)}</code> except that
     * this method takes <code>QName</code> arguments instead of
     * <code>String</code>s and has an additional <code>uuid</code> argument.
     * <p/>
     * <b>Important Notice:</b> This method is for internal use only! Passing
     * already assigned uuid's might lead to unexpected results and
     * data corruption in the worst case.
     *
     * @param nodeName     name of the new node
     * @param nodeTypeName name of the new node's node type or <code>null</code>
     *                     if it should be determined automatically
     * @param uuid         uuid of the new node or <code>null</code> if a new
     *                     uuid should be assigned
     * @return the newly added node
     * @throws ItemExistsException
     * @throws NoSuchNodeTypeException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws LockException
     * @throws RepositoryException
     */
    public synchronized _NodeImpl addNode(QName nodeName, QName nodeTypeName,
                                         String uuid, boolean checkCollision, boolean checkLocks, 
                                         boolean createAutoCreatedChilds, Long newNodeId)
            throws ItemExistsException, NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // make sure this node is checked-out
        if (checkLocks && !internalIsCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot add node to a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check lock status
        if (checkLocks){
        	checkLock();
        }

        NodeTypeImpl nt = null;
        if (nodeTypeName != null) {
            nt = stateManager.getNodeTypeManager().getNodeType(nodeTypeName);
        }
        return internalAddChildNode(nodeName, nt, uuid, checkCollision,checkLocks, createAutoCreatedChilds, newNodeId);
    }

    
    /**
     * Determines the checked-out status of this node.
     * <p/>
     * A node is considered <i>checked-out</i> if it is versionable and
     * checked-out, or is non-versionable but its nearest versionable ancestor
     * is checked-out, or is non-versionable and there are no versionable
     * ancestors.
     *
     * @return a boolean
     * @see Node#isCheckedOut()
     */
    protected boolean internalIsCheckedOut(boolean enforceCheck) throws RepositoryException {
    	return state.internalIsCheckedOut(enforceCheck, getConnection());
    }
    
    public boolean hasProperty(String relPath) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        PropertyId id = state.resolveRelativePropertyPath(relPath);
        if (id != null) {
            return true;
            //TODO check read permission
            //return itemMgr.itemExists(id);
        } else {
            return false;
        }
    }
    
    /**
     * Returns the id of the property at <code>relPath</code> or <code>null</code>
     * if no property exists at <code>relPath</code>.
     * <p/>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) property
     * @return the id of the property at <code>relPath</code> or
     *         <code>null</code> if no property exists at <code>relPath</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    
    public boolean hasProperty(QName name) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (!state.hasProperty(name)) {
            return false;
        }
        
        //TODO check read permissions
        //PropertyId propId = new PropertyId(thisState.getUUID(), name);
        return true;
    }    

    /**
     * Check whether this node is locked by somebody else.
     *
     * @throws LockException       if this node is locked by somebody else
     * @throws RepositoryException if some other error occurs
     */
    protected void checkLock() throws LockException, RepositoryException {
        if (isNew()) {
            // a new node needs no check
            return;
        }

        if (_getSession().isIgnoreLock()){
            return;
        }
        
//        if (repository.isIgnoreLock()){
//        	return;
//        }
        _getSession().getLockManager().checkLock(this);
    }

	public void checkDeepLock() throws LockException, RepositoryException {
        if (isNew()) {
            // a new node needs no check
            return;
        }
        if (repository.isIgnoreLock()){
        	return;
        }
        _getSession().getLockManager().checkDeepLock(this);
	}
	
    public boolean hasNode(String relPath) throws RepositoryException {
        sanityCheck();

         Long id = resolveRelativeNodePath(relPath, true);
        if (id != null) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Indicates whether a child node with the specified <code>name</code> exists.
     * Returns <code>true</code> if the child node exists and <code>false</code>
     * otherwise.
     *
     * @param name The qualified name of the child node.
     * @return <code>true</code> if the child node exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasNode(QName name) throws RepositoryException {
        return hasNode(name, 1);
    }
    
    /**
     * Indicates whether a child node with the specified <code>name</code> exists.
     * Returns <code>true</code> if the child node exists and <code>false</code>
     * otherwise.
     *
     * @param name  The qualified name of the child node.
     * @param index The index of the child node (in the case of same-name siblings).
     * @return <code>true</code> if the child node exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasNode(QName name, int index) throws RepositoryException {
        // check state of this instance
        sanityCheck();
        Path p;
       // try {
            //p = Path.create(getPrimaryPath(), name, index, true);
            p = Path.create(name, index);
            Long _id = resolveRelativeNodePath(p, true);//p.toJCRPath(_getNamespaceResolver())
            if (_id != null) {
                return true;
            } else {
                return false;
            }
        //} catch (NoPrefixDeclaredException e) {
            //TODO 
        //    throw new RepositoryException(e);
        //} 
    }
    
    protected Long resolveRelativeNodePath(String relPath, boolean checkSecurity)
    	throws RepositoryException {
    	Path p;
		try {
			p = state.compilePath(relPath);
	    	return resolveRelativeNodePath(p, checkSecurity);
		} catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, e);
		}
    }    

    /**
     * Returns the id of the node at <code>relPath</code> or <code>null</code>
     * if no node exists at <code>relPath</code>.
     * <p/>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) node
     * @return the id of the node at <code>relPath</code> or
     *         <code>null</code> if no node exists at <code>relPath</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    protected Long resolveRelativeNodePath(Path p, boolean checkSecurity)
            throws RepositoryException {
        try {
            /**
             * first check if relPath is just a name (in which case we don't
             * have to build & resolve absolute path)
             */
            
            if (p.getLength() == 1) {
                Path.PathElement pe = p.getNameElement();
                if (pe.denotesName()) {
                    // check if node entry exists
                    int index = pe.getIndex();
                    if (index == 0) {
                        index = 1;
                    }
                    _NodeState n = stateManager.getChildNode(state, pe.getName(), index, checkSecurity);
                    if (n != null) {
                        return n.getNodeId();
                    } else {
                        // there's no child node with that name
                        return null;
                    }
                }
            }
            /**
             * build and resolve absolute path
             */
            
            
            p = Path.create(getPrimaryPath(), p, true);
            try {
                Long id = stateManager.resolvePath(p);
/*                if (id.denotesNode()) {
                    return (NodeId) id;
                } else {
                    // not a node
                    return null;
                }*/
                return id;
            } catch (PathNotFoundException pnfe) {
                return null;
            }
            //throw new UnsupportedOperationException();
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + p + " relative to " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    
    public _NodeImpl getNode(QName name2, int index2, boolean checkSecurity) throws RepositoryException {
    	//throw new UnsupportedOperationException();
    	_NodeState childState =  stateManager.getChildNode(state, name2, index2, checkSecurity);
    	if (childState != null){
    		return buildNode(childState);
    	} else {
    		return null;
    	}
    	
    }

	abstract protected _NodeImpl buildNode(_NodeState nodeState) throws RepositoryException ;

    public _NodeImpl getNode(QName name) throws ItemNotFoundException, RepositoryException {
        return getNode(name, 1, true);
    }

	public _NodeState getNodeState() {
		return state;
	}
	
	
    protected ItemId getItemId() {
    	return new NodeId(getNodeId(), state.getInternalUUID());
    }

    public PropertyImpl internalSetProperty(QName name, InternalValue value, boolean setModification, boolean triggerEvents)
    throws ValueFormatException, RepositoryException {
    	return internalSetProperty(name, value, setModification, triggerEvents, true, true);
    }

    
    /**
     * Sets the internal value of a property without checking any constraints.
     * <p/>
     * Note that no type conversion is being performed, i.e. it's the caller's
     * responsibility to make sure that the type of the given value is compatible
     * with the specified property's definition.
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public PropertyImpl internalSetProperty(QName name, InternalValue value, 
    		boolean setModification, boolean triggerEvents,boolean checkCollision, boolean checkLocks)
            throws ValueFormatException, RepositoryException {
        int type;
        if (value == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status, null, checkCollision, checkLocks);
        try {
            if (value == null) {
                prop.internalSetValue(null, type, setModification, triggerEvents);
            } else {
                prop.internalSetValue(new InternalValue[]{value}, type, setModification, triggerEvents);
            }
            
            if (!setModification){
            	prop.getPropertyState().resetToNormal();
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name,setModification);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    protected PropertyImpl internalSetProperty(QName name, InternalValue[] values,
            int type, boolean setModified)throws ValueFormatException, RepositoryException{
    	return internalSetProperty(name, values, type, setModified, true, true);
    }

    
    /**
     * Sets the internal value of a property without checking any constraints.
     * <p/>
     * Note that no type conversion is being performed, i.e. it's the caller's
     * responsibility to make sure that the type of the given values is compatible
     * with the specified property's definition.
     *
     * @param name
     * @param values
     * @param type
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    protected PropertyImpl internalSetProperty(QName name, InternalValue[] values,
                                           int type, boolean setModified, boolean checkCollision, boolean checkLocks)
            throws ValueFormatException, RepositoryException {
        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status, null, checkCollision, checkLocks);
        try {
            prop.internalSetValue(values, type, setModified);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name, setModified);
            }
            // rethrow
            throw re;
        }
        return prop;
    }    

    /**
     * @param name
     * @param type
     * @param multiValued
     * @param status
     * @param unstructuredPropertyId 
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    protected PropertyImpl getOrCreateProperty(String name, int type,
                                               boolean multiValued,
                                               BitSet status)
            throws ConstraintViolationException, RepositoryException {
        QName qName;
        try {
            qName = _getNamespaceResolver().getQName(name);
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid property name: " + name, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid property name: " + name, upe);
        }
        return getOrCreateProperty(qName, type, multiValued, status, null);
    }

    
    protected synchronized PropertyImpl getOrCreateProperty(QName name,
			int type, boolean multiValued, BitSet status,
			Long unstructuredPropertyId) throws ConstraintViolationException,
			RepositoryException {
		return getOrCreateProperty(name, type, multiValued, status,
				unstructuredPropertyId, true, true);
	}
    
    /**
	 * @param name
	 * @param type
	 * @param multiValued
	 * @param status
	 * @param unstructuredPropertyId
	 * @return
	 * @throws ConstraintViolationException
	 *             if no applicable property definition could be found
	 * @throws RepositoryException
	 *             if another error occurs
	 */
    protected synchronized PropertyImpl getOrCreateProperty(QName name, int type,
                                                            boolean multiValued,
                                                            BitSet status, Long unstructuredPropertyId,
                                                            boolean checkCollision, boolean checkLocks)
            throws ConstraintViolationException, RepositoryException {
        status.clear();

        if (state.hasProperty(name)) {
            return getProperty(name);
        }

        // does not exist yet:
        // find definition for the specified property and create property
        PropertyDefinitionImpl def = state.getApplicablePropertyDefinition(name, type, multiValued);
        _PropertyState prop = createChildProperty(name, type, def, unstructuredPropertyId, checkCollision, checkLocks);
        status.set(CREATED);
        PropertyImpl p = createPropertyImpl(prop);
        return p;
    }
    public NodeId getNodeItemId() {
        return (NodeId) getId();
    }

    public void removeChildProperty(String name) throws IllegalNameException, UnknownPrefixException, RepositoryException {
        removeChildProperty(QName.fromJCRName(name, _getNamespaceResolver()), true);
    }

    public void removeChildProperty(QName name) throws IllegalNameException, UnknownPrefixException, RepositoryException {
        removeChildProperty(name, true);
    }


    public void removeChildProperty(QName name2, boolean setModification) throws RepositoryException {
    	//throw new UnsupportedOperationException();
    	state.removeChildProperty(name2, setModification);
    }
    
    
    private PropertyImpl createPropertyImpl(_PropertyState pState) throws RepositoryException {
		PropertyImpl prop = new PropertyImpl(this, pState);
		//props.put(pState.getName(), prop);
		return prop;
	}
    
    public PropertyImpl getProperty(QName name) throws RepositoryException {
        // check state of this instance
        sanityCheck();
        
        return _getProperty(name);
    }
    
    public PropertyImpl _getProperty(QName name) throws RepositoryException {

        /*PropertyId propId = new PropertyId(this.getNodeId(), name);
        //try {
            //TODO check read permissions
            //_NodeImpl n = propId.getNode();
            for(Iterator it = loadedProperties.iterator() ; it.hasNext() ;){
                PropertyImpl prop = (PropertyImpl) it.next();
                if (prop.getQName().equals(propId.getName())){
                    return prop;
                }
            }
            throw new ItemNotFoundException(name.toString());
            //return (_PropertyImpl) itemMgr.getItem(propId);
        //} catch (AccessDeniedException ade) {
        //    throw new ItemNotFoundException(name.toString());
        //}*/
    	 PropertyImpl p = new PropertyImpl(this,getPropertyState(name, true));
    	return p;
    }
    
	public _PropertyState getPropertyState(QName name, boolean checkExistence) throws ItemNotFoundException {
    	_PropertyState pState = state.getProperty(name, checkExistence);
		return pState;
	}    
    
    
    public PropertyImpl setProperty(QName name, Value value)
    throws ValueFormatException, VersionException, LockException,
    ConstraintViolationException, RepositoryException {
    	return setProperty(name, value, true, true);
    }
	
    /**
     * Same as <code>{@link Node#setProperty(String, Value)}</code> except that
     * this method takes a <code>QName</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(QName name, Value value, boolean checkCollision, boolean checkLocks)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status, null, checkCollision, checkLocks);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name, true);
            }
            // rethrow
            throw re;
        }
        return prop;
    }
    
    
    /**
     * Checks various pre-conditions that are common to all
     * <code>setProperty()</code> methods. The checks performed are:
     * <ul>
     * <li>this node must be checked-out</li>
     * <li>this node must not be locked by somebody else</li>
     * </ul>
     * Note that certain checks are performed by the respective
     * <code>Property.setValue()</code> methods.
     *
     * @throws VersionException    if this node is not checked-out
     * @throws LockException       if this node is locked by somebody else
     * @throws RepositoryException if another error occurs
     * @see javax.jcr.Node#setProperty
     */
    protected void checkSetProperty()
            throws VersionException, LockException, RepositoryException {
        // make sure this node is checked-out
        /*if (!internalIsCheckedOut()) {
            String msg = safeGetJCRPath()
                    + ": cannot set property of a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }*/

        // check lock status
        //checkLock();
    }
    
    
    /**
     * Same as <code>{@link Node#setProperty(String, Value[])}</code> except that
     * this method takes a <code>QName</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param values
     * @return
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(QName name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        int type;
        if (values == null || values.length == 0
                || values[0] == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }
        return setProperty(name, values, type);
    }
    
    /**
     * Same as <code>{@link Node#setProperty(String, Value[], int)}</code> except
     * that this method takes a <code>QName</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param values
     * @param type
     * @return
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(QName name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status, null);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
            	re.printStackTrace();
                removeChildProperty(name, true);
            }
            // rethrow
            throw re;
        }
        return prop;
    }
    
    public String getInternalUUID() throws RepositoryException{
    	return state.getInternalUUID();
    }
    

    public void removeChildNode(QName nodeName, int index,
			boolean skipSNSIpdate, boolean checkSecurity) throws RepositoryException {

		if (index == 0) {
			index = 1;
		}
		_NodeState childNodeState = stateManager.getChildNode(state, nodeName,
				index, true);
    	
    	
    	// check permission
    	if (checkSecurity){
    		canRemoveChild(childNodeState);
        	//check all childs
    		
    		//canRemoveAllChilds();
    	}

    	
		DatabaseSelectAllStatement stAllChilds = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false);
        stAllChilds.setDistinct(true);
        stAllChilds.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
        stAllChilds.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__PARENT);
        //stAllChilds.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__NODE_PATH);
        stAllChilds.addResultColumn(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__NODE_DEPTH);
        
        stAllChilds.addOrder(Order.asc(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__NODE_DEPTH));
        //stAllChilds.addOrder(Order.asc(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__PARENT));
        
        stAllChilds.addJoin(Constants.TABLE_NODE_PARENT, "parents", Constants.FIELD_ID, Constants.FIELD_TYPE_ID );
        stAllChilds.addCondition(Conditions.eq("parents."+Constants.TABLE_NODE_PARENT__PARENT_ID, childNodeState.getNodeId()));
        //stAllChilds.addCondition(Conditions.eqProperty(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID, Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID));
        //stAllChilds.addCondition(Conditions.not(Conditions.eq(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID, securityId)));
        //stAllChilds.addGroup(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__SECURITY_ID);
        stAllChilds.execute(getConnection());
        NodeTreeItem tree = new NodeTreeItem(childNodeState.getNodeId(), (long) childNodeState.getDepth(), null);
        NodeTreeItem active = tree;
        NodeTreeItem _active = tree;
        int i = 0 ;
        while (stAllChilds.hasNext()){
        	if (active == null){
        		active = _active;
        	}
        	_active = active;
        	RowMap r = stAllChilds.nextRow();
        	//System.out.println(r);
        	Long nId = r.getLong(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.FIELD_ID);
        	Long parentId = r.getLong(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__PARENT);
        	_NodeState cachedNode = stateManager.getNodeFromCache(nId);
        	
        	boolean parentIdChanged = false;
        	if (cachedNode != null && cachedNode.getParentId() != parentId){
        	    parentId = cachedNode.getParentId();
        	    parentIdChanged = true;
        	}
        	Long depth = r.getLong(Constants.DEFAULT_ROOT_ALIAS+"."+Constants.TABLE_NODE__NODE_DEPTH);
        	if (!active.getId().equals(parentId)){
        		active = active.find(parentId, (depth-2));
        	}
        	if (active == null || !active.getId().equals(parentId)){
        		active = tree.find(parentId, (depth-2));
        	}
            if (active == null && parentIdChanged){
                log.debug("parent not found");
                continue;
            }
        	new NodeTreeItem(nId, depth, active);
        	//i++;
        	if (checkSecurity){
        		try {
        			_getSession().getSecurityManager().checkPermission(nId, SecurityPermission.REMOVE.getPermissionName());
        		} catch (java.security.AccessControlException exc){
        			throw new AccessDeniedException(exc.getMessage());
        		}
        	}
        	
        }
        //System.out.println("Childs size "+i);
        tree.addChilds(_getRepository().getBatchSize());
    	
    	

		// modify the state of 'this', i.e. the parent node
		// NodeState thisState = (NodeState) getOrCreateTransientItemState();
		if (childNodeState == null) {
			String msg = "failed to remove child " + nodeName + " of "
					+ state.safeGetJCRPath();
			log.debug(msg);
			//only for debug
			stateManager.getChildNode(state, nodeName,
					index, true);
			throw new RepositoryException(msg);
		}

		// notify target of removal
		// Long childId = entry.getNodeId();
		
        
		ChildMaxPosition max = stateManager.getMaxChildPos(state, nodeName);

		try {
			childNodeState.onRemove(tree);
		} catch (RepositoryException exc){
			getSession().logout();
			throw exc;
		}
		//childNodeState.regesterRemoveNode();

		// check sns
		// TODO skip this if removes all childs of parent node
		max.dec();
		if (!skipSNSIpdate) {
			_NodeImpl childNode = instantiate(childNodeState);
			if (childNode.state.getDefinition().allowsSameNameSiblings()) {
				String childName = childNode.getName();
				if (childName.indexOf("[") > 0) {
					childName = childName.substring(0, childName.indexOf("["));
				}
				long pos = 1;
				NodeStateIterator childs = stateManager.getNodesWithName(state,childName, false);
				for (_NodeState n : childs) {
					n.updateName(null, pos++, null, stateManager.createNodeModification(), false); // 3 - state
				}
			}
		}
		state.clearCachedChildNames();
		registerModification();

	}
    

	protected void registerModification() {
		state.registerModification();
	}

	public void addMixin(QName mixinName) throws NoSuchNodeTypeException,
			VersionException, ConstraintViolationException, LockException,
			RepositoryException {
		// check state of this instance
		sanityCheck();

		canSetProperty();

		// make sure this node is checked-out
		if (!internalIsCheckedOut(false)) {
			String msg = safeGetJCRPath()
					+ ": cannot add a mixin node type to a checked-in node";
			log.debug(msg);
			throw new VersionException(msg);
		}

		// check protected flag
		if (getDefinition().isProtected()) {
			String msg = safeGetJCRPath()
					+ ": cannot add a mixin node type to a protected node";
			log.debug(msg);
			throw new ConstraintViolationException(msg);
		}

		// check lock status
		checkLock();

		NodeTypeManagerImpl ntMgr = getNodeTypeManager();
		NodeTypeImpl mixin = ntMgr.getNodeType(mixinName);
		if (!mixin.isMixin()) {
			throw new RepositoryException(mixinName + ": not a mixin node type");
		}
		NodeTypeImpl primaryType = ntMgr
				.getNodeType(state.getPrimaryTypeName());
		if (primaryType.isDerivedFrom(mixinName)) {
			throw new RepositoryException(mixinName
					+ ": already contained in primary node type");
		}

		// build effective node type of mixin's & primary type in order to
		// detect conflicts
		NodeTypeRegistry ntReg = stateManager.getNodeTypeRegistry();
		EffectiveNodeType entExisting;
		try {
			// existing mixin's
			Set<QName> set = getMixinTypeNames();
			// primary type
			set.add(state.getPrimaryTypeName());
			// build effective node type representing primary type including
			// existing mixin's
			entExisting = ntReg.getEffectiveNodeType((QName[]) set
					.toArray(new QName[set.size()]));
			if (entExisting.includesNodeType(mixinName)) {
				throw new RepositoryException(mixinName
						+ ": already contained in mixin types");
			}
			// add new mixin
			set.add(mixinName);
			// try to build new effective node type (will throw in case of
			// conflicts)
			EffectiveNodeType ent = ntReg.getEffectiveNodeType((QName[]) set
					.toArray(new QName[set.size()]));

			QName[] allTypes = ent.getAllNodeTypes();
			for (int i = 0; i < allTypes.length; i++) {
				QName ntName = allTypes[i];
				if (!state.isNodeTypeRegistered(ntName)) {
					registerNewNodeType(ntName, mixinName);
				}
			}

		} catch (NodeTypeConflictException ntce) {
			throw new ConstraintViolationException(ntce.getMessage());
		}

		//if (true) {
		//    throw new UnsupportedOperationException();
		//}
		// do the actual modifications implied by the new mixin;
		// try to revert the changes in case an exception occurs
		try {
			// modify the state of this node
			//NodeState thisState = (NodeState) getOrCreateTransientItemState();
			// add mixin name
			Set<QName> mixins = new HashSet<QName>(getMixinTypeNames());
			mixins.add(mixinName);
			//thisState.setMixinTypeNames(mixins);

			// set jcr:mixinTypes property
			setMixinTypesProperty(mixins);

			//registerNewNodeType(mixinName);

			// add 'auto-create' properties defined in mixin type
			PropertyDefinition[] pda = mixin
					.getAutoCreatedPropertyDefinitions();
			for (int i = 0; i < pda.length; i++) {
				PropertyDefinitionImpl pd = (PropertyDefinitionImpl) pda[i];
				// make sure that the property is not already defined by primary
				// type
				// or existing mixin's
				NodeTypeImpl declaringNT = (NodeTypeImpl) pd
						.getDeclaringNodeType();
				if (!entExisting.includesNodeType(declaringNT.getQName())) {
					createChildProperty(pd.getQName(), pd.getRequiredType(),
							pd, null);
				}
			}

			// recursively add 'auto-create' child nodes defined in mixin type
			NodeDefinition[] nda = mixin.getAutoCreatedNodeDefinitions();
			for (int i = 0; i < nda.length; i++) {
				NodeDefinitionImpl nd = (NodeDefinitionImpl) nda[i];
				// make sure that the child node is not already defined by primary type
				// or existing mixin's
				NodeTypeImpl declaringNT = (NodeTypeImpl) nd
						.getDeclaringNodeType();
				if (!entExisting.includesNodeType(declaringNT.getQName())) {
					createChildNode(nd.getQName(), nd, (NodeTypeImpl) nd
							.getDefaultPrimaryType(), null, true, null);
				}
			}
		} catch (RepositoryException re) {
			// try to undo the modifications by removing the mixin
			try {
				removeMixin(mixinName);
			} catch (RepositoryException re1) {
				// silently ignore & fall through
			}
			throw re;
		}
		registerModification();
	}

    public Set<QName> getMixinTypeNames() throws RepositoryException{
    	return state.getMixinTypeNames();
    }

    /**
     * Same as {@link Node#removeMixin(String)} except that it takes a
     * <code>QName</code> instead of a <code>String</code>.
     *
     * @see Node#removeMixin(String)
     */
    public void removeMixin(QName mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        canSetProperty();
        
        // make sure this node is checked-out
        if (!internalIsCheckedOut(false)) {
            String msg = state.safeGetJCRPath()
                    + ": cannot remove a mixin node type from a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check protected flag
        if (getDefinition().isProtected()) {
            String msg = state.safeGetJCRPath()
                    + ": cannot remove a mixin node type from a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        checkLock();

        // check if mixin is assigned
        if (!getMixinTypeNames().contains(mixinName)) {
            throw new NoSuchNodeTypeException();
        }

        NodeTypeManagerImpl ntMgr = stateManager.getNodeTypeManager();
        NodeTypeRegistry ntReg = stateManager.getNodeTypeRegistry();

        // build effective node type of remaining mixin's & primary type
        Set<QName> remainingMixins = new HashSet<QName>(getMixinTypeNames());
        // remove name of target mixin
        remainingMixins.remove(mixinName);
        EffectiveNodeType entRemaining;
        try {
            // remaining mixin's
            HashSet<QName> set = new HashSet<QName>(remainingMixins);
            // primary type
            set.add(state.getPrimaryTypeName());
            // build effective node type representing primary type including remaining mixin's
            entRemaining = ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            throw new ConstraintViolationException(ntce.getMessage());
        }

        /**
         * mix:referenceable needs special handling because it has
         * special semantics:
         * it can only be removed if there no more references to this node
         */
        NodeTypeImpl mixin = ntMgr.getNodeType(mixinName);
        if ((QName.MIX_REFERENCEABLE.equals(mixinName)
                || mixin.isDerivedFrom(QName.MIX_REFERENCEABLE))
                && !entRemaining.includesNodeType(QName.MIX_REFERENCEABLE)) {
            // removing this mixin would effectively remove mix:referenceable:
            // make sure no references exist
            PropertyIterator iter = getReferences();
            if (iter.hasNext()) {
                throw new ConstraintViolationException(mixinName + " can not be removed: the node is being referenced"
                        + " through at least one property of type REFERENCE");
            }
        }

        
        
        
        // modify the state of this node
        //NodeState thisState = (NodeState) getOrCreateTransientItemState();
        //setMixinTypeNames(remainingMixins);

        // set jcr:mixinTypes property
        setMixinTypesProperty(remainingMixins);

        // shortcut
        if (mixin.getChildNodeDefinitions().length == 0
                && mixin.getPropertyDefinitions().length == 0) {
            // the node type has neither property nor child node definitions,
            // i.e. we're done
            return;
        }
        //throw new UnsupportedOperationException();
        // walk through properties and child nodes and remove those that have been
        // defined by the specified mixin type

        // use temp set to avoid ConcurrentModificationException
        HashSet<QName> set = new HashSet<QName>(state.getPropertyNames());
        for (QName propName : set) {
            PropertyImpl prop = (PropertyImpl) getProperty(propName);
            // check if property has been defined by mixin type (or one of its supertypes)
            NodeTypeImpl declaringNT = (NodeTypeImpl) prop.getDefinition().getDeclaringNodeType();
            if (!entRemaining.includesNodeType(declaringNT.getQName())) {
                // the remaining effective node type doesn't include the
                // node type that declared this property, it is thus safe
                // to remove it
                removeChildProperty(propName, true);
            }
        }
        // use temp array to avoid ConcurrentModificationException
        //ArrayList list = new ArrayList(thisState.getChildNodeEntries());
        for (_NodeState node: stateManager.getNodesWithName(state, null, false) ) {
            //NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            //_NodeImpl node = (_NodeImpl) itemMgr.getItem(new NodeId(entry.getUUID()));
            //NodeImpl node = (NodeImpl) iter.nextNode();
            // check if node has been defined by mixin type (or one of its supertypes)
            NodeTypeImpl declaringNT = (NodeTypeImpl) node.getDefinition().getDeclaringNodeType();
            if (!entRemaining.includesNodeType(declaringNT.getQName())) {
                // the remaining effective node type doesn't include the
                // node type that declared this child node, it is thus safe
                // to remove it
                removeChildNode(node.getName(), node.getIndex(), false, true);
            }
        }
        state.unregisterNodeType(mixinName);
    }
    
    public PropertyIterator getReferences() throws RepositoryException {
        ArrayList<_PropertyState> result = new ArrayList<_PropertyState>();
        ArrayList<NodeReference> refs = state.getReferencesFrom();
//      TODO optimize read Ahead
        for(NodeReference nr : refs){
            //create property
            Long nodeId = nr.getFromId();
            _NodeState n = stateManager.getNodeState(nodeId,null, true,nodeId.toString());
            QName propertyName = nr.getPropertyQName();
            _PropertyState p = n.getProperty(propertyName, true);
            result.add(p);
        }
        return new PropertyIteratorImpl(_getSession(), result, null);
    }
    
    
    public PropertyIterator getReferences(String name, boolean getWeakReferences) {
    	throw new UnsupportedOperationException();
    	
    }
  
    protected void setMixinTypesProperty(Set mixinNames) throws RepositoryException {
        // get or create jcr:mixinTypes property
        _PropertyState prop;
        if (state.hasProperty(QName.JCR_MIXINTYPES)) {
            prop =  state.getProperty(QName.JCR_MIXINTYPES, true);
        } else {
            // find definition for the jcr:mixinTypes property and create property
            PropertyDefinitionImpl def = state.getApplicablePropertyDefinition(QName.JCR_MIXINTYPES, PropertyType.NAME, true);
            prop = createChildProperty(QName.JCR_MIXINTYPES, PropertyType.NAME, def, null);
        }

        if (mixinNames.isEmpty()) {
            // purge empty jcr:mixinTypes property
            removeChildProperty(QName.JCR_MIXINTYPES, true);
            return;
        }

        // call internalSetValue for setting the jcr:mixinTypes property
        // to avoid checking of the 'protected' flag
        InternalValue[] vals = new InternalValue[mixinNames.size()];
        Iterator iter = mixinNames.iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            vals[cnt++] = InternalValue.create((QName) iter.next());
        }
        prop.internalSetValue(vals, PropertyType.NAME, true, true);
        
    }    





    /**
     * Same as {@link Node#isNodeType(String)} except that it takes a
     * <code>QName</code> instead of a <code>String</code>.
     *
     * @param ntName name of node type
     * @return <code>true</code> if this node is of the specified node type;
     *         otherwise <code>false</code>
     */
    public boolean isNodeType(QName ntName) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return _isNodeType(ntName);
    }
    public boolean _isNodeType(QName ntName) throws RepositoryException {
        return state.isNodeType(ntName);
    }
    
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return state.getPrimaryNodeType();
    }

    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
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
                // setting value failed, get rid of newly created property
                try {
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
    
    public Property setProperty(QName name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status, null);
        try {
            if (type == PropertyType.UNDEFINED) {
                prop.setValue(value);
            } else {
                prop.setValue(ValueHelper.convert(value, type));
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                try {
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

    //-----------------------------------< versioning support: implementation >
    /**
     * Checks if this node is versionable, i.e. has 'mix:versionable'.
     *
     * @throws UnsupportedRepositoryOperationException
     *          if this node is not versionable
     */
    protected void checkVersionable()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!isNodeType(QName.MIX_VERSIONABLE)) {
            String msg = "Unable to perform versioning operation on non versionable node: " + safeGetJCRPath();
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
    }
    
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        checkVersionable();
        return (Version) getProperty(QName.JCR_BASEVERSION).getNode();
    }


}
