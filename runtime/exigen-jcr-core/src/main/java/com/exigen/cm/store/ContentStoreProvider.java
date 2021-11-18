/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;

/**
 * Provides access to registered ContentStores by name.
 * @author Maksims
 */
public class ContentStoreProvider {

    private Map<String, ContentStoreBuilder> storeBuilders = new HashMap<String, ContentStoreBuilder>();

    private static final Log log = LogFactory.getLog(ContentStoreProvider.class);
    
    private ConnectionProvider connectionProvider = null;
    private String jcrInstanceName = null;
    
//  Store type property name
    public static final String DEFAULT_STORE_TYPE = ContentStoreConstants.STORE_TYPE_DB;
    
    private static enum STORE_STATUS {VALID, INVALID};
    
    
//  --------------- Remote Access Related lazy initialized fields ---------
//  made Objects to allow remote package separation
    private Object updateManager = null;
    private Object cspCacheManager = null;

    
    /*
     * Holds map of validates stores with their validation statuses.
     */
    private Map<String, STORE_STATUS> storeStatus = new HashMap<String, STORE_STATUS>();
    

    private static Map<String, String> STORE_TYPES = new HashMap<String, String>();

    private static Map<String, Class<ContentStoreBuilder>> storeBuilderClasses = new HashMap<String, Class<ContentStoreBuilder>>();
    static{
        STORE_TYPES.put(ContentStoreConstants.STORE_TYPE_FILE, "com.exigen.cm.store.file.FileContentStoreBuilder");
        STORE_TYPES.put(ContentStoreConstants.STORE_TYPE_DB, "com.exigen.cm.store.db.DBContentStoreBuilder");
        STORE_TYPES.put(ContentStoreConstants.STORE_TYPE_CENTERA, "com.exigen.cm.store.centera.CenteraContentStoreBuilder");
        STORE_TYPES.put(ContentStoreConstants.STORE_TYPE_REMOTE, "com.exigen.cm.store.remote.client.ContentStoreProxyBuilder");
    }

    /**
     * Creates not configured instance of Content Store Provider.
     * Use ContentStoreProvider#init() method to initialize such and instance.
     */
    public ContentStoreProvider(){}
    
    
    public ContentStoreProvider(Map<String, String> config, ConnectionProvider connectionProvider) throws RepositoryException{
        ContentStoreProviderConfiguration cspc = new ContentStoreProviderConfiguration();
        cspc.configure(config);
        init(connectionProvider, cspc);
    }

    
    /**
     * Configures Content Sotore Provider. Content Stores can be accessed
     * after this method is called.
     * @param connectionProvider
     * @param configuration
     * @param jcrInstanceName
     */
    public void init(ConnectionProvider connectionProvider, ContentStoreProviderConfiguration configuration, String jcrInstanceName) throws RepositoryException{
        setJCRInstanceName(jcrInstanceName);
        init(connectionProvider, configuration);        
    }    
    
    /**
     * Configures Content Store Provider instance with no JCR instance name specified.
     * @param connectionProvider
     * @param configuration
     */
    public void init(ConnectionProvider connectionProvider, ContentStoreProviderConfiguration configuration) throws RepositoryException{
        this.connectionProvider = connectionProvider;
        
        Map<String, Map<String, String>> storesConfig = configuration.getStoresConfig();

        DatabaseConnection connection = null;
        try{
            connection = connectionProvider.createConnection();
            loadConfigFromDB(connection, storesConfig);// merges provided config with JCR Stores configuration
        }finally{
            if(connection != null)
                connection.close();
        }

        for(String storeName: storesConfig.keySet())
            addStore(storeName, storesConfig.get(storeName), configuration.isDefaultStoreModeLocal());
        
        if(!storeBuilders.containsKey(ContentStoreConstants.DEFAULT_STORE_NAME))
            addStore(ContentStoreConstants.DEFAULT_STORE_NAME, configuration.getDefaultStoreConfiguration(), true);
    }


    
    /**
     * Builds full Content Stores configuration for given Repository
     * @param connection
     * @param storesConfig
     * @return
     */
    protected void loadConfigFromDB(DatabaseConnection connection, Map<String, Map<String, String>> storesConfig) throws RepositoryException{
//        PreparedStatement statement = null;
        try{
            /*
            select store.NAME storeName, 
                   this_.CONFIG typeConfig, 
                   store.CONFIG storeConfig, 
                   this_.NAME typeName
            from jcr.CMCS_STORE_TYPE this_, jcr.CMCS_STORE store 
            where  ( store.TYPE = this_.ID )              
             */
            final String[] columns = new String[]{
                    //"store."+
                    Constants.TABLE_STORE__NAME,
                    "STYPE."+Constants.TABLE_STORE_TYPE__CONFIG,
                    //"store."+
                    Constants.TABLE_STORE__CONFIG,
                    "STYPE."+Constants.TABLE_STORE_TYPE__NAME
            };
            
            
            DatabaseSelectAllStatement configData = DatabaseTools.createSelectAllStatement(Constants.TABLE_STORE, true);
            configData.setRootAlias("store");
            configData.addJoin(Constants.TABLE_STORE_TYPE, "STYPE", Constants.TABLE_STORE__TYPE, Constants.FIELD_ID, true);
            
            for(String column:columns)
                configData.addResultColumn(column);

            configData.execute(connection);
            
            
            
//            while(configData.next()){
            while(configData.hasNext()){                
                RowMap config = configData.nextRow();
                String storeName = config.getString(columns[0]);
                if(storeName == null){
                    String message = "Bad Content Store configuration in DB. NULL is returned as store name";
                    log.error(message);
                    throw new RuntimeException(message);
                }

                Properties typeConfig = new Properties();
                String data = config.getString(columns[1]);
                if(data != null){
                    InputStream typeConfigData = new ByteArrayInputStream(data.getBytes());
                    typeConfig.load(typeConfigData);
                }

                Properties storeConfig = new Properties(typeConfig);
                data = config.getString(columns[2]);//dialect.convertStringFromSQL(configData.getString(3));
                if(data != null){
                    InputStream storeConfigData = new ByteArrayInputStream(data.getBytes());
                    storeConfig.load(storeConfigData);
                }
                
                Map<String, String> jcrStoreConfig = new HashMap<String, String>();
                Enumeration props = storeConfig.propertyNames();
                while(props.hasMoreElements()){
                    String key = (String)props.nextElement();
                    jcrStoreConfig.put(key, storeConfig.getProperty(key));
                }

                
                Map<String, String> localStoreConfig = storesConfig.remove(storeName);
//              Check type match
//              Stores without store type cannot be imported in DB thus storeType from DB
//              always exists here .. if one overrides type in JCR local config
//              it should be same as declared in DB or all store configuration local part
//              should be redefined. For now this situation decided to be a FAILURE!
                String storeType = config.getString(columns[3]).toUpperCase();//dialect.convertStringFromSQL(configData.getString(4).toUpperCase());
                String localDeclaredType = localStoreConfig == null ? null : localStoreConfig.get(ContentStoreConstants.PROP_STORE_LOCAL_TYPE);
                if(localDeclaredType != null && !storeType.equalsIgnoreCase(localDeclaredType)){
                    String message = MessageFormat.format("Locally declared Content Store type {0} doesn't match to Content Store Type {1} declared in Repository DB. Cannot proceed!", 
                            localDeclaredType, storeType);
                    log.error(message);
                    throw new RepositoryException(message);
                }
                
                if(localStoreConfig != null) // if local exists replace it with merged version
                    jcrStoreConfig.putAll(localStoreConfig);

                jcrStoreConfig.put(ContentStoreConstants.PROP_STORE_TYPE, storeType);    
                storesConfig.put(storeName, jcrStoreConfig);
            }
        }catch(IOException ex){
            String message = "Failed to load configuration from JCR DB.";
            log.error(message, ex);
            throw new RepositoryException(message, ex);
        }
    }

    
    
    
    /**
     * Returns Connection Provider instance.
     * @return
     */
    public ConnectionProvider getConnectionProvider(){
        return connectionProvider;
    }
    
    
    /**
     * Returns configured JCR instance name. Can be <code>null</code>
     * in case given instance runs not in JCR instance scope.
     * @return
     */
    public String getJCRInstanceName(){
        return jcrInstanceName;
    }
    
    /**
     * Sets JCR instance name which owns given instance.
     * @param instanceName
     */
    public void setJCRInstanceName(String instanceName){
        jcrInstanceName = instanceName;
    }

    /**
     * Returns <code>true</code> if store with specified name is registered.
     * @param storeName
     * @return
     */
    public boolean isStoreRegistered(String storeName){
        return storeBuilders.containsKey(storeName);
    }
    
    /**
     * Returns set of registered store names.
     * @return
     */
    public Set<String> getStoreNames(){
        return storeBuilders.keySet();
    }
    

    /**
     * Returns set of supported store type names
     * @return
     */
    public Set<String> getSupportedStoreTypeNames(){
        return STORE_TYPES.keySet();
    }

    public void addStore(String storeName, Map<String, String> configuration, boolean defaultModeLocal){
        
        if(storeBuilders.containsKey(storeName)){
            String message  = MessageFormat.format("{0} Store is already configured!",
                    new Object[]{storeName});
            log.error(message);
            throw new RuntimeException(message);
        }
        
        
        String isLocalStr = (String)configuration.get(ContentStoreConstants.PROP_STORE_LOCAL_MODE);
        boolean isLocal = isLocalStr == null ? defaultModeLocal : new Boolean(isLocalStr);
        if(isLocal) // store is configured in remote mode all local settings are are not important.
            addLocalStore(storeName, JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_LOCAL_PREFIX, configuration));
        else
            addRemoteStoreProxy(storeName, JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_REMOTE_PREFIX, configuration));
    }
    

    protected Class<ContentStoreBuilder> getStoreBuilderClass(String typeName){
        synchronized(storeBuilderClasses){
            Class<ContentStoreBuilder> bc = storeBuilderClasses.get(typeName);
            if(bc != null) 
                return bc;
            Class<ContentStoreBuilder> clazz = loadStoreBuilderClass(typeName, true);
            storeBuilderClasses.put(typeName, clazz);
            return clazz;
        }
    }

    @SuppressWarnings("unchecked")    
    private static Class<ContentStoreBuilder> loadStoreBuilderClass(String typeName, boolean reportError){
        String name = STORE_TYPES.get(typeName);
        if(name == null){
            String msg = MessageFormat.format("Unknown store type name {0} is requested ", typeName);
            log.error(msg);
            throw new RuntimeException(msg);
        }
        
        try{
            Class<ContentStoreBuilder> clazz = (Class<ContentStoreBuilder>)Class.forName(name);
            storeBuilderClasses.put(typeName, clazz);
            return clazz;
        }catch(Exception ex){
            if(reportError){
                String msg = MessageFormat.format("Cannot instantiate builder {0} for store type {1} is requested "
                        , name
                        , typeName);
                log.error(msg, ex);
                throw new RuntimeException(msg, ex);
            }
        }
        
        return null;
    }    
    
    /**
     * Adds Content Store Builder for Content Store Proxy
     * @param storeName
     * @param configuration
     */
    protected void addRemoteStoreProxy(String storeName, Map<String, String> configuration){
        
        Class<ContentStoreBuilder> c = getStoreBuilderClass(ContentStoreConstants.STORE_TYPE_REMOTE);
        try{
            ContentStoreBuilder csf = c.newInstance();
            ContentStoreConfiguration config = csf.newConfigurationInstance();
            config.configure(storeName, 
                                getConnectionProvider(), 
                                configuration, 
                                getJCRInstanceName(), 
                                getContentStoreUpdateManager(),
                                getCacheManager());
            csf.init(config);
            storeBuilders.put(storeName, csf);        
            
            if(log.isDebugEnabled())
                log.debug(MessageFormat.format("Content Store Proxy for Store {0} is Registered", storeName));
        }catch(Exception ex){
            String msg = MessageFormat.format("Failed to add Content Store Proxy for Store {0}"
                    , storeName);
            log.error(msg, ex);
            throw new RuntimeException(msg, ex);
        }
    }

    /**
     * Adds Content Store Builder for Content store of type specified by <code>type</code> 
     * with name specified by <code>storeName</code>.
     * @param storeName
     * @param type
     * @param configuration
     */
    protected void addLocalStore(String storeName, Map<String, String> configuration){
        
        String type = (String)configuration.get(ContentStoreConstants.PROP_STORE_TYPE);         
        
        if(type == null){
            type = DEFAULT_STORE_TYPE;
            String message = MessageFormat.format("Store type is not defined for store {0}. Default type {1} will be used."
                    , storeName, DEFAULT_STORE_TYPE);
            log.info(message);
        }
        
        try{
            ContentStoreBuilder csf = getStoreBuilderClass(type.toUpperCase()).newInstance();
            ContentStoreConfiguration config = csf.newConfigurationInstance();
            config.configure(storeName, getConnectionProvider(), configuration);
            csf.init(config);

            storeBuilders.put(storeName, csf);

            if(log.isDebugEnabled())
                log.debug(MessageFormat.format("Store {0} of type {1} is Registered", storeName, type));
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to initialize store {0} of type {1}", storeName, type);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
    /**
     * Returns store with validation.
     * @param storeName
     * @return
     */
    public ContentStore getStore(String storeName){
        return getStore(storeName, true);
    }
    
    /**
     * Returns new instance of ContentStore of specified Store Type Name.
     * @param storeName
     * @param validate if true validates store.
     * @return
     */
    public ContentStore getStore(String storeName, boolean validate){
        ContentStoreBuilder factory = storeBuilders.get(storeName.toUpperCase());
        if(factory == null){
            String message  = MessageFormat.format("{0} Store does not exist!",
                    new Object[]{storeName});
            log.error(message);            
            throw new RuntimeException(message);
        }

        ContentStore store = factory.createStore();
        if(validate)
            validateStore(storeName, store);
        
        return store;
    }

    
    /**
     * Revalidates Content Store.
     * @param name
     */
    public void revalidateStore(String name){
        storeStatus.remove(name);
        getStore(name);
    }

    
    /**
     * Validates all stores registered with given ContentStoreProvider instance.
     * @throws RepositoryException
     */
    public void validateStores(){
        Set<String> storeNames = getStoreNames();
        for(String storeName:storeNames)
            getStore(storeName); // get store invokes store validation.
    }
    
    /**
     * Validates single store and caches result.
     * @param storeName
     */
    private void validateStore(String name, ContentStore store){
        STORE_STATUS status = storeStatus.get(name);
        
        if(status == STORE_STATUS.VALID)
            return;

        if(status == STORE_STATUS.INVALID){
            String message = MessageFormat.format("Content Store {0} cached status is INVALID", name);
            log.warn(message);
            throw new RuntimeException(message);
        }

        try{
            log.info(MessageFormat.format("Validating Content Store {0}",name));
            store.validate();
            storeStatus.put(name, STORE_STATUS.VALID);            
            log.info(MessageFormat.format("Validating Content Store {0} ... Passed",name));            
        }catch(Exception ex){
            storeStatus.put(name, STORE_STATUS.INVALID);
            String message = MessageFormat.format("Content Store {0} validation FAILED.", name);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
/*
 *    ----------------- Remote Access Related methods ----------------------
 *      These are fields lazy initialized on startup thuse in normal case
 *      those won't be accessed from multiple threads and
 *      no synchronization is needed
 *    ----------------------------------------------------------------------
 */

    /**
     * Lazy initializes and returns Content Store Update Manager used
     * in case Remote Content Store access is configured.
     */
    public Object getContentStoreUpdateManager(){
        if(updateManager == null){
            try{
                Class clazz = Class.forName("com.exigen.cm.store.remote.client.ContentStoreUpdateManager");
                Constructor c = clazz.getConstructor(new Class[]{String.class});
                updateManager = c.newInstance(new Object[]{getJCRInstanceName()});
            }catch(Exception ex){
                String msg = "Failed to instantiate Update Manager for Remote store. Check remote stores classpath is properly configured";
                log.error(msg, ex);
                throw new RuntimeException(msg, ex);
            }
//            updateManager = new ContentStoreUpdateManager(getJCRInstanceName());
        }
        
        return updateManager;
    }

    /**
     * Lazy initializes and returns Content Store Proxy Cache Manager used
     * in case Remote Content Store access is configured.
     */
    public Object getCacheManager(){
        if(cspCacheManager == null){
            try{
                cspCacheManager = Class.forName("com.exigen.cm.store.remote.client.CSPCacheManager").newInstance();
            }catch(Exception ex){
                String msg = "Failed to instantiate Cache Manager for Remote store.Check remote stores classpath is properly configured";
                log.error(msg, ex);
                throw new RuntimeException(msg, ex);
            }
        }
        
        return cspCacheManager;
    }

    
// *************** Utility Methods *****************************
    /**
     * Tests ptovided content store type configuration items for existence
     * and returns set of missing properties.
     * @return
     */
    public static Set<String> getMissedConfigurationItems(String typeName, Map<String, String> configuration){
        if(!STORE_TYPES.containsKey(typeName)){
            String message = MessageFormat.format("Content Store Type {0} is not supported!",
                    typeName);
            log.error(message);
            throw new RuntimeException(message);
        }
        
        try{
            return loadStoreBuilderClass(typeName, true).newInstance().newConfigurationInstance().getMissedConfigurationItems(configuration);
        }catch(Exception ex){
            String message = MessageFormat.format("Falied to check configuration of Content Store Type {0} due to exception.",
                    typeName);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
    /**
     * Validates content store remote configuration values for existence
     * and returns set of missing properties.
     * @return
     *//*
    public static Set<String> getMissedRemoteConfigurationItems(Map<String, String> configuration){
        return new ContentStoreProxyConfiguration().getMissedConfigurationItems(configuration);
    }//*/
    
    /**
     * Returns <code>true</code> if content store type with <code>typeName</code> is supported.s
     * @param typeName
     * @return
     */
    public static boolean isContentStoreTypeSupported(String typeName){
        return typeName == null ? false : loadStoreBuilderClass(typeName.toUpperCase(), false) != null;
    }
}
/*
 * $Log: ContentStoreProvider.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.34  2007/02/22 09:24:39  dparhomenko
 * PTR#1803806 implement jsr283
 *
 * Revision 1.33  2006/12/13 14:43:06  maksims
 * #1803611 message log level changed to info
 *
 * Revision 1.32  2006/12/01 15:53:24  maksims
 * #0149528 store type name changed to upper case
 *
 * Revision 1.31  2006/12/01 12:14:49  maksims
 * #0149480 Fixed check for store type
 *
 * Revision 1.30  2006/11/30 14:54:32  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.29  2006/11/14 07:38:24  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.28  2006/11/09 15:44:23  maksims
 * #1801897 Centera Content Store added
 *
 * Revision 1.27  2006/08/21 11:20:12  maksims
 * #1801897 Default store mode local property added
 *
 * Revision 1.26  2006/08/15 16:11:05  maksims
 * #1802426 SQL framework is used to query DB
 *
 * Revision 1.25  2006/08/15 12:19:05  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.24  2006/08/15 08:38:08  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.23  2006/08/14 16:18:36  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.22  2006/08/08 13:10:35  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.21  2006/07/28 15:49:07  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.20  2006/07/26 08:45:48  maksims
 * #1802414 added ability to initialize stores without validation
 *
 * Revision 1.19  2006/07/25 12:30:55  maksims
 * #1802414 store status caching updated
 *
 * Revision 1.18  2006/07/25 07:24:49  maksims
 * #1802414 code layout change
 *
 * Revision 1.17  2006/07/25 07:23:53  maksims
 * #1802414 store validation fixed
 *
 * Revision 1.16  2006/07/24 12:08:31  maksims
 * #1802414 Store validation should be performed once by ContentStoreProvider owner (RepositoryImpl etc) when ContentStoreProvider is initialized. Separate validation of each Content Store instance is disabled now.
 *
 * Revision 1.15  2006/07/12 11:51:10  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.14  2006/07/06 08:22:12  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.13  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 * Revision 1.12  2006/06/22 12:00:29  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.11  2006/05/05 13:16:25  maksims
 * #0144986 JCRHelper.getPropertieByPrefix result changed to Map<String, Object> so init method signature is changed correspondingly
 *
 * Revision 1.10  2006/05/03 15:48:42  maksims
 * #0144986 validate result is made cached
 *
 * Revision 1.9  2006/05/03 13:01:03  maksims
 * #0144986 ContentStore validate method implemented
 *
 * Revision 1.8  2006/05/03 10:53:06  maksims
 * #0144986 null properties problem fixed
 *
 * Revision 1.7  2006/05/03 08:36:11  maksims
 * #0144986 Content store provider constructor changed
 *
 */