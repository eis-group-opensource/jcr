/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.impl.RepositoryConfiguration;
import com.martiansoftware.jsap.DefaultSource;
import com.martiansoftware.jsap.Defaults;
import com.martiansoftware.jsap.ExceptionMap;
import com.martiansoftware.jsap.IDMap;

/**
 * Retrieves configuration for "default" repository 
 * that is used as source for default property values
 * for JSAP library. Either "repository-default.properties" should be in classpath
 * In order to determine which parameter
 * a value is associated with, each property key is first compared to each
 * parameter's unique ID.  Failing a
 * match, each parameter's long flag is checked, and finally the short flags
 * are checked.  
 * Use registerDefaultSource(DefaultSource) method to add as default value source
 * 
 */
@Deprecated
public class ConfigurationReader implements DefaultSource {

    
    Map <String, String> config = new HashMap<String, String>();
    
    /**
     * Constructor
     */
    public ConfigurationReader(){
        try {
            config = new RepositoryConfiguration().getRepositoryConfiguration(Constants.DEFAULT_REPOSITORY_NAME);
        } catch (RepositoryException re) {/* no configuration - OK */}    
    }
    
    /**
     * Returns a Defaults object based upon Configuration
     * properties and the specified IDMap.
     * In order to determine which parameter
     * a value is associated with, each property key is first compared to each
     * parameter's unique ID.  Failing a
     * match, each parameter's long flag is checked, and finally the short
     * flags are checked.  A
     * Configuration may contain a mix of IDs, long flags, and short
     * flags.
     * @param idMap the IDMap containing the current JSAP configuration.
     * @param exceptionMap the ExceptionMap object within which any encountered
     * exceptions will be thrown.
     * @return a Defaults object based upon this PropertyDefaultSource's
     * properties and the specified IDMap.
     */
    public Defaults getDefaults(IDMap idMap, ExceptionMap exceptionMap) {
        Defaults defaults = new Defaults();
            Set <String> keys = config.keySet();
            for (String key: keys){
                if (idMap.idExists(key)) {
                    defaults.addDefault(key,config.get(key));
                } else {
                    String paramID = idMap.getIDByLongFlag(key);
                    if (paramID != null) {
                        defaults.addDefault(
                            paramID,
                            config.get(key));
                    } else if (key.length() == 1) {
                        paramID = idMap.getIDByShortFlag(key.charAt(0));
                        if (paramID != null) {
                            defaults.addDefault(
                                paramID,
                                config.get(key));
                        } 
                    } 
                }
        }
        return (defaults);
    }


}


/*
 * $Log: ConfigurationReader.java,v $
 * Revision 1.1  2007/04/26 09:00:12  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/10/13 09:20:33  dparhomenko
 * PTR#0148476 fix exception text
 *
 * Revision 1.3  2006/08/21 11:03:21  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.2  2006/06/02 07:21:38  dparhomenko
 * PTR#1801955 add new security
 *
 * Revision 1.1  2006/05/17 14:53:15  zahars
 * PTR#0144983 Constants renamed (PROPERTY_ prefix added to connectivity properties).
 *
 */
