/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.PROPERTY_DATASOURCE_JNDI_NAME;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_ZIP_BINARY;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.impl.ReflectHelper;
import com.exigen.cm.impl.RepositoryConfiguration;
import com.exigen.cm.impl.RepositoryImpl;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.QualifiedSwitch;
import com.martiansoftware.jsap.SimpleJSAP;

public class RepositoryRestore {

    private RepositoryImpl repository;
	
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        SimpleJSAP jsap = new SimpleJSAP(
                "RepositoryRestore",
                "Backup repository",
                new Parameter[] {});
        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        UtilsHelper.addRepositoryBackupParameter(jsap);
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_EXPORT_ZIP_BINARY,
				JSAP.BOOLEAN_PARSER, "true", JSAP.NOT_REQUIRED, 'z',
				PROPERTY_EXPORT_ZIP_BINARY,
				"Zip data file"));
        
        JSAPResult config = jsap.parse(args);

        boolean error = false;
        
        if (config.success() && !error && !config.getBoolean("help")) {
            String repConfig = config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES);
            try {
            	
            	boolean useZip = false;
            	if (config.getBoolean(PROPERTY_EXPORT_ZIP_BINARY)){
            		useZip = (Boolean) config.getObject(PROPERTY_EXPORT_ZIP_BINARY);
            	}
            	
            	String fName1 = config.getString(Constants.PROPERTY_IMPORT_DATA);
            	String fName2 = fName1;
            	if (useZip){
            		if (fName1.endsWith(".zip")){
            			fName2 = fName1.substring(0,fName1.length()-4)+".xml";
            		} else {
            			fName1 = fName1.substring(0,fName1.length()-4)+".zip";
            		}
            	}
            	
            	RepositoryRestore r = new RepositoryRestore();
            	
            	//String dataFile = config.getString(Constants.PROPERTY_IMPORT_DATA);
    	        String nodeTypes = config.getString(Constants.PROPERTY_IMPORT_NODETYPES);
    	        
    	        //check file existence
    	        //1.data
    	        JCRHelper.getInputStream(nodeTypes, true);
    	        
    	        InputStream fOut = JCRHelper.getInputStream(fName1, true);
    	        if (useZip){
    	        	ZipInputStream zipIn = new ZipInputStream(fOut);
    	        	zipIn.getNextEntry();
    	        	fOut = zipIn;
    	        }
    	        
    	        r.restore(repConfig, fOut, nodeTypes);
    	        
    	        fOut.close();
            	System.out.println("Done");
            	
            } catch (Exception exc){
                exc.printStackTrace();
                error = true;
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
            System.err.println("Usage: java " + RepositoryRestore.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);
        }
    }
    
    public void restore(String configName, InputStream data, String nodeTypes) throws RepositoryException{
        Map<String, String> config = initRepository(configName);
        config.put(Constants.PROPERTY_IMPORT_NODETYPES, nodeTypes);
        config.remove(Constants.PROPERTY_IMPORT_DATA);
        config.remove(Constants.PROPERTY_IMPORT_SECURITY);
        config.put(Constants.IMPORT_PREFIX+Constants.PROPERTY_DELIMITER+Constants.NODETYPE_SKIP_BUILTIN, "true");
        config.put(Constants.PROPERTY_DATASOURCE_SKIP_CHECK, "false");        
        config.put(Constants.PROPERTY_THREADS_ON, "false");
        config.put(Constants.PROPERTY_CMD_EXTRACTOR_ON, "false");
        
        DatabaseConnection conn = repository.getConnectionProvider().createConnection();

        try {
            Method method = ReflectHelper.getMethod(repository.getClass(), "dropRepository", new Class[]{Map.class, DatabaseConnection.class});
            try {
                method.invoke(repository, new Object[]{config, conn});
            } catch (Exception exc){
                //throw new RepositoryException("Error dropping repository", exc);
            }
            conn.commit();
        } finally {
            conn.close();
        }
        
        conn = repository.getConnectionProvider().createConnection();
        
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
        } catch (Exception exc){
            throw new RepositoryException("Error creating repository", exc);
        }
        
        
        //2.create repository
        try {
            method = ReflectHelper.getMethod(repository.getClass(), "initialize", new Class[]{config.getClass(), DataSource.class});
            config.put("SKIP_INIT", "true");
            try {
                method.invoke(repository, new Object[]{config, null});
            } catch (Exception exc){
                throw new RepositoryException("Error creating repository", exc);
            }
            conn.commit();
        } finally {
            conn.close();
        }
        
        //import data
        repository.restore(data);
        
	}
	
    private Map<String, String> initRepository(String configName) throws RepositoryException {
        Map<String, String> config = UtilsHelper.getRepositoryConfiguration(null,configName, true, true);
        
        config.put(Constants.CONFIG_MS_FTS_STOPWORD, "skip");
        config.put(Constants.PROPERTY_DEVELOPMENT_MODE, "true");
        config.put(Constants.PROPERTY_THREADS_ON, "false");
        config.put(Constants.PROPERTY_CMD_EXTRACTOR_ON, "false");
        
        (new RepositoryConfiguration()).checkFillConfiguration("default", config );
        
        String dialectName = (String) config.get(Constants.PROPERTY_DATASOURCE_DIALECT_CLASSNAME);
        //bind datasource
        Method method = ReflectHelper.getMethod(ConnectionProviderImpl.class, "bindDatasource", new Class[]{Map.class, String.class, String.class, InitialContext.class});
        try {
            method.invoke(null, new Object[]{config, "tempDS", dialectName, new InitialContext()});
            config.put(PROPERTY_DATASOURCE_JNDI_NAME, "tempDS");
        } catch (Exception exc){
            throw new RepositoryException("Error configuring datasource");
        }
        
        repository = createRepositoryObject();
        
        method = ReflectHelper.getMethod(repository.getClass(), "initVariables", new Class[]{Map.class, DataSource.class});
        try {
            method.invoke(repository, new Object[]{config, null});
        } catch (Exception exc){
            throw new RepositoryException("Error configuring Repository instance", exc);
        }
        return config;
    }


    public static RepositoryImpl createRepositoryObject() throws RepositoryException{
        //create repository
        Constructor constructor = ReflectHelper.getConstructor(RepositoryImpl.class, new Class[]{});
        try {
            return (RepositoryImpl) constructor.newInstance(new Object[]{});
        } catch (Exception e) {
            throw new RepositoryException("Error building Repository instance");
        }

    }
    

}


/*
 * $Log: RepositoryRestore.java,v $
 * Revision 1.8  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.7  2007/10/12 10:40:52  dparhomenko
 * Fix restore issues
 *
 * Revision 1.6  2007/10/11 14:20:10  dparhomenko
 * Fix restore issues
 *
 * Revision 1.5  2007/09/25 12:36:07  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2007/09/06 11:17:19  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2007/05/31 08:54:18  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.2  2007/05/21 13:31:44  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2007/03/22 12:10:02  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.4  2007/03/02 14:46:13  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.3  2007/02/02 15:40:16  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.2  2006/12/18 12:22:01  dparhomenko
 * PTR#1803217 fix errors
 *
 * Revision 1.1  2006/09/26 12:31:51  dparhomenko
 * PTR#1802402 add oracle tests
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