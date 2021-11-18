/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.security.authenticator;

import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.SessionImpl;
import com.exigen.cm.security.JCRAuthenticator;
import com.exigen.cm.security.JCRPrincipals;
import com.exigen.cm.user.UserHelper;
import com.exigen.vf.commons.logging.LogUtils;

public class RepositoryAuthenticator implements JCRAuthenticator {
    
    private RepositoryImpl repository = null;
    private Log log = LogFactory.getLog(RepositoryAuthenticator.class); 

    public JCRPrincipals authenticate(Credentials credentials, String workspaceName) throws LoginException, RepositoryException {
        SimpleCredentials simpleCredentials = (SimpleCredentials)credentials;
        SessionImpl systemSession = (SessionImpl)repository.getSystemSession();
        Node user = null;
        JCRPrincipals principal = null;
        try {
            user = (Node)systemSession.getItem("/"+UserHelper.USERS_NODE_PATH+"/"+simpleCredentials.getUserID());
            String password = user.getProperty(UserHelper.USER_PASSWORD_PROPERTY).getString();
            String passedPassword = JCRHelper.hashPassword(new String(simpleCredentials.getPassword()));
        
            if (!password.equalsIgnoreCase(passedPassword)) {
                String msg = "Incorrect user name or password!";
                LogUtils.error(log, msg);
                throw new LoginException(msg);
            }
                        
            List<String> groups = UserHelper.getUserGroups(user.getName(), repository);
            
            principal = new JCRPrincipals(user.getName(), groups, null, repository.isIgnoreCaseInSecurity());

        } catch (PathNotFoundException e) {
            String msg = "Incorrect user name or password!";
            LogUtils.error(log, msg);
            throw new LoginException(msg);
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
        return principal;
    }

    public void init(Map params, RepositoryImpl repository) throws RepositoryException {
        this.repository = repository;
        SessionImpl systemSession = (SessionImpl)repository.getSystemSession();
        try {
            systemSession.getItem("/"+UserHelper.PRINCIPALS_NODE_PATH);
        } catch (PathNotFoundException e) {
            systemSession.getRootNode().addNode(UserHelper.PRINCIPALS_NODE_PATH, UserHelper.PRINCIPALS_NODE_TYPE);
            systemSession.getRootNode().addNode(UserHelper.USERS_NODE_PATH, UserHelper.PRINCIPALS_NODE_TYPE);
            systemSession.getRootNode().addNode(UserHelper.GROUPS_NODE_PATH, UserHelper.PRINCIPALS_NODE_TYPE);                        
            systemSession.save();
            String rootUser = (String)params.get(Constants.PROPERTY_ROOT_USER);
            String rootPassword = (String)params.get(Constants.PROPERTY_ROOT_PASSWORD);
            UserHelper.addUser(rootUser, rootPassword, repository);
        } finally {
            if (systemSession != null) {
                systemSession.logout();     
            }
        }
        
    }
    
	public void setRepository(RepositoryImpl repository) throws RepositoryException {
		this.repository = repository;
		
	}


}


/*
 * $Log: RepositoryAuthenticator.java,v $
 * Revision 1.2  2008/04/29 10:56:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:01:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/12/04 11:11:45  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.5  2006/11/30 10:59:59  dparhomenko
 * PTR#1803097 fix ewf nodetypes
 *
 * Revision 1.4  2006/05/18 14:53:58  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.3  2006/04/27 16:56:19  ivgirts
 * PTR #1801676 UserHelper now has only static methods
 *
 * Revision 1.2  2006/04/20 11:43:16  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:47:21  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.5  2006/04/17 06:36:41  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/13 15:04:57  ivgirts
 * PTR #1801676 UserManagement implementation
 *
 * Revision 1.3  2006/04/13 10:03:48  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/12 13:45:12  dparhomenko
 * PTR#0144983 MS FTS
 *
 * Revision 1.1  2006/04/12 13:22:19  ivgirts
 * PTR #1801676 added implmentation of the user management and RepositoryAuthenticator
 *
 */
