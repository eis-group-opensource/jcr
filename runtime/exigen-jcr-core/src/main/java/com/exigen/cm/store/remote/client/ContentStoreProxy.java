/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.AbstractContentStore;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.common.BatchUnit;
import com.exigen.cm.store.remote.common.BatchUnitInsert;
import com.exigen.cm.store.remote.common.BatchUnitRemove;
import com.exigen.cm.store.remote.common.ContentDataSource;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;

/**
 * @author Maksims
 *
 */
public class ContentStoreProxy extends AbstractContentStore {

    private static final Log log = LogFactory.getLog(ContentStoreProxy.class);
    
    private final CSPCache cache;
    private final ContentStoreUpdate update;
    private TransportAdapterClient taClient;
    private String storeName; // needed to produce Batches
    
    private List<BatchUnit> transactionalUnits = null;
    
    
    
    /**
     * FIX ME Content Store proxy should be properly initalized by providing:
     * - ConnectionProvider
     * - CSPCache
     * - ContentStoreUpdate
     * - TransportAdapterClient
     * - Store Name
     */
    ContentStoreProxy(ContentStoreProxyConfiguration configuration,
            String storeName,
            CSPCache cache, 
            ContentStoreUpdate update,
            TransportAdapterClient taClient){
        super(configuration);
        
        this.storeName = storeName;
        this.cache=cache;
        this.update = update;
        this.taClient = taClient;
    }


    @Override
    public InputStream get(Long jcrContentId) { 
        InputStream result = cache.get(jcrContentId);
        if(result == null){
            OutputStream target = cache.createTargetFor(jcrContentId);
            taClient.get(storeName, jcrContentId, target);
            result = cache.get(jcrContentId);
        }
        
        getContentTracker().add(jcrContentId, result, new Throwable());
        
        return result;
    }
    
    
    @Override
    public void remove(Long jcrContentId){
        ensureCanRemove(jcrContentId);
        transactionalUnits.add(new BatchUnitRemove(jcrContentId));
        cache.remove(jcrContentId);
    }


    @Override
    public void put(Long jcrContentId, InputStream data, int length, Map<String, String> params) {
        
        cache.put(jcrContentId, data);

        ContentDataSource source = new CachedContentDataSource(cache, jcrContentId);
        transactionalUnits.add(new BatchUnitInsert(jcrContentId, params, source, length));

//      Cannot return complete content data ... contentId can be issued by real store only
//        return new ContentData(null, source.getLength());
    }

    @Override
    protected void _begin(DatabaseConnection connection) {
//        Need to start Cache transaction here
        transactionalUnits = new ArrayList<BatchUnit>();
        cache.begin(); // start cache transaction.
    }
    
    @Override
    protected void _commit() {
        update.process(getTransactionConnection(), new Batch(storeName, transactionalUnits));
        cache.commit(); // commit cache transaction.
    }
    
    @Override
    protected void _commitPut() {
        update.process(getTransactionConnection(), new Batch(storeName, transactionalUnits), BatchOperation.INSERT);
        cache.commitPut(); // commitPut cache transaction.
    }

    @Override
    protected void _rollback() {
//        Rollback to be done!
//      Need to delete all mappings added with current transaction
        transactionalUnits = null;
        cache.rollback(); // start cache transaction.
    }
    
    
    
    @Override
    protected void _validate() {
        if(!taClient.isAlive(storeName)){
            String message = MessageFormat.format("Content store {0} is invalid",
                    storeName);
            log.error(message);
            throw new RuntimeException(message);
        }
    }
    
    @Override
    public void create() {
//        Remote Store cannot be created. It is supposed to use existing store infrastructure.
        log.info(MessageFormat.format("Store {0} is a remote proxy thus it cannot be created. To create this store invoke create() on a store referred by this proxy.", storeName));        
    }
    
    @Override
    protected void _drop() {
//        Does nothing. Hides UnsupportedOp Exception
        log.info(MessageFormat.format("Store {0} is a remote proxy thus it cannot be droped. To drop this store invoke drop() on a store referred by this proxy.", storeName));
    }
    
    /**
     * Cached content source implementation.
     * @author Maksims
     *
     */
    static class CachedContentDataSource implements ContentDataSource{

        private final CSPCache cache;
        private final Long contentId;
        
        CachedContentDataSource(CSPCache cache, Long contentId){
            this.cache = cache;
            this.contentId = contentId;

        }
        
        public Long getContentId() {
            return contentId;
        }

        public InputStream getData() {
            return cache.get(contentId);
        }

        public int getLength() {
            return (int)cache.getContentLength(contentId);
        }
        
        public void release() {
            throw new UnsupportedOperationException();
        }
        
    }

}

/*
 * $Log: ContentStoreProxy.java,v $
 * Revision 1.2  2007/12/07 15:04:02  maksims
 * drop and create methods overriden to allow deployment
 *
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/10/09 11:23:06  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.7  2006/09/28 09:19:38  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.6  2006/08/15 13:20:00  maksims
 * #1802414 Unneeded comments removed
 *
 * Revision 1.5  2006/08/08 13:10:40  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.4  2006/07/28 15:49:09  maksims
 * #1802356 Content ID is changed to Long.
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