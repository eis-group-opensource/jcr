/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.cmd.fts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.extract.HTMLTextExtractor;
import com.exigen.cm.cmd.fts.extract.ODFTextExtractor;
import com.exigen.cm.cmd.fts.extract.PDFTextExtractor;
import com.exigen.cm.cmd.fts.extract.POIExcelTextExtractor;
import com.exigen.cm.cmd.fts.extract.POIPowerPointTextExtractor;
import com.exigen.cm.cmd.fts.extract.POIWordTextExtractor;
import com.exigen.cm.cmd.fts.extract.PlainTextExtractor;
import com.exigen.cm.cmd.fts.extract.RTFTextExtractor;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.DatabaseDialect;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseCallableStatement;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseStatement;
import com.exigen.cm.database.statements.DatabaseUpdateStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.database.statements.condition.FieldNameDatabaseCondition;
import com.exigen.cm.store.ContentStore;
import com.exigen.cm.store.ContentStoreProvider;
import com.exigen.cm.store.StoreHelper.FileBackedOutputStream;

/**
 * Command that execute text extraction from binary content
 * It handles three use cases:
 * 1. Text is provided with binary context
 * 2. Text could be extracted by native (DB) exstractor
 * 3. Text could be extracted by external (our) exstractor
 * 
 * Signal for text extraction is record in INDEXABLE_DATA
 * There should be record in FTS_DATA with corresponding information.
 * Use case 1.  If text is provided, it's stored in FTS_STAGE in UNICODE encoding
 *              Text is zipped and moved to FTS_DATA (for HSQL - not zipped)
 * Use case 2.  Native extractors are defined in dialect (based on MIME type)
 *              CONVERT_AND_MOVE stored procedure does extraction and zip
 * Use case 3.  Text exstracted by external exstractor (registered in this class) and stored in FTS_STAGE
 *              ZIP_AND_MOVE stored procedure is called to move and zip text
 * If non of the above scenarios is valid - text could not be extracted.
 * Corresponding info is added to the TEXT_SOURCE field of FTS_DATA                                       
 * 
 */
public class TextExtractionCommand extends FTSXCommand {
    
    private static final Log log = LogFactory.getLog(TextExtractionCommand.class);
    private Map<String, TextExtractor> externalExtractors = new HashMap<String,TextExtractor>();
    /**
     * TEXT_SOURCE comment
     */
    public static final String MSG_TEXT_PROVIDED = "Text provided";
    /**
     * TEXT_SOURCE comment
     */
    public static final String MSG_EXTRACTOR_NOT_FOUND = "Extractor not found for MIME type";
    /**
     * TEXT_SOURCE comment
     */
    public static final String MSG_NATIVE_EXTRACTOR = "Native extractor used for MIME type";
    /**
     * TEXT_SOURCE comment
     */
    public static final String MSG_EXTERNAL_EXTRACTOR = "External extractor used for MIME type";
    
    
    /**
     * Constructor
     */
    public TextExtractionCommand(){
       // register non-native extractors
        externalExtractors.put("text/plain", new PlainTextExtractor());
        externalExtractors.put("application/pdf", new PDFTextExtractor());                
//        extractors.put("application/msword", new OOWordTextExtractor(config));        
//        extractors.put("application/excel", new OOExcelTextExtractor(config));        
//        extractors.put("application/powerpoint", new OOPresentationTextExtractor(config));
        externalExtractors.put("application/msword", new POIWordTextExtractor());
        externalExtractors.put("application/excel", new POIExcelTextExtractor());
        externalExtractors.put("application/vnd.ms-excel", new POIExcelTextExtractor());
        externalExtractors.put("application/vndms-excel", new POIExcelTextExtractor());
        externalExtractors.put("application/x-excel", new POIExcelTextExtractor());
        externalExtractors.put("application/x-msexcel", new POIExcelTextExtractor());
        externalExtractors.put("application/powerpoint", new POIPowerPointTextExtractor());
        externalExtractors.put("application/vnd.ms-powerpoint", new POIPowerPointTextExtractor());
        externalExtractors.put("application/mspowerpoint", new POIPowerPointTextExtractor());
        externalExtractors.put("text/richtext", new RTFTextExtractor());
        externalExtractors.put("text/rtf", new RTFTextExtractor());
        externalExtractors.put("application/vnd.oasis.opendocument.text", new ODFTextExtractor());
        externalExtractors.put("text/html", new HTMLTextExtractor());
    }
    
    
    

    protected DatabaseSelectAllStatement buildSelectLockQuery (){
        
        // builds SELECT FOR UPDATE statement
        // SELECT FOR UPDATE x.ID, x.CONTENT_DATA FROM CM_INDEXABLE_DATA x WHERE x.MIME_TYPE <> 'application/octet-stream' 
        //      AND RESERVED=FALSE AND OPERATION = 'INSERT' AND x.CONTENT_DATA NOT IN 
        //      ( SELECT y.CONTENT_DATA FROM CM_INDEXABLE_DATA y WHERE x.CONTENT_DATA = y.CONTENT_DATA and y.OPERATION <> 'DELETE' )
        //      ORDER BY x.ID TOP 5
        DatabaseSelectAllStatement ds = super.buildSelectLockQuery();
        
        
        //x.MIME_TYPE is not "application/octet-stream" 
        ds.addCondition(Conditions.notEq(new FieldNameDatabaseCondition(Constants.TABLE_INDEXABLE_DATA__MIME_TYPE),Constants.UNDEFINED_MIME_TYPE));
        //AND OPERATION = 'INSERT' 
        ds.addCondition(Conditions.eq(Constants.TABLE_INDEXABLE_DATA__OPERATION, Constants.OPERATION_INSERT));
        
        return ds;
        
    }

    protected void process(List <IdData> data, DatabaseConnection connection,  ContentStoreProvider csp) throws RepositoryException {
    	boolean hasUpdated= false;
        DatabaseDialect dialect = connection.getDialect();
        InputStream is = null;
        int result = 0;
        final String msg1 = "ZIP_AND_MOVE failed. Return code: ";
        final String msg2 = "CONVERT_AND_MOVE failed. Return code: ";
        String msg = "";
        for (IdData record: data)
        {
            try 
            {
                if (record.stageId != null)
                {
                    // text provided - call ZipAndMove stored procedure and update text source
                    result = zipAndMove(connection, record, MSG_TEXT_PROVIDED);
                    msg = msg1+result;
                }
                else if (!isMIMETypeSupported(dialect, record.MIMEType))
                {
                    // no extractors for MIME type
                    noExtractors(connection,record);
                }
                else if (dialect.isMIMETypeSupported(record.MIMEType))
                {
                    // native extractor
                    ContentStore store = csp.getStore(record.storeName);
                    // put content into CM_FTS_STAGE and call CONVERT_AND_MOVE
                    store.begin(connection);
                    is = store.get(record.storeContentId);
                    insertIntoStage(connection, is, (int)store.getContentLength(record.storeContentId), record.ftsDataId,"x"+ getExtensionByMIME(record.MIMEType));
                    result = convertAndMove(connection, record);
                    msg = msg2+result;
                    store.rollback();
                } else {
                    // use external extractor
                    ContentStore store = csp.getStore(record.storeName);
                    
                    // call external extractor
                    TextExtractor extractor = externalExtractors.get(record.MIMEType);
                    FileBackedOutputStream fos = new FileBackedOutputStream();
                    is = store.get(record.storeContentId);
                    OutputStreamWriter osw = new OutputStreamWriter(fos,Constants.EXTRACTED_TEXT_ENCODING);

                    store.rollback();
                    
                    try {
                        extractor.extract(record.MIMEType,is,osw);
                    }
                    catch (Exception e){
                        log.error("extractor failed",e);
                        // cleanup and report error
                        is.close();
                        fos.close();
                        msg = "External extractor for MIME " + record.MIMEType; 
                        //reportErrorDeleteRecord(connection, record, ERROR_TYPE_TXT_EXTRACTION, ERROR_CODE_TXT_EXTRACTION_FAILED, "External extractor for MIME " + record.MIMEType);
                        result = -1;
                        //throw new RepositoryException("External Extractor failed",e);
                    }
                    
                    
                    log.debug("content: " + fos.dump());
                    int length = (int)fos.getLength();
                    log.debug("content length: " + length);
                    is = fos.toInputStream();
                    // put text to CM_FTS_STAGE and call ZIP_AND_MOVE
                    insertIntoStage(connection, is, length, record.ftsDataId,"");
                    msg = MSG_EXTERNAL_EXTRACTOR + " " + record.MIMEType;
                    result = zipAndMove(connection, record, msg);
                    msg = msg1 + result;
                    
                }
                if (result == 0) { // OK
                   DatabaseStatement ds = buildUpdateProcessedRecordStatement(record.id);
                   ds.execute(connection);
                   hasUpdated = true;
                }
                else { // issue
                    connection.rollback(); // clean stage
                    // report error
                    // report error and delete record 
                    reportErrorDeleteRecord(connection, record, ERROR_TYPE_TXT_EXTRACTION, ERROR_CODE_TXT_EXTRACTION_FAILED, msg);
                }
                connection.commit();
            }
            catch (Exception e){
                log.error("Text extraction failed",e);
                throw new RepositoryException("Text extraction failed", e);
            }
            finally{
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException ioe){
                        log.error("failed to close input stream", ioe);
                    }
                if (connection != null){
                    try {
                        connection.rollback();
                    } catch (RepositoryException re){
                        log.error("failed to rollback", re);
                    }
                }
                
            }
        }
        if (hasUpdated){
        	forceCommandExecutio(IndexingCommand.class.getName());
        }
            
    }
    
    
    /**
     * Checks if text could be extracted
     * @param dialect
     * @param mime
     * @return true, if yes
     */
    protected boolean isMIMETypeSupported(DatabaseDialect dialect, String mime){
        boolean result = false;
        if (mime != null) {
            result = (dialect.isMIMETypeSupported(mime) || externalExtractors.containsKey(mime));
        }
        return result;
    }
    
    /**
     * No extractors found for mime type - delete record and update CM_FTS_DATA
     * @param connection
     * @param record
     * @throws RepositoryException 
     */
    protected void noExtractors(DatabaseConnection connection, IdData record)throws RepositoryException{
       String msg = MSG_EXTRACTOR_NOT_FOUND + " " + record.MIMEType;
       log.debug("exstractor not found for MIME type: " + record.MIMEType);
       
       // delete from CM_INDEXABLE_DATA
       DatabaseStatement ds = new DatabaseDeleteStatement(Constants.TABLE_INDEXABLE_DATA,Constants.FIELD_ID,record.id);
       ds.execute(connection);
       updateTextSourceStatement(record.ftsDataId, msg).execute(connection);
       
       log.debug(msg); 
    }
    
    private DatabaseStatement updateTextSourceStatement(Long ftsDataId, String source){
        DatabaseUpdateStatement ds = new DatabaseUpdateStatement(Constants.TABLE_FTS_DATA);
        ds.addCondition(Conditions.eq(Constants.FIELD_ID,ftsDataId));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_DATA__TEXT_SOURCE,source));
        return ds;
    }
    
    private int zipAndMove(DatabaseConnection connection, IdData record, String msg) throws RepositoryException {
        // call stored procedure to zip text from CM_FTS_STAGE and put it into FTS data
        
        log.debug("Text provided. ZIP_AND_MOVE called ");
        DatabaseCallableStatement ds = new DatabaseCallableStatement(connection
				.getDialect().convertProcedureName(
						Constants.STORED_PROC_ZIP_AND_MOVE));
        ds.registerReturnParameterType(Types.INTEGER);
        ds.addParameter(record.ftsDataId);
        ds.execute(connection);
        Integer result = (Integer)ds.getReturnValue();
        if (result != 0) {
            // error
            log.error("ZIP_AND_MOVE failed. Return code: " + result);
//            reportErrorDeleteRecord(connection, record, ERROR_TYPE_TXT_EXTRACTION, ERROR_CODE_TXT_ZIP_AND_MOVE_FAILED, "failed with code " + result);
//            throw new RepositoryException("ZIP_AND_MOVE failed with code " + result );
        } else {
            updateTextSourceStatement(record.ftsDataId,msg).execute(connection);
        }
        return result;
        
    }
    private int convertAndMove(DatabaseConnection connection, IdData record) throws RepositoryException {
        // call stored procedure to extract text from CM_FTS_STAGE and put it into FTS data
        
        log.debug("Native extractor. CONVERT_AND_MOVE called ");
        String msg = MSG_NATIVE_EXTRACTOR + " " + record.MIMEType;
        DatabaseCallableStatement ds = new DatabaseCallableStatement(connection
				.getDialect().convertProcedureName(
						Constants.STORED_PROC_CONVERT_AND_MOVE));
        ds.registerReturnParameterType(Types.INTEGER);
        ds.addParameter(record.ftsDataId);
        ds.execute(connection);
        Integer result = (Integer)ds.getReturnValue();
        if (result != 0) {
            // error
            log.error("CONVERT_AND_MOVE failed. Return code: " + result);
//            reportErrorDeleteRecord(connection, record, ERROR_TYPE_TXT_EXTRACTION, ERROR_CODE_TXT_CONVERT_AND_MOVE_FAILED, "failed with code " + result);
            //throw new RepositoryException("CONVERT_AND_MOVE failed with code " + result );
        }
        else {
            updateTextSourceStatement(record.ftsDataId,msg).execute(connection);
        }
        return result;
    }

    
    protected DatabaseUpdateStatement buildUpdateProcessedRecordStatement(Long id) {
        DatabaseUpdateStatement ds = super.buildUpdateProcessedRecordStatement(id);
        
        ds.addValue(SQLParameter.create(Constants.TABLE_INDEXABLE_DATA__OPERATION,Constants.OPERATION_TEXT_EXTRACTED));
        return ds;
    }
    
    private void insertIntoStage(DatabaseConnection connection, InputStream is, int length, Long id, String fileName) throws SQLException, RepositoryException {
        DatabaseInsertStatement ds = new DatabaseInsertStatement(Constants.TABLE_FTS_STAGE);
        ds.addValue(SQLParameter.create(Constants.FIELD_ID, id));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_STAGE__DATA, is, length));
        ds.addValue(SQLParameter.create(Constants.TABLE_FTS_STAGE__FILENAME,fileName));
        ds.execute(connection);
        ds.close();
    }
    

    /**
     * Returns file extension by its MIME type
     * @param mime
     * @return  extension in format (.xxx)
     */
    public static String getExtensionByMIME(String mime){
        // TODO
        // !!! This should be extended
    	// types added for MS Office 2007
        final String[][] ext = { 
                        {".doc","application/msword",},
                        {".xls","application/excel",},
                        {".xls","application/vnd.ms-excel",},
                        {".xls","application/vndms-excel",},
                        {".xls","application/x-excel",},
                        {".xls","application/x-msexcel",},
                        {".ppt","application/mspowerpoint",},
                        {".ppt","application/powerpoint",},
                        {".ppt","application/vnd.ms-powerpoint",},
                        {".ppt","application/mspowerpoint",},
                        {".txt","text/plain",},
                        {".docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",},
                        {".dotx","application/vnd.openxmlformats-officedocument.wordprocessingml.template",},
                        {".xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",},
                        {".xltx","application/vnd.openxmlformats-officedocument.spreadsheetml.template",},
                        {".pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation",},
                        {".ppsx","application/vnd.openxmlformats-officedocument.presentationml.slideshow",},
                        {".potx","application/vnd.openxmlformats-officedocument.presentationml.template",},
//                      Uncomment this to enable native extraction
//                        {".rtf","text/rtf",},
//                        {".pdf","application/pdf",},                 
                        {".html","text/html"},
        };
        for (int i=0; i<ext.length; i++){
            if (ext[i][1].equals(mime))
                return ext[i][0];
        }
        return null;
    
    }
    
    public String getDisplayableName(){
        return "Text extraction";
    }
    

    

}


/*
 * $Log: TextExtractionCommand.java,v $
 * Revision 1.5  2009/06/12 10:27:28  zahars
 * Support for MS Office 2007 in Oracle 11
 *
 * Revision 1.4  2009/03/24 07:54:43  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.3  2009/02/11 15:08:18  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.2  2008/01/16 14:09:43  vpukis
 * PTR#0154674 - fixed to call db stored procedures using qualified names under SQL 2005
 *
 * Revision 1.1  2007/04/26 08:59:49  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.9  2006/11/10 12:18:42  zahars
 * PTR #1803381 transaction demarcation fixed for stored procedures
 *
 * Revision 1.8  2006/10/09 11:22:48  dparhomenko
 * PTR#0148482 fix name rebuild after workspace move
 *
 * Revision 1.7  2006/09/29 09:26:34  zahars
 * PTR#1802683  Commands uses connection instead of connection provider
 *
 * Revision 1.6  2006/09/28 12:23:39  dparhomenko
 * PTR#1802402 add oracle tests
 *
 * Revision 1.5  2006/08/09 08:29:00  vpukis
 * PTR#1801827 grammar error fixed in log message
 *
 * Revision 1.4  2006/08/04 10:52:48  maksims
 * #1802356 Code cleanup
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
 * Revision 1.13  2006/07/14 08:11:39  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.12  2006/07/12 14:44:08  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.11  2006/07/12 12:33:07  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.10  2006/07/10 12:06:16  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.9  2006/07/07 15:00:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.8  2006/07/06 11:17:38  zahars
 * PTR#0144986 document added
 *
 * Revision 1.7  2006/07/06 10:58:29  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.6  2006/07/06 10:09:38  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.4  2006/07/06 07:58:16  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.3  2006/07/05 08:28:30  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 * Revision 1.1  2006/07/04 15:47:27  zahars
 * PTR#0144986 TextExtractionCommand updated
 *
 */