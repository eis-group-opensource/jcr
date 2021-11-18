/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.io.ByteArrayInputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.AbstractRepositoryCommand;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.Order;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.DatabaseCondition;
import com.exigen.cm.store.remote.client.AsynchronousContentStoreUpdate.STATUS;
import com.exigen.cm.store.remote.client.ContentStoreProxy.CachedContentDataSource;
import com.exigen.cm.store.remote.common.Batch;
import com.exigen.cm.store.remote.common.BatchOperation;
import com.exigen.cm.store.remote.common.BatchUnit;
import com.exigen.cm.store.remote.common.ContentDataSource;
import com.exigen.cm.store.remote.transport.TransportAdapterClient;

/**
 * Command performing asynchronous update of Content Stores using
 * content update schedule in JCR DB
 */
public class AsynchronousContentStoreUpdater extends AbstractRepositoryCommand {

    private static final Log log = LogFactory.getLog(AsynchronousContentStoreUpdater.class);
    
    /**
     * Name of property holding number of hours allowed for single
     * content related operation to complete.
     */
    public static final String PROPERTY_EXEC_TIME="execTime";
    
    /**
     * Property name for batch size
     */
    public static final String PROPERTY_BATCH_SIZE = "batchSize";

    
    /**
     * Property name for jcr instance names given updater executed for.
     */
    public static final String PROPERTY_TARGETS = "targets";    
    
    /*
     * Template for query generation 
     */
    
//    private static final MessageFormat QUERY_INSERT_REMOVE_TEMPLATE = new MessageFormat(
//                "DELETE FROM {0}"+
//                " WHERE "+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" IN ("+
//                " SELECT sched1."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID +
//                " FROM {0} sched1, {0} sched2"+
//                " WHERE   sched1."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" = sched2."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+
//                    " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__OPERATION+"=?"+
//                    " and sched2."+Constants.TABLE_CONTENT_SCHEDULE__OPERATION+"=?"+
//                    " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME+" IN ( {1} )"+
//                    " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=?"+
//                    " and sched2."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=?)");
    
    
    /*
     * Select those which:
     * 1. belongs to JCR_INSTANCE
     * 2. content ID scheduled and not in progress
     * 3. content ID not scheduled for consequent insert-remove
     * 
     * SELECT ID
     * FROM CMCS_CONTENT_SCHEDULE sched
     * WHERE
     *  sched.INSTANCE_NAME IN ('aaa','bbb', ...)
     *  AND (sched.STATUS=<SCHEDULED> OR sched.STATUS=<IN_PROGRESS> AND sched.END_TIME< <Current Time>)
     *  AND NOT EXISTS (SELECT * 
     *              FROM CMCS_SCHEDULE sched0 
     *              WHERE sched0.CONTENT_ID=sched.CONTENT_ID AND 
     *                    sched0.STATUS=<IN_PROGRESS>) AND
     *  sched.CONTENT_ID NOT IN(
     *      SELECT sched1.CONTENT_ID 
     *      FROM CMCS_SCHEDULE sched1, CMCS_SCHEDULE sched2
     *      WHERE   sched1.INSTANCE_NAME=sched.INSTANCE_NAME
     *              AND sched1.CONTENT_ID= sched2.CONTENT_ID
     *              AND sched1.OPERATION=<INSERT>
     *              AND sched2.OPERATION=<REMOVE>
     *              AND sched1.STATUS=<SCHEDULED>
     *              AND sched2.STATUS=<SCHEDULED>)
     *  ORDER BY sched.CONTENT_ID ASC
     * 
     * 
     * 
     */
     /*
    private static final MessageFormat QUERY_SELECT_RESERVE_TEMPLATE = new MessageFormat(
            "SELECT "+Constants.FIELD_ID+" FROM {0} sched"+
            " WHERE "
            + "sched."+Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME+" IN ( {1} ) "
            + "AND (sched."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=? OR "
            + " sched."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=? AND "
            + " sched."+Constants.TABLE_CONTENT_SCHEDULE__END_TIME+"<?) AND NOT EXISTS( SELECT * FROM {0} sched0 WHERE sched0."
            + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID
            + "=sched."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID
            + " AND sched0."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=?) AND sched."
            + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" NOT IN ("+
            " SELECT sched1."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID +
            " FROM {0} sched1, {0} sched2"+
            " WHERE   " 
                + " sched1."+Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME+"=sched."
                + Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME+" AND "
                + "sched1."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" = sched2."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+
                " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__OPERATION+"=?"+
                " and sched2."+Constants.TABLE_CONTENT_SCHEDULE__OPERATION+"=?"+
                " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=?"+
                " and sched2."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+
                "= sched1."+Constants.TABLE_CONTENT_SCHEDULE__STATUS +
                ") ORDER BY sched.CONTENT_ID ASC");

    
//    private String queryRemoveInsert;
    private String querySelectReserved;
//*/    
    
//    private ContentStoreProvider contentStoreProvider;
//    private ConnectionProvider connectionProvider;
    private CSPCacheManager cacheManager;
    private Map<String, TransportAdapterClient> asynchStores;
    private List<String> jcrInstances;
    private int batchSize = 30;

    


    /**
     * Default allowed execution time is 24 hours
     */
    public static final int DEFAULT_EXEC_TIME=24;
    private long execTime = 24*3600*1000;


    /**
     * Holds instance properties.
     */
    private final Map<String, String> properties;

    
    public AsynchronousContentStoreUpdater(){
        this(new HashMap<String, String>());
    }
    
    public AsynchronousContentStoreUpdater(Map<String, String> properties){
        super();
        this.properties= properties;
        
    }
    
    
    @Override
    public boolean init() throws RepositoryException {
        super.init();
        
        cacheManager = (CSPCacheManager)getStoreProvider().getCacheManager();
        ContentStoreUpdateManager updateManager = (ContentStoreUpdateManager)getStoreProvider().getContentStoreUpdateManager();
        asynchStores = updateManager.getAsyncStoreTransport();

        
        
        String jcris = properties.get(PROPERTY_TARGETS);
        jcrInstances = new LinkedList<String>();
        if(jcris == null)
            jcrInstances.add(getStoreProvider().getJCRInstanceName());
        else{
            StringTokenizer st = new StringTokenizer(jcris, ",");
            while(st.hasMoreTokens())
                jcrInstances.add(st.nextToken().trim());
        }
        StringBuffer jcrInstanceNamePH = new StringBuffer();
        for(int i=0; i<jcrInstances.size();i++){
            jcrInstanceNamePH.append('?');
            if(i>0)
                jcrInstanceNamePH.append(',');
        }
            
        String bsVal = properties.get(PROPERTY_BATCH_SIZE);
        if(bsVal != null)
            batchSize = Integer.parseInt(bsVal);
        
//        DatabaseDialect dialect = connectionProvider.getDialect();
//        queryRemoveInsert = QUERY_INSERT_REMOVE_TEMPLATE.format(
//                new Object[]{dialect.convertTableName(Constants.TABLE_CONTENT_SCHEDULE), 
//                        jcrInstanceNamePH.toString()});   

//        querySelectReserved = QUERY_SELECT_RESERVE_TEMPLATE.format(
//                new Object[]{dialect.convertTableName(Constants.TABLE_CONTENT_SCHEDULE), 
//                        jcrInstanceNamePH.toString()});
//        
//        StringBuffer tmp = new StringBuffer(querySelectReserved);
//        dialect.limitResults(tmp, batchSize, false);
//        querySelectReserved = tmp.toString();
        
        String execTimeStr = properties.get(PROPERTY_EXEC_TIME);
        if(execTimeStr != null)
            execTime = Long.parseLong(execTimeStr) * 3600*1000;
        
        return true;
        
    }


    /**
     * @inheritDoc
     */
    public boolean execute() throws RepositoryException {
        
        DatabaseConnection connection = null;
        try{
            connection = getConnectionProvider().createConnection();
            boolean fullBatch =  execute(connection);
            connection.commit();
            return fullBatch;
        }finally{
            if(connection != null)
                connection.close();
        }
    }
    
    /**
     * Responsible for:
     * 1. Removing upload-remove record for same content
     * 2. Scheduling batches for upload
     */
    public boolean execute(DatabaseConnection connection) throws RepositoryException {
        try{
            if(log.isDebugEnabled()){
                String message = MessageFormat.format("Starting asynchronous Content update for JCR Instances: {0}",
                                jcrInstances);
                log.debug(message);
            }
            
            
            cleanInsertRemove(connection);
            List<Batch> batches = new LinkedList<Batch>();
            int count = reserveUnits(connection, batches);
            processBatches(connection, batches);

            return count == batchSize;
        }catch(Exception ex){
            String message = "Failed to execute asynchronous update.";
            log.error(message, ex);
            throw new RepositoryException(message,ex);
        }
    }

    /**
     * Collects batches from CMCS_CONTENT_SCHEDULE
     * @param connection
     * @param batchSize
     * @return
     * @throws Exception
     */
    protected int reserveUnits(DatabaseConnection connection, List<Batch> batches) throws Exception{
        Map<String, List<BatchUnit>> storeBatchUnits = new HashMap<String, List<BatchUnit>>();
        Map<String, CSPCache> caches = new HashMap<String, CSPCache>();
        
//        DatabaseDialect dialect = connection.getDialect();
//        PreparedStatement select = connection.prepareStatement(querySelectReserved, true);
        int count = 0;
    /*
     * SELECT ID
     * FROM CMCS_CONTENT_SCHEDULE sched
     * WHERE
     *  sched.INSTANCE_NAME IN ('aaa')
     *  AND (sched.STATUS=<SCHEDULED> OR sched.STATUS=<IN_PROGRESS> AND sched.END_TIME< <Current Time>)
     *  AND NOT EXISTS (SELECT * 
     *              FROM CMCS_SCHEDULE sched0 
     *              WHERE sched0.CONTENT_ID=sched.CONTENT_ID AND 
     *                    sched0.STATUS=<IN_PROGRESS>) 
     *  AND
     *  sched.CONTENT_ID NOT IN(
     *      SELECT sched1.CONTENT_ID 
     *      FROM CMCS_SCHEDULE sched1, CMCS_SCHEDULE sched2
     *      WHERE   sched1.INSTANCE_NAME=sched.INSTANCE_NAME
     *              AND sched1.CONTENT_ID= sched2.CONTENT_ID
     *              AND sched1.OPERATION=<INSERT>
     *              AND sched2.OPERATION=<REMOVE>
     *              AND sched1.STATUS=<SCHEDULED>
     *              AND sched2.STATUS=sched1.STATUS)
     *  ORDER BY sched.CONTENT_ID ASC
     *  
            SELECT "+Constants.FIELD_ID
            FROM {0} sched
            WHERE 
            + "sched."+Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME+" IN ( {1} ) "
            + "AND (sched."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=? OR "
            + " sched."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=? AND "
            + " sched."+Constants.TABLE_CONTENT_SCHEDULE__END_TIME+"<?) 
            
            AND NOT EXISTS( 
                SELECT * FROM {0} sched0 WHERE 
                sched0.Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID=sched. Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID
                AND sched0.Constants.TABLE_CONTENT_SCHEDULE__STATUS+=?) 
            
            AND sched." + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" NOT IN ("+
            " SELECT sched1."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID +
            " FROM {0} sched1, {0} sched2"+
            " WHERE   " 
                + "sched1."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+" = sched2."+Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID+
                +" AND "
                + " sched1."+Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME+"=sched.Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME                
                " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__OPERATION+"=?"+
                " and sched2."+Constants.TABLE_CONTENT_SCHEDULE__OPERATION+"=?"+
                " and sched1."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+"=?"+
                " and sched2."+Constants.TABLE_CONTENT_SCHEDULE__STATUS+
                "= sched1."+Constants.TABLE_CONTENT_SCHEDULE__STATUS +
                ") 
            ORDER BY sched.CONTENT_ID ASC;
     *  
     */
        String mainAlias = "sched";
        DatabaseSelectAllStatement main = DatabaseTools.createSelectAllStatement(Constants.TABLE_CONTENT_SCHEDULE, false);
        main.setRootAlias(mainAlias);
        main.addCondition(Conditions.in(mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME, jcrInstances));

        DatabaseCondition newCondition = Conditions.eq(mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.SCHEDULED.ordinal());        
        DatabaseCondition expiredCondition = Conditions.and(
                Conditions.eq(mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.IN_PROGRESS.ordinal()),
                Conditions.lt(mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__END_TIME, GregorianCalendar.getInstance()
                        ));
        main.addCondition(Conditions.or(newCondition, expiredCondition));
        
        
        String notExistsAlias = "sched0";
        DatabaseSelectAllStatement notExists = DatabaseTools.createSelectAllStatement(Constants.TABLE_CONTENT_SCHEDULE, false);
        notExists.setRootAlias(notExistsAlias);
        notExists.addCondition(Conditions.eqProperty(notExistsAlias+ "." + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID,
                mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID));
        notExists.addCondition(Conditions.eq(notExistsAlias+ "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.IN_PROGRESS.ordinal()));
        
        main.addCondition(Conditions.notExists(notExists));
        
        String joinedAlias1 = "sched1";
        String joinedAlias2 = "sched2";        
        DatabaseSelectAllStatement inData = DatabaseTools.createSelectAllStatement(Constants.TABLE_CONTENT_SCHEDULE, false);
        inData.setRootAlias(joinedAlias1);
        inData.addJoin(Constants.TABLE_CONTENT_SCHEDULE, joinedAlias2,Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID,Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);

        inData.addCondition(Conditions.eqProperty(joinedAlias1 + "." + Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME, 
                                                  mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME));
        
        inData.addCondition(Conditions.eq(joinedAlias1 + "." + Constants.TABLE_CONTENT_SCHEDULE__OPERATION, BatchOperation.INSERT.ordinal()));
        inData.addCondition(Conditions.eq(joinedAlias2 + "." + Constants.TABLE_CONTENT_SCHEDULE__OPERATION, BatchOperation.REMOVE.ordinal()));
        
        inData.addCondition(Conditions.eq(joinedAlias1 + "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.SCHEDULED.ordinal()));
        inData.addCondition(Conditions.eqProperty(joinedAlias1 + "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS, joinedAlias2 + "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS));
        inData.addResultColumn(joinedAlias1 + "." + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);
        main.addCondition(Conditions.notIn(mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID, inData));
        
        main.addResultColumn(Constants.FIELD_ID);
        main.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__STORE_NAME);            
        main.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);
        main.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__OPERATION);
        main.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__PARAMS);
        main.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__LENGTH);
        
        main.addOrder(Order.asc(mainAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID));
        main.setLockForUpdate(true);
        main.execute(connection);
        
        /*
        int p = 1;
        for(int i=0; i<jcrInstances.size(); i++)
            select.setString(p++, dialect.convertStringToSQL(jcrInstances.get(i)));
        
        select.setInt(p++, STATUS.SCHEDULED.ordinal());
        select.setInt(p++, STATUS.IN_PROGRESS.ordinal());
        select.setTimestamp(p++, new Timestamp(System.currentTimeMillis()));
        select.setInt(p++, STATUS.IN_PROGRESS.ordinal());
        select.setInt(p++, BatchOperation.INSERT.ordinal());
        select.setInt(p++, BatchOperation.REMOVE.ordinal());
        select.setInt(p++, STATUS.SCHEDULED.ordinal());


        ResultSet selected = select.executeQuery();
        //*/

//        while(selected.next()){
        while(main.hasNext()){            
//            Long id = selected.getLong(1);
            RowMap row = main.nextRow();
            Long id = row.getLong(Constants.FIELD_ID);
            /*
             * Is already locked by main.setLockForUpdate
            dialect.lockRow(connection, Constants.TABLE_CONTENT_SCHEDULE, selected.getLong(1));
            
            DatabaseSelectOneStatement selection = DatabaseTools.createSelectOneStatement(Constants.TABLE_CONTENT_SCHEDULE, Constants.FIELD_ID, id);
            selection.execute(connection);
            RowMap row = selection.getRow();
            int statusOrdinal = row.getLong(Constants.TABLE_CONTENT_SCHEDULE__STATUS).intValue();
            if(statusOrdinal == STATUS.IN_PROGRESS.ordinal()){
//                check timestamp
                GregorianCalendar time = (GregorianCalendar)row.get(Constants.TABLE_CONTENT_SCHEDULE__END_TIME);
                if(time.getTimeInMillis() > System.currentTimeMillis()){
                    connection.commit(); // release lock if taken by someone also
                    continue;
                }
            }//*/
            
            DatabaseUpdateStatement update = DatabaseTools.createUpdateStatement(Constants.TABLE_CONTENT_SCHEDULE, Constants.FIELD_ID, id);
            
//            long current = System.currentTimeMillis();
            Calendar setTime = GregorianCalendar.getInstance();
//            setTime.setTimeInMillis(current);
            
            Calendar endTime = GregorianCalendar.getInstance();
            endTime.setTimeInMillis(setTime.getTimeInMillis() + execTime);
            
            
            update.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.IN_PROGRESS.ordinal()));
            update.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__SET_TIME, setTime));
            update.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__END_TIME, endTime));            
            update.execute(connection);
//            connection.commit(); // update status and times
            
            String storeName = row.getString(Constants.TABLE_CONTENT_SCHEDULE__STORE_NAME);            
            Long contentId = row.getLong(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);
            int operationOrdinal = row.getLong(Constants.TABLE_CONTENT_SCHEDULE__OPERATION).intValue();
            String paramsStr = row.getString(Constants.TABLE_CONTENT_SCHEDULE__PARAMS);
            int length = row.getLong(Constants.TABLE_CONTENT_SCHEDULE__LENGTH).intValue();
            
            List<BatchUnit> units = storeBatchUnits.get(storeName);
            if(units == null){
                units = new ArrayList<BatchUnit>();
                storeBatchUnits.put(storeName, units);
            }

            
            BatchOperation operation = BatchOperation.values()[operationOrdinal];
            Properties params = new Properties();
            if(paramsStr != null)
                params.load(new ByteArrayInputStream(paramsStr.getBytes()));
            

            CachedContentDataSource cdSource = null;

            if(operation != BatchOperation.REMOVE){
                CSPCache cache = caches.get(storeName);
                if(cache == null){
                    cache = cacheManager.getCache(storeName);
                    caches.put(storeName,cache);
                }
                cdSource = new CachedContentDataSource(cache, contentId);
            }

            BatchUnit unit = new ScheduledBatchUnit(id, contentId, operation, params, cdSource);
            unit.setLength(length);
            units.add(unit);
            count++;
        }
        connection.commit(); // update status and times
        
        for(String storeName:storeBatchUnits.keySet()){
            List<BatchUnit> units = storeBatchUnits.get(storeName);
            batches.add(new Batch(storeName, units));
        }
        return count;
    }
    
    /**
     * Sends batches to stores.
     * @param connection
     * @param batches
     * @throws Exception
     */
    protected void processBatches(DatabaseConnection connection, List<Batch> batches) throws Exception{
        for(Batch batch:batches){
            
            boolean failed = false;
            try{
                String storeName = batch.getStoreName();
                if(log.isDebugEnabled()){
                    String message = MessageFormat.format("Submitting {0} update(s) to Content Store {1}",
                                    batch.getSize(), storeName);
                    log.debug(message);
                }
                
                
                TransportAdapterClient taClient = asynchStores.get(storeName);
                taClient.submit(batch);
            }catch(Exception ex){
                String message = MessageFormat.format("Failed to send {0} update(s) to Content Store {1}. Records marked with status ERROR: {2} in CMCS_CONTENT_SCHEDULE table.",
                        batch.getSize(), batch.getStoreName(), STATUS.ERROR.ordinal());
                log.error(message, ex);
                failed=true;
            }

            Iterator<BatchUnit> units = batch.getUnitsIterator();
            List<Long> ids = new LinkedList<Long>();
            
            while(units.hasNext()){
                ScheduledBatchUnit unit = (ScheduledBatchUnit)units.next();
                ids.add(unit.getScheduleId());
            }
            
            if(failed){
                
                if(log.isDebugEnabled()){
                    String message = MessageFormat.format("Setting ERROR status for schedule record ID(s) {0} for Content Store {1}",
                                    ids, batch.getStoreName());
                    log.debug(message);
                }
                
                DatabaseUpdateStatement update = DatabaseTools.createUpdateStatement(Constants.TABLE_CONTENT_SCHEDULE);
                update.addCondition(Conditions.in(Constants.FIELD_ID, ids));
                update.addValue(SQLParameter.create(Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.ERROR.ordinal()));
                update.execute(connection);
            }else{
                if(log.isDebugEnabled()){
                    String message = MessageFormat.format("Cleaning schedule record ID(s) {0} for Content Store {1}",
                                    ids, batch.getStoreName());
                    log.debug(message);
                }
                
                
                DatabaseDeleteStatement delete = DatabaseTools.createDeleteStatement(Constants.TABLE_CONTENT_SCHEDULE);
                delete.addCondition(Conditions.in(Constants.FIELD_ID, ids));
                delete.execute(connection);
            }
            
            connection.commit();
        }
    
    }
    
    /**
     * Cleans consequent insert-update scheduled for same Content in case processing is not yet started.
     * @param connection
     * @param instanceName
     * @throws Exception
     */
    protected void cleanInsertRemove(DatabaseConnection connection) throws Exception{
/*
DELETE FROM jcr.CMCS_CONTENT_SCHEDULE
WHERE CONTENT_ID IN (

SELECT sched1.CONTENT_ID 
FROM jcr.CMCS_CONTENT_SCHEDULE sched1, jcr.CMCS_CONTENT_SCHEDULE sched2
WHERE   sched1.CONTENT_ID = sched2.CONTENT_ID 
    and sched1.OPERATION=2 
    and sched2.OPERATION=1 
    and sched1.INSTANCE_NAME IN ('aaa', ...)
    and sched1.STATUS=1
    and sched2.STATUS=1)
 */   
//        PreparedStatement statement = null;
        try{

            String joinedAlias = "sched2";
            DatabaseSelectAllStatement inData = DatabaseTools.createSelectAllStatement(Constants.TABLE_CONTENT_SCHEDULE, false);
            inData.addJoin(Constants.TABLE_CONTENT_SCHEDULE, joinedAlias,Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID,Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);
            
            inData.addCondition(Conditions.eq(Constants.TABLE_CONTENT_SCHEDULE__OPERATION, BatchOperation.INSERT.ordinal()));
            inData.addCondition(Conditions.eq(joinedAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__OPERATION, BatchOperation.REMOVE.ordinal()));
            
            inData.addCondition(Conditions.in(Constants.TABLE_CONTENT_SCHEDULE__INSTANCE_NAME, jcrInstances));

            inData.addCondition(Conditions.eq(Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.SCHEDULED.ordinal()));
            inData.addCondition(Conditions.eq(joinedAlias + "." + Constants.TABLE_CONTENT_SCHEDULE__STATUS, STATUS.SCHEDULED.ordinal()));
            
            inData.addResultColumn(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID);

            DatabaseDeleteStatement del = DatabaseTools.createDeleteStatement(Constants.TABLE_CONTENT_SCHEDULE);
            del.addCondition(Conditions.in(Constants.TABLE_CONTENT_SCHEDULE__CONTENT_ID, inData));
            del.execute(connection);

            
            
            /*
            DatabaseDialect dialect = connection.getDialect();
            statement = connection.prepareStatement(queryRemoveInsert, true);
            statement.setInt(1, BatchOperation.INSERT.ordinal());
            statement.setInt(2, BatchOperation.REMOVE.ordinal());
            
            int p = 3;
            for(int i=0; i<jcrInstances.size(); i++)
                statement.setString(p++, dialect.convertStringToSQL(jcrInstances.get(i)));
            

            statement.setInt(p++, STATUS.SCHEDULED.ordinal());
            statement.setInt(p++, STATUS.SCHEDULED.ordinal());
            
            int updated = statement.executeUpdate();
            
            if(log.isDebugEnabled()){
                String message = MessageFormat.format("{0} record(s) (not yet uploaded contents already requested for remove) cleaned from update schedule.",
                                updated);
                log.debug(message);
            }
            //*/

            
        }finally{
//            if(statement != null)
//                DatabaseTools.closePreparedStatement(statement);
        }
    }
    
    
    public String getDisplayableName() {
        return "Asynchronous Content Store Updater";
    }

    public int getBatchSize() {
        return 0;
    }
    
    
    /**
     * BatchUnit describing scheduled task.
     * @author Maksims
     *
     */
    class ScheduledBatchUnit extends BatchUnit{
        private Long scheduleId;
        public ScheduledBatchUnit(Long scheduleId, Long contentId, BatchOperation operation, Properties params, ContentDataSource source) {
            super(contentId, operation, params, source);
            this.scheduleId = scheduleId;            
        }
        
        Long getScheduleId(){
            return scheduleId;
        }
    }

}

/*
 * $Log: AsynchronousContentStoreUpdater.java,v $
 * Revision 1.2  2009/01/23 07:21:39  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.12  2006/11/30 14:54:35  maksims
 * #1803505 Added support for wildcard in dereference
 *
 * Revision 1.11  2006/11/09 13:44:57  zahars
 * PTR #1803381  FTS batch size could be configured
 *
 * Revision 1.10  2006/08/15 16:11:03  maksims
 * #1802426 SQL framework is used to query DB
 *
 * Revision 1.9  2006/08/15 08:38:05  dparhomenko
 * PTR#1802426 add new features
 *
 * Revision 1.8  2006/08/08 13:10:40  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.7  2006/08/02 11:42:31  maksims
 * #1802426 SQL Framework used to generate queries
 *
 * Revision 1.6  2006/07/28 15:49:09  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.5  2006/07/25 12:57:30  maksims
 * #1802425 store threads fixed to implement _Command
 *
 * Revision 1.4  2006/07/12 11:51:14  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/07/12 07:44:34  dparhomenko
 * PTR#1802389 for update statement
 *
 * Revision 1.2  2006/07/06 16:43:11  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 * Revision 1.1  2006/07/06 08:22:45  maksims
 * #1802356 Content Store configuration import added
 *
 */