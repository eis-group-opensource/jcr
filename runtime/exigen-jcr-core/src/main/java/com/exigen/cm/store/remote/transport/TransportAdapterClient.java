/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.transport;

import java.io.OutputStream;
import java.util.Map;

import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;


/**
 * Transport adapter client interface used to transmit 
 * Content Store batches to server.
 * @author Maksims
 *
 */
public interface TransportAdapterClient {
    
    /**
     * Initializes instance. Configuration parameters are specific
     * to Transport Adapter implementation.
     * @param configuration
     */
    public void init(Map<String, String> configuration);
    
    /**
     * Submits batch to server side.
     * @param batch
     */
    public void submit(Batch batch);
    
    /**
     * Submits batch to server side applying operation filter if specified
     * i.e. only those batch units will be submitted which have operation
     * equal to provided operation filter.
     * 
     * @param batch
     * @param opFilter
     */
    public void submit(Batch batch, BatchOperation opFilter);    

    /**
     * Tests specified store alive.
     * @param storeName
     * @return
     */
    public boolean isAlive(String storeName);
    
    
    /**
     * Returns stream on content specified by content Id from store
     * specified by store Name.
     * 
     * FIXME Providing OutputStream here implies that CSPCache must be capable
     * to provide either File as a future content placeholder or a OutputStream itself
     * 
     * @param storeName
     * @param contentId
     * @param target is output stream to which content should be written
     */
    public void get(String storeName, Long jcrContentId, OutputStream target);
    
    
}

/*
 * $Log: TransportAdapterClient.java,v $
 * Revision 1.1  2007/04/26 08:59:53  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/07/28 15:49:13  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.2  2006/07/12 11:51:07  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:04:36  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */