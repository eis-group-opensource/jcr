/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.DEFAULT_REPOSITORY_NAME;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_DATA;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_NODETYPES;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_SECURITY;
import static com.exigen.cm.Constants.PROPERTY_EXPORT_ZIP_BINARY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.jcr.Credentials;
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
import com.martiansoftware.jsap.Switch;

/**
 * Export Utility.
 * Allows to export nodetypes, nodes and security hierarchy
 * 
 */
public class DataExport {
    
    //private Repository repository = null;
    //private SessionImpl session = null;
    
    private String username = null;
    private String password = null;
    private String workspace = null;
    private String path = null;
    
    private boolean norecurse = false;
    private boolean skipbinary = false;
    private boolean formatXML = false;
    
    private String nodeTypesFileName = UtilsHelper.DEFAULT_NODETYPES_FILE_NAME;
    private String securityFileName = UtilsHelper.DEFAULT_SECURITY_FILE_NAME;
    private String dataFileName = UtilsHelper.DEFAULT_DATA_FILE_NAME;
    private String zipFileName = null;
	private Map<String, String> configuration;
	private JSAPResult config;
	private SessionImpl _session;
    
    private static final String OPTIONS_SKIPBINARY = "skipbinary";
    private static final String OPTIONS_NORECURSE = "norecurse";
    private static final String OPTIONS_EXPORT_PATH = "path";
    private static final String OPTIONS_FORMAT_XML = "format";
    
    /**
     * Default constructor 
     */
    public DataExport() {
    }     
    

    public static void main(String[] args)  throws Exception {
        
        SimpleJSAP jsap = new SimpleJSAP(
                        "DataExport",
                        "Data Export",
                        new Parameter[] {
                                new Switch(OPTIONS_NORECURSE,'n', OPTIONS_NORECURSE, "Do not recurse, only one level of the node"),
                                new Switch(OPTIONS_SKIPBINARY, 'b', OPTIONS_SKIPBINARY, "Binary data will be not exported"),
                                new FlaggedOption(OPTIONS_EXPORT_PATH, JSAP.STRING_PARSER, "/",JSAP.NOT_REQUIRED, 'p', "repositoryPath","repository path to export"),
                                new Switch(OPTIONS_FORMAT_XML, 'x', OPTIONS_FORMAT_XML, "Format XML"),
                                });
        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        UtilsHelper.addRepositoryLoginParameters(jsap);
        UtilsHelper.addExportParameters(jsap);

        JSAPResult config = jsap.parse(args);   
        
        if (config.success() && !config.getBoolean("help")) {
        	DataExport de = new DataExport();
            de.initParams(config);                
            
            de.configure();
        
            de.export(jsap);
        } else {
            System.err.println();
            if (!jsap.messagePrinted()){
                for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                        .hasNext();) {
                    System.err.println("Error: " + errs.next());
                }
            }

            if (!config.getBoolean("help")){
	            System.err.println();
	            System.err.println("Usage: DataExport ");
	            System.err.println("                " + jsap.getUsage());
	            System.err.println();
	            System.err.println(jsap.getHelp());
	            System.exit(1);
            }
        }
    }    
    
    private void export(SimpleJSAP jsap) {
        boolean exportFound = false;
        if (config.getBoolean(PROPERTY_EXPORT_DATA)) {
            if (config.getBoolean(PROPERTY_EXPORT_ZIP_BINARY))
                exportNode(path, dataFileName,zipFileName);
            else
                exportNode(path, dataFileName,null);
            exportFound = true;
        }
        
        if (config.getBoolean(PROPERTY_EXPORT_SECURITY)) {
            exportSecurity(path, securityFileName);
            exportFound = true;
        }
        
        if (config.getBoolean(PROPERTY_EXPORT_NODETYPES)) {
            exportNodeTypes(nodeTypesFileName);
            exportFound = true;
        }
        
        if (!exportFound){
            System.out.println("Pls. provide what to export - nodes, security or node types");
            
            System.err.println();
            System.err.println("Usage: java " + DataExport.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);

        }
	}

	private SessionImpl getSession(){
		if (_session == null){
	        try {
	            configuration  = UtilsHelper.getRepositoryConfiguration(null,config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES), true, false);
	            RepositoryProvider provider = RepositoryProvider.getInstance();
	            provider.configure(DEFAULT_REPOSITORY_NAME, configuration);
	            Repository repository = provider.getRepository();

	            Credentials credentials = new SimpleCredentials(username, password.toCharArray());
	            
	            _session = (SessionImpl)repository.login(credentials, workspace);
	        } catch (RepositoryException e) {
	            throw new RuntimeException(e);            
	        }    	
		}
        return _session;
	}
    
    
	public void configure(){
    }
    
    public void exportNode(String path, String filePath, String zipPath) {
        FileOutputStream fo = null;
        try {
        	getSession();
        	System.out.println("Exporting data.");
            //creating  file
            File file = new File(filePath);
            File zFile = null;
            System.out.println("Exporting data to "+file.getAbsolutePath()+".");
            if (zipPath != null){
                zFile = new File(zipPath);
                System.out.println("Exporting binary to " + zFile.getAbsolutePath() + ".");
            }
            fo = new FileOutputStream(file);           
            ZipOutputStream zout = null;
            if (zipPath != null){
                // copy binaries to zip
                zout = new ZipOutputStream(new FileOutputStream(zFile));
            }
            getSession().exportDocumentView(path, fo, skipbinary, zout, norecurse, formatXML);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {/* do nothing*/}
            }
        }
    }
    
    public void exportSecurity(String path, String filePath) {
        FileOutputStream fo = null;
        try {
        	getSession();
            System.out.println("Exporting security.");
            //creating  file
            File file = new File(filePath);
            fo = new FileOutputStream(file);           
            //exporting to file
            getSession().exportSecurity(path, fo, norecurse, formatXML);
            System.out.println("Security exported to "+file.getAbsolutePath()+".");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {/* do nothing*/}
            }
        }
    }
    
    public void exportNodeTypes(String filePath) {
        FileOutputStream fo = null;
        try {
        	getSession();
            System.out.println("Exporting nodetypes.");
            //creating  file
            File file = new File(filePath);
            fo = new FileOutputStream(file);           
            //exporting to file
            getSession().exportNodeTypes(fo, formatXML);
            System.out.println("Nodetpyes exported to "+file.getAbsolutePath()+".");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fo != null) {
                try {
                    fo.close();
                } catch (IOException e) {/* do nothing*/}
            }
        }
    }    
    
     
    
    protected void initParams(JSAPResult config) {
        this.config = config;
        username = config.getString(UtilsHelper.OPTION_REPOSITORY_USER);
        password = config.getString(UtilsHelper.OPTION_REPOSITORY_PASSWORD);
        workspace = config.getString(UtilsHelper.OPTION_REPOSITORY_WORKSPACE);
        
        path = config.getString(OPTIONS_EXPORT_PATH);
        norecurse = config.getBoolean(OPTIONS_NORECURSE);
        skipbinary = config.getBoolean(OPTIONS_SKIPBINARY);
        formatXML = config.getBoolean(OPTIONS_FORMAT_XML);
        
        dataFileName = config.getString(PROPERTY_EXPORT_DATA);
        nodeTypesFileName = config.getString(PROPERTY_EXPORT_NODETYPES);
        securityFileName = config.getString(PROPERTY_EXPORT_SECURITY);
        zipFileName = config.getString(PROPERTY_EXPORT_ZIP_BINARY);
        
        dataFileName = dataFileName == null ? UtilsHelper.DEFAULT_DATA_FILE_NAME : dataFileName;
        nodeTypesFileName = nodeTypesFileName == null ? UtilsHelper.DEFAULT_NODETYPES_FILE_NAME : nodeTypesFileName;
        securityFileName = securityFileName == null ? UtilsHelper.DEFAULT_SECURITY_FILE_NAME : securityFileName;
        zipFileName = zipFileName == null ? UtilsHelper.DEFAULT_BINARY_ZIP_FILE_NAME : zipFileName;
    }
}


/*
 * $Log: DataExport.java,v $
 * Revision 1.3  2007/11/06 12:42:48  dparhomenko
 * PTR#1805691
 *
 * Revision 1.2  2007/05/21 13:31:44  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.23  2006/12/15 15:39:57  dparhomenko
 * PTR#1803631 fix DataExport urility
 *
 * Revision 1.22  2006/10/06 08:59:01  zahars
 * PTR#1803043   XML format introduced
 *
 * Revision 1.21  2006/08/28 11:25:48  zahars
 * PTR#0144986 DataImport utility introduced
 *
 * Revision 1.20  2006/08/24 14:54:22  zahars
 * PTR#0144986 export utility updated to support export binary to zip
 *
 * Revision 1.19  2006/08/21 13:36:14  dparhomenko
 * PTR#1802558 fix Utilities
 *
 * Revision 1.18  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.17  2006/07/12 11:51:20  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.16  2006/06/02 07:21:38  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.15  2006/05/19 11:41:55  zahars
 * PTR#0144983 Configuration for utils updated
 *
 * Revision 1.13  2006/05/18 15:14:47  zahars
 * PTR#0144983 Added ability to read default properties from configuration file
 *
 * Revision 1.12  2006/05/18 14:53:50  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 * Revision 1.11  2006/05/03 13:11:40  zahars
 * PTR#0144983 Store properties introduced
 *
 * Revision 1.10  2006/04/28 15:35:00  ivgirts
 * PTR #1801730 utilities modified
 *
 * Revision 1.9  2006/04/28 13:55:52  dparhomenko
 * PTR#0144983 fix utilities
 *
 * Revision 1.8  2006/04/27 10:25:57  zahars
 * PTR#0144983 parameters fixed
 *
 * Revision 1.7  2006/04/26 15:23:25  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.6  2006/04/26 15:18:49  dparhomenko
 * PTR#0144983 utility
 *
 * Revision 1.5  2006/04/26 13:59:52  ivgirts
 * PTR #1801730 added repositoryParameters and exportImportParameters
 *
 * Revision 1.4  2006/04/25 13:13:59  ivgirts
 * PTR #1801730 parameters for jdbc connection added
 *
 * Revision 1.3  2006/04/24 16:04:52  ivgirts
 * PTR #1800998 skipbinary default true
 *
 * Revision 1.2  2006/04/21 15:21:53  ivgirts
 * PTR #1801730 added command line utilities
 *
 * Revision 1.1  2006/04/17 06:46:43  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/11 15:47:12  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.5  2006/04/10 11:30:28  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.4  2006/04/07 14:43:08  ivgirts
 * PTR #1801059 Authenticator is used for user authentication
 *
 * Revision 1.3  2006/03/03 10:33:17  dparhomenko
 * PTR#0144983 versioning support
 *
 * Revision 1.2  2006/02/27 16:15:42  ivgirts
 * PTR #1800998 Exporter utility added.
 *
 * Revision 1.1  2006/02/23 10:48:20  ivgirts
 * PTR #1800998 added export utility
 *
 */
