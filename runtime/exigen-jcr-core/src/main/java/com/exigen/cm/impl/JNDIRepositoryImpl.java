/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.exigen.cm.RepositoryProvider;

public class JNDIRepositoryImpl implements Repository {

	private final String name;
	private Repository repository;
	
	public JNDIRepositoryImpl(String name){
		this.name = name;
	}

	private synchronized Repository getRepository(){
		if (this.repository == null){
			try {
				if (name == null){
					this.repository = RepositoryProvider.getInstance().getRepository();
				} else {
					this.repository = RepositoryProvider.getInstance().getRepository(name);
				}
			} catch (RepositoryException exc){
				throw new RuntimeException(exc);
			}
		}
		return repository;
	}
	
	public String getDescriptor(String key) {
		return getRepository().getDescriptor(key);
	}

	public String[] getDescriptorKeys() {
		return getRepository().getDescriptorKeys();
	}

	public Session login() throws LoginException, RepositoryException {
		return getRepository().login();
	}

	public Session login(Credentials credentials) throws LoginException, RepositoryException {
		return getRepository().login(credentials);
	}

	public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return getRepository().login(workspaceName);
	}

	public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
		return getRepository().login(credentials, workspaceName);
	}

}
