/* Copyright © 2016 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package com.exigen.cm.store.remote.common;

import java.util.Properties;

public class BatchUnitRemove extends BatchUnit{
    /**
     * Constructs BatchUnit for REMOVE operation.
     * @param contentId
     */
    public BatchUnitRemove(Long jcrContentId){
        super(jcrContentId, BatchOperation.REMOVE, (Properties)null, null);
    }
}

/*
 * $Log: BatchUnitRemove.java,v $
 * Revision 1.1  2007/04/26 09:01:01  dparhomenko
 * PTR#1804279 migrate JCR to maven from B302 directory
 *
 * Revision 1.3  2006/07/28 15:49:06  maksims
 * #1802356 Content ID is changed to Long.
 *
 * Revision 1.2  2006/07/06 16:43:07  maksims
 * #1802356 Cache and maintenance processes implementation added
 *
 * Revision 1.1  2006/07/04 14:04:39  maksims
 * #1802356 Remote Content Stores access implementation added
 *
 */