/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm;

import java.io.Reader;
import java.util.Map;

/**
 * TODO Put class description here
 * 
 */
/**
 * TODO Put class description here
 * 
 */
/**
 * Allows to provide additional information with binary property
 * 
 */
public interface StorableInputStream {


    /**
     * Returns Content Store Name
     * @return Store name
     */
    public String getStoreName() ;

    /**
     * Returns Content store specific properties
     * @return Store properties
     */
    public Map getStoreProperties() ;
    
    /**
     * Returns text to be used in Full Text Search
     * @return text
     */
    public Reader getExtractedText() ;
    
    /**
     * Check, if property should participate in Full Text Search process
     * @return True - use for FTS, False - not use for FTS, null - use default rules
     */
    public Boolean isFTSProcessing();
    
}


/*
 * $Log: StorableInputStream.java,v $
 * Revision 1.1  2007/04/26 08:59:29  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.6  2006/07/14 12:00:34  zahars
 * PTR#0144986 Storable interface changed
 *
 * Revision 1.5  2006/07/14 11:28:24  zahars
 * PTR#0144986 In StorableInputStream skipFTSProcessing changed
 *
 * Revision 1.4  2006/07/12 11:51:18  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.3  2006/07/12 10:10:30  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.2  2006/07/11 10:26:07  dparhomenko
 * PTR#1802310 Add new features to DatabaseConnection
 *
 * Revision 1.1  2006/04/17 06:47:17  dparhomenko
 * PTR#0144983 restructurization
 *
 * Revision 1.1  2006/04/03 13:08:24  dparhomenko
 * PTR#0144983 security
 *
 * Revision 1.1  2006/03/29 12:56:19  dparhomenko
 * PTR#0144983 optimization
 *
 */