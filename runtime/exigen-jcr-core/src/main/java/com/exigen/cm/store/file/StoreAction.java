/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/

package com.exigen.cm.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Presents single store action.
 * @author Maksims
 */
interface StoreAction{
    
    /**
     * Executes action.
     */
    void execute() throws Exception;
    
    
    /**
     * Restores state altered by given action.
     */
    void undo() throws Exception;
    
    /**
     * Removes backups after all actions are successfully executed.
     *
     */
    void cleanBackup();
    
    
    static class Remove implements StoreAction{
        private File backup;
        private final FileContentStore store;
        private final File target;
        private final File dir;

        
        Remove(FileContentStore store,File target) throws Exception{
            this.store=store;
            this.target=target;
            dir = target.getParentFile();
        }

        public void execute() throws Exception{
            
            backup = File.createTempFile("del", "fcs");
            store.transfer(new FileInputStream(target), new FileOutputStream(backup));
            backup.deleteOnExit();
            
            if(!target.delete())
                throw new Exception("Failed to delete file: " + target.getAbsolutePath());
        }
        
        
        public void undo()  throws Exception{
//          try to restore if backup exists and target has been deleted
            if(backup!= null && !target.exists()){ 
                target.createNewFile();
                store.transfer(new FileInputStream(backup), new FileOutputStream(target));
            }
        }
        
        public void cleanBackup(){
            if(backup!= null){
                backup.delete();
            }
            
            store.deleteIfEmpty(dir);
        }
        
        public String toString(){
            return new StringBuffer()
            .append("Action REMOVE: ")
            .append(target.getAbsolutePath())
            .toString();
        }
    }
    
    
    static class Put implements StoreAction{
        private final File target;
        private long size = -1;
        Put(FileContentStore store, File target, InputStream data) throws Exception{
            this.target=target;
            size = store.transfer(data, new FileOutputStream(target));
        }

        public long getContentSize(){
            return size;
        }
        
        public void execute()  throws Exception{}
        
        public void undo()  throws Exception{
            target.delete();
        }
        
        public String toString(){
            return new StringBuffer()
            .append("Action PUT: ")
            .append(target.getAbsolutePath())
            .toString();
        }
        
        public void cleanBackup(){}
    }

    /*
     * method update removed
     */
    /*
    static class Update implements StoreAction{
        private final File backup;        
        private final FileContentStore store;
        private final File target;
        
        Update(FileContentStore store, File target, InputStream data) throws Exception{
            this.store=store;
            this.target=target;
            backup = File.createTempFile("upd", "fcs");
            store.transfer(new FileInputStream(target), new FileOutputStream(backup));
            store.transfer(data, new FileOutputStream(target));
            backup.deleteOnExit();
        }

        public void undo()  throws Exception{
            store.transfer(new FileInputStream(backup), new FileOutputStream(target));            
        }

        
        public String toString(){
            return new StringBuffer()
            .append("Action UPDATE: ")
            .append(target.getAbsolutePath())
            .toString();
        }
    }//*/
    
}
/*
 * $Log: StoreAction.java,v $
 * Revision 1.1  2007/04/26 08:59:55  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.8  2006/11/07 16:27:41  maksims
 * #1801897 Empty dirs cleanup added
 *
 * Revision 1.7  2006/08/14 16:18:42  maksims
 * #1802414 Content Store configuration fixed
 *
 */