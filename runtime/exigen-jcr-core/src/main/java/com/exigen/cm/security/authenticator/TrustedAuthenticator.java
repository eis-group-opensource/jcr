/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.security.authenticator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.security.JCRAuthenticator;
import com.exigen.cm.security.JCRPrincipals;

/**
 * Simple authenticator used for trusted environment.
 * It assumes that no authentication is needed and all Principals
 * are already awailable in SimpleCredentials. Array of groups user
 * belongs to should be provided as attribute of SimpleCredentials under
 * name defined by ATTRIBUTE_GROUPS. If there is no such attribute it is
 * assumed that no groups are available
 *
 */
public class TrustedAuthenticator implements JCRAuthenticator {

	public static final String ATTRIBUTE_GROUPS = "groups";
	public static final String ATTRIBUTE_CONTEXTS = "contexts";
	private RepositoryImpl repository;
	
	 /** @inheritDoc */ 
	public JCRPrincipals authenticate(Credentials credentials, String workspaceName) throws LoginException, RepositoryException {
        if (!(credentials instanceof SimpleCredentials)){
            throw new RepositoryException("TrustedAuthenticator can check only SimpleCredentials");
        }
		return authenticate((SimpleCredentials)credentials);
	}

	
	
	/** @inheritDoc */ 
	public void init(Map params, RepositoryImpl repository) throws RepositoryException {
		// does nothing
		this.repository = repository;
	}
		
	protected JCRPrincipals authenticate(SimpleCredentials credentials) {
	    Object _groups = credentials.getAttribute(ATTRIBUTE_GROUPS);
        List<String> groups = null;
        if (_groups != null){
            if (_groups instanceof List){
                groups = (List<String>)_groups;   
            } else {
                groups = Arrays.asList((String[]) _groups);
            }
        }
        
        Object _contexts = credentials.getAttribute(ATTRIBUTE_CONTEXTS);
        List<String> contexts = null;
        if (_contexts != null){
            if (_groups instanceof List){
                contexts = (List<String>)_contexts;   
            } else {
                contexts = Arrays.asList((String[]) _contexts);
            }
        }

        if (groups == null) {
            groups = new ArrayList<String>();
        }
        if (contexts == null) {
            contexts = new ArrayList<String>();
        }
		return new JCRPrincipals(credentials.getUserID(), groups, contexts, repository.isIgnoreCaseInSecurity());
	}



	public void setRepository(RepositoryImpl repository) throws RepositoryException {
		this.repository = repository;
		
	}

}
