/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl.cache;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.RowMap;

public abstract class AbstractCacheManager implements CacheManager{


    public RowMap loadOrGet(DatabaseConnection conn, String entityName, Long entityId) throws RepositoryException {
        /*RowMap result = get(entityName, entityId);
        result = null;
        if (result == null){*/
    		RowMap result = conn.loadRow(entityName, Constants.FIELD_ID, entityId);
            /*put(entityName, entityId, result);
        }*/
        return result;
    }
    
    public void putAll(String entityName, DatabaseSelectAllStatement st) throws RepositoryException{
        while (st.hasNext()){
            RowMap row = st.nextRow();
            Long id = (Long) row.get(Constants.FIELD_ID);
            put(entityName, id, row);
        }
    }
    

    public void evict(ArrayList<CacheKey> evictList) {
        for(CacheKey k : evictList){
            evict(k.getEntityName(), k.getEntityId());
        }
        
    }


}


/*
 * $Log: AbstractCacheManager.java,v $
 * Revision 1.1  2007/04/26 09:00:15  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.4  2006/11/14 07:37:22  dparhomenko
 * PTR#0148854 fix error occured after adding max_pos column
 *
 * Revision 1.3  2006/10/09 11:22:55  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.2  2006/04/20 11:42:52  zahars
 * PTR#0144983 Constants and JCRHelper moved to com.exigen.cm
 *
 * Revision 1.1  2006/04/17 06:46:41  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/04 11:46:12  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/03/27 14:27:23  dparhomenko
 * PTR#0144983 security
 *
 */