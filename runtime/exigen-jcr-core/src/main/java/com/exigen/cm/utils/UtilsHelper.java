/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.DEFAULT_REPOSITORY_NAME;
import static com.exigen.cm.Constants.DEFAULT_ROOT_PASSWORD;
import static com.exigen.cm.Constants.DEFAULT_ROOT_USER_NAME;
import static com.exigen.cm.Constants.DEFAULT_WORKSPACE;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_DATA;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_NODETYPES;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_SECURITY;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_ZIP_BINARY;
import static com.exigen.cm.Constants.PROPERTY_IMPORT_DATA;
import static com.exigen.cm.Constants.PROPERTY_IMPORT_NODETYPES;
import static com.exigen.cm.Constants.PROPERTY_IMPORT_SECURITY;
import static com.exigen.cm.Constants.PROPERTY_IMPORT_ZIP_BINARY;
import static com.exigen.cm.Constants.PROPERTY_THREADS_ON;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.impl.ReflectHelper;
import com.exigen.cm.impl.RepositoryConfiguration;
import com.exigen.cm.impl.RepositoryImpl;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.QualifiedSwitch;
import com.martiansoftware.jsap.SimpleJSAP;

public class UtilsHelper {
    
    public final static String OPTION_REPOSITORY_USER = "repositoryUser";
    public final static String OPTION_REPOSITORY_PASSWORD = "password";
    public final static String OPTION_REPOSITORY_WORKSPACE = "workspace";
    
    public final static String DEFAULT_DATA_FILE_NAME = "data.xml";
    public final static String DEFAULT_NODETYPES_FILE_NAME = "nodetypes.xml";
    public final static String DEFAULT_SECURITY_FILE_NAME = "security.xml";
    public final static String DEFAULT_BINARY_ZIP_FILE_NAME = "binary.zip";
    
    /*public final static String OPTION_DEFAULT_STORE_TYPE = ContentStorePROPERTY_DEFAULT_STORE_TYPE;
    public final static String OPTION_DEFAULT_STORE_ROOTDIR = ContentStorePROPERTY_DEFAULT_STORE_ROOTDIR;
    public final static String OPTION_DEFAULT_STORE_TABLE = ContentStorePROPERTY_DEFAULT_STORE_TABLE;
    public final static String OPTION_TEXT_STORE_TYPE = ContentStorePROPERTY_TEXT_STORE_TYPE;
    public final static String OPTION_TEXT_STORE_ROOTDIR = ContentStorePROPERTY_TEXT_STORE_ROOTDIR;
    public final static String OPTION_TEXT_STORE_TABLE = ContentStorePROPERTY_TEXT_STORE_TABLE;
    */
    
    
	public static final String OPTION_REPOSITORY_PROPERTIES = "repositoryConfig";

    /*public static void configureDatabase(JSAPResult config, Map<String, String> repConfig, boolean addPrefix) {
        String prefix = addPrefix ? DEFAULT_REPOSITORY_NAME + "." : "" ;
        repConfig.put(prefix+PROPERTY_DATASOURCE_DIALECT_CLASSNAME, config.getString(PROPERTY_DATASOURCE_DIALECT_CLASSNAME));
        if (config.getString(PROPERTY_DATASOURCE_DRIVER_CLASSNAME) != null){
            repConfig.put(prefix+PROPERTY_DATASOURCE_DRIVER_CLASSNAME, config.getString(PROPERTY_DATASOURCE_DRIVER_CLASSNAME));
        }
        repConfig.put(prefix+PROPERTY_DATASOURCE_USER, config.getString(PROPERTY_DATASOURCE_USER));
        repConfig.put(prefix+PROPERTY_DATASOURCE_PASSWORD, config.getString(PROPERTY_DATASOURCE_PASSWORD));
        repConfig.put(prefix+PROPERTY_DATASOURCE_URL, config.getString(PROPERTY_DATASOURCE_URL));
        repConfig.put(prefix+CONFIG_MS_FTS_STOPWORD, "skip");
        if (config.getString(PROPERTY_ORACLE_CTXSYS_PASSWORD) != null){
            repConfig.put(prefix+PROPERTY_ORACLE_CTXSYS_PASSWORD, config.getString(PROPERTY_ORACLE_CTXSYS_PASSWORD));
        }
    }*/

    /*public static void addDatabaseParameters(SimpleJSAP jsap) throws JSAPException {
        jsap.registerParameter(new FlaggedOption(PROPERTY_DATASOURCE_DRIVER_CLASSNAME,JSAP.STRING_PARSER,JSAP.NO_DEFAULT, false, 'i', PROPERTY_DATASOURCE_DRIVER_CLASSNAME,"JDBC Driver class"));
        jsap.registerParameter(new FlaggedOption(PROPERTY_DATASOURCE_URL,JSAP.STRING_PARSER,JSAP.NO_DEFAULT, true, 'l', PROPERTY_DATASOURCE_URL,"JDBC URL to database"));
        jsap.registerParameter(new FlaggedOption(PROPERTY_DATASOURCE_USER,JSAP.STRING_PARSER,JSAP.NO_DEFAULT, true, 'r', PROPERTY_DATASOURCE_USER,"Database username"));
        jsap.registerParameter(new FlaggedOption(PROPERTY_DATASOURCE_PASSWORD,JSAP.STRING_PARSER,JSAP.NO_DEFAULT, true, 'a', PROPERTY_DATASOURCE_PASSWORD,"Database password"));
        jsap.registerParameter(new FlaggedOption(PROPERTY_DATASOURCE_DIALECT_CLASSNAME,JSAP.STRING_PARSER,JSAP.NO_DEFAULT, true, 'e', PROPERTY_DATASOURCE_DIALECT_CLASSNAME,"Database dialect"));        
    }*/
    
    /*public static void addDatabaseParametersForCreation(SimpleJSAP jsap) throws JSAPException {
        addDatabaseParameters(jsap);
        jsap.registerParameter(new FlaggedOption(PROPERTY_ORACLE_CTXSYS_PASSWORD,JSAP.STRING_PARSER,JSAP.NO_DEFAULT, false, JSAP.NO_SHORTFLAG, PROPERTY_ORACLE_CTXSYS_PASSWORD,"Oracle ctxsys password "));
    }*/
    
    public static void addRepositoryLoginParameters(SimpleJSAP jsap) throws JSAPException {
    	addRepositoryWorkspaceUser(jsap, true);
    	jsap.registerParameter(new FlaggedOption(OPTION_REPOSITORY_PASSWORD, JSAP.STRING_PARSER,DEFAULT_ROOT_PASSWORD, JSAP.NOT_REQUIRED, 'j', OPTION_REPOSITORY_PASSWORD,"Repository password"));
    }
    
    public static void addRepositoryWorkspaceUser(SimpleJSAP jsap, boolean useDefaultUserName) throws JSAPException{
        jsap.registerParameter(new FlaggedOption(OPTION_REPOSITORY_WORKSPACE, JSAP.STRING_PARSER, DEFAULT_WORKSPACE, JSAP.NOT_REQUIRED, 'w', OPTION_REPOSITORY_WORKSPACE, "Workspace name"));        
        jsap.registerParameter(new FlaggedOption(OPTION_REPOSITORY_USER, JSAP.STRING_PARSER,useDefaultUserName?DEFAULT_ROOT_USER_NAME:null, JSAP.NOT_REQUIRED, 'u', OPTION_REPOSITORY_USER,"Repository user name"));
    }
    
    public static void addRepositoryPropertiesParameter(SimpleJSAP jsap)  throws JSAPException {
    	jsap.registerParameter(new FlaggedOption(OPTION_REPOSITORY_PROPERTIES, JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_REPOSITORY_PROPERTIES, "Name of repository to be accessed"));
    }

    public static void addRepositoryBackupParameter(SimpleJSAP jsap)  throws JSAPException {
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_IMPORT_NODETYPES,JSAP.STRING_PARSER, "backup-nodetypes.xml", JSAP.NOT_REQUIRED, 'o', "nodetypes", "Node types file name"));
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_IMPORT_DATA,JSAP.STRING_PARSER, "backup-data.xml", JSAP.NOT_REQUIRED, 'd', "data","Data file name"));        
    }
    
    public static void addShortImportParameters(SimpleJSAP jsap) throws JSAPException {
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_IMPORT_DATA,JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f', PROPERTY_IMPORT_DATA,"Data file name"));
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_IMPORT_ZIP_BINARY, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, 'z', PROPERTY_IMPORT_ZIP_BINARY,"Zip file name for binary properties" ));
    }
    
    public static void addImportParameters(SimpleJSAP jsap) throws JSAPException {
        addShortImportParameters(jsap);
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_IMPORT_NODETYPES,JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', PROPERTY_IMPORT_NODETYPES, "Node types file name"));
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_IMPORT_SECURITY,JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'z', PROPERTY_IMPORT_SECURITY,"Security file name"));        
    }
    
    public static void addExportParameters(SimpleJSAP jsap) throws JSAPException {
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_EXPORT_DATA,JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'f', PROPERTY_EXPORT_DATA,"Data file name"));
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_EXPORT_NODETYPES,JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'o', PROPERTY_EXPORT_NODETYPES, "Node types file name"));
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_EXPORT_SECURITY,JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 'y', PROPERTY_EXPORT_SECURITY,"Security file name"));        
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_EXPORT_ZIP_BINARY, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, 'z', PROPERTY_EXPORT_ZIP_BINARY,"Zip file name for binary properties" ));
    }
    
    /*public static void addStoreParameters(SimpleJSAP jsap)throws JSAPException {
    	jsap.registerParameter(new FlaggedOption(OPTION_DEFAULT_STORE_TYPE, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,OPTION_DEFAULT_STORE_TYPE, "default store type: db of file" ));
    	jsap.registerParameter(new FlaggedOption(OPTION_TEXT_STORE_TYPE, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,OPTION_TEXT_STORE_TYPE, "text store type: db of file" ));
    	jsap.registerParameter(new FlaggedOption(OPTION_DEFAULT_STORE_ROOTDIR, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,OPTION_DEFAULT_STORE_ROOTDIR, "default store root dir (for file type)" ));
    	jsap.registerParameter(new FlaggedOption(OPTION_TEXT_STORE_ROOTDIR, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,OPTION_TEXT_STORE_ROOTDIR, "text store root dir (for file type)" ));
    	jsap.registerParameter(new FlaggedOption(OPTION_DEFAULT_STORE_TABLE, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,OPTION_DEFAULT_STORE_TABLE, "default store table (for db type)" ));
    	jsap.registerParameter(new FlaggedOption(OPTION_TEXT_STORE_TABLE, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG,OPTION_TEXT_STORE_TABLE, "text store table (for db type)" ));
    }*/
    
    /*public static void configureStore(JSAPResult config, Map<String, String> repConfig, boolean addPrefix) {
        String prefix = addPrefix ? DEFAULT_REPOSITORY_NAME + PROPERTY_DELIMITER : "" ;
        
        String defStoreType = config.getString(OPTION_DEFAULT_STORE_TYPE);
        if (!JCRHelper.isEmpty(defStoreType)){
            repConfig.put(prefix+ContentStorePROPERTY_DEFAULT_STORE_TYPE, defStoreType);
        }
        String textStoreType = config.getString(OPTION_TEXT_STORE_TYPE);
        if (!JCRHelper.isEmpty(textStoreType)){
            repConfig.put(prefix+ContentStorePROPERTY_TEXT_STORE_TYPE, textStoreType);
        }
        String defRootDir = config.getString(OPTION_DEFAULT_STORE_ROOTDIR);
        if (!JCRHelper.isEmpty(defRootDir)){
            repConfig.put(prefix+ContentStorePROPERTY_DEFAULT_STORE_ROOTDIR, defRootDir);
        }
        String textRootDir = config.getString(OPTION_TEXT_STORE_ROOTDIR);
        if (!JCRHelper.isEmpty(textRootDir)){
            repConfig.put(prefix+ContentStorePROPERTY_TEXT_STORE_ROOTDIR, textRootDir);
        }
        String defStoreTable = config.getString(OPTION_DEFAULT_STORE_TABLE);
        if (!JCRHelper.isEmpty(defStoreTable)){
        	repConfig.put(prefix+ContentStorePROPERTY_DEFAULT_STORE_TABLE,defStoreTable);
        }
        String textStoreTable = config.getString(OPTION_TEXT_STORE_TABLE);
        if (!JCRHelper.isEmpty(textStoreTable)){
        	repConfig.put(prefix+ContentStorePROPERTY_TEXT_STORE_TABLE,textStoreTable);
        }
        
        
    }*/
    
    public static final Map<String, String> getRepositoryConfiguration(RepositoryConfiguration repConfig,String configName, boolean disableThreads, Boolean allowDrop) throws RepositoryException{
    	if (repConfig == null){
    		repConfig = new RepositoryConfiguration();
    	}
        if (configName == null){
        	configName = DEFAULT_REPOSITORY_NAME;
        }
        
        
        Map<String, String> config = repConfig.getRepositoryConfiguration(configName);
        if (!config.containsKey(Constants.CONFIG_MS_FTS_STOPWORD)){
        	config.put(Constants.CONFIG_MS_FTS_STOPWORD,"skip");
        }
        if (disableThreads){
        	config.put(PROPERTY_THREADS_ON, "false");
        } else {
        	config.put(PROPERTY_THREADS_ON, "true");
        }

        if (allowDrop != null){
            config.put(Constants.PROPERTY_DEVELOPMENT_MODE, allowDrop?"true":"false");
            config.put(Constants.PROPERTY_DATASOURCE_DROP_CREATE, allowDrop?"true":"false");
        }
        return config;
    }
    
    public static RepositoryImpl getRepository(RepositoryConfiguration repConfig,JSAPResult config, Boolean allowDrop, Boolean allowThreads) throws RepositoryException{
    	if (repConfig == null){
    		repConfig = new RepositoryConfiguration();
    	}
        Map<String, String> configuration = UtilsHelper.getRepositoryConfiguration(repConfig, config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES), true, allowDrop);
        RepositoryProvider provider = RepositoryProvider.getInstance();
        if (allowThreads != null){
            configuration.put(Constants.PROPERTY_THREADS_ON, allowThreads?"true":"false");
        }
        provider.configure(DEFAULT_REPOSITORY_NAME, configuration);
        RepositoryImpl repository = (RepositoryImpl) provider.getRepository();
        return repository;
    }
    

	static Method getCheckStaticTablesMethod() throws RepositoryException {
		Method method = ReflectHelper.getMethod(RepositoryImpl.class, "checkStaticTables", new Class[]{Map.class, List.class, DatabaseConnection.class});
		return method;
	}

    
	public static void configureRepository(Map<String, String> config, boolean allowDrop, boolean allowThreads){
        config.put(Constants.PROPERTY_DEVELOPMENT_MODE, allowDrop ? "true":"false");
        config.put(Constants.PROPERTY_THREADS_ON, allowThreads ? "true":"false");
        config.put(Constants.PROPERTY_CMD_EXTRACTOR_ON, allowThreads ? "true":"false");


	}
	
	

}


/*
 * $Log: UtilsHelper.java,v $
 * Revision 1.6  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.5  2007/10/11 14:20:10  dparhomenko
 * Fix restore issues
 *
 * Revision 1.4  2007/08/29 12:55:30  dparhomenko
 * fix drop procedure for version without fts
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
 * Revision 1.30  2007/03/22 12:10:02  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.29  2006/12/21 09:12:57  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.28  2006/12/18 12:22:01  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.27  2006/10/09 10:18:41  zahars
 * PTR#1803093   Utilities (cmd) could be run from any directory
 *
 * Revision 1.26  2006/09/26 12:31:51  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.25  2006/09/08 11:43:33  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.24  2006/09/07 10:37:02  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.23  2006/08/28 11:25:48  zahars
 * PTR#0144986 DataImport utility introduced
 *
 * Revision 1.22  2006/08/24 14:54:22  zahars
 * PTR#0144986 export utility updated to support export binary to zip
 *
 * Revision 1.21  2006/08/21 13:36:14  dparhomenko
 * PTR#1802558 fix Utilities
 *
 * Revision 1.20  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.19  2006/07/12 11:51:20  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.18  2006/07/04 14:02:18  maksims
 * #1802356 Content Store Constants moved to ContentStoreConstants
 *
 * Revision 1.17  2006/06/02 07:21:38  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.16  2006/05/25 12:01:57  zahars
 * PTR#0144983 oracle.ctxsys.password configuration parameter introduced
 *
 * Revision 1.15  2006/05/19 11:41:55  zahars
 * PTR#0144983 Configuration for utils updated
 *
 * Revision 1.14  2006/05/19 09:07:51  zahars
 * PTR#0144983 Short option names updated
 *
 * Revision 1.13  2006/05/19 08:45:56  zahars
 * PTR#0144983 Short option names introduced
 *
 * Revision 1.12  2006/05/18 14:53:50  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.11  2006/05/17 14:53:15  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.10  2006/05/15 13:29:44  zahars
 * PTR#0144983 Constants from RepositoryProviderImpl are moved to Constants
 *
 * Revision 1.9  2006/05/03 13:11:40  zahars
 * PTR#0144983 Store properties introduced
 *
 * Revision 1.8  2006/04/28 13:55:52  dparhomenko
 * PTR#0144983 fix utilities
 *
 * Revision 1.7  2006/04/28 10:32:23  dparhomenko
 * PTR#0144983 build
 *
 * Revision 1.6  2006/04/27 10:25:57  zahars
 * PTR#0144983 parameters fixed
 *
 * Revision 1.5  2006/04/26 15:23:25  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.4  2006/04/26 15:18:49  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.3  2006/04/26 13:59:52  ivgirts
 * PTR #1801730 added repositoryParameters and exportImportParameters
 *
 * Revision 1.2  2006/04/20 11:42:57  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/12 08:49:28  maksims
 * #0144986 to Denis
 *
 * Revision 1.1  2006/04/11 15:47:12  dparhomenko
 * PTR#0144983 optimization
 *
 */