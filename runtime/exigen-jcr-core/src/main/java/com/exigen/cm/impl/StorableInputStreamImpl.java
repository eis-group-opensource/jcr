/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.exigen.cm.StorableInputStream;

/**
 * Gives ability to provide additional information with binary property
 * 1. Text (for binary document), that would be used for Full Text search (FTS)
 * 2. Store name (where binary property should be stored)
 * 3. Store properties (additional information, needed by store)
 * 4. Include/exclude property into FTS processing
 */
public class StorableInputStreamImpl extends InputStream implements StorableInputStream{

    private InputStream source;
    private String storeId;
    private HashMap<String, String> storeProperties = new HashMap<String, String>();
    private Reader text;
    private Boolean FTSProcessing = null;

    /**
     * 
     * @param source Binary property
     */
    public StorableInputStreamImpl(InputStream source){
        this(source, null, null);
    }

    /**
     * @param source Binary property
     * @param textStream Extracted text
     */
    public StorableInputStreamImpl(InputStream source, Reader textStream){
        this(source, null, textStream);
    }

    /**
     * @param source  Binary property
     * @param storeName Store name
     * @param text Extracted text
     */
    public StorableInputStreamImpl(InputStream source,String storeName, Reader text){
        this.source = source;
        this.storeId = storeName;
        this.text = text;
    }

    public int read() throws IOException {
        return source.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return source.read(b, off, len);
    }

    public int read(byte[] b) throws IOException {
        return source.read(b);
    }

    public long skip(long n) throws IOException {
        return source.skip(n);
    }

    public int available() throws IOException {
        return source.available();
    }

    public void close() throws IOException {
        source.close();
    }

    public synchronized void mark(int readlimit) {
        source.mark(readlimit);
    }

    public boolean markSupported() {
        return source.markSupported();
    }

    public synchronized void reset() throws IOException {
        source.reset();
    }
    
    /**
     * Sets store specic property
     * @param key
     * @param value
     */
    public void setStoreProperty(String key, String value){
        storeProperties.put(key, value);
    }

    /**
     * Set a collection of store specific properties
     * @param props
     */
    public void setStoreProperties(Map<String, String> props){
        storeProperties.putAll(props);
    }

    public String getStoreName() {
        return storeId;
    }

    public Map getStoreProperties() {
        return storeProperties;
    }

	public Reader getExtractedText() {
		return text;
	}

	public Boolean isFTSProcessing() {
		return FTSProcessing;
	}

	/**
	 * 
	 * @param FTSProcessing true, if FTS processing is needed, search otherwise
	 */
	public void setFTSProcessing(boolean FTSProcessing) {
		this.FTSProcessing = FTSProcessing;
	}

}


/*
 * $Log: StorableInputStreamImpl.java,v $
 * Revision 1.1  2007/04/26 08:58:24  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/07/14 12:00:32  zahars
 * PTR#0144986 Storable interface changed
 *
 * Revision 1.5  2006/07/14 11:28:17  zahars
 * PTR#0144986 In StorableInputStream skipFTSProcessing changed
 *
 * Revision 1.4  2006/07/12 11:51:05  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/07/12 10:10:18  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/11 10:25:58  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/04/17 06:46:37  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.2  2006/04/05 12:48:07  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/03/29 12:56:19  dparhomenko
 * PTR#0144983 optimization
 *
 */