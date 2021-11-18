/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.AbstractRepositoryCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;

/**
 * Runs a Cache Cleaning process
 */
public class CacheCleaner extends AbstractRepositoryCommand {

    private static final Log log = LogFactory.getLog(CacheCleaner.class);
    
    /**
     * Property name for batch size
     */
    public static final String PROPERTY_BATCH_SIZE = "batchSize";    

 // Number of cache record it processes for each store.
    private int batchSize = 300;    
//    private ContentStoreProvider contentStoreProvider;
    
//    private String queryCheckReserved;    

    /**
     * Holds instance properties.
     */
    private final Map<String, String> properties;

    
    public CacheCleaner(){
        this(new HashMap<String, String>());
    }

    
    public CacheCleaner(Map<String, String> properties){
        super();
        this.properties= properties;
    }
    
    @Override
    public boolean init() {
        
        Object bsVal = properties.get(PROPERTY_BATCH_SIZE);
        if(bsVal != null)
            batchSize = Integer.parseInt((String)bsVal);

//        DatabaseDialect dialect = getConnectionProvider().getDialect();
//        queryCheckReserved = "SELECT DISTINCT "+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+ 
//                    " FROM "+ dialect.convertTableName(Constants.TABLE_CONTENT_SCHEDULE)+
//                    " WHERE " +Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" IN (";
        
        return true;
    }

    
    /**
     * @inheritDoc
     */
    public boolean execute() throws RepositoryException {
        DatabaseConnection connection = getConnectionProvider().createConnection();
        try{
            boolean fullBatch =  execute(connection);
            connection.commit();
            return fullBatch;
        }finally{
            connection.close();
        }
    }    
    
    public boolean execute(DatabaseConnection connection)
            throws RepositoryException {

        CSPCacheManager cacheManager = (CSPCacheManager)getStoreProvider().getCacheManager();
        log.debug("Cache cleaning executed!");
        for(String cacheName : cacheManager.getCacheNames()){
            CSPCache cache =  cacheManager.getCache(cacheName);
            Set<Long> contents = new HashSet<Long>();
            log.debug(MessageFormat.format("Cleaning cache: {0}", cacheName));
            
            Iterator<Long> expired = cache.getExpiredItemsIterator();
            if(!expired.hasNext()){
                log.debug(MessageFormat.format("Cache: {0} has no expired items", cacheName));                
            }
            
            try{
                while(expired.hasNext()){
                    Long expiredId = expired.next();
                    log.debug(MessageFormat.format("Item {0} is expired and added in cleaning list", expiredId));
                    contents.add(expiredId);
                    if(contents.size() > batchSize){// do flush
                        flush(connection, cache, contents);
                        contents = new HashSet<Long>();
                    }
                }
                flush(connection, cache, contents);
            }catch(Exception ex){
                String message = MessageFormat.format("Failed to clean cache for Content Store {0}", cacheName);
                log.error(message, ex);
            }
            
        }
        
        return false;
    }
    
    /**
     * Flushes collected changes.
     * @param connection
     * @param cache
     * @param contents
     * @throws Exception
     */
    protected void flush(DatabaseConnection connection, CSPCache cache, Set<Long> contents) throws Exception{
        log.debug(MessageFormat.format("flush called for {0} items", contents.size()));
        
        if(contents.size() == 0)
            return;
        
        
        DatabaseSelectAllStatement select = DatabaseTools.createSelectAllStatement(Constants.TABLE_CONTENT_SCHEDULE, false);
        select.setDistinct(true);
        select.addCondition(Conditions.in(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID, contents));
        select.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);
//      FIX ME Results should be distinct!
        
        select.execute(connection);
        while(select.hasNext()){// just touch those are in schedule 
            Long id = select.nextRow().getLong(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);
            log.debug(MessageFormat.format("Item {0} is still in update schedule", id));
            if(contents.remove(id)){ // remove if in a list -> fix for non-distinct result set
                cache.touch(id);
                log.debug(MessageFormat.format("Item {0} is removed from cleaning list", id));
            }
        }
        select.close();

        log.debug(MessageFormat.format("There are {0} items in cleaning list", contents.size()));        

        cache.begin();
        for(Long content:contents)
            cache.remove(content);
        cache.commit();
    }

    public String getDisplayableName() {
        return "Cache Cleaner";
    }

    public int getBatchSize() {
        return batchSize;
    }
}

/*
 * $Log: CacheCleaner.java,v $
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.11  2006/11/30 14:54:35  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.10  2006/11/09 13:44:57  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.9  2006/08/15 13:20:00  maksims
 * #1802414 Unneeded comments removed
 *
 * Revision 1.8  2006/08/15 08:38:05  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.7  2006/08/14 16:18:38  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.6  2006/08/02 11:42:31  maksims
 * #1802426 SQL Framework used to generate queries
 *
 * Revision 1.5  2006/08/01 11:25:59  maksims
 * #1802356 content last access time update added
 *
 * Revision 1.4  2006/07/28 15:49:09  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.3  2006/07/25 12:57:30  maksims
 * #1802425 store threads fixed to implement _Command
 *
 * Revision 1.2  2006/07/12 11:51:14  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/06 16:43:11  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 */