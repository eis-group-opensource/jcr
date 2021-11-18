/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.state2;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.impl.NamespaceRegistryImpl;
import com.exigen.cm.impl.NodeId;
import com.exigen.cm.impl.NodeTypeManagerImpl;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.observation.ObservationManagerImpl;
import com.exigen.cm.impl.security.SecurityConditionFilter;
import com.exigen.cm.jackrabbit.lock.LockManagerImpl;
import com.exigen.cm.jackrabbit.name.NamespaceResolver;
import com.exigen.cm.jackrabbit.version.VersionManager;
import com.exigen.cm.security.JCRPrincipals;

public class _StandaloneStatemanager extends _AbstractsStateManager implements SecurityConditionFilter{

	private DatabaseConnection conn;
	private NamespaceRegistryImpl nsRegistry;
	private NodeTypeManagerImpl ntm;
	//private NodeTypeRegistry ntRegistry;
	private Long rootId;
	private Long workspaceId;
	private StoreContainer storeContainer;
	//private DBNodeTypeReader nodeTypeReader;

	public _StandaloneStatemanager(RepositoryImpl repository, DatabaseConnection conn, Long rootId, Long workspaceId
			, String userId, String workspaceName, boolean securityAllowed, LockManagerImpl lockManager) throws RepositoryException {
		super(repository, repository.nextId(), userId, workspaceName, securityAllowed, lockManager);
		this.rootId = rootId;
		this.workspaceId = workspaceId;
		this.conn = conn;
		this.nsRegistry = repository.getNamespaceRegistry();
		
        this.ntm = new NodeTypeManagerImpl(nsRegistry, repository.getNodeTypeReader(),  repository);
        //this.ntRegistry = NodeTypeRegistry.create(nsRegistry,ntm.getReader());

        this.storeContainer = new StandaloneStoreContainer(this);
        
        securityManager.configure();
        

	}

	@Override
	protected void assignSession(_NodeState result) throws ConstraintViolationException, RepositoryException {
		result.assignSession(this);
	}

	@Override
	public DatabaseConnection getConnection() throws RepositoryException {
		return conn;
	}

	@Override
	protected NamespaceRegistryImpl getNamespaceRegistry() {
		return nsRegistry;
	}

	@Override
	public NamespaceResolver getNamespaceResolver() {
		return nsRegistry;
	}

	@Override
	protected ObservationManagerImpl getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
		return null;
	}

	@Override
	protected NodeId getRootNodeId() throws RepositoryException {
		return buildNodeId(rootId, conn);
	}

	@Override
	protected SecurityConditionFilter getSecurityConditionFilter() {
		return this;
	}

	@Override
	public StoreContainer getStoreContainer() {
		return storeContainer;
	}

	@Override
	public Long getWorkspaceId() {
		return workspaceId;
	}

	@Override
	public NodeTypeManagerImpl getNodeTypeManager() {
		return ntm;
	}


	public void addSecurityConditions(DatabaseConnection conn, DatabaseSelectAllStatement st, boolean allowBrowse) throws RepositoryException {
		//do nothing
	}

	/*public RepositoryImpl getRepository() {
		return repository;
	}*/
	
	@Override
	protected VersionManager getVersionManager(){
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean allowVersionManager(){
		return false;
	}

	@Override
	public JCRPrincipals getPrincipals() {
		return new FakeJCRPrincipals();
	}

    @Override
    public boolean isSecuritySwitchedOn() {
        return false;
    }
}


class FakeJCRPrincipals extends JCRPrincipals{
	
	public FakeJCRPrincipals(){
		super(null, (String[])null, null, false);
	}
	
}