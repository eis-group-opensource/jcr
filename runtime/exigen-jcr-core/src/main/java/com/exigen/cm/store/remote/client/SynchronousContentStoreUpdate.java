/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.util.Map;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;

/**
 * Implements Synchronous Content Store Update. responsible for
 * synchronous Batch specificed modifications sending on server side.
 */
class SynchronousContentStoreUpdate extends ContentStoreUpdate {

    
    SynchronousContentStoreUpdate(String store, String instanceName, TransportAdapterClient ta, Map<String, String> configuration) {
        super(store, instanceName, ta, configuration);
    }

    @Override
    public void process(DatabaseConnection connection, Batch batch,BatchOperation opFilter) {
        getTaClient().submit(batch, opFilter);
    }

}

/*
 * $Log: SynchronousContentStoreUpdate.java,v $
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