/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import static com.exigen.cm.store.ContentStoreProvider.DEFAULT_STORE_TYPE;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.JCRHelper;

/**
 * Used to configure Content Store Provider.
 */
public class ContentStoreProviderConfiguration {

    private Map<String, Map<String, String>> storesConfiguration = new HashMap<String, Map<String, String>>();

//  Holds default settings for default Content Store
    private Map<String, String> defaultStoreConfiguration;

//  Default mode in which store should operate
//    private boolean defaultStoreModeLocal = false;
    private boolean defaultStoreModeLocal = true;
    
    private static final Log log = LogFactory.getLog(ContentStoreProviderConfiguration.class);
    
    public ContentStoreProviderConfiguration(){
        defaultStoreConfiguration = new HashMap<String, String>();
        defaultStoreConfiguration.put(ContentStoreConstants.PROP_STORE_TYPE, DEFAULT_STORE_TYPE);
    }

    
    /**
     * Building store configuration using map key store prefixes
     * @param localConfiguration
     */
    public void configure(Map<String, String> localConfiguration){
        if(localConfiguration == null)
            return;

        Set<String> storeProps = localConfiguration.keySet();
        
        for(String storeProperty:storeProps){
            String storeName = storeProperty.substring(0, storeProperty.indexOf('.'));
            
            if(storesConfiguration.containsKey(storeName.toUpperCase()))
                continue;

            Map<String, String> storeConfig = JCRHelper.getPropertiesByPrefix(storeName, localConfiguration);
            configureStore(storeName, storeConfig);
        }
    }

    /**
     * Adds configuration for Content Store specified by storeName parameter.
     * @param storeName
     * @param localConfiguration
     */
    public void configureStore(String storeName, Map<String, String> localConfiguration){
        if(storesConfiguration.containsKey(storeName.toUpperCase())){
            String message = MessageFormat.format("Content Store {0} is already configured",
                    storeName);
            log.error(message);
            throw new RuntimeException(message);
        }

        
        storesConfiguration.put(storeName.toUpperCase(), localConfiguration);
    }

    /**
     * Adds configuration for Default Content Store.
     * @param localConfiguration
     */
    public void configureDefaultStore(Map<String, String> localConfiguration){
        configureStore(ContentStoreConstants.DEFAULT_STORE_NAME, localConfiguration);
    }
    
    /**
     * Returns configured if Default Store is configured
     * Returns default settings for Default Content Store if default store has not been configured.
     * @return
     */
    public Map<String, String> getDefaultStoreConfiguration(){
        Map<String, String> defStoreConfig = storesConfiguration.get(ContentStoreConstants.DEFAULT_STORE_NAME);
        return defStoreConfig == null ? defaultStoreConfiguration : defStoreConfig;
    }
    
    public Map<String, Map<String, String>> getStoresConfig(){
        return storesConfiguration;
    }
    
    public void setDefaultStoreModeLocal(boolean localMode){
        defaultStoreModeLocal = localMode;
    }
    
    
    public boolean isDefaultStoreModeLocal(){
        return defaultStoreModeLocal;
    }
}

/*
 * $Log: ContentStoreProviderConfiguration.java,v $
 * Revision 1.3  2009/03/10 09:10:59  maksims
 * *** empty log message ***
 *
 * Revision 1.2  2007/12/07 15:04:52  maksims
 * added capability to create content table if not existing opn initialization
 *
 * Revision 1.1  2007/04/26 08:59:42  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/08/21 11:20:12  maksims
 * #1801897 Default store mode local property added
 *
 * Revision 1.3  2006/08/08 13:10:35  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.2  2006/07/12 11:51:10  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 */