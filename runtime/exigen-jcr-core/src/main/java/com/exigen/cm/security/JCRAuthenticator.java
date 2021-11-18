/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
/**
 * 
 */
package com.exigen.cm.security;

import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import com.exigen.cm.impl.RepositoryImpl;

/**
 * Supports user authentification.
 * Repository.login() should delegate authentication to the implementation of this interface.
 * Implementation should provide constructor without parameters.  
 *
 */
public interface JCRAuthenticator {
	/**
	 * Sets repository
     * 
     * @param repository instance of the repository
	 * @throws RepositoryException If any error occures
	 */
	public void setRepository(RepositoryImpl repository) throws RepositoryException;

	/**
	 * Intializes authenticator. This method should be called before authentication
     * 
	 * @param params Parameters are implementation specific
     * @param repository instance of the repository
	 * @throws RepositoryException If any error occures
	 */
	public void init(Map params, RepositoryImpl repository) throws RepositoryException;
	
	/**
	 * Authenticates user.
	 * @param credentials
     * @param workspaceName 
	 * @return 
	 * @throws LoginException  If authentication fails
	 * @throws RepositoryException If any error occures
	 */
	public JCRPrincipals authenticate(Credentials credentials, String workspaceName) throws LoginException, RepositoryException;

}
