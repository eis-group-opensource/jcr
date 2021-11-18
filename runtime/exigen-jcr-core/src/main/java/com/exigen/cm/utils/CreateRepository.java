/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_JNDI_NAME;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.impl.ReflectHelper;
import com.exigen.cm.impl.RepositoryConfiguration;
import com.exigen.cm.impl.RepositoryImpl;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;

public class CreateRepository extends AbstractUtil{

	private static final String CREATE_WORKSPACE_FLAG = "createWorkspace";
	
    private RepositoryImpl repository;
    private RepositoryConfiguration repConfig = new RepositoryConfiguration();
	
    
    public CreateRepository(RepositoryConfiguration repConfig){
    	this.repConfig = repConfig;
    }
    
    public CreateRepository(){
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        (new CreateRepository()).process(args);
    }
    
	@Override
	protected SimpleJSAP createJSAPConfiguration() throws JSAPException {
        SimpleJSAP jsap = new SimpleJSAP(
                "CreateRepository",
                "Create or drop repository",
                new Parameter[] {
                        new Switch("drop", 'd', "drop" ,"Drop repository"),
                        new Switch("dropAll", JSAP.NO_SHORTFLAG, "dropAll" ,"Drop all database"),
                                new Switch("create", 'c', "create" ,"Create repository")
                    });
        
        jsap.registerParameter(new FlaggedOption(CREATE_WORKSPACE_FLAG, JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, CREATE_WORKSPACE_FLAG, "Create new Workspace"));
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        //new Switch("activateSecurity", 'y', "activateSecurity" ,"Activate security"),
        return jsap;
	}

	@Override
	protected boolean execute() throws RepositoryException {
       // boolean error = false;
        boolean create = parameters.getBoolean("create");
        boolean drop = parameters.getBoolean("drop");
        boolean dropAll = parameters.getBoolean("dropAll");
        String workspaceName = parameters.getString(CREATE_WORKSPACE_FLAG);
        
        if (!create && !drop && !dropAll && workspaceName == null){
            return true;
        }

        if (drop || dropAll){
        	
            dropRepository(drop, dropAll);
        }
        if (create){

        	//Map<String, String> cfg = 
        	createRepository();
            /*if (config.getBoolean("activateSecurity") && config.getString("securityData") == null){
                //activate security
                RepositoryImpl rep = createRepositoryObject();
                String user = config.getString(UtilsHelper.OPTION_REPOSITORY_USER);
                String password = config.getString(UtilsHelper.OPTION_REPOSITORY_PASSWORD);
                String workspace = config.getString(UtilsHelper.OPTION_REPOSITORY_WORKSPACE);
                rep.initialize(cfg);
                SessionImpl session = (SessionImpl) rep.login(new SimpleCredentials(user, password.toCharArray()),workspace);
                SessionSecurityManager manager = session.getSecurityManager();
                Node root = session.getRootNode();
                manager.setUserPermission(root, user, SecurityPermission.X_GRANT, true, true);
                for(SecurityPermission p: SecurityPermission.values()){
                    manager.setUserPermission(root, user, p, true, true);
                }
                throw new UnsupportedOperationException();
            }*/
            System.out.println("Repository created");
        }

        if (workspaceName != null){
        	createWorkspace(workspaceName);
        }
		return false;
	}


    private void createWorkspace(String workspaceName) throws RepositoryException {
        initRepository();
        repository.createWorkspace(workspaceName);
        System.out.println("Workspace "+workspaceName+" created");
	}


    private Map<String, String> initRepository() throws RepositoryException {
        Map<String, String> config = UtilsHelper.getRepositoryConfiguration(repConfig, repepositoryConfig, true, true);
        
        if (config.get(Constants.CONFIG_MS_FTS_STOPWORD) == null){
        	config.put(Constants.CONFIG_MS_FTS_STOPWORD, "skip");
        }
        UtilsHelper.configureRepository(config, true, false);
        config.put(Constants.PROPERTY_DATASOURCE_SKIP_CHECK, "false");        
        
        (new RepositoryConfiguration()).checkFillConfiguration("default", config );
        
        String dialectName = (String) config.get(Constants.PROPERTY_DATASOURCE_DIALECT_CLASSNAME);
        //bind datasource
        Method method = ReflectHelper.getMethod(ConnectionProviderImpl.class, "bindDatasource", new Class[]{Map.class, String.class, String.class, InitialContext.class});
        try {
            method.invoke(null, new Object[]{config, "tempDS", dialectName, new InitialContext()});
            config.put(PROPERTY_DATASOURCE_JNDI_NAME, "tempDS");
        } catch (java.lang.reflect.InvocationTargetException exc){
            throw new RepositoryException("Error configuring datasource", exc.getTargetException());
        } catch (IllegalArgumentException exc) {
        	throw new RepositoryException("Error configuring datasource", exc);
		} catch (IllegalAccessException exc) {
			throw new RepositoryException("Error configuring datasource", exc);
		} catch (NamingException e) {
			throw new RepositoryException("Error configuring datasource", e);
		}
        
        repository = ReflectHelper.createRepositoryObject();
        
        method = ReflectHelper.getMethod(repository.getClass(), "initVariables", new Class[]{Map.class, DataSource.class});
        try {
            method.invoke(repository, new Object[]{config, null});
        } catch (java.lang.reflect.InvocationTargetException exc){
            throw new RepositoryException("Error configuring Repository instance", exc.getTargetException());
        } catch (IllegalArgumentException exc) {
        	throw new RepositoryException("Error configuring Repository instance", exc);
		} catch (IllegalAccessException exc) {
			throw new RepositoryException("Error configuring Repository instance", exc);		
        } catch (Exception exc){
            throw new RepositoryException("Error configuring Repository instance", exc);
        }
        return config;
    }

    public void dropRepository(boolean drop, boolean dropAll)  throws RepositoryException {
        Map<String, String> config = initRepository();
        DatabaseConnection conn = repository.getConnectionProvider().createConnection();
        try {
        	String methodName = "dropRepository";
        	if (dropAll){
        		methodName = "dropDatabase";
        	}
            Method method = ReflectHelper.getMethod(repository.getClass(), methodName, new Class[]{Map.class, DatabaseConnection.class});
            try {
                method.invoke(repository, new Object[]{config, conn});
            } catch (java.lang.reflect.InvocationTargetException exc){
                throw new RepositoryException("Error dropping repository", exc.getTargetException());
            } catch (IllegalArgumentException exc) {
            	throw new RepositoryException("Error dropping repository", exc);
			} catch (IllegalAccessException exc) {
				throw new RepositoryException("Error dropping repository", exc);
			}
            conn.commit();
        } finally {
            conn.close();
        }
    }


    public Map<String, String> createRepository() throws RepositoryException {
        Map<String, String> config = initRepository();
        config.put(Constants.PROPERTY_DEVELOPMENT_MODE, "true");
        config.put(Constants.PROPERTY_DATASOURCE_DROP_CREATE, "false");
        config.put(Constants.PROPERTY_DATASOURCE_SKIP_CHECK, "false");
        DatabaseConnection conn = repository.getConnectionProvider().createConnection();
        //1.check repository
        
        Method method = UtilsHelper.getCheckStaticTablesMethod();
        try {
            Map dbTables = DatabaseTools.getDatabaseTables(conn);
            List<String> missingTables = new LinkedList<String>();
            Boolean isEmpty = (Boolean) method.invoke(repository, new Object[]{dbTables, missingTables, conn});
            if (!isEmpty){
                throw new RepositoryException("Database contains existing repository");
            }
        } catch (RepositoryException re){
            throw re;
        } catch (java.lang.reflect.InvocationTargetException exc){
            throw new RepositoryException("Error creating repository", exc.getTargetException());
        } catch (IllegalArgumentException exc) {
        	throw new RepositoryException("Error creating repository", exc);
		} catch (IllegalAccessException exc) {
			throw new RepositoryException("Error creating repository", exc);
		}
        
        
        //2.create repository
        try {
            method = ReflectHelper.getMethod(repository.getClass(), "initialize", new Class[]{config.getClass(),DataSource.class});
            try {
                method.invoke(repository, new Object[]{config, null});
            } catch (java.lang.reflect.InvocationTargetException exc){
                throw new RepositoryException("Error creating repository", exc.getTargetException());
            } catch (IllegalArgumentException exc) {
            	throw new RepositoryException("Error creating repository", exc);
    		} catch (IllegalAccessException exc) {
    			throw new RepositoryException("Error creating repository", exc);
    		}
            conn.commit();
        } finally {
            conn.close();
        }
        return config;
    }

}


/*
 * $Log: CreateRepository.java,v $
 * Revision 1.9  2009/02/23 14:30:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.8  2009/02/05 10:00:40  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.7  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.6  2007/06/06 07:51:22  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.5  2007/05/31 08:54:18  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.4  2007/05/21 13:31:44  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.3  2007/05/21 10:58:05  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.2  2007/04/27 10:51:37  dparhomenko
 * PTR#1804279 migrate VFCommons to maven from B302 directory
 *
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.39  2007/03/22 12:10:02  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.38  2007/03/02 09:32:08  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.37  2007/02/26 13:14:53  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.36  2007/02/26 09:45:54  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.35  2007/02/02 15:38:51  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.34  2007/01/31 08:35:50  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.33  2006/12/21 09:12:57  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.32  2006/12/18 12:22:01  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.31  2006/12/14 11:24:19  MZaharenkovs
 * PTR#1803272 RepositoryImpl.checkStaticTables has changed signature
 * this caused error when calling it through reflection. The call through reflection
 * is changed accordingly.
 *
 * Revision 1.30  2006/10/30 15:03:51  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.29  2006/10/06 08:38:55  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.28  2006/10/05 14:13:22  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.27  2006/09/08 13:53:45  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.26  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.25  2006/07/12 11:51:20  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.24  2006/07/10 12:06:32  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.23  2006/06/22 12:00:34  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.22  2006/06/02 07:21:38  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.21  2006/05/25 12:01:57  zahars
 * PTR#0144983 oracle.ctxsys.password configuration parameter introduced
 *
 * Revision 1.20  2006/05/19 08:45:56  zahars
 * PTR#0144983 Short option names introduced
 *
 * Revision 1.19  2006/05/18 15:14:47  zahars
 * PTR#0144983 Added ability to read default properties from configuration file
 *
 * Revision 1.18  2006/05/18 14:53:50  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.17  2006/05/17 14:53:15  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.16  2006/05/16 15:49:08  zahars
 * PTR#0144983 added option to read properties from <repository name>.properties
 *
 * Revision 1.15  2006/05/12 08:58:41  dparhomenko
 * PTR#0144983 Fix import export parameter description
 *
 * Revision 1.14  2006/05/10 08:04:09  dparhomenko
 * PTR#0144983 build 004
 *
 * Revision 1.13  2006/05/03 13:11:40  zahars
 * PTR#0144983 Store properties introduced
 *
 * Revision 1.12  2006/04/28 13:55:52  dparhomenko
 * PTR#0144983 fix utilities
 *
 * Revision 1.11  2006/04/27 10:25:57  zahars
 * PTR#0144983 parameters fixed
 *
 * Revision 1.10  2006/04/27 09:19:38  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.9  2006/04/26 15:23:25  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.8  2006/04/26 15:18:49  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.7  2006/04/26 12:16:55  zahars
 * PTR#0144983 introduced short property for workspace
 *
 * Revision 1.6  2006/04/24 11:37:06  dparhomenko
 * PTR#0144983 build procedure
 *
 * Revision 1.5  2006/04/21 15:21:53  ivgirts
 * PTR #1801730 added command line utilities
 *
 * Revision 1.4  2006/04/20 14:07:14  dparhomenko
 * PTR#0144983 bild procedure
 *
 * Revision 1.3  2006/04/20 12:38:16  dparhomenko
 * PTR#0144983 tests
 *
 * Revision 1.1  2006/04/17 06:46:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.7  2006/04/13 10:03:51  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/04/12 13:45:06  dparhomenko
 * PTR#0144983 MS FTS
 *
 * Revision 1.4  2006/04/12 08:54:38  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/12 08:46:59  maksims
 * #0144986 to Denis
 *
 * Revision 1.2  2006/04/12 08:30:53  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/11 15:47:12  dparhomenko
 * PTR#0144983 optimization
 *
 */