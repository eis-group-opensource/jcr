/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Responsible for CSPCache management.
 */
public class CSPCacheManager {
    
    private static final Log log = LogFactory.getLog(CSPCacheManager.class);

    private final Map<String, CSPCacheFactory> cacheFactories;
    
    public CSPCacheManager(){
        cacheFactories = new HashMap<String, CSPCacheFactory>();
    }
    
    
    /**
     * Adds registration for new CSPCache with name and configuration specified.
     * @param name
     * @param configuration
     */
    public void addCache(String name, Map<String, String> configuration){
        if(cacheFactories.containsKey(name)){
            String message = MessageFormat.format("CSPCache with name {0} already registered!", name);
            log.error(message);
            throw new RuntimeException(message);
        }

        
        if(log.isInfoEnabled()){
            String message = MessageFormat.format("Registering CSPCache with name {0} and parameters: {1}", 
                    name, configuration);
            log.info(message);
        }
        
        cacheFactories.put(name, new CSPCacheFactory(configuration));
    }
    
    
    /**
     * Returns new CSPCache instance configured for name specified or
     * throws RuntimeException if no cache with such name is registered.
     * @param name is a CSPCache name.
     * @return
     */
    public CSPCache getCache(String name){
        if(!cacheFactories.containsKey(name)){
            String message = MessageFormat.format("CSPCache with name {0} is not registered!", name);
            log.error(message);
            throw new RuntimeException(message);
        }
        
        return cacheFactories.get(name).create();
    }
    
    /**
     * Returns name of Caches registered in CSPCache Manager
     * @return
     */
    public Set<String> getCacheNames(){
        return cacheFactories.keySet();
    }
    
}

/*
 * $Log: CSPCacheManager.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/07/12 11:51:14  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:04:43  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */