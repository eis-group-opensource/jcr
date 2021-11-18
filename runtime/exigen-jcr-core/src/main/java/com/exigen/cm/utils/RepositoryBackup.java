/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.PROPERTY_EXPORT_ZIP_BINARY;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.RepositoryImpl;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.QualifiedSwitch;
import com.martiansoftware.jsap.SimpleJSAP;

public class RepositoryBackup {
 
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        SimpleJSAP jsap = new SimpleJSAP(
                "RepositoryBackup",
                "Backup repository",
                new Parameter[] {});
        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        UtilsHelper.addRepositoryBackupParameter(jsap);
        jsap.registerParameter(new QualifiedSwitch(PROPERTY_EXPORT_ZIP_BINARY,
				JSAP.BOOLEAN_PARSER, "true", JSAP.NOT_REQUIRED, 'z',
				PROPERTY_EXPORT_ZIP_BINARY,
				"Zip data file"));
        jsap.registerParameter(new QualifiedSwitch(Constants.PROPERTY_CMD_XML_FORMAT,
				JSAP.BOOLEAN_PARSER, "false", JSAP.NOT_REQUIRED, 'f',
				Constants.PROPERTY_CMD_XML_FORMAT,
				"Format XML"));
        JSAPResult config = jsap.parse(args);

        boolean error = false;
        
        if (config.success() && !error && !config.getBoolean("help")) {
            try {
            	boolean useZip = true;
            	if (config.getBoolean(PROPERTY_EXPORT_ZIP_BINARY)){
            		useZip = (Boolean) config.getObject(PROPERTY_EXPORT_ZIP_BINARY);
            	}
            	boolean identing = false;
            	
            	if (config.getBoolean(Constants.PROPERTY_CMD_XML_FORMAT)){
            		identing = (Boolean) config.getObject(Constants.PROPERTY_CMD_XML_FORMAT);
            	}
            	RepositoryImpl r = UtilsHelper.getRepository(null,config, false, false);

            	
            	String fName1 = config.getString(Constants.PROPERTY_IMPORT_DATA);
            	String fName2 = fName1;
            	if (useZip){
            		if (fName1.endsWith(".zip")){
            			fName2 = fName1.substring(0,fName1.length()-4)+".xml";
            		} else {
            			fName1 = fName1.substring(0,fName1.length()-4)+".zip";
            		}
            	}
            	
            	
    	        OutputStream fOut = new FileOutputStream(fName1);
    	        OutputStream fOut3 = null;
    	        
    	        if (useZip){
    	        	fOut3 = fOut;
    	        	//fOut = new GZIPOutputStream(fOut);
    	        	fOut = new ZipOutputStream(fOut);
    	        	((ZipOutputStream)fOut).putNextEntry(new ZipEntry(fName2));
    	        }
    	        FileOutputStream fOut2 = new FileOutputStream(config.getString(Constants.PROPERTY_IMPORT_NODETYPES));
    	        
    	        r.backup(fOut, fOut2, identing);
    	        
    	        
    	        fOut2.close();
    	        if (fOut3 != null){
    	        	((ZipOutputStream)fOut).closeEntry();
        	        fOut.close();
    	        	fOut3.close();
    	        } else {
        	        fOut.close();

    	        }
            	
            
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
            System.err.println("Usage: java " + RepositoryBackup.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);
        }
    }

}


/*
 * $Log: RepositoryBackup.java,v $
 * Revision 1.4  2007/10/12 10:40:52  dparhomenko
 * Fix restore issues
 *
 * Revision 1.3  2007/10/11 14:20:09  dparhomenko
 * Fix restore issues
 *
 * Revision 1.2  2007/05/21 13:31:44  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2007/03/22 12:10:02  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.2  2006/10/09 11:22:57  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.1  2006/09/26 12:31:52  dparhomenko
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