/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.nodetype.DBNodeTypeReader;
import com.exigen.cm.impl.observation.ObservationManagerFactory;
import com.exigen.cm.impl.observation.ObservationManagerImpl;
import com.exigen.cm.jackrabbit.name.MalformedPathException;
import com.exigen.cm.jackrabbit.name.Path;
import com.exigen.cm.jackrabbit.xml.ImportHandler;
import com.exigen.cm.query.QueryManagerImpl;
import com.exigen.cm.security.JCRPrincipals;

public class WorkspaceImpl implements Workspace {

    /** Logger for this class */
    private static final Log log = LogFactory.getLog(WorkspaceImpl.class);
    
    
    private RepositoryImpl repository;
    String workspaceName;
    private Long workspaceId;
    private Long rootNodeId;
    private SessionImpl session;
    private NamespaceRegistryImpl namespaceRegistry;
    private NodeTypeManagerImpl nodeTypeManager;
    private DBNodeTypeReader nodeTypeReader;
    JCRPrincipals principals;
    DatabaseConnection connection;
    boolean externalConnection = false;
    protected ObservationManagerImpl obsMgr;
    private String[] workspaceNames;


    boolean externalAllowClose;


	private boolean securitySwitchedOn;
    

    public WorkspaceImpl(RepositoryImpl repository, String workspaceName, JCRPrincipals principals, DatabaseConnection conn) throws RepositoryException {
        this.repository = repository;
        this.workspaceName = workspaceName;
        

        if (principals != null){
            this.principals = principals.assignToSession(this);

            if ("".equals(principals.getUserId()))
            	throw new RepositoryException("User can't be empty");
        }
        
        if (conn == null){
            this.connection = getConnection();
        } else {
            this.connection = conn;
            externalConnection = true;
        }
        try {
            if (workspaceName != null){
                //chech workspace existance
                    //TODO use SQL cache
                    //DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(Constants.TABLE_WORKSPACE, Constants.TABLE_WORKSPACE__NAME, workspaceName);
                    try {
                    	//st.execute(connection);
                        //HashMap row = st.getRow();
                        //this.workspaceId = (Long) row.get(Constants.FIELD_ID);
                        //this.rootNodeId = (Long) row.get(Constants.TABLE_WORKSPACE__ROOT_NODE);
                        WorkspaceInfo wi = repository.getWorkspaceInfo(workspaceName);
                        //this.workspaceId = (Long) row.get(Constants.FIELD_ID);
                        this.rootNodeId = wi.getRootNodeId(connection);
                        this.workspaceId = wi.getWorkspaceId(connection);
                    } catch (ItemNotFoundException exc){
                    	throw new NoSuchWorkspaceException(workspaceName);
                    } 
            } else {
                this.workspaceId = null;
                this.rootNodeId = repository.getSystemRootId();
            }
            if (repository.isSupportSecurity()){
	            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_ACE, false);
	            st.setMaxResult(1);
	            st.execute(connection);
	            if (st.hasNext()){
	            	this.securitySwitchedOn = true;
	            } else {
	            	this.securitySwitchedOn = false;
	            }
            } else {
            	this.securitySwitchedOn = false;
            }
        } catch (RepositoryException exc){
        	if (!externalConnection){
                this.connection.setAllowClose(true);
        		this.connection.close();
        		this.connection = null;
        	}
        	throw exc;
        }
        
        if (externalConnection){
            externalAllowClose = this.connection.isAllowClose();
            this.connection.setAllowClose(false);
        }
        if (repository.isSupportNodeTypeCheck() || workspaceId == null){
        	repository.checkNodeTypeVersion(connection);
        }
        
        this.session = new SessionImpl(this, repository.getSequenceNextId(Constants.SESSION_SEQUENCE));
        namespaceRegistry = new NamespaceRegistryImpl(repository, repository.getNamespaceRegistry());
        nodeTypeReader = repository.getNodeTypeReader();
        //nodeTypeManager = new NodeTypeManagerImpl(session.getStateManager(), namespaceRegistry, nodeTypeReader);
        nodeTypeManager = repository.getNodeTypeManager();

        session.getNodeManager().init();
        
    }
    
    public JCRPrincipals getPrincipals(){
    	return principals;
    	
    }

    public Session getSession() {
        return session;
    }

    public String getName() {
        return workspaceName;
    }

    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // do intra-workspace copy
        internalCopy(srcAbsPath, this, destAbsPath, BatchedItemOperations.COPY);
    }

    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
    	sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace, delegate to intra-workspace copy method
            copy(srcAbsPath, destAbsPath);
            return;
        }

        // check authorization for specified workspace
        //TODO fix this
        /*if (!session.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }*/

        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = (SessionImpl) repository.login(principals, srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            internalCopy(srcAbsPath, srcWsp, destAbsPath, BatchedItemOperations.COPY);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    public void clone(String srcWorkspace, String srcAbsPath,
            String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // intra-workspace move...

        Path srcPath;
        try {
            srcPath = Path.create(srcAbsPath, session.getNamespaceResolver(), true);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        if (!srcPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + srcAbsPath);
        }

        Path destPath;
        try {
            destPath = Path.create(destAbsPath, session.getNamespaceResolver(), true);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        ;
        BatchedItemOperations ops =
                new BatchedItemOperations(repository, this, session.getNodeTypeRegistry(),
                        session.getLockManager(), session, 
                        session.getNamespaceResolver());

        try {
            ops.edit();
        } catch (IllegalStateException e) {
            String msg = "unable to start edit operation";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        boolean succeeded = false;

        try {
            ops.move(srcPath, destPath, true);
            ops.update();
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                ops.cancel();
            }
        }    
        
    }

    public void restore(Version[] versions, boolean removeExisting)
            throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException,
            LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    public QueryManager getQueryManager() throws RepositoryException {
//        switch(repository.getQueryVersion()){
//            case 1:
//                return new com.exigen.cm.query.QueryManagerImpl(this);
//            default:
        return new QueryManagerImpl(this);
//        }
    }

    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return _getNamespaceRegistry();
    }

    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return nodeTypeManager;
    }

    public ObservationManager getObservationManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        sanityCheck();

        if (obsMgr == null) {
            try {
                ObservationManagerFactory factory =
                        repository.getObservationManagerFactory(workspaceName);
                obsMgr = factory.createObservationManager(session);//session.getItemManager()
            } catch (NoSuchWorkspaceException nswe) {
                // should never get here
                String msg = "internal error: failed to instantiate observation manager";
                log.debug(msg);
                throw new RepositoryException(msg, nswe);
            }
        }
        return obsMgr;
    }


    
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        if (workspaceNames == null){
            DatabaseConnection conn = session.getConnection();
            try {
                DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_WORKSPACE, true);
                st.addResultColumn(Constants.TABLE_WORKSPACE__NAME);
                st.execute(conn);
                ArrayList<String> result = new ArrayList<String>();
                while(st.hasNext()){
                    RowMap row = st.nextRow();
                    result.add(row.getString(Constants.TABLE_WORKSPACE__NAME));
                }
                workspaceNames = result.toArray(new String[result.size()]);
                st.close();
            } finally {
                conn.close();
            }
        }
        return workspaceNames;
    }

    public ContentHandler getImportContentHandler(String parentAbsPath,
            int uuidBehavior) throws PathNotFoundException,
            ConstraintViolationException, VersionException, LockException,
            AccessDeniedException, RepositoryException {
        return session.getImportContentHandler(parentAbsPath, uuidBehavior);
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, InvalidSerializedDataException,
            LockException, AccessDeniedException, RepositoryException {
        ImportHandler handler = (ImportHandler) getImportContentHandler(parentAbsPath, uuidBehavior);
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
            session.save();
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public RepositoryImpl getRepository() {
        return repository;
    }

    public Long getRootNodeId() {
        return rootNodeId;
    }

    public NamespaceRegistryImpl _getNamespaceRegistry() {
        return namespaceRegistry;
    }

    public NodeTypeManagerImpl _getNodeTypeManager() {
        return nodeTypeManager;
        
    }

    public DBNodeTypeReader _getNodeTypeReader() {
        return nodeTypeReader;
    }

    public RepositoryImpl _getRepository() {
        return repository;
    }

    /**
     * Performs a sanity check on this workspace and the associated session.
     *
     * @throws RepositoryException if this workspace has been rendered invalid
     *                             for some reason
     */
    public void sanityCheck() throws RepositoryException {
        // check session status
        session.sanityCheck();
    }

    public DatabaseConnection getConnection() throws RepositoryException {
        if (this.connection == null || !this.connection.isLive()){
            this.connection = repository.getConnectionProvider().createConnection();
            this.connection.setAllowClose(false);
        }
        return connection;
    }
    
    
    /**
     * @param srcAbsPath
     * @param srcWsp
     * @param destAbsPath
     * @param flag        one of
     *                    <ul>
     *                    <li><code>COPY</code></li>
     *                    <li><code>CLONE</code></li>
     *                    <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     */
    private void internalCopy(String srcAbsPath,
                              WorkspaceImpl srcWsp,
                              String destAbsPath,
                              int flag)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        Path srcPath;
        try {
            srcPath = Path.create(srcAbsPath, ((SessionImpl)srcWsp.getSession()).getNamespaceResolver(), true);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        if (!srcPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + srcAbsPath);
        }

        Path destPath;
        try {
            destPath = Path.create(destAbsPath, ((SessionImpl)srcWsp.getSession()).getNamespaceResolver(), true);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        if (!destPath.isAbsolute()) {
            throw new RepositoryException("not an absolute path: " + destAbsPath);
        }

        try {
			if (srcPath.isAncestorOf(destPath) && srcWsp.getWorkspaceId().equals(getWorkspaceId()) ){
				throw new RepositoryException("Node cannot be copied into itself");
			}
		} catch (MalformedPathException e1) {
			throw new RepositoryException(e1);
		}
        /*BatchedItemOperations ops =
                new BatchedItemOperations(stateMgr, rep.getNodeTypeRegistry(),
                        session.getLockManager(), session, hierMgr,
                        session.getNamespaceResolver());
                        */
        BatchedItemOperations ops =
            new BatchedItemOperations(repository, this, session.getNodeTypeRegistry(),
            		session.getLockManager(), session, 
            		session.getNamespaceResolver());

        try {
            ops.edit();
        } catch (IllegalStateException e) {
            String msg = "unable to start edit operation";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        boolean succeeded = false;


        try {
        	
            ops.copy(srcPath, createItemStateManager(srcWsp),
                    createAccessManager(srcWsp),
                    destPath, flag);
            ops.update();
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                ops.cancel();
            }
        }
    }

	private AccessManager createAccessManager(WorkspaceImpl w) throws RepositoryException {
		return new AccessManager(w);
	}

	private ItemStateManager createItemStateManager(WorkspaceImpl w) throws RepositoryException {
		return new ItemStateManager(w);
	}

	public SessionImpl _getSession() {
		return session;
	}

	public boolean isSecuritySwitchedOn() {
		return this.principals == null ? false : securitySwitchedOn;
	}

    
    
}


/*
 * $Log: WorkspaceImpl.java,v $
 * Revision 1.8  2009/02/13 15:36:47  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/11/06 10:32:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/11/06 10:23:53  maksims
 * export stop types support added
 *
 * Revision 1.5  2008/10/21 10:49:46  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2008/07/15 11:27:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2008/03/28 13:45:57  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.2  2008/01/30 09:28:03  dparhomenko
 * PTR#1806303
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.25  2007/03/02 09:31:58  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.24  2007/02/26 14:39:09  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.23  2006/12/15 13:13:32  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.22  2006/12/15 10:29:03  maksims
 * #1803625 Stop support query version 1
 *
 * Revision 1.21  2006/12/11 09:29:29  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.20  2006/12/06 11:10:43  dparhomenko
 * PTR#1803525 fix ewf nodetypes
 *
 * Revision 1.19  2006/12/04 11:11:40  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.18  2006/11/22 08:45:06  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.17  2006/11/21 13:36:11  dparhomenko
 * PTR#0149232 node cannot be copied into itself
 *
 * Revision 1.16  2006/11/08 14:43:00  maksims
 * #1801897 Query version 2 made default
 *
 * Revision 1.15  2006/11/02 17:28:10  maksims
 * #1801897 Query2 addition
 *
 * Revision 1.14  2006/10/17 10:46:34  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.13  2006/09/07 10:36:54  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.12  2006/08/22 11:50:10  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.11  2006/08/22 08:36:44  dparhomenko
 * PTR#1802558 fix Utilities
 *
 * Revision 1.10  2006/08/07 14:25:54  dparhomenko
 * PTR#1802383 implement copy in workspace
 *
 * Revision 1.9  2006/07/06 07:54:17  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.8  2006/06/26 14:01:41  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.7  2006/05/22 14:48:02  dparhomenko
 * PTR#1801941 add observationsupport
 *
 * Revision 1.6  2006/04/24 11:37:12  dparhomenko
 * PTR#0144983 build procedure
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
 * Revision 1.7  2006/04/13 10:03:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/04/12 08:30:49  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.5  2006/04/11 15:47:11  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.4  2006/04/10 11:30:12  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.3  2006/04/07 14:43:01  ivgirts
 * PTR #1801059 Authenticator is used for user authentication
 *
 * Revision 1.2  2006/04/06 14:45:35  ivgirts
 * PTR #1801059 namespace and node types now cached in Repository
 *
 * Revision 1.1  2006/03/27 14:57:31  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.10  2006/03/23 14:26:49  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.9  2006/03/21 13:19:26  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.8  2006/03/16 13:13:09  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.7  2006/03/14 11:55:40  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.6  2006/03/13 09:24:33  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.5  2006/03/03 11:07:49  ivgirts
 * PTR #1801059 thorws SQLException replaced with throws RepositoryException
 *
 * Revision 1.4  2006/03/01 16:16:07  maksims
 * #0144986 getQueryManager method implemented
 *
 * Revision 1.3  2006/03/01 11:54:46  dparhomenko
 * PTR#0144983 support locking
 *
 * Revision 1.2  2006/02/16 13:53:04  dparhomenko
 * PTR#0144983 start jdbc implementation
 *
 * Revision 1.1  2006/02/13 12:40:40  dparhomenko
 * PTR#0143252 start jdbc implementation
 *
 */