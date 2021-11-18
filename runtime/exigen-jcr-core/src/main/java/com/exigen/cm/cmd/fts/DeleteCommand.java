/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.util.List;

import javax.jcr.RepositoryException;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.ContentStoreProvider;

/**
 * DeleteCommand 
 * 1. delete content from store
 * 2. delete record from FTS_INDEXABLE_DATA
 * 3. delete record from FTS_DATA (if any)
 * 4. refresh indexes - dialect specific operation
 * 
 */
public class DeleteCommand extends FTSCommand {
    
    @Override
    protected void process(List<IdData> data, DatabaseConnection connection,
                    ContentStoreProvider csp) throws RepositoryException {
        DatabaseDialect dialect = connection.getDialect();
            dialect.getDeleteProcessor().process(data, connection, csp);
//            connection.commit();

    }
    
    protected DatabaseSelectAllStatement buildSelectLockQuery (){
        
        // builds SELECT FOR UPDATE statement
        // SELECT FOR UPDATE x.ID, x.CONTENT_DATA FROM CM_INDEXABLE_DATA x WHERE 
        //      AND RESERVED=FALSE AND OPERATION = 'DELETE' AND x.CONTENT_DATA NOT IN 
        //      ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and RESERVED = TRUE )
        //      ORDER BY x.ID TOP 5
        
        DatabaseSelectAllStatement ds = super.buildSelectLockQuery();
        
        DatabaseSelectAllStatement innerSelect = DatabaseTools.createSelectAllStatement(Constants.TABLE_INDEXABLE_DATA,true);
        innerSelect.setRootAlias("y");
        innerSelect.addResultColumn(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA);
        innerSelect.addCondition(Conditions.eqProperty("x."+Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA,Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA));
        innerSelect.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__RESERVED,true));
        
        
        //AND x.CONTENT_DATA NOT IN ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        ds.addCondition(Conditions.notIn(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, innerSelect));
        ds.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__OPERATION,Constants.OPERATION_DELETE));
        return ds;
        
    }
    
    public String getDisplayableName(){
        return "Delete";
    }

}


/*
 * $Log: DeleteCommand.java,v $
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/10/09 11:22:48  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.5  2006/09/29 13:55:25  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.4  2006/09/29 09:26:34  zahars
 * PTR#1802683  Commands uses connection instead of connection provider
 *
 * Revision 1.3  2006/07/18 12:51:12  zahars
 * PTR#0144986 INFO log level improved
 *
 * Revision 1.2  2006/07/17 09:07:00  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.1  2006/07/14 08:21:29  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.3  2006/07/14 08:11:39  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.2  2006/07/10 12:06:16  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/07 15:00:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */
