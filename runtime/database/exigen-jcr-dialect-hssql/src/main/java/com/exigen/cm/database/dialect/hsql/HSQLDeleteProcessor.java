/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.hsql;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand.IdData;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DeleteProcessor;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.ContentStoreProvider;



/**
 * Specific actions on content delete operation
 * 
 */
public class HSQLDeleteProcessor extends DeleteProcessor {
    
    /**
     * Delete records from INDEX_ENTRY (word index)
     * I do not delete unreferenced words in WORD to optimize performance
     * @see com.exigen.cm.database.dialect.DeleteProcessor#process(java.util.List, com.exigen.cm.database.DatabaseConnection, com.exigen.cm.store.ContentStoreProvider)
     */
    public void process(List<IdData> records, DatabaseConnection connection,
                    ContentStoreProvider csp) throws RepositoryException {
        Collection<Long> inValues1 = new LinkedList<Long>();
        for (IdData record: records){
            if (record.ftsDataId !=null)
                inValues1.add(record.ftsDataId);
        }
        if (inValues1.size() > 0) {
            DatabaseDeleteStatement ds1 = new DatabaseDeleteStatement(Constants.TABLE_INDEX_ENTRY);
            ds1.addCondition(Conditions.in(Constants.TABLE_INDEX_ENTRY__DATA_ID, inValues1));
            ds1.execute(connection);
            ds1.close();
        }    
        
        super.process(records, connection, csp);
    }    

}


/*
 * $Log: HSQLDeleteProcessor.java,v $
 * Revision 1.1  2007/04/26 09:00:20  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/08/15 11:31:13  zahars
 * PTR#0144986 No delete items fixed
 *
 * Revision 1.5  2006/07/17 09:07:03  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.4  2006/07/14 11:54:25  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/07/14 11:52:41  zahars
 * PTR#0144986 FTSDataId could be null
 *
 * Revision 1.2  2006/07/14 08:21:35  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.1  2006/07/13 08:52:55  zahars
 * PTR#0144986 Hypersonic DeleteIndex implemented
 *
 */