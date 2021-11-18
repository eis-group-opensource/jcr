/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.file;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.store.ContentStoreConfiguration;
import com.exigen.cm.store.ContentStoreConstants;

/**
 * Holds file content store configuration.
 */
public class FileContentStoreConfiguration extends ContentStoreConfiguration {

    private static final Log log = LogFactory.getLog(FileContentStoreConfiguration.class);
    
    /**
     * Name of property pointing to content store root directory.
     * If this property isn't passed ./store directory will be used.
     */
    private File rootDir;
    private int bufferSize=1024;
    private int attempts=20;

    
    
    
    /**
     * 
     * @see com.exigen.cm.store.ContentStoreConfiguration#getType()
     */
    @Override
    public String getType() {
        return ContentStoreConstants.STORE_TYPE_FILE;
    }
    
    @Override
    public void configure(String store, ConnectionProvider connectionProvider, Map<String, String> configuration) throws RepositoryException {
        super.configure(store, connectionProvider, configuration);


        String rootDirName = configuration == null ? null : (String)configuration.get(ContentStoreConstants.PROP_FILE_STORE_ROOT);//, "./store");
        if(rootDirName == null){
            String message = MessageFormat.format("Store {0}.  Mandatory property rootDir is not defined",
                    getStoreName());
            throw new RuntimeException(message);
        }
        
        rootDir = new File(rootDirName);
        if(!rootDir.exists()){
            rootDir.mkdir();
        }else
        if(!rootDir.isDirectory()){
            String message = MessageFormat.format("Store {0}. Cannot use {1} because it is a file. Directory must be provided", 
                    new Object[]{getStoreName(), rootDirName});
            throw new RuntimeException(message);
        }
        

        String value = configuration == null ? null : (String)configuration.get(ContentStoreConstants.PROP_FILE_STORE_BUFFER_SIZE);
        if(value != null){
            bufferSize = new Integer(value).intValue();
        }
        
        value = configuration == null ? null : (String)configuration.get(ContentStoreConstants.PROP_FILE_STORE_ATTEMPTS);
        if(value != null){
            attempts = new Integer(value).intValue();
        }
        
        if(log.isDebugEnabled()){
            try{
                String message = MessageFormat.format("Store {0}. Initialization done with following parameters:\nRoot directory:{1}\nBufferSize:{2}\nMax Number of parallel content inserts:{3}\n",
                        getStoreName(), 
                        rootDir.getCanonicalPath(),
                        String.valueOf(bufferSize),
                        String.valueOf(attempts));
                log.debug(message);
            }catch(Exception ex){
                log.error("Failed to write log error", ex);
            }
        }    
    }

    public int getBufferSize() {
        return bufferSize;
    }
    

    public int getAttempts() {
        return attempts;
    }

    
    public File getRootDir() {
        return rootDir;
    }
    
    @Override
    public Set<String> getMissedConfigurationItems(Map<String, String> configuration) {
        if(configuration.containsKey(ContentStoreConstants.PROP_FILE_STORE_ROOT))
            return null;
        
        Set<String> missed = new HashSet<String>();
        missed.add(ContentStoreConstants.PROP_FILE_STORE_ROOT);
        return missed;
    }    
}

/*
 * $Log: FileContentStoreConfiguration.java,v $
 * Revision 1.2  2007/10/09 07:34:52  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:55  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/11/30 14:54:39  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.4  2006/08/21 11:20:17  maksims
 * #1801897 Default store mode local property added
 *
 * Revision 1.3  2006/08/14 16:18:42  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.2  2006/07/12 11:51:26  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:03:31  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 */