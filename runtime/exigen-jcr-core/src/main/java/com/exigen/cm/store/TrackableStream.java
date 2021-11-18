/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * InputStream allowing to register tracker capable to trace stream opener.
 */
public class TrackableStream extends FilterInputStream implements Trackable{

    private ContentTracker tracker = null;
	private String contentId;
    
    public TrackableStream(InputStream stream, String contentId){
        super(stream);
        this.contentId = contentId;
    }
    
    public void setTracker(ContentTracker tracker) {
        this.tracker = tracker;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if(tracker != null)
            tracker.remove(this);
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
    }
    @Override
    public String toString() {
    	if (contentId == null){
    		return super.toString();
    	} else {
    		return "contentId : "+contentId;
    	}
    }
}
/*
 * $Log: TrackableStream.java,v $
 * Revision 1.2  2009/02/23 14:30:20  dparhomenko
 * *** empty log message ***
 *
 * Revision 1.1  2007/04/26 08:59:41  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.1  2006/11/07 16:28:11  maksims
 * #1801897 Trackable stream moved to a separate file
 *
 */