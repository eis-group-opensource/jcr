/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.observation.ObservationManagerFactory;



class WorkspaceInfo {
    
    private static final Log log = LogFactory.getLog(WorkspaceInfo.class);


    /**
     * workspace configuration (passed in constructor)
     */
    private final WorkspaceConfig config;

    /**
     * file system (instantiated on init)
     */
    //private FileSystem fs;

    /**
     * persistence manager (instantiated on init)
     */
    //private PersistenceManager persistMgr;

    /**
     * item state provider (instantiated on init)
     */
    //private SharedItemStateManager itemStateMgr;

    /**
     * observation manager factory (instantiated on init)
     */
    private ObservationManagerFactory obsMgrFactory;

    /**
     * system session (lazily instantiated)
     */
    //private SystemSession systemSession;

    /**
     * search manager (lazily instantiated)
     */
    //private SearchManager searchMgr;

    /**
     * lock manager (lazily instantiated)
     */
    //private LockManagerImpl lockMgr;

    /**
     * flag indicating whether this instance has been initialized.
     */
    private boolean initialized;

    /**
     * timestamp when the workspace has been determined being idle
     */
    private long idleTimestamp;


	private RepositoryImpl repository;


	private Long workspaceId;


	private Long rootNodeId;

    /**
     * Creates a new <code>WorkspaceInfo</code> based on the given
     * <code>config</code>.
     *
     * @param config workspace configuration
     */
    protected WorkspaceInfo(RepositoryImpl repository,WorkspaceConfig config) {
        this.config = config;
        this.repository = repository;
        idleTimestamp = 0;
        initialized = false;
    }

    /**
     * Returns the workspace name.
     *
     * @return the workspace name
     */
    String getName() {
        return config.getName();
    }

    /**
     * Returns the workspace configuration.
     *
     * @return the workspace configuration
     */
    public WorkspaceConfig getConfig() {
        return config;
    }

    /**
     * Returns the timestamp when the workspace has become idle or zero
     * if the workspace is currently not idle.
     *
     * @return the timestamp when the workspace has become idle or zero if
     *         the workspace is not idle.
     */
    long getIdleTimestamp() {
        return idleTimestamp;
    }

    /**
     * Sets the timestamp when the workspace has become idle. if
     * <code>ts == 0</code> the workspace is marked as being currently
     * active.
     *
     * @param ts timestamp when workspace has become idle.
     */
    void setIdleTimestamp(long ts) {
        idleTimestamp = ts;
    }

    /**
     * Returns <code>true</code> if this workspace info is initialized,
     * otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this workspace info is initialized.
     */
    synchronized boolean isInitialized() {
        return initialized;
    }

    /**
     * Returns the workspace file system.
     *
     * @return the workspace file system
     */
    /*synchronized FileSystem getFileSystem() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        return fs;
    }*/

    /**
     * Returns the workspace persistence manager.
     *
     * @return the workspace persistence manager
     * @throws RepositoryException if the persistence manager could not be instantiated/initialized
     */
    /*synchronized PersistenceManager getPersistenceManager()
            throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        return persistMgr;
    }*/

    /**
     * Returns the workspace item state provider
     *
     * @return the workspace item state provider
     * @throws RepositoryException if the workspace item state provider
     *                             could not be created
     */
    /*synchronized SharedItemStateManager getItemStateProvider()
            throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        return itemStateMgr;
    }*/

    /**
     * Returns the observation manager factory for this workspace
     *
     * @return the observation manager factory for this workspace
     */
    synchronized ObservationManagerFactory getObservationManagerFactory() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        return obsMgrFactory;
    }

    /**
     * Returns the search manager for this workspace.
     *
     * @return the search manager for this workspace, or <code>null</code>
     *         if no <code>SearchManager</code>
     * @throws RepositoryException if the search manager could not be created
     */
    /*synchronized SearchManager getSearchManager() throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        if (searchMgr == null) {
            if (config.getSearchConfig() == null) {
                // no search index configured
                return null;
            }
            // search manager is lazily instantiated in order to avoid
            // 'chicken & egg' bootstrap problems
            searchMgr = new SearchManager(config.getSearchConfig(),
                    nsReg,
                    ntReg,
                    itemStateMgr,
                    rootNodeId,
                    getSystemSearchManager(getName()),
                    SYSTEM_ROOT_NODE_ID);
        }
        return searchMgr;
    }*/

    /**
     * Returns the lock manager for this workspace.
     *
     * @return the lock manager for this workspace
     * @throws RepositoryException if the lock manager could not be created
     */
    /*synchronized LockManager getLockManager() throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // lock manager is lazily instantiated in order to avoid
        // 'chicken & egg' bootstrap problems
        if (lockMgr == null) {
            lockMgr = new LockManagerImpl(getSystemSession(), fs);
        }
        return lockMgr;
    }*/

    /**
     * Returns the system session for this workspace.
     *
     * @return the system session for this workspace
     * @throws RepositoryException if the system session could not be created
     */
    /*synchronized SystemSession getSystemSession() throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // system session is lazily instantiated in order to avoid
        // 'chicken & egg' bootstrap problems
        if (systemSession == null) {
            systemSession = SystemSession.create(RepositoryImpl.this, config);
        }
        return systemSession;
    }*/

    /**
     * Initializes this workspace info. The following components are
     * initialized immediately:
     * <ul>
     * <li>file system</li>
     * <li>persistence manager</li>
     * <li>shared item state manager</li>
     * <li>observation manager factory</li>
     * </ul>
     * The following components are initialized lazily (i.e. on demand)
     * in order to save resources and to avoid 'chicken & egg' bootstrap
     * problems:
     * <ul>
     * <li>system session</li>
     * <li>lock manager</li>
     * <li>search manager</li>
     * </ul>
     */
    synchronized void initialize() throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        log.info("initializing workspace '" + getName() + "'...");

        /*FileSystemConfig fsConfig = config.getFileSystemConfig();
        fsConfig.init();
        fs = fsConfig.getFileSystem();
        */
        /*
        persistMgr = createPersistenceManager(new File(config.getHomeDir()),
                fs,
                config.getPersistenceManagerConfig(),
                rootNodeId,
                nsReg,
                ntReg);
        */
        // create item state manager
        /*try {
            itemStateMgr =
                    new SharedItemStateManager(persistMgr, rootNodeId, ntReg, true);
            try {
                itemStateMgr.addVirtualItemStateProvider(
                        vMgr.getVirtualItemStateProvider());
                itemStateMgr.addVirtualItemStateProvider(
                        virtNTMgr.getVirtualItemStateProvider());
            } catch (Exception e) {
                log.error("Unable to add vmgr: " + e.toString(), e);
            }
        } catch (ItemStateException ise) {
            String msg = "failed to instantiate shared item state manager";
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }*/

        obsMgrFactory = new ObservationManagerFactory(repository);

        initialized = true;

        log.info("workspace '" + getName() + "' initialized");
    }

    /**
     * Disposes all objects this <code>WorkspaceInfo</code> is holding.
     */
    synchronized void dispose() {
        /*if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        log.info("shutting down workspace '" + getName() + "'...");

        // dispose observation manager factory
        obsMgrFactory.dispose();
        obsMgrFactory = null;

        // shutdown search managers
        if (searchMgr != null) {
            searchMgr.close();
            searchMgr = null;
        }

        // close system session
        if (systemSession != null) {
            systemSession.removeListener(RepositoryImpl.this);
            systemSession.logout();
            systemSession = null;
        }

        // dispose shared item state manager
        itemStateMgr.dispose();
        itemStateMgr = null;

        // close persistence manager
        try {
            persistMgr.close();
        } catch (Exception e) {
            log.error("error while closing persistence manager of workspace "
                    + config.getName(), e);
        }
        persistMgr = null;

        // close lock manager
        if (lockMgr != null) {
            lockMgr.close();
            lockMgr = null;
        }

        // close workspace file system
        FileSystemConfig fsConfig = config.getFileSystemConfig();
        fsConfig.dispose();
        fs = null;

        // reset idle timestamp
        idleTimestamp = 0;

        initialized = false;

        log.info("workspace '" + getName() + "' has been shutdown");
        */
        throw new UnsupportedOperationException();
    }

	public Long getRootNodeId(DatabaseConnection connection) throws RepositoryException {
		initDBInfo(connection);
		return rootNodeId;
	}

	private void initDBInfo(DatabaseConnection connection) throws RepositoryException{
		if (workspaceId == null || rootNodeId == null){
	        DatabaseSelectOneStatement st = DatabaseTools.createSelectOneStatement(Constants.TABLE_WORKSPACE, Constants.TABLE_WORKSPACE__NAME, config.getName());
	        try {
		       	st.execute(connection);
		        RowMap row = st.getRow();
		        this.workspaceId = row.getLong(Constants.FIELD_ID);
		        this.rootNodeId = row.getLong(Constants.TABLE_WORKSPACE__ROOT_NODE);
	        } finally {
	        	st.close();
	        }
		}
	}

	public Long getWorkspaceId(DatabaseConnection connection) throws RepositoryException {
		initDBInfo(connection);
		return workspaceId;
	}
}

/*
 * $Log: WorkspaceInfo.java,v $
 * Revision 1.3  2008/03/28 13:45:57  dparhomenko
 * Autmaticaly unlock orphaned locks(session scoped)
 *
 * Revision 1.2  2008/01/30 09:28:03  dparhomenko
 * PTR#1806303
 *
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/11/30 10:59:48  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.2  2006/06/02 07:21:28  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.1  2006/05/22 14:48:02  dparhomenko
 * PTR#1801941 add observationsupport
 *
 */