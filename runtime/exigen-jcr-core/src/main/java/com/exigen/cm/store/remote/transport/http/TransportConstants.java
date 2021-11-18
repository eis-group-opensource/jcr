/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.transport.http;

public interface TransportConstants {
    
    /**
     * Commands server must support transferred in a request header
     * Holds available transport operations
     */
    public static enum Operation {UPDATE, GET, PING};
    
    /**
     * Name of header which contains name of command for execution.
     */
    public static final String HEADER_CMD_NAME="command";

    /**
     * Name of header which contains store name.
     */
    public static final String HEADER_STORE="contentStore";
    
    /**
     *     Name of parameter which contains content ID.
     */
    public static final String CONTENT_ID="contentId";

    
    /**
     *   mime-type for content sending
     */
    public static final String TRANSFER_MIME_TYPE="application/octet-stream";
    
    
    /**
     * Serialized Batch Unit fields.
     */
    public static enum SBU_FIELD {OPERATION, PARAM, DATA, LENGTH};


    
//  HTTP status codes are defined in java.net.HttpURLConnection fields
}

/*
 * $Log: TransportConstants.java,v $
 * Revision 1.1  2007/04/26 08:59:27  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.2  2006/08/08 13:10:36  maksims
 * #1802356 content length param added to store.put method
 *
 * Revision 1.1  2006/07/04 14:04:41  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */