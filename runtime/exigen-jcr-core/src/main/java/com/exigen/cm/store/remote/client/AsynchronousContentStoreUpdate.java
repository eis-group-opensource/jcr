/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.sql.PreparedStatement;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.store.StoreHelper;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.common.BatchUnit;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;

/**
 * Implements Asynchronous Content Store Update. Responsible
 * for scheduling of modifications specified in Batch for later upload.
 */
class AsynchronousContentStoreUpdate extends ContentStoreUpdate {
    
    private static final Log log = LogFactory.getLog(AsynchronousContentStoreUpdate.class);
    
    /**
     * Holds asynchronous update status values.
     * @author Maksims
     *
     */
    public enum STATUS {SCHEDULED, IN_PROGRESS, ERROR};
    
    
//    /**
//     * Name of property holding number of hours allowed for single
//     * content related operation to complete.
//     */
//    public static final String PROPERTY_EXEC_TIME="execTime";
//
//    /**
//     * Default allowed execution time is 24 hours
//     */
//    public static final int DEFAULT_EXEC_TIME=24;
//    private long execTime = 24*3600*1000;
    
    AsynchronousContentStoreUpdate(String store, String instanceName, TransportAdapterClient ta, Map<String, String> configuration) {
        super(store, instanceName, ta, configuration);
//        String execTimeStr = (String)configuration.get(PROPERTY_EXEC_TIME);
//        if(execTimeStr != null)
//            execTime = Long.parseLong(execTimeStr) * 3600*1000;
    }


    @Override
    public void process(DatabaseConnection connection, Batch batch, BatchOperation opFilter) {
        
        PreparedStatement statement = null;
        
        try{
            
            DatabaseInsertStatement insertSt = DatabaseTools.createInsertStatement(Constants.TABLE_CONTENT_SCHEDULE);
            Iterator<BatchUnit> units = batch.getUnitsIterator();
            while(units.hasNext()){
                BatchUnit unit = units.next();
                if(unit.isProcessed() || opFilter != null && unit.getOperation() != opFilter)
                    continue;
                
                insertSt.addValue(SQLParameter.create(Constants.FIELD_ID, connection.nextId()));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID,unit.getJCRContentId()));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME, getJcrInstanceName()));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__STORE_NAME, getStoreName()));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__OPERATION, unit.getOperation().ordinal()));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.SCHEDULED.ordinal()));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__PARAMS,StoreHelper.mapToPropertiesString("Parameters for content ID " + unit.getJCRContentId(), unit.getParams())));
                insertSt.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__LENGTH, unit.getLength()));
                
                insertSt.addBatch();
            }
            
            insertSt.execute(connection);
        }catch(Exception ex){
            String message = "Failed to schedule asynchronous update";
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }finally{
            if(statement != null)
                connection.closeStatement(statement);
        }
        
        
        Iterator<BatchUnit> units = batch.getUnitsIterator();            
        while(units.hasNext()){
            BatchUnit unit = units.next();
            if(unit.isProcessed() || opFilter != null && unit.getOperation() != opFilter)
                continue;
            unit.setProcessed(true);
        }
        
        
    }

}

/*
 * $Log: AsynchronousContentStoreUpdate.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.11  2006/09/29 13:55:34  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.10  2006/08/14 16:18:38  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.9  2006/08/08 13:10:40  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.8  2006/08/02 11:42:31  maksims
 * #1802426 SQL Framework used to generate queries
 *
 * Revision 1.7  2006/07/28 15:49:09  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.6  2006/07/12 11:51:14  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.5  2006/07/06 16:43:11  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 * Revision 1.4  2006/07/06 10:52:45  maksims
 * #1802356 Content STORE NAME added to CONTENT_SCHEDULE table
 *
 * Revision 1.3  2006/07/06 08:52:00  maksims
 * #1802356 Content ID added to CONTENT_SCHEDULE table
 *
 * Revision 1.2  2006/07/06 08:22:45  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.1  2006/07/04 14:04:43  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */