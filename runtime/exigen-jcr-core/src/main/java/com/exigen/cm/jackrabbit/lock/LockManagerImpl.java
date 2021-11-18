/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.lock;

import static com.exigen.cm.Constants.FIELD_ID;
import static com.exigen.cm.Constants.FIELD_TYPE_ID;
import static com.exigen.cm.Constants.TABLE_NODE;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__LOCK_OWNER;
import static com.exigen.cm.Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT;
import static com.exigen.cm.Constants.TABLE_NODE_PARENT__PARENT_ID;
import static com.exigen.cm.Constants._TABLE_NODE_LOCK_INFO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.FieldNameDatabaseCondition;
import com.exigen.cm.database.transaction.JCRTransaction;
import com.exigen.cm.database.transaction.TransactionHelper;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.SessionListener;
import com.exigen.cm.impl._NodeImpl;
import com.exigen.cm.impl.observation.RepositoryObservationManagerImpl;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SessionSecurityManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.PropDef;

public class LockManagerImpl implements LockManager{

    private final SessionImpl session;
    
    /** Logger for this class */
    private static final Log log = LogFactory.getLog(LockManagerImpl.class);
    
    //private HashMap locks = new HashMap();
    
    private HashMap<Long, LockInfo> localLock = new HashMap<Long, LockInfo>();
    
    private HashMap<Long, Long> deepLock = new HashMap<Long, Long>();
    
    
    boolean columnNamesInitialized = false;

	private PropDef isDeepProp;

	private PropDef ownerProp;

	private String lockableTableName;

	private final NodeTypeRegistry ntRegistry;

	private final DatabaseConnection connection;

	private RepositoryImpl repository;
	
	private List<LockManagerListener> listeners = new ArrayList<LockManagerListener>();

    public LockManagerImpl(SessionImpl session) throws RepositoryException {
        this.ntRegistry = null;
        this.connection = null;
        this.session = session;
        init();
    }
    public LockManagerImpl(NodeTypeRegistry ntRegistry, DatabaseConnection conn, RepositoryImpl repository) throws RepositoryException {
        this.ntRegistry = ntRegistry;
        this.connection = conn;
        this.repository = repository;
        this.session= null;
        init();
    }

    private void init() throws RepositoryException {
    	RepositoryImpl rep = repository != null ? repository : session._getRepository();
        RepositoryObservationManagerImpl repositoryObsMgr = rep.getObservationManagerFactory().getObservationManager();
    	listeners.addAll(repositoryObsMgr.getLockListeners());
	}

	public NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException {
    	if (session != null){
    		return session.getNodeTypeRegistry();
    	} else {
    		return ntRegistry;
    	}
    }
    
    public void initColumnNames() throws RepositoryException  	{
    	if (!columnNamesInitialized){
	        NodeTypeRegistry ntReg = getNodeTypeRegistry();
	        // mixin types
	        Set<QName> set = new HashSet<QName>();
	        set.add(QName.MIX_LOCKABLE);
	        QName[] types = new QName[set.size() ];
	        set.toArray(types);
	        try {
	        	EffectiveNodeType  ent =  ntReg.getEffectiveNodeType(types);
	            isDeepProp = ent.getApplicablePropertyDef(QName.JCR_LOCKISDEEP, PropertyType.BOOLEAN);
	            ownerProp = ent.getApplicablePropertyDef(QName.JCR_LOCKOWNER, PropertyType.STRING);
	            QName name = ownerProp.getDeclaringNodeType();
	            if (session == null){
	            	lockableTableName = repository.getNodeTypeManager().getNodeType(name).getTableName();
	            } else {
	            	lockableTableName = session.getNodeTypeManager().getNodeType(name).getTableName();
	            }
	        } catch (NodeTypeConflictException ntce) {
	            throw new RepositoryException("Error evaluating column name for locking mixin");
	        }
	        columnNamesInitialized = true;
    	}

    }
    
    
    @SuppressWarnings("unchecked")
    public Collection<LockManagerListener> getListeners(){
        return CollectionUtils.unmodifiableCollection(listeners);
    }
   
    
    public void checkLockInfo(List<_NodeState> _states, DatabaseConnection conn) throws RepositoryException{
    	ArrayList<_NodeState> states = new ArrayList<_NodeState>(_states);
    	ArrayList<_NodeState> remove = new ArrayList<_NodeState>();
    	ArrayList<Long> ids = new ArrayList<Long>();
    	ArrayList<Long> alreadeLoaded = new ArrayList<Long>();
    	//step1 
    	for(_NodeState state:states){
            if (localLock.containsKey(state.getNodeId())){
            	remove.add(state);
            	ids.add(state.getNodeId());
            }
    	}
    	if (remove.size() > 0){
            DatabaseSelectAllStatement st  = DatabaseTools.createSelectAllStatement(_TABLE_NODE_LOCK_INFO, false);
            st.addResultColumn(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
            st.addResultColumn(FIELD_TYPE_ID);
            //st.addResultColumn(FIELD_ID);
            //st.addResultColumn(FIELD_TYPE_ID);

            st.addCondition(Conditions.in(FIELD_TYPE_ID, ids));
            
            st.execute(conn);
            while (st.hasNext()){
	            RowMap row = st.nextRow();
	            Long ownerId = row.getLong(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
	            if (ownerId == null){
	                cleanLock(row.getLong(FIELD_TYPE_ID));
	            }
            }
    		
    	}
    	ids.clear();
    	states.removeAll(remove);
    	remove.clear();
    	
    	//step2
    	for(_NodeState state:states){
        	if (deepLock.containsKey(state.getNodeId())){
        		Long ownerId = (Long) deepLock.get(state.getNodeId());
            	remove.add(state);
            	if (!ids.contains(ownerId)){
            		ids.add(ownerId);
            	}
            }
    	}
    	for(Long id:ids){
    		if (!alreadeLoaded.contains(id)){
    			internalGetLockInfo(id, conn);
    			alreadeLoaded.add(id);
    		}
            
        }    	
    	ids.clear();
    	states.removeAll(remove);
    	remove.clear();

    	//step3
        ArrayList<RowMap> rows = new ArrayList<RowMap>();
    	for(_NodeState state:states){
    		ids.add(state.getNodeId());
    	}
    	if (ids.size() > 0){
	        DatabaseSelectAllStatement st  = DatabaseTools.createSelectAllStatement(_TABLE_NODE_LOCK_INFO, true);
	        st.addCondition(Conditions.in(FIELD_TYPE_ID, ids));
	        st.addResultColumn(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
	        st.addResultColumn(TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP);
	        st.addResultColumn(TABLE_NODE_LOCK_INFO__LOCK_OWNER);
	        st.addResultColumn(FIELD_TYPE_ID);
	        st.execute(conn);
	        while(st.hasNext()){
	        	rows.add(st.nextRow());
	        }
    	}
    	ids.clear();
    	states.removeAll(remove);
    	remove.clear();
    	
    	//step4
    	for(RowMap row:rows){
            Long ownerId = row.getLong(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
            Long nodeId = row.getLong(FIELD_TYPE_ID);
            if (ownerId != null){
                if (ownerId.equals(nodeId)){
                    //building lock info
                    LockToken token = new LockToken(ownerId);
                    
                    //TODO load this values
                    boolean sessionScoped = false;
                    boolean deep = row.getBoolean(TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP);
                    HashMap<String, Object> options = new HashMap<String, Object>();
                    for(LockManagerListener listener:listeners){
                    	listener.collectOptions(row, options);
                    }
                    //NodeTypeManagerImpl ntm = this.session.getNodeTypeManager();
                    
                    //DatabaseSelectOneStatement st1 = DatabaseTools.createSelectOneStatement(ntm.getNodeType(QName.__MIX_LOCKABLE).getTableName(),FIELD_TYPE_ID, ownerId);
                    //st1.execute(conn);
                    //RowMap lockRow = st1.getRow();
                    
                    //String lockOwner = lockRow.getString(ntm.findColumnName(QName.__MIX_LOCKABLE, QName.JCR_LOCKOWNER));//"X_LOCKOWNER"
                    String lockOwner = row.getString(TABLE_NODE_LOCK_INFO__LOCK_OWNER);
                    
                    LockInfo info = new LockInfo(token, sessionScoped, deep, lockOwner, options);
                    info.getLockToken(session);
                    localLock.put(ownerId, info);
                } else {
                	if (!alreadeLoaded.contains(ownerId)){
	                    internalGetLockInfo(ownerId ,conn);
	                    deepLock.put(nodeId, ownerId);
	                    alreadeLoaded.add(ownerId);
                	}
                }
            }
    		
    	}
    }
    
    
    public LockInfo internalGetLockInfo(Long nodeId, DatabaseConnection conn) throws RepositoryException{
    	//step1
        if (localLock.containsKey(nodeId)){
            //check that lock still exist
            LockInfo info = localLock.get(nodeId);
            //HashMap row = session._getRepository().getCacheManager().loadOrGet(conn, TABLE_NODE, nodeId);
            DatabaseSelectAllStatement st  = DatabaseTools.createSelectAllStatement(_TABLE_NODE_LOCK_INFO, false);
            st.addCondition(Conditions.eq(FIELD_TYPE_ID, nodeId));
            st.addResultColumn(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
            st.execute(conn);
            RowMap row = st.nextRow();
            Long ownerId = (Long) row.get(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
            if (ownerId == null){
                cleanLock(nodeId);
                return null;
            }
            info.setLive(true);
            return info;
        }
        //step2
        if (deepLock.containsKey(nodeId)){
            //find ownerNodeId
            Long ownerId = deepLock.get(nodeId);
            return internalGetLockInfo(ownerId, conn);
        }
        
        // try find in database
        //step3
        RowMap row;
        try {
        	//row = session._getRepository().getCacheManager().loadOrGet(conn, TABLE_NODE, nodeId);
            DatabaseSelectAllStatement st  = DatabaseTools.createSelectAllStatement(_TABLE_NODE_LOCK_INFO, false);
            st.addCondition(Conditions.eq(FIELD_TYPE_ID, nodeId));
            st.addResultColumn(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
            st.addResultColumn(TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP);
            st.addResultColumn(TABLE_NODE_LOCK_INFO__LOCK_OWNER);
            for(LockManagerListener listener:listeners){
            	listener.addResultColumns(st);
            }
            st.execute(conn);
            //System.out.println(">>>> "+nodeId);
            row = st.nextRow();
        } catch (ItemNotFoundException exc){
        	return null;
        }
        //step4
        Long ownerId = (Long) row.get(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID);
        if (ownerId != null){
            if (ownerId.equals(nodeId)){
                //building lock info
                LockToken token = new LockToken(ownerId);
                
                //TODO load this values
                boolean sessionScoped = false;
                boolean deep = row.getBoolean(TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP);
                
                //NodeTypeManagerImpl ntm = this.session.getNodeTypeManager();
                
                //DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(ntm.getNodeType(QName.__MIX_LOCKABLE).getTableName(),FIELD_TYPE_ID, ownerId);
                //st.execute(conn);
                //RowMap lockRow = st.getRow();
                
                //TODO optimize this
                //String lockOwner = lockRow.getString(ntm.findColumnName(QName.__MIX_LOCKABLE, QName.JCR_LOCKOWNER));//"X_LOCKOWNER"
                String lockOwner = row.getString(TABLE_NODE_LOCK_INFO__LOCK_OWNER);
                HashMap<String, Object> options = new HashMap<String, Object>();
                for(LockManagerListener listener:listeners){
                	listener.collectOptions(row, options);
                }
                LockInfo info = new LockInfo(token, sessionScoped, deep, lockOwner, options);
                info.getLockToken(session);
                localLock.put(ownerId, info);
                return info;
            } else {
                LockInfo info = internalGetLockInfo(ownerId ,conn);
                deepLock.put(nodeId, ownerId);
                return info;
            }
        }
        return null;

    }
    
/*    public LockInfo loadLockInfo(ArrayList<Long> nodeId, DatabaseConnection conn) throws RepositoryException{
    	ArrayList<Long> ids = new ArrayList<Long>(nodeId);
    	
        if (localLock.containsKey(nodeId)){
            //check that lock still exist
            LockInfo info = (LockInfo) localLock.get(nodeId);
            HashMap row = session._getRepository().getCacheManager().loadOrGet(conn, TABLE_NODE, nodeId);
            Long ownerId = (Long) row.get(TABLE_NODE__PARENT_LOCK_ID);
            if (ownerId == null){
                cleanLock(nodeId);
                return null;
            }
            return info;
        }
        if (deepLock.containsKey(nodeId)){
            //find ownerNodeId
            Long ownerId = (Long) deepLock.get(nodeId);
            return internalGetLockInfo(ownerId, conn);
        }
        
        // try find in database
        RowMap row;
        try {
        	row = session._getRepository().getCacheManager().loadOrGet(conn, TABLE_NODE, nodeId);
        } catch (ItemNotFoundException exc){
        	return null;
        }
        Long ownerId = (Long) row.get(TABLE_NODE__PARENT_LOCK_ID);
        if (ownerId != null){
            if (ownerId.equals(nodeId)){
                //building lock info
                LockToken token = new LockToken(ownerId);
                
                //TODO load this values
                boolean sessionScoped = false;
                boolean deep = row.getBoolean(TABLE_NODE__LOCK_IS_DEEP);
                
                NodeTypeManagerImpl ntm = this.session.getNodeTypeManager();
                
                DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(ntm.getNodeType(QName.__MIX_LOCKABLE).getTableName(),FIELD_TYPE_ID, ownerId);
                st.execute(conn);
                RowMap lockRow = st.getRow();
                
                String lockOwner = lockRow.getString(ntm.findColumnName(QName.__MIX_LOCKABLE, QName.JCR_LOCKOWNER));//"X_LOCKOWNER"
                
                LockInfo info = new LockInfo(token, sessionScoped, deep, lockOwner);
                info.getLockToken(session);
                localLock.put(ownerId, info);
                return info;
            } else {
                LockInfo info = internalGetLockInfo(ownerId ,conn);
                deepLock.put(nodeId, ownerId);
                return info;
            }
        }
        return null;
    }*/
    
    
    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, Map<String, Object> options) throws LockException, RepositoryException {
        
    	if (node._getRepository().isLockDisabled()){
    		throw new RepositoryException("Locks is disabled");
    	}
    	
    	if (isSessionScoped){
    		node._getSession().registerSession();
    	}
    	
        Long nodeId = node.getNodeId();
        
        if (node.isTransactionalNew()){
        	if (isSessionScoped){
        		return null;
        	} else {
        		throw new LockException("Node "+node.getPath()+" cannot be locked until transaction commit.");
        	}
        }
        
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = getConnection();
        conn.lockNode(node.getNodeId());
        try {
            //TODO Lock SQl row
            
            LockInfo info = internalGetLockInfo(nodeId, conn);
            if (info != null){
            	conn.rollback();
            	ArrayList<Long> ids= new ArrayList<Long>();
            	ids.add(nodeId);
                throw new ChildLockException(new String[]{JCRHelper.getNodePath(nodeId, session, conn)}, ids); 
            }
            
            info = new LockInfo(new LockToken(node.getNodeId()),
                    isSessionScoped, isDeep, session.getUserID(), options);
            
            
            if (info.deep){
                //TODO lock child SQL rows
                checkDeepLock(node.getNodeId(), conn, true);
            }
            
            // create lock token
            info.setLockHolder(session);
            info.setLive(true);
            
            //ArrayList evictList = new ArrayList();
            
            //save to db
            //TODO implement session scoped lock
            //1.update lock_parent_id
            
            //Calendar expires = null;
            
            internalSetParentLockId(nodeId, nodeId, conn, isDeep, info.lockOwner, isSessionScoped, node._getSession().getSessionId(), options);
            
            //TODO 2.deep lock
            if (info.deep){
                
                internalSetParentDeepLockId(nodeId, nodeId, conn, info.lockOwner, isSessionScoped, node._getSession().getSessionId(), options);
            }
            //TODO 3.update lock owner && lock is deep in mix:lockable
            internalSetLockOwner(node.getNodeId(), session.getUserID(), Boolean.valueOf(isDeep), conn);
            //conn.commit();
            //session._getRepository().getCacheManager().evict(evictList);
            
            session.addListener(info);
            session.addLockToken(info.lockToken.toString(), false);
            
            localLock.put(nodeId, info);
            
            return new LockImpl(info, node);
        } finally {
        	conn.commit();
            conn.close();
            stopOperation(tr);
        }
    }


    private void internalSetParentLockId(Long nodeId, Long lockId, DatabaseConnection conn, Boolean isDeep, String lockOwner, 
    		boolean sessionScoped, long sessionId, Map<String, Object> options/*, Calendar expires*/) throws RepositoryException {
        DatabaseUpdateStatement upd1 = DatabaseTools.createUpdateStatement(_TABLE_NODE_LOCK_INFO, FIELD_TYPE_ID, nodeId);
        upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID, lockId));
        upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__LOCK_IS_DEEP, isDeep));
        upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__LOCK_OWNER, lockOwner));
        //upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__LOCK_EXPIRES, expires));
        if (sessionScoped){
        	upd1.addValue(SQLParameter.create(Constants.TABLE_NODE_LOCK_INFO__SESSION_ID, sessionId));
        }
        for(LockManagerListener listener:listeners){
        	listener.internalSetParentLockId(conn, upd1, options, lockId);
        }
        //upd1.addValue(SQLParameter.createSQL(TABLE_NODE__VERSION_, TABLE_NODE__VERSION_+"+1"));
        upd1.execute(conn);
        upd1.close();
        //evictList.add(new CacheKey(TABLE_NODE, nodeId));
    }
    
    private void internalSetParentDeepLockId(Long nodeId, Long lockId, DatabaseConnection conn, String lockOwner,
    		boolean sessionScoped, Long sessionId, Map<String, Object> options ) throws RepositoryException {
        //create select in part
        DatabaseSelectAllStatement _st = DatabaseTools.createSelectAllStatement(TABLE_NODE, false);
        //add join
        _st.addJoin(TABLE_NODE_PARENT, "parents", FIELD_ID, FIELD_TYPE_ID );
        //add condition
        _st.addCondition(Conditions.eq("parents."+TABLE_NODE_PARENT__PARENT_ID, nodeId));
        _st.addResultColumn(FIELD_ID);

        DatabaseUpdateStatement upd1 = DatabaseTools.createUpdateStatement(_TABLE_NODE_LOCK_INFO);
        upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID, lockId));
        upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__LOCK_OWNER, lockOwner));
        //upd1.addValue(SQLParameter.createSQL(TABLE_NODE__VERSION_, TABLE_NODE__VERSION_+"+1"));
        if (sessionScoped){
        	upd1.addValue(SQLParameter.create(Constants.TABLE_NODE_LOCK_INFO__SESSION_ID, sessionId));
        }
        upd1.addCondition(Conditions.in(FIELD_TYPE_ID, _st));
        
        for(LockManagerListener listener:listeners){
        	listener.internalSetParentDeepLockId(conn, upd1, options, lockId);
        }
        
        upd1.execute(conn);
        upd1.close();
        
/*        st.execute(conn);
        while (st.hasNext()){
            RowMap row = st.nextRow();
            //evictList.add(new CacheKey(TABLE_NODE, row.getLong(FIELD_ID)));
        }*/
        //evictList.add(new CacheKey(TABLE_NODE, nodeId));
    }
    
    private void internalSetLockOwner(Long nodeId, String owner, Boolean isDeep, DatabaseConnection conn) throws RepositoryException {
        /*EffectiveNodeType ent = node.getNodeState().getEffectiveNodeType();
        PropDef isDeepProp = ent.getApplicablePropertyDef(QName.JCR_LOCKISDEEP, PropertyType.BOOLEAN);
        PropDef ownerProp = ent.getApplicablePropertyDef(QName.JCR_LOCKOWNER, PropertyType.STRING);
        QName name = ownerProp.getDeclaringNodeType();
        */
    	initColumnNames();
    	
        
        DatabaseUpdateStatement upd1 = DatabaseTools.createUpdateStatement(lockableTableName, FIELD_TYPE_ID, nodeId);
        upd1.addValue(SQLParameter.create(isDeepProp.getColumnName(), isDeep));
        upd1.addValue(SQLParameter.create(ownerProp.getColumnName(), owner));
        upd1.execute(conn);
        upd1.close();
    }

    public void unlock(NodeImpl node, Map<String, Object> options) throws LockException, RepositoryException {
    	if (node._getRepository().isLockDisabled()){
    		throw new RepositoryException("Locks is disabled");
    	}

        if (node.isTransactionalNew()){
       		return;
        }
    	unlock(node.getNodeId(), node.safeGetJCRPath(), node.getSecurityId(), 
    			((SessionImpl)node.getSession()).getSecurityManager(), options);
    }

    
    public void unlock(Long nodeId, String safeJCRPath, Long securityId, SessionSecurityManager securitymanager, Map<String, Object> options) throws LockException, RepositoryException {
    	if (repository == null){
    		repository = session._getRepository();
    	}
    	if (repository.isLockDisabled()){
    		throw new RepositoryException("Locks is disabled");
    	}

    	
        //Long nodeId = node.getNodeId();
        
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = getConnection();
        
        conn.lockNode(nodeId);
        try {
            //TODO Lock SQL row
            
            LockInfo originalInfo = internalGetLockInfo(nodeId, conn);

            //originalInfo = internalGetLockInfo(nodeId, conn);
            if (originalInfo == null){
            	System.err.println("Skip");
            	throw new LockException("Node not locked: "+safeJCRPath);
            }
            
            if (originalInfo.getLockToken(session) == null){
                try {
                	if (securityId != -1){
                		securitymanager.checkPermission(SecurityPermission.X_UNLOCK, nodeId, securityId);
                	}
                } catch (AccessDeniedException exc){
                    throw new AccessDeniedException("Can't find lock tocken in session");
                }
            }
            
            //clean cache
            localLock.remove(nodeId);
            deepLock.remove(nodeId);
            
            LockInfo info = internalGetLockInfo(nodeId, conn);
            if (info == null){
                throw new LockException("Node not locked: "+safeJCRPath);
            }
            if (!info.getNodeId().equals(nodeId)){
                throw new LockException("This node desn't contain lock: "+safeJCRPath);
            }
            //ArrayList evictList = new ArrayList();
            internalSetParentLockId(nodeId, null, conn, null, info.lockOwner, true, 0, options);
            
            if (info.deep){
                internalSetParentDeepLockId(nodeId, null, conn, info.lockOwner, true, (long)0, options);
            }
            
            internalSetLockOwner(nodeId, null, null, conn);

            //conn.commit();
            if (session != null){
	            //session._getRepository().getCacheManager().evict(evictList);
	            session.removeListener(info);
//	            session.addLockToken(info.lockToken.toString(), false);
	            session.removeLockToken(info.lockToken.toString(), false);//MAX
            }
            
            originalInfo.setLive(false);
            info.setLive(false);
            
            localLock.remove(nodeId);
            
            //clean deep lock
            cleanLock(nodeId);
        } finally {
        	conn.commit();
            conn.close();
            stopOperation(tr);
        }
    }


    private void cleanLock(Long nodeId) {
        ArrayList<Long> removed = new ArrayList<Long>();
        for(Iterator<Long> it= deepLock.keySet().iterator(); it.hasNext();){
            Long deepId = it.next();
            if (nodeId.equals(deepLock.get(deepId))){
                removed.add(deepId);
            }
        }
        for(Iterator<Long> it= removed.iterator(); it.hasNext();){
            deepLock.remove(it.next());
        }
    }

    
    private DatabaseConnection getConnection() throws RepositoryException {
        //TODO open new connection
    	if (connection != null){
    		return connection;
    	}
        return session.getConnection();
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock(NodeImpl node) throws LockException,
            RepositoryException {

        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = session.getConnection();
        try {
            
            LockInfo info = internalGetLockInfo(node.getNodeId(), conn);
            
            if (info == null){
                throw new LockException("Node not locked");
            }
            
            NodeImpl lockNode = ((SessionImpl)node.getSession()).getNodeManager().buildNode(info.getNodeId());
            
            return new LockImpl(info, lockNode);
        } finally {
            conn.close();
            stopOperation(tr);
        }
    }
    
    
    public boolean holdsLock(NodeImpl node) throws RepositoryException {
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = session.getConnection();
        try {
            Long nodeId = node.getNodeId();
            LockInfo info = internalGetLockInfo(nodeId, conn);
            if (info!= null && info.getNodeId().equals(nodeId)){
                return true;
            } else {
                return false;
            }
        } finally {
            conn.close();
            stopOperation(tr);
        }
    }

    public boolean isLocked(NodeImpl node) throws RepositoryException {
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = session.getConnection();
        try {
            Long nodeId = node.getNodeId();
            LockInfo info = internalGetLockInfo(nodeId, conn);
            if (info == null){
                return false;
            } else {
                return true;
            }
        } finally {
            conn.close();
            stopOperation(tr);
        }
    }

    public void checkLock(_NodeImpl node) throws LockException, RepositoryException{
    	if (!node._getRepository().isLockDisabled()){
    		checkLock(node.getNodeId());
    	}
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void checkLock(Long nodeId)
            throws LockException, RepositoryException {

        
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = session.getConnection();
        try {
            LockInfo info = internalGetLockInfo(nodeId, conn);
            if (info == null){
                return;
            } else {
                if (session.equals(info.getLockHolder()) || 
                		(session.getUserID().equals(info.lockOwner)  && session._getRepository().allowAutoLockToken())){
                    return;
                } else {
                    throw new LockException("Node "+safeGetJCRPath(nodeId)+" is locked");
                }
                
            }
        } finally {
            conn.close();
            stopOperation(tr);
        }
    }


    private String safeGetJCRPath(Long nodeId) {
		return session.getStateManager().safeGetJCRPath(nodeId);
	}


	/**
     * {@inheritDoc}
     */
    public void checkLock(Path path, Session session)
            throws LockException, RepositoryException {
        checkLock(((SessionImpl)session).getNodeManager().getNodeByPath(path));
    }
    
    // ----------------------------- Old implementation
    // --------------------------



    private JCRTransaction beginOperation() throws RepositoryException {
        JCRTransaction tr = TransactionHelper.getInstance().startNewTransaction();
        if (session != null){
        	session.pushConnection();
        }
        
        return tr;
    }

    private void stopOperation(JCRTransaction tr) throws RepositoryException {
    	if (tr != null){
    		TransactionHelper.getInstance().commitAndResore(tr);
    		
    	}
        if (session != null){
        	session.popConnection();
        }
        
    }

    public void lockTokenAdded(SessionImpl session, String lt) throws RepositoryException{
        Long nodeId = new Long(lt);
        
        if (localLock.containsKey(nodeId)){
            LockInfo info = (LockInfo) this.localLock.get(nodeId);
            info.setLockHolder(session);
        } else {
            JCRTransaction tr = beginOperation();
            DatabaseConnection conn = session.getConnection();
            try {
                LockInfo info = internalGetLockInfo(nodeId, conn);
                if (info != null){
                	info.setLockHolder(session);
                }
            } finally {
                conn.close();
                stopOperation(tr);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenRemoved(SessionImpl session, String lt) {
        Long nodeId = new Long(lt);
        
        if (localLock.containsKey(nodeId)){
            LockInfo info = (LockInfo) this.localLock.get(nodeId);
            info.setLockHolder(null);
        } else {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Contains information about a lock and gets placed inside the child
     * information of a {@link org.apache.jackrabbit.core.PathMap}.
     */
    static class LockInfo extends AbstractLockInfo implements SessionListener {

        /**
         * Create a new instance of this class.
         *
         * @param lockToken     lock token
         * @param sessionScoped whether lock token is session scoped
         * @param deep          whether lock is deep
         * @param lockOwner     owner of lock
         * @param options 
         */
        public LockInfo(LockToken lockToken, boolean sessionScoped,
                        boolean deep, String lockOwner, Map<String, Object> options) {
            super(lockToken, sessionScoped, deep, lockOwner, options);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * When the owning session is logging out, we have to perform some
         * operations depending on the lock type.
         * (1) If the lock was session-scoped, we unlock the node.
         * (2) If the lock was open-scoped, we remove the lock token
         *     from the session and set the lockHolder field to <code>null</code>.
         */
        public void loggingOut(SessionImpl session) {
            if (live) {
                if (sessionScoped) {
                    // if no session currently holds lock, reassign
                    SessionImpl lockHolder = getLockHolder();
                    if (lockHolder == null) {
                        setLockHolder(session);
                        session.getStateManager().resetAllModifications();
                    } else {
                    	lockHolder.getStateManager().resetAllModifications();
                    }
                    try {
                        NodeImpl node = (NodeImpl) session.getNodeManager().buildNode(getNodeId());
                        _NodeState state = node.getNodeState();
                        session.getStateManager().reloadState(state);
                        node.unlock();
                    } catch (RepositoryException e) {
                        log.warn("Unable to unlock session-scoped lock on node '"
                                + lockToken + "': " + e.getMessage());
                        log.debug("Root cause: ", e);
                    }
                } else {
                    if (session.equals(lockHolder)) {
                        session.removeLockToken(lockToken.toString());
                        lockHolder = null;
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void loggedOut(SessionImpl session) {
        }
    }

	public void registerListener(LockManagerListener listener) {
		listeners.add(listener);
	}

	public void checkDeepLock(_NodeImpl nodeImpl) throws LockException, RepositoryException{
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = session.getConnection();
        try {
    		checkDeepLock(nodeImpl.getNodeId(), conn, false);
        } finally {
            conn.close();
            stopOperation(tr);
        }
	
	}
	
	public void checkDeepLock(Long nodeId, DatabaseConnection conn, boolean checkSameUser) throws LockException, RepositoryException{
		DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(_TABLE_NODE_LOCK_INFO, true);
        //add join
        st.addJoin(TABLE_NODE_PARENT, "parents", FIELD_TYPE_ID, FIELD_TYPE_ID );
        //add condition
        st.addCondition(Conditions.eq("parents."+TABLE_NODE_PARENT__PARENT_ID, nodeId));
        st.addCondition(Conditions.notNull(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID));
        if (!checkSameUser){
        	st.addCondition(Conditions.notEq(new FieldNameDatabaseCondition(TABLE_NODE_LOCK_INFO__LOCK_OWNER), this.session.getUserID()));
        }
        st.execute(conn);
        boolean hasChilds = st.hasNext();
        List<RowMap> rows = st.getAllRows();
        st.close();
        if (hasChilds){
            HashSet<Long> ids = new HashSet<Long>();
            for(RowMap row:rows){
                ids.add(row.getLong(Constants.TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID));
            }
            if (!checkSameUser){
            	ids.removeAll(Arrays.asList(session.getLockTokens()));
            }
            ArrayList<String> paths = new ArrayList<String>();
            ArrayList<Long> _ids = new ArrayList<Long>();
            st = DatabaseTools.createSelectAllStatement(TABLE_NODE, true);
            st.addCondition(Conditions.in(FIELD_ID, ids));
            st.addResultColumn(Constants.TABLE_NODE__NODE_PATH);
            st.addResultColumn(Constants.FIELD_ID);
            st.addResultColumn(Constants.TABLE_NODE__SECURITY_ID);
            st.execute(conn);
            rows = st.getAllRows();
            st.close();
            for(RowMap row:rows){
                String path = row.getString(Constants.TABLE_NODE__NODE_PATH);
                Long id = row.getLong(Constants.FIELD_ID);
                Long securityId = row.getLong(Constants.TABLE_NODE__SECURITY_ID);
                path = JCRHelper.convertPath(path);
                Path pp;
                
                
                if (!checkSameUser){
	                LockInfo originalInfo = internalGetLockInfo(id, conn);
	
	                if (originalInfo != null && originalInfo.getLockToken(session) == null){
	                    try {
	                    	if (securityId != -1){
	                    		session.getSecurityManager().checkPermission(SecurityPermission.X_UNLOCK, nodeId, securityId);
	                    		continue;
	                    	}
	                    } catch (AccessDeniedException exc){
	                    }
	                }
                }
                
                
                _ids.add(id);
                try {
                    pp = Path.create(path, this.session.getNamespaceResolver(), true);
                    paths.add(pp.toJCRPath(session.getNamespaceResolver()));
                } catch (Exception e) {
                    e.printStackTrace();
                    paths.add(path);
                }
            }
            
            throw new ChildLockException( paths.toArray(new String[paths.size()]), _ids);  
        }	
     }
	
	/*
	public void touchLock(NodeImpl node, Calendar expires) throws LockException, RepositoryException {
    	
        Long nodeId = node.getNodeId();
        
        JCRTransaction tr = beginOperation();
        DatabaseConnection conn = getConnection();
        conn.lockNode(node.getNodeId());
        try {
            //TODO Lock SQl row
            
            LockInfo info = internalGetLockInfo(nodeId, conn);
            if (info != null){
            	conn.rollback();
            	ArrayList<Long> ids= new ArrayList<Long>();
            	ids.add(nodeId);
                throw new ChildLockException(new String[]{JCRHelper.getNodePath(nodeId, session, conn)}, ids); 
            }
            if (nodeId.toString().equals(info.getLockToken(node.getSession()))){
            	 DatabaseUpdateStatement upd1 = DatabaseTools.createUpdateStatement(_TABLE_NODE_LOCK_INFO, FIELD_TYPE_ID, nodeId);
                 upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__PARENT_LOCK_ID, nodeId));
                 upd1.addValue(SQLParameter.create(TABLE_NODE_LOCK_INFO__LOCK_EXPIRES, expires));
                 upd1.execute(conn);
                 upd1.close();            	
            } else {
            	throw new LockException("Lock token not found");
            }
        } finally {
        	conn.commit();
            conn.close();
            stopOperation(tr);
        }	}*/

}


/*
 * $Log: LockManagerImpl.java,v $
 * Revision 1.26  2011/09/09 09:42:56  jkersovs
 * EPB-335 'DatabaseConnection statements cache breaks on WAS 7.0.0.17'
 * Fix provided by V. Beilins
 *
 * Revision 1.25  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.24  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.23  2009/01/26 07:49:30  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.22  2009/01/23 14:06:17  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.21  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.20  2009/01/09 12:39:16  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.19  2008/10/03 08:34:55  dparhomenko
 * *** empty log message ***

 *
 */