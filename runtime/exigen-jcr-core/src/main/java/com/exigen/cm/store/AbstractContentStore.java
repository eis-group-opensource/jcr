/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ConnectionProvider;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.impl.SoftHashMap;



/**
 * Provides empty implementation for ContentStorage implementation transactions.
 * @author Maksims
 */
public abstract class AbstractContentStore implements ContentStore {
    private static final Log log = LogFactory.getLog(AbstractContentStore.class);



    private static final byte STATUS_NOT_CHECKED = 0;
    private static final byte STATUS_CHECKED_VALID = 1;
    private static final byte STATUS_CHECKED_INVALID = 2;

    private byte status = STATUS_NOT_CHECKED; // validness status



    /*
     * DB content rows cache
     */
    private SoftHashMap<Long, RowMap> rowCache = new SoftHashMap<Long, RowMap>(100);

    /**
     * Common Connection provider used to create JCR DB connections on get() calls.
     */
    private final ConnectionProvider commonConnectionProvider;

    /**
     * Holds connection opened for current transaction
     */
    private DatabaseConnection transactionConnection=null;

    protected ContentStoreConfiguration configuration;

    protected void _remove(String storeContentId){throw new UnsupportedOperationException();};
    protected InputStream _get(String storeContentId){throw new UnsupportedOperationException();};
    protected ContentData _put(Long jcrContentId, InputStream data, int length, Map<String, String> params){throw new UnsupportedOperationException();};

    protected void _validate(){throw new UnsupportedOperationException();};


    protected ContentTracker contentTracker = null;

    protected AbstractContentStore(ContentStoreConfiguration config){
        this.commonConnectionProvider = config.getConnectionProvider();
        configuration = config;
    }



    /**
     * Puts content into content store. Should be invoked inside of transaction.
     */
    public Long put(InputStream data, int length, Map<String, String> params) {
        ensureTransactionStarted();
        boolean failed = false;

        try{
            Long jcrContentId = getTransactionConnection().nextId();
            put(jcrContentId, data, length, params);
            return jcrContentId;//.toString();
        }catch(RepositoryException rex){
            failed = true;
            throw new RuntimeException(rex);
        }finally{

            if(failed)
                rollback();
        }
    }

    public void put(Long jcrContentId, InputStream data, int length, Map<String, String> params) {
      ensureTransactionStarted();
      boolean failed = false;

      try{
//          String storeContentId = _put(jcrContentId, data, length, params);
          ContentData storeContentData = _put(jcrContentId, data, length, params);

//        Should insert record in CMCS_CONTENT to register mapping
          DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(Constants.TABLE_CONTENT);
          insert.addValue(SQLParameter.create(Constants.FIELD_ID, jcrContentId));
          insert.addValue(SQLParameter.create(Constants.TABLE_CONTENT__STORE_CONTENT_ID, storeContentData.getStoreContentId()));//result.getStoreContentId()));
          insert.addValue(SQLParameter.create(Constants.TABLE_CONTENT__STORE_NAME, configuration.getStoreName()));
          insert.addValue(SQLParameter.create(Constants.TABLE_CONTENT__CONTENT_SIZE, storeContentData.getSize()));//result.getSize()));
          insert.execute(getTransactionConnection());

          RowMap cache = new RowMap();
          cache.put(Constants.FIELD_ID, jcrContentId);
          cache.put(Constants.TABLE_CONTENT__STORE_CONTENT_ID, storeContentData.getStoreContentId());
          cache.put(Constants.TABLE_CONTENT__STORE_NAME, configuration.getStoreName());
          cache.put(Constants.TABLE_CONTENT__CONTENT_SIZE, storeContentData.getSize());
          this.rowCache.put(jcrContentId, cache);
      }catch(RepositoryException rex){
          failed = true;
          throw new RuntimeException(rex);
      }catch(RuntimeException rex){
          failed = true;
          throw rex;
      }catch(Exception rex){
          failed = true;
          String message = "Failed to put content. Invoking automatic rollback";
          log.error(message, rex);
          throw new RuntimeException(message, rex);
      }finally{

          if(failed)
              rollback();
      }
  }



    /**
     * Returns row from CMCS_CONTENT for JCR Content ID provided.
     * @param jcrContentId is a Content JCR UID
     * @return
     */
    protected RowMap getContentRow(Long jcrContentId, DatabaseConnection connection){
    	RowMap result = rowCache.get(jcrContentId);
    	if (result != null){
    		return result;
    	}
        try{
            DatabaseSelectOneStatement select = DatabaseTools.createSelectOneStatement(Constants.TABLE_CONTENT, Constants.FIELD_ID, jcrContentId);
            select.execute(connection);
            result = select.getRow();
            if(result == null){
                String message = MessageFormat.format("Content with ID {0} not exists in Content Store {1}",
                        jcrContentId, configuration.getStoreName());
                log.error(message);
                throw new RuntimeException(message);
            }
            rowCache.put(jcrContentId, result);
            select.close();
            return result;
        }catch(RepositoryException rex){
            throw new RuntimeException(rex);
        }catch(RuntimeException rex){
            throw rex;
        }catch(Exception rex){
            String message = MessageFormat.format("Failed to get record for content with provided ID: {0}.",
                    jcrContentId);
            log.error(message, rex);
            throw new RuntimeException(message, rex);
        }
    }

    public InputStream get(Long jcrContentId) {
        DatabaseConnection connection = null;
        try{
            connection = getActiveConnection();
            String contentId = getContentRow(jcrContentId, connection).getString(Constants.TABLE_CONTENT__STORE_CONTENT_ID);
            InputStream result =_get(contentId);
            getContentTracker().add(jcrContentId, result, new Throwable());

            return result;
        }catch(RepositoryException rex){
            throw new RuntimeException(rex);
        }finally{
            try{
                closeNonTransactionalConnection(connection);
            }catch(RepositoryException ex){
                throw new RuntimeException(ex);
            }
        }
    }


    protected void ensureCanRemove(Long jcrContentId){
        Set<Throwable> openers = contentTracker.getContentOpeners(jcrContentId);
        if(openers != null){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            for(Throwable opener:openers){
                opener.printStackTrace(pw);
                pw.println("================================");
            }

            String message = MessageFormat.format("Attempt is made to delete content {0} in Content Store {1} which is still opened. See stack(s): \n{2}",
                    jcrContentId, configuration.getStoreName(), sw.getBuffer());
            log.error(message);

            rollback();
            throw new UnclosedContentStreamException(message);
        }
    }

    public void remove(Long jcrContentId) {
        ensureCanRemove(jcrContentId);
        ensureTransactionStarted();
        boolean failed = false;

        try{
//      Should query CMCS_CONTENT for contentId and invoke internal remove with it.
            DatabaseConnection connection = getTransactionConnection();
            String contentId = getContentRow(jcrContentId, connection).getString(Constants.TABLE_CONTENT__STORE_CONTENT_ID);
            _remove(contentId);

            DatabaseDeleteStatement delete = DatabaseTools.createDeleteStatement(Constants.TABLE_CONTENT, Constants.FIELD_ID, new Long(jcrContentId));
            delete.execute(connection);

        }catch(RepositoryException rex){
            failed = true;
            throw new RuntimeException(rex);
        }catch(RuntimeException rex){
            failed = true;
            throw rex;
        }catch(Exception rex){
            failed = true;
            String message = MessageFormat.format("Failed to remove content with provided ID: {0}. Invoking automatic rollback",
                    jcrContentId);
            log.error(message, rex);
            throw new RuntimeException(message, rex);
        }finally{

            if(failed)
                rollback();
        }
    }


    /**
     * Returns common connection provider.
     * @return
     */
    protected DatabaseConnection createCommonConnection() throws RepositoryException{
        return commonConnectionProvider.createConnection();
    }

    protected DatabaseConnection getTransactionConnection(){
        return transactionConnection;
    }

    /**
     * Returns database connection corresponding to store state.
     * In case transaction is active the transaction connection is returned
     * if not the new common connection is created using ConnectionProvider
     * @return
     */
    protected DatabaseConnection getActiveConnection() throws RepositoryException{
        return isTransactionStarted() ? transactionConnection : createCommonConnection();
    }

    /**
     * Closes database connection if not transactional.
     */
    protected void closeNonTransactionalConnection(DatabaseConnection connection) throws RepositoryException{
//        if(connection != null && !isTransactionStarted())
        if(connection != transactionConnection)
            connection.close();
    }

    /**
     * Clears transaction connection.
     */
    protected void clearTransactionVariables(){
        transactionConnection = null;
    }

    /**
     * {@inheritDoc}
     */
    public void begin(DatabaseConnection connection) {
        if(isTransactionStarted()){
            String message = "Cannot start new transaction while another transaction is in progress.";
            log.error(message);
            throw new RuntimeException(message);
          }

        transactionConnection = connection;
        _begin(connection);
    }



    /**
     * {@inheritDoc}
     */
    public void commit() {
        ensureTransactionStarted();

        try{
            _commit();
            clearTransactionVariables();
        }catch(Exception ex){
//          Automatic rollback should be invoked in case of exception
            rollback();
            clearTransactionVariables();
            String message = MessageFormat.format("Commit failed for content store {0}", configuration.getStoreName());
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commitPut() {
        ensureTransactionStarted();
        try{
            _commitPut();
        }catch(Exception ex){
//          Automatic rollback should be invoked in case of exception
            rollback();
            clearTransactionVariables();
        }
    }

    /**
     * Content Store specific commit implementation.
     */
    protected void _commit(){}

    /**
     * Content Store specific commitPut implementation.
     */
    protected void _commitPut(){}

    /**
     * Internal rollback implementation.
     *
     */
    protected void _rollback() {}

    protected void _begin(DatabaseConnection connection){}

    /**
     * {@inheritDoc}
     * Prevents transaction rollback if it is not started.
     */
    public void rollback() {
        if(isTransactionStarted()) {
            _rollback();
        }

    }

    /**
     * Default implementation. Always returns <code>true</code>.
     * Stores which doesn's support transactions shouldn't override this method.
     * If JCR is used for transactional code then before it would loose connection between transactions #HOLDEZ-5713, #CHBUG-408 and #EPBJCR-33
     */
    public boolean isTransactionStarted(){
        return transactionConnection != null && transactionConnection.isLive();
    }

    /**
     * Throws RuntimeException in case no transaction is started.
     */
    protected void ensureTransactionStarted(){
        if(!isTransactionStarted())
            throw new RuntimeException("No active transaction");
    }

    /**
     * {@inheritDoc}
     */
    public void validate() {
        if(!configuration.isValidationAllowed()){
            String warning = MessageFormat.format("WARNING! Store {0} ({1}) validation is explicitely DISABLED via configuration property local.validate=false!\nStore structure might be inconsistent therefore BE SURE WHAT YOU ARE DOING !!!!", configuration.getStoreName(), configuration.getType());
            log.warn(warning);

            System.out.println("\n****************************************************\n\n"
                                + warning
                                + "\n\n****************************************************\n");

            status = STATUS_CHECKED_VALID;
            return;
        }




        switch(status){
            case STATUS_NOT_CHECKED:
                try{
                 _validate();
                 status = STATUS_CHECKED_VALID;
                }catch(RuntimeException ex){
                    status = STATUS_CHECKED_INVALID;
                    throw ex;
                }
            break;

            case STATUS_CHECKED_INVALID:
                throw new RuntimeException("Invalid Store structure!");

            default:
        }
    }

    /**
     * {@inheritDoc}
     */
    public void create() {
//        creates internal store infrastructure
//        should be overriden by subclass
        throw new UnsupportedOperationException();
    }

    /**
     * Implements store specific drop
     *
     */
    protected void _drop() {throw new UnsupportedOperationException();}
    /**
     * {@inheritDoc}
     */
    public void drop() {

        _drop();
        DatabaseConnection connection = null;
        try{
//          deletes all items from mapping table
            connection = createCommonConnection();
            DatabaseDeleteStatement delete = DatabaseTools.createDeleteStatement(Constants.TABLE_CONTENT);
            delete.addCondition(Conditions.eq(Constants.TABLE_CONTENT__STORE_NAME, configuration.getStoreName()));
            delete.execute(connection);

            connection.commit();
        }catch(RepositoryException rex){
            throw new RuntimeException(rex);
        }catch(RuntimeException rex){
            throw rex;
        }catch(Exception rex){
            String message = MessageFormat.format("Failed to drop Content Store: {0}.",
                    configuration.getStoreName());
            log.error(message, rex);
            throw new RuntimeException(message, rex);
        }finally{
            if(connection != null){
                try{
                    connection.close();
                }catch(RepositoryException rex){
                    String message = "Failed to close database connection";
                    log.error(message, rex);
                    throw new RuntimeException(message,rex);
                }
            }
        }
    }




    /**
     * Returns content length
     */
    public long getContentLength(Long jcrContentId) {
//      Should query CMCS_CONTENT for content length
        DatabaseConnection connection = null;
        try{
            connection = getActiveConnection();
            RowMap result = getContentRow(jcrContentId, connection);
            if(result == null)
                return 0;

            Long length = result.getLong(Constants.TABLE_CONTENT__CONTENT_SIZE);
            return length;

        }catch(RepositoryException rex){
            throw new RuntimeException(rex);
        }finally{
            try{
                closeNonTransactionalConnection(connection);
            }catch(RepositoryException ex){
                throw new RuntimeException(ex);
            }
        }

    }

// ************** Methods left for compatilility with earlier versions *****

    public InputStream get(String jcrContentId){
        return get(new Long(jcrContentId));
    }

    public void remove(String jcrContentId) {
        remove(new Long(jcrContentId));
    }

    public long getContentLength(String jcrContentId) {
        return getContentLength(new Long(jcrContentId));
    }
// ***************************************************************************
    /**
     * @return Returns the contentTracker.
     */
    public ContentTracker getContentTracker() {
        return contentTracker;
    }
    /**
     * @param contentTracker The contentTracker to set.
     */
    public void setContentTracker(ContentTracker contentTracker) {
        this.contentTracker = contentTracker;
    }



    /**
     * Structure to hold information about content stored.
     * @author Maksims
     *
     */
    public class ContentData{
        public long contentSize;
        public String storeContentId;
        public ContentData(String contentId, long size){
           storeContentId = contentId;
           contentSize = size;
        }

        public long getSize(){
            return contentSize;
        }

        public String getStoreContentId(){
            return storeContentId;
        }
    }//*/
}
/*
 * $Log: AbstractContentStore.java,v $
 * Revision 1.4  2010/01/18 16:44:06  RRosickis
 * CHBUG-408, HOLDEZ-5713  and EPBJCR-33 - transaction validation also added for File storage as filestorage is using db connection as well for retrieving file metadata.
 *
 * Revision 1.3  2009/02/26 15:17:32  maksims
 * added support for property store.<store name>.validate=true|false
 *
 * Revision 1.2  2009/02/04 12:16:39  maksims
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:42  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.24  2007/02/02 08:48:58  dparhomenko
 * PTR#1803853 fix session scope lock
 *
 * Revision 1.23  2006/11/15 14:22:09  maksims
 * #1802721 content length on put made optional
 *
 * Revision 1.22  2006/09/29 13:55:35  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.21  2006/09/28 12:30:38  maksims
 * #0147862 formatted
 *
 * Revision 1.20  2006/09/28 12:23:36  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.19  2006/09/28 09:19:37  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.18  2006/08/08 13:10:35  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.17  2006/08/04 10:52:47  maksims
 * #1802356 Code cleanup
 *
 * Revision 1.16  2006/07/28 15:49:07  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.15  2006/07/26 08:45:48  maksims
 * #1802414 added ability to initialize stores without validation
 *
 * Revision 1.14  2006/07/24 12:08:31  maksims
 * #1802414 Store validation should be performed once by ContentStoreProvider owner (RepositoryImpl etc) when ContentStoreProvider is initialized. Separate validation of each Content Store instance is disabled now.
 *
 * Revision 1.13  2006/07/17 14:47:43  zahars
 * PTR#0144986 Cleanup
 *
 * Revision 1.12  2006/07/06 10:52:41  maksims
 * #1802356 Content STORE NAME added to CONTENT_SCHEDULE table
 *
 * Revision 1.11  2006/07/06 08:22:12  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.10  2006/07/04 15:37:25  maksims
 * #1802356 Remove fixed
 *
 * Revision 1.9  2006/07/04 14:03:33  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 * Revision 1.8  2006/05/03 15:48:42  maksims
 * #0144986 validate result is made cached
 *
 * Revision 1.7  2006/05/03 13:37:25  maksims
 * #0144986 ContentStore.begin method got DatabaseConnection param
 *
 * Revision 1.6  2006/05/03 08:36:11  maksims
 * #0144986 Content store provider constructor changed
 *
 * Revision 1.5  2006/05/02 11:44:28  maksims
 * #0144986 DB Content store type added
 *
 * Revision 1.4  2006/04/17 06:47:14  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.3  2006/04/12 12:18:43  maksims
 * #0144986 Seekable support added to File Store
 *
 */