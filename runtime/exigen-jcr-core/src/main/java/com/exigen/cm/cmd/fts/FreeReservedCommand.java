/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.cmd.AbstractRepositoryCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;

/**
 * Free reserved records in INDEXABLE_DATA (set RESERVED = false)
 * if current time is more that PLANNED_FINISHED_TIME
 * 
 */
public class FreeReservedCommand extends AbstractRepositoryCommand {

    static Log log = LogFactory.getLog(FreeReservedCommand.class);
    
    
    @Override
    public String getDisplayableName() {
        return "Free reserved";
    }

    public boolean execute(DatabaseConnection connection) throws RepositoryException {

        Calendar time = Calendar.getInstance(JCRHelper.getDBTimeZone());
        Calendar finish = null;
        // UPDATE CM_INDEXABLE_DATA set START_TIME = ?, FINISH_TIME=?,
        // RESERVED=? WHERE PLANNED_FINISH_TIME>?
        DatabaseUpdateStatement ds = new DatabaseUpdateStatement(
                        Constants.TABLE_INDEXABLE_DATA);
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME,
                        time));
        ds.addValue(SQLParameter.create(
                        Constants.TABLE_INDEXABLE_DATA__FINISH_TIME, finish));
        ds.addValue(SQLParameter.create(
                        Constants.TABLE_INDEXABLE_DATA__RESERVED, false));
        ds.addCondition(Conditions.lt(
                        Constants.TABLE_INDEXABLE_DATA__FINISH_TIME, time)); 
        
        ds.execute(connection);
        connection.commit();
        int count = ds.getUpdateCount();
        if (count > 0) { // there are unprocessed records
            FTSCommand.reportError(connection,
                            FTSCommand.ERRROR_TYPE_FTS_PROCESSING,
                            FTSCommand.ERROR_CODE_UNPROCESSED_FOUND,
                            "records: " + count);
            log.error("Expired processing records found: " + count);
        }
        return false;
    }

}


/*
 * $Log: FreeReservedCommand.java,v $
 * Revision 1.2  2008/08/28 09:56:09  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/11/09 13:44:52  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.5  2006/09/29 09:26:34  zahars
 * PTR#1802683  Commands uses connection instead of connection provider
 *
 * Revision 1.4  2006/09/28 12:39:46  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.3  2006/08/15 08:38:00  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.2  2006/07/26 14:02:08  maksims
 * #1802414 bad import removed
 *
 * Revision 1.1  2006/07/21 12:38:49  zahars
 * PTR#0144986 FreeReserved command introduced
 *
 */
