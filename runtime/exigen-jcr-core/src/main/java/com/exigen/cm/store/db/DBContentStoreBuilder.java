/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.db;

import com.exigen.cm.store.AbstractContentStoreBuilder;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreConfiguration;
import com.exigen.cm.store.ContentStoreConstants;

public class DBContentStoreBuilder extends AbstractContentStoreBuilder<DBContentStoreConfiguration> {

//    public static final String TYPE="DB";
    
    
    public void _init(DBContentStoreConfiguration configuration) {
        if(configuration.mayCreateIfAbsent()){
            new DBContentStore(configuration).createIfNotExists();
        }
    }

    protected ContentStore _createStore(DBContentStoreConfiguration configuration) {
        DBContentStore store = new DBContentStore(configuration);
        store.setContentTracker(this);
        return store;
    }

    public String getTypeName() {
        return ContentStoreConstants.STORE_TYPE_DB;
    }
    
    public ContentStoreConfiguration newConfigurationInstance() {
        return new DBContentStoreConfiguration();
    }

}

/*
 * $Log: DBContentStoreBuilder.java,v $
 * Revision 1.2  2007/12/07 15:04:43  maksims
 * added capability to create content table if not existing opn initialization
 *
 * Revision 1.1  2007/04/26 08:59:44  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/12/01 15:52:51  maksims
 * #0149528 AsbtractContentStoreBuilder renamed to AbstractContentStoreBuilder
 *
 * Revision 1.5  2006/11/30 14:54:41  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.4  2006/09/28 09:19:40  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.3  2006/08/14 16:18:41  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.2  2006/08/04 10:52:44  maksims
 * #1802356 Code cleanup
 *
 * Revision 1.1  2006/07/04 14:03:39  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 * Revision 1.6  2006/06/22 12:00:33  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.5  2006/05/05 13:16:56  maksims
 * #0144986 JCRHelper.getPropertieByPrefix result changed to Map<String, Object> so method signature is changed correspondingly
 *
 * Revision 1.4  2006/05/03 13:13:40  maksims
 * #0144986 table name changed
 *
 * Revision 1.3  2006/05/03 13:00:57  maksims
 * #0144986 ContentStore validate method implemented
 *
 * Revision 1.2  2006/05/03 08:36:16  maksims
 * #0144986 Content store provider constructor changed
 *
 * Revision 1.1  2006/05/02 11:44:26  maksims
 * #0144986 DB Content store type added
 *
 */