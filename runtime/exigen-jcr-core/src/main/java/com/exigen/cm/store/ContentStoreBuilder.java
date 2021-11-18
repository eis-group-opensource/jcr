/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.store;



/**
 * Implementation of this intefrace used to instantiate proper
 * instances of ContentStore.
 * 
 * @author Maksims
 * 
 * 1. method init() should be eliminated.
 *
 */
public interface ContentStoreBuilder{
    
    /**
     * Initializes Content Store with provided Configuration
     * @param config
     */
    public void init(ContentStoreConfiguration config);

    /**
     * Creates new configured Content Store instance specific to given builder.
     * @return
     */
    public ContentStore createStore();
    /**
     * Returns type name of stores given factory implementation provides.
     * @return
     */
    public String getTypeName();
    
    /**
     * Returns builder specific instance of configuration ready to configure.
     * @return
     */
    public ContentStoreConfiguration newConfigurationInstance();
}

/*
 * $Log: ContentStoreBuilder.java,v $
 * Revision 1.1  2007/04/26 08:59:42  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 */