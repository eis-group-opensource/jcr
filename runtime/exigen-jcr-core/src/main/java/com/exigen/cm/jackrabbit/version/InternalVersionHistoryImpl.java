/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.NodeReference;
import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.state2._AbstractsStateManager;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.ValueFactoryImpl;
/**
 * Implements a <code>InternalVersionHistory</code>
 */
public class InternalVersionHistoryImpl extends InternalVersionItemImpl
        implements InternalVersionHistory {

    /**
     * default logger
     */
    private static Log log = LogFactory.getLog(InternalVersionHistory.class);

    /**
     * the cache of the version labels
     * key = version label (String)
     * value = version
     */
    private HashMap labelCache = new HashMap();

    /**
     * the root version of this history
     */
    private InternalVersion rootVersion;

    /**
     * the hashmap of all versions
     * key = versionId (String)
     * value = version
     */
    private HashMap<String, InternalVersionImpl> versionCache = new HashMap<String, InternalVersionImpl>();

    /**
     * The nodes state of this version history
     */
    private NodeStateEx node;

    /**
     * the node that holds the label nodes
     */
    private NodeStateEx labelNode;

    /**
     * the id of this history
     */
    private Long historyId;

    /**
     * the if of the versionable node
     */
    private String versionableId;

    /**
     * Creates a new VersionHistory object for the given node state.
     */
    public InternalVersionHistoryImpl(VersionManagerImpl vMgr, NodeStateEx node)
            throws RepositoryException {
        super(vMgr);
        this.node = node;
        init();
    }

    /**
     * Initialies the history and loads all internal caches
     *
     * @throws RepositoryException
     */
    void init() throws RepositoryException {
        versionCache.clear();
        labelCache.clear();

        // get id
        historyId = node.getNodeId();

        // get versionable id
        versionableId = (String) node.getPropertyState(QName.JCR_VERSIONABLEUUID, true).getString();

        // get entries
        List<NodeStateEx> children = node.getNodes();
        for (int i = 0; i < children.size(); i++) {
            NodeStateEx child = children.get(i);
            if (child.getQName().equals(QName.JCR_VERSIONLABELS)) {
                labelNode = child;
                continue;
            }
            //InternalVersionImpl v = new InternalVersionImpl(this, child, child.getName());
            InternalVersionImpl v = new InternalVersionImpl(this, child);
            versionCache.put(child.getUUID(), v);
            if (v.isRootVersion()) {
                rootVersion = v;
            }
        }

        // resolve successors and predecessors
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersionImpl v = (InternalVersionImpl) iter.next();
            v.resolvePredecessors();
        }

        //try {
            // init label cache
            List<_PropertyState> labels = labelNode.getProperties();
            for (int i = 0; i < labels.size(); i++) {
            	_PropertyState pState = labels.get(i);
                if (pState.getType() == PropertyType.REFERENCE || pState.getType() == PropertyType283.WEAKREFERENCE) {
                    QName name = pState.getName();
                    UUID ref = (UUID) pState.getValues()[0].internalValue();
                    InternalVersionImpl v = (InternalVersionImpl) getVersion(ref.toString());
                    labelCache.put(name, v);
                    v.internalAddLabel(name);
                }
            }
        /*} catch (ItemStateException e) {
            throw new RepositoryException(e);
        }*/
    }

    /**
     * {@inheritDoc}
     */
    public Long getId() {
        return historyId;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getParent() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getRootVersion() {
        return rootVersion;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersion(QName versionName) throws VersionException {
        // maybe add cache by name?
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            if (v.getName().equals(versionName)) {
                return v;
            }
        }
        throw new VersionException("Version " + versionName + " does not exist.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersion(QName versionName) {
        // maybe add cache?
        Iterator iter = versionCache.values().iterator();
        while (iter.hasNext()) {
            InternalVersion v = (InternalVersion) iter.next();
            if (v.getName().equals(versionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasVersion(String uuid) {
        return versionCache.containsKey(uuid);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersion(String uuid) {
        return (InternalVersion) versionCache.get(uuid);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getVersionByLabel(QName label) {
        return (InternalVersion) labelCache.get(label);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getVersions() {
        return versionCache.values().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public int getNumVersions() {
        return versionCache.size();
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionableUUID() {
        return versionableId;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getVersionLabels() {
        return (QName[]) labelCache.keySet().toArray(new QName[labelCache.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionLabelsUUID() {
        //return labelNode.getUUID();
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the indicated version from this VersionHistory. If the specified
     * vesion does not exist, if it specifies the root version or if it is
     * referenced by any node e.g. as base version, a VersionException is thrown.
     * <p/>
     * all successors of the removed version become successors of the
     * predecessors of the removed version and vice versa. then, the entire
     * version node and all its subnodes are removed.
     *
     * @param versionName
     * @throws VersionException
     */
    void removeVersion(QName versionName) throws RepositoryException {

        InternalVersionImpl v = (InternalVersionImpl) getVersion(versionName);
        log.debug(v.getUUID());
        log.debug(v.getNode().getPath());
        if (v.equals(rootVersion)) {
            String msg = "Removal of " + versionName + " not allowed.";
            log.debug(msg);
            throw new VersionException(msg);
        }
        // check if any references (from outside the version storage) exist on this version
        List<PropertyImpl> refs = getVersionManager().getItemReferences(v);
        if (!refs.isEmpty()) {
            throw new ReferentialIntegrityException("Unable to remove version. At least once referenced.");
        }
        QName[] labels = v.internalGetLabels();
        v.internalDetach();
        getVersionManager().getItemReferences(v);

        // remove from persistance state
        node.removeChildNode(v.getName(),1 , false, false);

        getVersionManager().getItemReferences(v);

        // unregister from labels
        for (int i = 0; i < labels.length; i++) {
            v.internalRemoveLabel(labels[i]);
            labelNode.getNodeState().removeChildProperty(labels[i], true);
        }
        // detach from the version graph
        v.internalDetach();
        
        //remove references to version
        ArrayList<NodeReference> from = v.getNode().getNodeState().getReferencesFrom();
        _AbstractsStateManager sm = v.getNode().getStateManager();
//      TODO optimize read Ahead
        for(NodeReference nr: from){
        	Long nId = nr.getFromId();
        	_NodeState ns = sm.getNodeState(nId, null);
        	_PropertyState ps = ns.getProperty(nr.getPropertyQName(), true);
        	String uuid = v.getUUID();
        	if (ps.getDefinition().isMultiple()){
        		InternalValue[] values = ps.getValues();
        		ArrayList<InternalValue> newValues  = new ArrayList<InternalValue>();
        		for(InternalValue value : values){
        			Object iv = value.internalValue(); 
        			if (iv instanceof UUID ){
        				iv = ((UUID)iv).toString();
        			}
        			if (!iv.equals(uuid)){
        				newValues.add(value);
        			}
        		}
        		//ps.setValues(newValues.toArray(new InternalValue[newValues.size()]));
        		ns.internalSetProperty(ps.getName(), newValues.toArray(new InternalValue[newValues.size()]) , ps.getType(), false );
        	} else {
        		ns.removeChildProperty(nr.getPropertyQName(), true);
        	}
        }
        from = v.getNode().getNodeState().getReferencesFrom();
        String vuuid = v.getUUID();
        
        // and remove from history
        versionCache.remove(v.getId());
        versionCache.remove(vuuid);

        // store changes
        node.save(true, false,true);
        //List refs = getVersionManager().getItemReferences(v);
    }

	private String step(int step) {
		StringBuffer sb = new StringBuffer();
		for(int i = 0 ; i < step ; i++){
			sb.append("    ");
		}
		return sb.toString();
	}

	/**
     * Sets the version <code>label</code> to the given <code>version</code>.
     * If the label is already assigned to another version, a VersionException is
     * thrown unless <code>move</code> is <code>true</code>. If <code>version</code>
     * is <code>null</code>, the label is removed from the respective version.
     * In either case, the version the label was previously assigned to is returned,
     * or <code>null</code> of the label was not moved.
     *
     * @param versionName the name of the version
     * @param label the label to assgign
     * @param move  flag what to do by collisions
     * @return the version that was previously assigned by this label or <code>null</code>.
     * @throws VersionException
     */
    InternalVersion setVersionLabel(QName versionName, QName label, boolean move)
            throws VersionException {

        InternalVersion version =
            (versionName != null) ? getVersion(versionName) : null;
        if (versionName != null && version == null) {
            throw new VersionException("Version " + versionName + " does not exist in this version history.");
        }
        InternalVersionImpl prev = (InternalVersionImpl) labelCache.get(label);
        if (prev == null) {
            if (version == null) {
                return null;
            }
        } else {
            if (prev.equals(version)) {
                return version;
            } else if (!move) {
                // already defined elsewhere, throw
                throw new VersionException("Version label " + label + " already defined for version " + prev.getName());
            }
        }

        // update persistence
        try {
            if (version == null) {
                labelNode.getNodeState().removeChildProperty(label, true);
            } else {
                labelNode.setPropertyValue(label, InternalValue.create(new UUID(version.getUUID()), false));
            }
            labelNode.save(true, false,true);
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }

        // update internal structures
        if (prev != null) {
            prev.internalRemoveLabel(label);
            labelCache.remove(label);
        }
        if (version != null) {
            labelCache.put(label, version);
            ((InternalVersionImpl) version).internalAddLabel(label);
        }
        return prev;
    }

    /**
     * Checks in a node. It creates a new version with the given name and freezes
     * the state of the given node.
     *
     * @param name
     * @param src
     * @return
     * @throws RepositoryException
     */
    InternalVersionImpl checkin(QName name, NodeImpl src)
            throws RepositoryException {

        // copy predecessors from src node
        Value[] preds = src.getProperty(QName.JCR_PREDECESSORS).getValues();
        InternalValue[] predecessors = new InternalValue[preds.length];
        for (int i = 0; i < preds.length; i++) {
            String predId = preds[i].getString();
            // check if version exist
            if (!versionCache.containsKey(predId)) {
                throw new RepositoryException("invalid predecessor in source node");
            }
            predecessors[i] = InternalValue.create(new UUID(predId), false);
        }

        //String versionId = UUID.randomUUID().toString();
        NodeStateEx vNode = (NodeStateEx) node.addNode(name, QName.NT_VERSION, null);

        // initialize 'created' and 'predecessors'
        vNode.setPropertyValue(QName.JCR_CREATED, InternalValue.create(Calendar.getInstance()));
        vNode.setPropertyValues(QName.JCR_PREDECESSORS, predecessors, PropertyType.REFERENCE);

        // initialize 'empty' successors; their values are dynamically resolved
        vNode.setPropertyValues(QName.JCR_SUCCESSORS, InternalValue.EMPTY_ARRAY, PropertyType.REFERENCE);

        // checkin source node
        InternalFrozenNodeImpl.checkin(vNode, QName.JCR_FROZENNODE, src);

        // update version graph
        InternalVersionImpl version = new InternalVersionImpl(this, vNode);
        version.resolvePredecessors();
        //version.internalAttach();

        for(InternalValue p:predecessors){
        	UUID uuid = (UUID) p.internalValue();
        	InternalVersionImpl v0 =  versionCache.get(uuid.toString());
	        InternalVersion[] vv = v0.getSuccessors();
	        Value[] v11 = new Value[vv.length];
	        int i=0;
	        for(InternalVersion v:vv){
	        	//v11[i] = InternalValue.create(((InternalVersionImpl)v).getUUID());
	        	v11[i] = ValueFactoryImpl.getInstance().createValue(((InternalVersionImpl)v).getUUID());
	        	i++;
	        }
	        v0.getNode().setProperty(QName.JCR_SUCCESSORS, v11);
	        //vNode.setPropertyValues(QName.JCR_SUCCESSORS, v11, PropertyType.REFERENCE);
        }
        
        // and store
        node.save(true, false,true);

        // update cache
        versionCache.put(vNode.getUUID(), version);

        return version;

    }

    /**
     * Creates a new <code>InternalVersionHistory</code> below the given parent
     * node and with the given name.
     *
     * @param parent
     * @param name
     * @return
     * @throws RepositoryException
     */
    static InternalVersionHistoryImpl create(VersionManagerImpl vMgr,
                                             NodeStateEx parent,
                                             NodeId historyId, QName name,
                                             List created,
                                             String uuid,
                                             QName primaryTypeName,
                                             Set<QName> mixinTypeNames)
            throws RepositoryException {

        // create history node
        NodeStateEx pNode = (NodeStateEx) parent.addNode(name, QName.NT_VERSIONHISTORY, null, false, false, true, false);
        //TODO uncomment me
        //created.add(pNode.getUUID());

        // set the versionable uuid
        pNode.setPropertyValue(QName.JCR_VERSIONABLEUUID, InternalValue.create(uuid));

        // create label node
        //NodeStateEx lNode = (NodeStateEx) pNode.addNode(QName.JCR_VERSIONLABELS, QName.NT_VERSIONLABELS, null);
        //TODO uncomment me
        //created.add(lNode.getUUID());

        //throw new UnsupportedOperationException();
        // create root version
        //Long versionId = UUID.randomUUID().toString();

        //NodeStateEx vNode = (NodeStateEx) pNode.addNode(QName.JCR_ROOTVERSION, QName.NT_VERSION, null);
        NodeStateEx vNode = (NodeStateEx) pNode.getNode(QName.JCR_ROOTVERSION);
        //TODO uncomment me
        //created.add(vNode.getUUID());

        // initialize 'created' and 'predecessors'
        vNode.setPropertyValue(QName.JCR_CREATED, InternalValue.create(Calendar.getInstance()), false, false);
        vNode.setPropertyValues(QName.JCR_PREDECESSORS, InternalValue.EMPTY_ARRAY, PropertyType.REFERENCE, false, false);
        vNode.setPropertyValues(QName.JCR_SUCCESSORS, InternalValue.EMPTY_ARRAY, PropertyType.REFERENCE, false, false);

        // add also an empty frozen node to the root version
        NodeStateEx node = (NodeStateEx) vNode.addNode(QName.JCR_FROZENNODE, QName.NT_FROZENNODE, null, false, false, true, false);
        //NodeStateEx node = (NodeStateEx) vNode.getNode(QName.JCR_FROZENNODE);
        //TODO uncomment me
        //created.add(node.getUUID());
        
        // initialize the internal properties
        node.setPropertyValue(QName.JCR_FROZENUUID, InternalValue.create(uuid), false, false);
        node.setPropertyValue(QName.JCR_FROZENPRIMARYTYPE,
                InternalValue.create(primaryTypeName), false, false);

        Set mixins = mixinTypeNames;
        if (mixins.size() > 0) {
            InternalValue[] ivalues = new InternalValue[mixins.size()];
            Iterator iter = mixins.iterator();
            for (int i = 0; i < mixins.size(); i++) {
                ivalues[i] = InternalValue.create((QName) iter.next());
            }
            node.setPropertyValues(QName.JCR_FROZENMIXINTYPES, ivalues, PropertyType.NAME, false, false);
        }

        //parent.save();
        InternalVersionHistoryImpl history = new InternalVersionHistoryImpl(vMgr, pNode);
        vMgr.addHistoryItem(history);
        return history;
    }

	public _NodeState getNodeState() {
		return node.getNodeState();
	}
}
