/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.DEFAULT_REPOSITORY_NAME;
import static com.exigen.cm.Constants.PROPERTY_IMPORT_DATA;
import static com.exigen.cm.Constants.PROPERTY_IMPORT_ZIP_BINARY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.impl.SessionImpl;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

/**
 * Import utility.
 * Allows to import nodes hierarchy into specified node.
 * 
 */
public class DataImport {
    private Repository repository = null;
    private SessionImpl session = null;
    
    private String username = null;
    private String password = null;
    private String workspace = null;
    private String path = null;
    
    
    private String dataFileName = UtilsHelper.DEFAULT_DATA_FILE_NAME;
    private String zipFileName = null;
    private Map<String, String> configuration;
    private JSAPResult config;
    
    private int uuidBehavior = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

    private static final String OPTIONS_UUID_BEHAVIOR = "UUIDBehavior";
    private static final String OPTIONS_IMPORT_PATH = "path";
    
    

    /**
     * Import data to specified node
     * @param args
     * @throws Exception
     */
    public static void main(String[] args)  throws Exception {
        
        SimpleJSAP jsap = new SimpleJSAP(
                        "DataImport",
                        "Data Import",
                        new Parameter[] {
                                        new FlaggedOption(OPTIONS_UUID_BEHAVIOR, JSAP.INTEGER_PARSER,""+ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,JSAP.NOT_REQUIRED,'d', "UUIDBehavior", "UUID behavior on import. See ImportUUIDBehavior.class for details " ),
                                        new FlaggedOption(OPTIONS_IMPORT_PATH, JSAP.STRING_PARSER, "/",JSAP.NOT_REQUIRED, 'p', "repositoryPath","Repository path to import")
                                });
        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        UtilsHelper.addRepositoryLoginParameters(jsap);
        UtilsHelper.addShortImportParameters(jsap);

        JSAPResult config = jsap.parse(args); 
        if (jsap.messagePrinted()){
        	return;
        }
        if (config.success()) {
            DataImport di = new DataImport();
            di.initParams(config);                
            
            di.configure();
        
            di.importF();
        } else {
            System.err.println();
            if (!jsap.messagePrinted()){
                for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                        .hasNext();) {
                    System.err.println("Error: " + errs.next());
                }
            }

            System.err.println();
            System.err.println("Usage: DataImport ");
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);                 
        }
    }    
    
    private void importF() {
            if (config.getBoolean(PROPERTY_IMPORT_ZIP_BINARY)){
                importNode(path, dataFileName,zipFileName);
            }
            else{
                importNode(path, dataFileName,null);
            }
    }


    private void configure(){
        try {
            configuration  = UtilsHelper.getRepositoryConfiguration(null,config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES), true, false);
            RepositoryProvider provider = RepositoryProvider.getInstance();
            provider.configure(DEFAULT_REPOSITORY_NAME, configuration);
            repository = provider.getRepository();

            Credentials credentials = new SimpleCredentials(username, password.toCharArray());
            
            session = (SessionImpl)repository.login(credentials, workspace);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);            
        }       
    }
    
    private void importNode(String path, String filePath, String zipPath) {
        FileInputStream fi = null;
        try {
            System.out.println("Importing data.");
            //
            File file = new File(filePath);
            File zFile = null;
            ZipFile zipFile = null;
            if (zipPath != null){
                // read binaries from zip
                zFile = new File(zipPath);
            }
            System.out.println("Importing data from "+file.getAbsolutePath()+".");
            if (zFile != null){
                System.out.println("Importing binary from " + zFile.getAbsolutePath() + ".");
                zipFile = new ZipFile(zFile);
            }
            fi = new FileInputStream(file);           
            session.importXML(path, fi, uuidBehavior, zipFile);
            session.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fi != null) {
                try {
                    fi.close();
                } catch (IOException e) {/* do nothing*/}
            }
        }
    }
    
    
    
     
    
    private void initParams(JSAPResult config) {
        this.config = config;
        username = config.getString(UtilsHelper.OPTION_REPOSITORY_USER);
        password = config.getString(UtilsHelper.OPTION_REPOSITORY_PASSWORD);
        workspace = config.getString(UtilsHelper.OPTION_REPOSITORY_WORKSPACE);
        
        path = config.getString(OPTIONS_IMPORT_PATH);
        uuidBehavior = config.getInt(OPTIONS_UUID_BEHAVIOR);
        
        dataFileName = config.getString(PROPERTY_IMPORT_DATA);
        zipFileName = config.getString(PROPERTY_IMPORT_ZIP_BINARY);
        
        dataFileName = dataFileName == null ? UtilsHelper.DEFAULT_DATA_FILE_NAME : dataFileName;
        zipFileName = zipFileName == null ? UtilsHelper.DEFAULT_BINARY_ZIP_FILE_NAME : zipFileName;
    }

}


/*
 * $Log: DataImport.java,v $
 * Revision 1.5  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.4  2007/06/13 12:26:52  dparhomenko
 * PTR#1804802 fix EWFRepository configurator
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
 * Revision 1.3  2006/09/25 11:18:33  zahars
 * PTR#1802683  session.save() added
 *
 * Revision 1.2  2006/09/08 11:43:33  dparhomenko
 * PTR#1802838 add multiple instance support
 *
 * Revision 1.1  2006/08/28 11:25:48  zahars
 * PTR#0144986 DataImport utility introduced
 *
 */
