/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SessionSecurityManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.version.VersionHistoryImpl;
import com.exigen.vf.commons.logging.LogUtils;
public class NodeManager {

    /** Log for this class */
    private static final Log log = LogFactory.getLog(NodeManager.class);

	//private _StateManager stateManager;
	
    private SoftHashMap<Long, NodeImpl> loadedNodes = new SoftHashMap<Long, NodeImpl>(25);

    private Long workspaceId;


    //old variables
    
    
    //private WeakHashMap<NodeImpl, Long> _loadedNodes = new WeakHashMap<NodeImpl, Long>();
    //private HashSet<Long> loadedNodeIds = new HashSet<Long>(); 
    //private HashMap<Long, NodeImpl> deletedNodes = new HashMap<Long, NodeImpl>(0);
    //private ArrayList<NodeImpl> preventEvict = new ArrayList<NodeImpl>();
    //TODO may be weak ?
    //private HashMap<Path, Long> paths = new HashMap<Path, Long>();
    //private HashMap<String, Long> uuids = new HashMap<String, Long>();

    private ArrayList<NodeImpl> modifiedNodes = new ArrayList<NodeImpl>();

    private SessionImpl session;
    private long versionHistoryNodeTypeId;
    private long versionNodeTypeId;


    private SessionSecurityManager securityManager;


	//private RepositoryImpl repository;


	
	//new variables
	
    public NodeManager(SessionImpl session) throws NoSuchNodeTypeException, RepositoryException {
        this.session = session;
        //this.repository = session._getRepository();
        //this.stateManager = session.getStateManager();
        this.workspaceId = session._getWorkspace().getWorkspaceId();
    }

    public void init() throws NoSuchNodeTypeException, RepositoryException{
        NodeTypeImpl vhNodeType = session.getNodeTypeManager().getNodeType(QName.NT_VERSIONHISTORY);
        if (vhNodeType != null) {
            versionHistoryNodeTypeId = vhNodeType.getSQLId().longValue();
        } else {
            versionHistoryNodeTypeId = -1;
        }        

        NodeTypeImpl vNodeType = session.getNodeTypeManager().getNodeType(QName.NT_VERSION);
        if (vNodeType != null) {
            versionNodeTypeId = vNodeType.getSQLId().longValue();
        } else {
            versionNodeTypeId = -1;
        }      
        this.securityManager = session.getSecurityManager();
    }
    
    public NodeImpl buildNode(_NodeState nodeState) throws RepositoryException {
        return buildNode(nodeState, true);
    }

    public NodeImpl buildNode(_NodeState nodeState, boolean checkSecurity) throws RepositoryException {
        return buildNode(nodeState, checkSecurity, false);
    }
    
    public NodeImpl buildNode(_NodeState nodeState, boolean checkSecurity, boolean allowRemoved) throws RepositoryException {
        LogUtils.debug(log, "Build node {0}", nodeState.getNodeId());
        //if (log.isTraceEnabled()){
        //    log.trace("Build Node", new Exception());
        //}
        //find node
        NodeImpl n = null;
        n = _findLoadedNode(nodeState.getNodeId());
        if (n != null){
            return n;
        }
        /*if (allowRemoved){
            n = deletedNodes.get(nodeId);
            if (n != null){
                return n;
            }   
        }*/
        //create node
        
        
        
        try {

            
            //check security
            if (checkSecurity){
                Long securityId = nodeState.getSecurityId();
                try {
                    securityManager.checkPermission(SecurityPermission.READ, nodeState.getNodeId(), securityId);
                } catch (AccessDeniedException exc){
                    securityManager.checkPermission(SecurityPermission.BROWSE, nodeState.getNodeId(), securityId);
                }
            }
            
            Long nodeTypeId = nodeState.getNodeTypeId();
            //NodeId _nodeId = session.getStateManager().buildNodeId(nodeState);
            if (nodeTypeId != null && nodeTypeId == versionHistoryNodeTypeId){
                n = new VersionHistoryImpl( nodeState, session.getStateManager());
            } else if (nodeTypeId != null && nodeTypeId == versionNodeTypeId){
                n = new VersionImpl( nodeState, session.getStateManager());
            } else {
                n = new NodeImpl( nodeState, session.getStateManager());
            }
            if (nodeState.getWorkspaceId() != null &&  workspaceId != null &&  !workspaceId.equals(nodeState.getWorkspaceId()) ){
            	throw new RepositoryException("Internal Repository Error");
            }

            //registerNode(n);
            return n;
        } catch (NullPointerException exc){
            exc.printStackTrace();
            throw exc;
        } 

    }


    private NodeImpl _findLoadedNode(Long nodeId) {
        return loadedNodes.get(nodeId);
    }

    private DatabaseConnection getConnection() throws RepositoryException {
        return session.getConnection();
    }

    /*public void registerUUID(NodeImpl n) throws RepositoryException, UnsupportedRepositoryOperationException {
        if (n.isNodeType(QName.MIX_REFERENCEABLE)) {
            //TODO check another uuid registration fo this node
            String uuid = n.getUUID();
            uuids.put(uuid, n.getNodeId());
        }
    }*/
    

    public void registerModifedNode(NodeImpl node){
        if (!modifiedNodes.contains(node)){
            modifiedNodes.add(node);
        }
    }

    public void unregisterPath(NodeImpl node) throws RepositoryException {
        Path path = node.getPrimaryPath();
        unregisterPath(path);
    }

    public void unregisterPath(Path path) throws RepositoryException {
        //paths.remove(path);
    	throw new UnsupportedOperationException();
    }

    public void unregisterUUID(NodeImpl node) throws RepositoryException {
        //TODO
    	throw new UnsupportedOperationException();
    }

    public void unRegisterModifedNode(NodeImpl node) {
        modifiedNodes.remove(node);
    }
    

    public void registerNode(NodeImpl node) throws RepositoryException {
        //loadedNodeIds.add(node.getNodeId());
        /*registerPath(node);*/
        //if (loadedNodeIds.size() > 300 && loadedNodeIds.size() > _loadedNodes.size() + 200){
        //    loadedNodeIds.clear();
        //    loadedNodeIds.addAll(_loadedNodes.values());
        //    
        //}
    }


    public NodeImpl[] getModifiedNodesDirect(NodeImpl node) throws RepositoryException {
        //TODO use updatable tree for modified node 
        Long nodeId = node.state.getNodeId();
        ArrayList<NodeImpl> result = new ArrayList<NodeImpl>();
        for(Iterator it = modifiedNodes.iterator() ; it.hasNext() ; ){
            NodeImpl n = (NodeImpl) it.next();
            if (n.getNodeState().hasParent(nodeId)){
                boolean skip = false;
                for(Iterator it1 = result.iterator() ; it1.hasNext() ;){
                    NodeImpl n1 = (NodeImpl) it1.next();
                    if (n.getNodeState().hasParent(n1.state.getNodeId())){
                        skip = true;
                        break;
                    }
                }
                if (!skip){
                    result.add(n);
                    ArrayList<NodeImpl> removed = new ArrayList<NodeImpl>();
                    for(Iterator it1 = result.iterator() ; it1.hasNext() ;){
                        NodeImpl n1 = (NodeImpl) it1.next();
                        if (n1.getNodeState().hasParent(n.state.getNodeId())){
                            removed.add(n1);
                        }
                    }
                    result.removeAll(removed);
                }
            }
        }
        /*Collections.sort(result, new ModifiedNodeComparator());
        return (NodeImpl[]) result.toArray(new NodeImpl[result.size()]);*/
        throw new UnsupportedOperationException();
    }

/*    public List<NodeImpl> getModifiedNodesAll(NodeImpl node) throws RepositoryException {
        //TODO use updatable tree for modified node 
        Long nodeId = node.getNodeId();
        ArrayList<NodeImpl> result = new ArrayList<NodeImpl>();
        for(Iterator it = modifiedNodes.iterator() ; it.hasNext() ; ){
            NodeImpl n = (NodeImpl) it.next();
            if (n.getState().hasParent(nodeId)){
                result.add(n);
            }
        }
        return result;
    }
*/

    public NodeImpl getNodeByPath(Path parent, QName name, int index) throws RepositoryException{
        try {
            return getNodeByPath(Path.create(parent, name, index, true));
        } catch (MalformedPathException e){
            throw new RepositoryException();
        }
    }


    public NodeImpl getNodeByPath(Path  path) throws RepositoryException {
        /*if (paths.containsKey(path)){
        	try {
        		return buildNode((Long) paths.get(path), true);
        	} catch (ItemNotFoundException exc){
        		return null;
        	}
        }
        return null;*/
    	throw new UnsupportedOperationException();

    }



    /*public boolean hasChildNodes(Long nodeId) throws RepositoryException {
        
    }*/


    public void unRegisterNode(NodeImpl node) {
        /*deletedNodes.put(node.getNodeId(), node);
        loadedNodes.remove(node.getNodeId());
        for (Iterator it = paths.keySet().iterator() ; it.hasNext() ; ){
            Object key = it.next();
            Object value = paths.get(key);
            if (value.equals(node.getNodeId())){
                paths.remove(key);
                break;
            }
        }
        for (Iterator it = uuids.keySet().iterator() ; it.hasNext() ; ){
            Object key = it.next();
            Object value = uuids.get(key);
            if (value.equals(node.getNodeId())){
                uuids.remove(key);
                break;
            }
        }*/
    	throw new UnsupportedOperationException();
    }





    public NodeImpl __removeme__getItemByUUID(String uuid, boolean getDeleted) throws RepositoryException {
        //1 check local cache
        /*if (uuids.containsKey(uuid)){
            
            NodeImpl n =  buildNode((Long)uuids.get(uuid), true);
            if (n._status == NodeImpl.STATUS_DESTROYED || n._status == NodeImpl.STATUS_INVALIDATED){
                if (!getDeleted){
                    throw new ItemNotFoundException("Item with uuid "+uuid+" not found");
                }
                
            }
            return n;
        }
        //2.search in database
        //TODO use one query with join ...
        //2.a find in ReferenceDetail table
        DatabaseConnection conn = getDatabaseConnection();
        DatabaseSelectOneStatement st = null;
        try {
            //find mix:referenceable
            
            NodeTypeImpl nt = (NodeTypeImpl) session.getNodeTypeManager().getNodeType(QName.MIX_REFERENCEABLE);
            String tableName = nt.getTableName();
            PropDef prop = nt.getEffectiveNodeType().getApplicablePropertyDef(QName.JCR_UUID, PropertyType.STRING, false);
            String fieldName = prop.getColumnName();
            st = DatabaseTools.createSelectOneStatement(tableName, fieldName, uuid);
            st.execute(conn);
            HashMap row = st.getRow();
            Long id = (Long) row.get(Constants.FIELD_TYPE_ID);
            //2.b build node
            return buildNode(id, true); 
        } finally {
            if (st != null){
                st.close();
            }
            conn.close();
        }*/
    	throw new UnsupportedOperationException();
    }


    private DatabaseConnection getDatabaseConnection() throws RepositoryException {
        return session.getConnection();
    }





    public boolean isModified(NodeImpl node) {
        if (modifiedNodes.contains(node)){
            return true;
        }
        return false;
    }


    public NodeImpl getLoadedNode(Long nodeId) {
        return (NodeImpl) _findLoadedNode(nodeId);
    }

    public void evict(NodeImpl n) {
        /*loadedNodes.remove(n.getNodeId());
        modifiedNodes.remove(n);
        */
    	
    	throw new UnsupportedOperationException();
    }

    public void registerPath(NodeImpl node) throws RepositoryException {
        //paths.put(node.getPrimaryPath(), node.getNodeId());
    	throw new UnsupportedOperationException();

    }

    public void evictNonModifiedNodes() {
        /*loadedNodes.clear();
        for(NodeImpl node:modifiedNodes){
            loadedNodes.put(node.getNodeId(), node);
        }
        //TODO may be add deleted nodes ?
        */
    	throw new UnsupportedOperationException();
    }

	public NodeImpl buildNode(Long id, boolean checkSecurity, boolean allowRemoved) throws RepositoryException {
		return buildNode(session.getStateManager().getNodeState(id, null),checkSecurity,allowRemoved);
	}

	public NodeImpl buildNode(Long id) throws RepositoryException {
		return buildNode(session.getStateManager().getNodeState(id, null));
	}


    
}

/*
 * $Log: NodeManager.java,v $
 * Revision 1.6  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/07/16 11:42:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/06/18 08:18:01  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/06/09 12:36:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/06/02 11:36:11  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.20  2007/03/02 09:31:58  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.19  2006/12/15 09:36:44  dparhomenko
 * PTR#0149618 fix child node remove premission check
 *
 * Revision 1.18  2006/11/14 07:37:19  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.17  2006/10/30 15:03:35  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.16  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.15  2006/10/11 13:08:55  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.14  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.13  2006/08/14 10:16:14  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.12  2006/08/11 09:27:27  dparhomenko
 * PTR#1802633 fix problem with delete node
 *
 * Revision 1.11  2006/08/07 14:25:54  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.10  2006/06/26 14:01:41  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.9  2006/06/15 13:18:02  dparhomenko
 * PTR#0146580 fix sns remove on root node
 *
 * Revision 1.8  2006/06/09 08:55:40  dparhomenko
 * PTR#1802035 fix version history
 *
 * Revision 1.7  2006/06/02 07:21:28  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.6  2006/05/22 14:48:02  dparhomenko
 * PTR#1801941 add observationsupport
 *
 * Revision 1.5  2006/04/21 12:11:34  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.4  2006/04/20 11:42:46  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.3  2006/04/19 08:06:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/18 12:49:40  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.13  2006/04/12 12:48:59  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.12  2006/04/12 08:30:49  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.11  2006/04/10 11:30:12  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.10  2006/04/06 13:09:28  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.9  2006/04/06 09:16:21  dparhomenko
 * PTR#0144983 MS FTS
 *
 * Revision 1.8  2006/04/05 12:48:07  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.7  2006/04/04 11:46:08  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.6  2006/03/31 13:41:21  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.5  2006/03/31 10:41:51  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.4  2006/03/31 07:53:54  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.3  2006/03/29 12:56:19  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.2  2006/03/27 15:05:10  dparhomenko
 * PTR#0144983 remove _JCRHelper
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.15  2006/03/27 14:27:22  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.14  2006/03/27 07:22:22  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.13  2006/03/23 14:26:49  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.12  2006/03/21 13:19:27  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.11  2006/03/16 13:13:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.10  2006/03/14 11:55:40  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.9  2006/03/03 10:33:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.8  2006/03/01 11:54:46  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.7  2006/02/27 15:53:55  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.6  2006/02/27 15:02:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.5  2006/02/21 13:53:21  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.4  2006/02/20 15:32:25  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.3  2006/02/17 13:03:43  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.2  2006/02/16 15:47:59  dparhomenko
 * PTR#0144983 restructurize
 *
 * Revision 1.1  2006/02/16 13:53:05  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 */