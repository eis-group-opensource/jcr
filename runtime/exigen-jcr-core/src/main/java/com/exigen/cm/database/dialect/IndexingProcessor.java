/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand.IdData;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.condition.Conditions;

/**
 * Executes FTS indexing.
 * The DatabaseDialect returns proper processor for each database
 * It class should be extended by dialect specific processors, if needed
 * This class simply deletes all records 
 * 
 */
public class IndexingProcessor {
    /**
     * Process records from CM_INDEXABLE_DATA with OPERATION=TEXT_EXTRACTED 
     *  (simply deletes these records)
     * @param records
     * @param connection
     * @param isBatchFull 
     * @throws RepositoryException
     */
    @SuppressWarnings("unused")
    public void process(List<IdData> records, DatabaseConnection connection, boolean isBatchFull) throws RepositoryException {
     // default behaviour - delete records from CM_INDEXABLE_DATA   
    	if (records.size() == 0){
    		return;
    	}
    	DatabaseDeleteStatement ds = new DatabaseDeleteStatement(Constants.TABLE_INDEXABLE_DATA);
        Collection<Long> inValues = new LinkedList<Long>();
        for (IdData idData: records){
            inValues.add(idData.id);
        }
        ds.addCondition(Conditions.in(Constants.FIELD_ID,inValues));
        ds.execute(connection);
        ds.close();
         
    }
    
    
    
}


/*
 * $Log: IndexingProcessor.java,v $
 * Revision 1.1  2007/04/26 09:00:06  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.9  2006/07/19 10:31:16  zahars
 * PTR#0144986 Oracle index refresh interval introduced
 *
 * Revision 1.8  2006/07/17 09:07:02  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.7  2006/07/14 11:54:22  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.6  2006/07/14 08:21:32  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.5  2006/07/13 13:01:26  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/12 12:34:06  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.3  2006/07/11 12:15:49  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.2  2006/07/10 12:06:08  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/07 15:00:12  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */