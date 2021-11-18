/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.version;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.VersionException;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.NodeTypeImpl;
import com.exigen.cm.impl.PropertyImpl;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.uuid.UUID;
import com.exigen.cm.jackrabbit.value.InternalValue;

/**
 * Implements a <code>InternalFrozenNode</code>
 */
public class InternalFrozenNodeImpl extends InternalFreezeImpl
        implements InternalFrozenNode {

    /**
     * checkin mode version.
     */
    private static final int MODE_VERSION = 0;

    /**
     * checkin mode copy. specifies, that the items are always copied.
     */
    private static final int MODE_COPY = 1;

    /**
     * mode flag specifies, that the mode should be recursed. otherwise i
     * will be redetermined by the opv.
     */
    private static final int MODE_COPY_RECURSIVE = 3;

    /**
     * the underlying persistance node
     */
    private NodeStateEx node;

    /**
     * the list of frozen properties
     */
    private _PropertyState[] frozenProperties;

    /**
     * the frozen uuid of the original node
     */
    private String frozenUUID = null;

    /**
     * the frozen primary type of the orginal node
     */
    private QName frozenPrimaryType = null;

    /**
     * the frozen list of mixin types of the original node
     */
    private QName[] frozenMixinTypes = null;

    /**
     * Creates a new frozen node based on the given persistance node.
     *
     * @param node
     * @throws javax.jcr.RepositoryException
     */
    public InternalFrozenNodeImpl(VersionManagerImpl vMgr, NodeStateEx node,
                                  InternalVersionItem parent)
            throws RepositoryException {
        super(vMgr, parent);
        this.node = node;

        // init the frozen properties
        List<_PropertyState> props = node.getProperties();
        
        List propList = new ArrayList();

        for (_PropertyState prop :  props) {
            if (prop.getName().equals(QName.JCR_FROZENUUID)) {
                // special property
                //frozenUUID = node.getProperty(QName.JCR_FROZENUUID).internalValue().toString();
            	frozenUUID = prop.getString();
            } else if (prop.getName().equals(QName.JCR_FROZENPRIMARYTYPE)) {
                // special property
                frozenPrimaryType = (QName) prop.getValues()[0].internalValue();
            } else if (prop.getName().equals(QName.JCR_FROZENMIXINTYPES)) {
                // special property
                InternalValue[] values = prop.getValues();
                if (values == null) {
                    frozenMixinTypes = new QName[0];
                } else {
                    frozenMixinTypes = new QName[values.length];
                    for (int j = 0; j < values.length; j++) {
                        frozenMixinTypes[j] = (QName) values[j].internalValue();
                    }
                }
            } else if (prop.getName().equals(QName.JCR_PRIMARYTYPE)) {
                // ignore
            } else if (prop.getName().equals(QName.JCR_UUID)) {
                // ignore
            } else {
                propList.add(prop);
            }
        }
        frozenProperties = (_PropertyState[]) propList.toArray(new _PropertyState[propList.size()]);

        // do some checks
        if (frozenMixinTypes == null) {
            frozenMixinTypes = new QName[0];
        }
        if (frozenPrimaryType == null) {
            throw new RepositoryException("Illegal frozen node. Must have 'frozenPrimaryType'");
        }
    }

    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return node.getQName();
    }

    /**
     * {@inheritDoc}
     */
    public Long getId() {
        //return node.getUUID();
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public InternalFreeze[] getFrozenChildNodes() throws VersionException {
        try {
            // maybe add iterator?
            List<NodeStateEx> entries = node.getNodes();
            InternalFreeze[] freezes = new InternalFreeze[entries.size()];
            //Iterator iter = entries.iterator();
            //int i = 0;
            //while (iter.hasNext()) {
            for(int i=0 ; i < entries.size() ; i++){
                //NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                freezes[i] = (InternalFreeze) getVersionManager().getItem(entries.get(i).getNodeState());
            }
            //}
            return freezes;
        } catch (RepositoryException e) {
            throw new VersionException("Unable to retrieve frozen child nodes", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean hasFrozenHistory(String uuid) {
/*        try {
            NodeState.ChildNodeEntry entry  = node.getState().getChildNodeEntry(uuid);
            if (entry != null) {
                return getVersionManager().getItem(uuid) instanceof InternalFrozenVersionHistory;
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return false;*/
        try {
        List<NodeStateEx> entries = node.getNodes();
        for(NodeStateEx n: entries){
            if (uuid.equals(n.getUUID())){
                return true;
            }
        }
        } catch (Exception exc){
            exc.printStackTrace();
        }
        return false;

    }

    /**
     * {@inheritDoc}
     */
    public _PropertyState[] getFrozenProperties() {
        return frozenProperties;
    }

    /**
     * {@inheritDoc}
     */
    public String getFrozenUUID() {
        return frozenUUID;
    }

    /**
     * {@inheritDoc}
     */
    public QName getFrozenPrimaryType() {
        return frozenPrimaryType;
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getFrozenMixinTypes() {
        return frozenMixinTypes;
    }

    /**
     * Checks-in a <code>src</code> node. It creates a new child node of
     * <code>parent</code> with the given <code>name</code> and adds the
     * source nodes properties according to their OPV value to the
     * list of frozen properties. It creates frozen child nodes for each child
     * node of <code>src</code> according to its OPV value.
     *
     * @param parent
     * @param name
     * @param src
     * @return
     * @throws RepositoryException
     */
    protected static NodeStateEx checkin(NodeStateEx parent, QName name,
                                            NodeImpl src)
            throws RepositoryException {
        return checkin(parent, name, src, MODE_VERSION);
    }

    /**
     * Checks-in a <code>src</code> node. It creates a new child node of
     * <code>parent</code> with the given <code>name</code> and adds the
     * source nodes properties according to their OPV value to the
     * list of frozen properties. It creates frozen child nodes for each child
     * node of <code>src</code> according to its OPV value.
     *
     * @param parent
     * @param name
     * @param src
     * @return
     * @throws RepositoryException
     */
    private static NodeStateEx checkin(NodeStateEx parent, QName name,
                                            NodeImpl src, int mode)
            throws RepositoryException {

        // create new node
        NodeStateEx node = (NodeStateEx) parent.addNode(name, QName.NT_FROZENNODE, null, null);

        // initialize the internal properties
        if (src._isNodeType(QName.MIX_REFERENCEABLE)){
            node.setPropertyValue(QName.JCR_FROZENUUID, InternalValue.create(src.getInternalUUID()));            
        } else {
            //generate new uuid
            UUID uuid = src._getWorkspace()._getRepository().generateUUID();
            node.setPropertyValue(QName.JCR_FROZENUUID, InternalValue.create(uuid, false));            
        }
        node.setPropertyValue(QName.JCR_FROZENPRIMARYTYPE,
                InternalValue.create(((NodeTypeImpl) src.getPrimaryNodeType()).getQName()));
        if (src.hasProperty(QName.JCR_MIXINTYPES)) {
            NodeType[] mixins = src.getMixinNodeTypes();
            InternalValue[] ivalues = new InternalValue[mixins.length];
            for (int i = 0; i < mixins.length; i++) {
                ivalues[i] = InternalValue.create(((NodeTypeImpl) mixins[i]).getQName());
            }
            node.setPropertyValues(QName.JCR_FROZENMIXINTYPES, ivalues, PropertyType.NAME);
        }

        // add the properties
        PropertyIterator piter = src.getProperties();
        while (piter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) piter.nextProperty();
            int opv;
            if ((mode & MODE_COPY) > 0) {
                opv = OnParentVersionAction.COPY;
            } else {
                opv = prop.getDefinition().getOnParentVersion();
            }
            switch (opv) {
                case OnParentVersionAction.ABORT:
                	if (!parent.getNodeState().getStatus().equals(ItemStatus.New)){
                		parent.reload();
                	}
                    throw new VersionException("Checkin aborted due to OPV in " + prop.safeGetJCRPath());
                case OnParentVersionAction.COMPUTE:
                case OnParentVersionAction.IGNORE:
                case OnParentVersionAction.INITIALIZE:
                    break;
                case OnParentVersionAction.VERSION:
                case OnParentVersionAction.COPY:
                    node.copyFrom(prop);
                    break;
            }
        }

        // add the frozen children and histories
        NodeIterator niter = src.getNodes();
        while (niter.hasNext()) {
            NodeImpl child = (NodeImpl) niter.nextNode();
            int opv;
            if ((mode & MODE_COPY_RECURSIVE) > 0) {
                opv = OnParentVersionAction.COPY;
            } else {
                opv = child.getDefinition().getOnParentVersion();
            }
            switch (opv) {
                case OnParentVersionAction.ABORT:
                    throw new VersionException("Checkin aborted due to OPV in " + child.safeGetJCRPath());
                case OnParentVersionAction.COMPUTE:
                case OnParentVersionAction.IGNORE:
                case OnParentVersionAction.INITIALIZE:
                    break;
                case OnParentVersionAction.VERSION:
                    if (child.isNodeType(QName.MIX_VERSIONABLE)) {
                        // create frozen versionable child
                        NodeStateEx newChild = (NodeStateEx) node.addNode(child.getQName(), QName.NT_VERSIONEDCHILD, null);
                        newChild.setPropertyValue(QName.JCR_CHILDVERSIONHISTORY,
                                InternalValue.create(new UUID(child.getVersionHistory().getUUID()), false));
                        /*
                        newChild.setPropertyValue(JCR_BASEVERSION,
                                InternalValue.create(child.getBaseVersion().getUUID()));
                        */ 
                        break;
                    }
                    // else copy but do not recurse
                    checkin(node, child.getQName(), child, MODE_COPY);
                    break;
                case OnParentVersionAction.COPY:
                    checkin(node, child.getQName(), child, MODE_COPY_RECURSIVE);
                    break;
            }
        }
        return node;

    }


}
