/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.ColumnDefinition;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.TableDefinition;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.store.AbstractContentStore;
import com.exigen.cm.store.StoreHelper;
import com.exigen.cm.store.TrackableStream;

/**
 * Database based store implementation. Uses database dialect
 * to build queries.
 * @author Maksims
 *
 */
public class DBContentStore extends AbstractContentStore {
    
    
    /*
     tableName=CM_STORE_storeName => 25 chars + storeName.hashCode()
     property = 'table', OPTIONAL
     */
    
    private final String tableName;
    
    private Log log = LogFactory.getLog(DBContentStore.class);

    
    DBContentStore(DBContentStoreConfiguration config){
        super(config);
        this.tableName=config.getTableName();
    }

    @Override
//    protected ContentData _put(Long jcrContentId, InputStream data, Map params) {
    protected ContentData _put(Long jcrContentId, InputStream data, int length, Map params) {        

        File tmp = null;
        try{
            if(length<0){
                tmp = File.createTempFile("dbs", "tmp");
                OutputStream fos = new FileOutputStream(tmp);
                length = StoreHelper.transfer(data, fos);
                data = new FileInputStream(tmp);
            }
            
            DatabaseInsertStatement insert = DatabaseTools.createInsertStatement(tableName);
            insert.addValue(SQLParameter.create(Constants.FIELD_ID, jcrContentId));
            
            SQLParameter streamParam = SQLParameter.create(Constants.FIELD_BLOB, data, length);
            insert.addValue(streamParam);
            insert.execute(getTransactionConnection());
            ContentData result = new ContentData(jcrContentId.toString(), length);

            data.close();
            
            return result;
            
//            return jcrContentId.toString();
        }catch(Exception ex){
            String message = "Failed to put data into a DB Content Store";
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }finally{
            if(tmp != null && !tmp.delete())
                tmp.deleteOnExit();
        }
    }

    protected InputStream _get(String contentId) {

        DatabaseConnection connection = null;
        try{
            connection = getActiveConnection();
            DatabaseSelectOneStatement select = DatabaseTools.createSelectOneStatement(tableName, Constants.FIELD_ID, new Long(contentId));
            select.setIgnoreBLOB(false);
            
            select.execute(connection);
            return new TrackableStream(select.getRow().getStream(Constants.FIELD_BLOB), contentId);
        }catch(ItemNotFoundException ex){
            
            log.warn(MessageFormat.format("Cannot find content with id {0} in table {1}"
                    , contentId, tableName));
            
            return null;
        }catch(RepositoryException rex){
            throw new RuntimeException(rex);
        }finally{
            try{
                closeNonTransactionalConnection(connection);
            }catch(RepositoryException rex){
                throw new RuntimeException(rex);
            }
        }
    }

    protected void _remove(String contentId) {
        try{
            DatabaseDeleteStatement delete = DatabaseTools.createDeleteStatement(tableName, Constants.FIELD_ID, new Long(contentId));
            delete.execute(getTransactionConnection());
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to delete content with id {0} from table {1}"
                    , contentId, tableName);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
        
    }

    
    protected void _validate() {
        DatabaseConnection conn = null;
        try{
            conn = createCommonConnection();
            if(! DatabaseTools.validateTable(conn, getStoreTableDef())){
                String message = MessageFormat.format("DB Store table {0} has invalid structure and should be recreated", tableName);
                throw new Exception(message);
            }
        }catch(Exception ex){
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex.getMessage(), ex);
        }finally{
            if(conn!=null){
                try{
                    conn.close();
                }catch(RepositoryException ex){
                    throw new RuntimeException("Failed to close connection when validating DB Store structure", ex);
                }
            }
        }
    }

    protected TableDefinition getStoreTableDef()  throws RepositoryException{
        TableDefinition table = new TableDefinition(tableName, true);
        table.addColumn(new ColumnDefinition(table, Constants.FIELD_BLOB, Types.BLOB));
        return table;
    }
    
   
    /**
     * Creates database table if it is not existing.
     *
     */
    public void createIfNotExists(){
        
        DatabaseConnection conn = null;
        try{
            conn = createCommonConnection();
            DatabaseMetaData dbMD = conn.getConnectionMetaData();
            ResultSet result = dbMD.getTables(null, conn.getDialect().getSchemaName(conn), conn.getDialect().extractTableName(tableName), null);
            if(result.next()){ // there is table
                return;
            }
            
            create(conn);
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to initialize DBContent Store table {0}", tableName);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }finally{
            if(conn !=null && conn.isLive())
                try{
                    conn.close();
                }catch(Exception ex){
                  log.debug("Failed to close connection after table creation", ex);
                }
        }
        
    }
    
    public void create() {
        try{
            create(createCommonConnection());
        }catch(RepositoryException re){
            String message = MessageFormat.format("Failed to create DBContent Store table {0}", tableName);
            log.error(message, re);
            throw new RuntimeException(message, re);  
        }
    }
    public void create(DatabaseConnection connection) {
//        Constants.FIELD_ID  as NUMERIC
//        FIELD_DATA as BLOB
//        DatabaseConnection connection = null;
        try{
//            connection = createCommonConnection();
            connection.createTables(new TableDefinition[]{getStoreTableDef()});
            connection.commit();
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to initialize DBContent Store table {0}", tableName);
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }finally{
            if(connection !=null)
                try{
                    connection.close();
                }catch(Exception ex){
                  log.debug("Failed to close connection after table creation", ex);
                }
        }
    }

    protected void _drop() {
        DatabaseConnection connection = null;
        try{
            connection = createCommonConnection();
            connection.execute("DROP TABLE " + tableName);
            connection.commit();
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to drop DBContent Store table {0}. Table may not exist", tableName);
            log.debug(message);//, ex);
//            throw new RuntimeException(message, ex);
        }finally{
            if(connection !=null)
                try{
                    connection.close();
                }catch(Exception ex){
                  log.debug("Failed to close connection after DB Store table drop", ex);
                }
        }
    }
}

/*
 * $Log: DBContentStore.java,v $
 * Revision 1.3  2009/02/23 14:30:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2007/12/07 15:04:43  maksims
 * added capability to create content table if not existing opn initialization
 *
 * Revision 1.1  2007/04/26 08:59:44  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.26  2007/04/04 10:27:12  vpukis
 * PTR#1801827 Oracle Index organized table support, added columns in index over CM_NODE.NODE_PATH for index only access, in case of Oracle - tables CM_NODE_PARENTS and CM_TYPE made index organized
 *
 * Revision 1.25  2006/11/15 14:22:06  maksims
 * #1802721 content length on put made optional
 *
 * Revision 1.24  2006/11/07 16:28:10  maksims
 * #1801897 Trackable stream moved to a separate file
 *
 * Revision 1.23  2006/09/28 09:19:40  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.22  2006/08/14 16:18:41  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.21  2006/08/08 13:10:43  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.20  2006/08/04 12:33:42  maksims
 * #1802426 SQL Framework used to generate queries in DBContentStore
 *
 * Revision 1.19  2006/08/04 10:52:44  maksims
 * #1802356 Code cleanup
 *
 * Revision 1.18  2006/07/28 15:49:11  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.17  2006/07/26 08:45:45  maksims
 * #1802414 added ability to initialize stores without validation
 *
 * Revision 1.16  2006/07/06 08:22:41  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.15  2006/07/04 15:37:23  maksims
 * #1802356 Remove fixed
 *
 * Revision 1.14  2006/07/04 14:03:39  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 * Revision 1.13  2006/06/22 12:00:33  dparhomenko
 * PTR#0146672 move operations
 *
 * Revision 1.12  2006/05/09 11:24:04  maksims
 * #0144986 fixed to log only exception message on drop
 *
 * Revision 1.11  2006/05/08 13:43:32  maksims
 * #0144986 connection close on validate added
 *
 * Revision 1.10  2006/05/08 09:05:44  dparhomenko
 * PTR#0144983 fix prepared statement parameter position
 *
 * Revision 1.9  2006/05/03 15:48:40  maksims
 * #0144986 validate result is made cached
 *
 * Revision 1.8  2006/05/03 15:10:54  maksims
 * #0144986 get method fixed
 *
 * Revision 1.7  2006/05/03 13:53:39  maksims
 * #0144986 garbage cleaned up
 *
 * Revision 1.6  2006/05/03 13:49:58  maksims
 * #0144986 remove modified to delete records imeediately
 *
 * Revision 1.5  2006/05/03 13:37:22  maksims
 * #0144986 ContentStore.begin method got DatabaseConnection param
 *
 * Revision 1.4  2006/05/03 13:00:57  maksims
 * #0144986 ContentStore validate method implemented
 *
 * Revision 1.3  2006/05/03 11:53:14  maksims
 * #0144986 BLOBInsertStatement added
 *
 * Revision 1.2  2006/05/03 08:36:16  maksims
 * #0144986 Content store provider constructor changed
 *
 * Revision 1.1  2006/05/02 11:44:26  maksims
 * #0144986 DB Content store type added
 *
 */