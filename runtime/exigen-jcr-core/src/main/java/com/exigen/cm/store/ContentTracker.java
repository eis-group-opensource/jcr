/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.InputStream;
import java.util.Set;

/**
 * API for opened content tracking.
 */
public interface ContentTracker {
    
    /**
     * Adds content id to track list.
     * @param contentId
     */
    public void add(Long contentId, InputStream stream, Throwable owner);
    
    /**
     * Removes content id from track list.
     * @param contentId
     */
    public void remove(InputStream stream);
    
    /**
     * Returns set of stream openers as Throwable if content id is in track list.
     * @param contentId
     * @return
     */
    public Set<Throwable> getContentOpeners(Long contentId);

}

/*
 * $Log: ContentTracker.java,v $
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/09/28 09:19:37  maksims
 * #0147862 Unclosed content streams made tracked
 *
 */