/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.jackrabbit.nodetype;



import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.PropertyType283;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

import com.exigen.cm.impl.nodetype.DBNodeTypeReader;
import com.exigen.cm.impl.tmp.NodeTypeConflictException;
import com.exigen.cm.jackrabbit.cluster.NodeTypeEventChannel;
import com.exigen.cm.jackrabbit.cluster.NodeTypeEventListener;
import com.exigen.cm.jackrabbit.core.util.Dumpable;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.value.InternalValue;

/**
 * A <code>NodeTypeRegistry</code> ...
 */
public class NodeTypeRegistry implements Dumpable, NodeTypeEventListener {

	private static Log log = LogFactory.getLog(NodeTypeRegistry.class);

    private static final String BUILTIN_NODETYPES_RESOURCE_PATH =
            "org/apache/jackrabbit/core/nodetype/builtin_nodetypes.xml";
    //private static final String CUSTOM_NODETYPES_RESOURCE_NAME =
     //       "custom_nodetypes.xml";

    // file system where node type registrations are persisted
    //private final FileSystem ntStore;
    /**
     * resource holding custom node type definitions which are represented as
     * nodes in the repository; it is needed in order to make the registrations
     * persistent.
     */
    //private final FileSystemResource customNodeTypesResource;

    // persistent node type definitions of built-in & custom node types
    private final NodeTypeDefStore builtInNTDefs;
    //private final NodeTypeDefStore customNTDefs;

    // cache of pre-built aggregations of node types
    private final EffectiveNodeTypeCache entCache;

    // map of node type names and node type definitions
    private final ConcurrentReaderHashMap registeredNTDefs;

    // definition of the root node
    //private final NodeDef rootNodeDef;

    // map of id's and property definitions
    private final ConcurrentReaderHashMap _propDefs;
    // map of id's and node definitions
    private final ConcurrentReaderHashMap _nodeDefs;

    /**
     * namespace registry for resolving prefixes and namespace URI's;
     * used for (de)serializing node type definitions
     */
    private final NamespaceRegistry nsReg;

    /**
     * Listeners (soft references)
     */
    private final Map listeners =
            Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    /**
     * Node type event channel.
     */
    //private NodeTypeEventChannel eventChannel;

	private NodeTypeDefStore ntStore;

    /**
     * Create a new <code>NodeTypeRegistry</codes>
     *
     * @param nsReg
     * @param ntStore
     * @return <code>NodeTypeRegistry</codes> object
     * @throws RepositoryException
     */
    public static NodeTypeRegistry create(NamespaceRegistry nsReg,
			NodeTypeDefStore ntStore) throws RepositoryException {
		NodeTypeRegistry ntMgr = new NodeTypeRegistry(nsReg, ntStore);
		return ntMgr;
	}

	public static NodeTypeRegistry create(NamespaceRegistry nsReg,
			DBNodeTypeReader reader) throws RepositoryException {
		NodeTypeDefStore ntStore = new NodeTypeDefStore();
		ntStore.load(reader.all());
		NodeTypeRegistry ntMgr = new NodeTypeRegistry(nsReg, ntStore);
		return ntMgr;
	}

    //----------------------------------------< public NodeTypeRegistry 'api' >
    /**
     * Returns the names of all registered node types. That includes primary
     * and mixin node types.
     *
     * @return the names of all registered node types.
     */
    public QName[] getRegisteredNodeTypes() {
        return (QName[]) registeredNTDefs.keySet().toArray(new QName[registeredNTDefs.size()]);
    }

    /**
     * Validates the <code>NodeTypeDef</code> and returns
     * an  <code>EffectiveNodeType</code> object representing the newly
     * registered node type.
     * <p/>
     * The validation includes the following checks:
     * <ul>
     * <li>Supertypes must exist and be registered</li>
     * <li>Inheritance graph must not be circular</li>
     * <li>Aggregation of supertypes must not result in name conflicts,
     * ambiguities, etc.</li>
     * <li>Definitions of auto-created properties must specify a name</li>
     * <li>Default values in property definitions must satisfy value constraints
     * specified in the same property definition</li>
     * <li>Definitions of auto-created child-nodes must specify a name</li>
     * <li>Default node type in child-node definitions must exist and be
     * registered</li>
     * <li>The aggregation of the default node types in child-node definitions
     * must not result in name conflicts, ambiguities, etc.</li>
     * <li>Definitions of auto-created child-nodes must not specify default
     * node types which would lead to infinite child node creation
     * (e.g. node type 'A' defines auto-created child node with default
     * node type 'A' ...)</li>
     * <li>Node types specified as constraints in child-node definitions
     * must exist and be registered</li>
     * <li>The aggregation of the node types specified as constraints in
     * child-node definitions must not result in name conflicts, ambiguities,
     * etc.</li>
     * <li>Default node types in child-node definitions must satisfy
     * node type constraints specified in the same child-node definition</li>
     * </ul>
     *
     * @param ntd the definition of the new node type
     * @return an <code>EffectiveNodeType</code> instance
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public synchronized EffectiveNodeType registerNodeType(NodeTypeDef ntd)
            throws InvalidNodeTypeDefException, RepositoryException {
        // validate and register new node type definition
        //EffectiveNodeType ent = internalRegister(ntd);

        // persist new node type definition
        /*customNTDefs.add(ntd);
        persistCustomNodeTypeDefs(customNTDefs);

        if (eventChannel != null) {
            HashSet ntDefs = new HashSet();
            ntDefs.add(ntd);
            eventChannel.registered(ntDefs);
        }

        // notify listeners
        notifyRegistered(ntd.getName());

        return ent;*/
        throw new UnsupportedOperationException();
    }

    /**
     * Same as <code>{@link #registerNodeType(NodeTypeDef)}</code> except
     * that a collection of <code>NodeTypeDef</code>s is registered instead of
     * just one.
     * <p/>
     * This method can be used to register a set of node types that have
     * dependencies on each other.
     *
     * @param ntDefs a collection of <code>NodeTypeDef<code> objects
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public synchronized void registerNodeTypes(Collection ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException {
        // validate and register new node type definitions
        internalRegister(ntDefs);
        // persist new node type definitions
        /*for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntDef = (NodeTypeDef) iter.next();
            customNTDefs.add(ntDef);
        }
        persistCustomNodeTypeDefs(customNTDefs);

        // inform cluster
        if (eventChannel != null) {
            eventChannel.registered(ntDefs);
        }

        // notify listeners
        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntDef = (NodeTypeDef) iter.next();
            notifyRegistered(ntDef.getName());
        }*/
        throw new UnsupportedOperationException();
    }

    /**
     * Same as <code>{@link #unregisterNodeType(QName)}</code> except
     * that a set of node types is unregistered instead of just one.
     * <p/>
     * This method can be used to unregister a set of node types that depend on
     * each other.
     *
     * @param ntNames a collection of <code>QName</code> objects denoting the
     *                node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the specified names does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs
     * @see #unregisterNodeType(QName)
     */
    /*public synchronized void unregisterNodeTypes(Collection ntNames)
            throws NoSuchNodeTypeException, RepositoryException {
        // do some preliminary checks
        for (Iterator iter = ntNames.iterator(); iter.hasNext();) {
            QName ntName = (QName) iter.next();
            if (!registeredNTDefs.containsKey(ntName)) {
                throw new NoSuchNodeTypeException(ntName.toString());
            }
            if (builtInNTDefs.contains(ntName)) {
                throw new RepositoryException(ntName.toString()
                        + ": can't unregister built-in node type.");
            }
            // check for node types other than those to be unregistered
            // that depend on the given node types
            Set dependents = getDependentNodeTypes(ntName);
            dependents.removeAll(ntNames);
            if (dependents.size() > 0) {
                StringBuffer msg = new StringBuffer();
                msg.append(ntName
                        + " can not be removed because the following node types depend on it: ");
                for (Iterator depIter = dependents.iterator(); depIter.hasNext();) {
                    msg.append(depIter.next());
                    msg.append(" ");
                }
                throw new RepositoryException(msg.toString());
            }
        }

        // make sure node types are not currently in use
        for (Iterator iter = ntNames.iterator(); iter.hasNext();) {
            QName ntName = (QName) iter.next();
            checkForReferencesInContent(ntName);
        }

        // all preconditions are met, node types can now safely be unregistered
        internalUnregister(ntNames);

        // persist removal of node type definitions & notify listeners
        /*for (Iterator iter = ntNames.iterator(); iter.hasNext();) {
            QName ntName = (QName) iter.next();
            customNTDefs.remove(ntName);
            notifyUnregistered(ntName);
        }
        persistCustomNodeTypeDefs(customNTDefs);
        throw new UnsupportedOperationException();
    }*/

    /**
     * Unregisters the specified node type. In order for a node type to be
     * successfully unregistered it must meet the following conditions:
     * <ol>
     * <li>the node type must obviously be registered.</li>
     * <li>a built-in node type can not be unregistered.</li>
     * <li>the node type must not have dependents, i.e. other node types that
     * are referencing it.</li>
     * <li>the node type must not be currently used by any workspace.</li>
     * </ol>
     *
     * @param ntName name of the node type to be unregistered
     * @throws NoSuchNodeTypeException if <code>ntName</code> does not
     *                                 denote a registered node type.
     * @throws RepositoryException if another error occurs.
     * @see #unregisterNodeTypes(Collection)
     */
    /*public void unregisterNodeType(QName ntName)
            throws NoSuchNodeTypeException, RepositoryException {
        HashSet ntNames = new HashSet();
        ntNames.add(ntName);
        unregisterNodeTypes(ntNames);
    }*/

    /**
     * @param ntd
     * @return
     * @throws NoSuchNodeTypeException
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public synchronized EffectiveNodeType reregisterNodeType(NodeTypeDef ntd)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {
        QName name = ntd.getName();
        if (!registeredNTDefs.containsKey(name)) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        if (builtInNTDefs.contains(name)) {
            throw new RepositoryException(name.toString()
                    + ": can't reregister built-in node type.");
        }

        /**
         * validate new node type definition
         */
        validateNodeTypeDef(ntd, entCache, registeredNTDefs, nsReg, false);

        /**
         * build diff of current and new definition and determine type of change
         */
        NodeTypeDef ntdOld = (NodeTypeDef) registeredNTDefs.get(name);
        NodeTypeDefDiff diff = NodeTypeDefDiff.create(ntdOld, ntd);
        if (!diff.isModified()) {
            // the definition has not been modified, there's nothing to do here...
            return getEffectiveNodeType(name);
        }
        /*if (diff.isTrivial()) {
            //
             // the change is trivial and has no effect on current content
             // (e.g. that would be the case when non-mandatory properties had
             // been added);
             // re-register node type definition and update caches &
             // notify listeners on re-registration
             //
            internalUnregister(name);
            // remove old node type definition from store
            customNTDefs.remove(name);

            EffectiveNodeType entNew = internalRegister(ntd);

            // add new node type definition to store
            customNTDefs.add(ntd);
            // persist node type definitions
            persistCustomNodeTypeDefs(customNTDefs);

            // notify listeners
            notifyReRegistered(name);
            return entNew;
        }

        // make sure existing content would not conflict
        // with new node type definition
        checkForConflictingContent(ntd);

        // unregister old node type definition
        internalUnregister(name);
        // register new definition
        EffectiveNodeType entNew = internalRegister(ntd);

        // persist modified node type definitions
        customNTDefs.remove(name);
        customNTDefs.add(ntd);
        persistCustomNodeTypeDefs(customNTDefs);

        // notify listeners
        notifyReRegistered(name);
        return entNew;*/
        throw new UnsupportedOperationException();
    }

    /**
     * @param ntName
     * @return
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(QName ntName)
            throws NoSuchNodeTypeException {
        return getEffectiveNodeType(ntName, entCache, registeredNTDefs);
    }

    /**
     * @param ntNames
     * @return
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(QName[] ntNames)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        return getEffectiveNodeType(ntNames, entCache, registeredNTDefs);
    }

    /**
     * Returns the names of those registered node types that have
     * dependencies on the given node type.
     *
     * @param nodeTypeName node type name
     * @return a set of node type <code>QName</code>s
     * @throws NoSuchNodeTypeException
     */
    /*public Set getDependentNodeTypes(QName nodeTypeName)
            throws NoSuchNodeTypeException {
        if (!registeredNTDefs.containsKey(nodeTypeName)) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }

        
         //collect names of those node types that have dependencies on the given
         //node type
         
        HashSet names = new HashSet();
        Iterator iter = registeredNTDefs.values().iterator();
        while (iter.hasNext()) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            if (ntd.getDependencies().contains(nodeTypeName)) {
                names.add(ntd.getName());
            }
        }
        return names;
    }*/

    /**
     * Returns the node type definition of the node type with the given name.
     *
     * @param nodeTypeName name of node type whose definition should be returned.
     * @return the node type definition of the node type with the given name.
     * @throws NoSuchNodeTypeException if a node type with the given name
     *                                 does not exist
     */
    public NodeTypeDef getNodeTypeDef(QName nodeTypeName)
            throws NoSuchNodeTypeException, RepositoryException {
        NodeTypeDef def = (NodeTypeDef) registeredNTDefs.get(nodeTypeName);
        if (def == null) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }
        // return clone to make sure nobody messes around with the 'live' definition
        return (NodeTypeDef) def.clone();
    }

    /**
     * @param nodeTypeName
     * @return <code>true</code> if the specified node type is registered;
     *         <code>false</code> otherwise.
     */
    public boolean isRegistered(QName nodeTypeName) {
        return registeredNTDefs.containsKey(nodeTypeName);
    }

    /**
     * @param nodeTypeName
     * @return <code>true</code> if the specified node type is built-in;
     *         <code>false</code> otherwise.
     */
    public boolean isBuiltIn(QName nodeTypeName) {
        return builtInNTDefs.contains(nodeTypeName);
    }

    /**
     * @param id
     * @return the node definition for the given id.
     */
    public NodeDef getNodeDef(NodeDefId id) {
        return (NodeDef) _nodeDefs.get(id);
    }

    /**
     * @param id
     * @return the property definition for the given id.
     */
    public PropDef getPropDef(PropDefId id) {
        return (PropDef) _propDefs.get(id);
    }

    /**
     * Add a <code>NodeTypeRegistryListener</code>
     *
     * @param listener the new listener to be informed on (un)registration
     *                 of node types
     */
    public void addListener(NodeTypeRegistryListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Remove a <code>NodeTypeRegistryListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(NodeTypeRegistryListener listener) {
        listeners.remove(listener);
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.println("NodeTypeRegistry (" + this + ")");
        ps.println();
        ps.println("Registered NodeTypes:");
        ps.println();
        Iterator iter = registeredNTDefs.values().iterator();
        while (iter.hasNext()) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            ps.println(ntd.getName());
            QName[] supertypes = ntd.getSupertypes();
            ps.println("\tSupertypes");
            for (int i = 0; i < supertypes.length; i++) {
                ps.println("\t\t" + supertypes[i]);
            }
            ps.println("\tMixin\t" + ntd.isMixin());
            ps.println("\tOrderableChildNodes\t" + ntd.hasOrderableChildNodes());
            ps.println("\tPrimaryItemName\t" + (ntd.getPrimaryItemName() == null ? "<null>" : ntd.getPrimaryItemName().toString()));
            PropDef[] pd = ntd.getPropertyDefs();
            for (int i = 0; i < pd.length; i++) {
                ps.print("\tPropertyDefinition");
                ps.println(" (declared in " + pd[i].getDeclaringNodeType() + ") id=" + pd[i].getSQLId());
                ps.println("\t\tName\t\t" + (pd[i].definesResidual() ? "*" : pd[i].getName().toString()));
                String type = pd[i].getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(pd[i].getRequiredType());
                ps.println("\t\tRequiredType\t" + type);
                ValueConstraint[] vca = pd[i].getValueConstraints();
                StringBuffer constraints = new StringBuffer();
                if (vca == null) {
                    constraints.append("<null>");
                } else {
                    for (int n = 0; n < vca.length; n++) {
                        if (constraints.length() > 0) {
                            constraints.append(", ");
                        }
                        constraints.append(vca[n].getDefinition());
                    }
                }
                ps.println("\t\tValueConstraints\t" + constraints.toString());
                InternalValue[] defVals = pd[i].getDefaultValues();
                StringBuffer defaultValues = new StringBuffer();
                if (defVals == null) {
                    defaultValues.append("<null>");
                } else {
                    for (int n = 0; n < defVals.length; n++) {
                        if (defaultValues.length() > 0) {
                            defaultValues.append(", ");
                        }
                        defaultValues.append(defVals[n].toString());
                    }
                }
                ps.println("\t\tDefaultValue\t" + defaultValues.toString());
                ps.println("\t\tAutoCreated\t" + pd[i].isAutoCreated());
                ps.println("\t\tMandatory\t" + pd[i].isMandatory());
                ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(pd[i].getOnParentVersion()));
                ps.println("\t\tProtected\t" + pd[i].isProtected());
                ps.println("\t\tMultiple\t" + pd[i].isMultiple());
            }
            NodeDef[] nd = ntd.getChildNodeDefs();
            for (int i = 0; i < nd.length; i++) {
                ps.print("\tNodeDefinition");
                ps.println(" (declared in " + nd[i].getDeclaringNodeType() + ") id=" + nd[i].getSQLId());
                ps.println("\t\tName\t\t" + (nd[i].definesResidual() ? "*" : nd[i].getName().toString()));
                QName[] reqPrimaryTypes = nd[i].getRequiredPrimaryTypes();
                if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
                    for (int n = 0; n < reqPrimaryTypes.length; n++) {
                        ps.print("\t\tRequiredPrimaryType\t" + reqPrimaryTypes[n]);
                    }
                }
                QName defPrimaryType = nd[i].getDefaultPrimaryType();
                if (defPrimaryType != null) {
                    ps.print("\n\t\tDefaultPrimaryType\t" + defPrimaryType);
                }
                ps.println("\n\t\tAutoCreated\t" + nd[i].isAutoCreated());
                ps.println("\t\tMandatory\t" + nd[i].isMandatory());
                ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(nd[i].getOnParentVersion()));
                ps.println("\t\tProtected\t" + nd[i].isProtected());
                ps.println("\t\tAllowsSameNameSiblings\t" + nd[i].allowsSameNameSiblings());
            }
        }
        ps.println();

        entCache.dump(ps);
    }

    //------------------------------------------------< NodeTypeEventListener >

    /**
     * {@inheritDoc}
     */
    public void externalRegistered(Collection ntDefs)
            throws RepositoryException, InvalidNodeTypeDefException {

        // validate and register new node type definitions
        /*internalRegister(ntDefs);
        // persist new node type definitions
        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntDef = (NodeTypeDef) iter.next();
            customNTDefs.add(ntDef);
        }
        persistCustomNodeTypeDefs(customNTDefs);
        // notify listeners
        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntDef = (NodeTypeDef) iter.next();
            notifyRegistered(ntDef.getName());
        }*/
    	throw new UnsupportedOperationException();
    }

    //---------------------------------------------------------< overridables >
    /**
     * Protected constructor
     *
     * @param nsReg
     * @param ntStore
     * @throws RepositoryException
     */
    protected NodeTypeRegistry(NamespaceRegistry nsReg, NodeTypeDefStore ntStore)
            throws RepositoryException {
        this.nsReg = nsReg;
        this.ntStore = ntStore;
        //customNodeTypesResource =
        //        new FileSystemResource(this.ntStore, CUSTOM_NODETYPES_RESOURCE_NAME);
        /*try {
            // make sure path to resource exists
            if (!customNodeTypesResource.exists()) {
                customNodeTypesResource.makeParentDirs();
            }
        } catch (FileSystemException fse) {
            String error = "internal error: invalid resource: "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, fse);
        }*/

        entCache = new EffectiveNodeTypeCache();
        registeredNTDefs = new ConcurrentReaderHashMap();
        _propDefs = new ConcurrentReaderHashMap();
        _nodeDefs = new ConcurrentReaderHashMap();

        // setup definition of root node
        //rootNodeDef = createRootNodeDef();
        //nodeDefs.put(rootNodeDef.getId(), rootNodeDef);

        // load and register pre-defined (i.e. built-in) node types
        builtInNTDefs = new NodeTypeDefStore();
        
        try {
            // load built-in node type definitions
            loadBuiltInNodeTypeDefs(builtInNTDefs);

            // register built-in node types
            internalRegister(builtInNTDefs.all(), true);
        } catch (InvalidNodeTypeDefException intde) {
            String error =
                    "internal error: invalid built-in node type definition stored in "
                    + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.debug(error);
            throw new RepositoryException(error, intde);
        }

        // load and register custom node types
        /*customNTDefs = new NodeTypeDefStore();

        // load custom node type definitions
        loadCustomNodeTypeDefs(customNTDefs);

        // validate & register custom node types
        try {
            internalRegister(customNTDefs.all());
        } catch (InvalidNodeTypeDefException intde) {
            String error =
                    "internal error: invalid custom node type definition stored in "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, intde);
        }*/
    }

    /**
     * Loads the built-in node type definitions into the given <code>store</code>.
     * <p/>
     * This method may be overridden by extensions of this class; It must
     * only be called once and only from within the constructor though.
     *
     * @param store The {@link NodeTypeDefStore} into which the node type
     *              definitions are loaded.
     * @throws RepositoryException If an error occurrs while loading the
     *                             built-in node type definitions.
     */
    protected void loadBuiltInNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {
    	store.load(this.ntStore);
        /*InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(BUILTIN_NODETYPES_RESOURCE_PATH);
            store.load(in);
        } catch (IOException ioe) {
            String error =
                    "internal error: failed to read built-in node type definitions stored in "
                    + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.debug(error);
            throw new RepositoryException(error, ioe);
        } catch (InvalidNodeTypeDefException intde) {
            String error =
                    "internal error: invalid built-in node type definition stored in "
                    + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.debug(error);
            throw new RepositoryException(error, intde);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }*/
    }

    /**
     * Loads the custom node type definitions into the given <code>store</code>.
     * <p/>
     * This method may be overridden by extensions of this class; It must
     * only be called once and only from within the constructor though.
     *
     * @param store The {@link NodeTypeDefStore} into which the node type
     *              definitions are loaded.
     * @throws RepositoryException If an error occurrs while loading the
     *                             custom node type definitions.
     */
    /*protected void loadCustomNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {

        InputStream in = null;
        try {
            if (customNodeTypesResource.exists()) {
                in = customNodeTypesResource.getInputStream();
            }
        } catch (FileSystemException fse) {
            String error =
                    "internal error: failed to access custom node type definitions stored in "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, fse);
        }

        if (in == null) {
            log.info("no custom node type definitions found");
        } else {
            try {
                store.load(in);
            } catch (IOException ioe) {
                String error =
                        "internal error: failed to read custom node type definitions stored in "
                        + customNodeTypesResource.getPath();
                log.debug(error);
                throw new RepositoryException(error, ioe);
            } catch (InvalidNodeTypeDefException intde) {
                String error =
                        "internal error: invalid custom node type definition stored in "
                        + customNodeTypesResource.getPath();
                log.debug(error);
                throw new RepositoryException(error, intde);
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }*/

    /**
     * Persists the custom node type definitions contained in the given
     * <code>store</code>.
     *
     * @param store The {@link NodeTypeDefStore} containing the definitons to
     *              be persisted.
     * @throws RepositoryException If an error occurrs while persisting the
     *                             custom node type definitions.
     */
    protected void persistCustomNodeTypeDefs(NodeTypeDefStore store)
            throws RepositoryException {
        /*OutputStream out = null;
        try {
            out = customNodeTypesResource.getOutputStream();
            store.store(out, nsReg);
        } catch (IOException ioe) {
            String error =
                    "internal error: failed to persist custom node type definitions to "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, ioe);
        } catch (FileSystemException fse) {
            String error =
                    "internal error: failed to persist custom node type definitions to "
                    + customNodeTypesResource.getPath();
            log.debug(error);
            throw new RepositoryException(error, fse);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }*/
    	throw new UnsupportedOperationException();
    }

    /**
     * Checks whether there is existing content that would conflict with the
     * given node type definition.
     * <p/>
     * This method is not implemented yet and always throws a
     * <code>RepositoryException</code>.
     * <p/>
     * TODO
     * <ol>
     * <li>apply deep locks on root nodes in every workspace or alternatively
     * put repository in 'exclusive' or 'single-user' mode
     * <li>check if the given node type (or any node type that has
     * dependencies on this node type) is currently referenced by nodes
     * in the repository.
     * <li>check if applying the changed definitions to the affected items would
     * violate existing node type constraints
     * <li>apply and persist changes to affected nodes (e.g. update
     * definition id's, etc.)
     * </ul>
     * <p/>
     * the above checks/actions are absolutely necessary in order to
     * guarantee integrity of repository content.
     *
     * @param ntd The node type definition replacing the former node type
     *            definition of the same name.
     * @throws RepositoryException If there is conflicting content or if the
     *                             check failed for some other reason.
     */
    protected void checkForConflictingContent(NodeTypeDef ntd)
            throws RepositoryException {
        /**
         * collect names of node types that have dependencies on the given
         * node type
         */
        //Set dependentNTs = getDependentNodeTypes(ntd.getName());

        throw new RepositoryException("not yet implemented");
    }

    /**
     * Checks whether there is existing content that directly or indirectly
     * refers to the specified node type.
     * <p/>
     * This method is not implemented yet and always throws a
     * <code>RepositoryException</code>.
     * <p/>
     * TODO:
     * <ol>
     * <li>apply deep locks on root nodes in every workspace or alternatively
     * put repository in 'single-user' mode
     * <li>check if the given node type is currently referenced by nodes
     * in the repository.
     * <li>remove the node type if it is not currently referenced, otherwise
     * throw exception
     * </ul>
     * <p/>
     * the above checks are absolutely necessary in order to guarantee
     * integrity of repository content.
     *
     * @param nodeTypeName The name of the node type to be checked.
     * @throws RepositoryException If the specified node type is currently
     *                             being referenced or if the check failed for
     *                             some other reason.
     */
    protected void checkForReferencesInContent(QName nodeTypeName)
            throws RepositoryException {
        throw new RepositoryException("not yet implemented");
    }

    //-------------------------------------------------------< implementation >
    /**
     * @return the definition of the root node
     */
    public NodeDef getRootNodeDef() {
        //return rootNodeDef;
    	throw new UnsupportedOperationException();
    }

    /**
     * Set an event channel to inform about changes.
     *
     * @param eventChannel event channel
     */
    public void setEventChannel(NodeTypeEventChannel eventChannel) {
        //this.eventChannel = eventChannel;
        eventChannel.setListener(this);
    }


    /**
     * @param ntName
     * @param entCache
     * @param ntdCache
     * @return
     * @throws NoSuchNodeTypeException
     * @param entCache cache of already-built effective node types
     * @param ntdCache cache of node type definitions
     * @return
     * @throws NoSuchNodeTypeException if a node type reference (e.g. a supertype)
     *                                 could not be resolved.
     */
    static EffectiveNodeType getEffectiveNodeType(QName ntName,
                                                  EffectiveNodeTypeCache entCache,
                                                  Map ntdCache)
            throws NoSuchNodeTypeException {
        // 1. check if effective node type has already been built
        EffectiveNodeType ent = entCache.get(new QName[]{ntName});
        if (ent != null) {
            return ent;
        }

        // 2. make sure we've got the definition of the specified node type
        NodeTypeDef ntd = (NodeTypeDef) ntdCache.get(ntName);
        if (ntd == null) {
            throw new NoSuchNodeTypeException(ntName.toString());
        }

        // 3. build effective node type
        synchronized (entCache) {
            try {
                ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
                // store new effective node type
                entCache.put(ent);
                return ent;
            } catch (NodeTypeConflictException ntce) {
                // should never get here as all known node types should be valid!
                String msg = "internal error: encountered invalid registered node type " + ntName;
                log.debug(msg);
                throw new NoSuchNodeTypeException(msg, ntce);
            }
        }
    }

    /**
     * Returns an effective node type representation of the given node types.
     *
     * @param ntNames  array of node type names
     * @param entCache cache of already-built effective node types
     * @param ntdCache cache of node type definitions
     * @return
     * @throws NodeTypeConflictException if the effective node type representation
     *                                   could not be built due to conflicting
     *                                   node type definitions.
     * @throws NoSuchNodeTypeException if a node type reference (e.g. a supertype)
     *                                 could not be resolved.
     */
    static EffectiveNodeType getEffectiveNodeType(QName[] ntNames,
                                                  EffectiveNodeTypeCache entCache,
                                                  Map ntdCache)
            throws NodeTypeConflictException, NoSuchNodeTypeException {

        EffectiveNodeTypeCache.WeightedKey key =
                new EffectiveNodeTypeCache.WeightedKey(ntNames);

        // 1. check if aggregate has already been built
        if (entCache.contains(key)) {
            return entCache.get(key);
        }

        // 2. make sure we've got the definitions of the specified node types
        for (int i = 0; i < ntNames.length; i++) {
            if (!ntdCache.containsKey(ntNames[i])) {
                throw new NoSuchNodeTypeException(ntNames[i].toString());
            }
        }

        // 3. build aggregate
        EffectiveNodeType result = null;
        synchronized (entCache) {
            // build list of 'best' existing sub-aggregates
            ArrayList tmpResults = new ArrayList();
            while (key.getNames().length > 0) {
                // check if we've already built this aggregate
                if (entCache.contains(key)) {
                    tmpResults.add(entCache.get(key));
                    // subtract the result from the temporary key
                    // (which is 'empty' now)
                    key = key.subtract(key);
                    break;
                }
                /**
                 * walk list of existing aggregates sorted by 'weight' of
                 * aggregate (i.e. the cost of building it)
                 */
                boolean foundSubResult = false;
                Iterator iter = entCache.keyIterator();
                while (iter.hasNext()) {
                    EffectiveNodeTypeCache.WeightedKey k =
                            (EffectiveNodeTypeCache.WeightedKey) iter.next();
                    /**
                     * check if the existing aggregate is a 'subset' of the one
                     * we're looking for
                     */
                    if (key.contains(k)) {
                        tmpResults.add(entCache.get(k));
                        // subtract the result from the temporary key
                        key = key.subtract(k);
                        foundSubResult = true;
                        break;
                    }
                }
                if (!foundSubResult) {
                    /**
                     * no matching sub-aggregates found:
                     * build aggregate of remaining node types through iteration
                     */
                    QName[] remainder = key.getNames();
                    for (int i = 0; i < remainder.length; i++) {
                        NodeTypeDef ntd = (NodeTypeDef) ntdCache.get(remainder[i]);
                        EffectiveNodeType ent =
                                EffectiveNodeType.create(ntd, entCache, ntdCache);
                        // store new effective node type
                        entCache.put(ent);
                        if (result == null) {
                            result = ent;
                        } else {
                            result = result.merge(ent);
                            // store intermediate result (sub-aggregate)
                            entCache.put(result);
                        }
                    }
                    // add aggregate of remaining node types to result list
                    tmpResults.add(result);
                    break;
                }
            }
            // merge the sub-aggregates into new effective node type
            for (int i = 0; i < tmpResults.size(); i++) {
                if (result == null) {
                    result = (EffectiveNodeType) tmpResults.get(i);
                } else {
                    result = result.merge((EffectiveNodeType) tmpResults.get(i));
                    // store intermediate result
                    entCache.put(result);
                }
            }
        }
        // we're done
        return result;
    }

    static void checkForCircularInheritance(QName[] supertypes,
                                            Stack inheritanceChain,
                                            Map ntDefCache)
            throws InvalidNodeTypeDefException, RepositoryException {
        for (int i = 0; i < supertypes.length; i++) {
            QName nt = supertypes[i];
            int pos = inheritanceChain.lastIndexOf(nt);
            if (pos >= 0) {
                StringBuffer buf = new StringBuffer();
                for (int j = 0; j < inheritanceChain.size(); j++) {
                    if (j == pos) {
                        buf.append("--> ");
                    }
                    buf.append(inheritanceChain.get(j));
                    buf.append(" extends ");
                }
                buf.append("--> ");
                buf.append(nt);
                throw new InvalidNodeTypeDefException("circular inheritance detected: " + buf.toString());
            }

            try {

                NodeTypeDef ntd = (NodeTypeDef) ntDefCache.get(nt);
                QName[] sta = ntd.getSupertypes();
                if (sta.length > 0) {
                    // check recursively
                    inheritanceChain.push(nt);
                    checkForCircularInheritance(sta, inheritanceChain, ntDefCache);
                    inheritanceChain.pop();
                }
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "unknown supertype: " + nt;
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
    }

    static void checkForCircularNodeAutoCreation(EffectiveNodeType childNodeENT,
                                                 Stack definingParentNTs,
                                                 EffectiveNodeTypeCache anEntCache,
                                                 Map ntDefCache)
            throws InvalidNodeTypeDefException {
        // check for circularity through default node types of auto-created child nodes
        // (node type 'a' defines auto-created child node with default node type 'a')
        QName[] childNodeNTs = childNodeENT.getAllNodeTypes();
        for (int i = 0; i < childNodeNTs.length; i++) {
            QName nt = childNodeNTs[i];
            int pos = definingParentNTs.lastIndexOf(nt);
            if (pos >= 0) {
                StringBuffer buf = new StringBuffer();
                for (int j = 0; j < definingParentNTs.size(); j++) {
                    if (j == pos) {
                        buf.append("--> ");
                    }
                    buf.append("node type ");
                    buf.append(definingParentNTs.get(j));
                    buf.append(" defines auto-created child node with default ");
                }
                buf.append("--> ");
                buf.append("node type ");
                buf.append(nt);
                throw new InvalidNodeTypeDefException("circular node auto-creation detected: "
                        + buf.toString());
            }
        }

        NodeDef[] nodeDefs = childNodeENT.getAutoCreateNodeDefs();
        for (int i = 0; i < nodeDefs.length; i++) {
            QName dnt = nodeDefs[i].getDefaultPrimaryType();
            QName definingNT = nodeDefs[i].getDeclaringNodeType();
            try {
                if (dnt != null) {
                    // check recursively
                    definingParentNTs.push(definingNT);
                    checkForCircularNodeAutoCreation(getEffectiveNodeType(dnt, anEntCache, ntDefCache),
                            definingParentNTs, anEntCache, ntDefCache);
                    definingParentNTs.pop();
                }
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = definingNT
                        + " defines invalid default node type for child node " + nodeDefs[i].getName();
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
    }

    private EffectiveNodeType internalRegister(NodeTypeDef ntd)
            throws InvalidNodeTypeDefException, RepositoryException {
        QName name = ntd.getName();
        if (name != null && registeredNTDefs.containsKey(name)) {
            String msg = name + " already exists";
            log.debug(msg);
            throw new InvalidNodeTypeDefException(msg);
        }

        EffectiveNodeType ent =
                validateNodeTypeDef(ntd, entCache, registeredNTDefs, nsReg, false);

        // store new effective node type instance
        entCache.put(ent);

        // register clone of node type definition
        ntd = (NodeTypeDef) ntd.clone();
        registeredNTDefs.put(name, ntd);

        // store property & child node definitions of new node type by id
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            _propDefs.put(pda[i].getPropDefId(), pda[i]);
        }
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            _nodeDefs.put(nda[i].getNodeDefId(), nda[i]);
        }

        return ent;
    }

    /**
     * Validates and registers the specified collection of <code>NodeTypeDef</code>
     * objects. An <code>InvalidNodeTypeDefException</code> is thrown if the
     * validation of any of the contained <code>NodeTypeDef</code> objects fails.
     * <p/>
     * Note that in the case an exception is thrown no node type will be
     * eventually registered.
     *
     * @param ntDefs collection of <code>NodeTypeDef</code> objects
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     * @see #registerNodeType
     */
    private void internalRegister(Collection ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException {
        internalRegister(ntDefs, false);
    }

    /**
     * Same as {@link #internalRegister(java.util.Collection)} except for the
     * additional <code>lenient</code> parameter which governs whether
     * validation can be lenient (e.g. for built-in node types) or has to be
     * strict (such as in the case of custom node types). This differentiation
     * is unfortunately required as there are e.g. properties defined in built-in
     * node types which are auto-created but don't have a fixed default value
     * that can be exposed in a property definition because it is
     * system-generated (such as jcr:primaryType in nt:base).
     */
    private void internalRegister(Collection ntDefs, boolean lenient)
            throws InvalidNodeTypeDefException, RepositoryException {

        // create working copies of current ent & ntd caches:
        // cache of pre-built aggregations of node types
        EffectiveNodeTypeCache tmpENTCache = (EffectiveNodeTypeCache) entCache.clone();
        // map of node type names and node type definitions
        Map tmpNTDefCache = new HashMap(registeredNTDefs);

        // temporarily register the node type definition
        // and do some preliminary checks
        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            QName name = ntd.getName();
            if (name != null && registeredNTDefs.containsKey(name)) {
                String msg = name + " already exists";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // add definition to temporary cache
            tmpNTDefCache.put(ntd.getName(), ntd);
        }

        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();

            EffectiveNodeType ent =
                    validateNodeTypeDef(ntd, tmpENTCache, tmpNTDefCache, nsReg, lenient);

            // store new effective node type instance
            tmpENTCache.put(ent);
        }

        // since no exception was thrown so far the definitions are assumed to be valid
        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();

            // register clone of node type definition
            ntd = (NodeTypeDef) ntd.clone();
            registeredNTDefs.put(ntd.getName(), ntd);
            // store property & child node definitions of new node type by id
            PropDef[] pda = ntd.getPropertyDefs();
            for (int i = 0; i < pda.length; i++) {
                _propDefs.put(pda[i].getPropDefId(), pda[i]);
            }
            NodeDef[] nda = ntd.getChildNodeDefs();
            for (int i = 0; i < nda.length; i++) {
                _nodeDefs.put(nda[i].getNodeDefId(), nda[i]);
            }
        }

        // finally add newly created effective node types to entCache
        for (Iterator it = tmpENTCache.keyIterator(); it.hasNext(); ) {
            EffectiveNodeTypeCache.WeightedKey k =
                    (EffectiveNodeTypeCache.WeightedKey) it.next();
            if (!entCache.contains(k)) {
                entCache.put(tmpENTCache.get(k));
            }
        }
    }

    private void internalUnregister(QName name) throws NoSuchNodeTypeException {
        NodeTypeDef ntd = (NodeTypeDef) registeredNTDefs.get(name);
        if (ntd == null) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        registeredNTDefs.remove(name);
        /**
         * remove all affected effective node types from aggregates cache
         * (copy keys first to prevent ConcurrentModificationException)
         */
        ArrayList keys = new ArrayList(entCache.keySet());
        for (Iterator keysIter = keys.iterator(); keysIter.hasNext();) {
            EffectiveNodeTypeCache.WeightedKey k =
                    (EffectiveNodeTypeCache.WeightedKey) keysIter.next();
            EffectiveNodeType ent = entCache.get(k);
            if (ent.includesNodeType(name)) {
                entCache.remove(k);
            }
        }

        // remove property & child node definitions
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            _propDefs.remove(pda[i].getPropDefId());
        }
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            _nodeDefs.remove(nda[i].getNodeDefId());
        }
    }

    private void internalUnregister(Collection ntNames)
            throws NoSuchNodeTypeException {
        for (Iterator iter = ntNames.iterator(); iter.hasNext();) {
            QName name = (QName) iter.next();
            internalUnregister(name);
        }
    }

    /**
     * Utility method for verifying that the namespace of a <code>QName</code>
     * is registered; a <code>null</code> argument is silently ignored.
     *
     * @param name name whose namespace is to be checked
     * @param nsReg namespace registry to be used for checking
     * @throws RepositoryException if the namespace of the given name is not
     *                             registered or if an unspecified error occured
     */
    private static void checkNamespace(QName name, NamespaceRegistry nsReg)
            throws RepositoryException {
        if (name != null) {
            // make sure namespace uri denotes a registered namespace
            nsReg.getPrefix(name.getNamespaceURI());
        }
    }

    /**
     * Validates the specified <code>NodeTypeDef</code> within the context of
     * the two other given collections and returns an <code>EffectiveNodeType</code>.
     *
     * @param ntd
     * @param entCache
     * @param ntdCache cache of 'known' node type definitions, used to resolve dependencies
     * @param nsReg    namespace registry used for validating qualified names
     * @param lenient flag governing whether validation can be lenient or has to be strict
     * @return an effective node type representation of the specified <code>NodeTypeDef</code>
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException         if another error occurs
     */
    private static EffectiveNodeType validateNodeTypeDef(NodeTypeDef ntd,
                                                         EffectiveNodeTypeCache entCache,
                                                         Map ntdCache,
                                                         NamespaceRegistry nsReg,
                                                         boolean lenient)
            throws InvalidNodeTypeDefException, RepositoryException {

        /**
         * the effective (i.e. merged and resolved) node type resulting from
         * the specified node type definition;
         * the effective node type will finally be created after the definition
         * has been verified and checked for conflicts etc.; in some cases it
         * will be created already at an earlier stage during the validation
         * of child node definitions
         */
        EffectiveNodeType ent = null;

        QName name = ntd.getName();
        if (name == null) {
            String msg = "no name specified";
            log.debug(msg);
            throw new InvalidNodeTypeDefException(msg);
        }
        checkNamespace(name, nsReg);

        // validate supertypes
        QName[] supertypes = ntd.getSupertypes();
        if (supertypes.length > 0) {
            for (int i = 0; i < supertypes.length; i++) {
                checkNamespace(supertypes[i], nsReg);
                /**
                 * simple check for infinite recursion
                 * (won't trap recursion on a deeper inheritance level)
                 */
                if (name.equals(supertypes[i])) {
                    String msg = "[" + name + "] invalid supertype: "
                            + supertypes[i] + " (infinite recursion))";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
                if (!ntdCache.containsKey(supertypes[i])) {
                    String msg = "[" + name + "] invalid supertype: "
                            + supertypes[i];
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
            }

            /**
             * check for circularity in inheritance chain
             * ('a' extends 'b' extends 'a')
             */
            Stack inheritanceChain = new Stack();
            inheritanceChain.push(name);
            checkForCircularInheritance(supertypes, inheritanceChain, ntdCache);
        }

        /**
         * note that infinite recursion through inheritance is automatically
         * being checked by the following call to getEffectiveNodeType(...)
         * as it's impossible to register a node type definition which
         * references a supertype that isn't registered yet...
         */

        /**
         * build effective (i.e. merged and resolved) node type from supertypes
         * and check for conflicts
         */
        if (supertypes.length > 0) {
            try {
                EffectiveNodeType est = getEffectiveNodeType(supertypes, entCache, ntdCache);
                // make sure that all primary types except nt:base extend from nt:base
                if (!ntd.isMixin() && !QName.NT_BASE.equals(ntd.getName())
                        && !est.includesNodeType(QName.NT_BASE)) {
                    String msg = "[" + name + "] all primary node types except"
                            + " nt:base itself must be (directly or indirectly) derived from nt:base";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
            } catch (NodeTypeConflictException ntce) {
                String msg = "[" + name + "] failed to validate supertypes";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "[" + name + "] failed to validate supertypes";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }

        checkNamespace(ntd.getPrimaryItemName(), nsReg);

        // validate property definitions
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            /**
             * sanity check:
             * make sure declaring node type matches name of node type definition
             */
            if (!name.equals(pd.getDeclaringNodeType())) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] invalid declaring node type specified";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            checkNamespace(pd.getName(), nsReg);
            // check that auto-created properties specify a name
            if (pd.definesResidual() && pd.isAutoCreated()) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] auto-created properties must specify a name";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check that auto-created properties specify a type
            if (pd.getRequiredType() == PropertyType.UNDEFINED
                    && pd.isAutoCreated()) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] auto-created properties must specify a type";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            /**
             * check default values:
             * make sure type of value is consistent with required property type
             */
            InternalValue[] defVals = pd.getDefaultValues();
            if (defVals != null && defVals.length != 0) {
                int reqType = pd.getRequiredType();
                for (int j = 0; j < defVals.length; j++) {
                    if (reqType == PropertyType.UNDEFINED) {
                        reqType = defVals[j].getType();
                    } else {
                        if (defVals[j].getType() != reqType) {
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] type of default value(s) is not consistent with required property type";
                            log.debug(msg);
                            throw new InvalidNodeTypeDefException(msg);
                        }
                    }
                }
            } else {
                // no default values specified
                if (!lenient) {
                    // auto-created properties must have a default value
                    if (pd.isAutoCreated()) {
                        String msg = "[" + name + "#" + pd.getName()
                                + "] auto-created property must have a default value";
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                }
            }

            // check that default values satisfy value constraints
            ValueConstraint[] constraints = pd.getValueConstraints();
            if (constraints != null && constraints.length > 0) {
                if (defVals != null && defVals.length > 0) {
                    // check value constraints on every value
                    for (int j = 0; j < defVals.length; j++) {
                        // constraints are OR-ed together
                        boolean satisfied = false;
                        ConstraintViolationException cve = null;
                        for (int k = 0; k < constraints.length; k++) {
                            try {
                                constraints[k].check(defVals[j]);
                                // at least one constraint is satisfied
                                satisfied = true;
                                break;
                            } catch (ConstraintViolationException e) {
                                cve = e;
                                continue;
                            }
                        }
                        if (!satisfied) {
                            // report last exception we encountered
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] default value does not satisfy value constraint";
                            log.debug(msg);
                            throw new InvalidNodeTypeDefException(msg, cve);
                        }
                    }
                }

                /**
                 * ReferenceConstraint:
                 * the specified node type must be registered, with one notable
                 * exception: the node type just being registered
                 */
                if (pd.getRequiredType() == PropertyType.REFERENCE || pd.getRequiredType() == PropertyType283.WEAKREFERENCE) {
                    for (int j = 0; j < constraints.length; j++) {
                        ReferenceConstraint rc = (ReferenceConstraint) constraints[j];
                        QName ntName = rc.getNodeTypeName();
                        if (!name.equals(ntName) && !ntdCache.containsKey(ntName)) {
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] invalid REFERENCE value constraint '"
                                    + ntName + "' (unknown node type)";
                            log.debug(msg);
                            throw new InvalidNodeTypeDefException(msg);
                        }
                    }
                }
            }
        }

        // validate child-node definitions
        NodeDef[] cnda = ntd.getChildNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            NodeDef cnd = cnda[i];
            /**
             * sanity check:
             * make sure declaring node type matches name of node type definition
             */
            if (!name.equals(cnd.getDeclaringNodeType())) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] invalid declaring node type specified";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            checkNamespace(cnd.getName(), nsReg);
            // check that auto-created child-nodes specify a name
            if (cnd.definesResidual() && cnd.isAutoCreated()) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] auto-created child-nodes must specify a name";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check that auto-created child-nodes specify a default primary type
            if (cnd.getDefaultPrimaryType() == null
                    && cnd.isAutoCreated()) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] auto-created child-nodes must specify a default primary type";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check default primary type
            QName dpt = cnd.getDefaultPrimaryType();
            checkNamespace(dpt, nsReg);
            boolean referenceToSelf = false;
            EffectiveNodeType defaultENT = null;
            if (dpt != null) {
                // check if this node type specifies itself as default primary type
                if (name.equals(dpt)) {
                    referenceToSelf = true;
                }
                /**
                 * the default primary type must be registered, with one notable
                 * exception: the node type just being registered
                 */
                if (!name.equals(dpt) && !ntdCache.containsKey(dpt)) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] invalid default primary type '" + dpt + "'";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
                /**
                 * build effective (i.e. merged and resolved) node type from
                 * default primary type and check for conflicts
                 */
                try {
                    if (!referenceToSelf) {
                        defaultENT = getEffectiveNodeType(dpt, entCache, ntdCache);
                    } else {
                        /**
                         * the default primary type is identical with the node
                         * type just being registered; we have to instantiate it
                         * 'manually'
                         */
                        ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
                        defaultENT = ent;
                    }
                    if (cnd.isAutoCreated()) {
                        /**
                         * check for circularity through default primary types
                         * of auto-created child nodes (node type 'a' defines
                         * auto-created child node with default primary type 'a')
                         */
                        Stack definingNTs = new Stack();
                        definingNTs.push(name);
                        checkForCircularNodeAutoCreation(defaultENT, definingNTs, entCache, ntdCache);
                    }
                } catch (NodeTypeConflictException ntce) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] failed to validate default primary type";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg, ntce);
                } catch (NoSuchNodeTypeException nsnte) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] failed to validate default primary type";
                    log.debug(msg);
                    throw new InvalidNodeTypeDefException(msg, nsnte);
                }
            }

            // check required primary types
            QName[] reqTypes = cnd.getRequiredPrimaryTypes();
            if (reqTypes != null && reqTypes.length > 0) {
                for (int n = 0; n < reqTypes.length; n++) {
                    QName rpt = reqTypes[n];
                    checkNamespace(rpt, nsReg);
                    referenceToSelf = false;
                    /**
                     * check if this node type specifies itself as required
                     * primary type
                     */
                    if (name.equals(rpt)) {
                        referenceToSelf = true;
                    }
                    /**
                     * the required primary type must be registered, with one
                     * notable exception: the node type just being registered
                     */
                    if (!name.equals(rpt) && !ntdCache.containsKey(rpt)) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] invalid required primary type: " + rpt;
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                    /**
                     * check if default primary type satisfies the required
                     * primary type constraint
                     */
                    if (defaultENT != null && !defaultENT.includesNodeType(rpt)) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] default primary type does not satisfy required primary type constraint "
                                + rpt;
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                    /**
                     * build effective (i.e. merged and resolved) node type from
                     * required primary type constraint and check for conflicts
                     */
                    try {
                        if (!referenceToSelf) {
                            getEffectiveNodeType(rpt, entCache, ntdCache);
                        } else {
                            /**
                             * the required primary type is identical with the
                             * node type just being registered; we have to
                             * instantiate it 'manually'
                             */
                            if (ent == null) {
                                ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
                            }
                        }
                    } catch (NodeTypeConflictException ntce) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] failed to validate required primary type constraint";
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg, ntce);
                    } catch (NoSuchNodeTypeException nsnte) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] failed to validate required primary type constraint";
                        log.debug(msg);
                        throw new InvalidNodeTypeDefException(msg, nsnte);
                    }
                }
            }
        }

        /**
         * now build effective (i.e. merged and resolved) node type from
         * this node type definition; this will potentially detect more
         * conflicts or problems
         */
        if (ent == null) {
            try {
                ent = EffectiveNodeType.create(ntd, entCache, ntdCache);
            } catch (NodeTypeConflictException ntce) {
                String msg = "[" + name + "] failed to resolve node type definition";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "[" + name + "] failed to resolve node type definition";
                log.debug(msg);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
        return ent;
    }

    private static NodeDef createRootNodeDef() {
        NodeDefImpl def = new NodeDefImpl();

        // FIXME need a fake declaring node type:
        // rep:root is not quite correct but better than a non-existing node type
        def.setDeclaringNodeType(QName.REP_ROOT);
        def.setRequiredPrimaryTypes(new QName[]{QName.REP_ROOT});
        def.setDefaultPrimaryType(QName.REP_ROOT);
        def.setMandatory(true);
        def.setProtected(false);
        def.setOnParentVersion(OnParentVersionAction.VERSION);
        def.setAllowsSameNameSiblings(false);
        def.setAutoCreated(true);
        return def;
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been registered.
     */
    private void notifyRegistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la =
                new NodeTypeRegistryListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (NodeTypeRegistryListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].nodeTypeRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been re-registered.
     */
    private void notifyReRegistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la =
                new NodeTypeRegistryListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (NodeTypeRegistryListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].nodeTypeReRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been unregistered.
     */
    private void notifyUnregistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la =
                new NodeTypeRegistryListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (NodeTypeRegistryListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].nodeTypeUnregistered(ntName);
            }
        }
    }
}
