/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.nodetype.DBNodeTypeReader;
import com.exigen.cm.impl.security.SessionSecurityManager;
import com.exigen.cm.impl.state2.ItemStatus;
import com.exigen.cm.impl.state2.SessionInfo;
import com.exigen.cm.impl.state2.SessionStoreContainer;
import com.exigen.cm.impl.state2._ItemState;
import com.exigen.cm.impl.state2._NodeState;
import com.exigen.cm.impl.state2._PropertyState;
import com.exigen.cm.impl.state2._SessionStateManager;
import com.exigen.cm.impl.xml.SecurityExport;
import com.exigen.cm.impl.xml.SecurityImport;
import com.exigen.cm.jackrabbit.lock.LockManager;
import com.exigen.cm.jackrabbit.lock.LockManagerImpl;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.name.NoPrefixDeclaredException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.name.QName;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeDef;
import com.exigen.cm.jackrabbit.nodetype.NodeTypeRegistry;
import com.exigen.cm.jackrabbit.nodetype.xml.NodeTypeWriter;
import com.exigen.cm.jackrabbit.value.ValueFactoryImpl;
import com.exigen.cm.jackrabbit.version.VersionManager;
import com.exigen.cm.jackrabbit.version.VersionManagerImpl;
import com.exigen.cm.jackrabbit.xml.DocViewSAXEventGenerator;
import com.exigen.cm.jackrabbit.xml.ImportHandler;
import com.exigen.cm.jackrabbit.xml.NodeExportAcceptor;
import com.exigen.cm.jackrabbit.xml.SessionImporter;
import com.exigen.cm.jackrabbit.xml.SysViewSAXEventGenerator;
import com.exigen.vf.commons.logging.LogUtils;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class SessionImpl implements Session{

    /** Logger for this class */
    private static final Log log = LogFactory.getLog(SessionImpl.class);
    
    private Boolean sessionRegistered = false;
    
    private WorkspaceImpl workspace;
    
    private boolean alive = true;

    
    private HashSet<String> contextIds = new HashSet<String>();

    
    /**
     * Lock tokens
     */
    protected final Set<String> lockTokens = new HashSet<String>();

    private NodeManager nodeManager;


    //private ValueFactoryImpl valueFactory;

    /**
     * the attributes of this session
     */
    protected final HashMap<String, Object> attributes = new HashMap<String, Object>();
    
    /**
     * Listeners (weak references)
     */
    protected final Map listeners = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);
    


    private NodeImpl rootNode;


    private LockManagerImpl lockManager;


    private VersionManager versionMgr;

    //private final Long sessionId;

    private final Throwable createdThread;

    private _SessionStateManager stateManager;
    //private NodeTypeRegistry ntRegistry;

	private SessionStoreContainer storeContainer;

	/**
	 * Session flag if has value <code>true</code> denoting that changes performed
	 * in scope of this session must ignore node locking. May result in unexpected behaviour
	 * in case other session set lock on a node modified in scope of this session which
	 * might not know about this fact and will try to apply node modifications ...
	 * USE CAREFULLY  
	 */
	private Boolean ignoreLock = null;

    public SessionImpl(WorkspaceImpl workspace, Long sessionId) throws NoSuchNodeTypeException, RepositoryException {
    	//this.sessionId = sessionId;
        createdThread = new Throwable();
        this.workspace = workspace;
        this.storeContainer = new SessionStoreContainer(this);
        String userId = null;
        if (workspace.getPrincipals() != null) {
        	userId = workspace.getPrincipals().getUserId();
        }
        this.lockManager = new LockManagerImpl(this);
        this.stateManager = new _SessionStateManager(this, sessionId, userId ,workspace.getName(), workspace.isSecuritySwitchedOn(), lockManager);
        nodeManager = new NodeManager(this);
        //this.valueFactory = new ValueFactoryImpl();
        
        this.versionMgr = new VersionManagerImpl(this, _getRepository().getVersionStorageNodeId());
    }

    public Workspace getWorkspace() {
        return workspace;
    }
    
    public Repository getRepository(){
        return _getRepository();
    }

    
    /**
     * Overrides repository ignoreLock value for given session.
     * @param ignore
     */
    public void setIgnoreLock(boolean ignore){
        ignoreLock = Boolean.valueOf(ignore);
    }
    
    /**
     * clears session scoped ignore lock value and following
     * calls to isIgnoreLock would return repository isIgnoreLock value.
     */
    public void resetIgnoreLock(){
        ignoreLock = null;
    }
    
    /**
     * returns session configured ignoreLock value or repository
     * ignoreLock value if session doesn't override it.
     * @return
     */
    public boolean isIgnoreLock(){
        return ignoreLock == null ? _getRepository().isIgnoreLock() : ignoreLock;
    }
    
    public RepositoryImpl _getRepository() {
        return workspace.getRepository();    
    }
    
    public String getUserID() {
        if (workspace.principals != null){
            return workspace.principals.getUserId();
        } else {
            return null;
        }
    }

    public Collection<String> getGroupIDs() {
        if (workspace.principals != null){
            return workspace.principals.getGroupIdList();
        } else {
            return new ArrayList<String>();
        }
    }

    public Collection<String> getContextIDs() {
        if (workspace.principals != null){
            return workspace.principals.getContextIdList();
        } else {
        	return new ArrayList<String>();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }


    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public Node getRootNode() throws RepositoryException {
    	sanityCheck();
        if (rootNode == null){
            rootNode = getNodeManager().buildNode(workspace.getRootNodeId());
        }
        return rootNode;
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
        	_NodeState state = stateManager.getItemByUUID(uuid, false);
            NodeImpl node = getNodeManager().buildNode(state.getNodeId());
            return node;
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(uuid);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = Path.create(absPath, getNamespaceResolver(), true);
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            Item i = getItem(p);
            _ItemState s = null;
            if (i instanceof NodeImpl){
            	s = ((NodeImpl)i).getNodeState();
            } else {
            	s = ((PropertyImpl)i).getPropertyState();
            }
            if (s.getStatus().equals(ItemStatus.Destroyed) || s.getStatus().equals(ItemStatus.Invalidated)){
            	throw new PathNotFoundException(absPath);
            }
            return i;
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(absPath);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path:" + absPath;
            LogUtils.debug(log, msg);
            throw new RepositoryException(msg, mpe);
        }
    }
       


    public Item getItem(Path path) throws PathNotFoundException, RepositoryException {
    	_ItemState s = stateManager.getItem(path, true);
    	if (s instanceof _NodeState){
    		return nodeManager.buildNode((_NodeState)s);
    		//return new NodeImpl((_NodeState)s, stateManager);
    	} else {
    		_PropertyState ps = (_PropertyState) s;
    		return new PropertyImpl(new NodeImpl(ps.getParent(), stateManager),ps);
    	}
    }


    
    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        try {
            return path.toJCRPath(getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }
    public boolean itemExists(String absPath) throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        try {
            Path p = Path.create(absPath, getNamespaceResolver(), true);
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + absPath);
            }
            try {
                getItem(p);
                return true;
            } catch (PathNotFoundException pe){
                return false;
            }
        } catch (MalformedPathException mpe) {
            String msg = "invalid path:" + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ItemExistsException, PathNotFoundException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        // check sanity of this session
        sanityCheck();

        // check paths & get node instances

        Path srcPath;
        Path.PathElement srcName;
        Path srcParentPath;
        NodeImpl targetNode;
        NodeImpl srcParentNode;
        try {
            srcPath = Path.create(srcAbsPath, getNamespaceResolver(), true);
            if (!srcPath.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + srcAbsPath);
            }
            srcName = srcPath.getNameElement();
            srcParentPath = srcPath.getAncestor(1);
            ItemImpl item = (ItemImpl) getItem(srcPath);
            if (!item.isNode()) {
                throw new PathNotFoundException(srcAbsPath);
            }
            targetNode = (NodeImpl) item;
            //targetNode.lockRow();
            srcParentNode = (NodeImpl) getItem(srcParentPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(srcAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = srcAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        NodeImpl destParentNode;
        try {
            destPath = Path.create(destAbsPath, getNamespaceResolver(), true);
            if (!destPath.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + destAbsPath);
            }
            if (srcPath.isAncestorOf(destPath)) {
                String msg = destAbsPath + ": invalid destination path (cannot be descendant of source path)";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentNode = (NodeImpl) getItem(destParentPath);
            //destParentNode.lockRow();
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(destAbsPath);
        } catch (MalformedPathException mpe) {
            String msg = destAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // verify that both source and destination parent nodes are checked-out
        if (!srcParentNode.internalIsCheckedOut(true)) {
            String msg = srcAbsPath + ": cannot move a child of a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }
        if (!destParentNode.internalIsCheckedOut(true)) {
            String msg = destAbsPath + ": cannot move a target to a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check for name collisions

        try {
            ItemImpl item = (ItemImpl) getItem(destPath);
            if (!item.isNode()) {
                // there's already a property with that name
                throw new ItemExistsException(item.safeGetJCRPath());
            } else {
                // there's already a node with that name
                // check same-name sibling setting of both new and existing node
            	if (!((NodeImpl) item).getNodeState().isRemoved()){
	                if (!destParentNode.getDefinition().allowsSameNameSiblings()
	                        || !((NodeImpl) item).getDefinition().allowsSameNameSiblings()) {
	                    throw new ItemExistsException(item.safeGetJCRPath());
	                }
            	}
            }
        } catch (AccessDeniedException ade) {
            // FIXME by throwing ItemExistsException we're disclosing too much information
            throw new ItemExistsException(destAbsPath);
        } catch (PathNotFoundException pnfe) {
            // no name collision
        }

        // check constraints

        // get applicable definition of target node at new location
        NodeTypeImpl nt = (NodeTypeImpl) targetNode.getPrimaryNodeType();
        NodeDefinitionImpl newTargetDef;
        try {
            newTargetDef = destParentNode.state.getApplicableChildNodeDefinition(destName.getName(), nt.getQName());
        } catch (RepositoryException re) {
            String msg = destAbsPath + ": no definition found in parent node's node type for new node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        // check protected flag of old & new parent
        if (destParentNode.getDefinition().isProtected()) {
            String msg = destAbsPath + ": cannot add a child node to a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }
        if (srcParentNode.getDefinition().isProtected()) {
            String msg = srcAbsPath + ": cannot remove a child node from a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        srcParentNode.checkLock();
        destParentNode.checkLock();

        //String targetUUID = targetNode.getUUID();
        //Long targetId = targetNode.getNodeId();
        int index = srcName.getIndex();
        if (index == 0) {
            index = 1;
        }

        if (srcParentNode.isSame(destParentNode)) {
            // do rename
            //throw new UnsupportedOperationException();
            destParentNode.renameChildNode(targetNode, destName.getName(), getStateManager().createNodeModification());
        } else {
            // do move:
            // 1. remove child node entry from old parent
            //NodeState srcParentState = (NodeState) srcParentNode.getOrCreateTransientItemState();
            
            //srcParentState.removeChildNodeEntry(srcName.getName(), index);
            //// 2. re-parent target node
            //NodeState targetState = (NodeState) targetNode.getOrCreateTransientItemState();
            //targetState.setParentUUID(destParentNode.internalGetUUID());
            //// 3. add child node entry to new parent
            //NodeState destParentState = (NodeState) destParentNode.getOrCreateTransientItemState();
            //destParentState.addChildNodeEntry(destName.getName(), targetUUID);
            //
            
            internalMove(targetNode, destName.getName(), destParentNode, getStateManager().createNodeModification());
            
            
            //if (true){
            //    throw new UnsupportedOperationException();
            //}
                
        }

        // change definition of target
        targetNode.onRedefine(newTargetDef.unwrap().getSQLId());
        
    }


    private void internalMove(NodeImpl targetNode, QName destName, NodeImpl destNode, NodeModification nm) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        NodeImpl targetParent = (NodeImpl) targetNode.getParent();
        QName srcName = targetNode.getQName();
        int destIndex = 1;
        //1. check is dest exist
        NodeImpl tn = (NodeImpl) destNode.getNode(destName, 1 , false);
        if (tn != null){
            if (!tn.getDefinition().allowsSameNameSiblings()){
                throw new ItemExistsException(tn.safeGetJCRPath());
            }
            //1.a if exist , check SNS, and set index
            try {
                NodeIterator ni = destNode.getNodes(destName.toJCRName(workspace._getNamespaceRegistry()), false);
                while (ni.hasNext()){
                    ni.nextNode();
                    destIndex++;
                }
            } catch (NoPrefixDeclaredException e) {
            } catch (RepositoryException e) {
            }
        }
        //count src nodes count
        int srcCount = 0;
        try {
            NodeIterator ni = targetParent.getNodes(srcName.toJCRName(workspace._getNamespaceRegistry()), false);
            while (ni.hasNext()){
                ni.next();
                srcCount++;
            }
        } catch (Exception e) {
            throw new RepositoryException(e);
        } 
        
        //2.move node
        //targetNode.updateName(destName, new Long(destIndex), destNode);
        if (true){
            targetNode.getNodeState().updateName(destName, new Long(destIndex), destNode.getNodeState(), nm, true);
            //throw new RepositoryException("Implement me");
        }
        
        //4. TODO update security
        
        
        //5. if srx SNS, change SNS and child paths
        if (srcCount > 1){
            targetParent.rebuildChildIndex(srcName, nm, srcCount - 1);
        }  
    }

    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    	sanityCheck();
        //((NodeImpl)getRootNode()).save();
    	
    	Long rootNodeId = workspace.getRootNodeId();
    	stateManager.save(rootNodeId, true, true, true);
    }

    public void refresh(boolean keepChanges) throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        getRootNode().refresh(keepChanges);

    }

    public boolean hasPendingChanges() throws RepositoryException {
        // check sanity of this session
        sanityCheck();

        return stateManager.hasPendingChanges(workspace.getRootNodeId());
    }

    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return _getValueFactory();
    }

    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        sanityCheck();

        // build the set of actions to be checked
        String[] strings = actions.split(",");
        HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            set.add(strings[i]);
        }

        Item item = getItem(absPath);
        if (item instanceof Node){
            NodeImpl node = (NodeImpl) item;
            Long nodeId = node.getNodeId();
            getSecurityManager().checkPermission(nodeId ,actions);
        } else {
            throw new RepositoryException("Permissions supported only for nodes");
        }
        
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        return getImportContentHandler(parentAbsPath,uuidBehavior, null);
    }

    
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior, ZipFile zipFile) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item;
        try {
            Path p = Path.create(parentAbsPath, getNamespaceResolver(), true);
            if (!p.isAbsolute()) {
                throw new RepositoryException("not an absolute path: " + parentAbsPath);
            }
            _ItemState state = stateManager.getItem(p, true);
            item = buildItem(state);
        } catch (MalformedPathException mpe) {
            String msg = parentAbsPath + ": invalid path";
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(parentAbsPath);
        }
        if (!item.isNode()) {
            throw new PathNotFoundException(parentAbsPath);
        }
        NodeImpl parent = (NodeImpl) item;

        // verify that parent node is checked-out
        if (!parent.internalIsCheckedOut(true)) {
            String msg = parentAbsPath + ": cannot add a child to a checked-in node";
            log.debug(msg);
            throw new VersionException(msg);
        }

        // check protected flag of parent node
        if (parent.getDefinition().isProtected()) {
            String msg = parentAbsPath + ": cannot add a child to a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        parent.checkLock();

        SessionImporter importer = new SessionImporter(parent, this, uuidBehavior, zipFile);
        return new ImportHandler(importer, getNamespaceResolver(), workspace._getNamespaceRegistry());
        
    }

    private Item buildItem(_ItemState state) throws RepositoryException {
		if (state instanceof _NodeState){
			return new NodeImpl((_NodeState)state, stateManager);
		} else {
			NodeImpl node = new NodeImpl(state.getParent(), stateManager);
			return new PropertyImpl(node, (_PropertyState) state);
		}
	}

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        importXML(parentAbsPath, in, uuidBehavior, null);
    }

        
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior, ZipFile zipFile) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        ImportHandler handler = (ImportHandler)
                getImportContentHandler(parentAbsPath, uuidBehavior, zipFile);
        try {
            XMLReader parser =
                    XMLReaderFactory.createXMLReader();
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);
            // being paranoid...
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes",
                    false);

            parser.parse(new InputSource(in));
        } catch (SAXException se) {
            // check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                String msg = "failed to parse XML stream";
                log.debug(msg);
                throw new InvalidSerializedDataException(msg, se);
            }
        }
    }
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
    	exportSystemView(absPath, contentHandler, skipBinary, null, noRecurse, false);
    }

    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, boolean skipSystem) throws PathNotFoundException, SAXException, RepositoryException {
        exportSystemView(absPath, contentHandler, skipBinary, zout, noRecurse, skipSystem, null);        
    }

    /**
     * 
     * @param absPath
     * @param contentHandler
     * @param skipBinary
     * @param zout
     * @param noRecurse
     * @param skipSystem
     * @param stopTypes nodes of type listed in stopTypes won't be exported
     * @throws PathNotFoundException
     * @throws SAXException
     * @throws RepositoryException
     */
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, boolean skipSystem, NodeExportAcceptor exportAcceptor) throws PathNotFoundException, SAXException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item = getItem(absPath);
        if (!item.isNode()) {
            // there's a property, though not a node at the specified path
            throw new PathNotFoundException(absPath);
        }
        new SysViewSAXEventGenerator((Node) item, noRecurse, skipBinary, zout,
                contentHandler, skipSystem, exportAcceptor).serialize();
        if (zout != null){
            try {
                // we add empty entry in case there was no binary properties at all
                ZipEntry ze = new ZipEntry("empty");
                zout.putNextEntry(ze);
                zout.closeEntry();
                
                zout.close();
            } 
            catch (IOException ioe){
                log.error("failed to close ZipOutputStream", ioe);
                throw new RepositoryException(ioe);
            }
        }
        
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
       exportSystemView(absPath, out, skipBinary, null, noRecurse, false); 
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, boolean skipSystem) throws IOException, PathNotFoundException, RepositoryException {
        exportSystemView(absPath, out, skipBinary, zout, noRecurse, skipSystem, false);
    }

    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, boolean skipSystem, boolean indenting) throws IOException, PathNotFoundException, RepositoryException {
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportSystemView(absPath, serializer.asContentHandler(),
                    skipBinary, zout, noRecurse, skipSystem);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        exportDocumentView(absPath, contentHandler, skipBinary, null, noRecurse);
    }

    
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, ZipOutputStream zout, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        exportDocumentView(absPath, contentHandler, skipBinary, zout, noRecurse, null);        
    }
    
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, NodeExportAcceptor exportAcceptor) throws PathNotFoundException, SAXException, RepositoryException {
        // check sanity of this session
        sanityCheck();

        Item item = getItem(absPath);
        if (!item.isNode()) {
            // there's a property, though not a node at the specified path
            throw new PathNotFoundException(absPath);
        }
        new DocViewSAXEventGenerator((Node) item, noRecurse, skipBinary, zout,
                contentHandler, true, exportAcceptor).serialize();
        if (zout != null){
            try {
                // we add empty entry in case there was no binary properties at all
                ZipEntry ze = new ZipEntry("empty");
                zout.putNextEntry(ze);
                zout.closeEntry();
                
                zout.close();
            } 
            catch (IOException ioe){
                log.error("failed to close ZipOutputStream", ioe);
                throw new RepositoryException(ioe);
            }
        }
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        exportDocumentView(absPath, out, skipBinary, noRecurse, null);
    }
    
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse, NodeExportAcceptor exportAcceptor) throws IOException, PathNotFoundException, RepositoryException {
        exportDocumentView(absPath, out, skipBinary, null, noRecurse, false, exportAcceptor);
    }
    
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, ZipOutputStream zout, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        exportDocumentView(absPath, out, skipBinary, zout, noRecurse, false);
    }

    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, boolean indenting) throws IOException, PathNotFoundException, RepositoryException {
        exportDocumentView(absPath, out, skipBinary, zout, noRecurse, indenting, null);    
    }
    
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, ZipOutputStream zout, boolean noRecurse, boolean indenting, NodeExportAcceptor exportAcceptor) throws IOException, PathNotFoundException, RepositoryException {
        OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportDocumentView(absPath, serializer.asContentHandler(),
                    skipBinary, zout, noRecurse, exportAcceptor);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }
    

    public void exportSecurity(String absPath, ContentHandler contentHandler, boolean noRecurse) throws RepositoryException {
        // check sanity of this session
        sanityCheck();        
        try {            
            SecurityExport secExport = new SecurityExport(contentHandler, (Node)getItem(absPath), noRecurse, true);
            secExport.serialize(true);
        } catch (Exception e) {
            LogUtils.error(log, e.getMessage(), e);
            throw new RepositoryException(e);
        }          
    }    

    public void exportSecurity(String absPath, OutputStream out, boolean noRecurse) throws RepositoryException {
    	exportSecurity(absPath, out, noRecurse, false);
    }
    
    public void exportSecurity(String absPath, OutputStream out, boolean noRecurse, boolean indenting) throws RepositoryException {
        try {
            /*SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
            TransformerHandler handler = factory.newTransformerHandler();
            handler.getTransformer().setOutputProperty(OutputKeys.INDENT, indenting ? "yes" : "no");
            handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            handler.setResult(new StreamResult(out));*/
            OutputFormat format = new OutputFormat("xml", "UTF-8", indenting);
            XMLSerializer serializer = new XMLSerializer(out, format);
            exportSecurity(absPath, serializer, noRecurse);
        } catch (Exception e) {
            LogUtils.error(log, e.getMessage(), e);
            throw new RepositoryException(e);
        }          
    }

    public void exportNodeTypes(OutputStream out) throws RepositoryException {
    	exportNodeTypes(out, false);
    }
    
    public void exportNodeTypes(OutputStream out, boolean indenting) throws RepositoryException {        
        //DatabaseConnection conn = getConnection();
        RepositoryImpl repository = (RepositoryImpl)getRepository();
        NamespaceRegistryImpl nmRegistry = repository.getNamespaceRegistry();
        
        DBNodeTypeReader reader = repository.getNodeTypeReader();
        List listDefs = reader.all();
        NodeTypeDef[] types = (NodeTypeDef[]) listDefs.toArray(new NodeTypeDef[listDefs.size()]);
        try {
        	//Writer w = new OutputStreamWriter(out);
        	//CompactNodeTypeDefWriter.write(listDefs, nmRegistry, w);
        	//w.close();
            NodeTypeWriter.write(out, types, nmRegistry, indenting);
        } catch (Exception e) {
            LogUtils.error(log, e.getMessage(), e);
            throw new RepositoryException(e);
        }
    }
    
    public void importSecuirty(InputStream inputStream) throws RepositoryException {
        SecurityImport secImport = new SecurityImport(this, null);
        secImport.doImport(inputStream);        
    }

    public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public String[] getNamespacePrefixes() throws RepositoryException {
        return workspace._getNamespaceRegistry().getPrefixes();
    }

    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return getWorkspace().getNamespaceRegistry().getURI(prefix);
    }

    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return getWorkspace().getNamespaceRegistry().getPrefix(uri);
    }

    public void logout() {
    	try {
	    	try {
	    		sanityCheck();
	    	} catch (RepositoryException exc){
	    		throw new RuntimeException("session already logged out");
	    	}
	        // notify listeners that session is about to be closed
	    	
	        workspace._getRepository().sessionCount--;
	
	        
	        
	        try {
	        	while (connectionStack.size() > 0){
	                if (workspace.connection.isLive()){
		                workspace.connection.setAllowClose(true);
		                workspace.connection.close();
	                }
	                popConnection();
	        		
	        	}
	            if (workspace.connection != null && !workspace.externalConnection){
	                if (workspace.connection.isLive()){
	                    workspace.connection.setAllowClose(true);
	                    workspace.connection.close();
	                }
	                workspace.connection = null;
	            }
	        } catch (RepositoryException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        if (workspace.externalConnection){
	            workspace.connection.setAllowClose(workspace.externalAllowClose);
	        }
	        
	        
	        notifyLoggingOut();
	        
	        this.alive = false;
	        
	        // finally notify listeners that session has been closed
	        notifyLoggedOut();
	        
	        try {
	        	while (connectionStack.size() > 0){
	                if (workspace.connection.isLive()){
		                workspace.connection.setAllowClose(true);
		                workspace.connection.close();
	                }
	                popConnection();
	        		
	        	}
	            if (workspace.connection != null && !workspace.externalConnection){
	                if (workspace.connection.isLive()){
	                    workspace.connection.setAllowClose(true);
	                    workspace.connection.close();
	                }
	                workspace.connection = null;
	            }
	        } catch (RepositoryException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        if (workspace.externalConnection){
	            workspace.connection.setAllowClose(workspace.externalAllowClose);
	        }
	        
	        getStateManager().logout();
	        stateManager = null;
    	} finally {
    		try {
				unregisterSession();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
    	}
        
    }

    Stack<DatabaseConnection> connectionStack = new Stack<DatabaseConnection>();

	private DatabaseConnection connection;
    
	public void pushConnection() {
		connectionStack.push(workspace.connection);
		workspace.connection = null;
	}

	public void popConnection() {
		if (connectionStack.size() > 0 ){
			if (workspace.connection != null) {
				if (workspace.connection.isLive()){
	                workspace.connection.setAllowClose(true);
	                try {
						workspace.connection.close();
					} catch (RepositoryException e) {
					}
	            }
			}
				
			workspace.connection =connectionStack.pop();
		}
		
	}

    
	public boolean isDiconnected(){
		return workspace.connection == null;
	}
	
    public void disconnect() throws RepositoryException{
    	while (connectionStack.size() > 0){
    		try {
	            if (workspace.connection.isLive()){
	                workspace.connection.setAllowClose(true);
	                workspace.connection.close();
	            }
    		} catch (Exception exc){}
            popConnection();
    		
    	}
    	
    	
        if (workspace.connection != null){
            if (workspace.connection.isLive()){
                workspace.connection.setAllowClose(true);
                workspace.connection.close();
            }
            workspace.connection = null;
        }
    }

    public boolean isLive() {
        return alive;
    }


    public WorkspaceImpl _getWorkspace() {
        return workspace;
    }
    
    /**
     * Performs a sanity check on this session.
     *
     * @throws RepositoryException if this session has been rendered invalid
     *                             for some reason (e.g. if this session has
     *                             been closed explicitly or if it has expired)
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        if (!alive) {
            throw new RepositoryException("this session has been closed");
        }
    }

    public NamespaceResolver getNamespaceResolver() {
        //return new SessionNamespaceResolver(this);
        return workspace._getNamespaceRegistry();
    }

    public NodeTypeRegistry getNodeTypeRegistry() throws RepositoryException {
        /*if (ntRegistry == null){
            ntRegistry = new NodeTypeRegistry(workspace._getNamespaceRegistry(),workspace._getNodeTypeReader());
        }
        return ntRegistry;*/
        return workspace._getNodeTypeManager().getNodeTypeRegistry();
    }

    public ValueFactoryImpl _getValueFactory() {
        return this._getRepository().getValueFactory();
    }


    public NodeTypeManagerImpl getNodeTypeManager() throws RepositoryException {
        return (NodeTypeManagerImpl) workspace.getNodeTypeManager();
    }

    public DatabaseConnection getConnection() throws RepositoryException {
        sanityCheck();
        //TODO do we need rolback ???
        //workspace.connection.rollback();
        
        if (connection != null) {
        	return connection;
        }
        
        return workspace.getConnection();
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    public void addListener(SessionListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }
    
    /**
     * Remove a <code>SessionListener</code>
     *
     * @param listener an existing listener
     */
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify the listeners that this session has been closed.
     */
    protected void notifyLoggedOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        SessionListener[] la = new SessionListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (SessionListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].loggedOut(this);
            }
        }
    }
    
    /**
     * Notify the listeners that this session is about to be closed.
     */
    protected void notifyLoggingOut() {
        // copy listeners to array to avoid ConcurrentModificationException
        SessionListener[] la = new SessionListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (SessionListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].loggingOut(this);
            }
        }
    }

    //------------------------------------------------------< locking support >
    /**
     * {@inheritDoc}
     */
    public void addLockToken(String lt) {
        try {
            addLockToken(lt, true);
        } catch (RepositoryException e) {
            throw new RuntimeException("Error adding lock token");
        }
    }

    /**
     * Internal implementation of {@link #addLockToken(String)}. Additionally
     * takes a parameter indicating whether the lock manager needs to be
     * informed.
     * @throws RepositoryException 
     */
    public void addLockToken(String lt, boolean notify) throws RepositoryException {
        synchronized (lockTokens) {
            if (lockTokens.add(lt) && notify) {
                //try {
                    getLockManager().lockTokenAdded(this, lt);
                //} catch (RepositoryException e) {
                //    log.error("Lock manager not available.", e);
                //}
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        synchronized (lockTokens) {
            String[] result = new String[lockTokens.size()];
            lockTokens.toArray(result);
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken(String lt) {
        removeLockToken(lt, true);
    }

    /**
     * Internal implementation of {@link #removeLockToken(String)}. Additionally
     * takes a parameter indicating whether the lock manager needs to be
     * informed.
     */
    public void removeLockToken(String lt, boolean notify) {
        synchronized (lockTokens) {
            if (lockTokens.remove(lt) && notify) {
                //try {
                    getLockManager().lockTokenRemoved(this, lt);
                //} catch (RepositoryException e) {
                //    log.error("Lock manager not available.", e);
                //}
            }
        }
    }

    /**
     * Returns the <code>VersionManager</code> associated with this session.
     *
     * @return the <code>VersionManager</code> associated with this session
     */
    public VersionManager getVersionManager() {
        return versionMgr;
    }
    
    
    public SessionSecurityManager getSecurityManager(){
        return stateManager.getSecurityManager();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (isLive()){
                log.error("Unclosed session", createdThread);
                logout();
            }
        } catch (Exception exc){
            
        }
        super.finalize();
    }



    public NodeImpl getNodeById(NodeId nodeId) throws RepositoryException {
        return getNodeManager().buildNode(nodeId.getId());
    }
    
	public List<NodeImpl> getNodesById(HashSet<Long> nodeIds) throws RepositoryException{
		//ArrayList<_NodeState> cache = new ArrayList<_NodeState>();
		List<NodeImpl> result = new ArrayList<NodeImpl>();
		_SessionStateManager sm = getStateManager();
		ArrayList<Long> ids = new ArrayList<Long>(nodeIds);
		for(Long id:nodeIds){
			_NodeState state = sm.getNodeState(id, ids);
			result.add(getNodeManager().buildNode(state));
		}
		return result;
		
		
	}
    
    public NodeImpl getNodeById(Long nodeId) throws RepositoryException {
        return getNodeManager().buildNode(nodeId);
    }

	public _SessionStateManager getStateManager() {
		return stateManager;
	}

	public SessionStoreContainer getStoreContainer() {
		return storeContainer;
	}

	Long cahcedSessionId = null;
	
	public Long getSessionId() {
		if (cahcedSessionId == null){
			cahcedSessionId = stateManager.getSessionId();
		}
		return cahcedSessionId;
	}
	
	/**
	 * Creates a new Workspace with the specified name and returns it.
	 * Throws an AccessDeniedException if this Session does not have
	 * permission to create the new Workspace.
	 * Throws an UnsupportedRepositoryOperationException if the
	 * repository does not support the creation of workspaces.
	 *
	 * @param name workspace name
	 * @throws RepositoryException
	 * @throws AccessDeniedException
	 * @throws UnsupportedRepositoryOperationException
	 */
	public void createWorkspace(String name) throws RepositoryException,AccessDeniedException, UnsupportedRepositoryOperationException{
		_getRepository().createWorkspace(name);
	}

	public SessionInfo getSessionInfo() throws RepositoryException{
		return stateManager.getSessionInfo();
	}
	
	
	
	public void registerSession() throws RepositoryException{
		synchronized (sessionRegistered) {
			if (!sessionRegistered){
				_getRepository().getSessionManager().registerSession(this);
				sessionRegistered = true;
			}
		}
	}
	
	private void unregisterSession() throws RepositoryException{
		synchronized (sessionRegistered) {
			if (sessionRegistered){
				_getRepository().getSessionManager().unregisterSession(this);
				sessionRegistered = false;
			}
		}
	}

	public void registerContextId(String stepId){
		contextIds.add(stepId);
	}

	public void unregisterContextId(String stepId){
		contextIds.remove(stepId);
	}

	public Set<String> getContextIds() {
		return contextIds;
	}
	
	public void refreshAllNodes() throws RepositoryException{
		stateManager.refreshAllNodes();
	}
	
	public void setAttribute(String name, Object value){
		attributes.put(name, value);
	}

	public void setConnection(DatabaseConnection connection) {
		this.connection = connection;
	}

}


/*
 * $Log: SessionImpl.java,v $
 * Revision 1.27  2009/08/21 13:40:25  maksims
 * *** empty log message ***
 *
 * Revision 1.26  2009/03/23 11:20:42  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.25  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.24  2009/02/03 12:02:12  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.23  2009/01/29 13:38:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.22  2009/01/27 14:07:58  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.21  2008/12/05 08:25:23  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.20  2008/11/24 10:04:01  maksims
 * node export acceptor added to support custom filtering of exported nodes
 *
 */