/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.util.Calendar;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;

/**
 * Base class for some FTS commands
 * 
 */
public abstract class FTSXCommand extends FTSCommand {

    //private static final Log log = LogFactory.getLog(FTSXCommand.class);

 

    
    /**
     * @see com.exigen.cm.cmd.fts.FTSCommand#buildSelectLockQuery()
     */
    protected DatabaseSelectAllStatement buildSelectLockQuery (){
        
        // builds SELECT FOR UPDATE statement
        // SELECT FOR UPDATE x.ID, x.CONTENT_DATA FROM CM_INDEXABLE_DATA x WHERE x.MIME_TYPE <> 'application/octet-stream' AND FTS_STAGE_ID is NULL
        //      AND RESERVED=FALSE AND OPERATION = 'INSERT' AND x.CONTENT_DATA NOT IN 
        //      ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        //      ORDER BY x.ID TOP 5
        
        DatabaseSelectAllStatement ds = super.buildSelectLockQuery();
        
        DatabaseSelectAllStatement innerSelect = DatabaseTools.createSelectAllStatement(Constants.TABLE_INDEXABLE_DATA,true);
        innerSelect.setRootAlias("y");
        innerSelect.addResultColumn(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA);
        innerSelect.addCondition(Conditions.eqProperty("x."+Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA,Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA));
        innerSelect.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__OPERATION,Constants.OPERATION_DELETE));
        
        
        //AND x.CONTENT_DATA NOT IN ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        //ds.addCondition(Conditions.notIn(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA, innerSelect));
        
        return ds;
        
    }
    
    /**
     * Builds statement that updates processed record in INDEXABLE_DATA
     * Command should subclass it to add more values
     * @param id
     * @return update statement
     */
    protected DatabaseUpdateStatement buildUpdateProcessedRecordStatement(Long id) {
        Calendar start = Calendar.getInstance(JCRHelper.getDBTimeZone());       
        Calendar finish = null;
        
        
        DatabaseUpdateStatement ds = new DatabaseUpdateStatement(Constants.TABLE_INDEXABLE_DATA);

        ds.addCondition(Conditions.eq(Constants.FIELD_ID,id));
        
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED,false));
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME,start));
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FINISH_TIME, finish));
        return ds;
    }
    
   
    
    

}


/*
 * $Log: FTSXCommand.java,v $
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/09/29 13:55:25  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.4  2006/08/15 08:38:00  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.3  2006/07/21 12:38:49  zahars
 * PTR#0144986 FreeReserved command introduced
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
 * Revision 1.2  2006/07/06 09:32:01  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/07/04 15:47:27  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */
