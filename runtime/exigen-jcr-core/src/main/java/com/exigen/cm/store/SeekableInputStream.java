/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * InputStream supporting seek operation.
 * @author Maksims
 *
 */
public class SeekableInputStream extends InputStream implements Seekable, Trackable {
    
    private ContentTracker tracker;
    
    private final RandomAccessFile file;
    
    private long position=0;

    public SeekableInputStream(RandomAccessFile src){
        file=src;
    }

    /**
     * @inheritDoc
     */
    public void seek(long pos) throws IOException {
        position=pos;
        file.seek(position);
    }

    @Override
    public int read() throws IOException {
        int read = file.read();
        if(read>0) 
            position++;
        
        return read;
    }
    
    @Override
    public int available() throws IOException {
        return (int)(file.length()-position);
    }
    
    @Override
    public void close() throws IOException {
        file.close();
        
        if(tracker != null)
            tracker.remove(this);
        
//        System.out.println("Stream closed!");
    }
    
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = file.read(b, off, len);
        if(read>0) 
            position +=read;
        
        return read;
    }
    
    /**
     * Resets stream to start position.
     */
    public synchronized void reset() throws IOException {
        position=0;
        seek(position);
    }
    
    @Override
    public long skip(long n) throws IOException {
        int read = file.skipBytes((int)n);
        if(read>0) 
            position +=read;
        
        return read;
    }

    public void setTracker(ContentTracker tracker) {
        this.tracker = tracker;
    }
    
    
    @Override
    protected void finalize() throws Throwable {
        close();
    }
}

/*
 * $Log: SeekableInputStream.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/09/28 09:19:37  maksims
 * #0147862 Unclosed content streams made tracked
 *
 * Revision 1.2  2006/08/14 16:18:36  maksims
 * #1802414 Content Store configuration fixed
 *
 * Revision 1.1  2006/04/17 06:47:14  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/13 10:03:49  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/12 12:18:43  maksims
 * #0144986 Seekable support added to File Store
 *
 */