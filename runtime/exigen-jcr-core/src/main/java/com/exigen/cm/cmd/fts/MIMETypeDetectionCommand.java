/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.extract.MIMETypeDetector;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreProvider;
import com.exigen.cm.store.StoreHelper;

/**
 * Detects file MIME type by its content
 * 
 */
public class MIMETypeDetectionCommand extends FTSXCommand {

    private static final Log log = LogFactory.getLog(MIMETypeDetectionCommand.class);
    private MIMETypeDetector mimeDetector = new MIMETypeDetector();
    
    

    
    protected DatabaseSelectAllStatement buildSelectLockQuery (){
        
        // builds SELECT FOR UPDATE statement
        // SELECT FOR UPDATE x.ID, x.CONTENT_DATA FROM CM_INDEXABLE_DATA x WHERE x.MIME_TYPE = 'application/octet-stream' AND FTS_STAGE_ID is NULL
        //      AND RESERVED=FALSE AND OPERATION = 'INSERT' AND x.CONTENT_DATA NOT IN 
        //      ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        //      ORDER BY x.ID TOP 5
        DatabaseSelectAllStatement ds = super.buildSelectLockQuery();
        
        
        //x.MIME_TYPE is "application/octet-stream" 
        ds.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE,Constants.UNDEFINED_MIME_TYPE));
        //AND FTS_STAGE_ID is NULL
        ds.addCondition(Conditions.isNull(Constants.TABLE_INDEXABLE_DATA__FTS_STAGE_ID));
        //AND OPERATION = 'INSERT' 
        ds.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_INSERT));
        
        return ds;
        
    }
    
    protected void process(List <IdData> data, DatabaseConnection connection,  ContentStoreProvider csp) throws RepositoryException {
    	boolean hasUpdated = false;
        for (IdData record: data){
            ContentStore store = csp.getStore(record.storeName);
            File file = null;

            try {
                // retrieve content and create temporary file
                InputStream is = store.get(record.storeContentId);
                file = File.createTempFile("mime","mim");
                OutputStream os = new FileOutputStream(file);
                StoreHelper.transfer(is,os); // close both streams
            
                String mime = mimeDetector.detect(file);
                log.debug("MIME type detected: " + mime);
                is.close();
                updateOnMimeDetectionResult(connection, record.id, mime, record.ftsDataId);
                hasUpdated = true;
            }catch (Exception e){
                log.error("Mime processing failed",e);
                throw new RepositoryException("Mime processing failed",e);
            }
            finally{
                if (file != null){
                   boolean result = file.delete();
                   if (!result){
                       log.error("Failed to delete temporary file");    
                   }
                }
            }
            
        }
        if (hasUpdated){
        	forceCommandExecutio(TextExtractionCommand.class.getName());
        }

    }
        
    private void updateOnMimeDetectionResult(DatabaseConnection connection, Long recordId, String mime, Long ftsDataId) throws RepositoryException {
        // UPDATE CM_INDEXABLE_DATA SET RESERVED=false, MIME_TYPE =?, START_TIME=?, PLANNED_FINISH_TIME=? WHERE ID = ?
    	String detectedMimeType = null;
    	if (mime != null){
    		detectedMimeType = mime.equals(Constants.UNDEFINED_MIME_TYPE) ? null : mime;
    	}
        
        DatabaseUpdateStatement ds = buildUpdateProcessedRecordStatement(recordId);
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE, detectedMimeType));
        
        ds.execute(connection);
        ds.close();
        
        // update FTS_DATA
        // UPDATE CM_FTS_DATA SET MIME_SOURCE = ? WHERE ID = ?
        String mimeSource = detectedMimeType == null ? "MIME type not detected" : detectedMimeType;
        ds = new DatabaseUpdateStatement(Constants.TABLE_FTS_DATA);
        ds.addCondition(Conditions.eq(Constants.FIELD_ID,ftsDataId));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_DATA__MIME_SOURCE, mimeSource));
        ds.execute(connection);
        ds.close();
        
//        connection.commit();
    }
    
    public String getDisplayableName(){
        return "MIME type detection";
    }


}


/*
 * $Log: MIMETypeDetectionCommand.java,v $
 * Revision 1.7  2009/03/24 07:54:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.6  2009/02/12 16:12:38  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.5  2009/02/12 15:22:50  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.4  2009/02/12 15:22:35  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2009/02/12 15:21:52  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2009/02/11 15:08:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.5  2006/10/09 11:22:48  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
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
 * Revision 1.9  2006/07/14 08:11:39  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.8  2006/07/07 15:00:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.7  2006/07/06 09:32:01  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.6  2006/07/06 07:58:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.5  2006/07/04 15:46:21  zahars
 * PTR#0144986 MIMECommand updated
 *
 * Revision 1.3  2006/07/03 14:01:09  zahars
 * PTR#0144986 MIMEMIMECommandTest updated
 *
 * Revision 1.2  2006/07/03 13:50:36  zahars
 * PTR#0144986 MIMEMIMECommandTest updated
 *
 * Revision 1.1  2006/07/03 09:04:45  zahars
 * PTR#0144986MIME type detection command introduced
 *
 */
