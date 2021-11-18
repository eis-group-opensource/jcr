/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.ParentNode;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.observation.ObservationManagerImpl;
import com.exigen.cm.impl.security.SecurityConditionFilter;
import com.exigen.cm.jackrabbit.lock.LockManagerImpl;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.version.VersionManager;
import com.exigen.cm.security.JCRPrincipals;

public class _SessionStateManager extends _AbstractsStateManager {

	private SessionImpl session;
	private StoreContainer storeContainer;

	private static Log log = LogFactory.getLog(_SessionStateManager.class);
	
	public _SessionStateManager(SessionImpl session, Long sessionId2, String userId, String workspaceName, boolean allowSecurity, LockManagerImpl lockManager) {
		super(session._getRepository(), sessionId2, userId, workspaceName, allowSecurity, lockManager);
		this.session = session;
		this.storeContainer = session.getStoreContainer();
		securityManager.configure();
	}

	public SessionImpl getSession() {
		return session;
	}

	@Override
	public DatabaseConnection getConnection() throws RepositoryException {
		return session.getConnection();
	}

	@Override
	protected void assignSession(_NodeState result) throws ConstraintViolationException, RepositoryException {
		result.assignSession(this);
	}

	@Override
	protected NamespaceRegistryImpl getNamespaceRegistry() {
		return session._getWorkspace()._getNamespaceRegistry();
	}

	@Override
	public NamespaceResolver getNamespaceResolver() {
		return session.getNamespaceResolver();
	}

	@Override
	protected ObservationManagerImpl getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
		return (ObservationManagerImpl) session._getWorkspace().getObservationManager();
	}

	@Override
	protected NodeId getRootNodeId() throws RepositoryException {
		//TODO cache this info
		return buildNodeId(session._getWorkspace().getRootNodeId(), getConnection());
	}

	@Override
	protected SecurityConditionFilter getSecurityConditionFilter() {
		return session.getSecurityManager();
	}

	@Override
	public StoreContainer getStoreContainer() {
		return storeContainer;
	}

	@Override
	public Long getWorkspaceId() {
		return session._getWorkspace().getWorkspaceId();
	}

	@Override
	public NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException {
		return session.getNodeTypeManager();
	}

	@Override
	protected VersionManager getVersionManager(){
		return session.getVersionManager();
	}



	@Override
	protected boolean allowVersionManager(){
		return true;
	}

	@Override
	protected void loadLock(ArrayList<_NodeState> states) throws RepositoryException {
		((LockManagerImpl)session.getLockManager()).checkLockInfo(states, getConnection());
		/*for(_NodeState state:states){
			if (state != null){
				((LockManagerImpl)session.getLockManager()).internalGetLockInfo(state.getNodeId(), getConnection());
			}
		}*/
	}

	public String safeGetJCRPath(Long nodeId) {
		try {
			_NodeState state = getNodeState(nodeId, null);
			return safeGetJCRPath(state);
		} catch (Exception exc){
			return "NodeId["+nodeId+"]";
		}
	}

	private String safeGetJCRPath(_NodeState state) {
		try {
			Path p = state.getPrimaryPath();
			return p.toJCRPath(getNamespaceResolver());
		} catch (Exception e) {
			try {
				return state.getPrimaryPath().toString();
			} catch (RepositoryException e1) {
				return "NodeId["+state.getNodeId()+"]; Path["+state.getInternalPath()+"]";
			}
		}

	}

	@Override
	public JCRPrincipals getPrincipals() {
		// TODO Auto-generated method stub
		return session._getWorkspace().getPrincipals();
	}

    @Override
    public boolean isSecuritySwitchedOn() {
        return session._getWorkspace().isSecuritySwitchedOn();
    }

    public void verifyCheckedOut(Path nodePath) throws RepositoryException{
        _NodeState state = (_NodeState) getItem(nodePath, true);
        List<ParentNode> parents = state.getParentNodes();
        
        List<Long> parentIds = new ArrayList<Long>();
        for(ParentNode pn:parents){
            parentIds.add(pn.getParentId());
        }
        parentIds.add(state.getNodeId());
        
        DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(getVersionableTableName(), true);
        st.addJoin(Constants.TABLE_NODE, "node", Constants.FIELD_TYPE_ID, Constants.FIELD_ID);
        st.addCondition(Conditions.in("node."+Constants.TABLE_NODE__PARENT, parentIds));
        
        st.addCondition(Conditions.eq(getVersionableIsCheckedOutColumnName(), Boolean.FALSE));
        
        st.addResultColumn(getVersionableIsCheckedOutColumnName());
        //st.addResultColumn(fieldName);
        
        st.execute(getConnection());
        List<RowMap> rows = st.getAllRows();
        for(RowMap row:rows){
            
        }
        
        st.close();
        if (rows.size() > 0){
            throw new VersionException(safeGetJCRPath(nodePath) + " is checked-in");
        }
        
    }

    public String safeGetJCRPath(Path path) {
        try {
            return path.toJCRPath(session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }
}
