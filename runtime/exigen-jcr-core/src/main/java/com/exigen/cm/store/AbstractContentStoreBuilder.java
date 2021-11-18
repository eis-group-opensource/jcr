/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.store;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Implementation of this intefrace used to instantiate proper
 * instances of ContentStore.
 * 
 * @author Maksims
 * 
 * 1. method init() should be eliminated.
 *
 */
public abstract class AbstractContentStoreBuilder<T extends ContentStoreConfiguration> implements ContentStoreBuilder, ContentTracker{

    private static Log log = LogFactory.getLog(AbstractContentStoreBuilder.class);

    
//  List of tracked content
    private final Map<Long, Set<OpenContentData>> contentData = new HashMap<Long, Set<OpenContentData>>();
    private final Map<InputStream, OpenContentData> streamOpeners = new WeakHashMap<InputStream, OpenContentData>();
    
    
    /**
     * Holds configuration of specified type.
     */
    private T configuration;
    
    /**
     * Initializes factory. Set of properties allowed for this method
     * depends on factory implementation.
     * @param properties is a factory initialization properties.
     */
    @SuppressWarnings("unchecked")
    public void init(ContentStoreConfiguration config){
        try{
            configuration = (T)config;
            _init(configuration);
        }catch(ClassCastException cce){
            String message = MessageFormat.format("Content Store Builder {0} cannot be configured with configuration for type {1}.",
                    config.getStoreName(), configuration.getType());
            log.error(message, cce);
            throw new RuntimeException(message, cce);
        }
    }
        
    public ContentStore createStore(){
        if(configuration == null){
            throw new RuntimeException("");
        }
        
        return _createStore(configuration);
    }
    
    protected T getConfiguration(){
        return configuration;
    }
    
    /**
     * Default implementation. Returns always <code>null</code>.
     */
    public ContentStoreConfiguration newConfigurationInstance() {
        return null;
    }

    /**
     * Ovveride this method to add additional initialization logic
     * to a specific builder implementation.
     * @param configuration
     */
    protected void _init(T configuration){}
    
    
    /**
     * Creates initialized factory specific content store instance.
     * @return factory specific content store instance.
     */
    protected abstract ContentStore _createStore(T configuration);
    
    /**
     * Returns type name of stores given factory implementation provides.
     * @return
     */
    public abstract String getTypeName();
    

    /**
     * @inheritDoc
     */
    public void add(Long contentId, InputStream stream, Throwable opener){
        try{
            Trackable trackable = (Trackable)stream;
            trackable.setTracker(this);
            
            OpenContentData openContentData = new OpenContentData(contentId, opener);
    
            streamOpeners.put(stream, openContentData);
            
            synchronized(contentData){
                Set<OpenContentData> opens = contentData.get(contentId);
                if(opens == null){
                    opens = new HashSet<OpenContentData>();
                    contentData.put(contentId, opens);
                }
    
                opens.add(openContentData);
            }
        }catch(Exception ex){
//          Normally shouldn't occur because all stream to be passed in here expected implement it ...
            log.warn("Cannot add Content Stream to tracker because stream doesn't implement Trackable interface", ex);
        }
    }

    /**
     * @inheritDoc
     */
    public void remove(InputStream stream) {
        OpenContentData data;
//        synchronized(streamOpeners){
            data = streamOpeners.remove(stream);
            if(data == null)
                return;
//        }
        
        synchronized(contentData){      
            Set<OpenContentData> opens = contentData.get(data.contentId);
            opens.remove(data);
            if(opens.size()==0)
                contentData.remove(data.contentId);
        }
    }
    
    /**
     * @inheritDoc
     */
    public Set<Throwable> getContentOpeners(Long contentId){
        Set<OpenContentData> opens = contentData.get(contentId);
        if(opens == null)
            return null;
        
        Set<Throwable> openers = new HashSet<Throwable>();
        for(OpenContentData data:opens)
            openers.add(data.opener);
        
        return openers;
    }
    

    /**
     * Holds data on opened content.
     */
    private class OpenContentData{
        public final Throwable opener;
        public final Long contentId;

        public OpenContentData(Long contentId, Throwable opener){
            this.opener=opener;
            this.contentId=contentId;
        }
    }
}

/*
 * $Log: AbstractContentStoreBuilder.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/12/01 15:52:50  maksims
 * #0149528 AsbtractContentStoreBuilder renamed to AbstractContentStoreBuilder
 *
 * Revision 1.3  2006/11/09 15:44:20  maksims
 * #1801897 Centera Content Store added
 *
 * Revision 1.2  2006/09/28 09:19:37  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.1  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 */