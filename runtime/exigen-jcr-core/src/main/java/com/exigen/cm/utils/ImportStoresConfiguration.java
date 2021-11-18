/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.StoreConfigurationImporter;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

/**
 * Imports Content Stores configuration into specified database.<br>
 * Pre-requisites:
 * <li> Database should exists
 * <li> Database should contain Content Stores Configuration tables CMCS_STORE and CMCS_STORE_TYPE
 * 
 */
public class ImportStoresConfiguration {

    private static Log log = LogFactory.getLog(ImportStoresConfiguration.class);
    private static final String OPTIONS_IMPORT_PATH = "propFile";

    public static void main(String[] args) throws Exception{
        SimpleJSAP jsap = new SimpleJSAP(
                "ImportStoresConfiguration",
                "Import Stores Configuration",
                new Parameter[] {
                        new FlaggedOption(OPTIONS_IMPORT_PATH, JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, 'p', "storesConfiguration","Content Stores configuration file to import.")
                        });

        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        JSAPResult config = jsap.parse(args);   
        if (!config.success())
            reportError(jsap, config);
        
        
        Map<String, String> configuration = UtilsHelper.getRepositoryConfiguration(null,config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES), true, false);    
        
        String propertiesPath = config.getString(OPTIONS_IMPORT_PATH);
        if(log.isInfoEnabled())
            log.info(MessageFormat.format("Importing Content Stores configuration from {0} file.",
                    propertiesPath == null ? "DEFAULT": propertiesPath));
        
        
        ConnectionProvider provider = new ConnectionProviderImpl();
        provider.configure(configuration, null);
        
        StoreConfigurationImporter importer = new StoreConfigurationImporter();

        DatabaseConnection connection = provider.createConnection();
        try{
//            if(propertiesPath == null)
//                importer.importConfiguration(connection);
//            else
            importer.importConfiguration(connection, propertiesPath);
            connection.commit();
        }catch(Exception ex){
            
        }finally{
            connection.close();
        }

    }

    
    private static void reportError(SimpleJSAP jsap, JSAPResult config){
        System.err.println();
        if (!jsap.messagePrinted()){
            for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                    .hasNext();) {
                System.err.println("Error: " + errs.next());
            }
        }

        System.err.print("Usage: ImportStoresConfiguration ");
        System.err.println(jsap.getUsage());
        System.err.println();
        System.err.println(jsap.getHelp());
        System.exit(1);                 
    }
}

/*
 * $Log: ImportStoresConfiguration.java,v $
 * Revision 1.4  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
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
 * Revision 1.4  2006/12/14 12:34:40  maksims
 * #1803520 Default content store file disabled
 *
 * Revision 1.3  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.2  2006/08/14 16:18:35  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.1  2006/08/02 13:38:43  maksims
 * #1802356 Content Store configuration import utility added
 *
 */