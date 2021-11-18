/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.oracle;

import java.util.Date;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.cmd.fts.FTSCommand.IdData;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.IndexingProcessor;


/**
 * Specific actions on FTS indexing for Oracle - refresh FTS indexes 
 * 
 */
public class OracleIndexingProcessor extends IndexingProcessor {

    private static final long  REFRESH_INTERVAL= 30L*60L*1000L; // refresh index interval in milliseconds
    private static final Log log = LogFactory.getLog(OracleIndexingProcessor.class);
    
    /**
     * 
     * @see com.exigen.cm.database.dialect.IndexingProcessor#process(java.util.List, com.exigen.cm.database.DatabaseConnection)
     */
    private long lastIndexTime = new Date().getTime();
    
    public void process(List<IdData> records, DatabaseConnection connection, boolean isBatchFull) throws RepositoryException {
        super.process(records, connection, isBatchFull);
        // update FTS index
        OracleDatabaseDialect dialect = (OracleDatabaseDialect)connection.getDialect();
        
        // index update is expensive operation
        // we update indexes if there are no more records to process or time period passed
        if (!isBatchFull || (new Date().getTime() - lastIndexTime > REFRESH_INTERVAL) ){
            dialect.updateFTSIndexes(connection);
            lastIndexTime = new Date().getTime();
            log.debug("Indexes refreshed");
        }    
    }

}


/*
 * $Log: OracleIndexingProcessor.java,v $
 * Revision 1.1  2007/04/26 08:59:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/08/15 08:38:07  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.7  2006/07/19 10:31:18  zahars
 * PTR#0144986 Oracle index refresh interval introduced
 *
 * Revision 1.6  2006/07/17 09:07:05  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.5  2006/07/14 11:54:27  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/14 08:21:37  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.3  2006/07/12 12:34:12  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.2  2006/07/12 10:10:21  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/11 12:16:03  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */