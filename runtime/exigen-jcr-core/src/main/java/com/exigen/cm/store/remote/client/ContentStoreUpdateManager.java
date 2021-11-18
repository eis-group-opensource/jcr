/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.util.HashMap;
import java.util.Map;

import com.exigen.cm.store.remote.transport.TransportAdapterClient;

/**
 * Manages Content Store Update implementations.
 */
public class ContentStoreUpdateManager {
    
    private final String jcrInstanceName;
    private final Map<String, TransportAdapterClient> asyncStoreTransport;
    
    public ContentStoreUpdateManager(String instanceName){
        jcrInstanceName = instanceName;
        
        asyncStoreTransport = new HashMap<String, TransportAdapterClient>();
    }
    
    
    /**
     * Creates new instance of Content Store Update either synchronous or not depending on
     * <code>synchronous</code> parameter. References to transport clients for asynchronous modes are
     * are stored for use by AsynchronousContentStoreUpdater.
     * @param storeName
     * @param taClient
     * @param synchronous
     * @return
     */
    public ContentStoreUpdate newContentStoreUpdate(String storeName, TransportAdapterClient taClient, boolean asynchronous, Map<String, String> updateConfiguration){
        ContentStoreUpdate update = asynchronous ? new AsynchronousContentStoreUpdate(storeName, jcrInstanceName, taClient, updateConfiguration):
            new SynchronousContentStoreUpdate(storeName,jcrInstanceName, taClient, updateConfiguration);

        if(asynchronous)
            asyncStoreTransport.put(storeName, taClient);

        return update;
    }
    

    /**
     * Returns transports for Content Store configured to run in asynchronous mode.
     * Used by AsynchronousContentStoreUpdater command.
     * @return
     */
    Map<String, TransportAdapterClient> getAsyncStoreTransport(){
        return asyncStoreTransport;
    }
}

/*
 * $Log: ContentStoreUpdateManager.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/07/12 11:51:14  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/06 08:22:45  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.1  2006/07/04 14:04:43  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */