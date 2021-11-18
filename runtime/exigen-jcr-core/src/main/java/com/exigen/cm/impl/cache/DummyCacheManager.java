/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.cache;

import java.util.Map;

import javax.jcr.RepositoryException;

import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.impl.RepositoryImpl;

public class DummyCacheManager extends AbstractCacheManager{

    public void configure(RepositoryImpl repository, Map config) throws RepositoryException {
    }

    public void put(String entityName, Long entityId, RowMap row) {
    }

    public void evict(String entityName, Long entityId) {
    }

    public RowMap get(String entityName, Long entityId) {
        return null;
    }

    public void stats() {
    }

}


/*
 * $Log: DummyCacheManager.java,v $
 * Revision 1.1  2007/04/26 09:00:15  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/04/17 06:46:41  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/13 10:03:46  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/11 15:47:15  dparhomenko
 * PTR#0144983 optimization
 *
 */