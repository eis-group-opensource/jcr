/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.utils.dbtest.DatabaseAbstractTest;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;

public class DatabaseTest {

	private static String Oracle = "com.exigen.cm.utils.dbtest.Oracle10DatabaseTest";
	private static String MSSQL = "com.exigen.cm.utils.dbtest.MSSQLDatabaseTest";
	
	
	/**
	 * @param args
	 * @throws JSAPException 
	 * @throws RepositoryException 
	 */
	public static void main(String[] args) throws JSAPException, RepositoryException {
        SimpleJSAP jsap = new SimpleJSAP(
                "DatabaseTest",
                "Test database",
                new Parameter[] {});
        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);
        
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()){
        	return;
        }
        if (config.success()) {
        	String configName = config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES);
        	Map<String, String> configuration = UtilsHelper.getRepositoryConfiguration(null,configName, true, false);
            configuration.put(Constants.PROPERTY_DEVELOPMENT_MODE, "false");
        	
        	DatabaseAbstractTest test = null;
        	String dialectProvider = configuration.get(Constants.PROPERTY_DATASOURCE_DIALECT_CLASSNAME); 
        	
        	if (dialectProvider.toLowerCase().indexOf("oracle")>0){
        		test = instantiate(Oracle, configuration);
        	} else if (dialectProvider.toLowerCase().indexOf("mssql")>0){
        		test = instantiate(MSSQL, configuration);
        	}  
        	
        	if (test == null){
        		System.err.println("Database "+dialectProvider+" not supported");
        	} else {
        		test.test();
        	}
        	
        } else {
            System.err.println();

            if (!jsap.messagePrinted()){
                for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                        .hasNext();) {
                    System.err.println("Error: " + errs.next());
                }
            }
            System.err.println();
            System.err.println("Usage: java " + CreateRepository.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);

        }
		

	}


	private static DatabaseAbstractTest instantiate(String className, Map<String, String> configuration) {
		// new Oracle10DatabaseTest(configuration);
		DatabaseAbstractTest result = null;
		try {
			Class cls = DatabaseAbstractTest.class.getClassLoader().loadClass(className);
			Constructor constructor = cls.getConstructor(new Class[]{Map.class});
			result = (DatabaseAbstractTest) constructor.newInstance(new Object[]{configuration});
		} catch (Exception exc){
			throw new RuntimeException("Error initializing DatabaseTest",exc);
		}
		return result;
	}

}
