/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.DEFAULT_REPOSITORY_NAME;
import static com.exigen.cm.Constants.DEFAULT_WORKSPACE;
import static com.exigen.cm.Constants.TABLE_WORKSPACE;
import static com.exigen.cm.Constants.TABLE_WORKSPACE__NAME;
import static com.exigen.cm.Constants.TABLE_WORKSPACE__ROOT_NODE;
import static com.exigen.cm.utils.UtilsHelper.OPTION_REPOSITORY_PROPERTIES;
import static com.exigen.cm.utils.UtilsHelper.OPTION_REPOSITORY_USER;
import static com.exigen.cm.utils.UtilsHelper.OPTION_REPOSITORY_WORKSPACE;
import static com.exigen.cm.utils.UtilsHelper.addRepositoryPropertiesParameter;
import static com.exigen.cm.utils.UtilsHelper.addRepositoryWorkspaceUser;
import static com.exigen.cm.utils.UtilsHelper.getRepositoryConfiguration;

import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.RepositoryImpl;
import com.exigen.cm.impl.security.RepositorySecurityManager;
import com.exigen.cm.impl.security.SecurityPermission;
import com.exigen.cm.impl.security.SecurityPermissionDefinition;
import com.exigen.cm.impl.security.SecurityPostProcessor;
import com.exigen.cm.impl.security.SecurityPrincipal;
import com.exigen.cm.security.JCRPrincipals;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

public class GrantSecurity {

    public void grantSecurity(Map<String, String> configuration, String workspaceName,
            String userId, String groupId) throws RepositoryException {
        
        if (userId != null && groupId != null) {
            throw new RepositoryException(
                    "You must specify either userid or grupId");
        }
        if (userId == null && groupId == null) {
            throw new RepositoryException(
                    "You must specify either userid or grupId");
        }
        RepositoryImpl rep = getRepository(configuration);
        DatabaseConnection conn = rep.getConnectionProvider().createConnection();
        try {
            if (workspaceName == null) {
                workspaceName = DEFAULT_WORKSPACE;
            }
            // find root node id
            DatabaseSelectOneStatement st = DatabaseTools
                    .createSelectOneStatement(TABLE_WORKSPACE,
                            TABLE_WORKSPACE__NAME, workspaceName);
            st.execute(conn);
            RowMap row = st.getRow();
            Long rootNodeId = row.getLong(TABLE_WORKSPACE__ROOT_NODE);

            // set permission
            RepositorySecurityManager manager = rep.getSecurityManager();
            SecurityPostProcessor postProcessor = new SecurityPostProcessor(conn, manager, new JCRPrincipals("", new String[] {}, null, true));
            for(SecurityPermission p:SecurityPermission.values()){
              	  SecurityPermissionDefinition def = new SecurityPermissionDefinition(p, SecurityPrincipal.create(userId, groupId, null), null, true);
              	  manager.assignSecurity(conn, rootNodeId, rootNodeId, def, new JCRPrincipals("superuser",(String[])null, null, true), postProcessor);
            }
            postProcessor.process();
            conn.commit();
            for(SecurityPermission p:SecurityPermission.values()){
            	if (!p.equals(SecurityPermission.X_SUPER_DENY)){
                  postProcessor = new SecurityPostProcessor(conn, manager, new JCRPrincipals("", new String[] {}, null, true));
              	  SecurityPermissionDefinition def = new SecurityPermissionDefinition(p, SecurityPrincipal.create(userId, groupId, null), true, true);
              	  manager.assignSecurity(conn, rootNodeId, rootNodeId, def, new JCRPrincipals("superuser",(String[])null, null, true), postProcessor);
                  postProcessor.process();
                }
            	conn.commit();
            }
            conn.commit();
        } finally {
            conn.close();
        }

    }
    
    private RepositoryImpl getRepository(Map<String, String> configuration) throws RepositoryException {
        RepositoryProvider provider = RepositoryProvider.getInstance();
        provider.configure(DEFAULT_REPOSITORY_NAME, configuration);
        RepositoryImpl repository = (RepositoryImpl)provider.getRepository();
        return repository;
    }

    /**
     * @param args
     * @throws JSAPException 
     */
    public static void main(String[] args) throws Exception{
        SimpleJSAP jsap = new SimpleJSAP(
                "GrantSecurity",
                "Grant Security",
                new Parameter[] {
                        new FlaggedOption("group", JSAP.STRING_PARSER, null,JSAP.NOT_REQUIRED, 'g', "repositoryGroup","Repostiory Group")
                        });
        addRepositoryPropertiesParameter(jsap);
        addRepositoryWorkspaceUser(jsap, false);

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()){
        	return;
        }
        boolean error = false;
        String workspace = config.getString(OPTION_REPOSITORY_WORKSPACE);
        String user = config.getString(OPTION_REPOSITORY_USER);
        String group = config.getString("group");
        
        if (config.success()) {
            if(user == null && group == null){
                error = true;
            } else {
                Map<String, String> configuration = getRepositoryConfiguration(null,config.getString(OPTION_REPOSITORY_PROPERTIES), true, false);
                UtilsHelper.configureRepository(configuration, false, false);

                try {
                    (new GrantSecurity()).grantSecurity(configuration, workspace, user, group);
                    System.out.println("Security granted.");
                } catch (Exception exc){
                    exc.printStackTrace();
                    error = true;
                }
            }
        }
        if (!config.success() || error) {

            System.err.println();

            if (!jsap.messagePrinted()){
                for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                        .hasNext();) {
                    System.err.println("Error: " + errs.next());
                }
            }

            System.err.println();
            System.err.println("Usage: GrantSecurity ");
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);
        }

    }

}

/*
 * $Log: GrantSecurity.java,v $
 * Revision 1.11  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.10  2008/09/29 11:32:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.9  2008/09/29 05:14:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2008/07/16 08:45:04  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2008/06/13 09:35:29  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2008/06/11 10:07:15  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2008/04/29 10:56:00  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.3  2007/05/21 13:31:44  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.2  2007/04/27 10:51:37  dparhomenko
 * PTR#1804279 migrate VFCommons to maven from B302 directory
 *
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.13  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.12  2006/07/12 11:51:20  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.11  2006/06/02 07:21:38  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.10  2006/05/19 11:41:55  zahars
 * PTR#0144983 Configuration for utils updated
 *
 * Revision 1.9  2006/05/18 15:14:47  zahars
 * PTR#0144983 Added ability to read default properties from configuration file
 *
 * Revision 1.8  2006/05/18 14:53:50  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.7  2006/04/27 10:25:57  zahars
 * PTR#0144983 parameters fixed
 *
 * Revision 1.6  2006/04/26 15:23:25  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.5  2006/04/26 15:18:49  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.4  2006/04/25 13:13:59  ivgirts
 * PTR #1801730 parameters for jdbc connection added
 *
 * Revision 1.3  2006/04/24 11:37:06  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.2  2006/04/20 11:42:57  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/13 10:03:51  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/12 08:30:53  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/11 15:47:12  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/04/10 11:30:28  dparhomenko
 * PTR#0144983 security
 *
 */