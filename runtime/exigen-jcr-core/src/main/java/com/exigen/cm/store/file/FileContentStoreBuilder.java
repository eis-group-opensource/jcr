/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
/**
 * 
 */
package com.exigen.cm.store.file;

import com.exigen.cm.store.AbstractContentStoreBuilder;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConfiguration;
import com.exigen.cm.store.ContentStoreConstants;

/**
 * Factory for instantiation of file based content store instances.
 * @author Maksims
 */
public class FileContentStoreBuilder extends AbstractContentStoreBuilder<FileContentStoreConfiguration> {

//    public static String TYPE="FILE";
    /** 
     * Instantiates File based content store.
     * @see com.exigen.cm.store.ContentStoreBuilder#createStore()
     */
    protected ContentStore _createStore(FileContentStoreConfiguration config) {
//        return new FileContentStore(rootDir, load, bufferSize, connectionProvider);
        FileContentStore store = new FileContentStore(config);
        store.setContentTracker(this);
        return store;
    }


    /**
     * @inheritDoc
     */
    public String getTypeName() {
        return ContentStoreConstants.STORE_TYPE_FILE;
    }
    
    public ContentStoreConfiguration newConfigurationInstance() {
        return new FileContentStoreConfiguration();
    }
}
