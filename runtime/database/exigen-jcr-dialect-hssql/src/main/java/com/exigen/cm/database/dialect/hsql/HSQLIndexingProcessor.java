/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.database.dialect.hsql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.cmd.fts.FTSCommand;
import com.exigen.cm.cmd.fts.FTSCommand.IdData;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.dialect.IndexingProcessor;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.DatabaseSelectOneStatement;
import com.exigen.cm.database.statements.RowMap;
import com.exigen.cm.store.StoreHelper;
import com.exigen.cm.store.StoreHelper.FileBackedOutputStream;

/**
 * Specific actions on indexing for HSQL
 * 
 */
public class HSQLIndexingProcessor extends IndexingProcessor {

    private static final int BATCH_SIZE =200; // words to add
    private static final Log log = LogFactory.getLog(HSQLIndexingProcessor.class);
    /**
     * Process records from CM_INDEXABLE_DATA with OPERATION=TEXT_EXTRACTED
     * 1. Split text on words
     * 2. Add new words to WORD table
     * 3. Build index
     * 
     * @param records
     * @param connection
     * @throws RepositoryException
     */
    public void process(List<IdData> records, DatabaseConnection connection, boolean isBatchFull) throws RepositoryException {
        Set<String> stopWords = new HashSet<String>();
        readStopWords(connection, stopWords); 
        
        for (IdData record: records){
            Set<String> allWords = new HashSet<String>(); // all words in the document
            InputStream is = null;
            try {
                is = getInputStream(record, connection);
                Reader reader = null;
                if (is != null) {
                    reader = new InputStreamReader(is, Constants.EXTRACTED_TEXT_ENCODING);
                    List<String> words = new LinkedList<String>(); // words for one batch
                    boolean moreWords = true;
                    while ( moreWords ){
                        moreWords = fillWords(words, reader, BATCH_SIZE, stopWords, allWords);
                        Long[] wordIds = new Long[words.size()]; 
                        addNewWords(connection, words, wordIds);
                        buildIndex(connection, record.ftsDataId, wordIds);
                        words = new LinkedList<String>();                        
                    }
                }    
                
            } catch (Exception e){
                log.error("Indexing failed with exception", e);
                if (is != null)
                    try {
                      is.close();
                    } catch (IOException ie){/* empty*/}
                throw new RepositoryException("Indexing failed", e);    
            }
        }
        super.process(records, connection, isBatchFull);
    }
    
    /**
     * @param record
     * @param conn
     * @return null, if no text
     * @throws SQLException
     * @throws RepositoryException
     */
    private InputStream getInputStream(IdData record, DatabaseConnection conn) throws SQLException, RepositoryException, IOException {
        InputStream is = null;
        final String select = "SELECT " + Constants.TABLE_FTS_DATA__TEXT + " FROM " + Constants.TABLE_FTS_DATA + " WHERE " + Constants.FIELD_ID + " =?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        FileBackedOutputStream fbos = null;
        try {
            ps= conn.prepareStatement(select, false);
            ps.setLong(1,record.ftsDataId);
            rs = ps.executeQuery();
            if ( rs.next() ){
                is = rs.getBinaryStream(1);
            }else {
                // no text - report error
                FTSCommand.reportErrorDeleteRecord(conn,record,FTSCommand.ERROR_TYPE_TXT_INDEXING, FTSCommand.ERROR_CODE_TXT_NO_TEXT, "No text found for FTS_DATA record with id: " + record.ftsDataId);
                log.error("No text found for FTS_DATA record with id: " + record.ftsDataId);
            }
            // read into temporary file
            fbos = new FileBackedOutputStream();
            StoreHelper.transfer(is,fbos);
            conn.commit();
            log.debug("Read from FTS_DATA bytes: " + fbos.getLength());
            log.debug("Content from FTS_DATA " + fbos.dump());
        }    
        finally {
            if (rs != null)
                rs.close();
            if (ps!=null)
                ps.close();
        }
        return fbos.toInputStream();
    }
    
    private boolean fillWords(List<String> words, Reader reader, int size, Set<String> stopWords, Set<String> allWords) throws UnsupportedEncodingException, IOException {
        /* We assume that word starts with letter and consists of    
         * alphanumeric chars
         */
        
        boolean moreWords = true;
        int buf;
        while ( (buf=findWord(reader))!=-1 && words.size() <= size ){
            StringBuffer word = new StringBuffer();
            word.appendCodePoint(buf);
            buf = readWord(reader,word); 
            if (word.length() == 1)
                continue; // ignore word with length 1
            String theWord = word.toString().toLowerCase();
            log.debug("Word found: " +theWord);
            if ( allWords.contains(theWord) )
                continue; // word already exists
            if (stopWords.contains(theWord))
                continue; // 
            words.add(theWord);
            allWords.add(theWord);
            log.debug("Word added: " + theWord);
             if (buf == -1)
                    break;
        }
        if (buf == -1) {
            reader.close();
            moreWords = false;
        }
        return moreWords;
    }
    
    private int findWord(Reader reader)throws IOException{
        int buf;
        while ((buf=reader.read()) != -1){
            if (Character.isLetter(buf))
                break;
        }
        return buf;
    }
    
    private int readWord(Reader reader, StringBuffer word) throws IOException {

        int buf;
        while ((buf=reader.read()) != -1){
            if (!Character.isLetterOrDigit(buf))
                break;
            word.appendCodePoint(buf);
        }
        return buf;
        
    }
    
   private void readStopWords(DatabaseConnection conn, Set<String> stopWords) throws RepositoryException {
       DatabaseSelectAllStatement ds = new DatabaseSelectAllStatement(Constants.TABLE_STOPWORD, true);
       ds.addResultColumn(Constants.TABLE_STOPWORD__DATA);
       ds.execute(conn);
       while (ds.hasNext()){
           RowMap row = ds.nextRow();
           String word = row.getString(Constants.TABLE_STOPWORD__DATA);
           log.debug("Stopword: " + word);
           stopWords.add(word);
       }
       log.debug("total stopwords: " + stopWords.size());
       
       
   }
    
    
    
    /*
     * Check if word is in table WORD. If no - add it
     * fill id of the word in WORD
     */
    private void addNewWords(DatabaseConnection conn, List<String> words, Long[] ids) throws RepositoryException {
        
        int i=0;
        boolean isNewWord = false;
        DatabaseInsertStatement ins = new DatabaseInsertStatement(Constants.TABLE_WORD);
        for (String word: words){
            DatabaseSelectOneStatement ds = new DatabaseSelectOneStatement(Constants.TABLE_WORD, Constants.TABLE_WORD__DATA, word);
            try {
                ds.execute(conn);
                log.debug("Old word " + word);
                RowMap row = ds.getRow();
                ids[i] = row.getLong(Constants.FIELD_ID);
            } catch (ItemNotFoundException infe){
                // new word
                log.debug("New word " + word);
                isNewWord = true;
                ids[i] = conn.nextId();
                ins.addValue(SQLParameter.create(Constants.FIELD_ID,ids[i]));
                ins.addValue(SQLParameter.create(Constants.TABLE_WORD__DATA, word));
                ins.addBatch();
            }
            i++;
        }
        
        // Insert new words as batch
        if ( isNewWord ) {
            ins.execute(conn);
            conn.commit();
        }    
    }

    // Build index - fills index entry
    private void buildIndex(DatabaseConnection conn, Long ftsDataId, Long[] ids) throws RepositoryException {
        
        if (ids.length == 0)
            return;
        DatabaseInsertStatement ins = new DatabaseInsertStatement(Constants.TABLE_INDEX_ENTRY);
        for(Long wordId: ids ){
            ins.addValue(SQLParameter.create(Constants.TABLE_INDEX_ENTRY__DATA_ID, ftsDataId));
            ins.addValue(SQLParameter.create(Constants.TABLE_INDEX_ENTRY__WORD, wordId));
            ins.addBatch();
        }
        ins.execute(conn);
        conn.commit();
        
    }
    
    
}


/*
 * $Log: HSQLIndexingProcessor.java,v $
 * Revision 1.1  2007/04/26 09:00:20  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.10  2006/08/18 08:18:08  dparhomenko
 * PTR#1802558 add new features
 *
 * Revision 1.9  2006/08/18 07:32:44  zahars
 * PTR#0144986 Reader fixed
 *
 * Revision 1.8  2006/07/19 10:31:14  zahars
 * PTR#0144986 Oracle index refresh interval introduced
 *
 * Revision 1.7  2006/07/18 11:48:59  zahars
 * PTR#0144986 minor fix
 *
 * Revision 1.6  2006/07/17 09:07:03  zahars
 * PTR#0144986 Comments added
 *
 * Revision 1.5  2006/07/14 08:24:00  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.4  2006/07/14 08:21:35  zahars
 * PTR#0144986 Old FTS Deleted
 *
 * Revision 1.3  2006/07/12 15:05:43  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.2  2006/07/12 14:44:22  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 * Revision 1.1  2006/07/12 12:34:03  zahars
 * PTR#0144986 Hypersonic Indexing implemented
 *
 */
