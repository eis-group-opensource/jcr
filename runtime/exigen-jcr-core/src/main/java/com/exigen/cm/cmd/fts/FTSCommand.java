/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.JCRHelper;
import com.exigen.cm.cmd.AbstractRepositoryCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.Order;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.ContentStoreConstants;
import com.exigen.cm.store.ContentStoreProvider;

/**
 * Base class for commands working on INDEXABLE_DATA table
 * 
 */
/**
 * TODO Put class description here
 * 
 */
/**
 * TODO Put class description here
 * 
 */
public abstract class FTSCommand extends AbstractRepositoryCommand {

    private static final Log log = LogFactory.getLog(FTSCommand.class);
    /**
     * Error type for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_TYPE_TXT_EXTRACTION = "TXT_EXTRACTION";
    /**
     * Error type for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_TYPE_TXT_INDEXING = "TXT_INDEXING";
    
    /**
     * Error type for record in FTS_INDEXING_ERROR
     */
    public static final String ERRROR_TYPE_FTS_PROCESSING = "FTS_PROCESSING";
    
    /**
     * Error code for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_CODE_TXT_EXTRACTION_FAILED ="TXT_EXTRACTION_FAILED";
    /**
     * Error code for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_CODE_TXT_ZIP_AND_MOVE_FAILED ="ZIP_AND_MOVE_FAILED";
    /**
     * Error code for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_CODE_TXT_CONVERT_AND_MOVE_FAILED ="CONVERT_AND_MOVE_FAILED";
    /**
     * Error code for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_CODE_TXT_NO_TEXT ="NO TEXT FOR INDEXING";
    
    /**
     * Error code for record in FTS_INDEXING_ERROR
     */
    public static final String ERROR_CODE_UNPROCESSED_FOUND = "RECORDS ARE NOT PROCESSED IN TIME";
    
    
    
    /**
     * Number of records processed from INDEXABLE_DATA by one command
     */
    protected  int ftsBatchSize = 10;
    
    /**
     * Structure of record in INDEXABLE_DATA
     * 
     */
    public static class IdData {
        /**
         * record id
         */
        public Long id;
        /**
         * store name and content id based encoded in one string
         */
        public String contentData;
        /**
         * corresponding id in FTS_DATA table. Could be null for 'DELETE' operation
         */
        public Long ftsDataId;
        /**
         * correponding id in FTS_STAGE table. Null if text not provided
         */
        public Long stageId;
        /**
         * MIME type. 
         */
        public String MIMEType;
        /**
         * Store name, decoded from contentData. It is not field of the table
         */
        public String storeName;
        /**
         * Content id in store, , decoded from contentData. It is not field of the table
         */
        public String storeContentId;
    }

    /**
     * Contains records from INDEXABLE_DATA that should be processed by the command
     */
    protected List <IdData> processingData = new LinkedList<IdData>();

    /**
     * true, if quantity of records found match batch size
     * false, if quantity of records found less than batch size 
     */
    protected boolean isBatchFull = false;
 
    private String getStoreName(String contentData) {
        int i = contentData.indexOf(Constants.STORE_DELIMITER);
        if ( i<=0 || i == contentData.length()-1){
            return ContentStoreConstants.DEFAULT_STORE_NAME;
        }
        return contentData.substring(0,i);
    }

    private String getContentStoreId(String contentData) {
        int i = contentData.indexOf(Constants.STORE_DELIMITER);
        if ( i<=0 ){
            return contentData;
        }
        return contentData.substring(i+1);
    }

    
    /**
     * Set batch size for FTS command
     * @param size
     */
    public void setFTSBatchSize(int size){
        ftsBatchSize = size;
    }
    
    /**
     * 
     * 
     */
    public boolean execute(DatabaseConnection connection)throws RepositoryException {
        ContentStoreProvider csp = getStoreProvider();
        DatabaseSelectAllStatement ds = buildSelectLockQuery();
        processSelectLockQuery(ds, connection, processingData);
        if (processingData.size() == ftsBatchSize)
            isBatchFull = true;
        else
            isBatchFull = false;
        process(processingData, connection, csp);
        return isBatchFull;
    }

    /**
     * Command specific processing
     * @param data
     * @param connection 
     * @param csp
     * @throws RepositoryException
     */
    protected abstract void process(List <IdData> data, DatabaseConnection connection,  ContentStoreProvider csp) throws RepositoryException;

    /**
     * Reads selected records from INDEXABLE_DATA  and "reserve" records
     * @param ds
     * @param connection
     * @param records
     * @throws RepositoryException
     */
    private void processSelectLockQuery(DatabaseSelectAllStatement ds,DatabaseConnection connection, List <IdData> records) throws RepositoryException {
    	records.clear();
        // start transaction
        ds.execute(connection);
        while (ds.hasNext()) { // process rows
            RowMap row = ds.nextRow();
            IdData data = new IdData();
            data.id = row.getLong(Constants.FIELD_ID);
            data.contentData = row
                            .getString(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA);
            data.ftsDataId = row
                            .getLong(Constants.TABLE_INDEXABLE_DATA__FTS_DATA_ID);
            data.stageId = row
                            .getLong(Constants.TABLE_INDEXABLE_DATA__FTS_STAGE_ID);
            data.MIMEType = row
                            .getString(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE);
            data.storeName = getStoreName(data.contentData);
            data.storeContentId = getContentStoreId(data.contentData);
            log.debug("Record found: id = " + data.id);
            records.add(data);
        }
        log.debug("records found: " + records.size());
        updateRecords(connection, records);
        connection.commit();
        ds.close();
        
    }
    /**
     * Reserve selected records and set times
     * @param connection
     * @param records
     * @throws RepositoryException
     */
    private void updateRecords(DatabaseConnection connection,List <IdData> records ) throws RepositoryException {
        // UPDATE CM_INDEXABLE_DATA SET RESERVED=?, START_TIME=?, PLANNED_FINISH_TIME=? WHERE ID IN (?,...,?) 
        
        if (records.size() == 0)
            return; // nothing to update
        Calendar start = Calendar.getInstance(JCRHelper.getDBTimeZone());
        Calendar finish = Calendar.getInstance(JCRHelper.getDBTimeZone());
        finish.add(Calendar.HOUR,1); // processing time. make a constant later
        
        DatabaseUpdateStatement ds = new DatabaseUpdateStatement(Constants.TABLE_INDEXABLE_DATA);
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__RESERVED,true));
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__TIME,start));
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__FINISH_TIME,finish));
        Collection<Long> inValues = new LinkedList<Long>();
        for (IdData idData: records){
            inValues.add(idData.id);
        }
        ds.addCondition(Conditions.in(Constants.FIELD_ID,inValues));
        ds.execute(connection);
        ds.close();
    }
 
    /**
     * Builds "SELECT FOR UPDATE" query based on OPERATION and other fields. Subclass should override this to add more conditions 
     * @return query
     */
    protected DatabaseSelectAllStatement buildSelectLockQuery (){
        
        // builds SELECT FOR UPDATE statement   -- for example 
        // SELECT FOR UPDATE x.ID, x.CONTENT_DATA FROM CM_INDEXABLE_DATA x WHERE x.MIME_TYPE <> 'application/octet-stream' AND FTS_STAGE_ID is NULL
        //      AND RESERVED=FALSE AND OPERATION = 'INSERT' AND x.CONTENT_DATA NOT IN 
        //      ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        //      ORDER BY x.ID TOP 5
        
        DatabaseSelectAllStatement select = DatabaseTools.createSelectAllStatement(Constants.TABLE_INDEXABLE_DATA,true);
        select.setRootAlias("x");
        select.setLockForUpdate(true);
        select.setMaxResult(ftsBatchSize);  // !!! should be a parameter
        select.addResultColumn(Constants.FIELD_ID);
        select.addResultColumn(Constants.TABLE_INDEXABLE_DATA__CONTENT_DATA);
        select.addResultColumn(Constants.TABLE_INDEXABLE_DATA__FTS_DATA_ID);
        select.addResultColumn(Constants.TABLE_INDEXABLE_DATA__FTS_STAGE_ID);
        select.addResultColumn(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE);

        
        //AND RESERVED=FALSE 
        select.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__RESERVED, false));
        select.addOrder(Order.asc(Constants.FIELD_ID));
        
        return select;
        
    }
    
    /**
     * Adds record to FTS_INDEXING_ERROR table and delete record from INDEXABLE_DATA
     * @param conn
     * @param record
     * @param errorType
     * @param errorCode
     * @param comment
     * @throws RepositoryException
     */
    public static  void reportErrorDeleteRecord(DatabaseConnection conn, IdData record, String errorType, String errorCode, String comment) throws RepositoryException{

        _reportError(conn, record.ftsDataId, errorType, errorCode, comment );
        // delete from CM_INDEXABLE_DATA
        DatabaseDeleteStatement ds2 = new DatabaseDeleteStatement(Constants.TABLE_INDEXABLE_DATA);
        ds2.addCondition(Conditions.eq(Constants.FIELD_ID,record.id));
        ds2.execute(conn);
        ds2.close();
        
//        conn.commit();
    }
    
    private static void _reportError(DatabaseConnection conn, Long id, String errorType, String errorCode, String comment) throws RepositoryException{
        // insert into FTS_INDEXING_ERROR
        DatabaseInsertStatement ds = new DatabaseInsertStatement(Constants.TABLE_FTS_INDEXING_ERROR);
        ds.addValue(SQLParameter.create(Constants.FIELD_ID, id));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_INDEXING__ERROR_TYPE, errorType));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_INDEXING__ERROR_CODE, errorCode));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_INDEXING__COMMENT, comment));
        ds.execute(conn);
        ds.close();
        
    }

    /**
     * Adds record to FTS_INDEXING_ERROR table
     * @param conn
     * @param errorType
     * @param errorCode
     * @param comment
     * @throws RepositoryException
     */
    public static  void reportError(DatabaseConnection conn, String errorType, String errorCode, String comment) throws RepositoryException{
        Long id = conn.nextId();
        _reportError(conn, id, errorType, errorCode, comment);
        conn.commit();
    }
    
    
}


/*
 * $Log: FTSCommand.java,v $
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.11  2006/11/10 12:18:42  zahars
 * PTR #1803381 transaction demarcation fixed for stored procedures
 *
 * Revision 1.10  2006/11/09 13:44:52  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.9  2006/10/09 11:22:48  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.8  2006/09/29 09:26:34  zahars
 * PTR#1802683  Commands uses connection instead of connection provider
 *
 * Revision 1.7  2006/09/28 12:39:46  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.6  2006/08/15 08:38:00  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.5  2006/07/21 12:38:49  zahars
 * PTR#0144986 FreeReserved command introduced
 *
 * Revision 1.4  2006/07/19 10:31:17  zahars
 * PTR#0144986 Oracle index refresh interval introduced
 *
 * Revision 1.3  2006/07/17 14:47:47  zahars
 * PTR#0144986 Cleanup
 *
 * Revision 1.2  2006/07/17 09:07:00  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.1  2006/07/14 08:21:29  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.10  2006/07/13 13:01:27  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.9  2006/07/12 12:33:07  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.8  2006/07/12 11:51:16  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.7  2006/07/11 13:58:25  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.6  2006/07/11 12:15:35  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.5  2006/07/10 12:05:48  zahars
 * PTR#0144986 MIMECommand updated
 *
 * Revision 1.4  2006/07/07 15:00:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.3  2006/07/06 09:32:01  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/06 07:58:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.1  2006/07/04 15:47:27  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */