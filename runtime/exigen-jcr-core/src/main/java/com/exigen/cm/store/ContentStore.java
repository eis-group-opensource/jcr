/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.InputStream;
import java.util.Map;

import com.exigen.cm.database.DatabaseConnection;


/**
 * Represents single content store. Implementation may vary depending
 * on underlying storage.
 * Implementation of this interface aren't required to be thread-safe. 
 * @author Maksims
 * 
 *  As a sufficient solution (for now) we suggest the following:
 *  	4.1. method commitPut() savea all content properly but saving operation could be reversed 
 *  		(reversion is optional. if not implemented could lead to dead records)
 *      4.2. Method commit() should finalize the commit operation, i.e. if commitPut was called  already, it does commit for *remove*
 *           if not, does both (put and delete)
 *  5. Application that use ContentStore should use following protocol:
 *		5.1. Call begin()   on each store involved
 *		5.2. do store operations (put/get/delete)
 *		5.3. call commitPut() on each store involved
 *		5.4  commit own operations (application db etc.)
 *		5.5  call commit() on each store involved
 *		5.6  if 5.3 fails on any store or 5.4 fails call rollback on each store involved and rollback own operations 
 *		
 *     It looks like this protocol could guarantee application data consistance (all application data keep correct references to the store)
 *     but could lead to dead (undeleted) records in store which is considered unharmful and could be later cleaned up by the special utility.
 *	6. Introduce events for transaction operation (before/after). It could be one event for all operation.
 *	7. get() does not require transaction; put()/delete() should fail if transaction is not started
 *	
 *
 */
public interface ContentStore {
    /**
     * Creates new content record. Returns created content ID.
     * @param data content 
     * @param params is additional parameters needed to store data (store implementation specific).
     * @return created jcr content id.
     */
//    public String put(InputStream data, Map<String,String> params);
    public Long put(InputStream data, int length, Map<String,String> params);

    
    /**
     * Returns content data corresponding to passed identifier.
     * @param jcrContentId
     * @return input stream on content.
     * @deprecated
     */
    public InputStream get(String jcrContentId);
    public InputStream get(Long jcrContentId);    


    /**
     * Removes content specified by Content ID.
     * @param jcrContentId ID of content to be removed.
     * @deprecated
     */
    public void remove(String jcrContentId);

    
    /**
     * Removes content by JCR Content ID.
     * @param jcrContentId
     */
    public void remove(Long jcrContentId);

    // ************ Transactions support ******************
    /**
     * Begins content store transaction. 
     * @param connection 
     */
    public void begin(DatabaseConnection connection);
    
    /**
     * Commits content store transaction.
     */
    public void commit();

    /**
     * Commits insertions into content store.
     */
    public void commitPut();    
    
    /**
     * Rolls back content store transaction.
     *
     */
    public void rollback();
    
    
    /**
     * Returns <code>true</code> if there is active transaction.
     * @return true, if started
     */
    public boolean isTransactionStarted();
    
    
    // ********** Store Management support ****************
    
    /**
     * Ensures store is valid. Throws runtime exception if it doesn't.
     */
    public void validate();
    
    /**
     * Creates store.
     */
    public void create();
    

    /**
     * Drops store.
     */
    public void drop();
    
    
    /**
     * returns content length or throw UnsupportedOperationException in case isContentLengthSupported()
     * returns <code>false</code>
     * @param jcrContentId
     * @return length
     * @throws UnsupportedOperationException
     * @deprecated
     */
    public long getContentLength(String jcrContentId) throws UnsupportedOperationException;    
    public long getContentLength(Long jcrContentId) throws UnsupportedOperationException; 
    
}
/*
 * $Log: ContentStore.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.12  2006/08/08 13:10:35  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.11  2006/07/28 15:49:07  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.10  2006/07/17 14:47:43  zahars
 * PTR#0144986 Cleanup
 *
 * Revision 1.9  2006/07/06 08:22:12  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.8  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 * Revision 1.7  2006/05/03 15:48:42  maksims
 * #0144986 validate result is made cached
 *
 * Revision 1.6  2006/05/03 13:37:25  maksims
 * #0144986 ContentStore.begin method got DatabaseConnection param
 *
 * Revision 1.5  2006/05/02 11:44:28  maksims
 * #0144986 DB Content store type added
 *
 * Revision 1.4  2006/04/17 06:47:14  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.4  2006/04/12 12:18:43  maksims
 * #0144986 Seekable support added to File Store
 *
 */