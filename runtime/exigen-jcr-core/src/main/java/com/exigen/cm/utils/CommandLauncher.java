/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import static com.exigen.cm.Constants.DEFAULT_REPOSITORY_NAME;

import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.RepositoryProvider;
import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;

public class CommandLauncher {

	private Map<String, String> configuration;
	
	public static final String OPTION_SKIP_ALL = "skipAll";
	
	public static final String OPTION_ENFORCE_UNLOCK ="enforceUnlock";
	public static final String OPTION_EXTRACTOR ="extractor";
	public static final String OPTION_INDEXER ="indexer";
	public static final String OPTION_MIME_DETECTOR ="mimeDetector";
	public static final String OPTION_CLEAN ="delete";
	public static final String OPTION_FREE_RESERVED ="freeReserved";
	
    public static void main(String[] args)  throws Exception {
        
        SimpleJSAP jsap = new SimpleJSAP(
                        "CommandLauncher",
                        "Command Launcher",
                        new Parameter[] {
                                new Switch(OPTION_SKIP_ALL,'s', OPTION_SKIP_ALL, "Do not start all commands"),
                                new FlaggedOption(OPTION_CLEAN, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_CLEAN,"Enable or disable clean command"),
                                new FlaggedOption(OPTION_ENFORCE_UNLOCK, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_ENFORCE_UNLOCK,"Enable or disable enforce unlock command"),
                                new FlaggedOption(OPTION_INDEXER, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_INDEXER ,"Enable or disable indexer command"),
                                new FlaggedOption(OPTION_EXTRACTOR, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_EXTRACTOR,"Enable or disable extractor command"),
                                new FlaggedOption(OPTION_MIME_DETECTOR, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_MIME_DETECTOR,"Enable or disable mime type detector command"),
                                new FlaggedOption(OPTION_FREE_RESERVED, JSAP.STRING_PARSER, JSAP.NO_DEFAULT,JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, OPTION_FREE_RESERVED,"Enable or disable free reserved command")
                       });
        
        UtilsHelper.addRepositoryPropertiesParameter(jsap);

        JSAPResult config = jsap.parse(args); 
        if (jsap.messagePrinted()){
        	return;
        }
        if (config.success()) {
        	CommandLauncher de = new CommandLauncher();
            de.configure(config);
            de.launch();
        } else {
            System.err.println();
            if (!jsap.messagePrinted()){
                for (java.util.Iterator errs = config.getErrorMessageIterator(); errs
                        .hasNext();) {
                    System.err.println("Error: " + errs.next());
                }
            }

            System.err.println();
            System.err.println("Usage: CommandLauncher ");
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.err.println(jsap.getHelp());
            System.exit(1);                 
        }
    }


	private void launch() throws RepositoryException {
		System.out.println("Wait for termnination");
		
        RepositoryProvider provider = RepositoryProvider.getInstance();
        provider.configure(DEFAULT_REPOSITORY_NAME, configuration);
        provider.getRepository();
        while (true){
        	try {
				Thread.sleep(Integer.MAX_VALUE);
			} catch (InterruptedException e) {
				break;
			}
        }

		
	}

	private void configure(JSAPResult config) throws RepositoryException {
		this.configuration = UtilsHelper.getRepositoryConfiguration(null,config.getString(UtilsHelper.OPTION_REPOSITORY_PROPERTIES), false, false);
		
		if (config.getBoolean(OPTION_SKIP_ALL)){
			configuration.put(Constants.PROPERTY_CMD_ENFORCE_UNLOCK_ON, "false");                
			configuration.put(Constants.PROPERTY_CMD_EXTRACTOR_ON, "false");                
			configuration.put(Constants.PROPERTY_CMD_INDEXER_ON, "false");                
			configuration.put(Constants.PROPERTY_CMD_MIMEDETECTOR_ON, "false");                
			configuration.put(Constants.PROPERTY_CMD_CLEAN_ON, "false");                
			configuration.put(Constants.PROPERTY_CMD_FREE_RESERVED_ON, "false");                
		}
		
		checkParam(config, configuration, OPTION_CLEAN, Constants.PROPERTY_CMD_CLEAN_ON);
		checkParam(config, configuration, OPTION_ENFORCE_UNLOCK, Constants.PROPERTY_CMD_ENFORCE_UNLOCK_ON);
		checkParam(config, configuration, OPTION_EXTRACTOR, Constants.PROPERTY_CMD_EXTRACTOR_ON);
		checkParam(config, configuration, OPTION_INDEXER, Constants.PROPERTY_CMD_INDEXER_ON);
		checkParam(config, configuration, OPTION_MIME_DETECTOR, Constants.PROPERTY_CMD_MIMEDETECTOR_ON);
		checkParam(config, configuration, OPTION_FREE_RESERVED, Constants.PROPERTY_CMD_FREE_RESERVED_ON);
		
		
	}


	private void checkParam(JSAPResult config, Map<String, String> configuration2, String jsapName, String configName) {
		if (config.getString(jsapName) != null){
			boolean value = Boolean.parseBoolean(config.getString(jsapName));
			configuration.put(configName, Boolean.toString(value));
		}
	} 
}
