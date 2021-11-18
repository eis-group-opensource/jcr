/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import static com.exigen.cm.Constants.DEFAULT_ROOT_ALIAS;
import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_ACE;
import static com.exigen.cm.Constants.TABLE_ACE2;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION;
import static com.exigen.cm.Constants.TABLE_ACE_RESTRICTION__ACE_ID;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID;
import static com.exigen.cm.Constants.TABLE_NODE_REFERENCE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE;
import static com.exigen.cm.Constants.TABLE_NODE_UNSTRUCTURED__PROP_DEF;
import static com.exigen.cm.Constants.TABLE_NODE__INDEX;
import static com.exigen.cm.Constants.TABLE_NODE__INDEX_MAX;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_DEPTH;
import static com.exigen.cm.Constants.TABLE_NODE__NODE_PATH;
import static com.exigen.cm.Constants.TABLE_NODE__PARENT;
import static com.exigen.cm.Constants.TABLE_NODE__SECURITY_ID;
import static com.exigen.cm.Constants.TABLE_NODE__VERSION_;
import static com.exigen.cm.Constants.TABLE_NODE__WORKSPACE_ID;
import static com.exigen.cm.Constants.TABLE_TYPE;
import static com.exigen.cm.Constants.TABLE_TYPE__NODE_TYPE;
import static com.exigen.cm.Constants._TABLE_NODE_LOCK_INFO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.ValueChangeDatabaseStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.ModifiedNodeComparator;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeDefinitionImpl;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.NodeModification;
import com.exigen.cm.impl.NodeReference;
import com.exigen.cm.impl.NodeTypeContainer;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.ParentNode;
import com.exigen.cm.impl.PropertyDefinitionImpl;
import com.exigen.cm.impl.RepositoryConcurrentModificationException;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SecurityEntry;
import com.exigen.cm.impl.SecurityModificationEntry;
import com.exigen.cm.impl.SoftHashMap;
import com.exigen.cm.impl.observation.ChangeLog;
import com.exigen.cm.impl.observation.EventState;
import com.exigen.cm.impl.observation.EventStateCollection;
import com.exigen.cm.impl.observation.ObservationManagerImpl;
import com.exigen.cm.impl.security.JCRSecurityHelper;
import com.exigen.cm.impl.security.RepositorySecurityManager;
import com.exigen.cm.impl.security.SecurityConditionFilter;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SessionSecurityManager;
import com.exigen.cm.jackrabbit.lock.LockManagerImpl;
import com.exigen.cm.jackrabbit.lock.LockManagerListener;
import com.exigen.cm.jackrabbit.name.IllegalNameException;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.name.UnknownPrefixException;
import com.exigen.cm.jackrabbit.name.Path.PathElement;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.BLOBFileValue;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.version.VersionHistoryImpl;
import com.exigen.cm.jackrabbit.version.VersionManager;
import com.exigen.cm.jackrabbit.version.VersionManagerImpl;
import com.exigen.cm.jackrabbit.version.VersionStorageInitializer;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.vf.commons.logging.LogUtils;

public abstract class _AbstractsStateManager implements _StateManager{

    /** Log for this class */
    private static final Log log = LogFactory.getLog(_SessionStateManager.class);

    private SoftHashMap<Long, _NodeState> nodeCache = new SoftHashMap<Long, _NodeState>(100);
    private SoftHashMap<Long, List<_NodeState>> _modifiedCache = new SoftHashMap<Long, List<_NodeState>>(100); 

	private RepositoryStateManager sm;
	private HashSet<_NodeState> _lockedNodes = new HashSet<_NodeState>();
	private HashMap<Long,RowMap> _lockedRowMap = new HashMap<Long,RowMap>();

	private Map<Long, _NodeState> modifiedStates = new LinkedHashMap<Long, _NodeState>();

	//protected RepositoryImpl repository;
	
    private List<NodeModification> _nodeModificationList = new ArrayList<NodeModification>();
    
    protected SessionSecurityManager securityManager;

	//private ArrayList<_NodeState> readAheadNodes;
	//private final Long sessionId;

	private final SessionInfo sessionInfo;

    private LockManagerImpl lockManager;
	
	public void logout() {
		_modifiedCache.clear();
		_nodeModificationList.clear();
		childMax.clear();
		_lockedNodes.clear();
		_lockedRowMap.clear();
		modifiedStates.clear();
		nodeCache.clear();
		
		_modifiedCache = null;
		_nodeModificationList = null;
		childMax = null;
		_lockedNodes = null;
		_lockedRowMap = null;
		modifiedStates = null;
		nodeCache = null;
		
	}

	

	public _AbstractsStateManager(RepositoryImpl repository, Long sessionId, String userId, String workspaceName, boolean securityAllowed, LockManagerImpl lockManager) {
		super();
		this.lockManager = lockManager;
		this.securityAllowed = securityAllowed;
		//this.sessionId = sessionId;
		//this.repository = repository;
		sessionInfo = new SessionInfo(repository, sessionId, userId, workspaceName);
		this.sm = repository.getStateManager();
        this.securityManager = new SessionSecurityManager(this);
		
	}
	
	public _NodeState getNodeState(Long nodeId, ArrayList<Long> readAheadIds) throws RepositoryException {
		return getNodeState(nodeId, readAheadIds, true, nodeId.toString());
	}

	public _NodeState getNodeState(Long nodeId, ArrayList<Long> readAheadIds, boolean checkSecurity) throws RepositoryException {
		return getNodeState(nodeId, readAheadIds, checkSecurity, nodeId.toString());
	}

	   private LockManagerImpl getLockManager() {
	        return lockManager;
	    }
	
	void evictFromModiffiedCache(_NodeState state){
		_modifiedCache.remove(state.getNodeId());
		for(Long id:state.getParentCache()){
			_modifiedCache.remove(id);
		}
	}
	
	public _NodeState getChildNode(_NodeState state, QName childName,
			int childIndex, boolean checkSecurity) throws RepositoryException {
		
		if (state.getStatus() == ItemStatus.New){
			//Long id = state.getNodeId();
			//System.out.println("Look in new nodes "+modifiedStates.size());
			/*for(_NodeState st:modifiedStates.values()){
				if (id.equals(st.getParentId()) ){
					if (childIndex == st.getIndex() && childName.equals(st.getName())){
						return st;
					}
				}
			}*/
			IndexedQname iName = new IndexedQname(childName,childIndex);
			if (state.hasCachedChildName(iName)){
				Long _id = state.getCachedChildName(iName);
				return modifiedStates.get(_id); 
			}
			return null;
		}
		
		// TODO optimize this method (use cache or mass loading)
		_NodeState result = null;
		// TODO check already loaded nodes in nodemanager
		// TODO check node path against requested path
		/*
		 * result = getNodeManager().getNodeByPath(getPrimaryPath(), name2,
		 * index2); if (result != null){ return result; }
		 */
		boolean invalidated = false;
		boolean founded = true;
		int cIndex = childIndex;
		//while (founded) {
			IndexedQname childNodeName = new IndexedQname(childName, cIndex);
			if (state.hasCachedChildName(childNodeName)) {
				try {
					// TODO check node path against requested path
					Long nodeId = state.getCachedChildName(childNodeName);
					_NodeState childState = getNodeState(nodeId, null);
					if (childState.getStatus() == ItemStatus.Invalidated) {
						//return null;
						invalidated = true;
						//ppntln("skip invalidated node");
					} else {
						if (childState.getParentId().equals(state.getId())){
							return childState;
						} else {
							childState = null;
						}
					}
				} catch (Exception exc) {
					
				}
			}/* else {
				founded = false;
			}
			if (invalidated){
				cIndex++;
			}
		}
		IndexedQname childNodeName = new IndexedQname(childName, childIndex);*/
		boolean hasModifiedChildNodes = false;
		Long nnId = state.getNodeId();
		for(_NodeState nn:modifiedStates.values()){
			if (nnId.equals(nn.getNodeId())){
				hasModifiedChildNodes = true;
				break;
			}
		}
		if (invalidated){
			return null;
		}
		
		//getModifiedNodesDirect(state).size()
		if (hasModifiedChildNodes) {

			if (!checkSecurity) {
				IdIterator childIds = getChildNodesId(state, checkSecurity, childName);
				for (Long nodeId : childIds) {
					_NodeState child = getNodeState(nodeId,childIds.getNextIds(), checkSecurity, nodeId.toString());// mngr.buildNode(nodeId,
					// false);
					if (child.getName().equals(childName)
							&& childIndex == child.getIndex()) {
						result = child;
						break;
					}
				}
			} else {
				IdIterator childIds = getChildNodesId(state, checkSecurity, childName);
				for (Long nodeId : childIds) {
					_NodeState child = getNodeState(nodeId,childIds.getNextIds(), checkSecurity, nodeId.toString());// mngr.buildNode(nodeId,
					// false);
					if (child.getName().equals(childName)
							&& childIndex == child.getIndex()) {
						result = child;
						break;
					}
				}
				/*try {
					List<_NodeState> states = getNodes(state, childName
							.toJCRName(getNamespaceRegistry()), checkSecurity);
					for (_NodeState n : states) {
						if (n.getIndex() == childIndex) {
							result = n;
							break;
						}
					}
				} catch (NoPrefixDeclaredException e) {
					throw new RepositoryException(e);
				}*/
			}

		} else {
			// try to load from database
			result = getChildNodeStateFromDatabase(state, childName,
                    childIndex, checkSecurity, childNodeName);
		}
		return result;

	}



    private _NodeState getChildNodeStateFromDatabase(_NodeState state,
            QName childName, int childIndex, boolean checkSecurity,
             IndexedQname childNodeName)
            throws RepositoryException {
        _NodeState result = null;
        DatabaseConnection conn = getConnection();
        DatabaseSelectAllStatement st = null;

        try {

        	// TODO find all nodes , update indexes based on security info
        	st = DatabaseTools.createSelectAllStatement(TABLE_NODE, false);
        	st.addResultColumn(DEFAULT_ROOT_ALIAS + "." + FIELD_ID);
        	// add parent id
        	if (state.getInternalDepth() == 0) {
        		DatabaseCondition c1 = Conditions.eq(TABLE_NODE__PARENT, state.getNodeId());
        		DatabaseCondition c2 = Conditions.eq(TABLE_NODE__PARENT, getSystemRootId());
        		st.addCondition(Conditions.or(c1, c2));

        	} else {
        		st.addCondition(Conditions.eq(TABLE_NODE__PARENT, state.getNodeId()));
        	}
        	// add node name
        	JCRHelper.populateQNameCondition(st, childName, getNamespaceRegistry());
        	// add index
        	st.addCondition(Conditions.eq(TABLE_NODE__INDEX, new Integer(childIndex)));
        	if (checkSecurity) {
        		getSecurityConditionFilter().addSecurityConditions(conn, st, true);
        	}
        	st.execute(conn);
        	if (st.hasNext()) {
        		RowMap row = st.nextRow();
        		Long nodeId = (Long) row.get(DEFAULT_ROOT_ALIAS + "." + FIELD_ID);
        		result = getNodeState(nodeId,null, true,nodeId.toString());
        		if (result.getParentId().equals(state.getId())){
        			state.registerChild(childNodeName, nodeId);
        		}

        	}
        	st.close();
        } finally {
        	if (st != null) {
        		st.close();
        	}
        }
        return result;
    }
	public synchronized _NodeState getNodeState(Long nodeId, List<Long> readAheadIds, boolean checkSecurity, String objectDescription) throws RepositoryException {
		// find in local cache
		
		_NodeState result = modifiedStates.get(nodeId);
		if (result == null){
			result = nodeCache.get(nodeId);
			if (result != null && checkSecurity){
				try {
					result.checkDenyAccess();
				} catch (AccessDeniedException exc){
					result = null;
				}
			}
		}
		if (result  == null){
			result = nodeCache.get(nodeId);
			if (result == null){
				if (readAheadIds == null){
					readAheadIds = new ArrayList<Long>();
				} else {
					ArrayList<Long> _readAheadIds = new ArrayList<Long>();
					_readAheadIds.addAll(readAheadIds);
					readAheadIds = _readAheadIds;
				}
				if (!readAheadIds.contains(nodeId)){
					readAheadIds.add(0, nodeId);
				}
				int max = sessionInfo.getRepository().getBatchSize();
				if (readAheadIds.size() > max){
					readAheadIds = readAheadIds.subList(0, max);
				}
				ArrayList<_NodeState> existed = new ArrayList<_NodeState>();
				ArrayList<Long> skip = new ArrayList<Long>();
				for(Long id:readAheadIds){
					_NodeState n = nodeCache.get(id);
					if (n != null && checkSecurity){
						try {
							n.checkDenyAccess();
						} catch (AccessDeniedException e) {
							n = null;
						}
					}
					if (n != null){
						existed.add(n);
						skip.add(n.getNodeId());
					}
				}
				readAheadIds.removeAll(skip);
				ArrayList<_NodeState> _readAheadNodes = loadNodeStatesFromDB(readAheadIds, getConnection(),objectDescription, checkSecurity);
				ArrayList<_NodeState> lockCheck = new ArrayList<_NodeState>();
				for(_NodeState state:_readAheadNodes){
					assignSession(state);
					if (state.getStatus() != ItemStatus.New
							&& state.getStatus() != ItemStatus.Destroyed
							&& state.getStatus() != ItemStatus.Invalidated){
						lockCheck.add(state);
					}
					if (state.getNodeId().equals(nodeId)){
						result = state;
					} 
					nodeCache.put(state.getNodeId(), state);
					Long pId = state.getParentId();
					_NodeState ppp = nodeCache.get(pId);
					if (ppp != null){
						state.registerParent(ppp);
					}
				}
				loadLock(lockCheck);
				_readAheadNodes.addAll(existed);
				 //readAheadNodes = _readAheadNodes;
					
				// update properties to sessionspecific
				//load lock info
								
				
			}
		}
		//update sns max
		if (result == null){
			throw new ItemNotFoundException("Node Id"+nodeId);
		}
		updateSNSMax(result);
		return result;
	}
	

	public void updateSNSMax(_NodeState state) throws RepositoryException{
		if (state.getInternalDepthLong() != null && state.getDepth() > 0){
			Path p;
			try {
				Path p1 = state.getPrimaryPath();
				p1 = p1.getAncestor(1);
				p = Path.create(p1, state.getName(), true);
			} catch (MalformedPathException e) {
				//shoul never happend
				throw new RepositoryException(e);
			}
			ChildMaxPosition max = this.childMax.get(p);
			if (max != null && max.isChanged()){
				state.setSnsMax(max.getMax());
			}
		}
		
	}


    protected void loadLock(ArrayList<_NodeState> state) throws RepositoryException {
		// TODO Auto-generated method stub
		
	}

	public List<_NodeState> getModifiedNodesDirect(Long nodeId, boolean withDeleted)
			throws RepositoryException {
		// TODO use updatable tree for modified node

        //Long nodeId = state.getNodeId();
        List<_NodeState> _result = _modifiedCache.get(nodeId);
        if (_result != null){
        	return _result;
        }
        List<_NodeState> result = new LinkedList<_NodeState>();

		_NodeState __state = modifiedStates.get(nodeId);
		if (__state != null && __state.isNew()){
			Collection<Long> ids = __state.getCachedChilds();
			for(Long id:ids){
				result.add(modifiedStates.get(id));
			}
			_result = new ArrayList<_NodeState>(result);
			
	        Collections.sort(_result, new ModifiedNodeComparator());
	        _modifiedCache.put(nodeId, _result);
	        return _result;
		}
		
		
        HashSet<Long> added = new HashSet<Long>(); 
		for (_NodeState n : modifiedStates.values()) {
			boolean hasParent = false;
			if (withDeleted && n.getStatus() == ItemStatus.Invalidated){
				hasParent = n.hasParentWithDeleted(nodeId);
			} else {
				hasParent = n.hasParent(nodeId);
			}
            if (hasParent){
                boolean skip = false;
                /*for(_NodeState n1: result){
                    if (n.hasParent(n1.getNodeId())){
                        skip = true;
                        break;
                    }
                }*/
                if (withDeleted && n.getStatus() == ItemStatus.Invalidated){
                    for(Long ii:n.getParentCacheWithDeleted()){
                    	if (added.contains(ii)){
                    		skip = true;
                    		break;
                    	}
                    }
                } else {
                    for(Long ii:n.getParentCache()){
                    	if (added.contains(ii)){
                    		skip = true;
                    		break;
                    	}
                    }
                }
                if (!skip){
                    result.add(n);
                    added.add(n.getNodeId());
                    ArrayList<_NodeState> removed = new ArrayList<_NodeState>();
                    Long ii = n.getNodeId();
                    for(_NodeState n1: result){
                    	if (withDeleted && n1.getStatus() == ItemStatus.Invalidated){
	                        if (n1.hasParentWithDeleted(ii)){
	                            removed.add(n1);
	                            added.remove(n1.getNodeId());
	                        }
                    	} else {
	                        if (n1.hasParent(ii)){
	                            removed.add(n1);
	                            added.remove(n1.getNodeId());
	                        }
                    	}
                    }
                    result.removeAll(removed);
                }
            }
        }
		_result = new ArrayList<_NodeState>(result);
		
        Collections.sort(_result, new ModifiedNodeComparator());
        _modifiedCache.put(nodeId, _result);
        return _result;
		
	}
	
/*	public Long countTotal(Long nodeId, QName name) throws RepositoryException {
		DatabaseConnection conn = getConnection();
		DatabaseCountStatement st = null;
		try {
			st = DatabaseTools.createCountStatement(Constants.TABLE_NODE);
			// add parent id
			st.addCondition(Conditions.eq(Constants.TABLE_NODE__PARENT,
							nodeId));
			// add node name
			JCRHelper.populateQNameCondition(st, name, getNamespaceRegistry());
			st.execute(conn);
			Long total = st.getCount();
			st.close();

			// count new & removed nodes
			for (_NodeState n : modifiedStates.values()) {
				if (n.getParentId() != null) {
					// ((NodeImpl)n.getParent()).getNodeId().equals(nodeId)
					if (nodeId.equals(n.getParentId())
							&& name.equals(n.getName())) {
						if (n.getStatus() == ItemStatus.New) {
							total = new Long(total.longValue() + 1);
						} else if (n.getStatus() == ItemStatus.Invalidated) {
							total = new Long(total.longValue() - 1);
						}
					}
				}
			}
			return total;
		} finally {
			if (st != null) {
				st.close();
			}
			conn.close();
		}

	}*/

	public void registerUUID(NodeImpl impl) {
		throw new UnsupportedOperationException();
	}

	
	
	public synchronized void save(Long itemId, boolean validate, boolean allowSaveVersionHistory, boolean incVersion) throws RepositoryException {
		ArrayList<_NodeState> tmpCache = new ArrayList<_NodeState>();
		//Long itemId = item.getNodeId();
		//System.out.println("Cache size : "+nodeCache.size());
		//_modifiedCache.clear();
	/*	if (item.isNode() && item.getStatus() == ItemStatus.New) {
			throw new RepositoryException("Can't call save on new node");
		}

		if (!item.isNode()) {
			throw new RepositoryException("Can't call save on single property");
		}*/

		List<_ItemState> dirty = getDirtyItems(itemId);
		if (dirty == null || dirty.size() == 0) {
			return;
		}

		// TODO uncomment me
		ObservationManagerImpl oManager = getObservationManager();
		NodeId rootNodeId = getRootNodeId();
		ArrayList<EventState> events = new ArrayList<EventState>();
		ChangeLog cl = new ChangeLog();
		if (validate){
			for (_ItemState i : dirty) {
				if (i.getStatus() == ItemStatus.New) {
					cl.added(i);
					events.addAll(i.getEvents());
				} else if (i.getStatus() == ItemStatus.Modified) {
					cl.modified(i);
					events.addAll(i.getEvents());
				} else if (i.getStatus() == ItemStatus.Destroyed
						|| i.getStatus() == ItemStatus.Invalidated) {
					cl.deleted(i);
					events.addAll(i.getEvents());
				}
			}
			 
			dirty = getDirtyItems(itemId);
		}

		// check refereneces
		// TODO uncomment this
		
		
		ArrayList<WeakModifications> weakModifications = new ArrayList<WeakModifications>();
		  for (_ItemState i : dirty) {
			if (i instanceof _NodeState) {
				_NodeState n = (_NodeState) i;
				if (n.getStatus() == ItemStatus.Invalidated || n.getStatus() == ItemStatus.Destroyed) {
					if (n.getReferences().size() > 0) { 
						// TODO show references
						// nodes nodes
						boolean error = false;
						for (NodeReference nr : n.getReferencesFrom()) {

							// check if node removed
							if (nr.getState() != ItemStatus.Destroyed
									&& nr.getState() != ItemStatus.Invalidated) {
								//check if this weak reference
								Long toId = nr.getToId();
								Long fromId = nr.getFromId();
								_NodeState from = getNodeState(fromId, null, false);
								_PropertyState p = from.getProperty(nr.getPropertyQName(), false);
								if (p.getType() != PropertyType283.WEAKREFERENCE){
									error = true;									
								} else {
									//checge toTo id to null, and increase node version
									//1. find reference
									
									for(NodeReference to:from.getReferencesTo()){
										if (to.getToId().equals(toId) && to.getPropertyQName().equals(p.getName())){
											//modify from reference
											//to.setToId(null);
											weakModifications.add(new WeakModifications(to, from));
											
											//increase noe version
										}
									}
								}
							}
						}
						if (error) {
							//getSession().logout();
							throw new ReferentialIntegrityException("Other nodes references to this node("
											+ n.safeGetJCRPath()
											+ "), cannot be removed");
						}
					}
				}
			}
		}
		 
		if (oManager != null){
			EventStateCollection eventCollection = getObservationManager()
					.createEventStateCollection(events);
			eventCollection.createEventStates(rootNodeId, cl);
			eventCollection.dispatchBefore();
		}
		  

		if (validate){
			validateTransientItems(dirty);
		}
		
		
		//TODO validate node modification chains
        ArrayList<_NodeState> dirtyNodes = new ArrayList<_NodeState>();
        //ArrayList<_NodeState> dirtyVersionableNodes = new ArrayList<_NodeState>();
        ArrayList<String> newUUID = new ArrayList<String>();
        for(_ItemState i:dirty){
        	
            if (i.isNode()){
            	_NodeState n = (_NodeState)i;
            	
                dirtyNodes.add(n);
                if (n.isNodeTypeRegistered(QName.MIX_VERSIONABLE)){
                	//dirtyVersionableNodes.add(n);
                	newUUID.add(n.getInternalUUID());
                }
                //check references
                /* PROBABLY CODE DUPLICATION
                 
                 if (n.isRemoved()){
                    if (n.getReferences().size() > 0){
                        //TODO show references nodes nodes
                        for(Iterator it = n.getReferencesFrom().iterator() ; it.hasNext();){
                            NodeReference nr = (NodeReference) it.next();
                            log.info(nr);
                            //TODO optimize read Ahead
                            _NodeState refNode = getNodeState(nr.getFromId(), null);
                            if (refNode.isRemoved()){
                            	throw new RepositoryException("Other nodes references to this node("+toString()+"), cannot be removed, session become invalid");
                            }

                        }
                    }
                	
                }*/
                
            }
        }
        
        _NodeState vh = null;
        if (allowVersionManager() && allowSaveVersionHistory){
        	vh = ((VersionManagerImpl)getVersionManager()).getHistoryRoot().getNodeState();
        }

        
        HashMap<String, Long> paths = new HashMap<String, Long>(); 
        if (newUUID.size() > 0 && vh != null){
        	_NodeState st = getNodeState(getRepository().getVersionStorageNodeId(), null);
        	VersionStorageInitializer vi = new VersionStorageInitializer(getConnection(), getRepository(), getNamespaceRegistry(), getNodeTypeManager());
        	List<Long> ids1 = vi.initialize(newUUID, st);
        	getNodeState(ids1.get(0), ids1, false,ids1.get(0).toString());
        	paths = vi.buildPaths();
        	
        }

        List<NodeModification> nmList = _getNodeModificationList();
        for(NodeModification nm : nmList){
            for(_NodeState n : dirtyNodes){
                if (!nm.allowNodeSave(n,dirtyNodes)){
                    throw new ConstraintViolationException("not all modified nodes are in saved node tree");
                }
            }
        }
        List<FakeJCRTransaction> fakeTransactions = new ArrayList<FakeJCRTransaction>();

        //ContentStore store = session.getContentStore();
        //execute Statements
        DatabaseConnection conn = getConnection();
        boolean allowCommitRollback = conn.allowCommitRollback();
        try {
            //store.begin();
            ChangeLog changeLog = null;
            //ArrayList<CacheKey> evictList = new ArrayList<CacheKey>();
            while (changeLog == null || !changeLog.isEmpty()){
                if (changeLog == null){
                    changeLog = new ChangeLog();
                } else {
                    changeLog.reset();
                }
                vh = null;
                if (allowVersionManager() && allowSaveVersionHistory){
                	vh = ((VersionManagerImpl)getVersionManager()).getHistoryRoot().getNodeState();
                }

                ChangeState changeState = new ChangeState(getNamespaceRegistry(), getStoreContainer());
                ArrayList<DatabaseStatement> statements = new ArrayList<DatabaseStatement>();
                
                //process weak references
                for(WeakModifications wm:weakModifications){
                	if (!wm.getState().getStatus().equals(ItemStatus.New)){
                		conn.lockNode(wm.getState().getNodeId());
        	            RowMap r = conn.loadRow(TABLE_NODE, FIELD_ID, wm.getState().getNodeId());
        	            if (!r.getLong(TABLE_NODE__VERSION_).equals(wm.getState().getVersion())){
        	                //TODO optimize this (use batch loading)
        	            	this.loadNodeStateFromDB(wm.getState().getNodeId(), conn, wm.getState().getInternalPath(), false);
        	                throw new RepositoryConcurrentModificationException(wm.getState().safeGetJCRPath()+" is modified by another session",wm.getState().getInternalUUID());
        	            }
                	}
                	if (wm.getRef().getId() != null){
	                	DatabaseUpdateStatement update = DatabaseTools.createUpdateStatement(TABLE_NODE_REFERENCE, FIELD_ID, wm.getRef().getId());
	                    update.addValue(SQLParameter.create(Constants.TABLE_NODE_REFERENCE__TO, (Long)null));
	                    statements.add(update);
                	}
                	if (dirtyNodes.contains(wm.getState())){
                		wm.getRef().setToId(null);
                		//wm.getRef().setChanged();
                	} else {
                		
                    	if (!wm.getState().getStatus().equals(ItemStatus.New)){
	                        //update version
	                        DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(TABLE_NODE);
	                        st.addCondition(Conditions.eq(FIELD_ID, wm.getState().getNodeId()));
	                       	st.addValue(SQLParameter.create(TABLE_NODE__VERSION_, wm.getState().getVersion().longValue()+1));
	                       	statements.add(st);
                    	}
                    }
                }
                _lockedNodes.clear();
                _lockedRowMap.clear();
                
                weakModifications.clear();
                
                ArrayList<DatabaseStatement> statementsLast = new ArrayList<DatabaseStatement>();
                //long start = System.currentTimeMillis();
                
                save00(itemId, incVersion, paths, conn, changeLog, changeState, statementsLast, fakeTransactions);
                
                
                
                statements.addAll(_saveNode1(itemId, conn, statementsLast, changeLog,changeState, incVersion, paths));
                //long end1 = System.currentTimeMillis() - start;
                if (vh != null){
                    save00(vh.getNodeId(), incVersion, paths, conn, changeLog, changeState, statementsLast, fakeTransactions);
                	statements.addAll(_saveNode1(vh.getNodeId(), conn, statementsLast, changeLog,changeState, incVersion, paths));
                	//System.out.println("Number of statements :"+statements.size());
                }
                //long end2 = System.currentTimeMillis() - start;
                
                EventStateCollection eventCollection = null;
                if (oManager != null && validate){
                	eventCollection = getObservationManager().createEventStateCollection(new ArrayList<EventState>());
	                eventCollection.createEventStates(rootNodeId, changeLog);
                }
                
                statements.addAll(_saveNode2(itemId, conn, statementsLast, changeLog, changeState));
                //long end3 = System.currentTimeMillis() - start;
                if (vh != null){
                	statements.addAll(_save2(vh, conn, statementsLast, changeLog, changeState));
                }
                //long end4 = System.currentTimeMillis() - start;
                
                if (this.sm.getRepository().isSupportFTS()){
                	changeState.processRemoveFTSProperties(conn);
                }
                
                if (this.sm.getRepository().isSupportOCR()){
                	changeState.processRemoveOCRProperties(conn);
                }
                
                
                changeState.processRemovedParentNode(conn);
                //long end5 = System.currentTimeMillis() - start;
                changeState.processRemovedNodesStep1(conn);
                changeState.preocessNewNodes(conn, getLockManager());
                changeState.processNewUnstructuredMultiValueProperty(conn);
                //long end6 = System.currentTimeMillis() - start;
                changeState.processNewTypes(conn);
                //long end7 = System.currentTimeMillis() - start;
                
                
                //execute CM_TYPE_BASE

                
                
                
                DatabaseTools.executeStatements(statements, conn);
                DatabaseTools.executeStatements(statementsLast, conn);

                //long end8 = System.currentTimeMillis() - start;
                changeState.processNewParentNode(conn);
                //long end9 = System.currentTimeMillis() - start;
                changeState.processNewReferences(conn);
                
                //long end10 = System.currentTimeMillis() - start;
                if (this.sm.getRepository().isSupportFTS()){
                	changeState.processNewFTSProperties(conn);
                }
                
                if (this.sm.getRepository().isSupportOCR()){
                	changeState.processNewOCRProperties(conn);
                }
                
                changeState.processSetNodeVersions(conn);
                
                //long end11 = System.currentTimeMillis() - start;
                
                //update max values
                
                Path itemPath = null;
                
                itemPath = getItemPath(itemId, conn);
                Path vhPath = null;
                if (vh != null){
                	vhPath = vh.getPrimaryPath();
                	
                }
                //DatabaseUpdateStatement updSt = null;
                //TODO use batch
                ArrayList<Path> removed = new ArrayList<Path>();
				HashSet<_NodeState> _nodes = new HashSet<_NodeState>(nodeCache.values());
				_nodes.addAll(modifiedStates.values());
            	//System.out.println("ChildMax >"+childMax.size()+" ,   Nodes > "+_nodes.size());
				DatabaseUpdateStatement updSt1_1 = null;
				DatabaseUpdateStatement updSt2_1 = null;	
				DatabaseUpdateStatement updSt1_2 = null;
				DatabaseUpdateStatement updSt2_2 = null;	
				//int i = 0;
                for(Path path:childMax.keySet()){
                	ChildMaxPosition p = childMax.get(path);
                	if (p.isChanged()){
                		//System.out.println(p.getParent());
                		if ((path.isDescendantOf(itemPath) || path.equals(itemPath)) || (vhPath != null && (path.isDescendantOf(vhPath) || path.equals(vhPath)))){
                			if (p.getMax() > 0){
	            				_NodeState st = p.getItem().getParent();  //(_NodeState)getItem(p.getParent(), false);

                				
                				String nm = p.getChildName().getNamespaceURI();
                				DatabaseUpdateStatement updSt = null;
	                			if (nm != null && nm.length() > 0){
	                				if (updSt1_1 == null){
	                					updSt1_1 = DatabaseTools.createUpdateStatement(TABLE_NODE);
	                				}
	                				updSt = updSt1_1;
	                			} else {
	                				if (updSt2_1 == null){
	                					updSt2_1 = DatabaseTools.createUpdateStatement(TABLE_NODE);
	                				}
	                				updSt = updSt2_1;
	                			}
	                				
	                			//i++;
	            				updSt.addCondition(Conditions.eq(TABLE_NODE__PARENT, st.getNodeId()));
	            				JCRHelper.populateQName(updSt, p.getChildName(), getNamespaceRegistry(), true);
	            				updSt.addValue(SQLParameter.create(TABLE_NODE__INDEX_MAX, p.getMax()));
	            				//TODO do we need version inc ???
	            				updSt.addValue(SQLParameter.createSQL(Constants.TABLE_NODE__VERSION_, Constants.TABLE_NODE__VERSION_+"+1"));
	            				updSt.addBatch();
	            				
	            				//update version in localy loaded nodes
	            				Path pParent = p.getParent();
	            				for(_NodeState mn:_nodes){
	            					if (mn.getDepth() > 0){
	            						Path pp = null;
	            						_NodeState ps = mn.getWeakParentState();
	            						if (ps != null){
	            							pp = ps.getPrimaryPath();
	            						} else {
	            							pp = mn.getPrimaryPath().getAncestor(1);
	            						}
	            						if (pp.equals(pParent) && mn.getName().equals(p.getChildName())){
	            							mn.setVersion(mn.getVersion()+1);
	            							mn.setSnsMax(p.getMax());
	            						}
	            					}
	            				}
                			}
            				removed.add(path);
                		}
                	}
                }
                for(Path path:childMax.keySet()){
                	ChildMaxPosition p = childMax.get(path);
                	if (p.isNew()){
                		//System.out.println(p.getParent());
                		if ((path.isDescendantOf(itemPath) || path.equals(itemPath)) || (vhPath != null && (path.isDescendantOf(vhPath) || path.equals(vhPath)))){
                			if (p.getMax() > 0){
	            				_NodeState st = p.getItem().getParent();  //(_NodeState)getItem(p.getParent(), false);

                				
                				String nm = p.getChildName().getNamespaceURI();
                				DatabaseUpdateStatement updSt = null;
	                			if (nm != null && nm.length() > 0){
	                				if (updSt1_2 == null){
	                					updSt1_2 = DatabaseTools.createUpdateStatement(TABLE_NODE);
	                				}
	                				updSt = updSt1_2;
	                			} else {
	                				if (updSt2_2 == null){
	                					updSt2_2 = DatabaseTools.createUpdateStatement(TABLE_NODE);
	                				}
	                				updSt = updSt2_2;
	                			}
	                				
	                			//i++;
	            				updSt.addCondition(Conditions.eq(TABLE_NODE__PARENT, st.getNodeId()));
	            				JCRHelper.populateQName(updSt, p.getChildName(), getNamespaceRegistry(), true);
	            				updSt.addValue(SQLParameter.create(TABLE_NODE__INDEX_MAX, p.getMax()));
	            				updSt.addBatch();
	            				
	            				//update version in localy loaded nodes
	            				Path pParent = p.getParent();
	            				for(_NodeState mn:_nodes){
	            					if (mn.getDepth() > 0){
	            						Path pp = null;
	            						_NodeState ps = mn.getWeakParentState();
	            						if (ps != null){
	            							pp = ps.getPrimaryPath();
	            						} else {
	            							pp = mn.getPrimaryPath().getAncestor(1);
	            						}
	            						if (pp.equals(pParent) && mn.getName().equals(p.getChildName())){
	            							mn.setSnsMax(p.getMax());
	            						}
	            					}
	            				}
                			}
            				removed.add(path);
                		}
                		p.resetNew();
                	}
                }
                //System.out.println("SNSMax updat "+i);
            	if (updSt1_1 != null){
            		updSt1_1.execute(conn);
            	}
            	if (updSt2_1 != null){
            		updSt2_1.execute(conn);
            	}
            	if (updSt1_2 != null){
            		updSt1_2.execute(conn);
            	}
            	if (updSt2_2 != null){
            		updSt2_2.execute(conn);
            	}
                for(Path p:removed){
                	childMax.remove(p);
                }
                /*if (updSt != null){
                	updSt.execute(conn);
                }*/
                
                
                /*if(statements.size() > 50){
                	ppntln("Statements "+statements.size());
                	ppntln("Time1: "+end1);
                	ppntln("Time2: "+end2);
                	ppntln("Time3: "+end3);
                	ppntln("Time4: "+end4);
                	ppntln("Time5: "+end5);
                	ppntln("Time6: "+end6);
                	ppntln("Time7: "+end7);
                	ppntln("Time8: "+end8);
                	ppntln("Time9: "+end9);
                	ppntln("Time10: "+end10);
                	ppntln("Time11: "+end11);
                	HashMap<String,Long> tt = new HashMap<String, Long>();
                	int others = 0;
                	for(DatabaseStatement st:statements){
                		if (st instanceof ValueChangeDatabaseStatement){
                			String tableName = ((ValueChangeDatabaseStatement)st).getOriginalTableName();
                			Long t = tt.get(tableName);
                			if (t == null){
                				t = (long)0;
                			}
                			t++;
                			tt.put(tableName, t);
                		} else {
                			others++;
                		}
                	}
                	
                	for(String n:tt.keySet()){
                		Long v = tt.get(n);
                		
                	}
                }*/
                
                commitPutStores();
                
                _afterSave(itemId, conn, changeLog, tmpCache);
                if (vh != null){
                	_afterSave(vh, conn, changeLog, tmpCache);
                }
                //observation events ...
                getConnectionProvider().setAllowCommitRollback(conn, false);
                if (oManager != null && validate){
	                //EventStateCollection eventCollection = getObservationManager().createEventStateCollection();
	                //eventCollection.createEventStates(rootNodeId, changeLog);
	                eventCollection.dispatch();
                }
                if (!validate){
                	changeLog.reset();
                }
                
            }
            getConnectionProvider().setAllowCommitRollback(conn, allowCommitRollback);
            //getCacheManager().evict(evictList);
            commitStores();                
            conn.commit();
            
            
            nmList = _getNodeModificationList();
            HashSet<NodeModification> removed = new HashSet<NodeModification>();
            for(NodeModification nm : nmList){
                for(_NodeState n : dirtyNodes){
                    if (!nm.isFinished(n,dirtyNodes)){
                    	removed.add(nm);
                    }
                }
            }
            nmList.removeAll(removed);
            for(_ItemState i:dirty){
            	if (i.isNode()){
            		if (i.isDestroyed()){
            			modifiedStates.remove(i);
            			nodeCache.remove(i);
            		}
            	}
            }
            
            
        } catch (RepositoryException exc){
        	throw exc;
        } catch (Exception exc){
            try {
                rollbackStores();                
            } catch (Exception e){}
            conn.rollback();
            exc.printStackTrace();
            throw new RepositoryException("Error persisting changes", exc);
        } finally {
        	for(FakeJCRTransaction tr: fakeTransactions){
        		tr.reset();
        	}
        	
            //restore allowCommitRollback
            getConnectionProvider().setAllowCommitRollback(conn, allowCommitRollback);
            conn.close();
            _lockedNodes.clear();
            _lockedRowMap.clear();
        }
        
        //remove destroed nodes
        ArrayList<Long> removed = new ArrayList<Long>();
        for(_NodeState state:nodeCache.values()){
        	if (state.getStatus() == ItemStatus.Destroyed){
        		removed.add(state.getNodeId());
        	}
        }
        removed.removeAll(removed);
        
		
        
        //clear childMax
        if (childMax.size() > (nodeCache.size()+modifiedStates.size())){
	    	try {
		        HashSet<Path> _paths = new HashSet<Path>();
		        for(_NodeState st:nodeCache.values()){
		        	if (st.getDepth() > 1){
			        	_NodeState parent = st.getWeakParentState();
			        	Path p = null;
			        	if (parent != null){
			        		p = parent.getPrimaryPath();
			        	} else {
			        		p = st.getPrimaryPath().getAncestor(1);
			        	}
						p  = Path.create(p, st.getName(), true);
		            	_paths.add(p);
		        	}
		        }
		        for(_NodeState st:modifiedStates.values()){
		        	if (st.getDepth() > 1){
			        	_NodeState parent = st.getWeakParentState();
			        	Path p = null;
			        	if (parent != null){
			        		p = parent.getPrimaryPath();
			        	} else {
			        		p = st.getPrimaryPath().getAncestor(1);
			        	}
						p  = Path.create(p, st.getName(), true);
		            	_paths.add(p);
		        	}
		        }
		        ArrayList<Path> removedChildMax = new ArrayList<Path>();
		        for(Path p:childMax.keySet()){
		        	if (!_paths.contains(p)){
		        		removedChildMax.add(p);
		        	}
		        }
		        
		        //System.out.println("Before "+childMax.size());
		        for(Path p : removedChildMax){
		        	if (!childMax.get(p).isChanged()){
		        		childMax.remove(p);
		        	}
		        }
		        //childMax.clear();
		        //System.out.println("after "+childMax.size()+ "    "+nodeCache.size());
			} catch (MalformedPathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        

	}



    private Path getItemPath(Long itemId, DatabaseConnection conn)
            throws RepositoryException {
        Path itemPath = null;
        _NodeState _itemState = getNodeFromCache(itemId);
        if (_itemState != null){
            itemPath = _itemState.getPrimaryPath();
        } else {
            DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(TABLE_NODE, FIELD_ID, itemId);
            st.execute(conn);
            RowMap row = st.getRow();
            String p = row.getString(TABLE_NODE__NODE_PATH);
            String _path = JCRHelper.convertPath(p);
            if (_path.equals("")){
                itemPath = Path.ROOT;
            } else {
                try {
                    itemPath = Path.create(_path, getNamespaceResolver(), true);
                } catch (MalformedPathException e) {
                    throw new RepositoryException(e);
                }
            }
            //throw new UnsupportedOperationException();
        }
        return itemPath;
    }

	private void save00(Long nodeId, boolean incVersion, HashMap<String, Long> paths, DatabaseConnection conn, ChangeLog changeLog, ChangeState changeState, ArrayList<DatabaseStatement> statementsLast
			, List<FakeJCRTransaction> fakeTransactions) throws RepositoryException, InvalidItemStateException {
		HashSet<Long> nodeForLock = new HashSet<Long>();
		HashMap<Long, Long> checkVersions = new HashMap<Long, Long>();
		_save0(nodeId, conn, statementsLast, changeLog,changeState, incVersion, paths, nodeForLock, checkVersions, fakeTransactions);
		DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
		//System.out.println("Lock nodes "+nodeForLock);
		if (nodeForLock.size() > 0){
		    st1.addResultColumn(FIELD_ID);
		    st1.addResultColumn(TABLE_NODE__VERSION_);
		    st1.addCondition(Conditions.in(FIELD_ID, nodeForLock));
            conn.getTransactionSynchronization().registerLock(conn.getConnectionId(), TABLE_NODE, "ID", nodeForLock);
		    st1.setLockForUpdate(true);
		    st1.execute(conn);
		    //ArrayList<RowMap> rows = new ArrayList<RowMap>();
		    while (st1.hasNext()){
		    	RowMap row = st1.nextRow();
		    	//rows.add(row);
		    	Long id = row.getLong(FIELD_ID);
		    	Long version = row.getLong(TABLE_NODE__VERSION_);
		    	Long v2 = checkVersions.get(id);
		    	if (v2 != null && !v2.equals(version)){
		    		_NodeState state = getNodeState(id, null);
		    		boolean allow = isOnlyNewNodes(state);
		    		
		    		if (!allow){
		    		    throw new InvalidItemStateException(state.safeGetJCRPath()+" is modified by another session");
		    		}
		    	}
		    	nodeForLock.remove(id);
		    	checkVersions.remove(id);
		    }
		    if (nodeForLock.size() > 0 ){
		    	throw new InvalidItemStateException("Can't lock nodes "+nodeForLock);
		    }
		    st1.close();
		}
		if (checkVersions.size() > 0){
		    st1 = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
		    st1.addResultColumn(FIELD_ID);
		    st1.addResultColumn(TABLE_NODE__VERSION_);
		    st1.addCondition(Conditions.in(FIELD_ID, checkVersions.keySet()));
		    st1.setLockForUpdate(true);
            conn.getTransactionSynchronization().registerLock(conn.getConnectionId(), TABLE_NODE, "ID", checkVersions);            
		    st1.execute(conn);
		    while (st1.hasNext()){
		    	RowMap row = st1.nextRow();
		    	Long id = row.getLong(FIELD_ID);
		    	Long version = row.getLong(TABLE_NODE__VERSION_);
		    	Long v2 = checkVersions.get(id);
		    	if (!v2.equals(version)){
		    		_NodeState state = getNodeState(id, null);
		            throw new InvalidItemStateException(state.safeGetJCRPath()+" is modified by another session");                		
		    	}
		    	checkVersions.remove(id);
		    }
		    if (checkVersions.size() > 0 ){
		    	throw new InvalidItemStateException("Can't check nodes versions "+nodeForLock);
		    }
		    st1.close();
		}
	}



    private boolean isOnlyNewNodes(_NodeState state) throws RepositoryException {
        if (state.isBasePropertiesChanged()){
            return false;
        }
        boolean propertyChanged = false;
        boolean hasRemovedNoded = false;
        boolean hasSNS = false;
        
        if (state.isReferencesFromLoaded()){
	        for(NodeReference r:state.getReferencesFrom()){
	            if (r.getState() != ItemStatus.Normal){
	                propertyChanged = true;
	            }
	        }
        }
        if (state.isReferencesToLoaded()){
	        for(NodeReference r:state.getReferencesTo()){
	            if (r.getState() != ItemStatus.Normal){
	                propertyChanged = true;
	            }
	        }
        }
        for(_PropertyState p:state.getAllProperties()){
            if (p.getStatus() != ItemStatus.Normal){
                propertyChanged = true;
            }
        }
        List<_NodeState> nodes = getModifiedNodesDirect(state.getNodeId(), true);
        for(_NodeState n:nodes){
            if (n.getStatus() == ItemStatus.Invalidated || n.getStatus() == ItemStatus.Destroyed){
                hasRemovedNoded = true;
            }
            if (n.getStatus() == ItemStatus.New ){
                if (n.getDefinition().allowsSameNameSiblings()){
                    //n.getParent().getNodeTypeManager().getNodeTypeBySQLId(n.getParent().getNodeTypeId())P
                    hasSNS = true;
                }
            }
        }
        boolean allow = getRepository().isReducedVersionCheck() && !propertyChanged && !hasRemovedNoded && !hasSNS;
        return allow;
    }



	private void _afterSave(_ItemState item, DatabaseConnection conn, ChangeLog changeLog, List<_NodeState> tmpCache) throws RepositoryException {
		if (item instanceof _NodeState){
			_afterSave(((_NodeState)item).getNodeId(), conn, changeLog, tmpCache);
		} else {
			_afterSave((_PropertyState)item, conn, changeLog);
		}
	}

    protected synchronized void _afterSave(Long nodeId,DatabaseConnection conn, ChangeLog changeLog, List<_NodeState> tmpCache) throws RepositoryException {
        
        _NodeState _state = modifiedStates.get(nodeId);
        
        if (_state != null){
        LogUtils.debug(log, "After Persist "+nodeId + " ; state="+ _state.getStateString());
        
        if (_state.getStatus() == ItemStatus.Destroyed) {
        	return;
        }
        
        //3.TODO update parents
        ArrayList<ParentNode> rr = new ArrayList<ParentNode>();
        for(ParentNode pn:_state.getParentNodes()){
            if (pn.getState().equals(ItemStatus.New)){
                pn.resetStateToNormal();
            } else if (pn.getState().equals(ItemStatus.Invalidated)){
            	rr.add(pn);
            } else if (pn.getState().equals(ItemStatus.Destroyed)){
            	rr.add(pn);
            }
        }
        _state.getParentNodes().removeAll(rr);
        
        //TODO update Types & Details (Create) 
        for(NodeTypeContainer ntc: _state.getAllTypes()){
            if (ntc.getState() == ItemStatus.New){
                ntc.resetStateToNormal();
            }
        }
        
        //TODO save properties existing properties
        for(_PropertyState s: _state.getAllProperties()){
            _afterSave(s, conn, changeLog);
        }
        //TODO update state (new, removed)
        if (_state.getStatus().equals(ItemStatus.New)){
            _state.resetToNormal();
            unRegisterModifedState(_state);
            sm.registerNewState(_state);
            nodeCache.put(nodeId, _state);
            tmpCache.add(_state);
        } else if (_state.getStatus().equals(ItemStatus.Modified)){
            boolean incVersion = true;
            if (getRepository().isReducedVersionCheck() && _state.isOnlyNeNode){
                incVersion = false;
            }
            _state.resetToNormal();
            unRegisterModifedState(_state);
            if (incVersion){
                _state.increaseVersion();
            }            
        } else if (_state.getStatus().equals(ItemStatus.Normal)){
            // do nothing
        	unRegisterModifedState(_state);
        } else if (_state.getStatus().equals(ItemStatus.Invalidated)){
            _state.setStatusDestroyed();
            unRegisterModifedState(_state);
            unRegisterNode(_state);
        } else {
            throw new UnsupportedOperationException("Unknown State "+_state.getStateString());
        }

        }
        //TODO update Types & Details (remove) 
        
        //TODO save child nodes
        //List<_NodeState> nodes = state.getModifiedNodes();
        List<_NodeState> nodes = modifiedNodeCache.get(nodeId);
        for(_NodeState s: nodes){
        	if (s.isRemoved() || s.hasParent(nodeId)){
        		_afterSave(s, conn, changeLog, tmpCache);
        	}
        }
        //state.setModifiedNodes(null);
        modifiedNodeCache.remove(nodeId);
        if (_state != null){
        //TODO update references table
        if (_state.isReferencesToLoaded()){
	        ArrayList<NodeReference> destroyedRefs = new ArrayList<NodeReference>();
	        ArrayList<NodeReference> refTo = _state.getReferencesTo();
	        //TODO optimize read Ahead
	        /*HashSet<Long> ids = new HashSet<Long>();
	        for(NodeReference nr:refTo){
	        	ids.add(nr.getToId());
	        }*/
	        
	        for(NodeReference nr: refTo){
	            if (nr.getState().equals(ItemStatus.New)){
	                Long _nodeId = nr.getToId();
	                //TODO get also deleted items
	                _NodeState n = getNodeState(_nodeId, null);
	                if (n != null){
	                    n.registerPermanentRefeference(nr);
	                }
	                nr.resetStateToNormal();
	            } else if (nr.getState().equals(ItemStatus.Invalidated)){
	                Long _nodeId = nr.getToId();
	                try {
	                    //TODO get also deleted items
	                    _NodeState n = getNodeState(_nodeId, null);
	                    if (n!= null){
	                        n.registerPermanentRefeferenceRemove(nr);
	                    }
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
	        _state.getReferencesTo().removeAll(destroyedRefs);
        }
        }
        
        
    }
    
    private void unRegisterNode(_NodeState state) throws ItemNotFoundException {
    	unRegisterModifedState(state);
		this.sm.unregisterUUID(state.getInternalUUID());
		
		
	}

    public void unRegisterModifedState(_NodeState state) {
		modifiedStates.remove(state.getNodeId());
		evictFromModiffiedCache(state);

	}

	
	protected void _afterSave(_PropertyState state,DatabaseConnection conn, ChangeLog changeLog) throws RepositoryException {
		if (state.getStatus().equals(ItemStatus.New)){
            state.resetToNormal();
		} else if (state.getStatus().equals(ItemStatus.Normal)){
			
		} else if (state.getStatus().equals(ItemStatus.Modified)){
            state.resetToNormal();
		} else if (state.getStatus().equals(ItemStatus.Invalidated)){
            state.setStatusDestroyed();
		} else {
            //throw new UnsupportedOperationException();
			//do nothing
		}
    }


	private Collection<DatabaseStatement> _save2(_ItemState item, DatabaseConnection conn,  
			ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog,ChangeState changeState) throws RepositoryException {
		if (item instanceof _NodeState){
			return _saveNode2(((_NodeState)item).getNodeId(), conn, statementsLast, changeLog, changeState);
		} else {
			return _saveProperty2((_PropertyState)item, conn, statementsLast, changeLog);
		}
	}



	private Collection<DatabaseStatement> _saveProperty2(_PropertyState state, DatabaseConnection conn,
	        ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog) {
        return new ArrayList<DatabaseStatement>();
	}

	private Collection<DatabaseStatement> _saveNode2(Long nodeId, DatabaseConnection conn, 
			ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog,ChangeState changeState) throws RepositoryException {

	    ArrayList<DatabaseStatement> statements =  new ArrayList<DatabaseStatement>();
		
		_NodeState _state = this.modifiedStates.get(nodeId); 
		if (_state != null){
            LogUtils.debug(log, "Persist2 "+_state.getNodeId() + " ; state="+ _state.getStateString());
            
            if (_state.getStatus() == ItemStatus.Destroyed) {
            	return statements;
            }
        }
            
            //TODO save child nodes
            //List<_NodeState> nodes = state.getModifiedNodes();
        List<_NodeState> nodes = modifiedNodeCache.get(nodeId);
        for(_NodeState s:nodes){
            statements.addAll(_save2(s, conn, statementsLast, changeLog, changeState));
        }
            
        if (_state != null){   
            //TODO update references table
            if (_state.isReferencesToLoaded()) {
    	        for(NodeReference nr: _state.getReferencesTo()){
    	            if (nr.getState().equals(ItemStatus.New)){
    	/*                DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE_REFERENCE);
    	                insert.addValue(SQLParameter.create(FIELD_ID, nr.getId()));
    	                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__FROM, nr.getFromId()));
    	                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__TO, nr.getToId()));
    	                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__PROPERTY_NAME, nr.getPropertyName()));
    	                insert.addValue(SQLParameter.create(TABLE_NODE_REFERENCE__PROPERTY_NAMESPACE, nr.getPropertyNamespaceId()));
    	                statements.add(insert);*/
    	                changeState.addNewReference(nr);
    	            } else if (nr.getState().equals(ItemStatus.Normal)){
    	                //do nothing
    	            } else if (nr.getState().equals(ItemStatus.Normal)){
    	                //do nothing
    	            } else if (nr.getState().equals(ItemStatus.Invalidated)){
    	                if (nr.getId() != null) {
    	                    DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_REFERENCE, FIELD_ID, nr.getId());
    	                    statements.add(st);
    	                }
    	            }else if (nr.getState().equals(ItemStatus.Destroyed)){
    	                //do nothing
    	            } else {
    	                throw new UnsupportedOperationException();
    	            }
    	        }
            }
            
            ArrayList<String> droppedTables = new ArrayList<String>();
            for(NodeTypeContainer ntc:_state.getAllTypes()){
                if (ntc.getState() == ItemStatus.New){
                    //do nothing
                } else if (ntc.getState() == ItemStatus.Normal){
                    //do nothing
                } else if (ntc.getState() == ItemStatus.Invalidated){
                    //if (ntc.getId() != null) {
                    	String tableName = ntc.getNodeType().getTableName();
                    	if (_state.getStatus() == ItemStatus.Invalidated || _state.getStatus() == ItemStatus.Destroyed  ) {
                    		if (!droppedTables.contains(tableName)){
                    			DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(tableName, FIELD_TYPE_ID, nodeId);
       	                        statements.add(st);
       	                        droppedTables.add(tableName);
                    		}
                    	} else {
    	                	boolean otherPresent = false;
    	                	for(NodeTypeContainer _ntc:_state.getAllEffectiveTypes()){
    	                		if (_ntc.getNodeType().getTableName().equals(tableName)){
    	                			otherPresent = true;
    	                			break;
    	                		}
    	                	}
    	                	if (!otherPresent){
    	                        DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(tableName, FIELD_TYPE_ID, nodeId);
    	                        statements.add(st);
    	                	} else {
    	                		DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(tableName, FIELD_TYPE_ID, nodeId);
    	                		st.addValue(SQLParameter.create(ntc.getNodeType().getPresenceColumn(), false));
    		                    statements.add(st);
    	                	}
                    	}
                        DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_TYPE);
                        st.addCondition(Conditions.eq(TABLE_TYPE__NODE_TYPE, ntc.getNodeTypeId()));
                        st.addCondition(Conditions.eq(FIELD_TYPE_ID, ntc.getNodeId()));
                        statements.add(st);
                    //}
                } else {
                    throw new UnsupportedOperationException();
                }
            }
            
            
            
            //remove node
            if (_state.getStatus() == ItemStatus.Invalidated){
                //1. remove ace
                if (nodeId.equals(_state.getSecurityId())){
                    DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(TABLE_ACE, true);
                    st1.addCondition(Conditions.eq(TABLE_NODE__SECURITY_ID, _state.getSecurityId()));
                    st1.execute(conn);
                    while (st1.hasNext()){
                        RowMap row = st1.nextRow();
                        Long id = row.getLong(FIELD_ID);
                        DatabaseDeleteStatement st3 = DatabaseTools.createDeleteStatement(TABLE_ACE_RESTRICTION, TABLE_ACE_RESTRICTION__ACE_ID, id);
                        statements.add(st3);
                        DatabaseDeleteStatement st2 = DatabaseTools.createDeleteStatement(TABLE_ACE2, FIELD_TYPE_ID, id);
                        statements.add(st2);
                        DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_ACE, FIELD_ID, id);
                        statements.add(st);
                    }
                    st1.close();
                }
                //2.remove fts data
                /*DatabaseSelectAllStatement st1 = DatabaseTools.createSelectAllStatement(Constants.TABLE_FTS_DATA, true);
                st1.addCondition(Conditions.eq(FIELD_TYPE_ID, getNodeId()));
                st1.execute(conn);
                while (st1.hasNext()){
                    RowMap row = st1.nextRow();
                    Long id = row.getLong(FIELD_ID);
                    DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(Constants.TABLE_FTS_DATA, FIELD_ID, id);
                    statements.add(st);
                }
                st1.close();*/
                if (sm.getRepository().isSupportFTS()){
    	            DatabaseUpdateStatement st1 = DatabaseTools.createUpdateStatement(Constants.TABLE_FTS_DATA);
    	            st1.addCondition(Conditions.eq(Constants.FIELD_TYPE_ID, nodeId));
    	            st1.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID, (Long)null));
    	            st1.execute(conn);
    	            st1.close();
                }
                //3. remove node
                DatabaseDeleteStatement st2 = DatabaseTools.createDeleteStatement(TABLE_NODE_LOCK, FIELD_TYPE_ID, nodeId);
                statementsLast.add(st2);
    
                DatabaseDeleteStatement st1 = DatabaseTools.createDeleteStatement(_TABLE_NODE_LOCK_INFO, FIELD_TYPE_ID, nodeId);
                statementsLast.add(st1);
                
                
                DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE, FIELD_ID, nodeId);
                statementsLast.add(st);
                changeState.addRemoveNode(nodeId, _state);
                
            }
            
        _NodeState p = _state.getWeakParentState();
        if (p != null && p.getStatus() != ItemStatus.Invalidated && p.getStatus() != ItemStatus.Destroyed){
        	p.cachedChildNodes.clear();
        }
        }
        return statements;
	}

	public ArrayList<DatabaseStatement> _save1(_ItemState item, DatabaseConnection conn,
			ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog,ChangeState changeState, boolean incVersion, HashMap<String, Long> paths) 
		throws RepositoryException{

		if (item instanceof _NodeState){
			return _saveNode1(((_NodeState)item).getNodeId(), conn, statementsLast, changeLog, changeState, incVersion, paths);
		} else {
			return _saveProperty1((_PropertyState)item, conn, statementsLast, changeLog, new ArrayList<DatabaseStatement>(), changeState, incVersion);
		}
	}

	
	private HashMap<Long, List<_NodeState>> modifiedNodeCache = new HashMap<Long, List<_NodeState>>();
	
	public void _save0(Long nodeId, DatabaseConnection conn, 
			ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog,ChangeState changeState, boolean incVersion, HashMap<String, Long> paths,
			Set<Long> nodeForLock, HashMap<Long, Long> versionCheck, List<FakeJCRTransaction> fakeTransactions) 
		throws RepositoryException{

		//System.out.println(">>>>>>>>>>>>>>>Process Node "+state.getInternalPath());
	    _NodeState state = this.modifiedStates.get(nodeId);
	    /*_NodeState state = this.nodeCache.get(nodeId);
		if (state == null){
		    state = this.modifiedStates.get(nodeId);
		}*/
		if (state != null){
    		if (state.getStatus() == ItemStatus.New){
    		    
    		    
                //create new node in database
                //1.create node record
    			JCRTransaction tr = TransactionHelper.getCurrentTransaction();
    			if (tr == null){
    				tr = new FakeJCRTransaction(conn, state);
    				fakeTransactions.add((FakeJCRTransaction)tr);
    			}
    			state.setCreatedInTransaction(tr); 
    
            	// check parent
            	_NodeState parentState = state.getParent();
            	if (parentState.getParentLockId() != null){
            		_NodeState lockParent = getNodeState(parentState.getParentLockId(), null);
            		if (parentState.getStatus() != ItemStatus.New && !_lockedNodes.contains(lockParent)){
    	        		_lockedNodes.add(lockParent);
    	        		nodeForLock.add(lockParent.getNodeId());
            		}
            		//only if lock is deep
            		RowMap row = _lockedRowMap.get(lockParent.getNodeId());
            		if (row == null) {
            			row = conn.loadRow(Constants._TABLE_NODE_LOCK_INFO, FIELD_TYPE_ID, lockParent.getNodeId());
            			_lockedRowMap.put(lockParent.getNodeId(), row);
            		}
            		if (row.get(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID) != null){
    	        		if (row.get(Constants.TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP) != null && row.getBoolean(Constants.TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP)){
    	        			state.setParentLockId(parentState.getParentLockId());
    	        			state.setLockOwner(parentState.getParentLockOwner());
    	        			HashMap<String, Object> lockOptions = new HashMap<String, Object>();
    	        			if (getLockManager() != null){
    	        			    for(LockManagerListener listener :getLockManager().getListeners()){
    	        			        listener.collectOptions(row, lockOptions);
    	        			    }
    	        			}
    	        			state.setLockOptions(lockOptions);
    	        			
    	        		}
            		} else {
            		    state.setLockOptions(null);
            		}
            	}
        		if (parentState.getStatus() != ItemStatus.New && !_lockedNodes.contains(parentState)){
            		_lockedNodes.add(parentState);
        			//conn.lockRow(Constants.TABLE_NODE, parentState.getNodeId());
            		nodeForLock.add(parentState.getNodeId());
        		}
            	
                //2.TODO create Embedded row
            } else if (state.getStatus() == ItemStatus.Normal){
                // do nothing
            } else if (state.getStatus() == ItemStatus.Modified){
                //lock
            	nodeForLock.add(state.getNodeId());
                //check version
                if (incVersion){
    		            /*RowMap r = conn.loadRow(TABLE_NODE, FIELD_ID, state.getNodeId());
    		            if (!r.getLong(TABLE_NODE__VERSION_).equals(state.getVersion())){
    		            	sm._findNodeState(state.getNodeId(), conn);
    		                throw new InvalidItemStateException(state.safeGetJCRPath()+" is modified by another session");
    		            }*/
                		versionCheck.put(state.getNodeId(), state.getVersion());
                	//} catch (ItemNotFoundException exc){
                //		throw new InvalidItemStateException(state.safeGetJCRPath());
                	//}
                }
                //update version
            } else if (state.getStatus() == ItemStatus.Invalidated){
                ;
            } else if (state.getStatus() == ItemStatus.Destroyed) {
            	return;
            }else {
                throw new UnsupportedOperationException("Unknown state "+state.getStateString());
            }		
		}
        List<_NodeState> nodes = getModifiedNodesDirect(nodeId, true);
        /*if (state != null){
            state.setModifiedNodes(nodes);
        } else {
            System.out.println("Skip for "+nodeId);
        }*/
        modifiedNodeCache.put(nodeId, nodes);
		for (_NodeState n : nodes) {
			if (n.isRemoved() || n.hasParent(nodeId)) {
				_save0(n.getNodeId(), conn, statementsLast,changeLog, changeState, incVersion, paths, nodeForLock, versionCheck, fakeTransactions);
			} 
		}

		
	}
	
	




    private ArrayList<DatabaseStatement> _saveNode1(Long nodeId, DatabaseConnection conn, 
			ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog,
			ChangeState changeState, boolean incVersion, HashMap<String, Long> paths) throws RepositoryException {
		//System.out.println(state.getInternalPath()+" Persist "+state.getNodeId() + " ; state="+ state.getStateString());
        ArrayList<DatabaseStatement> statements = new ArrayList<DatabaseStatement>();

        _NodeState _state = modifiedStates.get(nodeId);
	    if (_state != null){
    		if (log.isDebugEnabled()){
    			LogUtils.debug(log, "Persist "+nodeId + " ; state="+ _state.getStateString());
    		}
                
        //TODO update state (new, removed)
        if (_state.getStatus() == ItemStatus.New){
            if (getRepository().isReducedVersionCheck() && !_state.getDefinition().allowsSameNameSiblings()){
                DatabaseSelectAllStatement st = null;
                  //validate that child not exist
                  //hasChildNode(node, name, checkSecurity)
                  try {
                      st = DatabaseTools.createSelectAllStatement(TABLE_NODE, false);
                      st.addResultColumn(DEFAULT_ROOT_ALIAS + "." + FIELD_ID);
                      // add parent id
                      if (_state.getInternalDepth() == 1) {
                          DatabaseCondition c1 = Conditions.eq(TABLE_NODE__PARENT, _state.getParentId());
                          DatabaseCondition c2 = Conditions.eq(TABLE_NODE__PARENT, getSystemRootId());
                          st.addCondition(Conditions.or(c1, c2));
  
                      } else {
                          st.addCondition(Conditions.eq(TABLE_NODE__PARENT, _state.getParentId()));
                      }
                      // add node name
                      JCRHelper.populateQNameCondition(st, _state.getName(), getNamespaceRegistry());
                      // add index
                      st.addCondition(Conditions.eq(TABLE_NODE__INDEX, new Integer(_state.getIndex())));
                      st.execute(conn);
                      if (st.hasNext()) {
                          throw new ItemExistsException("Item "+_state.getParent().safeGetJCRPath()+" already contains "+_state.getName());
                      }
                  } finally {
                      st.close();
                  }
                  //throw new UnsupportedOperationException();
              }
              
            
            //create new node in database
            //1.create node record
            
            /*DatabaseInsertStatement insert = JCRHelper.createNodeStatement(state.getNodeId(), state.getName(),
                    state.getIndexLong(), state.getNodeTypeId(), state.getInternalPath(), state.getInternalDepthLong(), state.getParentId(), state.getSecurityId(), 
                    state.getParentLockId(), state.getWorkspaceId(), state.getStoreConfigurationId(), getNamespaceRegistry());

            statements.add(insert);*/
        	// check parent
        	/*_NodeState parentState = state.getParent();
        	if (parentState.getParentLockId() != null){
        		_NodeState lockParent = getNodeState(parentState.getParentLockId(), null);
        		if (!lockedNodes.contains(lockParent) && parentState.getStatus() != ItemStatus.New){
	        		lockedNodes.add(lockParent);
	        		conn.lockRow(Constants.TABLE_NODE, lockParent.getNodeId());
        		}
        		//only if lock is deep
        		RowMap row = lockedRowMap.get(lockParent.getNodeId());
        		if (row == null) {
        			row = conn.loadRow(Constants.TABLE_NODE, FIELD_ID, lockParent.getNodeId());
        			lockedRowMap.put(lockParent.getNodeId(), row);
        		}
        		if (row.get(TABLE_NODE__PARENT_LOCK_ID) != null){
	        		if (row.get(Constants.TABLE_NODE__LOCK_IS_DEEP) != null && row.getBoolean(Constants.TABLE_NODE__LOCK_IS_DEEP)){
	        			state.setParentLockId(parentState.getParentLockId());
	        		}
        		}
        	}
    		if (parentState.getStatus() != ItemStatus.New && !lockedNodes.contains(parentState)){
        		lockedNodes.add(parentState);
    			conn.lockRow(Constants.TABLE_NODE, parentState.getNodeId());
    		}*/
        	
           /* boolean foundedInVersionInit = false;
            for(String s:paths.keySet()){
                if (s.startsWith(_state.getInternalPath())){
                	foundedInVersionInit = true;
                }
            }
            if (!foundedInVersionInit){*/
            	changeState.addNewNode(_state);
            /*} else {*/
  //          	_state.resetToNormal();
            //}
            changeLog.added(_state);
            changeLog.addEvents(_state.getEvents());
            changeLog.added(_state.getProperty(QName.JCR_PRIMARYTYPE, true));
            
            //2.TODO create Embedded row
        } else if (_state.getStatus() == ItemStatus.Normal){
            // do nothing
        } else if (_state.getStatus() == ItemStatus.Modified){
            //lock
            changeLog.addEvents(_state.getEvents());
            //conn.lockRow(TABLE_NODE, state.getNodeId());
            //check version
            /*if (incVersion){
            	try {
		            RowMap r = conn.loadRow(TABLE_NODE, FIELD_ID, state.getNodeId());
		            if (!r.getLong(TABLE_NODE__VERSION_).equals(state.getVersion())){
		            	sm._findNodeState(state.getNodeId(), conn);
		                throw new InvalidItemStateException(state.safeGetJCRPath()+" is modified by another session");
		            }
            	} catch (ItemNotFoundException exc){
            		throw new InvalidItemStateException(state.safeGetJCRPath());
            	}
            }*/
            //update version
            _state.isOnlyNeNode = isOnlyNewNodes(_state);
            if (_state.isBasePropertiesChanged()){
                DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(TABLE_NODE);
                st.addCondition(Conditions.eq(FIELD_ID, nodeId));
                //if (incVersion){
                //if (!(getRepository().isReducedVersionCheck() && isOnlyNewNodes(_state))){
                	st.addValue(SQLParameter.create(TABLE_NODE__VERSION_, _state.getVersion().longValue()+1));
                //}
                //}
                //update information in CM_NODE table
                //TODO update all properties
                JCRHelper.populateQName(st, _state.getName(), getNamespaceRegistry());
                st.addValue(SQLParameter.create(TABLE_NODE__INDEX, _state.getIndexLong()));
                st.addValue(SQLParameter.create(TABLE_NODE__INDEX_MAX, _state.getSnsMax()));
                st.addValue(SQLParameter.create(TABLE_NODE__NODE_PATH, _state.getInternalPath()));
                st.addValue(SQLParameter.create(TABLE_NODE__NODE_DEPTH, _state.getInternalDepthLong()));
                st.addValue(SQLParameter.create(TABLE_NODE__PARENT, _state.getParentId()));
                st.addValue(SQLParameter.create(TABLE_NODE__SECURITY_ID, _state.getSecurityId()));
                statements.add(st);
                
                changeState.addBasePropertyChanged(_state, st);
            }  else {
                if (!(getRepository().isReducedVersionCheck() && isOnlyNewNodes(_state))){
            	    changeState.addSetNodeVersion(_state.getNodeId(), _state.getVersion().longValue()+1);
                }
            }
            
        } else if (_state.getStatus() == ItemStatus.Invalidated){
            // do nothing
            changeLog.deleted(_state);
            changeLog.addEvents(_state.getEvents());

            DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(TABLE_NODE);
            st.addCondition(Conditions.eq(FIELD_ID, nodeId));
            st.addValue(SQLParameter.create(TABLE_NODE__INDEX, conn.nextId()));
            st.execute(conn);
            for(ParentNode pn : _state.getParentNodes()){
            	pn.setRemoved();
            }
            
        } else if (_state.getStatus() == ItemStatus.Destroyed) {
        	return statements;
        }else {
            throw new UnsupportedOperationException("Unknown state "+_state.getStateString());
        }

        List<SecurityModificationEntry> modifiedACE = _state.getModifiedSecurity();
        if (modifiedACE.size() > 0){
            HashMap<String, Long> ids = new HashMap<String, Long>();
            HashSet<Long> removedIds = new HashSet<Long>();
            for(SecurityModificationEntry sme:modifiedACE){
                if (sme.getAction() == SecurityModificationEntry.RESET){
                    
                    DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, false);
                    st.addResultColumn(Constants.FIELD_ID);
                    st.addCondition(Conditions.eq(TABLE_NODE__SECURITY_ID, _state.getSecurityId()));
                    
                    st.execute(conn);
                    List<RowMap> result = st.getAllRows();
                    ArrayList<Long> ids2 = new ArrayList<Long>();
                    for(RowMap r:result){
                    	ids2.add(r.getLong(Constants.FIELD_ID));
                    }
                    removedIds.addAll(ids2);
                    
                    DatabaseDeleteStatement st1 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE2);
                    st1.addCondition(Conditions.in(FIELD_TYPE_ID, st));
                    statements.add(st1);
                    
                    st1 = DatabaseTools.createDeleteStatement(Constants.TABLE_ACE_RESTRICTION);
                    st1.addCondition(Conditions.in(Constants.TABLE_ACE_RESTRICTION__ACE_ID, st));
                    statements.add(st1);
                    
                    DatabaseDeleteStatement dSt = DatabaseTools.createDeleteStatement(TABLE_ACE, TABLE_NODE__SECURITY_ID, _state.getSecurityId());
                    statements.add(dSt);
                    
                } else if (sme.getAction() == SecurityModificationEntry.SET_PERMISSION){
                    String tId = "g_"+sme.getGroupId()+"__u_"+sme.getUserId()+"__c_"+sme.getContextId();
                    Long __id = ids.get(tId);
                    //if (getRepository().getSecurityCopyType() == SecurityCopyType.Copy){
	                    //Long seqId = conn.nextId();
	                    //try to find ACE
	                    if (__id == null){
	                    	for(SecurityEntry se :  _state.getACEList()){
	                    		if ( (se.isUserEntry() && se.getUserId().equals(sme.getUserId())) ||
	                    				( se.isGroupEntry() && se. getGroupId().equals(sme.getGroupId())) ) {
	                    			if ((se.getContextId() == null && sme.getContextId() == null) | (se.getContextId() != null && se.getContextId().equals(sme.getContextId()) )){
		                    			if (se.getId() != null){
		                    				if (!removedIds.contains(se.getId())){
			                    				__id = se.getId();
			                    				ids.put(tId, __id);
			                    				break;
		                    				}
		                    			}
	                    			}
	                    		}
	                    			
	                    	}
	                    }
	                    
	                    if (__id == null){
	                        __id = conn.nextId();
	                        DatabaseInsertStatement ins = DatabaseTools.createInsertStatement(TABLE_ACE);
	                        ins.addValue(SQLParameter.create(FIELD_ID, __id));
	                        ins.addValue(SQLParameter.create(TABLE_NODE__SECURITY_ID, _state.getSecurityId()));
	                        ins.addValue(SQLParameter.create(Constants.TABLE_ACE__USER_ID,sme.getUserId()));
	                        ins.addValue(SQLParameter.create(Constants.TABLE_ACE__GROUP_ID,sme.getGroupId()));
	                        ins.addValue(SQLParameter.create(Constants.TABLE_ACE__CONTEXT_ID,sme.getContextId()));
	                        ids.put(tId, __id);
	                        statements.add(ins);
	                        
	                        DatabaseInsertStatement ins2 = DatabaseTools.createInsertStatement(TABLE_ACE2);
	                        ins2.addValue(SQLParameter.create(Constants.FIELD_TYPE_ID,__id));
	                        statements.add(ins2);
	
	                    }
	                        
	                    DatabaseUpdateStatement udp = DatabaseTools.createUpdateStatement(TABLE_ACE, FIELD_ID, __id);
	                    udp.addValue(SQLParameter.create(sme.getPermission().getColumnName(),sme.getValue()));
	                    udp.addValue(SQLParameter.create(sme.getPermission().getColumnName()+Constants.TABLE_ACE___DIRECT_SUFFIX,sme.isDirect()));
	                    
	                    DatabaseUpdateStatement udp2 = DatabaseTools.createUpdateStatement(TABLE_ACE2, FIELD_TYPE_ID, __id);
	                    udp2.addValue(SQLParameter.create(sme.getPermission().getColumnName()+Constants.TABLE_ACE2___PARENT_SUFFIX,sme.getValueParent()));
	                    udp2.addValue(SQLParameter.create(sme.getPermission().getColumnName()+Constants.TABLE_ACE2___SEQUENCE_SUFFIX,__id));
	                    udp2.addValue(SQLParameter.create(sme.getPermission().getColumnName()+Constants.TABLE_ACE2___FROM_SUFFIX,sme.getValueFrom() == null ? sme.getPermission().getExportName():sme.getValueFrom()));
	                    
	                    statements.add(udp);
	                    statements.add(udp2);
                   /* } else if (getRepository().getSecurityCopyType() == SecurityCopyType.Inherit){
                    	DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(TABLE_NODE);
                        st.addCondition(Conditions.eq(FIELD_ID, nodeId));
                        Long sId = _state.getParent().getSecurityId();
                        st.addValue(SQLParameter.create(Constants.TABLE_NODE__SECURITY_ID, sId));
                        _state.setSecurityId(sId);
                    } else {
                    	throw new UnsupportedOperationException();
                    }*/
                }  else if (sme.getAction() == SecurityModificationEntry.REMOVE_ACE){
                    throw new UnsupportedOperationException();
                }
            }
            
            _state.getModifiedSecurity().clear();
        }        
        
        //3.TODO update parents
            for(ParentNode pn: _state.getParentNodes()){
                if (pn.getState().equals(ItemStatus.New)){
                	/*if (pn.getId() == null) {
	                    Long nextId = conn.nextId();
	                    pn.setId(nextId);
                	}*/
                    /*DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_NODE_PARENT);
                    insert.addValue(SQLParameter.create(FIELD_ID, nextId));
                    insert.addValue(SQLParameter.create(FIELD_TYPE_ID, pn.getChildId()));
                    insert.addValue(SQLParameter.create(TABLE_NODE_PARENT__PARENT_ID, pn.getParentId()));
                    insert.addValue(SQLParameter.create(TABLE_NODE_PARENT__LEVEL, pn.getPosition()));
                    statements.add(insert);*/
                    changeState.addNewParentNode(pn);
                } else if (pn.getState().equals(ItemStatus.Normal)){
                    //do nothing
                } else if (pn.getState().equals(ItemStatus.Invalidated)){
                    //if (pn.getId() != null) {
                        changeState.addRemovedParentNode(pn);
                        /*DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(TABLE_NODE_PARENT, FIELD_ID, pn.getId());
                        statements.add(st);*/
                    //}
                } else {
                    changeState.addRemovedParentNode(pn);
                    //throw new UnsupportedOperationException();
                }
            }
        
        //TODO update Types & Details (Create) 
        for(NodeTypeContainer ntc : _state.getAllTypes()){
            //TODO use switch
            if (ntc.getState() == ItemStatus.New){
                //create type row
                //Long typeId = conn.nextId();
                if (ntc.getFromTypeId() == null){
                    throw new RepositoryException("FromTypeId can't be empty in NodeTypeContainer");
                }
                
                /*DatabaseInsertStatement insert = JCRHelper.createNodeTypeStatement(state.getNodeId(), typeId, ntc.getNodeTypeId(), ntc.getFromTypeId());
                statements.add(insert);*/
                //ntc.setId(typeId);
                changeState.addNewType(ntc);

                boolean tablePresent = false;
                String tableName = ntc.getNodeType().getTableName();
                for(NodeTypeContainer _ntc:_state.getAllTypes()){
                	if (_ntc.getState().equals(ItemStatus.Normal) && _ntc.getNodeType().getTableName().equals(tableName)){
                		tablePresent = true;
                	}
                }
                
                DatabaseStatement st = JCRHelper.findPropertyStatement(_state, statements, tableName);
                //for single value property
                if (st == null){
                }
                if (st == null){
                	if (tablePresent){
                    	st = DatabaseTools.createUpdateStatement(tableName,FIELD_TYPE_ID,nodeId);
                	} else {
                        st = JCRHelper.createNodeTypeDetailsStatement(nodeId, ntc.getNodeType().getTableName());
                	}
                	statements.add(st);
                }
                ((ValueChangeDatabaseStatement)st).addValue(SQLParameter.create(ntc.getNodeType().getPresenceColumn(), true));                
                
                if (ntc.getNodeTypeId().equals(getRepository().getNodeTypeHelper().getOcrTypeId())){
                	scheduleOcrRecognition(_state, ntc, changeState);
                }
/*                DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(TABLE_TYPE);
                insert.addValue(SQLParameter.create(FIELD_ID, typeId));
                insert.addValue(SQLParameter.create(TABLE_TYPE__NODE_TYPE, ntc.getNodeTypeId()));
                insert.addValue(SQLParameter.create(TABLE_TYPE__NODE_ID, getNodeId()));
                insert.addValue(SQLParameter.create(TABLE_TYPE__FROM_NODE_TYPE, ntc.getFromTypeId()));
                statements.add(insert);
                ntc.id = typeId;
                //creat type detail row
                String tableName = ntc.getNodeType().getTableName();
                insert = DatabaseTools.createInsertStatement(tableName);
                insert.addValue(SQLParameter.create(FIELD_TYPE_ID, getNodeId()));
                statements.add(insert);
  */              
                
            } else if (ntc.getState() == ItemStatus.Normal){
                //do nothing
            } else if (ntc.getState() == ItemStatus.Invalidated){
                // do nothing
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        initVersionHistories(_state, paths);  
        
        //TODO save properties existing propertie
        if (!ItemStatus.Destroyed.equals(_state.getStatus())){
	        for(_PropertyState p:_state.getAllProperties()){
	            if (!p.getName().equals(QName.JCR_PRIMARYTYPE) && !p.getName().equals(QName.JCR_MIXINTYPES)){
	                statements.addAll(_saveProperty1(p,conn, new ArrayList<DatabaseStatement>(), changeLog, statements, changeState, incVersion));
	            } 
	            if (p.isNew()){
	                changeLog.added(p);
	                //changeLog.addEvents(state.getEvents());	                
	            }
	            if (p.isModified() && p.isTriggerEvents()){
	                changeLog.modified(p);
	                //changeLog.addEvents(state.getEvents());	                
	            }
	            if (p.isRemoved()){
	            	changeLog.deleted(p);
	                //changeLog.addEvents(state.getEvents());	            	
	            }
	        }
        }

        //TODO save child nodes
        //List<_NodeState> nodes = getModifiedNodesDirect(state, true);
		//state.setModifiedNodes(nodes);
        //List<_NodeState> nodes = state.getModifiedNodes();
	    }
        List<_NodeState> nodes = modifiedNodeCache.get(nodeId);
		for (_NodeState n : nodes) {
			if (n.isRemoved() || n.hasParent(nodeId)) {
				statements.addAll(_save1(n, conn, statementsLast,
						changeLog, changeState, incVersion, paths));
			} else {
			}
		}

        return statements;
	}
	






	private ArrayList<DatabaseStatement> _saveProperty1(_PropertyState state, DatabaseConnection conn, 
				ArrayList<DatabaseStatement> statementsLast, ChangeLog changeLog,
				ArrayList<DatabaseStatement> existingStatements, ChangeState changeState, boolean incVersion) throws RepositoryException {
        ArrayList<DatabaseStatement> result = new ArrayList<DatabaseStatement>();
        if (state.getStatus().equals(ItemStatus.New)){
            //create create statement
        	if (!state.getParent().getStatus().equals(ItemStatus.Destroyed) && 
        			!state.getParent().getStatus().equals(ItemStatus.Invalidated)){
	            result.addAll(createPropertyCreateStatement(state,conn,existingStatements, changeState));
	            if (state.isTriggerEvents()){
	                changeLog.added(state);
	            }
        	}
        } else if (state.getStatus().equals(ItemStatus.Normal)){
        	
        } else if (state.getStatus().equals(ItemStatus.Modified)){
            //create update statement
            result.addAll(createPropertyUpdateStatement(state,conn,existingStatements, changeState));
            if (state.isTriggerEvents()){
                changeLog.modified(state);
            }
        } else if (state.getStatus().equals(ItemStatus.Invalidated)){
            result.addAll(createPropertyDeleteStatement(state,conn,existingStatements, changeState));
            state.getPrimaryPath();
            changeLog.deleted(state);
        } else if (state.getStatus().equals(ItemStatus.Destroyed)){
            //throw new UnsupportedOperationException("Object already destroyed");
        	//do nothing
        } else {
            throw new UnsupportedOperationException();
        }
        state.setTriggerEvents(false);
        return result;	
    }
	
    private ArrayList<DatabaseStatement> createPropertyDeleteStatement(_PropertyState state,DatabaseConnection conn, ArrayList<DatabaseStatement> existingStatements, ChangeState changeState) throws RepositoryException {
        ArrayList<DatabaseStatement> result =  JCRHelper.createRemovedPropertyStatement(state, state.getDefinition(), getStoreContainer(), existingStatements);
        updateIndexableData(state.getParent(),state,result, conn, false, true, changeState);
        return result;
    }
    
    

	private ArrayList<DatabaseStatement> createPropertyCreateStatement(_PropertyState state,DatabaseConnection conn, ArrayList<DatabaseStatement> existingStatements, ChangeState changeState) throws RepositoryException {
        ArrayList<DatabaseStatement> result = JCRHelper.createNewPropertyStatement(state, state.getDefinition(), conn, getNamespaceRegistry(), getStoreContainer(),existingStatements, changeState); 
        //check for content
        updateIndexableData(state.getParent(),state,result, conn, true, false,changeState);
        return result;
    }

	
	
    private Collection<DatabaseStatement> createPropertyUpdateStatement(_PropertyState state, DatabaseConnection conn, ArrayList<DatabaseStatement> existingStatements, ChangeState changeState) throws ItemNotFoundException, AccessDeniedException, ValueFormatException, UnsupportedRepositoryOperationException, RepositoryException {
        ArrayList<DatabaseStatement> result = new ArrayList<DatabaseStatement>();
        PropertyDefinitionImpl definition = state.getDefinition();
        if (definition.isUnstructured()){
            if (definition.isMultiple()){
                //TODO rewrite this
                //update values
                InternalValue[] iv = state.getInitialValues();
                for(int i = 0 ; i < iv.length ; i++){
                    //TODO use batch delete be property id
                    DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(Constants.TABLE_NODE_UNSTRUCTURED_VALUES, Constants.FIELD_ID, iv[i].getSQLId());
                    result.add(st);
                }
                //Value values[] = getValues();
                //PropertyState state = (PropertyState) _getItemState();
                InternalValue[] values = state.getValues();
                for(int i = 0 ; i < values.length ; i++){
                    //save unstructured property
                    //TODO implement order
                    if (values[i].getSQLId() == null){
                        values[i].setSQLId(conn.nextId());
                    }
                    Value v =  values[i].toJCRValue(getNamespaceResolver());
                    DatabaseInsertStatement st = DatabaseTools.createInsertStatement(Constants.TABLE_NODE_UNSTRUCTURED_VALUES);
                    st.addValue(SQLParameter.create(Constants.FIELD_ID, values[i].getSQLId()));
                    st.addValue(SQLParameter.create(Constants.TABLE_NODE_UNSTRUCTURED_VALUES__PROPERTY, state.getUnstructuredPropertyId()));
                    st.addValue(SQLParameter.create(TABLE_NODE_UNSTRUCTURED__PROP_DEF, definition.getSQLId()));
                    st.addValue(SQLParameter.create(FIELD_TYPE_ID, state.getNodeId()));

                    st.addValue(SQLParameter._create(getStoreContainer() ,JCRHelper.getValueColumn(values[i].getType()), JCRHelper.getValueObject(v), values[i]));
                    if (v.getType() == PropertyType.LONG || v.getType() == PropertyType.DATE){
                        st.addValue(SQLParameter._create(getStoreContainer() ,TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE, v.getDouble(), values[i]));
                    }
                    result.add(st);
                }
                
                
            } else {
                Value v = JCRHelper.getPropertyValue(state, getNamespaceResolver());
                DatabaseUpdateStatement st = DatabaseTools.createUpdateStatement(Constants.TABLE_NODE_UNSTRUCTURED, Constants.FIELD_ID, state.getUnstructuredPropertyId());
                //value
                st.addValue(SQLParameter.create(Constants.TABLE_NODE_UNSTRUCTURED__TYPE, v.getType()));
                st.addValue(SQLParameter._create(getStoreContainer() ,JCRHelper.getValueColumn(v.getType()), JCRHelper.getValueObject(v), state.getValues()[0]));
                if (v.getType() == PropertyType.LONG || v.getType() == PropertyType.DATE){
                    st.addValue(SQLParameter._create(getStoreContainer() ,TABLE_NODE_UNSTRUCTURED__DOUBLE_VALUE, v.getDouble(), null));
                }
                result.add(st);
                //throw new UnsupportedOperationException();
            }
        } else {
            Value v = JCRHelper.getPropertyValue(state, getNamespaceResolver());
            String tableName = ((NodeTypeImpl)definition.getDeclaringNodeType()).getTableName();
            String columnName = definition.getColumnName();
            DatabaseStatement st = JCRHelper.findPropertyStatement(state, existingStatements, tableName);            
            //for single value property
            if (st == null){
            	st = DatabaseTools.createUpdateStatement(tableName,Constants.FIELD_TYPE_ID,state.getParent().getNodeId());
                result.add(st);
            }
            ((ValueChangeDatabaseStatement)st).addValue(SQLParameter._create(getStoreContainer() ,columnName ,JCRHelper.getValueObject(v), state.getValues()[0]));
            //if (true){
            //    throw new UnsupportedOperationException();
            //}
        }
        updateIndexableData(state.getParent(),state, result, conn, true, true, changeState);
        return result;
	}




	/**
     * Initializes the version history of all new nodes of node type
     * <code>mix:versionable</code>.
     * <p/>
     * Called by {@link #save()}.
	 * @param paths 
     *
     * @param iter
     * @return true if this call generated new transient state; otherwise false
     * @throws RepositoryException
     */
    _NodeState initVersionHistories(_NodeState state, HashMap<String, Long> paths) throws RepositoryException {
    	if (allowVersionManager()){
            if (state.isNode() && (state.getStatus() == ItemStatus.New || state.getStatus() == ItemStatus.Modified)) {
                if (state.isNodeType(QName.MIX_VERSIONABLE)) {
                    if (!state.hasProperty(QName.JCR_VERSIONHISTORY)) {
                        VersionManager vMgr = getVersionManager();
                        //NodeState nodeState = (NodeState) itemState;
                        /**
                         * check if there's already a version history for that
                         * node; this would e.g. be the case if a versionable
                         * node had been exported, removed and re-imported with
                         * either IMPORT_UUID_COLLISION_REMOVE_EXISTING or
                         * IMPORT_UUID_COLLISION_REPLACE_EXISTING;
                         * otherwise create a new version history
                         */
                        VersionHistory vh = vMgr.getVersionHistory(buildNodeId(state));
                        //boolean created = false;
                        if (vh == null) {
                        	NodeId ss = buildNodeId(state);
                        	QName pTypeName = state.getPrimaryTypeName();
                        	Set<QName> mixins = state.getMixinTypeNames();
                            vh = vMgr.createVersionHistory(ss, pTypeName, mixins, paths);
                            //created = true;
                        }
                        
                        String rootVersionUUID = ((VersionHistoryImpl)vh).getRootVersionState().getInternalUUID();
                        state.internalSetProperty(QName.JCR_VERSIONHISTORY, InternalValue.create(new UUID(vh.getUUID()), false), true, true, true);
                        state.internalSetProperty(QName.JCR_BASEVERSION, InternalValue.create(new UUID(rootVersionUUID), false), true, true, true);
                        state.internalSetProperty(QName.JCR_ISCHECKEDOUT, InternalValue.create(true), true, true, true);
                        state.internalSetProperty(QName.JCR_PREDECESSORS, 
                                new InternalValue[]{InternalValue.create(new UUID(rootVersionUUID), false)},
                                PropertyType.REFERENCE, true);
                        
                        
                        return ((VersionManagerImpl)vMgr).getHistoryRoot().getNodeState();
                        //TODO do we need this ?
                        /*if (created){
                            session.getNodeManager().evict((VersionHistoryImpl) vh);
                            session.getNodeManager().evict((NodeImpl) vh.getRootVersion());
                        }*/
                    }
                }
            }
    	}
    	return null;
    }  
    
	



	protected abstract boolean allowVersionManager() ;

	public NodeId buildNodeId(_NodeState state) {
		return new NodeId(state.getNodeId(), state.getInternalUUID());
	}

	private List<_ItemState> getDirtyItems(Long nodeId)
			throws RepositoryException {
		List<_ItemState> dirty = new ArrayList<_ItemState>();
		//if (item.isNode()) {
			//dirty.addAll(getModifiedNodesAll(((_NodeState) item).getNodeId()));
		    dirty.addAll(getModifiedNodesAll(nodeId));
			if (modifiedStates.containsKey(nodeId)) {
				// TODO implements root validation
				// if (!this.equals(session.getRootNode())){
				dirty.add(modifiedStates.get(nodeId));
				// }
			}
		/*} else {*/
			/*if (item.getParent().isModified()) {
				// TODO implements root validation
				_NodeState parent = item.getParent();
				if (!parent.getNodeId().equals(
						getRootNodeId().getId())) {
					dirty.add(parent);
				}
			}*/
			/*throw new UnsupportedOperationException();
		}*/

		if (dirty.size() == 0) {
			// no transient items, nothing to do here
			return null;
		}

		// TODO addonly dirty properties
		ArrayList<_PropertyState> dirtyProps = new ArrayList<_PropertyState>();
		for (_ItemState state : dirty) {
			_NodeState n = (_NodeState) state;
			for (_PropertyState p : n.getAllProperties()) {
				if (p.isModified() || p.isNew() || p.isRemoved()) {
					dirtyProps.add(p);
				}
			}
		}
		dirty.addAll(dirtyProps);
		return dirty;
	}

	public List<_NodeState> getModifiedNodesAll(Long nodeId)
			throws RepositoryException {
		// TODO use updatable tree for modified node
		ArrayList<_NodeState> result = new ArrayList<_NodeState>();
		for (_NodeState n : modifiedStates.values()) {
			if (n.hasParentWithDeleted(nodeId)) {
				result.add(n);
			}
		}
		return result;
	}

	private void validateTransientItems(List<_ItemState> dirtyItems)
			// , Iterator removedIter
			throws AccessDeniedException, ConstraintViolationException,
			RepositoryException {
		/**
		 * the following validations/checks are performed on transient items:
		 * 
		 * for every transient item: - if it is 'modified' check the WRITE
		 * permission
		 * 
		 * for every transient node: - if it is 'new' check that its node type
		 * satisfies the 'required node type' constraint specified in its
		 * definition - check if 'mandatory' child items exist
		 * 
		 * for every transient property: - check if the property value satisfies
		 * the value constraints specified in the property's definition
		 * 
		 * note that the protected flag is checked in Node.addNode/Node.remove
		 * (for adding/removing child entries of a node), in
		 * Node.addMixin/removeMixin (for mixin changes on nodes) and in
		 * Property.setValue (for properties to be modified).
		 */

		// TODO uncomment me
		// AccessManager accessMgr = session.getAccessManager();
		// walk through list of dirty transient items and validate each
		for(_ItemState item:dirtyItems){

			if (item.getStatus() != ItemStatus.New) {
				// transient item is not 'new', therefore it has to be
				// 'modified'

				// check WRITE permission
				// TODO uncomment me
				/*
				 * ItemId id = itemState.getId(); if (!accessMgr.isGranted(id,
				 * AccessManager.WRITE)) { String msg =
				 * itemMgr.safeGetJCRPath(id) + ": not allowed to modify item";
				 * log.debug(msg); throw new AccessDeniedException(msg); }
				 */
			}

			if (item.isNode()) {
				// the transient item is a node
				// NodeState nodeState = (NodeState) itemState;
				// ItemId id = nodeState.getId();
				_NodeState node = (_NodeState) item;
				NodeDefinition def = node.getDefinition();
				// primary type
				NodeTypeImpl pnt = (NodeTypeImpl) node.getPrimaryNodeType();
				// effective node type (primary type incl. mixins)
				EffectiveNodeType ent = node.getEffectiveNodeType();
				/**
				 * if the transient node was added (i.e. if it is 'new'), check
				 * its node's node type against the required node type in its
				 * definition
				 */
				if (node.getStatus() == ItemStatus.New) {
					NodeType[] nta = def.getRequiredPrimaryTypes();
					for (int i = 0; i < nta.length; i++) {
						NodeTypeImpl ntReq = (NodeTypeImpl) nta[i];
						if (!(pnt.getQName().equals(ntReq.getQName()) || pnt
								.isDerivedFrom(ntReq.getQName()))) {
							/**
							 * the transient node's primary node type does not
							 * satisfy the 'required primary types' constraint
							 */
							String msg = node.safeGetJCRPath()
									+ " must be of node type "
									+ ntReq.getName();
							log.debug(msg);
							throw new ConstraintViolationException(msg);
						}
					}
				}

				if (node.getStatus() == ItemStatus.Invalidated) {
					if (node.getDefinition().isMandatory()) {
						// allowed only if parent node already deleted
						if (node.getParent().getStatus() != ItemStatus.Invalidated
								&& !hasChildNode(node.getParent(),node.getName(), false)) {
							String msg = node.safeGetJCRPath()
									+ "is mandatory node";
							log.debug(msg);
							throw new ConstraintViolationException(msg);
						}
					}
				}
				if (node.getStatus() != ItemStatus.Invalidated && node.getStatus() != ItemStatus.Destroyed) {
					// mandatory child properties
					PropDef[] pda = ent.getMandatoryPropDefs();
					for (int i = 0; i < pda.length; i++) {
						PropDef pd = pda[i];
						if (pd.getDeclaringNodeType().equals(
								QName.MIX_VERSIONABLE)) {
							/**
							 * todo FIXME workaround for mix:versionable: the
							 * mandatory properties are initialized at a later
							 * stage and might not exist yet
							 */
							continue;
						}
						if (!node.hasProperty(pd.getName(), true)) {
							String msg = node.safeGetJCRPath()
									+ ": mandatory property " + pd.getName()
									+ " does not exist in nodetype "
									+ pd.getDeclaringNodeType();
							log.debug(msg);
							throw new ConstraintViolationException(msg);
						}
					}
					// mandatory child nodes
					NodeDef[] cnda = ent.getMandatoryNodeDefs();
					for (int i = 0; i < cnda.length; i++) {
						NodeDef cnd = cnda[i];
						if (!hasChildNode(node, cnd.getName(), false)) {
							String msg = node.safeGetJCRPath()
									+ ": mandatory child node " + cnd.getName()
									+ " does not exist in nodetype "
									+ cnd.getDeclaringNodeType();
							log.debug(msg);
							throw new ConstraintViolationException(msg);
						}
					}
				}
			} else {
				// the transient item is a property
				// PropertyState propState = (PropertyState) itemState;
				// ItemId propId = propState.getId();
				_PropertyState prop = (_PropertyState) item;
				//PropertyState propState = prop.getPropertyState();
				PropertyDefinitionImpl def = (PropertyDefinitionImpl) prop
						.getDefinition();

				/**
				 * check value constraints (no need to check value constraints
				 * of protected properties as those are set by the
				 * implementation only, i.e. they cannot be set by the user
				 * through the api)
				 */
				if (!def.isProtected() && (prop.getStatus().equals(ItemStatus.New ) || prop.getStatus().equals(ItemStatus.Modified))) {
					String[] constraints = def.getValueConstraints();
					if (constraints != null) {
						InternalValue[] values = prop.getValues();
						try {
							NodeTypeImpl.checkSetPropertyValueConstraints(def,
									values);
						} catch (RepositoryException e) {
							// repack exception for providing verboser error
							// message
							String msg = prop.safeGetJCRPath() + ": "
									+ e.getMessage();
							log.debug(msg);
							throw new ConstraintViolationException(msg);
						}

						/**
						 * need to manually check REFERENCE value constraints as
						 * this requires a session (target node needs to be
						 * checked)
						 */
						if (constraints.length > 0
								&& (def.getRequiredType() == PropertyType.REFERENCE || def.getRequiredType() == PropertyType283.WEAKREFERENCE)) {
							for (int i = 0; i < values.length; i++) {
								boolean satisfied = false;
								try {
									UUID targetUUID = (UUID) values[i].internalValue();
									_NodeState targetNode ;
									if (def.getRequiredType() == PropertyType.REFERENCE){
										targetNode = getItemByUUID(targetUUID.toString(), false);
									} else {
										//weakreference
										try {
											targetNode = getItemByUUID(targetUUID.toString(), false);
										} catch (ItemNotFoundException e){
											continue;
										}
									}
									/**
									 * constraints are OR-ed, i.e. at least one
									 * has to be satisfied
									 */
									for (int j = 0; j < constraints.length; j++) {
										/**
										 * a REFERENCE value constraint
										 * specifies the name of the required
										 * node type of the target node
										 */
										String ntName = constraints[j];
										if (targetNode.isNodeType(createQName(ntName))) {
											satisfied = true;
											break;
										}
									}
								} catch (RepositoryException re) {
									String msg = prop.safeGetJCRPath() + ": failed to check REFERENCE value constraint";
									log.debug(msg);
									throw new ConstraintViolationException(msg, re);
								}
								if (!satisfied) {
									String msg = prop.safeGetJCRPath()
											+ ": does not satisfy the value constraint "
											+ constraints[0]; // just report
																// the 1st
									log.debug(msg);
									throw new ConstraintViolationException(msg);
								}
							}
						}
					}
				}

				/**
				 * no need to check the protected flag as this is checked in
				 * PropertyImpl.setValue(Value)
				 */
			}
		}

		// TODO uncomment me
		/*
		 * // walk through list of removed transient items and check REMOVE
		 * permission while (removedIter.hasNext()) { ItemState itemState =
		 * (ItemState) removedIter.next(); ItemId id = itemState.getId(); //
		 * check WRITE permission if (!accessMgr.isGranted(id,
		 * AccessManager.REMOVE)) { String msg = itemMgr.safeGetJCRPath(id) + ":
		 * not allowed to remove item"; log.debug(msg); throw new
		 * AccessDeniedException(msg); } }
		 */
	}
	
	private QName createQName(String rawName) throws RepositoryException{
		QName result;
		try {
			result = QName.fromJCRName(rawName, getNamespaceResolver());
		} catch (IllegalNameException e) {
			throw new RepositoryException(e.getMessage());
		} catch (UnknownPrefixException e) {
			throw new RepositoryException(e.getMessage());
		}
		return result;
	}

	public boolean hasChildNode(_NodeState node, QName name, boolean checkSecurity) throws RepositoryException{
        return hasChildNode(node, name, 1, checkSecurity);
    }
    
    public boolean hasChildNode(_NodeState node,QName name, int index, boolean checkSecurity) throws RepositoryException{
        return getNode(node, name, index, checkSecurity) != null;
    }

    public _NodeState getChildNode(_NodeState node,QName name) throws ItemNotFoundException, RepositoryException {
        return getNode(node, name, 1, true);
    }
    
    _NodeState getNode(_NodeState node, QName name, int index, boolean checkSecurity) throws RepositoryException {
    	_NodeState childState =  getChildNode(node, name, index, checkSecurity);
    	return childState;
    }
    
    private void updateIndexableData(_NodeState node, _PropertyState prop, ArrayList<DatabaseStatement> result, DatabaseConnection conn, 
    		boolean create, boolean delete, ChangeState changeState) throws ItemNotFoundException, AccessDeniedException, ValueFormatException, RepositoryException {
    	    	if (PropertyType.BINARY != prop.getType()){
    	    		return;
    	    	}
    	    	PropertyDefinitionImpl definition = prop.getDefinition();
    	    	
    	        //if (parent.getPrimaryTypeName().equals(Constants.VF_RESUORCE) || parent.getPrimaryTypeName().equals(QName.NT_RESOURCE)){
    	            //if (getQName().equals(Constants.VF_RESOURCE_DATA) || getQName().equals(Constants.JCR_RESOURCE_DATA)){
    		        if (delete){
    		        	changeState.addRemoveFTSProperty(prop);
    		            //find fts Id
    		        }
    	      
    	            if (create){
    	            	BLOBFileValue v = (BLOBFileValue) prop.getValues()[0].internalValue();
    	            	Boolean processFTS = null;
    	                //if (definition.unwrap().isFullTestSearch()){
    	               // 	processFTS = true;
    	                //}
    	            	if (v.isFtsProcessing() != null){
    	            		processFTS = v.isFtsProcessing();
    	            	} else {
    	                    processFTS = definition.unwrap().isFullTextSearch();
    	                }
    	                
    	                
    	            	if (processFTS){
        		            changeState.addNewFTSProperty(prop);

    	            	}
    	            }
    	            
    	            
    	        //}
    	    }



	public void registerNewState(_NodeState state) throws ConstraintViolationException, RepositoryException {
		modifiedStates.put(state.getNodeId(), state);
		assignSession(state);
		evictFromModiffiedCache(state);
	}

	public void registerModifiedState(_NodeState state) {
		modifiedStates.put(state.getNodeId(), state);
		evictFromModiffiedCache(state);
	}

	public Long nextId() throws RepositoryException {
		return sessionInfo.getRepository().nextId();
	}

	public _NodeState getItemByUUID(String uuid, boolean getDeleted) throws RepositoryException {
		//1.find in local nodes
		boolean foundDeleted = false;
		_NodeState deletedState = null;
		for(_NodeState state:modifiedStates.values()){
			if (uuid.equals(state.getInternalUUID())){
				if (getDeleted || state.getStatus().equals(ItemStatus.Normal)
						|| state.getStatus().equals(ItemStatus.New)
						|| state.getStatus().equals(ItemStatus.Modified)) {
					return state;
				}  else {
					foundDeleted = true;
					deletedState = state;
				}
			}
		}
		for(_NodeState state:nodeCache.values()){
			if (uuid.equals(state.getInternalUUID())){
				if (getDeleted || state.getStatus().equals(ItemStatus.Normal)
						|| state.getStatus().equals(ItemStatus.New)
						|| state.getStatus().equals(ItemStatus.Modified)) {
					return state;
				}  else {
					foundDeleted = true;
					deletedState = state;
				}
			}
		}
		if (foundDeleted && getDeleted){
			return deletedState;
		}
		if (foundDeleted && !getDeleted){
			throw new ItemNotFoundException(uuid);
		}
		//2. ask database
		Long id = this.sm.getNodeIdByUUID(uuid, getConnection());
		return getNodeState(id, null);
	}
	

    public IdIterator getChildNodesId(_NodeState state, boolean checkSecurity, QName childName) throws RepositoryException {
        return getChildNodesId(state, checkSecurity, childName, null);
    }

    public IdIterator getChildNodesId(_NodeState state, boolean checkSecurity, QName childName, QName[] nodetypes) throws RepositoryException {
    	return getChildNodesId(state, checkSecurity, childName, nodetypes, 0, -1);
    }
    public IdIterator getChildNodesId(_NodeState state, boolean checkSecurity, QName childName, QName[] nodetypes, int offset, int limit) throws RepositoryException {
        //TODO implement nodetype filtering
        ArrayList<Long> _result = new ArrayList<Long>();
        ArrayList<Long> removed = new ArrayList<Long>();
        // add new childs
        
        if (state.isNew()){
        	
        	if (childName == null && nodetypes == null){
        		Collection<Long> ids = state.getCachedChilds();
        		//IdIterator(st, conn, result, removed, sessionInfo.getRepository().getBatchSize());
        		return new IdIterator(null, getConnection(), ids, new ArrayList<Long>(), sessionInfo.getRepository().getBatchSize());
        	} else {
        		Collection<Long> ids = state.getCachedChilds(childName, nodetypes);
        		//IdIterator(st, conn, result, removed, sessionInfo.getRepository().getBatchSize());
        		return new IdIterator(null, getConnection(), ids, new ArrayList<Long>(), sessionInfo.getRepository().getBatchSize());
        	}
        }
        
        for(_NodeState n : modifiedStates.values()){
    		if (n.getParentId() != null && n.getParentId().equals(state.getNodeId())){
    			if (childName == null || childName.equals(n.getName())){
    	            if (n.getStatus().equals(ItemStatus.New)){
    	                
    	                //TODO filter by nodetypes
    	                boolean skip = false;
    	                if (nodetypes != null && nodetypes.length > 0){
    	                    skip = true;
    	                    for(QName nodetype:nodetypes){
    	                        if (n.isNodeType(nodetype)){
    	                            skip = false;
    	                        }
    	                    }
    	                    
    	                }
    	                if (!skip && isNodeType(n, nodetypes)){
    	                    _result.add(n.getNodeId());
    	                }
    	            } else if (n.getStatus().equals(ItemStatus.Modified) && n.isBasePropertiesChanged()){
    	            	if (isNodeType(n, nodetypes)){
    	            		_result.add(n.getNodeId());
    	            	}
    	            } else if (n.getStatus().equals(ItemStatus.Invalidated) || n.getStatus().equals(ItemStatus.Destroyed)){
    	            	removed.add(n.getNodeId());
    	            }
    			}
            } else if (n.getParentId() == null){
            	removed.add(n.getNodeId());
            } else if (n.getParentId() != null && n.getParentCacheWithDeleted().contains(state.getNodeId())){
	            	removed.add(n.getNodeId());
            }
        }
        
        DatabaseConnection conn = getConnection();
        DatabaseSelectAllStatement st = null;
        if (state.getStatus() != ItemStatus.New) {
	        st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, false, false);
	        if (state.getInternalDepth() == 0){
	            DatabaseCondition c1 = Conditions.eq(TABLE_NODE__PARENT, state.getNodeId());
	            DatabaseCondition c2 = Conditions.eq(TABLE_NODE__PARENT, getSystemRootId());
	            st.addCondition(Conditions.or(c1, c2));
	            
	        } else {
	            st.addCondition(Conditions.eq(TABLE_NODE__PARENT, state.getNodeId()));
	        }
	        //TODO add nodetype filtering condition
	        if (nodetypes != null && nodetypes.length > 0){
	            ArrayList<Long> nodeTypeIds = new ArrayList<Long>();
	            for(QName nodetype:nodetypes){
	                NodeTypeImpl nt = getNodeTypeManager().getNodeType(nodetype);
	                Long nodeTypeId = nt.getNodeTypeDef().getId();
	                nodeTypeIds.add(nodeTypeId);
	            }
	            st.setDistinct(true);
	            st.addJoin(Constants.TABLE_TYPE, "types", Constants.FIELD_ID, FIELD_TYPE_ID);
	            st.addCondition(Conditions.in("types."+TABLE_TYPE__NODE_TYPE, nodeTypeIds));
	        }
	        
	        
	        if (childName != null && childName.getLocalName().indexOf("*") < 0){
	        	JCRHelper.populateQNameCondition(st, childName, getNamespaceRegistry());
	        }
	        
	        
	        st.addResultColumn(Constants.FIELD_ID);
	        //add security filter
	        if (checkSecurity){
	            getSecurityConditionFilter().addSecurityConditions(conn, st, true);
	        }
        }
        return new IdIterator(st, conn, _result, removed, sessionInfo.getRepository().getBatchSize(), offset, limit);


    }

    protected boolean isNodeType(_NodeState n, QName[] nodetypes) throws RepositoryException {
		if (nodetypes == null){
			return true;
		} else {
			for(QName nodetype:nodetypes){
                if (n.isNodeType(nodetype)){
                    return true;
                }
            }
		}
		return false;
	}



	public NodeStateIterator getNodesWithName(_NodeState state, String name, boolean checkSecurity) throws RepositoryException {
        IdIterator ids;
		try {
			QName qname = null;
			if (name != null){
				qname = getNamespaceResolver().getQName(name);
			}
			ids = getChildNodesId(state, checkSecurity, qname);
	        return new NodeStateIterator(ids, this, null);
		} catch (IllegalNameException e) {
			throw new RepositoryException(e);
		} catch (UnknownPrefixException e) {
			throw new RepositoryException(e);
		}
    }

    public NodeStateIterator getNodesWithPattern(_NodeState state, String namePattern, boolean checkSecurity) throws RepositoryException {
        IdIterator ids = getChildNodesId(state, checkSecurity, null);
        return new NodeStateIterator(ids, this, namePattern);
        /*for(Long childId:ids){
            _NodeState child = getNodeState(childId, false);
            try {
				if (namePattern == null || ChildrenCollectorFilter.matches(child.getName().toJCRName(getNamespaceRegistry()), namePattern)){
				    nodes.add(child);
				}
			} catch (NoPrefixDeclaredException e) {
				throw new RepositoryException(e);			
			}
        }*/
        //return Collections.unmodifiableList(nodes);

        /*if (checkSecurity){
            // traverse children using a special filtering 'collector'
            state.accept(new ChildrenCollectorFilter(namePattern, nodes, true, false, 1));
            return Collections.unmodifiableList(nodes);
        } else {
            List<Long> ids = getChildNodesId(state, false);
            for(Long childId:ids){
                _NodeState child = getNodeState(childId, false);
                if (ChildrenCollectorFilter.matches(child.getName().toJCRName(getNamespaceRegistry()), namePattern)){
                    nodes.add(child);
                }
            }
            return Collections.unmodifiableList(nodes);
        }*/
    }

	/*protected  CacheManager getCacheManager(){
		return sessionInfo.getRepository().getCacheManager();
	}*/

	protected  ConnectionProvider getConnectionProvider(){
		return sessionInfo.getRepository().getConnectionProvider();
	}

	protected void commitPutStores() {
		getStoreContainer().commitPutStores();

	}

	protected void commitStores() {
		getStoreContainer().commitStores();
	}

	
	protected void rollbackStores(){
		getStoreContainer().rollbackStores();
	}

	protected Long getSystemRootId(){
		return sessionInfo.getRepository().getSystemRootId();
	}

	NodeDefinitionImpl rootNodeDefinition = null;
	
	public NodeDefinitionImpl getRootNodeDefinition() throws RepositoryException {
		if (rootNodeDefinition == null){
	        NodeTypeImpl nt = getNodeTypeManager().getNodeType(QName.REP_ROOT);
	        rootNodeDefinition = new NodeDefinitionImpl(getNodeTypeManager(),nt,sessionInfo.getRepository().getRootNodeDef(), getNamespaceResolver());
		}
		return rootNodeDefinition;
	}

	
	public abstract DatabaseConnection getConnection() throws RepositoryException;

	abstract protected SecurityConditionFilter getSecurityConditionFilter();

	abstract protected void assignSession(_NodeState result) throws ConstraintViolationException, RepositoryException;	

	abstract protected NamespaceRegistryImpl getNamespaceRegistry();
	
	abstract protected NodeId getRootNodeId() throws RepositoryException ;

	public abstract Long getWorkspaceId();
	
	public abstract NamespaceResolver getNamespaceResolver();

	abstract protected ObservationManagerImpl getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException ;
	
    public abstract StoreContainer getStoreContainer() ;

    public NodeId buildNodeId(Long id, DatabaseConnection conn) throws RepositoryException {
		return sessionInfo.getRepository().buildNodeId(id, conn);
	}


	abstract public NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException ;

	public synchronized _ItemState getItem(Path path, boolean checkSecurity) throws RepositoryException {
        // shortcut
        if (path.denotesRoot()) {
            return getRootNode();
        }
    
        if (!path.isCanonical()) {
            String msg = "path is not canonical";
            LogUtils.debug(log, msg);
            throw new RepositoryException(msg);
        }
        //check cache
        ArrayList<_NodeState> states = new ArrayList<_NodeState>(nodeCache.values());
        states.addAll(modifiedStates.values());
        Path pPath = path.getAncestor(1);
        _NodeState parent = null;
        _NodeState result = null;
        for(_NodeState s: states){
        	if (s.getPrimaryPath().equals(path)){
        		result = s;
        		if (result.getStatus() != ItemStatus.Destroyed){
        			return s;
        		}
        	}
        	if (s.getPrimaryPath().equals(pPath)){
        		if (parent == null || parent.getStatus() == ItemStatus.Destroyed){
        			parent = s; 
        		}
        	}
        }
        if (result != null){
        	return result;
        }
        if (parent != null){
        	PathElement el = path.getNameElement();
        	if (parent.hasProperty(el.getName())){
        		return parent.getProperty(el.getName(), false);
        	}
        }
        
        return getItem(path, getRootNodeId().getId(), 1, checkSecurity);
	}
	
    private _NodeState getRootNode() throws RepositoryException {
		return getNodeState(getRootNodeId().getId(), null);
	}

	protected _ItemState getItem(Path path, Long nodeId, int next, boolean checkSecurity) throws RepositoryException {
       Path searchPath = path;
       if (!nodeId.equals(getRootNodeId().getId())){
    	   Path parentPath = getItemPath(nodeId, getConnection());
    	   if (!parentPath.denotesRoot()){
    	       try {
                searchPath = Path.create(parentPath, path, true);
            } catch (MalformedPathException e) {
                throw new RepositoryException(e);
            }
    	   }
       }
       
       Path parentPath = null;
       if (!searchPath.denotesRoot()){
           parentPath = searchPath.getAncestor(1);
       }
       String pathStr = JCRHelper.convertPathToDBString(path, getNamespaceRegistry());
       String parentPathStr = null;
       if (parentPath != null){
           parentPathStr = JCRHelper.convertPathToDBString(parentPath, getNamespaceRegistry());
       }
	    
       DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
       
       DatabaseCondition c1 = null;
       DatabaseCondition c2 = null;
       if (searchPath.denotesRoot()){
           DatabaseCondition c3 = Conditions.eq(TABLE_NODE__NODE_PATH, pathStr);
           DatabaseCondition c4 = Conditions.eq(TABLE_NODE__WORKSPACE_ID, getWorkspaceId());
           c1 = Conditions.and(c3,c4);
       } else {
           DatabaseCondition c3 = Conditions.eq(TABLE_NODE__NODE_PATH, pathStr);
           DatabaseCondition c4 = Conditions.eq(TABLE_NODE__WORKSPACE_ID, getWorkspaceId());
           DatabaseCondition c5 = Conditions.isNull(TABLE_NODE__WORKSPACE_ID);
           DatabaseCondition c6 = Conditions.or(c4,c5);
           c1 = Conditions.and(c3,c6);
           
       }
       
       if (parentPathStr != null){
           if (parentPath.denotesRoot()){
               DatabaseCondition c3 = Conditions.eq(TABLE_NODE__NODE_PATH, parentPathStr);
               DatabaseCondition c4 = Conditions.eq(TABLE_NODE__WORKSPACE_ID, getWorkspaceId());
               c2 = Conditions.and(c3,c4);
           } else {
               DatabaseCondition c3 = Conditions.eq(TABLE_NODE__NODE_PATH, parentPathStr);
               DatabaseCondition c4 = Conditions.eq(TABLE_NODE__WORKSPACE_ID, getWorkspaceId());
               DatabaseCondition c5 = Conditions.isNull(TABLE_NODE__WORKSPACE_ID);
               DatabaseCondition c6 = Conditions.or(c4,c5);
               c2 = Conditions.and(c3,c6);
               
           }
           
       }
       
       if (c2 != null){
           st.addCondition(Conditions.or(c1,c2));
       } else{
           st.addCondition(c1);
       }
       
       if (isSecurityAllowed()){
           getConnection().getDialect().addSecurityConditions(getPrincipals(), st, true);
       }
       
       
       /*if (parentPathStr != null){
           DatabaseCondition c1 = Conditions.eq(TABLE_NODE__NODE_PATH, pathStr);
           DatabaseCondition c2 = Conditions.eq(TABLE_NODE__NODE_PATH, parentPathStr);
           st.addCondition(Conditions.or(c1,c2));
       } else {
           st.addCondition(Conditions.eq(TABLE_NODE__NODE_PATH, pathStr));
       }
       
       DatabaseCondition c3 = Conditions.eq(TABLE_NODE__WORKSPACE_ID, getWorkspaceId());
       DatabaseCondition c4 = Conditions.isNull(TABLE_NODE__WORKSPACE_ID);
       st.addCondition(Conditions.or(c3,c4));
       */
       st.addResultColumn(FIELD_ID);
       st.addResultColumn(TABLE_NODE__NODE_DEPTH);
       st.addResultColumn(TABLE_NODE__SECURITY_ID);
       
       st.execute(getConnection());
       List<RowMap> rows = st.getAllRows();
       if (rows.size() == 0){
            throw new PathNotFoundException(searchPath.safeToJCRPath(getNamespaceResolver()));
       } else {
           if (rows.size() == 1){
               Long id = rows.get(0).getLong(FIELD_ID);
               _NodeState state = getNodeState(id, null);
               if (state.getInternalPath().equals(pathStr)){
                   return state;
               }
               PathElement[] elements = path.getElements();
               
               if (state.hasProperty(elements[elements.length-1].getName())){
                   return state.getProperty(elements[elements.length-1].getName(), true);
               }
               
               throw new PathNotFoundException(searchPath.safeToJCRPath(getNamespaceResolver()));
           } else if (rows.size() == 2){
               Long id = rows.get(0).getLong(FIELD_ID);
               Long d1 = rows.get(0).getLong(TABLE_NODE__NODE_DEPTH);
               //_NodeState state = getNodeState(id, null);
               Long id2 = rows.get(1).getLong(FIELD_ID);
               Long d2 = rows.get(1).getLong(TABLE_NODE__NODE_DEPTH);
               //_NodeState state2 = getNodeState(id2, null);
               if (d1.longValue() > d2.longValue()){
                   return getNodeState(id, null);
               } else {
                   return getNodeState(id2, null);
               }
               
           } else {
               for(RowMap row:rows){
                   //System.out.println(row);
                   log.error(row);
               }
               throw new RepositoryException("Too many rows, probably database is broken, or fatal error in JCR ("+searchPath.safeToJCRPath(getNamespaceRegistry())+")");
           }
//           throw new UnsupportedOperationException();
       }
	    
      /*  Path.PathElement[] elements = path.getElements();
        if (elements.length == next) {
            return node;
        }
        Path.PathElement elem = elements[next];

        QName name = elem.getName();
        int index = elem.getIndex();
        if (index == 0) {
            index = 1;
        }

        _NodeState parentNode = (_NodeState) node;
        _NodeState childNode;

        if (hasChildNode(node, name, index, checkSecurity)) {
            // child node
        	_NodeState n = getChildNode(parentNode, name, index, true);
            childNode = (_NodeState) n;

        } else if (node.hasProperty(name)) {
            // property
            if (index > 1) {
                // properties can't have same name siblings
                throw new PathNotFoundException(node.safeGetJCRPath(path));

            } else if (next < elements.length - 1) {
                // property is not the last element in the path
                throw new PathNotFoundException(node.safeGetJCRPath(path));
            }

            return node.getProperty(name, true);

        } else {
            // no such item
            throw new PathNotFoundException(node.safeGetJCRPath(path));
        }
        return getItem(path, childNode, next + 1, checkSecurity);*/
	    //throw new UnsupportedOperationException();
    }





    public Long resolvePath(Path p) throws PathNotFoundException, RepositoryException {
        /*if (paths.containsKey(p)){
            return (Long) paths.get(p);
        }*/
        _ItemState item = getItem(p, true);
        if (item.isNode()){
            return ((_NodeState)item).getNodeId();
        } else {
            return null;
        }

    }
    
    public boolean hasChildNodes(_NodeState node) throws RepositoryException {
        return getChildNodesId(node, false, null).hasNext();
    }    
    
    public boolean hasPendingChanges(Long nodeId) throws RepositoryException {
        //check changes in nodes
        _NodeState n = getNodeState(nodeId, null, true, nodeId.toString());
        return hasPendingChanges(n);
    }

    public boolean hasPendingChanges(_NodeState n) throws RepositoryException {
        //check changes in nodes
        if (n.isModified()){
            return true;
        }
        if (getModifiedNodesAll(n.getNodeId()).size() > 0){
            return true;
        }
        return false;
    }
    
    public void resetAllModifications(){
    	for(_NodeState st:modifiedStates.values()){
    		st.resetToNormal();
    		for(_PropertyState pst:st.getAllProperties()){
    			pst.resetToNormal();
    		}
    	}
    	childMax.clear();
    	modifiedStates.clear();
    	
    	try {
    		getStoreContainer().rollbackStores();
    	} catch (Throwable th){
    		
    	}
    }
    
    
	abstract protected VersionManager getVersionManager() ;

	public synchronized void evictState(_NodeState state) {
		this.modifiedStates.remove(state.getNodeId());
		this.nodeCache.remove(state.getNodeId());
		evictFromModiffiedCache(state);
	}
	
    public NodeModification createNodeModification() {
        NodeModification result = new NodeModification();
        _nodeModificationList.add(result);
        return result;
    }


    public List<NodeModification> _getNodeModificationList() {
        return _nodeModificationList;
    }

	public SessionSecurityManager getSecurityManager() {
		// TODO Auto-generated method stub
		return securityManager;
	}

	public RepositoryImpl getRepository() {
		return sessionInfo.getRepository();
	}

/*	abstract public String getUserId();
 
	abstract public List<String> getGroupIDs();*/

	public final NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException {
		return (NodeTypeRegistry) getNodeTypeManager().getNodeTypeRegistry();
	}

	public void reloadState(_NodeState state) throws RepositoryException{
		_NodeState newState  = loadNodeStateFromDB(state.getNodeId(), getConnection(), state.getInternalPath(), true);
		newState.createCopy(state);
		state.assignSession(this);
		
	}

	public boolean itemExists(String nodeUUID) {
		try {
			if (getItemByUUID(nodeUUID, false) == null){
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public synchronized void evictAll() {
		nodeCache.clear();
		this._modifiedCache.clear();
		
		
	}
	
	//TODO may be use soft hashMap to avoid memory leak ???
	private HashMap<Path,ChildMaxPosition> childMax = new HashMap<Path,ChildMaxPosition>();

	private boolean securityAllowed;

	public ChildMaxPosition getMaxChildPos(_NodeState parentState, QName child) throws RepositoryException{
		Path p;
		Path parent = parentState.getPrimaryPath();
		try {
			p  = Path.create(parent, child,true);
		} catch (MalformedPathException e){
			//should never happend
			throw new RepositoryException(e);
		}
		ChildMaxPosition result = childMax.get(p);
		
		if (result == null){
			//try load from db
			try {
				_NodeState n = getChildNode(parentState, child, 1, false);
				if (n != null){
					result = new ChildMaxPosition(parent, child, n, n.getSnsMax(), this);
					childMax.put(p, result);
				}
			} catch (Exception e) {
				// do nothing, child absent
			}
		}
		
		if (result == null){
			result = new ChildMaxPosition(parent, child, null, 0,this);
			childMax.put(p, result);
		}
		return result;
	}

	public void updateSNSMax(ChildMaxPosition p) throws RepositoryException {
		if (p.getMax() > 1 && p.getItem().getDefinition().allowsSameNameSiblings()){
			Collection<_NodeState> values = nodeCache.values();
			values.addAll(modifiedStates.values());
			for(_NodeState s:values){
				if (s.getParentId() != null && s.getName() != null &&  s.getName().equals(p.getChildName()) && s.getPrimaryPath().getAncestor(1).equals(p.getParent())){
					s.setSnsMax(p.getMax());
					registerModifiedState(s);
				}
			}
		}
	}

	public synchronized void processDeepLock(Long nodeId, Long value, String lockOwner) throws RepositoryException {
		HashSet<_NodeState> s = new HashSet<_NodeState>(nodeCache.values());
		s.addAll(modifiedStates.values());
		for(_NodeState state: s){
			if (state.hasParent(nodeId)){
				state.setParentLockId(value);
				state.setLockOwner(lockOwner);
			}
		}
		
		
	}

	public Long getSessionId() {
		return sessionInfo.getSessionId();
	}

	
	public SessionInfo getSessionInfo() throws UnsupportedRepositoryOperationException {
		return sessionInfo;
	}

	public _NodeState getNodeFromCache(Long id) {
		_NodeState result = nodeCache.get(id);
		if (result == null){
			result = modifiedStates.get(id);
		}
		return result;
	}



	public boolean isSecurityAllowed() {
		return securityAllowed;
	}



	public void loadToReferences(_NodeState state) throws RepositoryException {
		this.sm._loadReferencesTo(state, getConnection());
	}



	public void loadFromReferences(_NodeState state) throws RepositoryException {
		this.sm._loadReferencesFrom(state, getConnection());
	}



	public abstract JCRPrincipals getPrincipals();



    abstract public boolean isSecuritySwitchedOn();

    
    private ArrayList<_NodeState> loadNodeStatesFromDB(List<Long> readAheadIds,
            DatabaseConnection connection, String objectDescription, boolean checkSecurity) throws RepositoryException {
        ArrayList<_NodeState> result =  sm.findNodeState(readAheadIds, connection, objectDescription);
        loadACE(connection, result, checkSecurity);
        //load ACE records
        return result;
    }

   


    private _NodeState loadNodeStateFromDB(Long nodeId,
            DatabaseConnection connection, String internalPath, boolean checkSecurity) throws RepositoryException{
        _NodeState result = sm.findNodeState(nodeId, connection, internalPath);
        loadACE(connection, new ArrayList<_NodeState>(Arrays.asList(new _NodeState[]{result})), checkSecurity);
        return result;
    }
    
    private void loadACE(DatabaseConnection connection,
            List<_NodeState> result, boolean checkSecurity) throws RepositoryException{
        if (!isSecurityAllowed()){
            return;
        }
        HashMap<Long, _NodeState> states = new HashMap<Long, _NodeState>();
        HashSet<Long> securityIds = new HashSet<Long>();
        for(_NodeState st:result){
            states.put(st.getNodeId(), st);
            securityIds.add(st.getSecurityId());
        }
        JCRPrincipals principals = getPrincipals();
        
        //find all ACE records
        List<RowMap> aces = RepositorySecurityManager.loadSecurityPermissions(getRepository(), connection, 
        		getPrincipals(), securityIds);
        
        //populate all ACE records
        for(RowMap row:aces){
            Long securityId = row.getLong(Constants.TABLE_NODE__SECURITY_ID);
            /*_NodeState st = states.get(nodeId); 
            st.addACE(row);*/
            for(_NodeState st:result){
                if (securityId.equals(st.getSecurityId())){
                    st.addACE(row);
                }
            }
        }

        //validate for browse permission
        ArrayList<_NodeState> forRemove = new ArrayList<_NodeState>();
        for(_NodeState st:result){
            Boolean _read = JCRSecurityHelper.validateSecurityPermission(st.getNodeId(), st.getACEs(), principals, SecurityPermission.READ);
            Boolean _browse = JCRSecurityHelper.validateSecurityPermission(st.getNodeId(), st.getACEs(), principals, SecurityPermission.BROWSE);
            Boolean _denyAccess = JCRSecurityHelper.validateSecurityPermission(st.getNodeId(), st.getACEs(), principals, SecurityPermission.X_SUPER_DENY);
            boolean read = _read == null? false : _read;
            boolean browse = _browse == null? false : _browse;
            boolean denyAccess= _denyAccess == null? false : _denyAccess;
            
            boolean remove = false;
            if (!read && browse){
                st.browseMode = true;
            }
            st.denyAccess = denyAccess;
            if (st.denyAccess){
                //throw new AccessDeniedException("You have no acces to node "+st.getNodeId());
            	remove = true;
            }
            if (!read && !browse){
            	remove = true;
            }
            if (checkSecurity && remove && st.getWorkspaceId() != null){
            	forRemove.add(st);
            	nodeCache.remove(st.getNodeId());
            }
        }
        result.removeAll(forRemove);
    }

    private String versionableIsCheckedOutColumnName = null;
    private String versionableTableName = null;
    
    public synchronized String getVersionableIsCheckedOutColumnName() throws RepositoryException{
        if (versionableIsCheckedOutColumnName == null){
            NodeTypeImpl ntImpl = getNodeTypeManager().getNodeType(QName.MIX_VERSIONABLE);
            versionableTableName = ntImpl.getNodeTypeDef().getTableName();
            PropertyDefinitionImpl pd = ntImpl.getPropertyDefinition(QName.JCR_ISCHECKEDOUT);
            versionableIsCheckedOutColumnName = pd.getColumnName();
        }
        return versionableIsCheckedOutColumnName;
    }

    public String getVersionableTableName() throws RepositoryException {
        getVersionableIsCheckedOutColumnName();
        return versionableTableName;
    }


    public void refreshAllNodes() throws RepositoryException{
    	HashSet<_NodeState> states = new HashSet<_NodeState>(nodeCache.values());
    	for(_NodeState state:states){
    		if (state.getStatus() != ItemStatus.Destroyed)
    		reloadState(state);
    	}
    }

    /****************************** OCR 
     * @param changeState ****************************/
    
	private void scheduleOcrRecognition(_NodeState _state, NodeTypeContainer ntc, ChangeState changeState) throws RepositoryException{
		System.out.println("Schedule OCR");
		Long workId = getRepository().nextId();
		InternalValue vv = InternalValue.create(workId);
		_state.internalSetProperty(Constants.ECR_OCR_MIXIN__WORK_ID, vv, true, true, false);
		//TODO schedule work into OCR table
		changeState.scheduleOCRWork(workId, _state);
		
	}
    
	/**************************** END OCR ***************************/
}

