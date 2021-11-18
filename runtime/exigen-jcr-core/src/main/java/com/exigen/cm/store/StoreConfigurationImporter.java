/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

//import static com.exigen.cm.store.ContentStoreConstants.STORE_DEF_IMPORT;
//import static com.exigen.cm.store.ContentStoreConstants.STORE_DEF_IMPORT_DATA;
//import static com.exigen.cm.store.ContentStoreConstants.STORE_DEF_IMPORT_DATA_DEFAULT;
import static com.exigen.cm.store.ContentStoreConstants.STORE_DEF_PREFIX;
import static com.exigen.cm.store.ContentStoreConstants.TYPE_DEF_PREFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;

/**
 * Used to import Content Store configuration into JCR database
 */
public class StoreConfigurationImporter {
    
    private static final Log log = LogFactory.getLog(StoreConfigurationImporter.class);
    
    /* *
     * Imports Content Stores configuration in DB taking 
     * config file location from <code>configuration</code> parameter.
     * @param connection is a database connection used to write data in DB.
     * @param configuration - part of Repository properties related to Content Store configuration.
     * @throws RepositoryException
     * /
    public void importConfiguration(DatabaseConnection connection, Map<String, String> configuration) throws RepositoryException{
        String needsImport = configuration.get(STORE_DEF_IMPORT);
        if(needsImport == null || !Boolean.parseBoolean((String)needsImport))
            return;
        
        
        String properties = configuration.get(STORE_DEF_IMPORT_DATA);
        if(properties == null)
            importConfiguration(connection);
        else
            importConfiguration(connection, properties);
    }


    
    public void importConfiguration(DatabaseConnection connection) throws RepositoryException{
        log.info("Importing Content Store definitions from default store.properties in classpath");        
        importConfiguration(connection, getClass().getResourceAsStream(STORE_DEF_IMPORT_DATA_DEFAULT));        
    }
    //*/
    /**
     * Imports Content Stores configuration in DB taking
     * data from file provided as <code>propertiesFile</code> parameter.
     * @param connection is a database connection used to write data in DB.
     * @param propertiesFile is a reference to file containing Stores configurationdata. If <code>null</code> provided
     * classpath will be checked for existence of Content Stores configuration file store.properties.
     * @throws RepositoryException
     */
    public void importConfiguration(DatabaseConnection connection, String propertiesPath) throws RepositoryException{    
        File propertiesFile = new File(propertiesPath);
        if(!propertiesFile.exists()){
            String message = MessageFormat.format("Cannot import Content Stores configuration from provided file: {0}. File not exists!",
                    propertiesFile.getAbsolutePath());
            log.error(message);
            throw new RepositoryException(message);
        }

        log.info(MessageFormat.format("Importing Content Store definitions from {0}",
                    propertiesFile.getAbsolutePath()));
        
        try{
            importConfiguration(connection, new FileInputStream(propertiesFile));
        }catch(FileNotFoundException ex){/*Cannot be thrown 'cos is caught above*/
            log.error("Stores config import error!", ex);
            throw new RepositoryException(ex);
        }
    }

    
    @SuppressWarnings("unchecked")
    public void importConfiguration(DatabaseConnection connection, InputStream data) throws RepositoryException{
        if(data == null){
            String message = "Cannot import Content Store configuration. NULL stream is provided!";
            log.error(message);
            throw new RepositoryException(message);
        }
        
        Map<String,String> storeDefProps;

        try{
            Properties storeDefPropsTmp = new Properties();
            storeDefPropsTmp.load(data);
            storeDefProps = (Map)storeDefPropsTmp;
        }catch(IOException ex){
            String message = "Failed to load Content Store definitions.";
            log.error(message, ex);
            throw new RepositoryException(message, ex);
        }
        importConfiguration(connection, storeDefProps);
    }
    

    public void importConfiguration(DatabaseConnection connection, Map<String,String> configuration) throws RepositoryException{
        Map<String, String> typeDefs = JCRHelper.getPropertiesByPrefix(TYPE_DEF_PREFIX, configuration);
        Map<String, String> storeDefs = JCRHelper.getPropertiesByPrefix(STORE_DEF_PREFIX, configuration);
        
        Map<String, Map<String,String>> typeDefinitions = getDefinitions(typeDefs);
        Map<String, Map<String,String>> storeDefinitions = getDefinitions(storeDefs);
        validate(typeDefinitions, storeDefinitions);
        
        Map<String, Long> typeIds = importStoreTypeDefs(connection, typeDefinitions);
        importStoreDefs(connection, storeDefinitions, typeIds);
    }


    /**
     * Validates configuration by invoking corrsponding type configuration impls.
     * @param typeDefinitions
     * @param storeDefinitions
     */
    private void validate(Map<String, Map<String, String>> typeDefinitions, Map<String, Map<String, String>> storeDefinitions) {
        
        Set<String> storeNames = storeDefinitions.keySet();
//        boolean defaultStoreFound = false; Default store is created in any case so it shouldn't be necessary explicitely declared
        
        Set<String> validatedTypeNames = new HashSet<String>();// not all types might be assigned to stores. such a types should also be validated
        
        for(String store:storeNames){
//            if(!defaultStoreFound && ContentStoreConstants.DEFAULT_STORE_NAME.equalsIgnoreCase(store))
//                defaultStoreFound=true;
            
            Map<String, String> storeConfig = storeDefinitions.get(store);
            String storeType = storeConfig.get(ContentStoreConstants.PROP_STORE_LOCAL_TYPE);
            if(storeType == null ){
                String message = MessageFormat.format("Cannot import store definition for {0}. Store type parameter is not defined!",store);
                log.error(message);
                throw new RuntimeException(message);
            }
            
            if(!ContentStoreProvider.isContentStoreTypeSupported(storeType)){
                String message = MessageFormat.format("Cannot import store definition for {0}. Store type {1} is not supported!",store, storeType);
                log.error(message);
                throw new RuntimeException(message);
            }            

            
            storeType = storeType.toUpperCase();
            
//          Add validated type
            validatedTypeNames.add(storeType);

            
            
            Map<String,String> fullLocalConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_LOCAL_PREFIX, storeConfig);
            
            Map<String, String> typeConfig = typeDefinitions.get(storeType);
            if(typeConfig == null){
//                String message = MessageFormat.format("Cannot import store definition for {0}. Store type configuration for {1} is not defined!"
//                        ,store, storeType);
                String message = MessageFormat.format("Store definition for {0} will not have default settings because Store type configuration for {1} is not provided!"
                        ,store, storeType);
                
                log.warn(message);
                
//              If type referenced, supported but not configured add dummy configuration for it
                typeDefinitions.put(storeType, new HashMap<String,String>());
                
            }else{
                Map<String,String> typeLocalConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_LOCAL_PREFIX, typeConfig);
                typeLocalConfig.putAll(fullLocalConfig); // override type default by local
                fullLocalConfig = typeLocalConfig;
            }



            Set<String> missed = ContentStoreProvider.getMissedConfigurationItems(storeType, fullLocalConfig);
            if(missed != null){
                String message = MessageFormat.format("Content Store {0} of type {1} configuration does not contain all required properties. Missed properties are: {2} " +
                        "\nEnsure these properties are existing in Repository local stores configuration.",
                        store, storeType, missed);
                log.warn(message);
            }

            
            Map<String,String> fullRemoteConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_REMOTE_PREFIX, storeConfig);
            if(typeConfig != null){
                Map<String,String> typeLocalConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_REMOTE_PREFIX, typeConfig);
                typeLocalConfig.putAll(fullLocalConfig); // override type default by local
                fullLocalConfig = typeLocalConfig;
            }
            
            
            if(fullRemoteConfig.size() != 0){ // remote config is optional, but if exists it should be validated
//                missed = ContentStoreProvider.getMissedRemoteConfigurationItems(fullRemoteConfig);
                missed = ContentStoreProvider.getMissedConfigurationItems(ContentStoreConstants.STORE_TYPE_REMOTE, fullRemoteConfig);
                if(missed != null){
                    String message = MessageFormat.format("Content Store {0} of type {1} Remote configuration does not contain all required properties. Missed properties are: {2} " +
                            "\nEnsure these properties are existing in Repository local stores configuration.",
                            store, storeType, missed);
                    log.warn(message);
                }            
            }
        }

// Validate types which aren't referred from Store defs        
        Set<String> typeNames = typeDefinitions.keySet();
//        typeNames.removeAll(validatedTypeNames);
        for(String type : typeNames){
            if(validatedTypeNames.contains(type))
                continue;
            
            Map<String,String> typeConfig = typeDefinitions.get(type);
            Set<String> missed = ContentStoreProvider.getMissedConfigurationItems(type, typeConfig);
            if(missed != null){
                String message = MessageFormat.format("Content Store Type {0} configuration does not contain all required properties. Missed properties are: {1} " +
                        "\nIn order to use Content Stores of this type missing properties should be provided Content Store configuration in Repository local stores configuration.",
                        type, missed);
                log.warn(message);
            }

            Map<String,String> typeRemoteConfig = JCRHelper.getPropertiesByPrefix(ContentStoreConstants.PROP_STORE_REMOTE_PREFIX, typeConfig);
            if(typeRemoteConfig.size() != 0){ // remote config is optional, but if exists it should be validated
                missed = ContentStoreProvider.getMissedConfigurationItems(ContentStoreConstants.STORE_TYPE_REMOTE, typeRemoteConfig);
                if(missed != null){
                    String message = MessageFormat.format("Content Store Type {0} Remote configuration does not contain all required properties. Missed properties are: {1} " +
                            "\nIn order to use Content Stores of this type in remote mode missing properties should be provided Content Store configuration in Repository local stores configuration.",
                            type, missed);
                    log.warn(message);
                }            
            }
        }
    }


    protected Map<String, Map<String,String>> getDefinitions(Map<String, String> configuration){
        Set<String> definitionNames = configuration.keySet();
        
        Map<String, Map<String,String>> definitions = new HashMap<String, Map<String,String>>();
        
        for(String defProperty:definitionNames){
            String defName = defProperty.substring(0, defProperty.indexOf('.'));
            
            if(definitions.containsKey(defName.toUpperCase()))
                continue;

            Map<String, String> storeTypeConfig = JCRHelper.getPropertiesByPrefix(defName, configuration);
            definitions.put(defName.toUpperCase(), storeTypeConfig);
        }
        
        return definitions;
    }


    
    protected Map<String, Long> importStoreTypeDefs(DatabaseConnection connection, Map<String, Map<String,String>> typeDefinitions) throws RepositoryException{
        Map<String, Long> typeIds = new HashMap<String, Long>();

//      First load existing types
        DatabaseSelectAllStatement select = DatabaseTools.createSelectAllStatement(Constants.TABLE_STORE_TYPE, true);
        select.addResultColumn(Constants.FIELD_ID);
        select.addResultColumn(Constants.TABLE_STORE_TYPE__NAME);
        select.execute(connection);
        
        while(select.hasNext()){
            RowMap row = select.nextRow();
            typeIds.put(row.getString(Constants.TABLE_STORE_TYPE__NAME).toUpperCase(), row.getLong(Constants.FIELD_ID));
        }
        
        
        
        
        if(typeDefinitions.size() == 0){
            log.info("No Content Store types for import is provided");
            return typeIds;
        }

        
        DatabaseInsertStatement inserts = DatabaseTools.createInsertStatement(Constants.TABLE_STORE_TYPE);

        Set<String> types = typeDefinitions.keySet();
        try{
            int insertsCount = 0;
            for(String type:types){
                if(typeIds.containsKey(type)){
                    String message = MessageFormat.format("Type {0} is already registered. Cannot override. Record is ignored!", type);
                    log.warn(message);
                    continue;
                }
                
                Long id = connection.nextId();
                typeIds.put(type, id);
                
                inserts.addValue(SQLParameter.create(Constants.FIELD_ID, id));
                inserts.addValue(SQLParameter.create(Constants.TABLE_STORE_TYPE__NAME, type));
                
                String config = StoreHelper.mapToPropertiesString("Configuration for Content Store Type: " + type,typeDefinitions.get(type));
                inserts.addValue(SQLParameter.create(Constants.TABLE_STORE_TYPE__CONFIG, config));
                
                if(log.isDebugEnabled()){
                    String message = MessageFormat.format("Adding configuration for Store Type {0}. Configuration: {1}",
                            type, config);
                    log.debug(message);
                }
                
                inserts.addBatch();
                insertsCount++;
            }
            if(insertsCount>0)
                inserts.execute(connection);
            
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to insert Content Store types information into {0} table.",
                    Constants.TABLE_STORE_TYPE);
            log.error(message, ex);
            throw new RepositoryException(message, ex);
        }
        return typeIds;
    }
    
    
    protected void importStoreDefs(DatabaseConnection connection, Map<String, Map<String,String>> storeDefinitions, Map<String, Long> typeIds) throws RepositoryException{

        if(storeDefinitions.size() == 0){
            log.info("No Content Store  configuration for import is provided");
            return;
        }
        
        Set<String> stores = storeDefinitions.keySet();
        
//      First load existing types
        DatabaseSelectAllStatement select = DatabaseTools.createSelectAllStatement(Constants.TABLE_STORE, true);
        select.addCondition(Conditions.in(Constants.TABLE_STORE__NAME, stores));
        select.addResultColumn(Constants.TABLE_STORE__NAME);
        select.execute(connection);


        if(select.hasNext()){
            while(select.hasNext()){
                String registered = select.nextRow().getString(Constants.TABLE_STORE__NAME);
                storeDefinitions.remove(registered.toUpperCase());
                log.warn(MessageFormat.format("Content Store {0} is already registered. Importing configuration for it will be ignored!", registered));
            }

            if(storeDefinitions.size() == 0){
                log.info("All Content Store configurations provided for import are already registered.");
                return;
            }
            stores = storeDefinitions.keySet();            
        }
        
        
        
        DatabaseInsertStatement inserts = DatabaseTools.createInsertStatement(Constants.TABLE_STORE);
        try{
            for(String store:stores){
                Long id = connection.nextId();
                
                Map<String, String> storeConfig = storeDefinitions.get(store);
                String typeName = (String)storeConfig.get(ContentStoreConstants.PROP_STORE_LOCAL_TYPE);
                typeName = typeName.toUpperCase();
                Long typeId = typeIds.get(typeName);
                if(typeId == null){
                    String message = MessageFormat.format("Cannot register Content Store {0} of Type {1}. Type is not configured!",
                            store, typeName);
                    log.error(message);
                    throw new RepositoryException(message);
                }
                
                
                String storeConfigStr = StoreHelper.mapToPropertiesString("Configuration for Content Store: " + store, storeConfig);
                
                inserts.addValue(SQLParameter.create(Constants.FIELD_ID, id));
                
                inserts.addValue(SQLParameter.create(Constants.TABLE_STORE__NAME, store));
                inserts.addValue(SQLParameter.create(Constants.TABLE_STORE__TYPE, typeId));
                inserts.addValue(SQLParameter.create(Constants.TABLE_STORE__CONFIG, storeConfigStr));
                inserts.addBatch();
                
                if(log.isDebugEnabled()){
                    String message = MessageFormat.format("Adding configuration for Store {0} of Type {1}. Configuration: {2}",
                            store, typeName, storeConfigStr);
                    log.debug(message);
                }
            }        
            inserts.execute(connection);
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to insert Content Stores information into {0} table.",
                    Constants.TABLE_STORE);
            log.error(message, ex);
            throw new RepositoryException(message, ex);
        }        
    }
}

/*
 * $Log: StoreConfigurationImporter.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/11/30 14:54:32  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.7  2006/08/21 11:20:12  maksims
 * #1801897 Default store mode local property added
 *
 * Revision 1.6  2006/08/17 12:13:31  maksims
 * #1801897 Fixed to get test connection properties from default.properties
 *
 * Revision 1.5  2006/08/14 16:18:36  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.4  2006/08/02 13:38:45  maksims
 * #1802356 Content Store configuration import utility added
 *
 * Revision 1.3  2006/08/02 11:42:29  maksims
 * #1802426 SQL Framework used to generate queries
 *
 * Revision 1.2  2006/07/12 11:51:10  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/06 08:22:12  maksims
 * #1802356 Content Store configuration import added
 *
 */