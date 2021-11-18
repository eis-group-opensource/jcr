/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.InternalValue;

/**
 * Implements a <code>InternalVersion</code>
 */
public class InternalVersionImpl extends InternalVersionItemImpl
        implements InternalVersion {

    /**
     * the list/cache of predecessors (values == InternalVersion)
     */
    private ArrayList predecessors = new ArrayList();

    /**
     * the list of successors (values == InternalVersion)
     */
    private ArrayList successors = new ArrayList();

    /**
     * the underlying persistance node of this version
     */
    //private NodeStateEx node;

    /**
     * the date when this version was created
     */
    private Calendar created;

    /**
     * the set of version labes of this history (values == String)
     */
    private HashSet labelCache = null;

    /**
     * specifies if this is the root version
     */
    private final boolean isRoot;

    /**
     * the version name
     */
    private final QName name;

    /**
     * the version history
     */
    private final InternalVersionHistory versionHistory;

    private NodeStateEx node;

    /**
     * Creates a new internal version with the given version history and
     * persistance node. please note, that versions must be created by the
     * version history.
     *
     * @param node
     * @throws ItemNotFoundException 
     */
    public InternalVersionImpl(InternalVersionHistoryImpl vh, NodeStateEx node) throws ItemNotFoundException {
        super(vh.getVersionManager());
        this.versionHistory = vh;
        this.node = node;
        this.name = node.getQName();

        // init internal values
        _PropertyState values = node.getPropertyState(QName.JCR_CREATED, false);
        if (values != null) {
            created = (Calendar) values.getValues()[0].internalValue();
        }
        isRoot = name.equals(QName.JCR_ROOTVERSION);
        //throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Long getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionItem getParent() {
        return versionHistory;
    }

    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public InternalFrozenNode getFrozenNode() {
        // get frozen node
        try {
            //NodeState.ChildNodeEntry entry = node.getState().getChildNodeEntry(QName.JCR_FROZENNODE, 1);
            //Long childNodeId = node.findNode(QName.JCR_FROZENNODE, 1);
        	_NodeState childNode = node.getStateManager().getChildNode(node.getNodeState(), QName.JCR_FROZENNODE);
            if (childNode == null) {
                throw new InternalError("version has no frozen node: " + getId());
            }
            return (InternalFrozenNode) getVersionManager().getItem(childNode);
        } catch (RepositoryException e) {
            throw new IllegalStateException("unable to retrieve frozen node: " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getCreated() {
        return created;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion[] getSuccessors() {
        return (InternalVersionImpl[]) successors.toArray(new InternalVersionImpl[successors.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion[] getPredecessors() {
        return (InternalVersionImpl[]) predecessors.toArray(new InternalVersionImpl[predecessors.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMoreRecent(InternalVersion v) {
        for (int i = 0; i < predecessors.size(); i++) {
            InternalVersion pred = (InternalVersion) predecessors.get(i);
            if (pred.equals(v) || pred.isMoreRecent(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory() {
        return versionHistory;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLabel(QName label) {
        return internalHasLabel(label);
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getLabels() {
        return internalGetLabels();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRootVersion() {
        return isRoot;
    }

    /**
     * resolves the predecessors property and indirectly adds it self to their
     * successor list.
     * @throws ItemNotFoundException 
     */
    void resolvePredecessors() throws ItemNotFoundException {
       InternalValue[] values = node.getPropertyState(QName.JCR_PREDECESSORS, true).getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalVersionImpl v = (InternalVersionImpl) versionHistory.getVersion(values[i].internalValue().toString());
                predecessors.add(v);
                v.addSuccessor(this);
            }
        }
        //throw new UnsupportedOperationException();

    }
    /*
     * 
    public InternalVersion[] getPredecessors() {
        InternalValue[] values = node.getPropertyValues(QName.JCR_PREDECESSORS);
        if (values != null) {
            InternalVersion[] versions = new InternalVersion[values.length];
            for (int i = 0; i < values.length; i++) {
                NodeId vId = new NodeId((UUID) values[i].internalValue());
                versions[i] = versionHistory.getVersion(vId);
            }
            return versions;
        } else {
            return new InternalVersion[0];
        }
    }
     */

    /**
     * adds a successor version to the internal cache
     *
     * @param successor
     */
    private void addSuccessor(InternalVersion successor) {
        successors.add(successor);
    }

    /**
     * stores the internal predecessor cache to the persistance node
     *
     * @throws RepositoryException
     */
    private void storePredecessors() throws RepositoryException {
        InternalValue[] values = new InternalValue[predecessors.size()];
        InternalValue[] values2 = new InternalValue[predecessors.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = InternalValue.create(new UUID(((InternalVersion) predecessors.get(i)).getUUID()), false);
            values2[i] = InternalValue.create(new UUID(((InternalVersion) predecessors.get(i)).getUUID()), false);
        }
        node.setPropertyValues(QName.JCR_PREDECESSORS, values, PropertyType.STRING);
        
        NodeStateEx frozenNode = (NodeStateEx)node.getNode(QName.JCR_FROZENNODE);
        frozenNode.setPropertyValues(QName.JCR_PREDECESSORS, values2, PropertyType.STRING);
        //node.get(QName.JCR_FROZENNODE)
        //QName.JCR_FROZENNODE
        
        //throw new UnsupportedOperationException();

    }

    /**
     * Detaches itself from the version graph.
     *
     * @throws RepositoryException
     */
    void internalDetach() throws RepositoryException {
        // detach this from all successors
    	
        InternalVersionImpl[] succ = (InternalVersionImpl[]) getSuccessors();
        for (int i = 0; i < succ.length; i++) {
            succ[i].internalDetachPredecessor(this);
        }
        // detach cached successors from preds
        InternalVersionImpl[] preds = (InternalVersionImpl[]) getPredecessors();
        for (int i = 0; i < preds.length; i++) {
            preds[i].internalDetachSuccessor(this);
        }

        // clear properties
        successors.clear();
        predecessors.clear();
        labelCache = null;
    }

    /**
     * Removes the predecessor V of this predecessors list and adds all of Vs
     * predecessors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachPredecessor(InternalVersionImpl v) throws RepositoryException {
        // remove 'v' from predecessor list
        for (int i = 0; i < predecessors.size(); i++) {
            if (predecessors.get(i).equals(v)) {
                predecessors.remove(i);
                break;
            }
        }
        // attach v's predecessors
        predecessors.addAll(Arrays.asList(v.getPredecessors()));
        storePredecessors();
        node.save(true, false,true);

    }

    /**
     * Removes the successor V of this successors list and adds all of Vs
     * successors to it.
     * <p/>
     * please note, that this operation might corrupt the version graph
     *
     * @param v the successor to detach
     */
    private void internalDetachSuccessor(InternalVersionImpl v) {
        // remove 'v' from successors list
        for (int i = 0; i < successors.size(); i++) {
            if (successors.get(i).equals(v)) {
                successors.remove(i);
                break;
            }
        }
        // attach v's successors
        successors.addAll(Arrays.asList(v.getSuccessors()));
        
        //_internalDetachSuccessor(getFrozenNode(), v);
    }

    /*private void _internalDetachSuccessor(InternalFrozenNode frozenNode, InternalVersionImpl v) {
		
		
	}*/

	/**
     * adds a label to the label cache. does not affect storage
     *
     * @param label
     * @return
     */
    boolean internalAddLabel(QName label) {
        if (labelCache == null) {
            labelCache = new HashSet();
        }
        return labelCache.add(label);
    }

    /**
     * removes a label from the label cache. does not affect storage
     *
     * @param label
     * @return
     */
    boolean internalRemoveLabel(QName label) {
        if (labelCache == null) {
            return false;
        } else {
            return labelCache.remove(label);
        }
    }

    /**
     * checks, if a label is in the label cache
     *
     * @param label
     * @return
     */
    boolean internalHasLabel(QName label) {
        if (labelCache == null) {
            return false;
        } else {
            return labelCache.contains(label);
        }
    }

    /**
     * returns the array of the cached labels
     *
     * @return
     */
    QName[] internalGetLabels() {
        if (labelCache == null) {
            return new QName[0];
        } else {
            return (QName[]) labelCache.toArray(new QName[labelCache.size()]);
        }
    }

    public String getUUID() throws ValueFormatException, IllegalStateException, RepositoryException {
        return node.getUUID();
    }

	public NodeStateEx getNode() {
		return node;
	}
	
	
    /**
     * Attaches this version as successor to all predecessors. assuming that the
     * predecessors are already set.
     *
     * @throws RepositoryException
     */
    void internalAttach() throws RepositoryException {
        InternalVersion[] preds = getPredecessors();
        for (int i = 0; i < preds.length; i++) {
            ((InternalVersionImpl) preds[i]).internalAddSuccessor(this, true);
        }
    }
    
    /**
     * Adds a version to the set of successors.
     *
     * @param succ
     * @param store
     * @throws RepositoryException
     */
    private void internalAddSuccessor(InternalVersionImpl succ, boolean store)
            throws RepositoryException {
        List l = new ArrayList(Arrays.asList(getSuccessors()));
        if (!l.contains(succ)) {
            l.add(succ);
            storeXCessors(l, QName.JCR_SUCCESSORS, store);
        }
    }
    
    /**
     * stores the given successors or predecessors to the persistance node
     *
     * @throws RepositoryException
     */
    private void storeXCessors(List cessors, QName propname, boolean store)
            throws RepositoryException {
        InternalValue[] values = new InternalValue[cessors.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = InternalValue.create(
                    ((InternalVersionImpl) cessors.get(i)).getUUID());
        }
        //node.setPropertyValues(propname, PropertyType.STRING, values);
        PropertyImpl prop = node.internalSetProperty(QName.JCR_ISCHECKEDOUT, InternalValue.create(false), false, true);
        prop.getItemState().setStatusModified();

        //if (store) {
        //    node.store();
        //}
    }
    
    public _NodeState getNodeState(){
    	return node.getNodeState();
    }
}
