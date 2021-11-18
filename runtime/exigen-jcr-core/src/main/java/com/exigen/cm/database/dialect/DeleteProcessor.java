/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand.IdData;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreProvider;


/**
 * 1. delete content from store
 * 2. delete record from FTS_INDEXABLE_DATA
 * 3. delete record from FTS_DATA
 * 
 * This class should be subclassed if DB specific action 
 * on updating index are needed
 */
public class DeleteProcessor {
    
    private static final Log log = LogFactory.getLog(DeleteProcessor.class);

    
    /**
     * 1. delete content from store
     * 2. delete record from FTS_INDEXABLE_DATA
     * 3. delete record from FTS_DATA
     * @param records
     * @param connection
     * @param csp
     * @throws RepositoryException
     */
    
    public void process(List<IdData> records, DatabaseConnection connection,
                    ContentStoreProvider csp) throws RepositoryException {
        if (records.size() == 0){
        	return;
        }
        for (IdData record: records){
                // delete from store
                // TODO let store delete collection - could be optimal
            ContentStore store = csp.getStore(record.storeName);
            try {
                store.begin(connection);
                store.remove(record.storeContentId);
                store.commit();
            } catch (Throwable te){
                log.error("failed to delete from store", te);
            }
        }
                
      // delete from FTS_INDEXABLE_DATA by content_data - all records
        Collection<String> inValues1 = new LinkedList<String>();
        for (IdData record: records){
            inValues1.add(record.contentData);
        }
        DatabaseDeleteStatement ds1 = new DatabaseDeleteStatement(Constants.TABLE_INDEXABLE_DATA);
        ds1.addCondition(Conditions.in(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, inValues1));
        ds1.execute(connection);
        ds1.close();
                
                // delete from FTS_DATA
        Collection<Long> inValues2 = new LinkedList<Long>();
        for (IdData record: records){
            if (record.ftsDataId != null)
                inValues2.add(record.ftsDataId); 
        }
        if (inValues2.size() > 0){
            DatabaseDeleteStatement ds2 = new DatabaseDeleteStatement(Constants.TABLE_FTS_DATA);
            ds2.addCondition(Conditions.in(Constants.FIELD_ID, inValues2));
            ds2.execute(connection);
            ds2.close();
        }
        connection.commit();
    }
    

}


/*
 * $Log: DeleteProcessor.java,v $
 * Revision 1.1  2007/04/26 09:00:06  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.9  2006/10/09 11:22:51  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.8  2006/09/29 09:26:37  zahars
 * PTR#1802683  Commands uses connection instead of connection provider
 *
 * Revision 1.7  2006/09/22 10:11:02  zahars
 * PTR#0148427   case when all ftsDataId ==null fixed
 *
 * Revision 1.6  2006/07/17 09:07:02  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.5  2006/07/14 11:54:22  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/14 11:52:28  zahars
 * PTR#0144986 FTSDataId could be null
 *
 * Revision 1.3  2006/07/14 08:21:32  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.2  2006/07/10 12:06:08  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/07 15:00:12  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */