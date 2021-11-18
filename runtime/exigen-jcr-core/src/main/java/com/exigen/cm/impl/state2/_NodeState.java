/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.LEFT_INDEX;
import static com.exigen.cm.Constants.PATH_DELIMITER;
import static com.exigen.cm.Constants.RIGHT_INDEX;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT__PARENT_ID;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.impl.NodeDefinitionImpl;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeModification;
import com.exigen.cm.impl.NodeReference;
import com.exigen.cm.impl.NodeTreeItem;
import com.exigen.cm.impl.NodeTypeContainer;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.ParentNode;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SecurityCopyType;
import com.exigen.cm.impl.SecurityEntry;
import com.exigen.cm.impl.SecurityModificationEntry;
import com.exigen.cm.impl.SoftHashMap;
import com.exigen.cm.impl.observation.EventState;
import com.exigen.cm.impl.observation.EventStateCollection;
import com.exigen.cm.impl.security.BaseSecurityPermission;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SecurityPrincipal;
import com.exigen.cm.impl.state.NodeOverlayedState;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.jackrabbit.BaseException;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.nodetype.PropDefImpl;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.InternalValue;

public class _NodeState extends _ItemState {

    /** Log for this class */
    private static final Log log = LogFactory.getLog(_NodeState.class);
	
	private Long version = (long) 1;

	private Long nodeId;

	private QName name;

	private Long index;
	private Long snsMax;

	private Long parentLockId;
	private String lockOwner;

	private Long storeConfigurationId;

	private Long securityId;

	private Long parentId;

	private Long nodeTypeId;

	private String internalPath;

	private Long internalDepth;

	private String internalUUID;

	private Long workspaceId;

	private ArrayList<_PropertyState> removedProperties = new ArrayList<_PropertyState>();

	private HashMap<QName, _PropertyState> properties = new HashMap<QName, _PropertyState>();
	private HashMap<QName, _PropertyState> originalProperties = null;
	

	private ArrayList<ParentNode> parentNodes = new ArrayList<ParentNode>();

	protected NodeDefinitionImpl definition;

	//private ArrayList<NodeReference> referencesTo = new ArrayList<NodeReference>();
	private ArrayList<NodeReference> _referencesTo = null;

	//private ArrayList<NodeReference> referencesFrom = new ArrayList<NodeReference>();
	private ArrayList<NodeReference> _referencesFrom = null;
    private ArrayList<NodeReference> tmpReferencesFromAdd = new ArrayList<NodeReference>();
    private ArrayList<NodeReference> tmpReferencesFromRemoved = new ArrayList<NodeReference>();

	private ArrayList<NodeTypeContainer> nodeTypes = new ArrayList<NodeTypeContainer>();

	private boolean _basePropertiesChanged = false;
	
    private List<SecurityEntry> acl = null;
    
    private Set<Long> _parentCache;
    private Set<Long> _parentCacheAll;

	//transient
	private ArrayList<NodeTypeContainer> effectiveNodeTypes = null;
	private HashSet<QName> effectiveMixinTypes = null;
	HashMap<IndexedQname, Long> cachedChildNodes = new HashMap<IndexedQname, Long>();
	protected _AbstractsStateManager stateManager;
    private NodeOverlayedState overlayedState = null;
    private ArrayList<EventState> events = new ArrayList<EventState>();
	private List<SecurityModificationEntry> modifiedSecurity = new ArrayList<SecurityModificationEntry>();

	//private List<_NodeState> modifiedNodes;

	private EffectiveNodeType cachedEffectiveNodeType = null;
	
	boolean browseMode = false;
	
	private ArrayList<RowMap> aces = new ArrayList<RowMap>();
	
	public _NodeState(Long nodeId, RepositoryImpl repository) {
		super(repository);
		this.nodeId = nodeId;
	}

	public _NodeState(RepositoryImpl repository) throws RepositoryException{
		super(repository);
		this.nodeId = repository.nextId();
	}

	public _NodeState createCopy() throws RepositoryException {
		return createCopy(null);
	}

	public _NodeState createCopy(_NodeState other) throws RepositoryException {
		if (other == null) {
			other = new _NodeState(nodeId, this.getRepository());
		}
		//1.copy base properties
		other.version = version;
		other.name = name;
		other.index = index;
		other.snsMax = snsMax;
		other.parentLockId = parentLockId;
		other.lockOwner = lockOwner;
		other.storeConfigurationId = storeConfigurationId;
		other.securityId = securityId;
		other.parentId = parentId;
		other.nodeTypeId = nodeTypeId;
		other.internalPath = internalPath;
		other.internalDepth = internalDepth;
		other.internalUUID = internalUUID;
		other.workspaceId = workspaceId;
		other.definition = definition;
		other._basePropertiesChanged = false;
		other.createInTransaction = createInTransaction;

		//2.copy properties
		other.properties.clear();
		for(_PropertyState p:properties.values()){
			if (!p.getName().equals(QName.JCR_PRIMARYTYPE) && !p.getName().equals(QName.JCR_MIXINTYPES)){
				other.addProperty(p.copyProperty(other));
			}
		}
		
		other.parentNodes.clear();
		//3.copy parent nodes info
		for(ParentNode pn:parentNodes){
			other.addParentNode(pn.copy());
		}
		
		//4.copy nodetypes
		other._parentCache = null;
		other._parentCacheAll = null;
		other.nodeTypes.clear();
		other.cachedEffectiveNodeType = null;
		for(NodeTypeContainer nt: nodeTypes){
			other.registerNodeType(nt.copy());
		}

		//copy references
		if (other.isReferencesFromLoaded()){
			other._referencesFrom.clear();
		}
		other.tmpReferencesFromAdd.clear();
		other.tmpReferencesFromRemoved.clear();
		if (other.isReferencesToLoaded()){
			other.getReferencesTo().clear();
		}
		
		if (isReferencesToLoaded()){
			other._referencesTo = new ArrayList<NodeReference>();
			for(NodeReference r: getReferencesTo()){
				NodeReference r1 = new NodeReference(getNamespaceRegistry(),r.getId(), r.getFromId(), r.getToId(), r.getPropertyQName(), r.getUUID());
				r1.resetStateToNormal();
				other.addReferencesTo(r1);
			}
		} else {
			other._referencesTo = null;
		}

		if (isReferencesFromLoaded()){
			other._referencesFrom = new ArrayList<NodeReference>();
			for(NodeReference r: getReferencesFrom()){
				NodeReference r1 = new NodeReference(getNamespaceRegistry(),r.getId(), r.getFromId(), r.getToId(), r.getPropertyQName(), r.getUUID());
				r1.resetStateToNormal();
				other.addReferencesFrom(r1);
			}
		}

		other.resetToNormal();
		
		
		other.effectiveNodeTypes = null;
		other.effectiveMixinTypes = null;
		other.cachedChildNodes = new HashMap<IndexedQname, Long>();
		other.overlayedState = null;
		return other;
	}


	void setNodeTypes(ArrayList<NodeTypeContainer> value) {
		this.nodeTypes = value;
		this.cachedEffectiveNodeType = null;
		effectiveNodeTypes = null;
		effectiveMixinTypes = null;
	}
	
	public void addParentNode(ParentNode pn) {
		parentNodes.add(pn);
		_parentCache = null;
		_parentCacheAll = null;
	}

	public void setParentId(Long value) {
		this.parentId = value;
	}

	public void setStoreConfigurationId(Long value) {
		this.storeConfigurationId = value;
	}

	void setVersion(Long value) {
		this.version = value;
	}

	public void setSecurityId(Long value) {
		this.securityId = value;
	}

	public void setWorkspaceId(Long value) {
		this.workspaceId = value;
	}

	public void setParentLockId(Long value) {
		this.parentLockId = value;
	}

	public void setIndex(Long value) {
		this.index = value;
	}

	public void setInternalDepth(Long value) {
		this.internalDepth = value;
	}

	public void setInternalPath(String value) {
		this.internalPath = value;
	}

	public void setNodeTypeId(Long value) {
		this.nodeTypeId = value;
	}

	public void setName(QName value) {
		this.name = value;
	}

	public Long getNodeId() {
		return nodeId;
	}


	public void addProperty(_PropertyState property) throws RepositoryException {
		//check that property already exusts
		if (hasProperty(property.getName())){
			throw new RepositoryException("Property "+property.getName()+" already exists in "+safeGetJCRPath());
		}
		this.properties.put(property.getName(), property);
		if (this.stateManager != null){
			property.assignSession(stateManager);
		}
	}

	public Set<QName> getMixinTypeNames()  throws RepositoryException {
		if (effectiveMixinTypes == null){
	        List<NodeTypeContainer> types = getAllEffectiveTypes();
	        HashSet<QName> result = new HashSet<QName>();
	        for(NodeTypeContainer type : types){
	            if (type.isMixin() && type.getNodeTypeId().equals(type.getFromTypeId())){
	                result.add(type.getName());
	            }
	        }
	        effectiveMixinTypes = result;
		}
		return effectiveMixinTypes;
	}
	
	public List<NodeTypeContainer> getAllEffectiveTypes() throws RepositoryException {
		if (effectiveNodeTypes == null){
	        ArrayList<NodeTypeContainer> result  = new ArrayList<NodeTypeContainer>();
	        for(NodeTypeContainer type:nodeTypes){
	            if (type.isEffective()){
	                result.add(type);
	            }
	        }
	        effectiveNodeTypes = result;
		}
        return effectiveNodeTypes;
    }
    public boolean hasProperty(QName name) {
        return hasProperty(name, false);
    }

	
	public boolean hasProperty(QName name, boolean allowInBrowseMode) {
		if (properties.containsKey(name)){
			return true;
		}
		if (allowInBrowseMode && originalProperties != null){
		    if (originalProperties.containsKey(name)){
		        return true;
		    }
		}
		return false;
	}

	public void setInternalUUID(String value) {
		this.internalUUID = value;
	}

	
    public PropertyDefinitionImpl getApplicablePropertyDefinition(
            QName propertyName, int type, boolean multiValued)
            throws ConstraintViolationException, RepositoryException {
        PropDef pd = getEffectiveNodeType().getApplicablePropertyDef(
                propertyName, type, multiValued);
        return getNodeTypeManager().getPropertyDefinition(pd);
    }
    
	public EffectiveNodeType getEffectiveNodeType() throws RepositoryException {
		if (cachedEffectiveNodeType == null){
	        // build effective node type of mixins & primary type
	        NodeTypeRegistry ntReg = getNodeTypeRegistry();
	        // mixin types
	        Set<QName> set = getMixinTypeNames();
	        QName[] types = new QName[set.size() + 1];
	        set.toArray(types);
	        // primary type
	        types[types.length - 1] = getPrimaryTypeName();
	        try {
	        	cachedEffectiveNodeType =  ntReg.getEffectiveNodeType(types);
	        } catch (NodeTypeConflictException ntce) {
	            String msg = "internal error: failed to build effective node type for node " + safeGetJCRPath();
	            log.debug(msg);
	            throw new RepositoryException(msg, ntce);
	        }
		}
		return cachedEffectiveNodeType;
    }

    public QName getPrimaryTypeName() throws RepositoryException {
        return ((NodeTypeImpl)getPrimaryNodeType()).getQName();
    }

    public NodeTypeImpl getPrimaryNodeType() throws RepositoryException {
        return getNodeTypeManager().getNodeTypeBySQLId(nodeTypeId);
    }

	@Override
	public boolean isNode() {
		return true;
	}

	public String getInternalPath() {
		return internalPath;
	}

	@Override
	public QName getName() {
		return name;
	}

	private WeakReference<_NodeState> parentState = null;

    private EffectiveNodeType cachedParentEffectiveNodeType;
	
	WeakReference<_NodeState> getParentState(){
		return parentState;
	}
	
	public _NodeState getWeakParentState(){
		if (parentState == null){
			return null;
		}
		return parentState.get();
	}
	
	public void registerParent(_NodeState _parent){
		parentState = new WeakReference<_NodeState>(_parent);
	}
	
	@Override
	public _NodeState getParent() throws RepositoryException {
		if (parentState != null){
			_NodeState p = parentState.get();
			if (p != null){
				return p;
			}
		}
		if (parentId != null){
			_NodeState p =  stateManager.getNodeState(parentId, null);
			parentState = new WeakReference<_NodeState>(p);
			return p;
		} else {
			return null;
		}
	}

	public String getInternalUUID(){
		if (internalUUID == null){
			if (hasProperty(QName.JCR_UUID, true)){
				_PropertyState state;
				try {
					state = getProperty(QName.JCR_UUID, true, true);
					if (((NodeTypeImpl)state.getDefinition().getDeclaringNodeType()).getQName().equals(QName.MIX_REFERENCEABLE)){
						Object value = state.getValues()[0].internalValue();
						if (value instanceof String){
							internalUUID = (String) value;
						} else {
							UUID newUUID = (UUID) state.getValues()[0].internalValue();
							internalUUID = newUUID.toString();
						}
					}
				} catch (ItemNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			
		}
		return internalUUID;
	}

	public int getIndex() {
		return index.intValue();
	}

	public Long getIndexLong() {
		return index;
	}

	public NodeType[] getMixinNodeTypes() throws RepositoryException {
		Set<QName> mixinNames = getMixinTypeNames();
        if (mixinNames.isEmpty()) {
            return new NodeType[0];
        }
        NodeType[] nta = new NodeType[mixinNames.size()];
        Iterator<QName> iter = mixinNames.iterator();
        int i = 0;
        while (iter.hasNext()) {
            nta[i++] = getNodeTypeManager().getNodeType(iter.next());
        }
        return nta;	
     }

	public Long getVersion() {
		return version;
	}

	public Long getSecurityId() {
		return securityId;
	}

	public Long getNodeTypeId() {
		return nodeTypeId;
	}

	public Long getWorkspaceId() {
		return workspaceId;
	}

	/*public boolean hasPropertyName(QName propertyName) {
        for(_PropertyState pState:properties){
	        if (pState.getName().equals(propertyName)){
	            return true;
	        }
	    }
	    return false;
	}*/

	public int getInternalDepth() {
		return internalDepth.intValue();
	}
	

	public Long getInternalDepthLong() {
		return internalDepth;
	}

	public boolean hasCachedChildName(IndexedQname name) {
		return cachedChildNodes.containsKey(name);
	}

	public Long getCachedChildName(IndexedQname name) {
		return cachedChildNodes.get(name);
	}
	
	public Collection<Long> getCachedChilds(){
		return cachedChildNodes.values();
	}
	public Collection<Long> getCachedChilds(QName _name, QName[] nodetypes) throws RepositoryException{
		ArrayList<Long> result = new ArrayList<Long>();
		for(IndexedQname name:cachedChildNodes.keySet()){
			if (_name == null || name.getName().equals(_name)){
			    Long childNodeId = cachedChildNodes.get(name);
			    if (nodetypes != null && nodetypes.length > 0){
			        //check nodetypes
			        boolean skip = true;
			        _NodeState n = getStateManager().getNodeState(childNodeId, null);
			        for(QName nodetype:nodetypes){
			            if (n.isNodeType(nodetype)){
			                skip = false;
			                break;
			            }
			            
			        }
			        if (skip){
			            continue;
			        }
			    }
				result.add(childNodeId);
			}
		}
		return result; 
	}

	public void registerChild(IndexedQname childNodeName, Long nodeId2) {
		cachedChildNodes.put(childNodeName, nodeId2);
	}
	
	public Long getParentId(){
		return parentId;
	}

    public NodeDefinition getDefinition() throws RepositoryException {
        if (definition == null){
            if (parentId == null){
                definition = stateManager.getRootNodeDefinition();
            } else {
                _NodeState _parentState = stateManager.getNodeFromCache(parentId);
                if (_parentState != null){
                    definition = stateManager.getNodeState(parentId, null).getApplicableChildNodeDefinition(name, getPrimaryTypeName());
                } else {
                    NodeDef cnd = getParentetEffectiveNodeType().getApplicableChildNodeDef(
                            name, getPrimaryTypeName(), getNodeTypeRegistry());
                    definition = getNodeTypeManager().getNodeDefinition(cnd.getSQLId());
                    //throw new UnsupportedOperationException("cause:new security implementation");
                }
                
            }
        }
        return definition;
    }
    
    private EffectiveNodeType getParentetEffectiveNodeType() throws RepositoryException {
        _NodeState _parentState = stateManager.getNodeFromCache(parentId);
        if (_parentState != null){
            return _parentState.getEffectiveNodeType();
        } else {
            try {
                _parentState = stateManager.getNodeState(parentId, null);
                return _parentState.getEffectiveNodeType();
            } catch (Exception e) {
                
            }
            //loading directly from database
            if (cachedParentEffectiveNodeType == null){
                // build effective node type of mixins & primary type
                NodeTypeRegistry ntReg = getNodeTypeRegistry();
                // mixin types
                DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_TYPE, true);
                st.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, parentId));
                st.execute(stateManager.getConnection());
                List<RowMap> rows = st.getAllRows();
                Set<QName> set = new HashSet<QName>();
                for(RowMap row:rows){
                    Long ntid = row.getLong(Constants.TABLE_TYPE__NODE_TYPE);
                    Long fromid = row.getLong(Constants.TABLE_TYPE__FROM_NODE_TYPE);
                    if (ntid.equals(fromid)){
                        QName name = getNodeTypeManager().getNodeTypeBySQLId(ntid).getQName();
                        set.add(name);
                    }
                }
                QName[] types = new QName[set.size()];
                set.toArray(types);
                //System.out.println(rows);
                try {
                    cachedParentEffectiveNodeType =  ntReg.getEffectiveNodeType(types);
                } catch (NodeTypeConflictException ntce) {
                    String msg = "internal error: failed to build effective node type for node " + safeGetJCRPath();
                    log.debug(msg);
                    throw new RepositoryException(msg, ntce);
                }
                /*Set<QName> set = getMixinTypeNames();
                QName[] types = new QName[set.size() + 1];
                set.toArray(types);
                // primary type
                types[types.length - 1] = getPrimaryTypeName();
                try {
                    cachedParentEffectiveNodeType =  ntReg.getEffectiveNodeType(types);
                } catch (NodeTypeConflictException ntce) {
                    String msg = "internal error: failed to build effective node type for node " + safeGetJCRPath();
                    log.debug(msg);
                    throw new RepositoryException(msg, ntce);
                }*/
                //throw new UnsupportedOperationException();
            } 
            return cachedParentEffectiveNodeType;
        }
        
    }

    /*
     * 

     */
    
    public NodeDefinitionImpl getApplicableChildNodeDefinition(
            QName nodeName, QName nodeTypeName)
            throws ConstraintViolationException, RepositoryException {
        NodeDef cnd = getEffectiveNodeType().getApplicableChildNodeDef(
                nodeName, nodeTypeName, getNodeTypeRegistry());
        return getNodeTypeManager().getNodeDefinition(cnd.getSQLId());
    }    
    
    public void assignSession(_AbstractsStateManager sm) throws ConstraintViolationException, RepositoryException{
    	this.stateManager = sm;
    	this.nodeTypeManager = sm.getNodeTypeManager();
    	alwaysCheckCheckedOut = sm.getRepository().isAlwaysCheckCheckedOut();
    	//load security values
    	 
    	if (sm.isSecurityAllowed() && getWorkspaceId() != null){
        	boolean onlyBrowse = false;
        	boolean read = false;
            try {
                sm.getSecurityManager().checkPermission(SecurityPermission.READ, getACEs() ,getNodeId(), getSecurityId());
                read = true;
            } catch (Exception exc){
                
            }
            if (!read){
            	try {
            	    sm.getSecurityManager().checkPermission(SecurityPermission.BROWSE, getACEs() , getNodeId(),getSecurityId());
            	    onlyBrowse = true;
            	} catch (Exception exc){
            	    
            	}
            }
        	if (!read && !onlyBrowse){
        	}
        	if (!read){
        	    onlyBrowse = true;
        	}
        	
        	if (onlyBrowse && !isNew()){
        	    originalProperties  = new HashMap<QName, _PropertyState>();
        	    originalProperties.putAll(properties);
        	    properties.clear();  
        	}
    	}
    	
    	createDefaultProperties(!browseMode);
    	for(_PropertyState p:properties.values()){
    		p.assignSession(sm);
    	}
    	events.clear();
    }

	public Long getStoreConfigurationId() {
		return storeConfigurationId;
	}

	public void buildInternalPath(_NodeState _parentNode) throws RepositoryException {
		parentState = null;
		_NodeState parentNode = _parentNode == null? getParent(): _parentNode;
		
		String parentPath = parentNode.getInternalPath();
		Long parentDepth = parentNode.getInternalDepthLong();
		buildInternalPath(parentPath, parentDepth);
		
        if (_parentNode != null){
	        for(ParentNode ps:getParentNodes()){
	        	ps.setRemoved();
	        }
	        for(ParentNode ps:parentNode.getParentNodes()){
	        	if (ps.getState() != ItemStatus.Invalidated && ps.getState() != ItemStatus.Destroyed){
	        		addParentNode(new ParentNode(getNodeId(), ps.getParentId(), ps.getPosition().longValue() + 1));
	        	}
	        }
	        addParentNode(new ParentNode(getNodeId(), parentNode.getNodeId(), 1));
        }

        registerModification();

	}
	
	public void buildInternalPath(String parentPath, Long parentDepth) throws RepositoryException {
		
        String uri = name.getNamespaceURI();
        com.exigen.cm.impl.BaseNamespaceRegistryImpl.Namespace ns = null;
        if (uri != null && uri.length() > 0){
            ns = getNamespaceRegistry()._getByURI(uri);
        }

        StringBuffer newpath = new StringBuffer(parentPath);
        newpath.append(PATH_DELIMITER);
        if (ns != null){
            newpath.append(ns.getId());
            newpath.append(":");
        }
        newpath.append(name.getLocalName());
        newpath.append(LEFT_INDEX);
        newpath.append(index);
        newpath.append(RIGHT_INDEX);
        
        if (newpath.length() > 3998){
        	throw new RepositoryException("Node path is too long");
        }

        
        internalPath = newpath.toString();
        internalDepth = parentDepth+1;
        

        setBasePropertiesChanged(true);
        cachedPath = null;
		
	}

	public void setDefinition(NodeDefinitionImpl def) {
		this.definition = def;
	}

	public List<ParentNode> getParentNodes(){
		return parentNodes;
	}

	public void createDefaultProperties(boolean allowMixinType) throws ConstraintViolationException, RepositoryException {
		//check for primary type
		_PropertyState pState = null;
		if (hasProperty(QName.JCR_PRIMARYTYPE)){
			pState = getProperty(QName.JCR_PRIMARYTYPE, true);
		} else {
			PropertyDefinitionImpl def = getApplicablePropertyDefinition(QName.JCR_PRIMARYTYPE, PropertyType.NAME, false);
			pState = new _PropertyState(getRepository(), this, QName.JCR_PRIMARYTYPE, PropertyType.NAME, 
					def.getRequiredType(), def.isMultiple(), (PropDefImpl)def.unwrap(), null);
			addProperty(pState);
			pState.setStatusNormal();
		}
		pState.internalSetValue(new InternalValue[]{InternalValue.create(getPrimaryTypeName())}, PropertyType.NAME, false, false);

		if (allowMixinType){
    		//check for mixins
    		Set<QName> mixins = getMixinTypeNames();
    		if (mixins.size() > 0 ){
    			pState = null;
    			if (hasProperty(QName.JCR_MIXINTYPES)){
    				pState = getProperty(QName.JCR_MIXINTYPES, true);
    			} else {
    				PropertyDefinitionImpl def = getApplicablePropertyDefinition(QName.JCR_MIXINTYPES, PropertyType.NAME, true);
    				pState = new _PropertyState(getRepository(), this, QName.JCR_MIXINTYPES, PropertyType.NAME, 
    						def.getRequiredType(), def.isMultiple(), (PropDefImpl)def.unwrap(), null);
    				addProperty(pState);
    			}
    			
    			pState.internalSetValue(InternalValue.create((QName[]) mixins.toArray(new QName[mixins.size()])), PropertyType.NAME, false, false);
    			pState.setStatusNormal();
    		}
		}
		
	}
	
	public Set<Long> getParentCache(){
		if (_parentCache == null){
    		_parentCache = new HashSet<Long>();
            for(ParentNode p:getParentNodes()){
            	if (!p.getState().equals(ItemStatus.Destroyed) && !p.getState().equals(ItemStatus.Invalidated)){
            		_parentCache.add(p.getParentId());
            	}
            }
    		
    	} 
		return _parentCache;
	}
	
	public Set<Long> getParentCacheWithDeleted(){
		if (_parentCacheAll == null){
			_parentCacheAll = new HashSet<Long>();
            for(ParentNode p:getParentNodes()){
            	_parentCacheAll.add(p.getParentId());
            }
    		
    	} 
		return _parentCacheAll;
	}
	
    public boolean hasParent(Long id) throws RepositoryException {
        return getParentCache().contains(id);
    }

    public boolean hasParentWithDeleted(Long id) throws RepositoryException {
        return getParentCacheWithDeleted().contains(id);
    }

	public List<_PropertyState> getProperties() {
		return new ArrayList<_PropertyState>(properties.values());
	}

	
	public List<_PropertyState> getAllProperties(){
		ArrayList<_PropertyState> result = new ArrayList<_PropertyState>(properties.values());
		result.addAll(this.removedProperties);
		return result;
	}

	public List<NodeReference> getReferences() throws RepositoryException {
		//return this.referencesFrom;
		initReferencesFrom();
		return this._referencesFrom;
		//return getReferencesFrom();
	}

    public _PropertyState getProperty(QName name, boolean checkExistence) throws ItemNotFoundException {
        return getProperty(name, checkExistence, false);
    }

	
	public _PropertyState getProperty(QName name, boolean checkExistence, boolean allowInBrowseMode) throws ItemNotFoundException {
		/*for(_PropertyState st: properties){
			if (st.getName().equals(name)){
				return st;
			}
		}*/
		_PropertyState st = properties.get(name);
		if (st != null){
			return st;
		}
		if (allowInBrowseMode && originalProperties != null){
		    st =originalProperties.get(name);
		    if (st != null){
		        try {
                    st.assignSession(this.stateManager);
                } catch (RepositoryException e) {
                    throw new UnsupportedOperationException();
                }
		        return st;
		    }
		}
		if (checkExistence){
			throw new ItemNotFoundException("Property "+name+" not found");
		} else {
			return null;
		}
	}

	public Long getParentLockId() {
		return parentLockId;
	}

	public boolean isBasePropertiesChanged() {
		return _basePropertiesChanged;	
	}

	public List<SecurityModificationEntry> getModifiedSecurity() {
		//TODO implement this
		return modifiedSecurity;
	}

	public ArrayList<NodeTypeContainer> getAllTypes() {
		return nodeTypes;
	}

	public void increaseVersion() {
		version++;
		
	}

	public void resetToNormal() {
		removedProperties.clear();
		_basePropertiesChanged = false;
		setStatusNormal();
		events.clear();
	}

	public boolean isNodeType(QName ntName) throws RepositoryException {
		if (browseMode){
			if (ntName.equals(getPrimaryNodeType().getName())){
				return true;
			}
			if (!ntName.equals(QName.MIX_REFERENCEABLE )){
				return false;
			}			
		}
		// first do trivial checks without using type hierarchy
		if (ntName.equals(getPrimaryTypeName())) {
			return true;
		}
		if (getMixinTypeNames().contains(ntName)) {
			return true;
		}

		// check effective node type
		return getEffectiveNodeType().includesNodeType(ntName);
	}

	public void removeChildProperty(QName name, boolean setModification) throws RepositoryException {
		_PropertyState p = properties.get(name);
        //for(_PropertyState p:properties){
        //if (p.getName().equals(name)){
            
            if (p.getType() == PropertyType.REFERENCE || p.getType() == PropertyType283.WEAKREFERENCE){
                //unregister reference
                //ArrayList refs = getReferencesTo();
                InternalValue[] values = p.getValues();
                if (values != null){
                    for(int i = 0 ; i < values.length ; i++){
                        InternalValue value = values[i];
                        //UUID uuid = (UUID) value.internalValue();
                        //String uuidS = uuid.toString();
                        //for(Iterator it1 = refs.iterator() ; it1.hasNext();){
                        //    NodeReference ref = (NodeReference) it1.next();
                        //    _NodeImpl to = getNodeManager().buildNode(ref.getToId());
                        //    if (to.getInternalUUID().equals(uuidS) && ref.getPropertyQName().equals(name2)){
                        //        ref.setRemoved();
                        //        to.registerReferenceRemove(ref);
                        //        //TODO unregister on referenced node
                        //    }
                        //}
                        removeReference(p, value);
                    }
                }

            }

            properties.remove(p.getName());
            p.setStatusInvalidated();
            if (setModification){
                removedProperties.add(p);
            	registerModification();
            }
            //break;
        //}
    //}
	}
	
    public NodeReference removeReference(_PropertyState prop, InternalValue value) throws RepositoryException {
    	if (workspaceId != null){
//    		TODO optimize read Ahead
    		if (log.isDebugEnabled()){
    			log.debug("Remove reference for property "+prop.getName()+" in node "+getPrimaryPath()+" ("+getNodeId()+")");
    		}
	        ArrayList<NodeReference> refs = getReferencesTo();
	        UUID uuid = (UUID) value.internalValue();
	        String uuidS = uuid.toString();
	        for(NodeReference ref : refs){
	            if (!ref.getState().equals(ItemStatus.Invalidated) && ref.getPropertyQName().equals(prop.getName())){
	            	//if (ref.getToId() != null){
		                //_NodeState to = stateManager.getNodeState(ref.getToId(), null, true);
		                if (ref.getUUID().equals(uuidS)){
		                    ref.setRemoved();
		                    if (ref.getToId() != null) {
		                    	//build referenced node cache
	                    		_NodeState to = stateManager.getNodeState(ref.getToId(), null, true, ref.getToId().toString());
	                    		to.registerReferenceRemove(ref);
		                    }
		                    return ref;
		                }
	            	//}
	            }
	        }
	        
	        throw new RepositoryException("Reference not found");
    	} else {
    		return null;
    	}
    }
    
    public void registerReferenceRemove(NodeReference ref) {
        tmpReferencesFromRemoved.add(ref);
        
    }    
    public void unregisterReferenceRemove(NodeReference ref) {
        tmpReferencesFromRemoved.remove(ref);
        
    }    

    public void registerTmpRefeference(NodeReference ref) {
		if (ref != null){
			tmpReferencesFromAdd.add(ref);
		} 
    }
    
    public void unregisterTmpRefeference(NodeReference ref) {
			tmpReferencesFromAdd.remove(ref);
    }


	public void registerModification() {
        if (getStatus().equals(ItemStatus.New) || getStatus().equals(ItemStatus.Modified)){
            //do nothing
        } else if (getStatus().equals(ItemStatus.Normal)){
            setStatusModified();
            stateManager.registerModifiedState(this);
        } else {
            throw new UnsupportedOperationException("Unknown state");
        }	
    }

	public void addReferencesTo(NodeReference reference) throws RepositoryException {
		if (workspaceId  != null){
			getReferencesTo().add(reference);
		}
	}	
	
	public void registerReferencesTo(NodeReference reference) {
		if (workspaceId  != null){
			//be sure that referenceas are initialized
			_referencesTo.add(reference);
		}
	}	
	
	public void addReferencesFrom(NodeReference reference) throws RepositoryException {
		initReferencesFrom();
		_referencesFrom.add(reference);
}	
	public void registerReferencesFrom(NodeReference reference) {
		
		_referencesFrom.add(reference);
}	
    public void registerPermanentRefeference(NodeReference nr) {
	        if (tmpReferencesFromAdd.contains(nr)){
	            tmpReferencesFromAdd.remove(nr);
	        }
	        if (_referencesFrom != null){
	            _referencesFrom.add(nr);
	        }
    }    
    

    public void registerPermanentRefeferenceRemove(NodeReference nr) {
	        if (tmpReferencesFromRemoved.contains(nr)){
	            tmpReferencesFromRemoved.remove(nr);
	        }
	        if (_referencesFrom != null){
	            _referencesFrom.remove(nr);
	        }
    }	
	
    public NodeReference registerReference(_PropertyState prop, InternalValue v, _NodeState refTo) throws RepositoryException {
    	if (workspaceId == null && refTo.getWorkspaceId() != null){
    		//System.out.println(getInternalPath()+" -> "+refTo.getInternalPath());
    		return null;
    	}
    	/*if (w1 == null && w2 == null){
    		System.out.println(getInternalPath()+" -> "+refTo.getInternalPath());
    	}*/
		if (workspaceId  != null){
	        Long nextId = stateManager.nextId();
	
	        ArrayList<NodeReference> refs = getReferencesTo();
	        NodeReference ref = new NodeReference(getNamespaceRegistry(), nextId, this, refTo, prop.getName(), refTo.getInternalUUID());
	        refs.add(ref);
	        return ref;
		}
		//System.out.println(getInternalPath()+" -> "+refTo.getInternalPath());
		return null;
    }

	public synchronized ArrayList<NodeReference> getReferencesTo() throws RepositoryException {
		if (!isReferencesToLoaded()){
			loadReferencesTo();
		}
		return _referencesTo;
	}
	
	public boolean isReferencesToLoaded(){
		return _referencesTo != null;
	}

    private void loadReferencesTo() throws RepositoryException {
    	this._referencesTo = new ArrayList<NodeReference>();
    	this.stateManager.loadToReferences(this);
	}
    
    
	public synchronized ArrayList<NodeReference> getReferencesFrom() throws RepositoryException {
		if (!isReferencesFromLoaded()){
			loadReferencesFrom();
		}
        ArrayList<NodeReference> result = new ArrayList<NodeReference>(_referencesFrom);
        result.addAll(tmpReferencesFromAdd);
        result.removeAll(tmpReferencesFromRemoved);
		return result;
	}
    
	public boolean isReferencesFromLoaded(){
		return _referencesFrom != null;
	}

    private void loadReferencesFrom() throws RepositoryException {
    	this._referencesFrom = new ArrayList<NodeReference>();
    	this.stateManager.loadFromReferences(this);
	}
    
    private void initReferencesFrom() throws RepositoryException {
		if (!isReferencesFromLoaded()){
			loadReferencesFrom();
		}
	}
    

	public boolean hasOverlayedState() {
        return overlayedState != null;
    }

    public NodeOverlayedState getOverlayedState() {
        return overlayedState;
    }

	public NodeId getNodeItemId() throws ItemNotFoundException {
		return new NodeId(getNodeId(), getInternalUUID());
	}
	
    public void accept(_ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }	
    
    public boolean isNodeTypeRegistered(QName ntName) {
		for(NodeTypeContainer nt:nodeTypes){
			if (nt.getName().equals(ntName) && nt.isEffective()){
				return true;
			}
		}
		return false;
	}

	
    public void updateName(QName destName, Long destIndex, _NodeState newParentNode, NodeModification nm, boolean rename) throws RepositoryException {
        nm.registerNodeModification(this);
        
        //test
        //SessionImpl session = ge;
        if (rename){
	        fireRemoveEvent(false);
        }
 	    
        //*/
        
        
        if (destName != null){
            this.name = destName;
        }
        if (destIndex != null){
            this.index = destIndex;
        }
        if (newParentNode != null){
        	
            if (parentId == null || newParentNode.getNodeId().longValue() != parentId.longValue()){
                ArrayList<Long> nodes = new ArrayList<Long>();
                if (getRepository().getSecurityCopyType() == SecurityCopyType.Copy){
                    List<SecurityEntry> parentACE = stateManager.getSecurityManager().getSecurityEntries(newParentNode, false);
                    prepareSecurityOnMove(nodes, newParentNode.getSecurityId(), parentACE);
                } else if (getRepository().getSecurityCopyType() == SecurityCopyType.Inherit){
                    if (getSecurityId().equals(getNodeId())){
                        //remove old aces  
                        //Long securityId = getSecurityId();
                        modifiedSecurity.add(new SecurityModificationEntry(getNodeId(), SecurityModificationEntry.RESET, SecurityPrincipal.user(null), null, null, null, null, false));
                        
                    } 
                    this.securityId = newParentNode.getSecurityId();
                } else {
                	throw new UnsupportedOperationException("Unsupported security copy type");
                }
                
            }
            nm.registerNodeModification(newParentNode);
            nm.registerNodeModification(this.getParent());
            newParentNode.registerModification();
            getParent().registerModification();
            this.parentId = newParentNode.getNodeId();
            
            
        }
        // 
        
        buildInternalPath(newParentNode);
        cachedPath = null;
        
        registerModification();
        setBasePropertiesChanged(true);
        
        for(_NodeState node:stateManager.getNodesWithName(this, null, false)){
            node.updateName(null, null, this, nm, rename);
        }   
        _parentCache = null;
        _parentCacheAll = null;
        
        ChildMaxPosition max = stateManager.getMaxChildPos(getParent(), name);
        setSnsMax(max.getMax());
        
        if (newParentNode != null){
            newParentNode.registerChild(new IndexedQname(getName(), getIndex()), this.getNodeId());
        }
        
        if (rename){
	        fireAddEvent(false);

        }
    }

	public void fireAddEvent(boolean fireChildNodes) throws RepositoryException, UnsupportedRepositoryOperationException {
		_NodeState parent = getParent();
		NodeTypeImpl nodeType = EventStateCollection.getNodeType(parent, getNodeTypeManager());
		Set<QName> mixins = parent.getMixinTypeNames();
		Path path = getPrimaryPath();
		//String ddd = getUserId();

		EventState event = EventState.childNodeAdded(new NodeId(parent.getNodeId(), parent.getInternalUUID()),
				EventStateCollection.getParent(path),
		        new NodeId(getNodeId(), getInternalUUID()),
		        path.getNameElement(),
		        nodeType,
		        mixins,
		        getInternalUUID(), 
		        stateManager.getSessionInfo());
		events.add(event);
		
		if (fireChildNodes){
	        for(_NodeState node:stateManager.getNodesWithName(this, null, false)){
	            node.fireAddEvent(fireChildNodes);
	        }   
		}
	}

	public void fireRemoveEvent(boolean fireChildNodes) throws RepositoryException, UnsupportedRepositoryOperationException {
		_NodeState parent = getParent();
		NodeTypeImpl nodeType = EventStateCollection.getNodeType(parent, getNodeTypeManager());
		Set<QName> mixins = parent.getMixinTypeNames();
		Path path = getPrimaryPath();
		//String ddd = getUserId();
		EventState event = EventState.childNodeRemoved(new NodeId(parent.getNodeId(), parent.getInternalUUID()),
				EventStateCollection.getParent(path),
		        new NodeId(getNodeId(), getInternalUUID()),
		        path.getNameElement(),
		        nodeType,
		        mixins,
		        getInternalUUID(), 
		        stateManager.getSessionInfo());
		events.add(event);
		//parentId, parentPath, childId, childPath, nodeType, mixins, parentUUID, info)
		if (fireChildNodes){
	        for(_NodeState node:stateManager.getNodesWithName(this, null, false)){
	            node.fireRemoveEvent(fireChildNodes);
	        }   
		}

	}

    

	public void onRemove(NodeTreeItem tree) throws RepositoryException {
        // modify the state of 'this', i.e. the target node
        //NodeState thisState = (NodeState) getOrCreateTransientItemState();
        
		NodeTreeItem active = tree.find(getNodeId(),(long) (getDepth()-1));
        if (active.getChilds().size() > 0) {//stateManager.hasChildNodes(this)
            // remove child nodes
            // use temp array to avoid ConcurrentModificationException
        	
        	
        	//List<Long> batch = active.getBatch();
        	/*ArrayList<Long> tmp = new ArrayList<Long>();
        	for(Long id:stateManager.getChildNodesId(this, false, null)){
        		tmp.add(id);
        	}*/
        	Collection<NodeTreeItem> childs = active.getChilds();
        	
            
            // remove from tail to avoid problems with same-name siblings
            /*for (int i = tmp.size() - 1; i >= 0; i--) {
                _NodeState childNode = stateManager.getNodeState((Long)tmp.get(i), tmp, true);
                childNode.onRemove(tree);
                // remove the child node entry
                //thisState.removeChildNodeEntry(entry.getName(), entry.getIndex());
                childNode.regesterRemoveNode();
            }*/
        	for (NodeTreeItem i:childs) {
                _NodeState childNode = stateManager.getNodeState(i.getId(), i.getBatch(), true, i.getId().toString());
                childNode.onRemove(tree);
                // remove the child node entry
                //thisState.removeChildNodeEntry(entry.getName(), entry.getIndex());
                childNode.regesterRemoveNode();
            }
        }

        // remove properties
        ArrayList<_PropertyState> tmp = new ArrayList<_PropertyState>(properties.values());
        for(_PropertyState p:tmp){
        	if (p.getType() == PropertyType.REFERENCE || p.getType() == PropertyType283.WEAKREFERENCE || p.getDefinition().isUnstructured()){
        		removeChildProperty(p.getName(), true);
        	}
        }
        
        
        // use temp set to avoid ConcurrentModificationException
//        HashSet tmp = new HashSet(getProperties());
//        for (Iterator iter = tmp.iterator(); iter.hasNext();) {
            /*QName propName = (QName) iter.next();
            // remove the property entry
            thisState.removePropertyName(propName);
            // remove property
            PropertyId propId = new PropertyId(thisState.getUUID(), propName);
            itemMgr.getItem(propId).setRemoved();*/
//            Property prop = (Property) iter.next();
            
//        }
        for(_PropertyState p : new ArrayList<_PropertyState>(properties.values()) ){
            //skip jcr:primaryType
            if (!p.getName().equals(QName.JCR_PRIMARYTYPE) && !p.getName().equals(QName.JCR_MIXINTYPES)
                    && !p.getName().equals(QName.JCR_UUID)){
                removeChildProperty(p.getName(), true);
            }
        }

        // finally remove this node
        //itemMgr.getItem(id).setRemoved();
        
        //1. unregister types
        for(NodeTypeContainer ntc: getAllTypes()){
            ntc.setRemoved();
        }
        //2. unregister parents
        for(ParentNode pn : getParentNodes()){
            pn.setRemoved();
        }
        //3. references
        /*for(Iterator it = getReferencesTo().iterator() ; it.hasNext()){
            ParentNode pn = parentNodes[i];
            pn.setRemoved();
        }*/
        
        // has references ?
/*        if (getReferences().size() > 0){
            //TODO show references nodes nodes
            for(Iterator it = getReferencesFrom().iterator() ; it.hasNext();){
                NodeReference nr = (NodeReference) it.next();
                
            }
            throw new RepositoryException("Other nodes references to this node("+toString()+"), cannot be removed, session become invalid !!!");
        }*/
        
        
        regesterRemoveNode();

    }
    
    public void regesterRemoveNode() throws RepositoryException {
        stateManager.registerModifiedState(this);
        setStatusInvalidated();
    }

    
    public _PropertyState internalSetProperty(QName name, InternalValue value,
			boolean setModification, boolean triggerEvents, boolean allowInBrowseMode)
			throws ValueFormatException, RepositoryException {
        if (browseMode && !allowInBrowseMode){
            throw new AccessDeniedException();
        }
		int type;
		if (value == null) {
			type = PropertyType.UNDEFINED;
		} else {
			type = value.getType();
		}

		BitSet status = new BitSet();
		_PropertyState prop = getOrCreateProperty(name, type, false, status, null, allowInBrowseMode);
		try {
			if (value == null) {
				prop.internalSetValue(null, type, setModification,
						triggerEvents);
			} else {
				prop.internalSetValue(new InternalValue[] { value }, type,
						setModification, triggerEvents);
				registerModification();
			}
		} catch (RepositoryException re) {
			removeChildProperty(name, setModification);
			throw re;
		}
		return prop;
	}
    
    public _PropertyState internalSetProperty(QName name,
			InternalValue[] values, int type, boolean setModified)
			throws ValueFormatException, RepositoryException {
        if (browseMode){
            throw new AccessDeniedException();
        }

		BitSet status = new BitSet();
		_PropertyState prop = getOrCreateProperty(name, type, true, status, null, false);
		try {
			prop.internalSetValue(values, type, setModified, true);
			registerModification();
		} catch (RepositoryException re) {
			removeChildProperty(name, setModified);
			throw re;
		}
		return prop;
	}    
    
    protected synchronized _PropertyState getOrCreateProperty(QName name,
			int type, boolean multiValued, BitSet status,
			Long unstructuredPropertyId, boolean allowInBrowseMode) throws ConstraintViolationException,
			RepositoryException {
		status.clear();

		if (hasProperty(name, allowInBrowseMode)) {
			return getProperty(name, true, allowInBrowseMode);
		}

		// does not exist yet:
		// find definition for the specified property and create property
		PropertyDefinitionImpl def = getApplicablePropertyDefinition(
				name, type, multiValued);
		_PropertyState prop = createChildProperty(name, type, def,
				unstructuredPropertyId);
		return prop;
	}
    
    protected synchronized _PropertyState createChildProperty(QName name,
			int type, PropertyDefinitionImpl def, Long unstructuredPropertyId)
			throws RepositoryException {

		
		/*if (stateManager.hasChildNode(this, name, false)) {
			String msg = "there's already a child node with name " + name;
			log.debug(msg);
			throw new RepositoryException(msg);
		}*/

		
		_PropertyState result = new _PropertyState(getRepository(), this,
				name, type, def.getRequiredType(), def.isMultiple(),
				(PropDefImpl) def.unwrap(), unstructuredPropertyId);
		addProperty(result);
		
		InternalValue[] genValues = computeSystemGeneratedPropertyValues(name,
				def);
		InternalValue[] defValues = def.unwrap().getDefaultValues();
		if (genValues != null && genValues.length > 0) {
			result.setValues(genValues);
		} else if (defValues != null && defValues.length > 0) {
			result.setValues(defValues);
		} else if (def.isAutoCreated()) {
			throw new RepositoryException(
					"Default values for autocreated property " + name
							+ " not defined");
		}

		
		return result;
	}
    
    public InternalValue[] computeSystemGeneratedPropertyValues(QName name,
			PropertyDefinitionImpl def) throws RepositoryException {
		InternalValue[] genValues = null;

		/**
		 * todo: need to come up with some callback mechanism for applying
		 * system generated values (e.g. using a NodeTypeInstanceHandler
		 * interface)
		 */

		// NodeState thisState = (NodeState) state;
		// compute system generated values
		NodeTypeImpl nt = (NodeTypeImpl) def.getDeclaringNodeType();
		if (nt.getQName().equals(QName.MIX_REFERENCEABLE)) {
			// mix:referenceable node type
			if (name.equals(QName.JCR_UUID)) {
				// TODO register UUID in NodeManager
				// jcr:uuid property
				genValues = new InternalValue[] { InternalValue
						.create(getRepository().generateUUID().toString()) };
			}

			// todo consolidate version history creation code (currently in
			// ItemImpl.initVersionHistories)
		} // else if (nt.getQName().equals(QName.MIX_VERSIONABLE)) {
		// mix:versionable node type
		// VersionHistory hist =
		// session.getVersionManager().getOrCreateVersionHistory(this);
		// if (name.equals(QName.JCR_VERSIONHISTORY)) {
		// // jcr:versionHistory property
		// genValues = new InternalValue[]{InternalValue.create(new
		// UUID(hist.getUUID()))};
		// } else if (name.equals(QName.JCR_BASEVERSION)) {
		// // jcr:baseVersion property
		// genValues = new InternalValue[]{InternalValue.create(new
		// UUID(hist.getRootVersion().getUUID()))};
		// } else if (name.equals(QName.JCR_ISCHECKEDOUT)) {
		// / // jcr:isCheckedOut property
		// genValues = new InternalValue[]{InternalValue.create(true)};
		// } else if (name.equals(QName.JCR_PREDECESSORS)) {
		// // jcr:predecessors property
		// genValues = new InternalValue[]{InternalValue.create(new
		// UUID(hist.getRootVersion().getUUID()))};
		// }

		// }
		else if (nt.getQName().equals(QName.NT_HIERARCHYNODE)) {
			// nt:hierarchyNode node type
			if (name.equals(QName.JCR_CREATED)) {
				// jcr:created property
				genValues = new InternalValue[] { InternalValue.create(Calendar
						.getInstance()) };
			}
		} else if (nt.getQName().equals(QName.NT_RESOURCE)) {
			// nt:resource node type
			if (name.equals(QName.JCR_LASTMODIFIED)) {
				// jcr:lastModified property
				genValues = new InternalValue[] { InternalValue.create(Calendar
						.getInstance()) };
			}
		} else if (nt.getQName().equals(QName.NT_VERSION)) {
			// nt:version node type
			if (name.equals(QName.JCR_CREATED)) {
				// jcr:created property
				genValues = new InternalValue[] { InternalValue.create(Calendar
						.getInstance()) };
			}
		} else if (nt.getQName().equals(QName.NT_BASE)) {
			// nt:base node type
			if (name.equals(QName.JCR_PRIMARYTYPE)) {
				// jcr:primaryType property
				genValues = new InternalValue[] { InternalValue.create(getPrimaryTypeName()) };
			} else if (name.equals(QName.JCR_MIXINTYPES)) {
				// jcr:mixinTypes property
				Set<QName> mixins = getMixinTypeNames();
				ArrayList<InternalValue> values = new ArrayList<InternalValue>(
						mixins.size());
				Iterator<QName> iter = mixins.iterator();
				while (iter.hasNext()) {
					values.add(InternalValue.create((QName) iter.next()));
				}
				genValues = values.toArray(new InternalValue[values.size()]);
			}
		} else if (nt.getQName().equals(Constants.ECR_OCR_MIXIN)) {
			// nt:version node type
			if (name.equals(Constants.ECR_OCR_MIXIN__USER_ID)) {
				// jcr:created property
				genValues = new InternalValue[] { InternalValue.create(getUserId()) };
			}
		} else if (def.getRequiredType() == PropertyType.DATE
				&& def.isMandatory() && def.isAutoCreated()
				&& !def.isMultiple()) {
			genValues = new InternalValue[] { InternalValue.create(Calendar
					.getInstance()) };
		}

		return genValues;
	}

	public Collection<QName> getPropertyNames() {
		ArrayList<QName> result = new ArrayList<QName>(properties.size());
		for(_PropertyState s: properties.values()){
			result.add(s.getName());
		}
		return result;
	}

	public void registerNodeType(NodeTypeContainer nt) {
		this.nodeTypes.add(nt);
		this.cachedEffectiveNodeType = null;
		effectiveNodeTypes = null;
		effectiveMixinTypes = null;
	}
	
    public void unregisterNodeType(QName nodeTypeName) throws RepositoryException {
        NodeTypeImpl nt = getNodeTypeManager().getNodeType(nodeTypeName);
        
        for(NodeTypeContainer ntc : getAllTypes()){
            if (ntc.getFromTypeId().equals(nt.getSQLId())){
                ntc.setRemoved();
            }
        }
        this.effectiveNodeTypes = null;
        effectiveMixinTypes = null;
        this.cachedEffectiveNodeType = null;
        
        
    }

    private void prepareSecurityOnMove(ArrayList<Long> _nodes, Long parentSecurityId, List<SecurityEntry> parentACE) throws RepositoryException{
        ArrayList<Long> nodes = new ArrayList<Long>(_nodes);
        if (getNodeId().longValue() == getSecurityId().longValue()){
            nodes.add(getNodeId());
            modifiedSecurity.add(new SecurityModificationEntry(getNodeId(), SecurityModificationEntry.RESET, SecurityPrincipal.user(null), null, null, null, null, false));
            List<SecurityEntry> acl = new ArrayList<SecurityEntry>(parentACE);
            for(SecurityEntry ace : acl){
                for(BaseSecurityPermission p: getStateManager().getSecurityManager().getAllPermissions()){
                    Boolean value = ace.getPermission(p);
                        SecurityModificationEntry sme = new SecurityModificationEntry(getNodeId(), SecurityModificationEntry.SET_PERMISSION,
                                ace.getPrincipalEntry(), p, value, value != null ? ace.getPermissionParentId(p):null, value != null ? ace.getPermissionFromAsString(p):null,
                                		value != null ? ace.isDirectPermission(p):false);
                        modifiedSecurity.add(sme);
                }
            }
            acl = stateManager.getSecurityManager().getSecurityEntries(this, true);
            for(SecurityEntry ace : acl){
                for(BaseSecurityPermission p: getStateManager().getSecurityManager().getAllPermissions()){
                    Boolean value = ace.getPermission(p);
                    if (value != null && nodes.contains(ace.getPermissionParentId(p))){
                        SecurityModificationEntry sme = new SecurityModificationEntry(getNodeId(), SecurityModificationEntry.SET_PERMISSION,
                                ace.getPrincipalEntry(), p, value, value != null ? ace.getPermissionParentId(p):null, value != null ? ace.getPermissionFromAsString(p):null,
                                		value != null ? ace.isDirectPermission(p):false);
                        modifiedSecurity.add(sme);
                    }
                }
            }
            registerModification();
        } else if (!nodes.contains(this.securityId)){
            this.securityId = parentSecurityId;
            registerModification();
            setBasePropertiesChanged(true);
        }
        for(_NodeState node:stateManager.getNodesWithName(this, null, false)){
            node.prepareSecurityOnMove(nodes, this.securityId, parentACE);
        } 
    }

	public void setBasePropertiesChanged(boolean basePropertiesChanged) throws AccessDeniedException {
        if (browseMode){
            throw new AccessDeniedException();
        }

		this._basePropertiesChanged = basePropertiesChanged;
	}

	
    public void applySecurityOnMove(List<SecurityEntry> parentACL, ArrayList<Long> parents, Long parentSecurityId, boolean moveSecurity) throws RepositoryException {
        ArrayList<Long> _parents = new ArrayList<Long>(parents);
        _parents.add(getNodeId());

        if (!getNodeId().equals(securityId)){
            if (!_parents.contains(securityId)){
                this.securityId = parentSecurityId;
            }
        } else {//merge security with parent security
            if (moveSecurity){
                getACEList();
                List<SecurityEntry> _parentACL = new ArrayList<SecurityEntry>(parentACL);
                for(SecurityEntry ace:acl){
                    SecurityEntry parentACE  = null;
                    for(SecurityEntry s:_parentACL){
                        if ( (s.isUserEntry() && s.getUserId().equals(ace.getUserId())) ||
                              (s.isGroupEntry() && s.getGroupId().equals(ace.getGroupId())) ){
                            parentACE = s;
                            break;
                        }
                    }
                    if (parentACE != null){
                        _parentACL.remove(parentACE);
                    }
                    for(BaseSecurityPermission p:getStateManager().getSecurityManager().getAllPermissions()){
                        boolean neetToChange = false;
                        if (ace.getPermission(p) != null){
                            if (!_parents.contains(ace.getPermissionParentId(p))){
                                neetToChange = true;
                            }
                        } else {
                            neetToChange = true;
                        }
                        if (neetToChange){
                            if (parentACE == null || parentACE.getPermission(p) == null){
                                SecurityModificationEntry sme = new SecurityModificationEntry(getNodeId(),SecurityModificationEntry.SET_PERMISSION, ace.getPrincipalEntry(), p, null, null, null , false);
                                modifiedSecurity.add(sme);
                            } else {
                                SecurityModificationEntry sme = new SecurityModificationEntry(getNodeId(),SecurityModificationEntry.SET_PERMISSION, ace.getPrincipalEntry(), p, 
                                		parentACE.getPermission(p), parentACE.getPermissionParentId(p), parentACE.getPermissionFromAsString(p), parentACE.isDirectPermission(p) );
                                modifiedSecurity.add(sme);
                            } 
                        }
                    }
                }
                for(SecurityEntry ace:_parentACL){
                    for(BaseSecurityPermission p:getStateManager().getSecurityManager().getAllPermissions()){
                        if (ace.getPermission(p) != null){
                            SecurityModificationEntry sme = new SecurityModificationEntry(getNodeId(),SecurityModificationEntry.SET_PERMISSION, ace.getPrincipalEntry(), 
                            		p, ace.getPermission(p), ace.getPermissionParentId(p), ace.getPermissionFromAsString(p), ace.isDirectPermission(p) );
                            modifiedSecurity.add(sme);
                        }
                    }
                }
            } else {
                //remove old ace
                if (securityId.equals(getNodeId())){
                    SecurityModificationEntry sme = new SecurityModificationEntry(getNodeId(),SecurityModificationEntry.RESET, SecurityPrincipal.user(null), null, null, null, null, false);
                    modifiedSecurity.add(sme);
                } else {
                    //do nothing
                }
                //aply new security id
                this.securityId = parentSecurityId;
                
            }
            
        }
        
        for(_NodeState ns:stateManager.getNodesWithName(this, null, false)){
            ns.applySecurityOnMove(parentACL, _parents, parentSecurityId, moveSecurity);
        } 
        registerModification();

            
    }
    
    public List<SecurityEntry> getACEList() throws RepositoryException {
        if (acl == null){
            acl = getRepository().getSecurityManager().getNodeACL(getSecurityId(), stateManager.getConnection(), stateManager);
        }
        //apply modified ACE's
        for(SecurityModificationEntry sme:getModifiedSecurity()){
        	boolean found = false;
        	for(SecurityEntry e:acl){
        		if ( (e.getUserId() != null && e.getUserId().equals(sme.getUserId()))
        				||
        				(e.getGroupId() != null && e.getGroupId().equals(sme.getGroupId()))	
        				){
        			e.modifyValue(sme.getPermission(),sme.getValue(), sme.getValueParent());
        			found = true;
        		}
        	}
        	if (!found){
        		//create new ACE
        		if (sme.getPrincipal().getName() != null){
	        		SecurityEntry e = new SecurityEntry(sme, stateManager);
	        		acl.add(e);
        		}
        	}
        }
        return acl;
    }


	

	public void resetReferencesFrom() {
		if (isReferencesFromLoaded()){
			_referencesFrom.clear();
		}
		
	}

	public _AbstractsStateManager getStateManager() {
		return stateManager;
	}

	public Long getSnsMax() {
		return snsMax;
	}

	public void setSnsMax(Long snsMax) throws RepositoryException {
		Long old = this.snsMax;
		if (snsMax != null && snsMax.equals(old)){
			return;
		}
		this.snsMax = snsMax;
		//if (snsMax == null || old == null || old.longValue() != snsMax.longValue()){
		setBasePropertiesChanged(true);
			if (parentId != null){
				if (stateManager != null){
					_NodeState p = stateManager.getNodeState(parentId, null,  false, parentId.toString());
					if (p != null && p.getStatus() != ItemStatus.New){
						p.cachedChildNodes.clear();
						//stateManager.findChildsInCache();
					}
				}
			}
		//}
		
	}
	
	 private SoftHashMap<String, Path> relPathCahce = new SoftHashMap<String, Path>();

	public Path compilePath(String relPath) throws MalformedPathException {
		Path result = relPathCahce.get(relPath);
		if (result == null){
			result =  Path.create(relPath, stateManager.getNamespaceResolver(), false);
			relPathCahce.put(relPath, result);
		}
		return result;
	}

	
	HashMap<String, QName> propnameCahche = new HashMap<String, QName>();

	private JCRTransaction createInTransaction;

    public boolean denyAccess = false;

    private HashMap<String, Object> lockOptions;

    public boolean isOnlyNeNode;
	
	public PropertyId resolveRelativePropertyPath(String relPath)
	    throws RepositoryException {
		try {
		    /**
		     * first check if relPath is just a name (in which case we don't
		     * have to build & resolve absolute path)
		     */
		    if (relPath.indexOf('/') == -1) {
		    	QName propName = propnameCahche.get(relPath);
		    	if (propName == null){
		    		propName = stateManager.getNamespaceResolver().getQName(relPath);
		    		//propName = QName.fromJCRName(relPath, stateManager.getNamespaceResolver());
		    		propnameCahche.put(relPath, propName);
		    	}
		        if (hasProperty(propName)) {
		            return new PropertyId(getNodeId(), propName);
		        } else {
		            return null;
		        }
		    }
		    throw new RepositoryException();
		} catch (BaseException e) {
		    String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
		    log.debug(msg);
		    throw new RepositoryException(msg, e);
		}
	}


	public void clearCachedChildNames() {
		cachedChildNodes.clear();
		
	}	
	
    protected String getUserId() {
    	return stateManager.getPrincipals().getUserId();
	}
	
	public Collection<EventState> getEvents(){
		return events;
	}
	
	@Override
	public String toString(){
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("path",getInternalPath());
		ts.append("id",getNodeId());
		ts.append("index",getIndex());
		return ts.toString();
	}



	public String getParentLockOwner() {
		
		return lockOwner;
	}

	public void setLockOwner(String lockOwner) {
		this.lockOwner = lockOwner;
	}

	public void setCreatedInTransaction(JCRTransaction currentTransaction) {
		this.createInTransaction = currentTransaction;
		
	}

	public JCRTransaction getCreateInTransaction() {
		return createInTransaction;
	}

    public void checkBrowseMode() throws javax.jcr.AccessDeniedException{
        if (browseMode || denyAccess){
            throw new AccessDeniedException("You have no permission for this operation");
        }
    }

    public void checkDenyAccess() throws javax.jcr.AccessDeniedException{
        if (denyAccess){
            throw new AccessDeniedException("You have no permission for this operation");
        }
    }

    public void addACE(RowMap row) {
        aces.add(row);
    }

	public void addAllAces(List<RowMap> es) {
		aces.addAll(es);
	}

    
    public List<RowMap> getACEs() {
        return aces;
    }

    public void setLockOptions(HashMap<String, Object> lockOptions) {
        this.lockOptions = lockOptions;
    }
    
    public HashMap<String, Object> getLockOptions(){
        return lockOptions;
    }

    
    Boolean checkedOut = null;

	private boolean alwaysCheckCheckedOut = true;
    
    public boolean internalIsCheckedOut(boolean enforceCheck, DatabaseConnection conn) throws RepositoryException {
    	if (!enforceCheck && checkedOut != null && !alwaysCheckCheckedOut ){
    		return checkedOut;
    	}
    	
        /**
         * try shortcut first:
         * if current node is 'new' we can safely consider it checked-out
         * since otherwise it would had been impossible to add it in the first
         * place
         */
        if (isNew()) {
            return true;
        }

        // search nearest ancestor that is versionable
        /**
         * FIXME should not only rely on existence of jcr:isCheckedOut property
         * but also verify that node.isNodeType("mix:versionable")==true;
         * this would have a negative impact on performance though...
         */
        //build sql statemtn to check all parent for checking out
        
        if (hasProperty(QName.JCR_ISCHECKEDOUT)){
        	checkedOut = (Boolean)getProperty(QName.JCR_ISCHECKEDOUT, true).getValues()[0].internalValue();
            return checkedOut;
        }

        if (getDepth() == 0){
        	checkedOut = true;
            return true;
        }
        
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(stateManager.getVersionableTableName(), true);
        st.addResultColumn(stateManager.getVersionableIsCheckedOutColumnName()+" as value");
        
        //st.addCondition(Conditions.eq(ntDef.getPresenceColumn(), true));
        st.addCondition(Conditions.eq(stateManager.getVersionableIsCheckedOutColumnName(), true));
        
        //add all parents
        DatabaseSelectAllStatement parent = DatabaseTools.createSelectAllStatement(TABLE_NODE_PARENT, false);
        parent.addCondition(Conditions.eq(FIELD_TYPE_ID, getNodeId()));
        parent.addResultColumn(TABLE_NODE_PARENT__PARENT_ID);

        parent.execute(conn);
        parent.getAllRows();
        
        st.addCondition(Conditions.in(Constants.FIELD_TYPE_ID, parent));
        
        //add cm_node relation
        st.addJoin(Constants.TABLE_NODE, "NODE", Constants.FIELD_TYPE_ID, Constants.FIELD_ID);
        st.addResultColumn("NODE."+Constants.TABLE_NODE__NODE_DEPTH+" as depth");

        
        st.execute(conn);
        List<RowMap> all = st.getAllRows();
        Boolean result = null;
        int depth = -1;
        
        if (all.size() == 0){
        	checkedOut = true;
            return true;
        }
        for(RowMap r:all){
            if (r.getBoolean("VALUE")!= null){
                Long d = r.getLong("DEPTH");
                if (d > depth){
                    result = r.getBoolean("VALUE"); 
                }
                
            }
            
        }
        
        //return total > 0 ? true:false;
        checkedOut = result == null ? true : result;
        return result == null ? true : result;
        
        /*_NodeImpl node = this;
        while (!node.hasProperty(QName.JCR_ISCHECKEDOUT)) {
            if (node.getDepth() == 0) {
                return true;
            }
            node = (_NodeImpl) node._getParent();
        }
        return node.getProperty(QName.JCR_ISCHECKEDOUT).getBoolean();*/
    }

	public void resetCheckedOut() {
		checkedOut = null;
		
	}

}
