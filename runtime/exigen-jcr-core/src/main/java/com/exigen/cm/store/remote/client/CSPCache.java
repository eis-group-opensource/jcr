/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.exigen.cm.store.SeekableInputStream;
import com.exigen.cm.store.StoreHelper;

/**
 * Content Store Proxy cache implementation.
 */
public class CSPCache{
   
    private static final Log log = LogFactory.getLog(CSPCache.class);
    
//  Holds max number of files per directory
    private int maxFileCount = 1000;
    
    private final File rootDir;
    private final int expirationTime;
    private final int bufferSize;
    
    private List<File> inserts = null;
    private List<File> removes = null;    
    
    CSPCache(File rootDir, int expirationSeconds, int bufferSize){
        this.rootDir=rootDir;
        
//      Root dir can be passed only from Factory which is responsible for dir creation!
//        if(!rootDir.exists() && !rootDir.mkdir()){
//                String message = MessageFormat.format("Failed to create cache Root directory at {0}",
//                        rootDir.getAbsolutePath());
//                log.error(message);
//                throw new RuntimeException(message);
//            }
        
        
//      If less then 0 lives forever if 0 expires immediately
        this.expirationTime=expirationSeconds<1?expirationSeconds : expirationSeconds*1000;
        this.bufferSize=bufferSize;
    }

    public void begin(){
        inserts = new LinkedList<File>();
        removes = new LinkedList<File>();
    }
    
    public void commitPut(){
        inserts = null;
    }
    
    public void commit(){
        inserts = null; // reset inserts
        for(File removed : removes){
              if(!removed.delete()){
                  String message = MessageFormat.format("Failed to delete file for content ID {0} in {1}",
                          removed.getName().substring(0, removed.getName().length()-1),
                          removed.getParentFile().getAbsolutePath());
                  log.warn(message);
                  throw new RuntimeException(message);
              }else
                  if(log.isDebugEnabled()){
                      String message = MessageFormat.format("Deleted file for content ID {0} in {1}",
                              removed.getName().substring(0, removed.getName().length()-1),
                              removed.getParentFile().getAbsolutePath());
                      log.debug(message);
                  }
              
              File dir = removed.getParentFile();
              if(dir.list().length == 0) // clean empty cache directories
                  if(!dir.delete()){
                      String message = MessageFormat.format("Failed to delete empty cache directory at {0}",
                              dir.getAbsolutePath());
                      log.warn(message);
                  }
        }
    }
    

    /**
     * Rolls back changes
     *
     */
    public void rollback(){
        for(File inserted : inserts){ // rollback inserts
            if(!inserted.delete()){
                String message = MessageFormat.format("Failed to delete file for content ID {0} in {1}",
                        inserted.getName().substring(0, inserted.getName().length()-1),
                        inserted.getParentFile().getAbsolutePath());
                log.warn(message);
//                throw new RuntimeException(message);
            }
            
            File dir = inserted.getParentFile();
            if(dir.list().length == 0) // clean empty cache directories
                if(!dir.delete()){
                    String message = MessageFormat.format("Failed to delete empty cache directory at {0}",
                            dir.getAbsolutePath());
                    log.warn(message);
                }            
      }
      
        for(File removed : removes){ // rollback removes
            String name = removed.getName();
            name = name.substring(0, name.length() -1);
            
            File to = new File(removed.getParentFile(), name);
            removed.renameTo(to);
        }
    }
    
    
    
    public InputStream get(Long contentId){
        File dir = getDirForId(contentId);
        try{

            File file = new File(dir, contentId.toString());
            file.setLastModified(System.currentTimeMillis());
            
            RandomAccessFile res = new RandomAccessFile(file, "r");
//            return new FileInputStream(file);
            return new SeekableInputStream(res);
        }catch(FileNotFoundException ex){
            String message = MessageFormat.format("File for content ID {0} not found in {1}",
                    contentId,
                    dir.getAbsolutePath());
            log.debug(message);
        }
        
        return null;
    }
    

    /**
     * Removes file from cache.
     * @param contentId
     */
    public void remove(Long contentId){
        if(removes == null)
            throw new RuntimeException("Cannot delete without active transaction");
            
        File dir = getDirForId(contentId);
        File file = new File(dir, contentId.toString());
        if(!file.exists()){
            log.info(MessageFormat.format("File {0} scheduled for delete is not found in {1} !!!",
                    file.getName(),
                    dir.getParentFile().getAbsolutePath()));
            
            return;
        }

        File backup = new File(dir, contentId+"_");
        file.renameTo(backup);
        removes.add(backup);
        
        if(log.isDebugEnabled()){
            String message = MessageFormat.format("File {0} scheduled for delete by renaming to {1} in {2}",
                    file.getName(),
                    backup.getName(),
                    dir.getParentFile().getAbsolutePath());
            log.debug(message);
        }
        
    }
    

    /**
     * Puts content into cache.
     * @param contentId
     * @param data
     */
    public void put(Long contentId, InputStream data){
        if(inserts == null)
            throw new RuntimeException("Cannot put without active transaction");
            
        File dir = getDirForId(contentId);
        
        synchronized(CSPCache.class){
            if(!dir.exists() && !dir.mkdir()){    
                String message = MessageFormat.format("Failed to create cache directory at {0}",
                        dir.getAbsolutePath());
                log.error(message);
                throw new RuntimeException(message);
            }
        }


        try{
            File file = new File(dir, contentId.toString());
            file.createNewFile();
            StoreHelper.transfer(data, new FileOutputStream(file), bufferSize);
            file.setLastModified(System.currentTimeMillis());
            
            inserts.add(file);
        }catch(Exception ex){
            String message = MessageFormat.format("Failed to write data for content ID {0} in {1}",
                    contentId,
                    dir.getAbsolutePath());
            log.error(message, ex);
            throw new RuntimeException(message,ex);
        }
    }

    
    
    /**
     * Updates File last modified time to current.
     * @param contentId
     */
    public void touch(Long contentId){
        log.debug(MessageFormat.format("Touch invoked for {0}", contentId));
        
        File dir = getDirForId(contentId);
        if(!dir.exists())
            return;
        
       File f = new File(dir, contentId.toString());
       if(f.exists()){
           f.setLastModified(System.currentTimeMillis());
           log.debug(MessageFormat.format("Touch invoked for {0}... Access date updated", contentId));           
       }else{
           log.debug(MessageFormat.format("Touch invoked for {0}... Failed. File not found!", contentId));                      
       }
    }

    /**
     * Returns on demand output stream initialized with target file to write in.
     * File will be created only if write is invoked on this stream.
     * @param contentId
     * @return
     */
    public OutputStream createTargetFor(Long contentId) {
        File dir = getDirForId(contentId);
        if(!dir.exists() && !dir.mkdir()){
                String message = MessageFormat.format("Failed to create cache directory at {0}",
                        dir.getAbsolutePath());
                log.error(message);
                throw new RuntimeException(message);
            }
       return StoreHelper.createOndemandOutputStream(new File(dir, contentId.toString()));
    }

    /**
     * Returns content length in bytes.
     * @param contentId
     * @return
     */
    public long getContentLength(Long contentId) {
        File dir = getDirForId(contentId);
//        if(!dir.exists()){
//                String message = MessageFormat.format("Failed to access content length for {0} as it is not in cache.",
//                        contentId);
//                log.warn(message);
//                return 0;
//            }
        
        File target = new File(dir, contentId.toString());
        
        return target.exists() ? target.length() : 0;
    }
    
    
    /*
     * Returns file reference on cached content if it is not expired.
     * Otherwise <code>null</code> is returned.
     * @param contentId
     * @return
     *
    protected File getFileIfNotExpired(Long contentId){
        File dir = getDirForId(contentId);
        File target = new File(dir, contentId.toString());
        if((target.lastModified()+expirationTime) < System.currentTimeMillis()){
            if(!target.delete()){
                String message = MessageFormat.format("Failed to delete expired content {0} from cache at {1}.",
                            contentId,
                            target.getAbsolutePath());
                log.error(message);
                throw new RuntimeException(message);
            }
            return null;
        }
        
        return target;
    }//*/
    
    public Iterator<Long> getExpiredItemsIterator(){
        return new ExpiredItemsIterator(rootDir, expirationTime);
    }

    
    /**
     * Calculates directory from Content ID taking into 
     * account max number of files per directory.
     * @param idStr
     * @return
     */
    protected File getDirForId(Long id){
//        long id = Long.parseLong(idStr);
        long dirId = id/maxFileCount;
        return new File(rootDir, String.valueOf(dirId));
    }
    
    
    /**
     * Provides iteration over expired file names.
     * @author Maksims
     *
     */
    class ExpiredItemsIterator implements Iterator<Long>{
        
        private File[] dirs;
        private Iterator<File> expiredFiles;
        private long expirationTime;        

        
        private int dirIdx = 0;
        private int childIdx=0;

//      Expired items loaded by chunks of this size
        private int maxChunkSize=100;
        
        
        ExpiredItemsIterator(File root, long expirationTime){
            this.expirationTime = expirationTime;
            if(expirationTime>=0)
                dirs=root.listFiles();
        }
        
        
        public boolean hasNext() {
            if(expirationTime<0) // expiration is OFF
                return false;
            
            if(expiredFiles!= null && expiredFiles.hasNext())
                return true;
            
            loadMore();
            
            return expiredFiles.hasNext();
        }

        public Long next() {
            if(!hasNext())
                return null;

            while(hasNext()){
                String name = expiredFiles.next().getName();
                if(name.charAt(name.length()-1) != '_') // scheduled for delete. skip it!
                    return Long.parseLong(name); // file name is an ID so can return it
            }
                
            return null; // only files scheduled for deletion remain in cache ... no next!
        }

        private void loadMore(){
            List<File> expired = new LinkedList<File>();
            for(; dirIdx<dirs.length; dirIdx++){
                File[] children = dirs[dirIdx].listFiles();
                if(children == null){
                    continue;// no files in this dir ... check another ...
                }
                
                for(;childIdx<children.length; childIdx++){
                    if(children[childIdx].exists() && 
                       (children[childIdx].lastModified()+expirationTime) < System.currentTimeMillis()){
                            expired.add(children[childIdx]);
                    
                        if(expired.size() > maxChunkSize){
                            expiredFiles = expired.iterator();
                            return;
                        }
                    }
                }
                if(expired.size() == 0) // no expired found in current dir
                    continue;
                childIdx=0;
            }
            
            expiredFiles = expired.iterator();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }

}

/*
 * $Log: CSPCache.java,v $
 * Revision 1.1  2007/04/26 09:00:03  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/08/16 11:29:42  maksims
 * #0147862 Unit test added
 *
 * Revision 1.7  2006/08/14 16:18:38  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.6  2006/08/04 12:48:38  maksims
 * #1802356 Empty Cache directories cleanup added
 *
 * Revision 1.5  2006/08/01 11:25:59  maksims
 * #1802356 content last access time update added
 *
 * Revision 1.4  2006/07/28 15:49:09  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.3  2006/07/06 16:43:11  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 * Revision 1.2  2006/07/06 08:22:45  maksims
 * #1802356 Content Store configuration import added
 *
 * Revision 1.1  2006/07/04 14:04:43  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */