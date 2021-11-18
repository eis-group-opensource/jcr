/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.db;

import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.store.ContentStoreConfiguration;
import com.exigen.cm.store.ContentStoreConstants;

/**
 * Used to configure DB Content Store.
 */
public class DBContentStoreConfiguration extends ContentStoreConfiguration {

//  Name of property defining table name for store
//    public static final String PROP_TABLE_NAME="table";

    private String tableName;
    
    /**
     * If <code>true</code> declares that it is allowed to try to
     * create table if it is not exists.
     */
    private Boolean mayCreateIfAbsent;
    
    @Override
    public String getType() {
        return ContentStoreConstants.STORE_TYPE_DB;
    }

    @Override
    public void configure(String store, ConnectionProvider connectionProvider, Map<String, String> configuration) throws RepositoryException {
        super.configure(store, connectionProvider, configuration);

        tableName = configuration == null ? null : (String)configuration.get(ContentStoreConstants.PROP_DB_STORE_TABLE);
        tableName = tableName == null ? getStoreName() : tableName;
        tableName = connectionProvider.getDialect().convertTableName("CM_STORE_"+tableName);
        
        mayCreateIfAbsent = new Boolean(configuration == null 
                                            ? null 
                                            : (String)configuration.get(ContentStoreConstants.PROP_DB_STORE_MAY_CREATE_TABLE));
    }
    
    
    
    public String getTableName(){
        return tableName;
    }

    @Override
    public Set<String> getMissedConfigurationItems(Map<String, String> configuration) {
//      All params are optional!
        return null;
    }

    public Boolean mayCreateIfAbsent() {
        return mayCreateIfAbsent;
    }
}

/*
 * $Log: DBContentStoreConfiguration.java,v $
 * Revision 1.3  2007/12/07 15:04:43  maksims
 * added capability to create content table if not existing opn initialization
 *
 * Revision 1.2  2007/10/09 07:34:55  dparhomenko
 * Add mssql2005 support
 *
 * Revision 1.1  2007/04/26 08:59:44  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/11/30 14:54:41  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.3  2006/08/14 16:18:41  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.2  2006/07/12 11:51:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 14:03:39  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 */