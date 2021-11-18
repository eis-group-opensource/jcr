/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import com.exigen.cm.store.AbstractContentStoreBuilder;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConfiguration;
import com.exigen.cm.store.ContentStoreConstants;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;
import com.exigen.cm.store.remote.transport.TransportAdapters;

/**
 * Responsible for building ContentStoreProxy instances.
 */
public class ContentStoreProxyBuilder extends AbstractContentStoreBuilder<ContentStoreProxyConfiguration> {

//    private static Log log = LogFactory.getLog(ContentStoreProxyBuilder.class);
    
//    public static String TYPE="remote";
    
    /*
     * Holds configuration for Content Store Proxies.
     */
//    private ContentStoreProxyConfiguration configuration;
    
    private TransportAdapterClient taClient = null;
    private CSPCacheManager cacheManager = null;
    private ContentStoreUpdate update = null;
    private String storeName = null;
    
    /**
     * Initializes instance.
     */
    protected void _init(ContentStoreProxyConfiguration config) {
        storeName = config.getStoreName();
        cacheManager = config.getCacheManager();
        cacheManager.addCache(storeName, config.getCacheConfiguration());
        
        taClient = TransportAdapters.newClient(config.getTransportTypeName(), config.getTransportConfiguration());

        update = config.getUpdateManager().newContentStoreUpdate(storeName, taClient, config.isAsynchronousUpdateMode(), config.getUpdateConfiguration());        
    }

    /**
     * Creates instance of ContentStoreProxy 
     */
    protected ContentStore _createStore(ContentStoreProxyConfiguration config) {
        
        ContentStoreProxy store = new ContentStoreProxy(config,
                                                        storeName, 
                                                        cacheManager.getCache(storeName),
                                                        update, taClient);

        store.setContentTracker(this);
        return store;
    }

    /**
     * Returns type of this ContentStoreBuilder as remote.
     */
    public String getTypeName() {
        return ContentStoreConstants.STORE_TYPE_REMOTE;
    }

    @Override
    public ContentStoreConfiguration newConfigurationInstance() {
        return new ContentStoreProxyConfiguration();
    }
    
}

/*
 * $Log: ContentStoreProxyBuilder.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/12/01 15:52:53  maksims
 * #0149528 AsbtractContentStoreBuilder renamed to AbstractContentStoreBuilder
 *
 * Revision 1.5  2006/11/30 14:54:35  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.4  2006/09/28 09:19:38  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.3  2006/07/06 16:43:11  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 * Revision 1.2  2006/07/06 08:22:45  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.1  2006/07/04 14:04:43  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */