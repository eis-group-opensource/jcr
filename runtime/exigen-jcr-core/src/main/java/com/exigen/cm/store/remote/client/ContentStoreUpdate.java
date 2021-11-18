/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.util.Map;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;

/**
 * Base class for Content Store Update implementations.
 */
public abstract class ContentStoreUpdate {

    private final String jcrInstanceName;
    private final TransportAdapterClient taClient;
    private final String storeName;
    private final Map<String, String> configuration;    
    
    protected ContentStoreUpdate(String store, String instanceName, TransportAdapterClient ta, Map<String, String> configuration){
        jcrInstanceName=instanceName;
        taClient = ta;
        storeName=store;
        this.configuration = configuration;
    }
    
    
    /**
     * Invokes Batch processing using connection provided with batch units filtering
     * by optional opFilter parameter.
     * @param connection
     * @param batch
     * @param opFilter
     */
    public abstract void process(DatabaseConnection connection, Batch batch, BatchOperation opFilter);

    
    
    public void process(DatabaseConnection connection, Batch batch){
        process(connection, batch, null);
    }


    /**
     * @return Returns the JCR Instance Name.
     */
    protected String getJcrInstanceName() {
        return jcrInstanceName;
    }


    /**
     * @return Returns the Content Store name.
     */
    protected String getStoreName() {
        return storeName;
    }


    /**
     * @return Returns the Transport Adapter Client instance.
     */
    protected TransportAdapterClient getTaClient() {
        return taClient;
    }

    /**
     * Returns configuration records for update
     * @return
     */
    protected Map<String, String> getConfiguration(){
        return configuration;
    }
}

/*
 * $Log: ContentStoreUpdate.java,v $
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