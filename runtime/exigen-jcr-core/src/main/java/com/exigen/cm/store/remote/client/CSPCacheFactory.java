/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.store.ContentStoreConstants;

/**
 * Creates configured CSPCache instances.
 */
public class CSPCacheFactory {
    
    private final File rootDir;
    private final int expirationSeconds;
    private final int bufferSize;
    private static final Log log = LogFactory.getLog(CSPCacheFactory.class);
    
    
    /**
     * Creates factory instance to produce configured CSPCache instances.
     * @param configuration
     */
    public CSPCacheFactory(Map<String, String> configuration){
        
        Object val = configuration.get(ContentStoreConstants.PROP_CACHE_BUFFER_SIZE);
        bufferSize = val == null ? 1024 : Integer.valueOf((String)val);
        
        val = configuration.get(ContentStoreConstants.PROP_CACHE_EXPIRATION);
        expirationSeconds = val == null ? -1 : Integer.valueOf((String)val);        
        
        val = configuration.get(ContentStoreConstants.PROP_CACHE_ROOT);
        rootDir = val == null ? new File(System.getProperty("java.io.tmpdir")) :
            new File((String)val);

        synchronized(CSPCacheFactory.class){
            if(!rootDir.exists() && !rootDir.mkdir()){
                String message = MessageFormat.format("Failed to create cache Root directory at {0}",
                rootDir.getAbsolutePath());
                log.error(message);
                throw new RuntimeException(message);
           }
        }
    }
    
    /**
     * Creates configured instance of CSPCache.
     * @return
     */
    public CSPCache create(){
        return new CSPCache(rootDir, expirationSeconds, bufferSize);
    }

}

/*
 * $Log: CSPCacheFactory.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/08/14 16:18:38  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.2  2006/07/12 11:51:14  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:04:43  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */