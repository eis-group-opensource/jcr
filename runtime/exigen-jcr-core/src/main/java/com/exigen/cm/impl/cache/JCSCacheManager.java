/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.cache;

import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.RepositoryImpl;

public class JCSCacheManager extends AbstractCacheManager{

    //private static final String CACHE_NODE = "CM_NODE";
    //private HashMap<String, JCS> caches = new HashMap<String, JCS>();;

    public void configure(RepositoryImpl repository, Map config) throws RepositoryException {
/*        try {
            CompositeCacheManager ccm = CompositeCacheManager.getUnconfiguredInstance(); 
            Properties props = new Properties();
            props.put("jcs.default","");
            props.put("jcs.default.cacheattributes","org.apache.jcs.engine.CompositeCacheAttributes");
            props.put("jcs.default.cacheattributes.MaxObjects","100");
            props.put("jcs.default.cacheattributes.MemoryCacheName","org.apache.jcs.engine.memory.lru.LRUMemoryCache");
            props.put("jcs.CM_NODE","");
            props.put("jcs.CM_NODE.cacheattributes","org.apache.jcs.engine.CompositeCacheAttributes");
            props.put("jcs.CM_NODE.cacheattributes.MaxObjects","100");
            props.put("jcs.CM_NODE.cacheattributes.MemoryCacheName","org.apache.jcs.engine.memory.lru.LRUMemoryCache");
            //props.put("","");
            //props.put("","");*/
            //props.load(/* load properties from some location defined by your app */); 
            /*ccm.configure(props);            
            
            //getCache(CACHE_NODE);
        } catch (Exception exc){
            exc.printStackTrace();
            throw new RepositoryException("Error configuring JCS cache", exc);
        }*/
    	throw new UnsupportedOperationException();
    }

    public void put(String entityName, Long entityId, RowMap row){
        /*CacheKey key = new CacheKey(entityName, entityId);
        try {
            getCache(entityName).put(key, row);
        } catch (CacheException e) {
            e.printStackTrace();
            //do nothing
        }*/
    	throw new UnsupportedOperationException();

    }
    
    //private synchronized JCS getCache(String entityName) throws CacheException {
        /*if (!caches.containsKey(entityName)){
            caches.put(entityName, JCS.getInstance(entityName));
        }
        return caches.get(entityName);
        */
    	//throw new UnsupportedOperationException();

    //}

    public void evict(String entityName, Long entityId){
        /*CacheKey key = new CacheKey(entityName, entityId);
        try {
            getCache(entityName).remove(key);
        } catch (CacheException e) {
            e.printStackTrace();
            //do nothing
        }*/
    	throw new UnsupportedOperationException();

    }
    
    public RowMap get(String entityName, Long entityId){
        /*CacheKey key = new CacheKey(entityName, entityId);
        try {
            //JCS cache = getCache(entityName);
            RowMap result = (RowMap) getCache(entityName).get(key);
            
            
            return result;
        } catch (CacheException e) {
            e.printStackTrace();
            //do nothing
        }
        return null;*/
    	throw new UnsupportedOperationException();

    }

    public void stats() {
       /* for(Iterator it = caches.values().iterator(); it.hasNext() ; ){
        }*/
    	throw new UnsupportedOperationException();

    }


    
    

}


/*
 * $Log: JCSCacheManager.java,v $
 * Revision 1.1  2007/04/26 09:00:15  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/10/30 15:03:45  dparhomenko
 * PTR#1803272 a lot of fixes
 *
 * Revision 1.1  2006/04/17 06:46:41  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/13 10:03:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/05 14:30:44  dparhomenko
 * PTR#0144983 optimization
 *
 * Revision 1.1  2006/03/27 14:27:23  dparhomenko
 * PTR#0144983 security
 *
 */