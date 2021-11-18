/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import javax.jcr.RepositoryException;

import com.exigen.cm.RepositoryProvider;
import com.exigen.cm.impl.RepositoryImpl;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.SimpleJSAP;

public abstract class AbstractUtil {

    protected SimpleJSAP jsapConfig;
	protected JSAPResult parameters;
	protected String repepositoryConfig;

	public void process(String[] args) throws JSAPException{
		jsapConfig = createJSAPConfiguration();
		parameters = jsapConfig.parse(args);
		
		repepositoryConfig = parameters.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES);
		
		boolean help = parameters.getBoolean("help");
		if (help){
			return;
		}
		if (!parameters.success()){
			printHelp();
		} else {
			boolean printHelp = false;
			try {
				printHelp = execute();
			} catch (Exception e) {
				//error = true;
				e.printStackTrace();
			}
			if (printHelp){
				printHelp();
			}
		}
	}

	abstract protected boolean execute() throws RepositoryException;

	protected void printHelp(){

        if (!jsapConfig.messagePrinted()){
            for (java.util.Iterator errs = parameters.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
        }
        System.err.println();
        System.err.println("Usage: java " + RepositoryRestore.class.getName());
        System.err.println("                " + jsapConfig.getUsage());
        System.err.println();
        System.err.println(jsapConfig.getHelp());
        System.exit(1);
	}

	abstract protected SimpleJSAP createJSAPConfiguration() throws JSAPException ;

	protected RepositoryImpl getRepository() throws RepositoryException{
		if (repepositoryConfig == null){
			return (RepositoryImpl) RepositoryProvider.getInstance().getRepository();
		} else {
			return (RepositoryImpl) RepositoryProvider.getInstance().getRepository(repepositoryConfig);
		}
	}
	
}
