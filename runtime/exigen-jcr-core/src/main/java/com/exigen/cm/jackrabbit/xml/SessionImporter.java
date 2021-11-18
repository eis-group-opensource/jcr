/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.xml;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipFile;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.impl.NodeImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.jackrabbit.core.util.ReferenceChangeTracker;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.EffectiveNodeType;
import com.exigen.cm.jackrabbit.nodetype.PropDef;
import com.exigen.cm.jackrabbit.value.InternalValue;
import com.exigen.cm.jackrabbit.value.ValueHelper;
/**
 * <code>SessionImporter</code> ...
 */
public class SessionImporter implements Importer {

    private static Log log = LogFactory.getLog(SessionImporter.class);

    private final SessionImpl session;
    private final NodeImpl importTargetNode;
    private final int uuidBehavior;
    private final ZipFile zipFile;

    private Stack parents;

    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

    public SessionImporter(NodeImpl importTargetNode,
                    SessionImpl session,
                    int uuidBehavior) {
        this(importTargetNode, session, uuidBehavior, null);
    }    
    
    
    /**
     * Creates a new <code>SessionImporter</code> instance.
     *
     * @param importTargetNode
     * @param session
     * @param uuidBehavior     any of the constants declared by
     *                         {@link ImportUUIDBehavior}
     * @param zin ZipInputStream with binary properties (if binary property exported into zip), null otherwise
     */
    public SessionImporter(NodeImpl importTargetNode,
                           SessionImpl session,
                           int uuidBehavior,
                           ZipFile zipFile) {
        this.importTargetNode = importTargetNode;
        this.session = session;
        this.uuidBehavior = uuidBehavior;
        this.zipFile = zipFile;
        
        refTracker = new ReferenceChangeTracker();

        parents = new Stack();
        parents.push(importTargetNode);
    }

    protected NodeImpl createNode(NodeImpl parent,
                                  QName nodeName,
                                  QName nodeTypeName,
                                  QName[] mixinNames,
                                  String uuid)
            throws RepositoryException {
        NodeImpl node;

        if (parent.hasProperty(nodeName)) {
            /**
             * a property with the same name already exists; if this property
             * has been imported as well (e.g. through document view import
             * where an element can have the same name as one of the attributes
             * of its parent element) we have to rename the onflicting property;
             *
             * see http://issues.apache.org/jira/browse/JCR-61
             */
            Property conflicting = parent.getProperty(nodeName);
            if (conflicting.isNew()) {
                // assume this property has been imported as well;
                // rename conflicting property
                // @todo use better reversible escaping scheme to create unique name
                QName newName = new QName(nodeName.getNamespaceURI(), nodeName.getLocalName() + "_");
                if (parent.hasProperty(newName)) {
                    newName = new QName(newName.getNamespaceURI(), newName.getLocalName() + "_");
                }

                if (conflicting.getDefinition().isMultiple()) {
                    parent.setProperty(newName, conflicting.getValues());
                } else {
                    parent.setProperty(newName, conflicting.getValue());
                }
                conflicting.remove();
            }
        }

        // add node
        node = (NodeImpl)parent.addNode(nodeName, nodeTypeName, uuid);
        // add mixins
        if (mixinNames != null) {
            for (int i = 0; i < mixinNames.length; i++) {
                node.addMixin(mixinNames[i]);
                if (QName.MIX_REFERENCEABLE.equals(mixinNames[i]) && uuid != null){
                	//set proper uuid
                	_PropertyState prop = node.getNodeState().getProperty(QName.JCR_UUID, true);
                	node.getNodeState().setInternalUUID(uuid);
                	prop.setValues(new InternalValue[]{InternalValue.create(uuid)});
                }
            }
        }
        return node;
    }

    protected NodeImpl resolveUUIDConflict(NodeImpl parent,
                                           NodeImpl conflicting,
                                           NodeInfo nodeInfo)
            throws RepositoryException {
        NodeImpl node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            if (node.isNodeType(QName.MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getUUID(), node.getUUID());
            }
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
            String msg = "a node with uuid " + nodeInfo.getUUID() + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTargetNode or an ancestor thereof
            if (importTargetNode.getPath().startsWith(conflicting.getPath())) {
                String msg = "cannot remove ancestor node";
                log.debug(msg);
                throw new ConstraintViolationException (msg);
            }
            // remove conflicting
            conflicting.remove();
            // create new with given uuid
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getDepth() == 0) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            parent = (NodeImpl) conflicting.getParent();
            // remove conflicting
            conflicting.remove();
            // create new with given uuid at same location as conflicting
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return node;
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List propInfos,
                          NamespaceResolver nsContext)
            throws RepositoryException {
        NodeImpl parent = (NodeImpl) parents.peek();

        // process node

        NodeImpl node = null;
        String uuid = nodeInfo.getUUID();
        QName nodeName = nodeInfo.getName();
        QName ntName = nodeInfo.getNodeTypeName();
        QName[] mixins = nodeInfo.getMixinNames();

        if (parent == null) {
            // parent node was skipped, skip this child node also
            parents.push(null); // push null onto stack for skipped node
            log.debug("skipping node " + nodeName);
            return;
        }
        if (parent.hasNode(nodeName)) {
        	
            // a node with that name already exists...
            NodeImpl existing =(NodeImpl) parent.getNode(nodeName);
            NodeDefinition def = existing.getDefinition();
            if (!def.allowsSameNameSiblings()) {
                // existing doesn't allow same-name siblings,
                // check for potential conflicts
                if (def.isProtected() && existing.isNodeType(ntName)) {
                    // skip protected node
                    parents.push(null); // push null onto stack for skipped node
                    log.debug("skipping protected node " + existing.safeGetJCRPath());
                    return;
                }
                if (def.isAutoCreated() && existing.isNodeType(ntName)) {
                    // this node has already been auto-created, no need to create it
                    node = existing;
                } else {
                    throw new ItemExistsException(existing.safeGetJCRPath());
                }
            }
        }

        if (parent.getDepth() == 0 && nodeName.equals(QName.JCR_ROOT)){
        	return;
        }
        if (node == null) {
            // create node
            if (uuid == null) {
                // no potential uuid conflict, always add new node
                node = createNode(parent, nodeName, ntName, mixins, null);
            } else {
                // potential uuid conflict
                NodeImpl conflicting;
                try {
                    conflicting = (NodeImpl) session.getNodeByUUID(uuid);
                } catch (ItemNotFoundException infe) {
                    conflicting = null;
                }
                if (conflicting != null) {
                    // resolve uuid conflict
                    node = resolveUUIDConflict(parent, conflicting, nodeInfo);
                } else {
                    // create new with given uuid
                    node = createNode(parent, nodeName, ntName, mixins, uuid);
                }
            }
        }

        // process properties

        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            QName propName = pi.getName();
            TextValue[] tva = pi.getValues();
            int type = pi.getType();

            // find applicable definition
            EffectiveNodeType ent = node.getNodeState().getEffectiveNodeType();
            PropDef def;
            // multi- or single-valued property?
            if (tva.length == 1) {
                // could be single- or multi-valued (n == 1)
                def = ent.getApplicablePropertyDef(propName, type);
            } else {
                // can only be multi-valued (n == 0 || n > 1)
            	try {
            		def = ent.getApplicablePropertyDef(propName, type, true);
            	} catch (ConstraintViolationException cExc){
            		if (tva.length == 0){
            			def = ent.getApplicablePropertyDef(propName, type);
            			tva = pi._getValues();
            		} else {
            			throw cExc;
            		}
            	}
            }

            if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property " + propName);
                continue;
            }

            // convert serialized values to Value objects
            Value[] va = new Value[tva.length];
            int targetType = def.getRequiredType();
            if (targetType == PropertyType.UNDEFINED) {
                if (type == PropertyType.UNDEFINED) {
                    targetType = PropertyType.STRING;
                } else {
                    targetType = type;
                }
            }
            for (int i = 0; i < tva.length; i++) {
                TextValue tv = tva[i];

                if (targetType == PropertyType.NAME
                        || targetType == PropertyType.PATH) {
                    // NAME and PATH require special treatment because
                    // they depend on the current namespace context
                    // of the xml document

                    // retrieve serialized value
                    String serValue;
                    try {
                        serValue = tv.retrieve();
                    } catch (IOException ioe) {
                        String msg = "failed to retrieve serialized value";
                        log.debug(msg, ioe);
                        throw new RepositoryException(msg, ioe);
                    }

                    // convert serialized value to InternalValue using
                    // current namespace context of xml document
                    InternalValue ival =
                            InternalValue.create(serValue, targetType, nsContext, session.getStoreContainer());
                    // convert InternalValue to Value using this
                    // session's namespace mappings
                    va[i] = ival.toJCRValue(session.getNamespaceResolver());
                } else if (targetType == PropertyType.BINARY) {
                    try {
                        if (tv.length() < 0x10000 && zipFile == null) {
                            // < 65kb: 
                                //deserialize BINARY type using String
                                va[i] = ValueHelper.deserialize(tv.retrieve(), targetType, false, null);
                        } else {
                            // >= 65kb or zip: deserialize BINARY type using Reader
                            Reader reader = tv.reader();
                            try {
                                va[i] = ValueHelper.deserialize(reader, targetType, false, zipFile);
                            } finally {
                                reader.close();
                            }
                        }
                        
                    } catch (IOException ioe) {
                        String msg = "failed to deserialize binary value";
                        log.error(msg, ioe);
                        throw new RepositoryException(msg, ioe);
                    }
                } else {
                    // all other types

                    // retrieve serialized value
                    String serValue;
                    try {
                        serValue = tv.retrieve();
                    } catch (IOException ioe) {
                        String msg = "failed to retrieve serialized value";
                        log.debug(msg, ioe);
                        throw new RepositoryException(msg, ioe);
                    }

                    va[i] = ValueHelper.deserialize(serValue, targetType, true, null);
                }
            }

            // multi- or single-valued property?
            if (targetType != PropertyType.REFERENCE && targetType != PropertyType283.WEAKREFERENCE) {
	            if (va.length == 1) {
	                // could be single- or multi-valued (n == 1)
	                try {
	                    // try setting single-value
	                    node.setProperty(propName, va[0]);
	                } catch (ValueFormatException vfe) {
	                    // try setting value array
	                    node.setProperty(propName, va, type);
	                } catch (ConstraintViolationException cve) {
	                    // try setting value array
	                    node.setProperty(propName, va, type);
	                }
	            } else {
	                // can only be multi-valued (n == 0 || n > 1)
	                node.setProperty(propName, va, type);
	            }
            }
            if (targetType == PropertyType.REFERENCE || targetType == PropertyType283.WEAKREFERENCE) {
                // store reference for later resolution
                //refTracker.processedReference(node.getProperty(propName));
                refTracker.addReference(node, propName, va, type);
            }
        }

        parents.push(node);
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        parents.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly generated uuid's on import
         */
    	
        refTracker.processRefs();

        /*Iterator iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            Property prop = (Property) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE) {
                continue;
            }
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                Value[] newVals = new Value[values.length];
                for (int i = 0; i < values.length; i++) {
                    Value val = values[i];
                    String original = val.getString();
                    String adjusted = refTracker.getMappedUUID(original);
                    if (adjusted != null) {
                        newVals[i] = new ReferenceValue(session.getNodeByUUID(adjusted));
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = val;
                    }
                }
                prop.setValue(newVals);
            } else {
                Value val = prop.getValue();
                String original = val.getString();
                String adjusted = refTracker.getMappedUUID(original);
                if (adjusted != null) {
                    prop.setValue(session.getNodeByUUID(adjusted));
                }
            }
        }*/
        
        refTracker.clear();
    }
}
