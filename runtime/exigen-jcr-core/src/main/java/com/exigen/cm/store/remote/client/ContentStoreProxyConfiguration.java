/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import static com.exigen.cm.Constants.PROPERTY_DELIMITER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exigen.cm.JCRHelper;
import com.exigen.cm.store.ContentStoreConfiguration;
import com.exigen.cm.store.ContentStoreConstants;

/**
 * Used to configure ContentStoreProxy instances.
 * Responsible for requesting JCR DB for additional configuration.
 */
public class ContentStoreProxyConfiguration extends ContentStoreConfiguration {

    private Map<String, String> cacheConfiguration;
    private String transportTypeName;
    private Map<String, String> transportConfiguration;
    private boolean isAsynchronousUpdateMode = false;
    private Map<String, String> updateConfiguration;
    

    private ContentStoreUpdateManager updateManager;
    private CSPCacheManager cacheManager;
    private String jcrInstanceName;
    
    
    /**
     * Property names of Content Store Proxy
     */
    public static final String PROP_CSP_CACHE="cache";
    
    public static final String PROP_CSP_TRANSPORT="transport";
    public static final String PROP_CSP_TRANSPORT_NAME="name";
    public static final String PROP_CSP_TRANSPORT_PARAM="param";

    
    public static final String PROP_CSP_ASYNC_MODE="asyncMode";
    public static final String PROP_CSP_MODE_PREFIX_ASYNC="async";
    public static final String PROP_CSP_MODE_PREFIX_SYNC="sync";    
    
    /**
     * Provides ability to configure store proxy without specifying
     * @param store
     * @param connectionProvider
     * @param configuration
     * @param params
     */
    public void configure(Map<String, String> configuration,
                          String jcrName, 
                          Object updateManager, 
                          Object cacheManager) {

        this.jcrInstanceName = jcrName;
        this.updateManager = (ContentStoreUpdateManager)updateManager;
        this.cacheManager = (CSPCacheManager)cacheManager;

        /*
         * Here we should have remote configuration already without remote prefix ...
         */
        parseCacheConfiguration(configuration);
        parseTransportConfiguration(configuration);
        parseUpdateConfiguration(configuration);
    }//*/
    
    @Override
    public Set<String> getMissedConfigurationItems(Map<String, String> configuration) {
        /* Transport */
        if(configuration.containsKey(PROP_CSP_TRANSPORT + PROPERTY_DELIMITER + PROP_CSP_TRANSPORT_NAME))
            return null;

        
        Set<String> missed = new HashSet<String>();
        missed.add(PROP_CSP_TRANSPORT + PROPERTY_DELIMITER + PROP_CSP_TRANSPORT_NAME);
        
        /* Cache and Update params are optional*/
        
        return missed;
    }       

    /**
     * Parses configuration to get:
     * - cacheConfiguration
     */
    protected void parseCacheConfiguration(Map<String, String> configuration){
        cacheConfiguration = JCRHelper.getPropertiesByPrefix(PROP_CSP_CACHE, configuration);
    }

    /**
     * Parses configuration to get:
     * - transportTypeName,
     * - transportConfiguration
     */
    protected void parseTransportConfiguration(Map<String, String> configuration){
        Map<String, String> tc = JCRHelper.getPropertiesByPrefix(PROP_CSP_TRANSPORT, configuration);

        transportTypeName = (String)tc.get(PROP_CSP_TRANSPORT_NAME);
        if(transportTypeName == null){
            throw new RuntimeException("Cannot initialize ContentStoreProxyBuilder without Transport Name specified. Ensure configuration contains: transport.name property value");
        }
        
        transportConfiguration = JCRHelper.getPropertiesByPrefix(PROP_CSP_TRANSPORT_PARAM, tc);
    }

    /**
     * Parses configuration to get:
     * - isSynchronous
     */
    protected void parseUpdateConfiguration(Map<String, String> configuration){
        isAsynchronousUpdateMode = new Boolean(configuration.get(PROP_CSP_ASYNC_MODE));
        String prefix = isAsynchronousUpdateMode ? PROP_CSP_MODE_PREFIX_ASYNC : PROP_CSP_MODE_PREFIX_SYNC;
        updateConfiguration =  JCRHelper.getPropertiesByPrefix(prefix, configuration);
    }

    
    @Override
    public String getType() {
        return ContentStoreConstants.STORE_TYPE_REMOTE;
    }

    /**
     * @return Returns the cacheConfiguration.
     */
    Map<String, String> getCacheConfiguration() {
        return cacheConfiguration;
    }

    /**
     * @return Returns the cacheManager.
     */
    CSPCacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * @return Returns the isSynchronousUpdateMode.
     */
    boolean isAsynchronousUpdateMode() {
        return isAsynchronousUpdateMode;
    }

    /**
     * @return Returns the jcrInstanceName.
     */
    String getJcrInstanceName() {
        return jcrInstanceName;
    }

    /**
     * @return Returns the transportConfiguration.
     */
    Map<String, String> getTransportConfiguration() {
        return transportConfiguration;
    }

    /**
     * @return Returns the transportTypeName.
     */
    String getTransportTypeName() {
        return transportTypeName;
    }

    /**
     * @return Returns the updateManager.
     */
    ContentStoreUpdateManager getUpdateManager() {
        return updateManager;
    }

    /**
     * Returns configuration for update
     * @return
     */
    Map<String,String> getUpdateConfiguration(){
        return updateConfiguration;
    }
}

/*
 * $Log: ContentStoreProxyConfiguration.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.7  2006/11/30 14:54:35  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.6  2006/08/14 16:18:38  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.5  2006/08/08 13:10:40  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.4  2006/07/28 15:49:09  maksims
 * #1802356 Content ID is changed to Long.
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