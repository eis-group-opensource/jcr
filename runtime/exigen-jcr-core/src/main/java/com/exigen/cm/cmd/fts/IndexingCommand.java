/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.ContentStoreProvider;

/**
 * Executes words indexing for FTS.
 * Indexing command depends on Database (dialect)
 * If DB supports FTS, it delegates it to DB, otherwise build index as DB table
 * 
 * 
 */
public class IndexingCommand extends FTSXCommand {


    @Override
    protected void process(List<IdData> data, DatabaseConnection connection,
                    ContentStoreProvider csp) throws RepositoryException {
        DatabaseDialect dialect = connection.getDialect();
        dialect.getIndexingProcessor().process(data, connection, isBatchFull);
//        connection.commit();
    }
    
    protected DatabaseSelectAllStatement buildSelectLockQuery (){
        
        // builds SELECT FOR UPDATE statement
        // SELECT FOR UPDATE x.ID, x.CONTENT_DATA FROM CM_INDEXABLE_DATA x WHERE x.MIME_TYPE <> 'application/octet-stream' 
        //      AND RESERVED=FALSE AND OPERATION = 'INSERT' AND x.CONTENT_DATA NOT IN 
        //      ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        //      ORDER BY x.ID TOP 5
        DatabaseSelectAllStatement ds = super.buildSelectLockQuery();
        
        
        //AND OPERATION = 'TEXT_EXSTRACTED' 
        ds.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_TEXT_EXTRACTED));
        
        return ds;
    }
    
    public String getDisplayableName(){
        return "Indexing";
    }
    

}


/*
 * $Log: IndexingCommand.java,v $
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.21  2006/10/09 11:22:48  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.20  2006/09/29 09:26:34  zahars
 * PTR#1802683  Commands uses connection instead of connection provider
 *
 * Revision 1.19  2006/07/19 10:31:17  zahars
 * PTR#0144986 Oracle index refresh interval introduced
 *
 * Revision 1.18  2006/07/18 12:51:12  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.17  2006/07/17 09:07:00  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.16  2006/07/14 08:21:29  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.4  2006/07/14 08:11:39  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.3  2006/07/12 12:33:07  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.2  2006/07/10 12:06:16  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/07 15:00:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */
