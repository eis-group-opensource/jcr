/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.util.Map;
import java.util.Set;
import static com.exigen.cm.store.ContentStoreConstants.PROP_STORE_VALIDATION_ON;
import javax.jcr.RepositoryException;

import com.exigen.cm.database.ConnectionProvider;

/**
 * Base class for ContentStoreConfiguration
 */
public abstract class ContentStoreConfiguration {
    private String storeName;
    private ConnectionProvider connectionProvider;
    private Map<String, String> configuration;

    /**
     * Configures instance.
     * @param store
     * @param connectionProvider
     * @param configuration
     * @throws RepositoryException 
     */
    public void configure(String store, 
                            ConnectionProvider connectionProvider, 
                            Map<String, String> configuration,
                            String instanceName,
                            Object updateManager,
                            Object cacheManager) throws RepositoryException{
        configure(store, connectionProvider, configuration);
        configure(configuration, instanceName, updateManager, cacheManager);
    }    
    
    
    /**
     * Classes that require additional configuration should
     * override this method.
     * @param configuration
     * @param instanceName
     * @param updateManager
     * @param cacheManager
     */
    protected void configure(Map<String, String> configuration,
                            String instanceName,
                            Object updateManager,
                            Object cacheManager){
    }
    /**
     * Configures instance.
     * @param store
     * @param connectionProvider
     * @param configuration
     * @throws RepositoryException 
     */
    public void configure(String store, 
                            ConnectionProvider connectionProvider, 
                            Map<String, String> configuration) throws RepositoryException{
        storeName = store;
        this.connectionProvider = connectionProvider;
        this.configuration = configuration;
    }
    
    public abstract String getType();

    
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public String getStoreName() {
        return storeName;
    }


    /**
     * Checks provided <code>configuration</code> for existence of all required properties
     * and returns set of missed configuration items. 
     * Returns <code>null</code> if no required properties are missed
     * @param configuration
     * @return
     */
    public abstract Set<String> getMissedConfigurationItems(Map<String, String> configuration);
    
    
    /**
     * Returns <code>true</code> if store validation is allowed.
     * Returns <code>false</code> otherwise.
     * Store validation is disabled if store configuration contains
     * property:
     *  validate=false
     *  
     * example disabling validation of store default:
     * store.default.validate=false
     * @return
     */
    public boolean isValidationAllowed(){
        String val = configuration.get(PROP_STORE_VALIDATION_ON);
        return val == null ? true : Boolean.valueOf(val);
    }
}

/*
 * $Log: ContentStoreConfiguration.java,v $
 * Revision 1.3  2009/02/26 15:17:32  maksims
 * added support for property store.<store name>.validate=true|false
 *
 * Revision 1.2  2007/10/09 07:34:55  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:42  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/11/30 14:54:32  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.3  2006/08/14 16:18:36  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.2  2006/07/12 11:51:10  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 */