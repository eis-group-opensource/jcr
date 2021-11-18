/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.ConnectionProviderImpl;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.store.AbstractContentStore;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConstants;
import com.exigen.cm.store.ContentStoreProvider;
import com.exigen.cm.store.ContentStoreProviderConfiguration;
import com.exigen.cm.store.StoreHelper;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchUnit;
import com.exigen.cm.store.remote.common.ContentDataSource;

/**
 * Used by TransportAdapterClient server side to invoke operations
 * on Content Stores.
 */
public class ContentRequestDispatcher {

    private ContentStoreProvider storeProvider;
    private ConnectionProvider connectionProvider;    
    
    private static final Log log  = LogFactory.getLog(ContentRequestDispatcher.class);

    /**
     * Holds name of default properties file name.
     */
    public static final String DEFAULT_PROPERTIES_FILE_NAME = "dispatcher.properties";

    /**
     * Using provided configuration should create:
     * - ConnectionProvider
     * - ContentStoreProvider
     * @param configuration
     */
    public ContentRequestDispatcher(){
        InputStream propsStream = getClass().getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE_NAME);
        if(propsStream == null)
            throw new RuntimeException("dispatcher.properties is not found in classpath");

        
        try{

            Properties config = new Properties();
            config.load(propsStream);
        
//    }
//    
//    public ContentRequestDispatcher(Map<String, String> config){
//        
//        try{
            connectionProvider = new ConnectionProviderImpl();
            if(config.get(Constants.PROPERTY_DATASOURCE_JNDI_NAME) == null)
                config.put(Constants.PROPERTY_DATASOURCE_JNDI_NAME, "DS_CONTENT_REQUEST_DISPATCHER_"+ this.hashCode());
            connectionProvider.configure(config, null);
            
            Map<String, String> storesConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.STORE_CONFIG_PREFIX, config);
            ContentStoreProviderConfiguration cspConfig = new ContentStoreProviderConfiguration();
            cspConfig.configure(storesConfig);
            
            storeProvider = new ContentStoreProvider();
            storeProvider.init(connectionProvider, cspConfig);
            storeProvider.validateStores();
            
        }catch(RuntimeException ex){
           throw ex;
        }catch(Exception ex){
            String message = "Cannot create instance";
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
    /**
     * Processes batch. Responsible for DatabaseConnection management.
     * @param batch
     */
    public void processBatch(Batch batch){
        AbstractContentStore store = (AbstractContentStore)storeProvider.getStore(batch.getStoreName());        
        DatabaseConnection connection = null;
        try{
            connection = connectionProvider.createConnection();
            store.begin(connection);
            
            Iterator<BatchUnit> units = batch.getUnitsIterator();
            while(units.hasNext()){
                BatchUnit unit = units.next();
                
                switch(unit.getOperation()){
                    case INSERT:
                        ContentDataSource dataSource = unit.getDataSource();
                        if(dataSource == null){
                            String message = MessageFormat.format("Bad INSERT provided with no content data provided. Rollback invoked. Content Store: {0}, Content ID: {1}",
                                    batch.getStoreName(), unit.getJCRContentId());
                            log.error(message);
                            store.rollback();
                            throw new RuntimeException(message);
                        }
                        
                        if(unit.getLength() != dataSource.getLength()){
                            log.debug(MessageFormat.format("Declared length: {0} Recieved length: {1}", unit.getLength(), dataSource.getLength()));
                        }

                        
                        
                        if(unit.getLength() != dataSource.getLength()){
                            
                            String message = MessageFormat.format("Bad INSERT provided. Provided content size {0} is not the same as downloaded {1}. Rollback invoked. Content Store: {2}, Content ID: {3}",
                                    String.valueOf(unit.getLength()), 
                                    String.valueOf(dataSource.getLength()), 
                                    batch.getStoreName(), 
                                    String.valueOf(unit.getJCRContentId()));
                            log.error(message);
                            store.rollback();
                            throw new RuntimeException(message);
                        }
                        
                        store.put(unit.getJCRContentId(), dataSource.getData(), unit.getLength(), unit.getParams());

//                        long length = store.getContentLength(unit.getJCRContentId());
//                        if(unit.getLength() != length){
//                            log.debug(MessageFormat.format("Declared length: {0} Written length: {1}", unit.getLength(), length));
//                        }

                        
                            
                            
                        dataSource.release();
                        break;
                    case REMOVE:
                        store.remove(unit.getJCRContentId());
                        break;
                        default:
                            throw new RuntimeException("Unknown operation");
                }
            }
            store.commit();
            connection.commit();
        }catch(RuntimeException ex){
           throw ex;
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to execute batch on Content Store {0}. Rolling back ...",
                    batch.getStoreName());
            log.error(message, ex);
            
            store.rollback();
            throw new RuntimeException(message, ex);
        }finally{
            try{
                if(connection != null) 
                    connection.close();
            }catch(Exception rex){
                log.debug("Failed to close connection", rex);
            }
        }
    }
    
    
    /**
     * Returns input stream on Content specified by contentId parameter
     * from Content Store specified by storeName parameter.
     * @param storeName - name of Content Store from which content should be obtained.
     * @param contentId - ID of Content to be returned.
     * @return
     */
    public void getContent(String storeName, String jcrContentId, OutputStream target) throws Exception{
        ContentStore store = storeProvider.getStore(storeName);
        StoreHelper.transfer(store.get(new Long(jcrContentId)), target);
    }
    
    
    /**
     * Returns <code>true</code> if Content Store specified
     * by storeName parameter is alive.
     * @param storeName - name of Content Store to be tested.
     * @return <code>true</code> if Content Store is alive.
     */
    public boolean isAlive(String storeName){
        try{
            ContentStore store = storeProvider.getStore(storeName);
            store.validate();
            return true;
        }catch(Exception ex){}
        
        return false;
    }
}

/*
 * $Log: ContentRequestDispatcher.java,v $
 * Revision 1.2  2007/05/31 08:54:10  dparhomenko
 * PTR#1804512 move to jcr project
 *
 * Revision 1.1  2007/04/26 09:02:21  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/12/15 11:54:35  dparhomenko
 * PTR#1803217 code reorganization
 *
 * Revision 1.7  2006/08/14 16:18:40  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.6  2006/08/08 13:10:39  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.5  2006/08/02 11:42:32  maksims
 * #1802426 SQL Framework used to generate queries
 *
 * Revision 1.4  2006/07/28 15:49:08  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.3  2006/07/24 12:08:32  maksims
 * #1802414 Store validation should be performed once by ContentStoreProvider owner (RepositoryImpl etc) when ContentStoreProvider is initialized. Separate validation of each Content Store instance is disabled now.
 *
 * Revision 1.2  2006/07/12 11:51:12  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:04:45  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */