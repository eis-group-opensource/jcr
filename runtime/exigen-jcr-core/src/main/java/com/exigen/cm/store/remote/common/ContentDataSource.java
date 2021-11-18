/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.common;

import java.io.InputStream;

/**
 * Returns input stream on Content Data.
 * @author Maksims
 *
 */
public interface ContentDataSource {
    
    /**
     * Returns ID of Content given data source represents.
     * @return
     */
    public Long getContentId();
    
    /**
     * Returns input stream on content data
     * @return
     */
    public InputStream getData();
    
    /**
     * Returns size of Content data in bytes if known.
     * @return
     */
    public int getLength();
    

    /**
     * Releases resources captured by given data source.
     */
    public void release();
}

/*
 * $Log: ContentDataSource.java,v $
 * Revision 1.1  2007/04/26 09:01:01  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/08/08 13:10:37  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.2  2006/07/28 15:49:06  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.1  2006/07/04 14:04:39  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */