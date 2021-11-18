/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.collections.map.ReferenceMap;
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
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyId;
import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.VersionImpl;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.virtual.VirtualItemStateProvider;


/**
 * This Class implements a VersionManager.
 */
public class VersionManagerImpl implements VersionManager {

    /** Logger for this class */
    private static final Log log = LogFactory.getLog(VersionManagerImpl.class);

    
    SessionImpl session;

    private NodeStateEx _historyRoot;
    
    /**
     * the virtual item state provider that exposes the version storage
     */
    //private VirtualItemStateProvider versProvider;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private ReferenceMap versionItems = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    private Long versionStorageNodeId;


	private RepositoryImpl repository;


	private String _historyRootPath;

    public VersionManagerImpl(SessionImpl session, Long versionStorageNodeId) {
        this.session = session;
        this.repository = session._getRepository();
        
        /*if (!pMgr.exists(new NodeId(rootUUID))) {
            NodeState root = pMgr.createNew(new NodeId(rootUUID));
            root.setParentUUID(rootParentUUID);
            root.setDefinitionId(ntReg.getEffectiveNodeType(QName.REP_SYSTEM).getApplicableChildNodeDef(
                    QName.JCR_VERSIONSTORAGE, QName.REP_VERSIONSTORAGE).getId());
            root.setNodeTypeName(QName.REP_VERSIONSTORAGE);
            PropertyState pt = pMgr.createNew(new PropertyId(rootUUID, QName.JCR_PRIMARYTYPE));
            pt.setDefinitionId(ntReg.getEffectiveNodeType(QName.REP_SYSTEM).getApplicablePropertyDef(
                    QName.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
            pt.setMultiValued(false);
            pt.setType(PropertyType.NAME);
            pt.setValues(new com.exigen.cm.impl.tmp.InternalValue[]{InternalValue.create(QName.REP_VERSIONSTORAGE)});
            root.addPropertyName(pt.getName());
            //ChangeLog cl = new ChangeLog();
            //cl.added(root);
            //cl.added(pt);
            pMgr.store(cl);
        }*/
        
        //throw new UnsupportedOperationException();
        
        this.versionStorageNodeId = versionStorageNodeId;
    }

    public VirtualItemStateProvider getVirtualItemStateProvider() {
        throw new UnsupportedOperationException();
    }

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version checkin(NodeImpl node) throws RepositoryException {
    	return checkin(node, false);
    }
    
    public Version checkin(NodeImpl node, boolean minorChange) throws RepositoryException {
        SessionImpl session = (SessionImpl) node.getSession();
        InternalVersion version = internalCheckin(node,minorChange);

        VersionImpl v = (VersionImpl) session.getNodeManager().buildNode(version.getId());
        v.setVersion(version);
        // generate observation events
        //List events = new ArrayList();

        //TODO implement me
        //generateAddedEvents(events, (_NodeImpl) v.getParent(), v, true);

        // invalidate predecessors successor property
        InternalVersion[] preds = version.getPredecessors();
        for (int i=0; i<preds.length; i++) {
            PropertyId propId = new PropertyId(preds[i].getId(), QName.JCR_SUCCESSORS);
            //TODO do we need this ???
            //versProvider.onPropertyChanged(propId);
            //throw new UnsupportedOperationException();
        }
        //TODO implement me
        //obsMgr.dispatch(events, session);

        return v;
    }

    /**
     * Removes the specified version from the history
     *
     * @param history the version history from where to remove the version.
     * @param name the name of the version to remove.
     * @throws VersionException if the version <code>history</code> does
     *  not have a version with <code>name</code>.
     * @throws RepositoryException if any other error occurs.
     */
    public void removeVersion(VersionHistory history, QName name)
            throws VersionException, RepositoryException {
        if (!((VersionHistoryImpl) history).hasNode(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }
        // generate observation events
        //SessionImpl session = (SessionImpl) history.getSession();
        //VersionImpl version = (VersionImpl) ((VersionHistoryImpl) history).getNode(name);
        //List events = new ArrayList();
        //TODO implements me
        //generateRemovedEvents(events, (_NodeImpl) history, version, true);

        InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                ((VersionHistoryImpl) history).getInternalVersionHistory();

        try {
            beginEdit();
        } catch (IllegalStateException e) {
            throw new VersionException("Unable to start edit operation", e);
        }
        boolean succeeded = false;
        try {
            vh.removeVersion(name);
            this.executeUpdate();
            succeeded = true;
        /*} catch (ItemStateException e) {
            log.error("Error while storing: " + e.toString());*/
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                cancelEdit();
            }
        }

        // invalidate predecessors successor properties
        //TODO implements me
        /*InternalVersion preds[] = version.getInternalVersion().getPredecessors();
        for (int i=0; i<preds.length; i++) {
            PropertyId propId = new PropertyId(preds[i].getId(), QName.JCR_SUCCESSORS);
            versProvider.onPropertyChanged(propId);
        }
        obsMgr.dispatch(events, session);
        */
    }

    /**
     * {@inheritDoc}
     */
    public Version setVersionLabel(VersionHistory history, QName version,
                                   QName label, boolean move)
            throws RepositoryException {
        SessionImpl session = (SessionImpl) history.getSession();

        InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                ((VersionHistoryImpl) history).getInternalVersionHistory();
        NodeImpl labelNode = (NodeImpl) ((VersionHistoryImpl) history).getNode(QName.JCR_VERSIONLABELS);

        try {
            beginEdit();
        } catch (IllegalStateException e) {
            throw new VersionException("Unable to start edit operation", e);
        }
        InternalVersion v = null;
        boolean success = false;
        try {
            v = vh.setVersionLabel(version, label, move);
            vh.init();
            this.executeUpdate();
            success = true;
        /*} catch(ItemStateException e) {
            log.error("Error while storing: " + e.toString());*/
        } finally {
            if (!success) {
                // update operation failed, cancel all modifications
                cancelEdit();
            }
        }

        //TODO implement me
        /*
        // collect observation events
        List events = new ArrayList();
        if (version == null && v != null) {
            // label removed
            events.add(EventState.propertyRemoved(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getMixinTypeNames(),
                    labelNode.getSession()
            ));
        } else if (v == null) {
            // label added
            events.add(EventState.propertyAdded(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getMixinTypeNames(),
                    labelNode.getSession()
            ));
        } else {
            // label modified
            events.add(EventState.propertyChanged(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getMixinTypeNames(),
                    labelNode.getSession()
            ));
        }
        obsMgr.dispatch(events, session);
        */
        if (v == null) {
            return null;
        } else {
            return (VersionImpl) session.getNodeManager().buildNode(v.getId());
        }
    }


    public boolean hasVersionHistory(String id) {
        throw new UnsupportedOperationException();
    }

    public InternalVersionHistory getInternalVersionHistory(SessionImpl session, NodeId nodeId) throws RepositoryException {
        return getInternalVersionHistory(nodeId.getUUID());
    }
    public InternalVersionHistory getInternalVersionHistory(String nodeUUID) throws RepositoryException {
        DatabaseConnection conn = session.getConnection();
        try{
            NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(QName.NT_VERSIONHISTORY);
            PropDef prop = nt.getEffectiveNodeType().getApplicablePropertyDef(QName.JCR_VERSIONABLEUUID, PropertyType.STRING);
            /*DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_NODE, true);
            
            st.addJoin(nt.getTableName(), "vh_", Constants.FIELD_ID, Constants.FIELD_TYPE_ID);
            st.addCondition(Conditions.eq("vh_." + prop.getColumnName(), nodeUUID));
            
            st.addResultColumn(Constants.FIELD_ID);*/
            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(nt.getTableName(), true);
            
            //st.addJoin(nt.getTableName(), "vh_", Constants.FIELD_ID, Constants.FIELD_TYPE_ID);
            st.addCondition(Conditions.eq(prop.getColumnName(), nodeUUID));
            
            st.addResultColumn(Constants.FIELD_TYPE_ID);
            st.execute(conn);
            if (st.hasNext()){
                RowMap row = st.nextRow();
                //Long histId = row.getLong(Constants.FIELD_ID);
                Long histId = row.getLong(Constants.FIELD_TYPE_ID);
                InternalVersionHistory history = (InternalVersionHistory) versionItems.get(histId);
                if (history != null){
                    return history;
                }
//              TODO optimize read Ahead
                _SessionStateManager sm = session.getStateManager();
                NodeStateEx nodeState = new NodeStateEx(sm.getNodeState(histId, null), sm);
                history = new InternalVersionHistoryImpl(this, nodeState);
                versionItems.put(nodeState.getNodeId(), history);
                return history;
                /*InternalVersionHistoryImpl history = (InternalVersionHistoryImpl) getVersionHistory(node);
                VersionHistoryImpl vh = (VersionHistoryImpl) session.getNodeManager().buildNode(history.getId());
                vh.setHistory(history);
                return vh;*/
            } else {
                return null;
            }
        } finally {
            conn.close();
        }
    }

    public VersionHistory getVersionHistory(NodeId nodeId) throws RepositoryException {
        InternalVersionHistoryImpl history = (InternalVersionHistoryImpl) getInternalVersionHistory(session, nodeId);
        if (history != null){
            VersionHistoryImpl vh = (VersionHistoryImpl) session.getNodeManager().buildNode(history.getId());
            vh.setHistory(history);
            return vh;
            
        } else {
            return null;
        }
    }

    public boolean hasVersion(String id) {
        throw new UnsupportedOperationException();
    }

    public InternalVersion getVersion(String id) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    public void close() throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks in a node
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see javax.jcr.Node#checkin()
     */
    private synchronized InternalVersion internalCheckin(NodeImpl node, boolean minorChange) throws RepositoryException {
        // assuming node is versionable and checkout (check in nodeimpl)
        // To create a new version of a versionable node N, the client calls N.checkin.
        // This causes the following series of events:
        //String histUUID = node.getProperty(QName.JCR_VERSIONHISTORY).getString();
        InternalVersionHistoryImpl history = (InternalVersionHistoryImpl) getInternalVersionHistory((SessionImpl)node.getSession(), (NodeId)node.getId());

        // 0. resolve the predecessors
        Value[] values = node.getProperty(QName.JCR_PREDECESSORS).getValues();
        InternalVersion[] preds = new InternalVersion[values.length];
        for (int i = 0; i < values.length; i++) {
            preds[i] = history.getVersion(values[i].getString());
        }

        // 0.1 search a predecessor, suitable for generating the new name
        String versionName = null;
        int maxDots = Integer.MAX_VALUE;
        for (int i = 0; i < preds.length; i++) {
            // take the first pred. without a successor
            if (preds[i].getSuccessors().length == 0) {
                versionName = preds[i].getName().getLocalName(); //assuming no namespaces in version names
                // need to count the dots
                int pos = -1;
                int numDots = 0;
                while (versionName.indexOf('.', pos + 1) >= 0) {
                    pos = versionName.indexOf('.', pos + 1);
                    numDots++;
                }
                if (numDots < maxDots) {
                    maxDots = numDots;
                    if (pos < 0) {
                        versionName = "v1.0";
                    } else {
                        String a = versionName.substring(1);
                        a = a.substring(0,pos-1);
                        int ii = Integer.parseInt(a) + 1;
                        if (minorChange){
                        	versionName = versionName.substring(0, pos + 1)
                            + (Integer.parseInt(versionName.substring(pos + 1)) + 1);
                        } else {
                        	versionName = "v"+ii+".0";
                        }
                        
                        /*versionName = versionName.substring(0, pos + 1)
                                + (Integer.parseInt(versionName.substring(pos + 1)) + 1);*/
                    }
                }
                break;
            }
        }
        // if no empty found, generate new name
        if (versionName == null) {
            versionName = preds[0].getName().getLocalName();
            do {
                versionName += ".1";
            } while (history.hasVersion(new QName("", versionName)));
        }

        beginEdit();

        boolean succeeded = false;

        try {
            InternalVersionImpl v = history.checkin(new QName("", versionName), node);
            executeUpdate();

            succeeded = true;

            return v;
        //} catch (ItemStateException e) {
         //   throw new RepositoryException(e);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                cancelEdit();
            }
        }
        
    }

    private void executeUpdate() {
        //stateMgr.update();
        
    }

    private void cancelEdit() {
        // TODO Auto-generated method stub
        
    }

    private void beginEdit() {
        // TODO Auto-generated method stub
        
    }

    public VersionHistory createVersionHistory( NodeId nodeId, QName primaryType, Set<QName> mixinTypes, HashMap<String, Long> paths) throws RepositoryException {
        List created = new LinkedList();
        InternalVersionHistory history = createVersionHistory(created, nodeId, primaryType, mixinTypes, paths);
        if (history == null) {
            throw new VersionException("History already exists for node " + nodeId.getUUID());
        }
        VersionHistoryImpl vh = (VersionHistoryImpl) session.getNodeManager().buildNode(history.getNodeState(), false);
        vh.setHistory(history);

        // generate observation events
        //TODO implement observer
        /*List events = new ArrayList();
        Iterator iter = created.iterator();
        while (iter.hasNext()) {
            String uuid = (String) iter.next();
            NodeImpl child = (NodeImpl) ((SessionImpl) session).getItemManager().getItem(new NodeId(uuid));
            generateAddedEvents(events, (NodeImpl) child.getParent(), child, false);
        }
        obsMgr.dispatch(events, (SessionImpl) session);*/
        return vh;
    }
    /**
     * Creates a new Version History.
     *
     * @param created a list for adding the uuids of the newly created nodes
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws RepositoryException
     */
    private InternalVersionHistory createVersionHistory(List created, NodeId nodeId,QName primaryType, Set<QName> mixinTypes, HashMap<String, Long> paths)
            throws RepositoryException {

        beginEdit();

        
    	VersionStorageInitializer vi = new VersionStorageInitializer(session.getConnection(), session._getRepository(), 
    			(NamespaceRegistryImpl)session._getWorkspace().getNamespaceRegistry(), session.getNodeTypeManager());
    	ArrayList<String> newUUID = new ArrayList<String>();
    	newUUID.add(nodeId.getUUID());
    	List<Long> ids1 = vi.initialize(newUUID, session.getStateManager().getNodeState(versionStorageNodeId, null));

        //boolean succeeded = false;

        try {
            // create deep path
            String uuid = nodeId.getUUID();
            
            
            String n1 = "h"+uuid.substring(0 * 2, 0 * 2 + 2);
            String n2 = "h"+uuid.substring(1 * 2, 1 * 2 + 2);
            String n3 = "h"+uuid.substring(2 * 2, 2 * 2 + 2);
            //String n4 = "h"+uuid.substring(3 * 2, 3 * 2 + 2);
            
            String p1 = this._historyRootPath+"/"+n1+"{1}";
            String p2 = p1+"/"+n2+"{1}";
            String p3 = p2+"/"+n3+"{1}";
            //String p4 = p3+"/"+n3+"{1}";

            Long _nodeId = paths.get(p3);
            NodeStateEx root;
            if (_nodeId != null){
            	_NodeState _root = session.getStateManager().getNodeState(_nodeId, null);
            	root = new NodeStateEx(_root, session.getStateManager());
            } else {
                root = getHistoryRoot();
                session.getConnection().lockNode(root.getNodeId());
                session.getStateManager().reloadState(root.getNodeState());

                NodeStateEx hRoot = root;
                for (int i = 0; i < 3; i++) {
                    QName name = new QName(QName.NS_DEFAULT_URI, "h"+uuid.substring(i * 2, i * 2 + 2));
                    if (!root.hasNode(name)) {
                        NodeStateEx n = (NodeStateEx) root.addNode(name, QName.REP_VERSIONSTORAGE, null, false, false, true, false);
                    	//root = (NodeStateEx) root.getNode(name, 1, false);
                        root = n;
                        //TODO uncomment me (why ?)
                        //created.add(n.getUUID());
                        //root.save();
                    } else {
                    	root = (NodeStateEx) root.getNode(name, 1, false);
                    	if (root.getNodeState().getStatus() != ItemStatus.New){
                    		session.getStateManager().reloadState(root.getNodeState());
                    	}

                    }
                }

            }
            
            

            QName historyNodeName = new QName(QName.NS_DEFAULT_URI, "h"+uuid);
            if (root.hasNode(historyNodeName)) {
                // already exists
                return null;
            }

            // create new history node in the persistent state
            //throw new UnsupportedOperationException();
           NodeId historyId = new NodeId(session._getRepository().nextId(), UUID.randomUUID());
           //NodeImpl nodeState = session.getNodeById(nodeId);

           InternalVersionHistoryImpl hist = InternalVersionHistoryImpl.create(this, root, historyId, historyNodeName, created, nodeId.getUUID(), primaryType, mixinTypes);

           //hRoot.save(false);

            // end update
            executeUpdate();
            
            //uncomment me !!!
            //session.getStateManager().save(hRoot.getNodeState(), true, false, false);
            //succeeded = true;

            //log.info("Created new version history " + hist.getId() + " for " + node + ".");
            return hist;

        /*} catch (ItemStateException e) {
            throw new RepositoryException(e);*/
        } finally {
            cancelEdit();
        }
    }
    
    public NodeStateEx getHistoryRoot() throws RepositoryException{
        if (_historyRoot == null){
            DatabaseConnection conn = session.getConnection();
            try {
                _SessionStateManager sm = session.getStateManager();
                _historyRoot = new NodeStateEx(sm.getNodeState(versionStorageNodeId, null), sm);
                this._historyRootPath = _historyRoot.getNodeState().getInternalPath();
            } finally {
                conn.close();
            }
            
            
        }
        return _historyRoot;
    }
    
    /**
     * checks, if the node with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasItem(Long id) {
        //return versionItems.containsKey(id) || stateMgr.hasItemState(new NodeId(id));
        throw new UnsupportedOperationException();
    }
    /**
     * Returns the item with the given persistent id
     *
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    synchronized InternalVersionItem getItem(_NodeState _node) throws RepositoryException {
        NodeId id = repository.buildNodeId(_node.getNodeId(), session.getConnection());
        //try {
            InternalVersionItem item = (InternalVersionItem) versionItems.get(id);
            if (item == null) {
                //if (stateMgr.hasItemState(id)) {
                    //NodeState state = (NodeState) stateMgr.getItemState(id);
                    DatabaseConnection conn = session.getConnection();
                    try {
                        _SessionStateManager sm = session.getStateManager();
                        NodeStateEx pNode = new NodeStateEx(_node, sm);
                    
                        //NodeStateEx pNode = new NodeStateEx(stateMgr, ntReg, state, null);
                        _NodeState _parent = _node.getParent();
                        InternalVersionItem parent =
                                (_parent != null) ? getItem(_parent) : null;
                        //QName ntName = state.getNodeTypeName();
                        QName ntName = pNode.getNodeState().getPrimaryTypeName();
                        if (ntName.equals(QName.NT_FROZENNODE)) {
                            item = new InternalFrozenNodeImpl(this, pNode, parent);
                        } else if (ntName.equals(QName.NT_VERSIONEDCHILD)) {
                            item = new InternalFrozenVHImpl(this, pNode, parent);
                        } else if (ntName.equals(QName.NT_VERSION)) {
                            item = ((InternalVersionHistory) parent).getVersion(pNode.getUUID());
                        } else if (ntName.equals(QName.NT_VERSIONHISTORY)) {
                            item = new InternalVersionHistoryImpl(this, pNode);
                        } else {
                            //return null;
                        }
                    } finally {
                        conn.close();
                    }
                }
                if (item != null) {
                    versionItems.put(id, item);
                }
            //}
            return item;
        /*} catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
        throw new UnsupportedOperationException();*/
    }

    public void addHistoryItem(InternalVersionHistoryImpl history) {
        versionItems.put(history.getId(), history);
    }

    public List<PropertyImpl> getItemReferences(InternalVersionImpl v) throws RepositoryException {
        NodeImpl n = session.getNodeManager().buildNode(v.getId());
        ArrayList<PropertyImpl> result = new ArrayList<PropertyImpl>();
        for(PropertyIterator pi = n.getReferences(); pi.hasNext();){
        	PropertyImpl p = (PropertyImpl) pi.nextProperty();
        	if (!p._getParent().getNodeState().hasParent(versionStorageNodeId)){
        		result.add(p);
        	} else {
        		log.debug("Skip referece from "+p.getName()+" in "+p.getParent().getPath()+"   ; "+p._getParent().getNodeId()+"->"+n.getNodeId()+"  status:"+p.getItemState().getStatus());
        	}
        }
        return result;
    }    
}
