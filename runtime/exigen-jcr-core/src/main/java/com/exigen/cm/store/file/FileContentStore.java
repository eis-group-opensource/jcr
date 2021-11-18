/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.file;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.Constants;
import com.exigen.cm.database.DatabaseConnection;
import com.exigen.cm.database.DatabaseTools;
import com.exigen.cm.database.params.SQLParameter;
import com.exigen.cm.database.statements.DatabaseDeleteStatement;
import com.exigen.cm.database.statements.DatabaseInsertStatement;
import com.exigen.cm.database.statements.DatabaseSelectAllStatement;
import com.exigen.cm.database.statements.condition.Conditions;
import com.exigen.cm.store.AbstractContentStore;
import com.exigen.cm.store.SeekableInputStream;
import com.exigen.cm.store.StoreHelper;

/**
 * File based content store implementation.
 * @author Maksims
 */
public class FileContentStore extends AbstractContentStore{

    
    

    /**
     * File containing store validation data
     */
    private static final String VALIDATION_DATA_FILE = "store.dat";
    
    private static final int[] CALENDAR_FIELDS = new int[]{
//                                                        Calendar.SECOND,
                                                          Calendar.MINUTE,
                                                          Calendar.HOUR_OF_DAY,
                                                          Calendar.DAY_OF_MONTH,
                                                          Calendar.MONTH,
                                                          Calendar.YEAR
                                                      };

    private static final long[] CALENDAR_FIELDS_DIFS = new long[]{
//                                                        60*1000,
                                                          60*1000,
                                                          60*60*1000,
                                                          24*60*60*1000,
                                                          31*24*60*60*1000,
                                                          12*31*24*60*60*1000
                                                        };    
    
    
    
    
    private Log log = LogFactory.getLog(FileContentStore.class);

    private static final Random tailGenerator = new Random();
    
    /**
     * Holds list of transactional objects.
     */
    private List<StoreAction> transactionList = null;

    
    
    /**
     * Maximum number of attemts to be performed to reserve new file
     * for content record.
     * Assuming that no more immediate write requests can be recieved
     * at the same time (with msec precision) then specified by this constant.
     */
    private final int maxAttempts;
    
    /**
     * Declares size of buffer to be used to write files.
     */
    private final int bufferSize;
    
    /**
     * Store root directory.
     */
    private final File rootDir;
    private final int rootPathLength;

    
    
    FileContentStore(FileContentStoreConfiguration config){
        super(config);
        this.rootDir = config.getRootDir();
        rootPathLength = this.rootDir.getAbsolutePath().length()+1;
        
        
        this.maxAttempts = config.getAttempts();
        this.bufferSize = config.getBufferSize();
        
//        System.out.println("************************** Fixed FileContentStore V1 *******************");
    }
    
    /**
     * {@inheritDoc}
     */
    protected void _begin(DatabaseConnection connection) {
        transactionList = new ArrayList<StoreAction>();
    }

    /**
     * {@inheritDoc}
     */
    protected void _rollback() {
        if(transactionList ==null) // nothing to rollback ...
            return;
        
        StoreAction action = null;
        try{
            List<StoreAction> current = new ArrayList<StoreAction>(transactionList);
            transactionList = null;
            
            for(int i=current.size()-1; i>=0; i--){
                action = current.get(i);
                action.undo();
            }
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to rollback when executing {0}", new Object[]{action == null ? "" : action.toString()});
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
    /**
     * {@inheritDoc}
     */    
    protected void _commit(){
        List<StoreAction> current = new ArrayList<StoreAction>(transactionList);
        transactionList = null;
        
        for(StoreAction action : current){
            try{
                action.execute();
            }catch(Exception ex){
                String message = MessageFormat.format("Failed to commit when executing {0}", new Object[]{action == null ? "" : action.toString()});
                log.error(message, ex);
                throw new RuntimeException(message, ex);
            }
        }
        
        for(StoreAction action : current)
          action.cleanBackup();
            
    }
    
    
    /**
     * {@inheritDoc}
     */
    protected ContentData _put(Long jcrContentId, InputStream inStream, int length, Map params) {
        try{
            File outFile = reserveFile();
            
            String contentId = outFile.getAbsolutePath().substring(rootPathLength);
            contentId = contentId.replace('\\', '/'); // UNIX fix. Windows path is not supported on *nix systems  but *nix path is Ok for Windows ...
            
            StoreAction.Put put = new StoreAction.Put(this, outFile, inStream);

            ContentData res = new ContentData(contentId, put.getContentSize());
            
            transactionList.add(put);

            return res;
//            return contentId;
            
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to write file into FileStore with root {0}", 
                    new Object[]{rootDir.getAbsolutePath()});
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected InputStream _get(String contentId) {
        File src = new File(rootDir, contentId);
        try{
            RandomAccessFile file = new RandomAccessFile(src, "r");
            return new SeekableInputStream(file);
        	//return new FileInputStream(src);
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to read file {0} from FileStore", 
                    new Object[]{src.getAbsolutePath()});
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
        
    }
    
    /**
     * {@inheritDoc}
     */
    protected void _remove(String contentId) {
        
        File file = new File(rootDir, contentId);
        try{
            transactionList.add(new StoreAction.Remove(this, file));
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to remove file {0} from FileStore", 
                    new Object[]{file.getAbsolutePath()});
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
    
    
    /**
     * Transfers data from input stream to output stream.
     * @param inStream - input data.
     * @param outStream - sink
     * @throws Exception is thrown in case of error.
     */
    long transfer(InputStream inStream, OutputStream outStream) throws Exception{
        return StoreHelper.transfer(inStream, outStream, bufferSize);
    }
    
    /**
     * Reserves new file for writing.
     * <code>null</code> is returned in case no file can be reserved.
     * @return reference on new file ready for writing.
     * @throws IOException is thrown in case of error.
     */
    private static final char separatorChar = '/';//File.separatorChar;
    private File reserveFile() throws IOException{
         
        /*
         FIX ME Need to implement policy not to put more then specific number of file in a single directory!
         
         /year/month/day/hour/minute/content_file...
         
         */
        
        Calendar time = Calendar.getInstance();
        StringBuffer currentDirName = new StringBuffer(10);
        currentDirName.append(time.get(Calendar.YEAR))
            .append(separatorChar)
            .append(time.get(Calendar.MONTH)+1)// Java month starts from 0, more convenient to have month in paht start from 1
            .append(separatorChar)
            .append(time.get(Calendar.DAY_OF_MONTH))
            .append(separatorChar)
            .append(time.get(Calendar.HOUR_OF_DAY))
            .append(separatorChar)
            .append(time.get(Calendar.MINUTE))
            .append(separatorChar)
            .append(time.get(Calendar.SECOND));
        
        File currentDir = new File(rootDir, currentDirName.toString());
        currentDir.mkdirs();
        
        int attemptsCount = 0;

        StringBuffer fileName = new StringBuffer(String.valueOf(System.currentTimeMillis()));
        fileName.append("_");
        

        
        while(attemptsCount++ < maxAttempts){
            StringBuilder currentName = new StringBuilder(fileName);
            currentName
            .append(tailGenerator.nextInt(Integer.MAX_VALUE))
            .append('.').append(attemptsCount);
            
            File file = new File(currentDir, currentName.toString());
            try{
                if(file.createNewFile())
                   return file;
            }catch(IOException ex){
                log.debug("Attempt failed to reserve file due to exception" + ex.getMessage());
            }
        }

        throw new IOException("Failed to reserve file for content");
    }
    
    @Override
    public void create() {
        if(!rootDir.exists() && !rootDir.mkdir()){
            String message = MessageFormat.format("Failed to create root directory {0} for store", rootDir.getAbsolutePath());
            log.error(message);
            throw new RuntimeException(message);
        }

        if(new File(rootDir, VALIDATION_DATA_FILE).exists()){
            String message = MessageFormat.format("Validation data found in store''s {0} root directory {1}. Assuming another store ... cannot create new store in this place."
                    , configuration.getStoreName() , rootDir.getAbsolutePath());
            log.error(message);
            throw new RuntimeException(message);
        }
            
        
        String message = MessageFormat.format("Using root directory {0} for store", rootDir.getAbsolutePath());
        log.info(message);
        
        DatabaseConnection connection = null;
        try {
            
            String key = generateValidationKey();
            String value= generateValidationValue();
            
            DatabaseInsertStatement inst = DatabaseTools.createInsertStatement(Constants.TABLE_SYSTEM_PROPERTIES);
            inst.addValue(SQLParameter.create(Constants.FIELD_ID, key));
            inst.addValue(SQLParameter.create(Constants.TABLE_SYSTEM_PROPERTIES__VALUE, value));
            
            connection = createCommonConnection();
            inst.execute(connection);

            storeValidationValue(value);
            
            connection.commit();
        } catch (RepositoryException e) {
            message = MessageFormat.format("Failed to write validation data to DB for store {0}", configuration.getStoreName());
            log.error(message, e);
            throw new RuntimeException(message, e);
        } catch (Exception e) {
            message = MessageFormat.format("Failed to write validation data for store {0}", configuration.getStoreName());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }finally{
            if(connection != null)
                try {
                    connection.close();
                } catch (RepositoryException e1) {
                    log.error("Failed to close connection to write validation data to DB", e1);
                }
        }
        
    }
    
    
    private String generateValidationValue() {
        return String.valueOf(hashCode()).concat(String.valueOf(System.currentTimeMillis()));
    }

    private String generateValidationKey() {
        return "storeControlValue_".concat(configuration.getStoreName());
    }

    private void storeValidationValue(String value) throws Exception{
        File validationFile = new File(rootDir, VALIDATION_DATA_FILE);
        if(!validationFile.createNewFile())
            throw new Exception(MessageFormat.format("Validation file already exists for store {0} at {1}"
                    , configuration.getStoreName(), validationFile.getAbsolutePath()));

        PrintStream ps = new PrintStream(new FileOutputStream(validationFile));
        try{
            ps.print(value);
            ps.flush();
        }finally{
            ps.close();
        }
    }
    
    private String loadValidationValue() throws Exception{
        String value = null;
        File validationFile = new File(rootDir, VALIDATION_DATA_FILE);
        if(!validationFile.exists())
            throw new Exception(MessageFormat.format("Ensure store is propertly configured. Cannot load validation data for store {0} from {1}"
                    , configuration.getStoreName(), validationFile.getAbsolutePath()));

        BufferedReader sr = new BufferedReader( new FileReader(validationFile));
        
        try{
            value = sr.readLine();
        }finally{
            sr.close();
        }
        
        return value;
    }
    
    protected void _validate() {
        if(!rootDir.exists()){
            String message = MessageFormat.format("Content Store Root directory {0} doesn't exist", rootDir.getAbsolutePath());
            log.error(message);
            throw new RuntimeException(message);
        }
        
        DatabaseConnection connection = null;
        try {
            
            
            String key = generateValidationKey();
            String value = loadValidationValue();
            
            DatabaseSelectAllStatement st = DatabaseTools.createSelectAllStatement(Constants.TABLE_SYSTEM_PROPERTIES, true);
            st.addCondition(Conditions.eq(Constants.FIELD_ID, key));
            st.addCondition(Conditions.eq(Constants.TABLE_SYSTEM_PROPERTIES__VALUE, value));
            
            
            connection = createCommonConnection();
            st.execute(connection);
            if(!st.hasNext()){
                String message = MessageFormat.format("Failed to test validation data for store {0}. Control value stored in DB do not match control value stored in {1} file.", configuration.getStoreName(), new File(rootDir, "store.dat").getAbsolutePath());
                log.error(message);
                throw new RuntimeException(message);
            }
        }catch(RuntimeException e){
            throw e;
        }catch (RepositoryException e) {
            String message = MessageFormat.format("Failed to compare validation data for store {0}. stored in DB with control value stored in {1} file.", configuration.getStoreName(), new File(rootDir, "store.dat").getAbsolutePath());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }catch (Exception e) {
            String message = MessageFormat.format("Failed to compare validation data for store {0}. stored in DB with control value stored in {1} file.", configuration.getStoreName(), new File(rootDir, "store.dat").getAbsolutePath());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }finally{
            if(connection != null)
                try {
                    connection.close();
                } catch (RepositoryException e1) {
                    log.error("Failed to close connection to compare validation data with persisted in DB", e1);
                }
        }
        
//      Test store can perform basic operations
        tryOp();        
    }
    
    /**
     * try store operations.
     */
    protected void tryOp(){
        int state = 0;
        InputStream readStream = null;
        
        try{
    //      try write
            byte[] testData = new byte[]{0,1,2,3,4,5};
            Long testId = 0L;
            ByteArrayInputStream inStream = new ByteArrayInputStream(testData);
            
            _begin(null);
            ContentData data = _put(testId, inStream, testData.length, null);
            inStream.close();
            state++; // write passed
            
            _commit();
            state++;// write commit passed
            
            readStream = _get(data.getStoreContentId());
            state++;// read passed
            
            
            int b;
            int c=0;
            while( (b=readStream.read()) > 0)
                if(testData[c++] != b)
                    throw new Exception();
            readStream.close();
            
            state++;// read compare passed
            
            
            _begin(null);
            _remove(data.getStoreContentId());
            state++;// remove passed
            
            _commit();
        }catch(Exception e){
            String message;
            switch(state){
                case 0:     // put failed
                    message = MessageFormat.format("Failed to validate store {0}. Test data put operation failed for store located at {1}"
                            , configuration.getStoreName()
                            , rootDir.getAbsolutePath());
                    break;
                case 1:     // put commit failed
                    message = MessageFormat.format("Failed to validate store {0}. Test data put commit operation failed for store located at {1}"
                            , configuration.getStoreName()
                            , rootDir.getAbsolutePath());
                    break;
                case 2:     // get failed
                    message = MessageFormat.format("Failed to validate store {0}. Test data read operation failed for store located at {1}"
                            , configuration.getStoreName()
                            , rootDir.getAbsolutePath());
                    break;
                case 3:     // read compare failed
                    message = MessageFormat.format("Failed to validate store {0}. Test read returned incorrect data from store located at {1}"
                                                        , configuration.getStoreName()
                                                        , rootDir.getAbsolutePath());
                    break;
                case 4:     // remove failed
                    message = MessageFormat.format("Failed to validate store {0}. Test data remove operation failed for store located at {1}"
                            , configuration.getStoreName()
                            , rootDir.getAbsolutePath());
                    break;
                case 5:     // remove commit failed
                    message = MessageFormat.format("Failed to validate store {0}. Test data remove commit operation failed for store located at {1}"
                            , configuration.getStoreName()
                            , rootDir.getAbsolutePath());
                    break;
                    
                default:
                    message = "UNKNOWN";
            }
            throw new RuntimeException(message, e);
        }finally{
            if(readStream != null)
                try {
                    readStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

    }
    
    
    protected void _drop() {

        File validationFile = new File(rootDir, VALIDATION_DATA_FILE);
        if(validationFile.exists()){
            log.info(MessageFormat.format("Deleting validation data file for store at {0}"
                    , validationFile.getAbsolutePath()));

        if(!validationFile.delete())
            log.warn(MessageFormat.format("Failed to delete validation data file for store at {0}"
                    , validationFile.getAbsolutePath()));

        }
        
        boolean success = drop(rootDir); 
        
        log.info(MessageFormat.format("File store at {0} is removed {1}completely. Clearing validation data from DB"
                , rootDir.getAbsolutePath()
                , success ? "":"in"));
        
        
        DatabaseConnection connection = null;
        try {
            String key = generateValidationKey();
            
            DatabaseDeleteStatement st = DatabaseTools.createDeleteStatement(Constants.TABLE_SYSTEM_PROPERTIES);
            st.addCondition(Conditions.eq(Constants.FIELD_ID, key));
            connection = createCommonConnection();
            st.execute(connection);
            connection.commit();
        }catch(RuntimeException e){
            throw e;
        }catch (RepositoryException e) {
            String message = MessageFormat.format("Failed to clear validation data for store {0}", configuration.getStoreName());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }catch (Exception e) {
            String message = MessageFormat.format("Failed to clear validation data for store {0}", configuration.getStoreName());
            log.error(message, e);
            throw new RuntimeException(message, e);
        }finally{
            if(connection != null)
                try {
                    connection.close();
                } catch (RepositoryException e1) {
                    log.error("Failed to close connection to clear validation data to DB", e1);
                }
        }
        
        
    }
    
    protected boolean drop(File target){
        if(target.isDirectory()){
            for(File f:target.listFiles())
                drop(f);
        }
        
        if(!target.delete()){
            String message = MessageFormat.format("Failed to delete {0} while dropping file store. Store {1} dropped incompletely. Please cleanup manully", target.getAbsolutePath(), configuration.getStoreName());
            log.debug(message);
            return false;
        }
        
        return true;
    }


    /**
     * Deletes directory subtree if it consists of empty folders which 
     * cannot be used to store content due to existing store dirs hierarchy ...
     * e.g. if there is an empty dir for 2006/11/07 and now is 2007/01/08
     * such a dir will never get new content so it can be removed.
     * @param dir
     */
    void deleteIfEmpty(File dir) {
        if(!dir.exists()) // can already be removed ...
            return;
        
        Calendar c = Calendar.getInstance();

        File cd =dir;
        for(int i=0; i<CALENDAR_FIELDS.length; i++){
            c.set(CALENDAR_FIELDS[i], Integer.parseInt(cd.getName()));
            cd = cd.getParentFile();
        }
        try{
            deleteIfEmpty(dir, c, 0);
        }catch(Exception ex){
            if(log.isDebugEnabled()){
                String message = MessageFormat.format("Failed to perform empty directories cleanup for {0}",
                        dir.getAbsolutePath());
                log.debug(message, ex);
            }
        }
    }
    
    void deleteIfEmpty(File dir, Calendar c, int position) {
        if(dir.list().length != 0 || position == CALENDAR_FIELDS.length)
            return;
        
        File parent = dir.getParentFile();
        if(System.currentTimeMillis() - c.getTimeInMillis() > CALENDAR_FIELDS_DIFS[position])
            dir.delete();

        c.set(CALENDAR_FIELDS[position++], 0);
        deleteIfEmpty(parent, c, position);
    }
}
/*
 * $Log: FileContentStore.java,v $
 * Revision 1.9  2009/02/24 14:26:26  maksims
 * *** empty log message ***
 *
 * Revision 1.8  2009/02/24 12:35:11  maksims
 * *** empty log message ***
 *
 * Revision 1.6  2009/02/06 12:21:48  maksims
 * *** empty log message ***
 *
 * Revision 1.5  2009/02/05 16:19:32  maksims
 * *** empty log message ***
 *
 * Revision 1.4  2009/02/05 10:42:33  maksims
 * *** empty log message ***
 *
 * Revision 1.3  2009/02/05 10:02:49  maksims
 * *** empty log message ***
 *
 * Revision 1.2  2009/02/04 12:16:39  maksims
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:55  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.24  2006/12/15 11:54:43  dparhomenko
 * PTR#1803217 code reorganization
 *
 * Revision 1.23  2006/11/15 14:22:11  maksims
 * #1802721 content length on put made optional
 *
 * Revision 1.22  2006/11/07 16:27:41  maksims
 * #1801897 Empty dirs cleanup added
 *
 * Revision 1.21  2006/10/17 10:47:23  dparhomenko
 * PTR#0148641 fix default values
 *
 * Revision 1.20  2006/10/16 14:45:12  maksims
 * #0147862 Seconds level added
 *
 * Revision 1.19  2006/10/11 13:09:07  dparhomenko
 * PTR#1803094 drop only jcr objects
 *
 * Revision 1.18  2006/10/11 11:50:31  maksims
 * #0147862 Exception catch in reserve file added.
 *
 * Revision 1.17  2006/09/28 11:11:05  maksims
 * #0147862 file name extension denoting attemp added
 *
 * Revision 1.16  2006/09/28 11:06:36  maksims
 * #0147862 file name tail generation changed to random
 *
 * Revision 1.15  2006/09/28 09:19:41  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.14  2006/08/14 16:18:42  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.13  2006/08/08 13:10:45  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.12  2006/07/28 15:49:14  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.11  2006/07/06 08:22:47  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.10  2006/07/04 15:37:29  maksims
 * #1802356 Remove fixed
 *
 * Revision 1.9  2006/07/04 14:03:31  maksims
 * #1802356 Content Stores Configuration implementation updated
 *
 * Revision 1.8  2006/05/03 15:48:41  maksims
 * #0144986 validate result is made cached
 *
 * Revision 1.7  2006/05/03 13:37:26  maksims
 * #0144986 ContentStore.begin method got DatabaseConnection param
 *
 * Revision 1.6  2006/05/03 08:36:19  maksims
 * #0144986 Content store provider constructor changed
 *
 * Revision 1.5  2006/05/02 11:44:27  maksims
 * #0144986 DB Content store type added
 *
 * Revision 1.4  2006/04/17 06:47:02  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.6  2006/04/12 12:19:54  maksims
 * #0144986 imports organized
 *
 * Revision 1.5  2006/04/12 12:18:45  maksims
 * #0144986 Seekable support added to File Store
 *
 */