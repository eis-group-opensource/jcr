/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.security;


import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.ItemId;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SecurityEntry;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.security.JCRPrincipals;

public class SessionSecurityManager implements SecurityConditionFilter{

    private _AbstractsStateManager stateManager;
    private RepositoryImpl repository;
    private RepositorySecurityManager repositorySecurityManager;
    private JCRPrincipals principals;
    
    public SessionSecurityManager(_AbstractsStateManager stateManager) {
        this.stateManager = stateManager;
        this.repository = stateManager.getRepository();
        this.repositorySecurityManager = repository.getSecurityManager();

    }

    public void configure(){
    	principals = stateManager.getPrincipals();
    }
    
    public List<BaseSecurityPermission> getMyPermissions(Long nodeId) throws RepositoryException{
        return _getUserPermissions(nodeId, principals);
    }
    
    public List<BaseSecurityPermission> getUserPermissions(Long nodeId, JCRPrincipals _principals) throws RepositoryException{
        //check grant permision
        Long securityId = buildNode(nodeId).getSecurityId();
        checkPermission(SecurityPermission.X_GRANT, nodeId, securityId);
        
        return _getUserPermissions(nodeId, _principals);
    }
    
    public boolean hasUserPermission(Long nodeId, JCRPrincipals _principals, BaseSecurityPermission p) throws RepositoryException{
        List<BaseSecurityPermission> result = _getUserPermissions(nodeId, _principals);
        for(BaseSecurityPermission p1:result){
        	if (p1.getPermissionName().equals(p.getPermissionName())){
        		return true;
        	}
        }
        return false;
    }
    
    private List<BaseSecurityPermission> _getUserPermissions(Long nodeId, JCRPrincipals _principals) throws RepositoryException{
    	
    	_NodeState state = stateManager.getNodeFromCache(nodeId);
    	List<RowMap> allRows = null;
    	if (state != null && _principals.equals(principals)){
    		allRows = state.getACEs();
    	}
        ArrayList<BaseSecurityPermission> result = new ArrayList<BaseSecurityPermission>();
        List<BaseSecurityPermission> all = getAllPermissions();
        Long securityId = buildNode(nodeId).getSecurityId();
        
        if (allRows == null){
        	allRows = RepositorySecurityManager.loadSecurityPermissions(repository, getConnection(), _principals, Arrays.asList(new Long[]{securityId}));
        }
        for(BaseSecurityPermission permission : all){
            Boolean allowed = JCRSecurityHelper.validateSecurityPermission(nodeId, allRows, _principals, permission);
            if (allowed != null && allowed){
                result.add(permission);
            }
        }
        
        return result;
    }
    
    public void checkPermission(Long nodeId, String actions) throws RepositoryException {
        if (getWorkspaceId() == null || principals == null || principals.getUserId() == null){
            return;
        }
        if (!stateManager.isSecurityAllowed()){
        	return ;
        }
        
        String[] strings = actions.split(",");
        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            set.add(strings[i]);
        }


        Long securityId = null;
        
        List<BaseSecurityPermission> all = getAllPermissions();
        
       for(String action : set){
            if (securityId == null){
                securityId = buildNode(nodeId).getSecurityId();
            }
            for(BaseSecurityPermission p: all){
                if (p.getPermissionName().equals(action)){
                    try {
                        checkPermission(getConnection(), principals, p, nodeId, securityId);
                    } catch (PathNotFoundException pnfe) {
                        // target does not exist, throw exception
                        throw new AccessControlException(p.getPermissionName());
                    } catch (AccessDeniedException re) {
                        // otherwise the RepositoryException catch clause will
                        // log a warn message, which is not appropriate in this case.
                        throw new AccessControlException(p.getPermissionName());
                    }
                }
            }
        }
    }

	private _NodeState buildNode(Long nodeId) throws RepositoryException {
		return stateManager.getNodeState(nodeId, null);
	}

	private DatabaseConnection getConnection() throws RepositoryException {
		return stateManager.getConnection();
	}

    private Long getWorkspaceId() {
		return stateManager.getWorkspaceId();
	}


    public void propagateNodeSecurity(Node node) throws RepositoryException{
    	repositorySecurityManager.propagateNodeSecurity((NodeImpl) node, this.principals);
    }

    protected void removePermission(Node node, String userId, String groupId, String contextId, boolean propagate) throws RepositoryException {
        removePermission(node, SecurityPrincipal.create(userId, groupId, contextId), propagate);
    }
    
    public void removePermission(Node node, SecurityPrincipal targetPrincipal, boolean propagate) throws RepositoryException {
    	if (repository.isIgnoreCaseInSecurity()){
    	    targetPrincipal.setIgnoreCase(true);
    	}
        NodeImpl _node = (NodeImpl) node;
        Long nodeId = _node.getNodeId();
        Long securityId = _node.getSecurityId();
        //TODO may be suspend transaction ???
        DatabaseConnection conn = getConnection();
        SecurityPostProcessor postProcessor = new SecurityPostProcessor(conn, repositorySecurityManager, principals);
        try {
            //check grant permission
            checkPermission(conn, this.principals, SecurityPermission.X_GRANT, nodeId, securityId, targetPrincipal);

                SecurityPermissionDefinition def = new SecurityPermissionDefinition(SecurityPermission.READ, targetPrincipal, null, propagate);
                repositorySecurityManager.removePermission(conn, _node.getNodeState(), def, this.stateManager,this.principals, postProcessor);
            //}
            if (securityId.longValue() != nodeId.longValue()){
                _node.setSecurityId(nodeId);
            }
            postProcessor.process();
            conn.commit();
        } finally {
            conn.close();
        }
        
        
    }


    public void setPermission(Node node, List<SecurityPermissionDefinition> securityDefs) throws RepositoryException{
        NodeImpl nodeImpl = (NodeImpl) node;
        setPermission(nodeImpl.getNodeState(), securityDefs);
    }
        
    public void setPermission(Node node, SecurityPermissionDefinition securityDef) throws RepositoryException{
        NodeImpl nodeImpl = (NodeImpl) node;
        ArrayList<SecurityPermissionDefinition> securityDefs = new ArrayList<SecurityPermissionDefinition>(1);
        securityDefs.add(securityDef);
        setPermission(nodeImpl.getNodeState(), securityDefs);
    }

    public void setPermission(_NodeState node, List<SecurityPermissionDefinition> defs) throws RepositoryException {
    	_setPermission(node, defs, true);
    }
 
    public void _setPermission(_NodeState node, List<SecurityPermissionDefinition> defs, boolean checkSecurity) throws RepositoryException {
        
        checkIsNew(node);
        if (defs == null || defs.size() == 0){
            return;
        }
    	if (repository.isIgnoreCaseInSecurity()){
    		for(SecurityPermissionDefinition def:defs){
    			def.getPrincipal().setIgnoreCase(true);
    		}
    	}
    	/*boolean hasPropogate = false;
		for(SecurityPermissionDefinition def:defs){
			if (def.isPropogate()){
				hasPropogate = true;
			}

		}*/

        Long securityId = node.getSecurityId();
        Long nodeId = node.getNodeId();
        DatabaseConnection conn = getConnection();
        SecurityPostProcessor postProcessor = new SecurityPostProcessor(conn, repositorySecurityManager, principals);
        try {
            //check grant permission
            if (this.stateManager.isSecuritySwitchedOn() && checkSecurity){
                for(SecurityPermissionDefinition def:defs){
                    checkPermission(conn, principals, SecurityPermission.X_GRANT, nodeId, securityId, 
                            def.getPrincipal());
                }
            }
    		for(SecurityPermissionDefinition def:defs){
    			//if (def.isPermit() != null){
    				repositorySecurityManager.assignSecurity(conn, nodeId, securityId, def,this.principals, postProcessor);
    			//} else {
    			//	repositorySecurityManager.removePermission(conn, node, def, stateManager,this.principals, postProcessor);
    			//}
    		}
    		postProcessor.process();
            conn.commit();
            if (securityId.longValue() != nodeId.longValue()){
                node.setSecurityId(nodeId);
            }
        } finally {
            conn.close();
        }
    }


    private void checkIsNew(_NodeState node) throws RepositoryException{
        if (node.isNew()){
            throw new RepositoryException("Cannot set security on unsaved node");
        }
    }

    public void addSecurityConditions(DatabaseConnection conn, DatabaseSelectAllStatement st, boolean allowBrowse) throws RepositoryException {
        if (getWorkspaceId() == null || principals == null || principals.getUserId() == null){
            return;
        }        
        if (stateManager.isSecurityAllowed()){
        	repositorySecurityManager.addSecurityConditions(conn, principals, st, allowBrowse);
        }
    }
    
    public List<SecurityEntry> getSecurityEntries(Node node) throws RepositoryException{
        return getSecurityEntriesBySecurityId(getSecurityId(node), true);
        //throw new UnsupportedOperationException();
    }

    public List<SecurityEntry> getSecurityEntries(_NodeState node, boolean checkPermissions) throws RepositoryException{
        return getSecurityEntriesBySecurityId(getSecurityId(node), checkPermissions);
        //throw new UnsupportedOperationException();
    }


    public List<SecurityEntry> getSecurityEntriesBySecurityId(Long securityId, boolean checkPermissions) throws RepositoryException{
        //1. check node grant permission (only user with grant permission can read ACL)
    	if (checkPermissions){
    		checkPermission(SecurityPermission.X_GRANT, securityId, securityId);
    	}
        //2. load security
        DatabaseConnection conn = getConnection();
        try {
            return repositorySecurityManager.getNodeACL(securityId, conn, stateManager);
        } finally {
            conn.close();
        }
    }    
    
    public Node getSecurityOwner(Node node) throws RepositoryException{
        Long securityId = getSecurityId(node);
        return new NodeImpl(buildNode(securityId), stateManager);
    }
    
    private SecurityPermission getPermission(String permission) throws RepositoryException {
        for(SecurityPermission p:SecurityPermission.values()){
            if (p.getPermissionName().equals(permission)){
                return p;
            }
        }
        throw new RepositoryException("Unknown permission "+permission);
    }
    
    

    private Long getSecurityId(Node node) throws RepositoryException {
        if (node instanceof NodeImpl){
            return ((NodeImpl)node).getSecurityId();
        } else {
            throw new RepositoryException("Node is not instance of NodeImpl");
        }
    }    

    private Long getSecurityId(_NodeState node) throws RepositoryException {
    	return node.getSecurityId();
    }

	public boolean isSecurityEnabled() throws RepositoryException{
		SessionImpl s = ((_SessionStateManager)stateManager).getSession();
		Long rootNodeId = s._getWorkspace().getRootNodeId();
		List<SecurityEntry> entries = getSecurityEntries(stateManager.getNodeState(rootNodeId, null), true);
		if (entries.size() > 0){
			return true;
		} else {
			return false;
		}
	}

	public boolean isGranted(ItemId targetId, SecurityPermission permission) {
		Long nodeId = null;
		if (targetId instanceof PropertyId){
			nodeId = ((PropertyId)targetId).getParentId();
		} else {
			nodeId = ((NodeId)targetId).getId();
		}
		try {
			this.checkPermission(nodeId, permission.getPermissionName());
			return true;
		} catch (RepositoryException e){
			return false;
		}
	}

    public void checkPermission(DatabaseConnection conn, JCRPrincipals principals, BaseSecurityPermission permission, Long nodeId, Long securityId) throws RepositoryException {
        conn = fillNullConnection(conn);
        checkPermission(conn, principals, permission, nodeId, securityId, null);
    }	
	private DatabaseConnection fillNullConnection(DatabaseConnection conn) throws RepositoryException {
        if (conn == null){
            conn = this.stateManager.getConnection();
        }
        return conn;
    }

    void checkPermission(DatabaseConnection conn, JCRPrincipals principals, BaseSecurityPermission permission, Long nodeId, Long securityId, 
            SecurityPrincipal targetPrincipal) throws RepositoryException {
	    repositorySecurityManager.checkPermission(conn, principals, permission, nodeId, securityId, 
	            targetPrincipal);
	}
	
	   public void checkPermission(BaseSecurityPermission permission, Long nodeId, Long securityId) throws RepositoryException {
	       if (getWorkspaceId() == null || principals == null || principals.getUserId() == null){
               return;
           } 
           if (!stateManager.isSecurityAllowed()){
               return ;
           }

		   _NodeState state = stateManager.getNodeFromCache(nodeId);
		   List<RowMap> allRows = null;
		   if (state != null){
			   allRows = state.getACEs(); 
		   } else {
			   allRows = RepositorySecurityManager.loadSecurityPermissions(repository, getConnection(), principals, Arrays.asList(new Long[]{securityId}));
		   }

           checkPermission(permission, allRows, nodeId, securityId);
	    }
	   
	   public void checkPermission(BaseSecurityPermission permission, List<RowMap> allRows, Long nodeId,  Long securityId) throws RepositoryException {
	       if (getWorkspaceId() == null || principals == null || principals.getUserId() == null){
               return;
           } 
           if (!stateManager.isSecurityAllowed()){
               return ;
           }

           repositorySecurityManager.checkPermission(getConnection(), principals, permission, nodeId, securityId, null, allRows);
	    }
	   
/*	    public void checkPermissionBySecurityId(Long securityId, SecurityPermission permission) throws RepositoryException {
	        //Long securityId = buildNode(nodeId).getSecurityId();
	        if (getWorkspaceId() == null || principals.getUserId() == null){
	            return;
	        } 
	        if (!stateManager.isSecurityAllowed()){
	            return ;
	        }
	        DatabaseConnection conn = getConnection();
	        try {
	            checkPermission(conn, principals, permission, securityId);
	        } finally {
	            conn.close();
	        }
	    }
*/
	   //deprecated methods
	   
	    @Deprecated
	    public void removeUserPermission(Node node, String _userId, boolean propagate) throws RepositoryException {
	        removePermission(node, SecurityPrincipal.create(_userId, null, null), propagate);    
	    }

	    @Deprecated
	    public void removeGroupPermission(Node node, String _groupId, boolean propagate) throws RepositoryException {
	        removePermission(node, SecurityPrincipal.create(null, _groupId, null), propagate);
	    }
	
	    //set user permission
	    
	    @Deprecated
	    public void setUserPermission(Long nodeId, String _userId, SecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException{
	           setUserPermission(nodeId, _userId, null, permission, permit, propagate);
	    }   
	    @Deprecated
	    public void setUserPermission(Long nodeId, String _userId, String contextId, SecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException{
	        _NodeState node = buildNode(nodeId);
	        setPermission(node, _userId, null, contextId, permission, permit, propagate);
	    }
	    @Deprecated
	    public void setUserPermission(Node node, String _userId, BaseSecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException{
	        //setPermission(((NodeImpl) node).getNodeState(), _userId, null, permission, permit, propagate);
	        setUserPermission(node, _userId, null, permission, permit, propagate);
	    }
	    @Deprecated
	    public void setUserPermission(Node node, String _userId, String contextId, BaseSecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException{
	        setPermission(((NodeImpl) node).getNodeState(), _userId, null, contextId, permission, permit, propagate);
	    }
	    
	    
	    
	    /*public void setGrantRestriction(_NodeState node, SecurityPrincipal targetUser, SecurityPrincipal restriction ) throws RepositoryException{
	        DatabaseConnection conn = getConnection();
	        try {
	            this.repositorySecurityManager.assignRestriction(conn, node.getSecurityId(), targetUser, restriction);
	            conn.commit();
	        } finally{
	            conn.close();
	        }
	    }*/
	    
	    //set group permission
	    
	    @Deprecated
	    public void setGroupPermission(Long nodeId, String _groupId, String contextId, String permission, boolean permit, boolean propagate) throws RepositoryException{
	        _NodeState node = buildNode(nodeId);
	        setPermission(node, null,  _groupId, contextId, getPermission(permission), permit, propagate);
	    }

	    @Deprecated
	    public void setGroupPermission(Long nodeId, String _groupId, String permission, boolean permit, boolean propagate) throws RepositoryException{
	        setGroupPermission(nodeId, _groupId, null, permission, permit, propagate);
	    }

	    @Deprecated
	    public void setGroupPermission(Long nodeId, String _groupId, String contextId, SecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException{
	        _NodeState node = buildNode(nodeId);
	       setPermission(node, null, _groupId, contextId, permission, permit, propagate);
	   }
	    @Deprecated
	    public void setGroupPermission(Long nodeId, String _groupId, SecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException{
	       setGroupPermission(nodeId, _groupId, null, permission, permit, propagate);
	   }
	    @Deprecated
	    public void setGroupPermission(Node node, String _groupId, SecurityPermission permission, boolean permit, boolean propagate)  throws RepositoryException{
	        setGroupPermission((NodeImpl) node, _groupId, permission, permit, propagate);
	    }

	    @Deprecated
	    public void setGroupPermission(Node node, String _groupId, String contextId, SecurityPermission permission, boolean permit, boolean propagate)  throws RepositoryException{
	        setGroupPermission((NodeImpl) node, _groupId, contextId, permission, permit, propagate);
	    }

	    @Deprecated
	    public void setGroupPermission(NodeImpl node, String _groupId, String contextId, SecurityPermission permission, boolean permit, boolean propagate)  throws RepositoryException{
	        setPermission(((NodeImpl) node).getNodeState(), null, _groupId, contextId, permission, permit, propagate);
	    }

	    @Deprecated
	    public void setGroupPermission(NodeImpl node, String _groupId, SecurityPermission permission, boolean permit, boolean propagate)  throws RepositoryException{
	        setGroupPermission(node, _groupId, null, permission, permit, propagate);
	    }

	    @Deprecated
	    private void setPermission(_NodeState node, String _userId, String _groupId, String contextId, BaseSecurityPermission permission, boolean permit, boolean propagate) throws RepositoryException {
	        ArrayList<SecurityPermissionDefinition> defs = new ArrayList<SecurityPermissionDefinition>();
	        defs.add(new SecurityPermissionDefinition(permission, SecurityPrincipal.create(_userId, _groupId, contextId), permit, propagate));
	        setPermission(node, defs);
	    }

        public BaseSecurityPermission getPermissionByName(String permissionName) throws RepositoryException{
               for(BaseSecurityPermission p:repositorySecurityManager.getAllPermissions()){
                   if (p.getPermissionName().equals(permissionName)){
                       return p;
                   }
               }
            throw new RepositoryException("Permission "+permissionName+" not found");
        }
	    
        public List<BaseSecurityPermission> getAllPermissions() {
            return this.repositorySecurityManager.getAllPermissions();
        }

}


/*
 * $Log: SessionSecurityManager.java,v $
 * Revision 1.28  2009/03/16 12:13:20  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.27  2009/01/28 06:50:59  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.26  2009/01/09 13:54:45  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.25  2008/12/22 09:08:49  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.24  2008/12/03 08:52:28  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.23  2008/11/14 07:04:06  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.22  2008/11/06 10:32:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.21  2008/10/07 12:05:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.20  2008/09/29 11:32:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.19  2008/07/22 09:06:26  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.18  2008/07/17 06:35:02  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.17  2008/07/16 13:06:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.16  2008/07/16 11:42:51  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.15  2008/07/16 08:45:04  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.14  2008/06/30 10:41:33  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.13  2008/06/26 07:20:49  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.12  2008/06/18 08:18:01  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.11  2008/06/13 09:35:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/06/11 10:07:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/06/09 12:36:14  dparhomenko
 * *** empty log message ***
 *
 *
 */